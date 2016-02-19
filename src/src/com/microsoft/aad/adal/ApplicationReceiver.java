//Copyright © Microsoft Open Technologies, Inc.
//
//All Rights Reserved
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
//OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
//ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
//PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
//See the Apache License, Version 2.0 for the specific language
//governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.google.gson.Gson;
import com.microsoft.aad.adal.AuthenticationConstants.Broker;

/**
 * Receives system broadcast message for application install events. You need to
 * register this receiver in your manifest for PACKAGE_INSTALL and
 * PACKAGE_ADDED.
 */
public class ApplicationReceiver extends BroadcastReceiver {

    private static final String TAG = "ApplicationReceiver";

    public static final String INSTALL_REQUEST_TRACK_FILE = "adal.broker.install.track";

    public static final String INSTALL_REQUEST_KEY = "adal.broker.install.request";

    private static final String INSTALL_UPN_KEY = "username";

    public static final String INSTALL_URL_KEY = "app_link";
    
    private String installedPackageName = null;
    
    private BrokerProxy brokerProxy;

    /**
     * This method receives message for any application status based on filters
     * defined in your manifest.
     */
    @Override
    public void onReceive(Context context, Intent intent) 
    {
        // Check if the application is install and belongs to the broker package
        final String methodName = ":onReceive";
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) 
        {
            Logger.v(TAG + methodName, "Application install message is received");
            if (intent != null && intent.getData() != null) 
            {
                Logger.v(TAG + methodName, "ApplicationReceiver detectes the installation of " + intent.getData().toString());
                final String receivedInstalledPackageName = intent.getData().toString();
                if (receivedInstalledPackageName.equalsIgnoreCase("package:" + AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME) ||
                        receivedInstalledPackageName.equalsIgnoreCase("package:" + AuthenticationSettings.INSTANCE.getBrokerPackageName()))
                {
                    Logger.v(TAG + methodName, receivedInstalledPackageName + " is installed, start sending request to broker.");
                    
                    installedPackageName = receivedInstalledPackageName.equalsIgnoreCase("package:" + AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME)? 
                            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME : AuthenticationConstants.Broker.PACKAGE_NAME;
                    
                    String request = getInstallRequestInthisApp(context);
                    brokerProxy = new BrokerProxy(context);
                    if (!StringExtensions.IsNullOrBlank(request) && brokerProxy.canSwitchToBroker()) 
                    {
                        resumeRequestInBroker(context, request);
                    }
                    else
                    {
                        Logger.v(TAG + methodName, "No request saved in sharedpreferences, cannot resume broker request.");
                    }
                }
            }
        }
    }

    public static void saveRequest(final Context ctx, final AuthenticationRequest request, final String url) 
    {
        final String methodName = ":saveRequest";
        
        Logger.v(TAG + methodName, "ApplicationReceiver starts to save the request in shared preference.");
        SharedPreferences prefs = ctx.getSharedPreferences(INSTALL_REQUEST_TRACK_FILE,
                Activity.MODE_PRIVATE);
        
        if (prefs != null) {
            HashMap<String, String> parameters = StringExtensions.getUrlParameters(url);
            if (parameters != null && parameters.containsKey(INSTALL_UPN_KEY)) 
            {
                Logger.v(TAG + methodName, "Coming redirect contains the UPN, setting it on the request for both loginhint and broker account name.");
                request.setLoginHint(parameters.get(INSTALL_UPN_KEY));
                request.setBrokerAccountName(parameters.get(INSTALL_UPN_KEY));
            }
            Editor prefsEditor = prefs.edit();
            Gson gson = new Gson();
            String jsonRequest = gson.toJson(request);
            prefsEditor.putString(INSTALL_REQUEST_KEY, jsonRequest);
            prefsEditor.apply();
        }
        else
        {
            Logger.v(TAG + methodName, "SharePreference is null, nothing saved.");
        }
    }

    /**
     * Get username that started the install flow.
     * 
     * @param ctx
     * @return
     */
    public static String getUserName(Context ctx) {
        Logger.v(TAG, "ApplicationReceiver:getUserName");
        String request = getInstallRequestInthisApp(ctx);
        if (!StringExtensions.IsNullOrBlank(request)) {
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
     * Clear the username after resuming login.
     * 
     * @param ctx
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

    private void resumeRequestInBroker(final Context context, final String request) 
    {
        final String methodName = ":resumeRequestInBroker";
        Logger.v(TAG + methodName, "Start resuming request in broker");
        Gson gson = new Gson();
        final AuthenticationRequest pendingRequest = gson.fromJson(request, AuthenticationRequest.class);
        ExecutorService sThreadExecutor = Executors.newSingleThreadExecutor();
        
        sThreadExecutor.submit(new Callable<Object>() 
        {
            @Override
            public Object call() 
            {
                Logger.v(TAG + methodName, "Running task in thread:" + android.os.Process.myTid() + ", trying to get intent for "
                        + "broker activity.");
                final Intent resumeIntent = brokerProxy.getIntentForBrokerActivity(pendingRequest);
                resumeIntent.setAction(Intent.ACTION_PICK);
                
                Logger.v(TAG + methodName, "Setting flag for broker resume request for calling package " + context.getPackageName());
                resumeIntent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST_RESUME,
                AuthenticationConstants.Broker.BROKER_REQUEST_RESUME);
                resumeIntent.putExtra(AuthenticationConstants.Broker.CALLER_INFO_PACKAGE, context.getPackageName());

                final String brokerProtocolVersion = resumeIntent.getStringExtra(AuthenticationConstants.Broker.BROKER_VERSION);
                if (StringExtensions.IsNullOrBlank(brokerProtocolVersion))
                {
                    Logger.v(TAG + methodName, "Broker request resume is not supported in the older version of broker.");
                    return null;
                }
                
                PackageManager packageManager = context.getPackageManager();
                // Get activities that can handle the intent
                List<ResolveInfo> activities = packageManager.queryIntentActivities(resumeIntent, 0);

                // Check if 1 or more were returned
                boolean isIntentSafe = activities.size() > 0;

                if (isIntentSafe) 
                {
                    if (!isRunningInForeground(context, installedPackageName))
                    {
                        Logger.v(TAG + methodName, "Broker " + installedPackageName + " is not running in foreground yet. "
                            + "Thread will be sleeping for 2 seconds to wait for broker package going foreground.");
                        try 
                        {
                            Thread.sleep(2000);
                        } 
                        catch (InterruptedException e) 
                        {
                            Logger.v(TAG + methodName, "Receiving InterruptedException for thread sleep: " + e.getMessage());
                        }
                    }
                    Logger.v(TAG + methodName, "It's safe to start .ui.AccountChooserActivity.");
                    resumeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    context.startActivity(resumeIntent);
                }
                else
                {
                    Logger.v(TAG + methodName, "Unable to resolve .ui.AccountChooserActivity.");
                }
                return null;
            }
        });
    }
    
    protected boolean isRunningInForeground(final Context context, final String packageName) 
    {
        ActivityManager activityManager = (ActivityManager) context.getSystemService( Context.ACTIVITY_SERVICE );
        List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for(RunningAppProcessInfo appProcess : appProcesses)
        {
            if(appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
            {
                return appProcess.processName.equalsIgnoreCase(packageName);
            }
        }
        
        return false;
    }

    public static String getInstallRequestInthisApp(final Context context) 
    {
        final String methodName = ":getInstallRequestInthisApp";
        
        Logger.v(TAG + methodName, "Retrieve saved request from shared preference.");
        SharedPreferences prefs = context.getSharedPreferences(INSTALL_REQUEST_TRACK_FILE,
                Activity.MODE_PRIVATE);
        if (prefs != null && prefs.contains(INSTALL_REQUEST_KEY)) 
        {
            String request = prefs.getString(INSTALL_REQUEST_KEY, "");
            Logger.d(TAG + methodName, "Install request:" + request);
            return request;
        }

        Logger.v(TAG + methodName, "Unable to retrieve saved request from shared preference.");
        return "";
    }
}
