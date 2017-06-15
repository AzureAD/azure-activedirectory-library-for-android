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
 * This BroadcastReceiver is no longer used to catch system broadcasts.
 *
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

    /**
     * This method receives message for any application status based on filters
     * defined in your manifest.
     *
     * @param context ApplicationContext
     * @param intent to get the installed package name
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Method intentionally left blank
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
}
