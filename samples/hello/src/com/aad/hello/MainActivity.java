// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.aad.hello;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;

public class MainActivity extends Activity {

	protected static final String TAG = "Main";

	final static String AUTHORIZATION_HEADER = "Authorization";

	final static String AUTHORIZATION_HEADER_BEARER = "Bearer ";

	private AuthenticationContext mAuthContext;

	private ProgressDialog mLoginProgressDialog;

	private AuthenticationResult mResult;

	TextView textView1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		textView1 = (TextView) findViewById(R.id.textView1);

		mLoginProgressDialog = new ProgressDialog(this);
		mLoginProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mLoginProgressDialog.setMessage("Login in progress...");

		try {
			// Provide key info for Encryption
			Utils.setupKeyForSample();
			// init authentication Context
			mAuthContext = new AuthenticationContext(MainActivity.this,
					Constants.AUTHORITY_URL, false);
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Encryption failed",
					Toast.LENGTH_SHORT).show();
		}

		Toast.makeText(getApplicationContext(), TAG + "done",
				Toast.LENGTH_SHORT).show();
	}

	public void onClickToken(View v) {
		Log.v(TAG, "token button is clicked");
		mLoginProgressDialog.show();
		mAuthContext.acquireToken(MainActivity.this, Constants.RESOURCE_ID,
				Constants.CLIENT_ID, Constants.REDIRECT_URL,
				Constants.USER_HINT,
				new AuthenticationCallback<AuthenticationResult>() {

					@Override
					public void onError(Exception exc) {
						if (mLoginProgressDialog.isShowing()) {
							mLoginProgressDialog.dismiss();
						}

						Toast.makeText(getApplicationContext(),
								TAG + "getToken Error:" + exc.getMessage(),
								Toast.LENGTH_SHORT).show();

					}

					@Override
					public void onSuccess(AuthenticationResult result) {
						if (mLoginProgressDialog.isShowing()) {
							mLoginProgressDialog.dismiss();
						}

						mResult = result;
						Toast.makeText(getApplicationContext(),
								"Token is returned", Toast.LENGTH_SHORT)
								.show();
					}

				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		mAuthContext.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * send token in the header with async task
	 * 
	 * @param result
	 */
	public void onClickUseToken(View view) {
		if (mResult != null && mResult.getAccessToken() != null) {
			textView1.setText("");
			displayMessage("Sending token to a service");
			new RequestTask(Constants.SERVICE_URL, mResult.getAccessToken())
					.execute();
		} else {
			textView1.setText("Token is empty");
		}
	}

	public void onClickClearTokens(View view) {
		if (mAuthContext != null && mAuthContext.getCache() != null) {
			displayMessage("Clearing tokens");
			mAuthContext.getCache().removeAll();
		} else {
			textView1.setText("Cache is null");
		}
	}

	public void onClickClearCookies(View view) {
		CookieSyncManager.createInstance(MainActivity.this);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
		CookieSyncManager.getInstance().sync();
	}

	private void displayMessage(String msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Simple get request for test
	 * 
	 * @author omercan
	 */
	class RequestTask extends AsyncTask<Void, String, String> {

		private String mUrl;

		private String mToken;

		public RequestTask(String url, String token) {
			mUrl = url;
			mToken = token;
		}

		@Override
		protected String doInBackground(Void... empty) {

			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse httpResponse;
			String responseString = "";

			try {
				HttpGet getRequest = new HttpGet(mUrl);
				getRequest.addHeader(AUTHORIZATION_HEADER,
						AUTHORIZATION_HEADER_BEARER + mToken);
				getRequest.addHeader("Accept", "application/json");

				httpResponse = httpclient.execute(getRequest);
				StatusLine statusLine = httpResponse.getStatusLine();

				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					// negative for unknown
					if (httpResponse.getEntity().getContentLength() != 0) {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						httpResponse.getEntity().writeTo(out);
						out.close();
						responseString = out.toString();
					}
				} else {
					// Closes the connection.
					httpResponse.getEntity().getContent().close();
					throw new IOException(statusLine.getReasonPhrase());
				}
			} catch (ClientProtocolException e) {
				responseString = e.getMessage();
			} catch (IOException e) {
				responseString = e.getMessage();
			}
			return responseString;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			textView1.setText("");
			if (result != null && !result.isEmpty()) {
				textView1.setText("TOKEN_USED");
			}
		}
	}

}
