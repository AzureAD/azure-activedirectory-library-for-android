// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/**
 * Receives system broadcast message for application install events. You need to
 * register this receiver in your manifest for PACKAGE_INSTALL and
 * PACKAGE_ADDED.
 */
public class ApplicationReceiver extends BroadcastReceiver {

    private static final String TAG = ApplicationReceiver.class.getSimpleName() + ":";

    /**
     *  Shared preference to track broker install.
     */
    public static final String INSTALL_REQUEST_TRACK_FILE = "adal.broker.install.track";

    /**
     * Shared preference key for install request.
     */
    public static final String INSTALL_REQUEST_KEY = "adal.broker.install.request";

    /**
     * Shared preference timestamp for install.
     */
    public static final String INSTALL_REQUEST_TIMESTAMP_KEY = "adal.broker.install.request.timestamp";

    private static final String INSTALL_UPN_KEY = "username";

    /**
     * Application link to open in the browser.
     */
    public static final String INSTALL_URL_KEY = "app_link";
    
    // Allow 5 mins for broker app to be installed
    private static final int BROKER_APP_INSTALLATION_TIME_OUT = 5;
    
    private BrokerProxy mBrokerProxy;

    /**
     * This method receives message for any application status based on filters
     * defined in your manifest.
     *
     * @param context ApplicationContext
     * @param intent to get the installed package name
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the application is install and belongs to the broker package
        final String methodName = "onReceive";
        if (!intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) || intent.getData() == null) {
            return;
        }
        Logger.v(TAG + methodName, "Application install message is received");
        Logger.v(TAG + methodName, "ApplicationReceiver detectes the installation of " + intent.getData().toString());
        final String receivedInstalledPackageName = intent.getData().toString();
        if (receivedInstalledPackageName.equalsIgnoreCase("package:"
                + AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME)
                || receivedInstalledPackageName.equalsIgnoreCase("package:"
                + AuthenticationSettings.INSTANCE.getBrokerPackageName())) {

            String request = getInstallRequestInthisApp(context);
            mBrokerProxy = new BrokerProxy(context);
            final Date dateTimeForSavedRequest = new Date(getInstallRequestTimeStamp(context));

            // Broker request will be resumed if
            // 1) there is saved request in sharedPreference
            // 2) app has the correct configuration to get token from broker
            // 3) the saved request is not timeout
            if (!StringExtensions.isNullOrBlank(request) && mBrokerProxy.canSwitchToBroker("") == BrokerProxy.SwitchToBroker.CAN_SWITCH_TO_BROKER
                    && isRequestTimestampValidForResume(dateTimeForSavedRequest)) {
                Logger.v(TAG + methodName, receivedInstalledPackageName + " is installed, start sending request to broker.");
                resumeRequestInBroker(context, request);
            } else {
                Logger.v(TAG + methodName, "No request saved in sharedpreferences or request already timeout"
                        + ", cannot resume broker request.");
            }
        }
    }

    /**
     * Save request fields into shared preference.
     *
     * @param ctx application context
     * @param request AuthenticationRequest object
     * @param url request url
     */
    public static void saveRequest(final Context ctx, final AuthenticationRequest request, final String url) {
        final String methodName = "saveRequest";
        
        Logger.v(TAG + methodName, "ApplicationReceiver starts to save the request in shared preference.");
        SharedPreferences prefs = ctx.getSharedPreferences(INSTALL_REQUEST_TRACK_FILE,
                Activity.MODE_PRIVATE);
        
        if (prefs != null) {
            HashMap<String, String> parameters = StringExtensions.getUrlParameters(url);
            if (parameters != null && parameters.containsKey(INSTALL_UPN_KEY)) {
                Logger.v(TAG + methodName, "Coming redirect contains the UPN, setting it on the request for both loginhint and broker account name.");
                request.setLoginHint(parameters.get(INSTALL_UPN_KEY));
                request.setBrokerAccountName(parameters.get(INSTALL_UPN_KEY));
            }
            Editor prefsEditor = prefs.edit();
            Gson gson = new Gson();
            String jsonRequest = gson.toJson(request);
            prefsEditor.putString(INSTALL_REQUEST_KEY, jsonRequest);
            
            // Also saving the timestamp
            final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            prefsEditor.putLong(INSTALL_REQUEST_TIMESTAMP_KEY, calendar.getTimeInMillis());
            
            prefsEditor.apply();
        } else {
            Logger.v(TAG + methodName, "SharedPreference is null, nothing saved.");
        }
    }

    /**
     * Get username that started the install flow.
     * 
     * @param ctx app/activity context
     * @return the username that started the install flow
     */
    public static String getUserName(Context ctx) {
        Logger.v(TAG, "ApplicationReceiver:getUserName");
        String request = getInstallRequestInthisApp(ctx);
        if (!StringExtensions.isNullOrBlank(request)) {
            Gson gson = new Gson();
            AuthenticationRequest pendingRequest = gson.fromJson(request,
                    AuthenticationRequest.class);
            if (pendingRequest != null) {
                return pendingRequest.getBrokerAccountName();
            }
        }

        return null;
    }

    /**
     * Read install request key from shared preference.
     *
     * @param context application context
     * @return the saved request stored in SharedPreference
     */
    public static String getInstallRequestInthisApp(final Context context) {
        final String methodName = "getInstallRequestInthisApp";
        
        Logger.v(TAG + methodName, "Retrieve saved request from shared preference.");
        SharedPreferences prefs = context.getSharedPreferences(INSTALL_REQUEST_TRACK_FILE,
                Activity.MODE_PRIVATE);
        if (prefs != null && prefs.contains(INSTALL_REQUEST_KEY)) {
            String request = prefs.getString(INSTALL_REQUEST_KEY, "");
            Logger.d(TAG + methodName, "Install request:" + request);
            return request;
        }

        Logger.v(TAG + methodName, "Unable to retrieve saved request from shared preference.");
        return "";
    }

    /**
     * Clear the username after resuming login.
     * 
     * @param ctx app/activity context
     */
    public static void clearUserName(Context ctx) {
        Logger.v(TAG, "ApplicationReceiver:clearUserName");
        SharedPreferences prefs = ctx.getSharedPreferences(INSTALL_REQUEST_TRACK_FILE,
                Activity.MODE_PRIVATE);
        if (prefs != null) {
            Editor prefsEditor = prefs.edit();
            prefsEditor.putString(INSTALL_REQUEST_KEY, "");
            prefsEditor.apply();
        }
    }
    
    private boolean isRequestTimestampValidForResume(final Date savedRequestTimestamp) {
        final String methodName = "isRequestTimestampValidForResume";
        
        // Get current UTC time
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.MINUTE, BROKER_APP_INSTALLATION_TIME_OUT * (-1));
        if (savedRequestTimestamp.compareTo(calendar.getTime()) >= 0) {
            Logger.v(TAG + methodName, "Saved request is valid, not timeout yet.");
            return true;
        }
        
        Logger.v(TAG + methodName, "Saved request is already timeout");
        return false;
    }

    private void resumeRequestInBroker(final Context context, final String request) {
        final String methodName = "resumeRequestInBroker";
        Logger.v(TAG + methodName, "Start resuming request in broker");
        Gson gson = new Gson();
        final AuthenticationRequest pendingRequest = gson.fromJson(request, AuthenticationRequest.class);
        ExecutorService sThreadExecutor = Executors.newSingleThreadExecutor();
        
        sThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Logger.v(TAG + methodName, "Running task in thread:" + android.os.Process.myTid() + ", trying to get intent for "
                        + "broker activity.");
                final Intent resumeIntent = mBrokerProxy.getIntentForBrokerActivity(pendingRequest);
                resumeIntent.setAction(Intent.ACTION_PICK);
                
                Logger.v(TAG + methodName, "Setting flag for broker resume request for calling package " + context.getPackageName());
                resumeIntent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST_RESUME,
                AuthenticationConstants.Broker.BROKER_REQUEST_RESUME);
                resumeIntent.putExtra(AuthenticationConstants.Broker.CALLER_INFO_PACKAGE, context.getPackageName());

                final String brokerProtocolVersion = resumeIntent.getStringExtra(AuthenticationConstants.Broker.BROKER_VERSION);
                if (StringExtensions.isNullOrBlank(brokerProtocolVersion)) {
                    Logger.v(TAG + methodName, "Broker request resume is not supported in the older version of broker.");
                    return;
                }
                
                PackageManager packageManager = context.getPackageManager();
                // Get activities that can handle the intent
                List<ResolveInfo> activities = packageManager.queryIntentActivities(resumeIntent, 0);

                // Check if 1 or more were returned
                boolean isIntentSafe = activities.size() > 0;

                if (isIntentSafe) {
                    Logger.v(TAG + methodName, "It's safe to start .ui.AccountChooserActivity.");
                    resumeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    context.startActivity(resumeIntent);
                } else {
                    Logger.v(TAG + methodName, "Unable to resolve .ui.AccountChooserActivity.");
                }
            }
        });
    }
    
    private long getInstallRequestTimeStamp(final Context context) {
        final String methodName = "getInstallRequestTimeStamp";
        
        Logger.v(TAG + methodName, "Retrieve timestamp for saved request from shared preference.");
        SharedPreferences prefs = context.getSharedPreferences(INSTALL_REQUEST_TRACK_FILE,
                Activity.MODE_PRIVATE);
        if (prefs != null && prefs.contains(INSTALL_REQUEST_TIMESTAMP_KEY)) {
            final long savedRequestTimeStamp = prefs.getLong(INSTALL_REQUEST_TIMESTAMP_KEY, 0);
            Logger.v(TAG + methodName, "Timestamp for saved request is: " + savedRequestTimeStamp);
            return savedRequestTimeStamp;
        }
        
        return 0;
    }
}
