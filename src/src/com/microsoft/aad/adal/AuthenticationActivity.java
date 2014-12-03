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

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.DigestException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.gson.Gson;
import com.microsoft.aad.adal.ChallangeResponseBuilder.ChallangeResponse;

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

        if (mAuthRequest.getResource() == null || mAuthRequest.getResource().isEmpty()) {
            returnError(ADALError.ARGUMENT_EXCEPTION,
                    AuthenticationConstants.Broker.ACCOUNT_RESOURCE);
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
        Log.d(TAG, "OnCreate redirectUrl:" + mRedirectUrl);
        setupWebView();
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
        
        if (isBrokerRequest(getIntent())) {
            // This activity is started from calling app and running in
            // Authenticator's process
            Logger.v(TAG, "It is a broker request");
            mCallingPackage = getCallingPackage();
            if (mCallingPackage == null) {
                Log.d(TAG, "startActivityForResult is not used to call this activity");
                Intent resultIntent = new Intent();
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                        AuthenticationConstants.Browser.WEBVIEW_INVALID_REQUEST);
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                        "startActivityForResult is not used to call this activity");
                returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
                return;
            }
            
            mAccountAuthenticatorResponse = getIntent().getParcelableExtra(
                    AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            if (mAccountAuthenticatorResponse != null) {
                mAccountAuthenticatorResponse.onRequestContinued();
            }
            PackageHelper info = new PackageHelper(AuthenticationActivity.this);
            mCallingPackage = getCallingPackage();
            mCallingUID = info.getUIDForPackage(mCallingPackage);
            String signatureDigest = info.getCurrentSignatureForPackage(mCallingPackage);
            mStartUrl = getBrokerStartUrl(mStartUrl, mCallingPackage, signatureDigest);

            if (!isCallerBrokerInstaller()) {
                Logger.v(TAG, "Caller needs to be verified using special redirectUri");
                mRedirectUrl = PackageHelper.getBrokerRedirectUrl(mCallingPackage, signatureDigest);
            }
            Logger.v(TAG,
                    "OnCreate redirectUrl:" + mRedirectUrl + " startUrl:" + mStartUrl
                            + " calling package:" + mCallingPackage + " signatureDigest:"
                            + signatureDigest + " current Context Package: " + getPackageName()
                            + " accountName:" + mAuthRequest.getBrokerAccountName() + " loginHint:"
                            + mAuthRequest.getLoginHint());
        }
        mRegisterReceiver = false;
        final String postUrl = mStartUrl;
        Logger.v(TAG, "OnCreate startUrl:" + mStartUrl + " calling package:" + mCallingPackage
                + " loginHint:" + mAuthRequest.getLoginHint());

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
            Logger.d(TAG, "Reuse webview");
        }
    }

    private boolean isCallerBrokerInstaller() {
        // Allow intune's signature check
        PackageHelper info = new PackageHelper(AuthenticationActivity.this);
        String packageName = getCallingPackage();
        if (!StringExtensions.IsNullOrBlank(packageName)) {
            
            if (packageName.equals(AuthenticationSettings.INSTANCE.getBrokerPackageName())) {
                Logger.v(TAG, "isCallerBrokerInstaller: same package as broker " + packageName);
                return true;
            }
            
            String signature = info.getCurrentSignatureForPackage(packageName);
            Logger.v(TAG, "isCallerBrokerInstaller: Check signature for " + packageName
                    + " signature:" + signature + " brokerSignature:"
                    + AuthenticationSettings.INSTANCE.getBrokerSignature());
            return signature.equals(AuthenticationSettings.INSTANCE.getBrokerSignature()) || 
                    signature.equals(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_SIGNATURE);
        }

        return false;
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

    private void setupWebView() {

        // Create the Web View to show the page
        mWebView = (WebView)findViewById(this.getResources().getIdentifier("webView1", "id",
                this.getPackageName()));
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
            String resource = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_RESOURCE);
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
            authRequest = new AuthenticationRequest(authority, resource, clientidKey, redirect,
                    loginhint, correlationIdParsed);
            authRequest.setBrokerAccountName(accountName);
            authRequest.setPrompt(promptBehavior);
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
        // manager since activity is not visible
        if (mReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }
        mRegisterReceiver = true;

        if (mSpinner != null) {
            Logger.d(TAG, "Spinner at onPause will dismiss");
            mSpinner.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TAG, "onResume");
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
        Logger.d(TAG, "AuthenticationActivity onRestart");
        super.onRestart();
        mRegisterReceiver = true;
    }

    @Override
    public void onBackPressed() {
        Logger.d(TAG, "Back button is pressed");
        
        // User should be able to click back button to cancel in case pkeyauth happen.
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

    class CustomWebViewClient extends WebViewClient {

        private static final String BLANK_PAGE = "about:blank";

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Logger.d(TAG, "shouldOverrideUrlLoading:url=" + url);
            displaySpinner(true);
            if (url.startsWith(AuthenticationConstants.Broker.CLIENT_TLS_REDIRECT)) {
                Logger.v(TAG, "Webview detected request for client certificate");
                view.stopLoading();
                // avoid main thread locking
                mPkeyAuthRedirect = true;
                final String challangeUrl = url;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ChallangeResponseBuilder certHandler = new ChallangeResponseBuilder(
                                    mJWSBuilder);
                            final ChallangeResponse challangeResponse = certHandler
                                    .getChallangeResponseFromUri(challangeUrl);
                            final HashMap<String, String> headers = new HashMap<String, String>();
                            headers.put(AuthenticationConstants.Broker.CHALLANGE_RESPONSE_HEADER,
                                    challangeResponse.mAuthorizationHeaderValue);
                            mWebView.post(new Runnable() {

                                @Override
                                public void run() {
                                    String loadUrl = challangeResponse.mSubmitUrl;
                                    HashMap<String, String> parameters = StringExtensions
                                            .getUrlParameters(challangeResponse.mSubmitUrl);
                                    Logger.v(TAG, "SubmitUrl:" + challangeResponse.mSubmitUrl);
                                    if (!parameters
                                            .containsKey(AuthenticationConstants.OAuth2.CLIENT_ID)) {
                                        loadUrl = loadUrl + "?" + mQueryParameters;
                                    }
                                    Logger.v(TAG, "Loadurl:" + loadUrl);
                                    mWebView.loadUrl(loadUrl, headers);
                                }
                            });
                        } catch (IllegalArgumentException e) {
                            Logger.e(TAG, "Argument exception", e.getMessage(),
                                    ADALError.ARGUMENT_EXCEPTION, e);
                            // It should return error code and finish the
                            // activity, so that onActivityResult implementation
                            // returns errors to callback.
                            returnAuthenticationException(new AuthenticationException(
                                    ADALError.ARGUMENT_EXCEPTION, e.getMessage(), e));
                        } catch (AuthenticationException e) {
                            Logger.e(TAG, "It is failed to create device certificate response",
                                    e.getMessage(), ADALError.DEVICE_CERTIFICATE_RESPONSE_FAILED, e);
                            // It should return error code and finish the
                            // activity, so that onActivityResult implementation
                            // returns errors to callback.
                            returnAuthenticationException(e);
                        }
                    }
                }).start();

                return true;
            } else if (url.toLowerCase(Locale.US).startsWith(mRedirectUrl.toLowerCase(Locale.US))) {
                Logger.v(TAG, "Webview reached redirecturl");
                if (!isBrokerRequest(getIntent())) {
                    // It is pointing to redirect. Final url can be processed to
                    // get the code or error.
                    Logger.v(TAG, "It is not a broker request");
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, url);
                    resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                            mAuthRequest);
                    returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE,
                            resultIntent);
                    view.stopLoading();
                    return true;
                } else {
                    Logger.v(TAG, "It is a broker request");
                    displaySpinnerWithMessage(AuthenticationActivity.this
                            .getText(AuthenticationActivity.this.getResources().getIdentifier(
                                    "broker_processing", "string", getPackageName())));

                    view.stopLoading();

                    // do async task and show spinner while exchanging code for
                    // access token
                    new TokenTask(mWebRequestHandler, mAuthRequest, mCallingPackage, mCallingUID)
                            .execute(url);
                    return true;
                }
            } else if (url.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX)) {
                Logger.v(TAG, "It is an external website request");
                openLinkInBrowser(url);
                view.stopLoading();
                return true;
            }

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

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            displaySpinner(false);
            Logger.e(TAG, "Webview received an error. Errorcode:" + errorCode + " " + description,
                    "", ADALError.ERROR_WEBVIEW);
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                    "Error Code:" + errorCode);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                    description);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
            returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
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
            returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Logger.v(TAG, "Page finished:" + url);

            /*
             * Once web view is fully loaded,set to visible
             */
            mWebView.setVisibility(View.VISIBLE);
            if (!url.startsWith(BLANK_PAGE)) {
                displaySpinner(false);
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            displaySpinner(true);
        }

        private void openLinkInBrowser(String url) {
            String link = url
                    .replace(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, "https://");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            startActivity(intent);
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
            Logger.d(TAG, "displaySpinner:" + show + " showing:" + mSpinner.isShowing());
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

    /**
     * Processes the authorization code to get token and stores inside the
     * Account before returning back to the calling app. App does not receive
     * refresh tokens. Calling app does not have access to
     * setUserData/getUserData inside the AccountManager. This is used only for
     * broker related call.
     */
    class TokenTask extends AsyncTask<String, String, TokenTaskResult> {

        String mPackageName;

        int mAppCallingUID;

        AuthenticationRequest mRequest;

        AccountManager mAccountManager;

        IWebRequestHandler mRequestHandler;

        public TokenTask() {
        }

        public TokenTask(IWebRequestHandler webHandler, final AuthenticationRequest request,
                final String packagename, final int callingUID) {
            mRequestHandler = webHandler;
            mRequest = request;
            mPackageName = packagename;
            mAppCallingUID = callingUID;
            mAccountManager = AccountManager.get(AuthenticationActivity.this);
        }

        @Override
        protected TokenTaskResult doInBackground(String... urlItems) {
            Oauth2 oauthRequest = new Oauth2(mRequest, mRequestHandler, mJWSBuilder);
            TokenTaskResult result = new TokenTaskResult();
            try {
                result.taskResult = oauthRequest.getToken(urlItems[0]);
                Logger.v(TAG, "TokenTask processed the result. " + mRequest.getLogInfo());
            } catch (Exception exc) {
                Logger.e(TAG, "Error in processing code to get a token. " + mRequest.getLogInfo(),
                        "Request url:" + urlItems[0],
                        ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN, exc);
                result.taskException = exc;
            }

            if (result != null && result.taskResult != null
                    && result.taskResult.getAccessToken() != null) {
                Logger.v(TAG, "Setting account:" + mRequest.getLogInfo());

                // Record account in the AccountManager service
                try {
                    setAccount(result);
                } catch (Exception exc) {
                    Logger.e(TAG, "Error in setting the account" + mRequest.getLogInfo(), "",
                            ADALError.BROKER_ACCOUNT_SAVE_FAILED, exc);
                    result.taskException = exc;
                }
            }

            return result;
        }

        private String getBrokerAppCacheKey(StorageHelper cryptoHelper, String cacheKey)
                throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException,
                InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException,
                NoSuchPaddingException, IOException {
            // include UID in the key for broker to store caches for different
            // apps under same account entry
            String digestKey = StringExtensions
                    .createHash(AuthenticationConstants.Broker.USERDATA_UID_KEY + mAppCallingUID
                            + cacheKey);
            Logger.d(TAG, "Cache key original:" + cacheKey + " digestKey:" + digestKey
                    + " calling app UID:" + mAppCallingUID);
            return digestKey;
        }

        private void appendAppUIDToAccount(StorageHelper cryptoHelper, Account account)
                throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException,
                InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException,
                NoSuchPaddingException, IOException, KeyStoreException, CertificateException,
                NoSuchProviderException, UnrecoverableEntryException, DigestException {
            String appIdList = mAccountManager.getUserData(account,
                    AuthenticationConstants.Broker.ACCOUNT_UID_CACHES);
            if (appIdList == null) {
                appIdList = "";
            } else {
                appIdList = cryptoHelper.decrypt(appIdList);
            }
            Logger.v(TAG, "Add calling UID:" + mAppCallingUID);
            if (!appIdList.contains(AuthenticationConstants.Broker.USERDATA_UID_KEY
                    + mAppCallingUID)) {
                Logger.v(TAG, "Account has new calling UID:" + mAppCallingUID);
                mAccountManager
                        .setUserData(
                                account,
                                AuthenticationConstants.Broker.ACCOUNT_UID_CACHES,
                                cryptoHelper.encrypt(appIdList
                                        + AuthenticationConstants.Broker.USERDATA_UID_KEY
                                        + mAppCallingUID));
            }
        }

        private void setAccount(final TokenTaskResult result) throws InvalidKeyException,
                InvalidKeySpecException, InvalidAlgorithmParameterException,
                IllegalBlockSizeException, BadPaddingException, IOException {
            // TODO Add token logging
            // TODO update for new cache logic

            // Authenticator sets the account here and stores the tokens.
            try {
                String name = mRequest.getBrokerAccountName();
                Account[] accountList = mAccountManager
                        .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);

                if (accountList == null || accountList.length != 1) {
                    result.taskResult = null;
                    result.taskException = new AuthenticationException(
                            ADALError.BROKER_SINGLE_USER_EXPECTED);
                    return;
                }

                // Single user in authenticator is already created.
                // This is only registering UID for the app
                if (result.taskResult.getUserInfo() == null
                        || StringExtensions.IsNullOrBlank(result.taskResult.getUserInfo()
                                .getUserId())) {
                    // return userid in the userinfo and use only account name
                    // for all fields
                    Logger.v(TAG, "Set userinfo from account");
                    result.taskResult.setUserInfo(new UserInfo(name, name, "", "", name));
                    mRequest.setLoginHint(name);
                }

                result.accountName = name;
                Logger.v(TAG, "Setting account. Account name: " + name + " package:"
                        + mCallingPackage + " calling app UID:" + mAppCallingUID);
                Account newaccount = accountList[0];

                // Cache logic will be changed based on latest logic
                // This is currently keeping accesstoken and MRRT separate
                // Encrypted Results are saved to AccountManager Service
                // sqllite database. Only Authenticator and similar UID can
                // access.
                Gson gson = new Gson();
                Logger.v(TAG, "app context:" + getApplicationContext().getPackageName()
                        + " context:" + AuthenticationActivity.this.getPackageName()
                        + " calling packagename:" + getCallingPackage());
                StorageHelper cryptoHelper = new StorageHelper(getApplicationContext());
                TokenCacheItem item = new TokenCacheItem(mRequest, result.taskResult, false);
                String json = gson.toJson(item);
                String encrypted = cryptoHelper.encrypt(json);

                // Single user and cache is stored per account
                String key = CacheKey.createCacheKey(mRequest, null);
                saveCacheKey(key, newaccount, mAppCallingUID);
                mAccountManager.setUserData(newaccount, getBrokerAppCacheKey(cryptoHelper, key),
                        encrypted);

                if (result.taskResult.getIsMultiResourceRefreshToken()) {
                    // ADAL stores MRRT refresh token separately
                    TokenCacheItem itemMRRT = new TokenCacheItem(mRequest, result.taskResult, true);
                    json = gson.toJson(itemMRRT);
                    encrypted = cryptoHelper.encrypt(json);
                    key = CacheKey.createMultiResourceRefreshTokenKey(mRequest, null);
                    saveCacheKey(key, newaccount, mAppCallingUID);
                    mAccountManager.setUserData(newaccount,
                            getBrokerAppCacheKey(cryptoHelper, key), encrypted);
                }

                // Record calling UID for this account so that app can get token
                // in the background call without requiring server side
                // validation
                Logger.v(TAG, "Set calling uid:" + mAppCallingUID);
                appendAppUIDToAccount(cryptoHelper, newaccount);
            } catch (NoSuchAlgorithmException e) {
                Logger.e(TAG, "Algorithm does not exist in the device", "",
                        ADALError.DEVICE_CACHE_IS_NOT_WORKING, e);
                result.taskException = e;
            } catch (NoSuchPaddingException e) {
                Logger.e(TAG, "Padding type does not exist in the device", "",
                        ADALError.DEVICE_CACHE_IS_NOT_WORKING, e);
                result.taskException = e;
            } catch (KeyStoreException e) {
                Logger.e(TAG, "Key store type is not supported", "",
                        ADALError.DEVICE_CACHE_IS_NOT_WORKING, e);
                result.taskException = e;
            } catch (CertificateException e) {
                Logger.e(TAG, "Certificate exception", "", ADALError.DEVICE_CACHE_IS_NOT_WORKING, e);
                result.taskException = e;
            } catch (NoSuchProviderException e) {
                Logger.e(TAG, "Requested security provider does not exists in the device", "",
                        ADALError.DEVICE_CACHE_IS_NOT_WORKING, e);
                result.taskException = e;
            } catch (UnrecoverableEntryException e) {
                Logger.e(TAG, "Key entry is not recoverable", "",
                        ADALError.DEVICE_CACHE_IS_NOT_WORKING, e);
                result.taskException = e;
            } catch (DigestException e) {
                Logger.e(TAG, "Digest is not valid", "", ADALError.DEVICE_CACHE_IS_NOT_WORKING, e);
                result.taskException = e;
            }
        }

        private void saveCacheKey(String key, Account cacheAccount, int callingUID) {
            Logger.d(TAG, "Get CacheKeys for account");
            // Store cachekeys for each UID
            // Activity has access to packagename and UID, but background call
            // in getAuthToken only knows about UID
            String keylist = mAccountManager.getUserData(cacheAccount,
                    AuthenticationConstants.Broker.USERDATA_CALLER_CACHEKEYS + callingUID);
            if (keylist == null) {
                keylist = "";
            }
            if (!keylist.contains(AuthenticationConstants.Broker.CALLER_CACHEKEY_PREFIX + key)) {
                Logger.v(TAG, "Account does not have this cache key:" + key
                        + " It will save it to accoun for the callerUID:" + callingUID);
                keylist += AuthenticationConstants.Broker.CALLER_CACHEKEY_PREFIX + key;
                mAccountManager.setUserData(cacheAccount,
                        AuthenticationConstants.Broker.USERDATA_CALLER_CACHEKEYS + callingUID,
                        keylist);
                Logger.v(TAG, "keylist:" + keylist);
            }
        }

        @Override
        protected void onPostExecute(TokenTaskResult result) {
            Logger.v(TAG, "Token task returns the result");
            displaySpinner(false);
            Intent intent = new Intent();
            if (result.taskResult != null) {
                intent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, mWaitingRequestId);
                intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN,
                        result.taskResult.getAccessToken());
                intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_NAME, result.accountName);
                intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE,
                        result.taskResult.getExpiresOn().getTime());
                if (result.taskResult.getUserInfo() != null) {
                    intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID,
                            result.taskResult.getUserInfo().getUserId());
                    intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME,
                            result.taskResult.getUserInfo().getGivenName());
                    intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME,
                            result.taskResult.getUserInfo().getFamilyName());
                    intent.putExtra(
                            AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER,
                            result.taskResult.getUserInfo().getIdentityProvider());
                    intent.putExtra(
                            AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE,
                            result.taskResult.getUserInfo().getDisplayableId());
                }
                returnResult(AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE, intent);
            } else {
                returnError(ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN,
                        result.taskException.getMessage());
            }
        }
    }

    class TokenTaskResult {
        AuthenticationResult taskResult;

        Exception taskException;

        String accountName;
    }
}
