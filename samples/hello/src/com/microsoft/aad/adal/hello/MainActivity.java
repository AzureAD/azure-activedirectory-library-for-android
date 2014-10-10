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

package com.microsoft.aad.adal.hello;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.IWindowComponent;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.WebviewHelper;

public class MainActivity extends Activity {

    protected static final String TAG = "Main";

    final static String AUTHORIZATION_HEADER = "Authorization";

    final static String AUTHORIZATION_HEADER_BEARER = "Bearer ";

    private AuthenticationContext mAuthContext;

    private ProgressDialog mLoginProgressDialog;

    private AuthenticationResult mResult;
    
    private CustomWebView mWebViewDialog;
    
    private Handler mHandler;

    TextView textView1;

    EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CookieSyncManager.createInstance(getApplicationContext());
        textView1 = (TextView)findViewById(R.id.textView1);

        // Clear previous sessions
        clearSessionCookie();
        try {
            // Provide key info for Encryption
            if (Build.VERSION.SDK_INT < 18) {
                Utils.setupKeyForSample();
            }

            // init authentication Context
            mAuthContext = new AuthenticationContext(MainActivity.this, Constants.AUTHORITY_URL,
                    false);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Encryption failed", Toast.LENGTH_SHORT).show();
        }

        mEditText = (EditText)findViewById(R.id.editTextUsername);
        mEditText.setText("");

        Toast.makeText(getApplicationContext(), TAG + "done", Toast.LENGTH_SHORT).show();
        
        // example for custom webview
        mHandler = new Handler(Looper.getMainLooper());
        mWebViewDialog = new CustomWebView(mHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // To test session cookie behavior
        mLoginProgressDialog = new ProgressDialog(this);
        mLoginProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLoginProgressDialog.setMessage("Login in progress...");

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLoginProgressDialog != null) {
            // to test session cookie behavior
            mLoginProgressDialog.dismiss();
            mLoginProgressDialog = null;
        }
    }

    public void onClickFragmentTest(View v) {
        Intent intent = new Intent(getApplicationContext(), FragmentHolderActivity.class);
        this.startActivity(intent);
    }

    public void onClickAcquireByRefreshToken(View v) {
        Log.v(TAG, "onClickAcquireByRefreshToken is clicked");
        if (mResult != null && mResult.getRefreshToken() != null
                && !mResult.getRefreshToken().isEmpty()) {
            mLoginProgressDialog.show();
            mAuthContext.acquireTokenByRefreshToken(mResult.getRefreshToken(), Constants.CLIENT_ID,
                    Constants.RESOURCE_ID, new AuthenticationCallback<AuthenticationResult>() {

                        @Override
                        public void onError(Exception exc) {
                            if (mLoginProgressDialog.isShowing()) {
                                mLoginProgressDialog.dismiss();
                            }

                            Toast.makeText(getApplicationContext(),
                                    TAG + "getToken Error:" + exc.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override
                        public void onSuccess(AuthenticationResult result) {
                            if (mLoginProgressDialog.isShowing()) {
                                mLoginProgressDialog.dismiss();
                            }

                            mResult = result;
                            Toast.makeText(getApplicationContext(), "Token is returned",
                                    Toast.LENGTH_SHORT).show();

                            if (mResult.getUserInfo() != null) {
                                Toast.makeText(getApplicationContext(),
                                        "User:" + mResult.getUserInfo().getUserId(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Toast.makeText(getApplicationContext(),
                    TAG + "onClickAcquireByRefreshToken refresh token is not present",
                    Toast.LENGTH_SHORT).show();
        }

    }

    private AuthenticationCallback<AuthenticationResult> getCallback() {
        return new AuthenticationCallback<AuthenticationResult>() {

            @Override
            public void onError(Exception exc) {
                if (mLoginProgressDialog != null && mLoginProgressDialog.isShowing()) {
                    mLoginProgressDialog.dismiss();
                }

                Toast.makeText(getApplicationContext(), TAG + "getToken Error:" + exc.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(AuthenticationResult result) {
                if (mLoginProgressDialog != null && mLoginProgressDialog.isShowing()) {
                    mLoginProgressDialog.dismiss();
                }

                mResult = result;
                Log.v(TAG, "Token info:" + result.getAccessToken());
                Log.v(TAG, "IDToken info:" + result.getIdToken());
                Toast.makeText(getApplicationContext(), "Token is returned", Toast.LENGTH_SHORT)
                        .show();

                if (mResult.getUserInfo() != null) {
                    Log.v(TAG, "User info userid:" + result.getUserInfo().getUserId()
                            + " displayableId:" + result.getUserInfo().getDisplayableId());
                    mEditText.setText(result.getUserInfo().getDisplayableId());
                    Toast.makeText(getApplicationContext(),
                            "User:" + mResult.getUserInfo().getDisplayableId(), Toast.LENGTH_SHORT)
                            .show();
                }
            }

        };
    }

    public void onClickAcquireTokenForceRefresh(View v) {
        Log.v(TAG, "onClickAcquireTokenForceRefresh");
        mLoginProgressDialog.show();
        mAuthContext.acquireToken(MainActivity.this, Constants.RESOURCE_ID, Constants.CLIENT_ID,
                Constants.REDIRECT_URL, getUserLoginHint(), PromptBehavior.REFRESH_SESSION, "",
                getCallback());
    }

    public void onClickAcquireTokenSilent(View v) {
        Log.v(TAG, "onClickAcquireTokenSilent is clicked");
        mLoginProgressDialog.show();
        mAuthContext.acquireTokenSilent(Constants.RESOURCE_ID, Constants.CLIENT_ID, getUserId(),
                getCallback());
    }

    public void onClickToken(View v) {
        Log.v(TAG, "token button is clicked");
        mLoginProgressDialog.show();
        mAuthContext.acquireToken(MainActivity.this, Constants.RESOURCE_ID, Constants.CLIENT_ID,
                Constants.REDIRECT_URL, getUserLoginHint(), getCallback());
    }

    
    
    public void onClickDialog(View v) {
        Log.v(TAG, "token button is clicked");
        mLoginProgressDialog.show();
        mAuthContext.acquireToken(mWebViewDialog, Constants.RESOURCE_ID, Constants.CLIENT_ID,
                Constants.REDIRECT_URL, getUserLoginHint(), PromptBehavior.Auto, "", getCallback());
    }

    class CustomWebView implements IWindowComponent {
        Dialog mDialog;
        Activity mActivity;
        Handler mHandlerInView;
        public CustomWebView(Handler handler){
            mHandlerInView  = handler;
            mActivity = MainActivity.this;
        }
        
        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public void startActivityForResult(final Intent intent, final int requestCode) {
            mHandlerInView.post(new Runnable() {
                
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                    LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                    View webviewInDialog = inflater.inflate(R.layout.custom_webview, null);
                    builder.setTitle("custom webview layout");
                    
                    final WebView webview = (WebView)webviewInDialog.findViewById(R.id.webView1);
                    webview.getSettings().setJavaScriptEnabled(true);
                    webview.requestFocus(View.FOCUS_DOWN);

                    // Set focus to the view for touch event
                    webview.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent event) {
                            int action = event.getAction();
                            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                                if (!view.hasFocus()) {
                                    view.requestFocus();
                                }
                            }
                            return false;
                        }
                    });

                    webview.getSettings().setLoadWithOverviewMode(true);
                    webview.getSettings().setDomStorageEnabled(true);
                    webview.getSettings().setUseWideViewPort(true);
                    webview.getSettings().setBuiltInZoomControls(true);

                    final WebviewHelper helper = new WebviewHelper(intent);

                    try {
                        final String startUrl = helper.getStartUrl();

                        final String stopRedirect = helper.getRedirectUrl();
                        webview.setWebViewClient(new CustomWebViewClient(helper, stopRedirect, requestCode));
                        webview.post(new Runnable() {
                            @Override
                            public void run() {
                                webview.loadUrl(startUrl);
                            }
                        });

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    
                    builder.setMessage("Example usage").setView(webviewInDialog).setCancelable(true);
                    mDialog = builder.create();
                    mDialog.show();                    
                }
            });            
        }

        class CustomWebViewClient extends WebViewClient {
            private String mRedirect;

            private WebviewHelper mWebviewHelper;

            private int mRequestCode;

            CustomWebViewClient(WebviewHelper helper, String redirect, int requestCode) {
                mWebviewHelper = helper;
                mRedirect = redirect;
                mRequestCode = requestCode;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(mRedirect)) {
                    Intent resultIntent = mWebviewHelper.getResultIntent(url);
                    mAuthContext.onActivityResult(mRequestCode,
                            AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, resultIntent);
                    view.stopLoading();
                    return true;
                }
                return false;
            }
        }
    }

    private void clearSessionCookie() {
        // Webview by default does not clear session cookies even after app is
        // closed(Bug in Webview).
        // Example to clean session cookie
        Logger.v(TAG, "Clear session cookies");
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();
        CookieSyncManager.getInstance().sync();
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
            new RequestTask(Constants.SERVICE_URL, mResult.getAccessToken()).execute();
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

    private String getUserId() {
        if (mResult != null && mResult.getUserInfo() != null
                && mResult.getUserInfo().getUserId() != null) {
            return mResult.getUserInfo().getUserId();
        }

        return null;
    }

    private String getUserLoginHint() {
        return mEditText.getText().toString();
    }

    /**
     * Simple get request for test
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
                getRequest.addHeader(AUTHORIZATION_HEADER, AUTHORIZATION_HEADER_BEARER + mToken);
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
