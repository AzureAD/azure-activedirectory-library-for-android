/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

/**
 * TODO: overwrite webview functions TODO: when url is reached call the complete
 * to "setresult" for the calling activity Activity will launch webview that is
 * defined static. It will set the result back to calling activity.
 * 
 * @author omercan
 */
public class LoginActivity extends Activity {

    private final String TAG = "com.microsoft.adal.LoginActivity";
    private Button btnCancel;
    private WebView wv;
    private ProgressDialog spinner;
    private String redirectUrl;
    private AuthenticationRequest mAuthRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Get the message from the intent
        Intent intent = getIntent();
        mAuthRequest = (AuthenticationRequest) intent
                .getSerializableExtra(AuthenticationConstants.BROWSER_REQUEST_MESSAGE);
        redirectUrl = mAuthRequest.getRedirectUri();
        Log.d(TAG, "OnCreate redirect"+redirectUrl);
        
        // cancel action will send the request back to onActivityResult method
        btnCancel = (Button) findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                confirmCancelRequest();
            }
        });

        spinner = new ProgressDialog(this);
        spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        spinner.setMessage(this.getText(R.string.app_loading));
        spinner.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                confirmCancelRequest();
            }
        });

        spinner.show();
        
        // Create the Web View to show the login page
        wv = (WebView) findViewById(R.id.webView1);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.requestFocus(View.FOCUS_DOWN);

        // Set focus to the view for touch event
        wv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN
                        || action == MotionEvent.ACTION_UP) {
                    if (!view.hasFocus()) {
                        view.requestFocus();
                    }
                }
                return false;
            }
        });

        wv.getSettings().setLoadWithOverviewMode(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setUseWideViewPort(true);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.getSettings().setPluginState(WebSettings.PluginState.ON);
        wv.getSettings().setPluginsEnabled(true);
        wv.setWebViewClient(new CustomWebViewClient());

//        wv.setVisibility(View.INVISIBLE);

        final Activity currentActivity = this;
        String startUrl = "about:blank";
        
        try {
            startUrl = mAuthRequest.getCodeRequestUrl();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());

            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_REQUEST_INFO, mAuthRequest);
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
        }

        final String postUrl = startUrl;
        wv.post( new Runnable()
        {
            @Override
            public void run()
            {
                wv.loadUrl( postUrl );
            }
        } );

    }

    private void ReturnToCaller(int resultCode, Intent data)
    {
        Log.d(TAG, "ReturnToCaller=" + resultCode);
        setResult(resultCode, data);
        this.finish();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button is pressed");
        
        // Ask user if they rally want to cancel the flow
        if(!wv.canGoBack())
        {
            confirmCancelRequest();   
        }
        else
        {
            // Don't use default back pressed action, since user can go back in webview
            // super.onBackPressed sets the result.
            wv.goBack();
        }
        
    }
        
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    
    private void confirmCancelRequest()
    {
        new AlertDialog.Builder(LoginActivity.this)
        .setTitle("Confirm?")
        .setMessage("Do you want to cancel the login?")
        .setNegativeButton(android.R.string.no, null)  
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int arg1) {
                Intent resultIntent = new Intent();        
                ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, resultIntent);
            }
        }).create().show();
    }
    
    private class CustomWebViewClient extends WebViewClient {
        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            Log.d(TAG, "shouldOverrideUrlLoading:url=" + url);
            if (spinner != null && !spinner.isShowing()) {
                spinner.show();
            }
            
            if (url.startsWith(redirectUrl)) {
                Log.d(TAG, "shouldOverrideUrlLoading: reached redirect");
                Intent resultIntent = new Intent();
                resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_FINAL_URL, url);
                resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_REQUEST_INFO, mAuthRequest);
                ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, resultIntent);
                view.stopLoading();
                return true;
            }

            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_ERROR_CODE, errorCode);
            resultIntent
                    .putExtra(AuthenticationConstants.BROWSER_RESPONSE_ERROR_MESSAGE, description);
            resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_REQUEST_INFO, mAuthRequest);
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);

        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (AuthenticationSettings.getInstance().getIgnoreSSLErrors()) {
                handler.proceed();
            } else {
                super.onReceivedSslError(view, handler, error);

                handler.cancel();
                Intent resultIntent = new Intent();
                resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_ERROR_CODE,
                        ERROR_FAILED_SSL_HANDSHAKE);
                resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_ERROR_MESSAGE,
                        error.toString());
                resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_REQUEST_INFO, mAuthRequest);
                ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {

            super.onPageStarted(view, url, favicon);
            Log.d(TAG,"Page started:"+url);
            if (spinner != null && !spinner.isShowing()) {
                spinner.show();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(TAG,"Page finished"+url);
            if (spinner != null && spinner.isShowing()) {
                spinner.dismiss();
            }
            /*
             * Once web view is fully loaded,set to visible
             */
            wv.setVisibility(View.VISIBLE);
        }
    }

}
