
package com.microsoft.adal;

import java.io.Serializable;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

/**
 * activity to launch webview for authentication
 * 
 * @author omercan
 */
public class AuthenticationActivity extends Activity {

    private final String TAG = "AuthenticationActivity";

    private Button btnCancel;

    private WebView wv;

    private ProgressDialog spinner;

    private String redirectUrl;

    private AuthenticationRequest mAuthRequest;

    // TODO move error codes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        // Get the message from the intent
        Intent intent = getIntent();
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

        if (!(request instanceof AuthenticationRequest)) {

            Log.d(TAG, "Request item is null, so it returns to caller");
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                    AuthenticationConstants.Browser.WEBVIEW_INVALID_REQUEST);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                    "Intent does not have request details");
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
            return;
        } else {
            mAuthRequest = (AuthenticationRequest)request;
        }
        
        redirectUrl = mAuthRequest.getRedirectUri();
        Log.d(TAG, "OnCreate redirect" + redirectUrl);

        // cancel action will send the request back to onActivityResult method
        btnCancel = (Button)findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                confirmCancelRequest();
            }
        });

        // Spinner dialog to show some message while it is loading
        spinner = new ProgressDialog(this);
        spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        spinner.setMessage(this.getText(R.string.app_loading));
        spinner.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                confirmCancelRequest();
            }
        });

        // Create the Web View to show the page
        wv = (WebView)findViewById(R.id.webView1);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.requestFocus(View.FOCUS_DOWN);

        // Set focus to the view for touch event
        wv.setOnTouchListener(new View.OnTouchListener() {
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

        wv.getSettings().setLoadWithOverviewMode(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setUseWideViewPort(true);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.getSettings().setPluginState(WebSettings.PluginState.ON);
        wv.setWebViewClient(new CustomWebViewClient());
        wv.setVisibility(View.INVISIBLE);

        String startUrl = "about:blank";

        try {
            startUrl = new Oauth2(mAuthRequest, null).getCodeRequestUrl();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());

            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
        }

        final String postUrl = startUrl;

        wv.post(new Runnable() {
            @Override
            public void run() {
                wv.loadUrl("about:blank");// load blank first
                wv.loadUrl(postUrl);
            }
        });

    }

    /**
     * activity sets result to go back to the caller
     * 
     * @param resultCode
     * @param data
     */
    private void ReturnToCaller(int resultCode, Intent data) {
        Log.d(TAG, "ReturnToCaller=" + resultCode);
        setResult(resultCode, data);
        this.finish();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button is pressed");

        // Ask user if they rally want to cancel the flow, if navigation is not
        // possible
        // User may navigated to another page and does not need confirmation to
        // go back to previous page.
        if (!wv.canGoBack()) {
            confirmCancelRequest();
        } else {
            // Don't use default back pressed action, since user can go back in
            // webview
            wv.goBack();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    private void confirmCancelRequest() {
        new AlertDialog.Builder(AuthenticationActivity.this)
                .setTitle(
                        AuthenticationActivity.this
                                .getString(R.string.title_confirmation_activity_authentication))
                .setMessage(
                        AuthenticationActivity.this
                                .getString(R.string.confirmation_activity_authentication))
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        Intent resultIntent = new Intent();
                        ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL,
                                resultIntent);
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
                Log.i(TAG, "Webview reached redirecturl");

                // It is pointing to redirect. Final url can be processed to get
                // the code or error.
                Intent resultIntent = new Intent();
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, url);
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                        mAuthRequest);
                ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE,
                        resultIntent);
                view.stopLoading();
                return true;
            }

            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            Log.e(TAG, "Webview received an error. Errorcode:" + errorCode + " " + description);
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE, errorCode);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                    description);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // Developer does not have option to control this for now
            super.onReceivedSslError(view, handler, error);
            handler.cancel();
            Log.e(TAG, "Received ssl error");

            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                    ERROR_FAILED_SSL_HANDSHAKE);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                    error.toString());
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);

        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {

            super.onPageStarted(view, url, favicon);
            Log.d(TAG, "Page started:" + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(TAG, "Page finished:" + url);
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
