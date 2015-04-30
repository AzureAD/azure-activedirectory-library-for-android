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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.google.gson.Gson;

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

	/**
	 * This method receives message for any application status based on filters
	 * defined in your manifest.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		// Check if the application is install and belongs to the broker package
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
			Logger.v(TAG, "Application install message is received");
			if (intent != null && intent.getData() != null) {
				Logger.v(TAG, "Installing:" + intent.getData().toString());
				if (intent
						.getData()
						.toString()
						.equalsIgnoreCase(
								"package:"
										+ AuthenticationSettings.INSTANCE
												.getBrokerPackageName())) {
					Logger.v(TAG, "Message is related to the broker");
					String request = getInstallRequestInthisApp(context);
					if (!StringExtensions.IsNullOrBlank(request)) {
						Logger.v(TAG, "Resume request in broker");
						resumeRequestInBroker(context, request);
					}
				}
			}
		}
	}

	public static void saveRequest(Context ctx, AuthenticationRequest request,
			String url) {
		SharedPreferences prefs = ctx.getSharedPreferences(
				INSTALL_REQUEST_TRACK_FILE, Activity.MODE_PRIVATE);
		if (prefs != null) {
			HashMap<String, String> parameters = StringExtensions
					.getUrlParameters(url);
			if (parameters != null && parameters.containsKey(INSTALL_UPN_KEY)) {
				request.setLoginHint(parameters.get(INSTALL_UPN_KEY));
				request.setBrokerAccountName(parameters.get(INSTALL_UPN_KEY));
			}
			Editor prefsEditor = prefs.edit();
			Gson gson = new Gson();
			String jsonRequest = gson.toJson(request);
			prefsEditor.putString(INSTALL_REQUEST_KEY, jsonRequest);
			prefsEditor.apply();
		}
	}

	private void resumeRequestInBroker(Context ctx, String request) {
		Gson gson = new Gson();
		AuthenticationRequest pendingRequest = gson.fromJson(request,
				AuthenticationRequest.class);
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_PICK);
		intent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST,
				pendingRequest);
		intent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST_RESUME,
				AuthenticationConstants.Broker.BROKER_REQUEST_RESUME);
		intent.setPackage(AuthenticationSettings.INSTANCE
				.getBrokerPackageName());
		intent.setClassName(
				AuthenticationSettings.INSTANCE.getBrokerPackageName(),
				AuthenticationSettings.INSTANCE.getBrokerPackageName()
						+ ".AccountChooserActivity");

		PackageManager packageManager = ctx.getPackageManager();

		// Get activities that can handle the intent
		List<ResolveInfo> activities = packageManager.queryIntentActivities(
				intent, 0);

		// Check if 1 or more were returned
		boolean isIntentSafe = activities.size() > 0;

		if (isIntentSafe) {
			ctx.startActivity(intent);
		}
	}

	public static String getInstallRequestInthisApp(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(
				INSTALL_REQUEST_TRACK_FILE, Activity.MODE_PRIVATE);
		if (prefs != null && prefs.contains(INSTALL_REQUEST_KEY)) {
			String request = prefs.getString(INSTALL_REQUEST_KEY, "");
			Logger.d(TAG, "Install request:" + request);
			return request;
		}

		return "";
	}
}
