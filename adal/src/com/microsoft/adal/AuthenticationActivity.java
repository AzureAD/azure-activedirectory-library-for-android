
package com.microsoft.adal;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
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

    private boolean mRestartWebview = false;

    private WebView wv;

    private String mStartUrl;

    private ProgressDialog spinner;

    private String redirectUrl;

    private AuthenticationRequest mAuthRequest;

    /**
     * JavascriptInterface to report page content in errors
     */
    private JavaScriptInterface mScriptInterface;

    // Broadcast receiver for cancel
    private ActivityBroadcastReceiver mReceiver = null;

    // Broadcast receiver is needed to cancel outstanding AuthenticationActivity
    // for this AuthenticationContext since each instance of context can have
    // one active activity
    private class ActivityBroadcastReceiver extends android.content.BroadcastReceiver {

        private int mWaitingRequestId = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.v(TAG, "ActivityBroadcastReceiver onReceive");

            if (intent.getAction().equalsIgnoreCase(AuthenticationConstants.Browser.ACTION_CANCEL)) {
                try {
                    Logger.v(TAG,
                            "ActivityBroadcastReceiver onReceive action is for cancelling Authentication Activity");

                    int cancelRequestId = intent.getIntExtra(
                            AuthenticationConstants.Browser.REQUEST_ID, 0);

                    if (cancelRequestId == mWaitingRequestId) {
                        Logger.v(TAG, "Waiting requestId is same and cancelling this activity");
                        AuthenticationActivity.this.finish();
                        // no need to send result back to activity. It is
                        // cancelled
                        // and callback will be called after this request.
                    }
                } catch (Exception ex) {
                    Logger.e(TAG, "ActivityBroadcastReceiver onReceive exception",
                            ExceptionExtensions.getExceptionMessage(ex),
                            ADALError.BROADCAST_RECEIVER_ERROR);
                }
            }
        }
    }

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
        mScriptInterface = new JavaScriptInterface();
        wv.addJavascriptInterface(mScriptInterface, "ScriptInterface");
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
        wv.setWebViewClient(new CustomWebViewClient());
        wv.setVisibility(View.INVISIBLE);
        Logger.v(TAG, "User agent:" + wv.getSettings().getUserAgentString());
        mStartUrl = "about:blank";

        try {
            mStartUrl = new Oauth2(mAuthRequest, null).getCodeRequestUrl();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());

            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
        }

        final String postUrl = mStartUrl;

        wv.post(new Runnable() {
            @Override
            public void run() {
                wv.loadUrl("about:blank");// load blank first
                wv.loadUrl(postUrl);
            }
        });

        // Create the broadcast receiver for cancel
        Logger.v(TAG, "Init broadcastReceiver with requestId:" + mAuthRequest.getRequestId());
        mReceiver = new ActivityBroadcastReceiver();
        mReceiver.mWaitingRequestId = mAuthRequest.getRequestId();
        mRestartWebview = false;
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(AuthenticationConstants.Browser.ACTION_CANCEL));
    }

    /**
     * activity sets result to go back to the caller
     * 
     * @param resultCode
     * @param data
     */
    private void ReturnToCaller(int resultCode, Intent data) {
        Logger.d(TAG, "Return To Caller:" + resultCode);
        displaySpinner(false);

        if (data == null) {
            data = new Intent();
        }

        if (mAuthRequest != null) {
            // set request id related to this response to send the delegateId
            Logger.d(TAG, "Return To Caller REQUEST_ID:" + mAuthRequest.getRequestId());
            data.putExtra(AuthenticationConstants.Browser.REQUEST_ID, mAuthRequest.getRequestId());
        } else {
            Logger.w(TAG, "Request object is null", "",
                    ADALError.ACTIVITY_REQUEST_INTENT_DATA_IS_NULL);
        }

        setResult(resultCode, data);
        this.finish();
    }

    @Override
    protected void onPause() {
        Logger.d(TAG, "AuthenticationActivity onPause unregister receiver");
        super.onPause();

        // Unregister the cancel action listener from the local broadcast
        // manager
        // since activity is not visible
        if (mReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }
        mRestartWebview = true;
        // restart webview when it comes back from onresume
    }

    @Override
    protected void onStart() {
        Logger.d(TAG, "AuthenticationActivity onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // It can come here from onCreate,onRestart or onPause. It
        // will post the url again since webview could not start at the middle
        // of redirect url.
        // If it reaches the final url, it will set result back to caller.
        if (mRestartWebview) {
            Logger.v(TAG, "Webview onResume will post start url again:" + mStartUrl);
            final String postUrl = mStartUrl;

            if (mReceiver != null) {
                Logger.v(TAG, "Webview onResume register broadcast receiver for requestId"
                        + mReceiver.mWaitingRequestId);
                LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                        new IntentFilter(AuthenticationConstants.Browser.ACTION_CANCEL));
            }

            wv.post(new Runnable() {
                @Override
                public void run() {
                    wv.loadUrl("about:blank");// load blank first
                    wv.loadUrl(postUrl);
                }
            });
        }
        mRestartWebview = false;
    }

    @Override
    protected void onRestart() {
        Logger.d(TAG, "AuthenticationActivity onRestart");
        super.onRestart();
        mRestartWebview = true;
    }

    @Override
    protected void onStop() {
        // Called when you are no longer visible to the user.
        Logger.d(TAG, "AuthenticationActivity onStop");
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        Logger.d(TAG, "Back button is pressed");

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
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
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

    /**
     * javascript injection to the loaded page to retrieve content
     */
    private class JavaScriptInterface {
        String mHtml;

        @JavascriptInterface
        public void setContent(String html) {
            mHtml = html;
        }

        public String getContent() {
            return mHtml;
        }
    }

    private class CustomWebViewClient extends WebViewClient {

        private void loadContent(WebView view) {
            // Get page content to report
            // Load page content
            wv.loadUrl("javascript:window.ScriptInterface.setContent('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
        }

        private void reportContent(WebView view) {
            loadContent(view);
            if (mScriptInterface != null
                    && !StringExtensions.IsNullOrBlank(mScriptInterface.getContent())) {
                Logger.v(TAG, "Webview content:" + mScriptInterface.getContent());
            }
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            Logger.d(TAG, "shouldOverrideUrlLoading:url=" + url);
            displaySpinner(true);

            if (url.startsWith(redirectUrl)) {
                Logger.v(TAG, "Webview reached redirecturl");

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
            displaySpinner(false);
            Logger.e(TAG, "Webview received an error. Errorcode:" + errorCode + " " + description,
                    "", ADALError.ERROR_WEBVIEW);
            reportContent(view);
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                    "Error Code:" + errorCode);
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
            displaySpinner(false);
            handler.cancel();
            Logger.e(TAG, "Received ssl error", "", ADALError.ERROR_FAILED_SSL_HANDSHAKE);

            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE, "Code:"
                    + ERROR_FAILED_SSL_HANDSHAKE);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                    error.toString());
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);

        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Logger.v(TAG, "Page started:" + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Logger.v(TAG, "Page finished:" + url);
            displaySpinner(false);

            /*
             * Once web view is fully loaded,set to visible
             */
            wv.setVisibility(View.VISIBLE);

            // Load page content to use in reporting errors
            loadContent(wv);
        }
    }

    /**
     * handle spinner display
     * 
     * @param show
     */
    private void displaySpinner(boolean show) {
        if (!AuthenticationActivity.this.isFinishing() && spinner != null) {
            if (show && !spinner.isShowing()) {
                spinner.show();
            }

            if (!show && spinner.isShowing()) {
                spinner.dismiss();
            }
        }
    }
}
