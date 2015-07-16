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

package com.microsoft.aad.adal;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

/**
 * Authentication Activity to launch {@link WebView} for authentication.
 */
@SuppressLint({
        "SetJavaScriptEnabled", "ClickableViewAccessibility"
})
public class AuthenticationActivity extends Activity {

    static final int BACK_PRESSED_CANCEL_DIALOG_STEPS = -2;

    private static final String TAG = "AuthenticationActivity";

    private boolean mRegisterReceiver = false;

    private WebView mWebView;

    private String mStartUrl;

    private ProgressDialog mSpinner;

    private String mRedirectUrl;

    private AuthenticationRequest mAuthRequest;

    // Broadcast receiver for cancel
    private ActivityBroadcastReceiver mReceiver = null;

    private String mCallingPackage;

    private int mWaitingRequestId;

    private int mCallingUID;

    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;

    private Bundle mAuthenticatorResultBundle = null;

    private IWebRequestHandler mWebRequestHandler = new WebRequestHandler();

    private IJWSBuilder mJWSBuilder = new JWSBuilder();

    private String mQueryParameters;

    private boolean mPkeyAuthRedirect = false;

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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(this.getResources().getIdentifier("activity_authentication", "layout",
                this.getPackageName()));
        CookieSyncManager.createInstance(getApplicationContext());
        CookieSyncManager.getInstance().sync();
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        // Get the message from the intent
        mAuthRequest = getAuthenticationRequestFromIntent(getIntent());
        if (mAuthRequest == null) {
            Log.d(TAG, "Request item is null, so it returns to caller");
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                    AuthenticationConstants.Browser.WEBVIEW_INVALID_REQUEST);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                    "Intent does not have request details");
            returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
            return;
        }

        if (mAuthRequest.getAuthority() == null || mAuthRequest.getAuthority().isEmpty()) {
            returnError(ADALError.ARGUMENT_EXCEPTION,
                    AuthenticationConstants.Broker.ACCOUNT_AUTHORITY);
            return;
        }

        if (mAuthRequest.getScope() == null || mAuthRequest.getScope().length == 0) {
            returnError(ADALError.ARGUMENT_EXCEPTION, AuthenticationConstants.Broker.ACCOUNT_SCOPE);
            return;
        }

        if (mAuthRequest.getClientId() == null || mAuthRequest.getClientId().isEmpty()) {
            returnError(ADALError.ARGUMENT_EXCEPTION,
                    AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY);
            return;
        }

        if (mAuthRequest.getRedirectUri() == null || mAuthRequest.getRedirectUri().isEmpty()) {
            returnError(ADALError.ARGUMENT_EXCEPTION,
                    AuthenticationConstants.Broker.ACCOUNT_REDIRECT);
            return;
        }

        mRedirectUrl = mAuthRequest.getRedirectUri();
        Logger.v(TAG, "OnCreate redirectUrl:" + mRedirectUrl);
        // Create the Web View to show the page
        mWebView = (WebView)findViewById(this.getResources().getIdentifier("webView1", "id",
                this.getPackageName()));
        Logger.v(TAG, "User agent:" + mWebView.getSettings().getUserAgentString());
        mStartUrl = "about:blank";

        try {
            Oauth2 oauth = new Oauth2(mAuthRequest);
            mStartUrl = oauth.getCodeRequestUrl();
            mQueryParameters = oauth.getAuthorizationEndpointQueryParameters();
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, e.getMessage());
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
            returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
            return;
        }

        // Create the broadcast receiver for cancel
        Logger.v(TAG, "Init broadcastReceiver with requestId:" + mAuthRequest.getRequestId() + " "
                + mAuthRequest.getLogInfo());
        mReceiver = new ActivityBroadcastReceiver();
        mReceiver.mWaitingRequestId = mAuthRequest.getRequestId();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(AuthenticationConstants.Browser.ACTION_CANCEL));

        String userAgent = mWebView.getSettings().getUserAgentString();
        mWebView.getSettings().setUserAgentString(
                userAgent + AuthenticationConstants.Broker.CLIENT_TLS_NOT_SUPPORTED);
        userAgent = mWebView.getSettings().getUserAgentString();
        Logger.v(TAG, "UserAgent:" + userAgent);

        mRegisterReceiver = false;
        final String postUrl = mStartUrl;
        Logger.i(TAG, "OnCreate startUrl:" + mStartUrl + " calling package:" + mCallingPackage,
                " device:" + android.os.Build.VERSION.RELEASE + " " + android.os.Build.MANUFACTURER
                        + android.os.Build.MODEL);

        setupWebView(mRedirectUrl, mQueryParameters, mAuthRequest);

        if (savedInstanceState == null) {
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    // load blank first to avoid error for not loading webview
                    mWebView.loadUrl("about:blank");
                    mWebView.loadUrl(postUrl);
                }
            });
        } else {
            Logger.v(TAG, "Reuse webview");
        }
    }
     
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the state of the WebView
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restore the state of the WebView
        mWebView.restoreState(savedInstanceState);
    }

    private void setupWebView(String redirect, String queryParam, AuthenticationRequest request) {

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.requestFocus(View.FOCUS_DOWN);

        // Set focus to the view for touch event
        mWebView.setOnTouchListener(new View.OnTouchListener() {
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

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setWebViewClient(new CustomWebViewClient());
        mWebView.setVisibility(View.INVISIBLE);
    }

    private AuthenticationRequest getAuthenticationRequestFromIntent(Intent callingIntent) {
        AuthenticationRequest authRequest = null;
        if (isBrokerRequest(callingIntent)) {
            Logger.v(TAG, "It is a broker request. Get request info from bundle extras.");
            String authority = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY);
            String[] scope = callingIntent
                    .getStringArrayExtra(AuthenticationConstants.Broker.ACCOUNT_SCOPE);
            String[] additionalScope = callingIntent
                    .getStringArrayExtra(AuthenticationConstants.Broker.ACCOUNT_ADDITONAL_SCOPE);
            String redirect = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_REDIRECT);
            String loginhint = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT);
            String accountName = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_NAME);
            String clientidKey = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY);
            String correlationId = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID);
            String prompt = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT);

            UserIdentifier userId = UserIdentifier.createFromIntent(callingIntent);

            PromptBehavior promptBehavior = PromptBehavior.Auto;
            if (!StringExtensions.IsNullOrBlank(prompt)) {
                promptBehavior = PromptBehavior.valueOf(prompt);
            }

            mWaitingRequestId = callingIntent.getIntExtra(
                    AuthenticationConstants.Browser.REQUEST_ID, 0);
            UUID correlationIdParsed = null;
            if (!StringExtensions.IsNullOrBlank(correlationId)) {
                try {
                    correlationIdParsed = UUID.fromString(correlationId);
                } catch (IllegalArgumentException ex) {
                    correlationIdParsed = null;
                    Logger.e(TAG, "CorrelationId is malformed: " + correlationId, "",
                            ADALError.CORRELATION_ID_FORMAT);
                }
            }

            authRequest = new AuthenticationRequest(authority, scope, clientidKey, redirect,
                    userId, promptBehavior, "", correlationIdParsed);
            authRequest.setBrokerAccountName(accountName);
            authRequest.setPrompt(promptBehavior);
            authRequest.setAdditionalScope(additionalScope);
            authRequest.setRequestId(mWaitingRequestId);
        } else {
            Serializable request = callingIntent
                    .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

            if (request instanceof AuthenticationRequest) {
                authRequest = (AuthenticationRequest)request;
            }
        }
        return authRequest;
    }

    /**
     * Return error to caller and finish this activity.
     */
    private void returnError(ADALError errorCode, String argument) {
        // Set result back to account manager call
        Log.w(TAG, "Argument error:" + argument);
        Intent resultIntent = new Intent();
        // TODO only send adalerror from activity side as int
        resultIntent
                .putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE, errorCode.name());
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE, argument);
        if (mAuthRequest != null) {
            resultIntent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, mWaitingRequestId);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
        }
        this.setResult(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
        this.finish();
    }

    private void returnAuthenticationException(final AuthenticationException e) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION, e);
        if (mAuthRequest != null) {
            resultIntent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, mWaitingRequestId);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
        }
        this.setResult(AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION,
                resultIntent);
        this.finish();
    }

    private String getBrokerStartUrl(String loadUrl, String packageName, String signatureDigest) {
        if (!StringExtensions.IsNullOrBlank(packageName)
                && !StringExtensions.IsNullOrBlank(signatureDigest)) {
            try {
                return loadUrl + "&package_name="
                        + URLEncoder.encode(packageName, AuthenticationConstants.ENCODING_UTF8)
                        + "&signature="
                        + URLEncoder.encode(signatureDigest, AuthenticationConstants.ENCODING_UTF8);
            } catch (UnsupportedEncodingException e) {
                // This encoding issue will happen at the beginning of API call,
                // if it is not supported on this device. ADAL uses one encoding
                // type.
                Log.e(TAG, "Encoding", e);
            }
        }
        return loadUrl;
    }

    private boolean isBrokerRequest(Intent callingIntent) {
        Logger.v(TAG, "Packagename:" + getPackageName() + " Broker packagename:"
                + AuthenticationSettings.INSTANCE.getBrokerPackageName() + " Calling packagename:"
                + getCallingPackage());

        // Intent should have a flag and activity is hosted inside broker
        return callingIntent != null
                && !StringExtensions.IsNullOrBlank(callingIntent
                        .getStringExtra(AuthenticationConstants.Broker.BROKER_REQUEST));
    }

    /**
     * Activity sets result to go back to the caller.
     * 
     * @param resultCode
     * @param data
     */
    private void returnToCaller(int resultCode, Intent data) {
        Logger.v(TAG, "Return To Caller:" + resultCode);
        displaySpinner(false);

        if (data == null) {
            data = new Intent();
        }

        if (mAuthRequest != null) {
            // set request id related to this response to send the delegateId
            Logger.v(TAG, "Return To Caller REQUEST_ID:" + mAuthRequest.getRequestId());
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
        Logger.v(TAG, "AuthenticationActivity onPause unregister receiver");
        super.onPause();

        // Unregister the cancel action listener from the local broadcast
        // manager since activity is not visible
        if (mReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }
        mRegisterReceiver = true;

        if (mSpinner != null) {
            Logger.v(TAG, "Spinner at onPause will dismiss");
            mSpinner.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.v(TAG, "onResume");
        // It can come here from onCreate, onRestart or onPause.
        // Don't load url again since it will send another 2FA request
        if (mRegisterReceiver) {
            Logger.v(TAG, "Webview onResume will register receiver:" + mStartUrl);
            if (mReceiver != null) {
                Logger.v(TAG, "Webview onResume register broadcast receiver for requestId"
                        + mReceiver.mWaitingRequestId);
                LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                        new IntentFilter(AuthenticationConstants.Browser.ACTION_CANCEL));
            }
        }
        mRegisterReceiver = false;

        // Spinner dialog to show some message while it is loading
        mSpinner = new ProgressDialog(this);
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage(this.getText(this.getResources().getIdentifier("app_loading", "string",
                this.getPackageName())));
    }

    @Override
    protected void onRestart() {
        Logger.v(TAG, "AuthenticationActivity onRestart");
        super.onRestart();
        mRegisterReceiver = true;
    }

    @Override
    public void onBackPressed() {
        Logger.v(TAG, "Back button is pressed");

        // User should be able to click back button to cancel in case pkeyauth
        // happen.
        if (mPkeyAuthRedirect || !mWebView.canGoBackOrForward(BACK_PRESSED_CANCEL_DIALOG_STEPS)) {
            // counting blank page as well
            cancelRequest();
        } else {
            // Don't use default back pressed action, since user can go back in
            // webview
            mWebView.goBack();
        }
    }

    private void cancelRequest() {
        Logger.v(TAG, "Sending intent to cancel authentication activity");
        Intent resultIntent = new Intent();
        returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, resultIntent);
    }

    class CustomWebViewClient extends BasicWebViewClient {

        public CustomWebViewClient() {
            super(AuthenticationActivity.this, mRedirectUrl, mQueryParameters, mAuthRequest);
        }

        public void processRedirectUrl(final WebView view, String url) {
            // It is pointing to redirect. Final url can be processed to
            // get the code or error.
            Logger.i(TAG, "It is not a broker request", "");
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, url);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
            returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, resultIntent);
            view.stopLoading();
        }

        public boolean processInvalidUrl(final WebView view, String url) {
            if (isBrokerRequest(getIntent())
                    && url.startsWith(AuthenticationConstants.Broker.REDIRECT_PREFIX)) {
                returnError(ADALError.DEVELOPER_REDIRECTURI_INVALID, String.format(
                        "The RedirectUri is not as expected. Received %s and expected %s", url,
                        mRedirectUrl));
                view.stopLoading();
                return true;
            }

            return false;
        }

        public void showSpinner(boolean status) {
            displaySpinner(status);
        }

        @Override
        public void sendResponse(int returnCode, Intent responseIntent) {
            returnToCaller(returnCode, responseIntent);
        }

        @Override
        public void cancelWebViewRequest() {
            cancelRequest();
        }

        @Override
        public void setPKeyAuthStatus(boolean status) {
            mPkeyAuthRedirect = status;
        }

        @Override
        public void postRunnable(Runnable item) {
            mWebView.post(item);
        }
    }

    /**
     * handle spinner display.
     * 
     * @param show
     */
    private void displaySpinner(boolean show) {
        if (!AuthenticationActivity.this.isFinishing()
                && !AuthenticationActivity.this.isChangingConfigurations() && mSpinner != null) {
            Logger.v(TAG, "displaySpinner:" + show + " showing:" + mSpinner.isShowing());
            if (show && !mSpinner.isShowing()) {
                mSpinner.show();
            }

            if (!show && mSpinner.isShowing()) {
                mSpinner.dismiss();
            }
        }
    }

    private void displaySpinnerWithMessage(CharSequence charSequence) {
        if (!AuthenticationActivity.this.isFinishing() && mSpinner != null) {
            mSpinner.show();
            mSpinner.setMessage(charSequence);
        }
    }

    private void returnResult(int resultcode, Intent intent) {
        // Set result back to account manager call
        this.setAccountAuthenticatorResult(intent.getExtras());
        this.setResult(resultcode, intent);
        this.finish();
    }

    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result
     * isn't present.
     */
    @Override
    public void finish() {
        // Added here to make Authenticator work with one common code base
        if (isBrokerRequest(getIntent()) && mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            Logger.v(TAG, "It is a broker request");
            if (mAuthenticatorResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mAuthenticatorResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }

    /**
     * Set the result that is to be sent as the result of the request that
     * caused this Activity to be launched. If result is null or this method is
     * never called then the request will be canceled.
     * 
     * @param result this is returned as the result of the
     *            AbstractAccountAuthenticator request
     */
    private final void setAccountAuthenticatorResult(Bundle result) {
        mAuthenticatorResultBundle = result;
    }
}
