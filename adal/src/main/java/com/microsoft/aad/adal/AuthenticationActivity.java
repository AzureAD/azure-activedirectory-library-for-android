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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ClientCertRequest;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.microsoft.aad.adal.AuthenticationResult.AuthenticationStatus;
import com.microsoft.identity.common.adal.internal.JWSBuilder;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.adal.internal.net.IWebRequestHandler;
import com.microsoft.identity.common.adal.internal.net.WebRequestHandler;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.UUID;

import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_AUTHORITY;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_NAME;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_PROMPT;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_RESOURCE;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_UID_CACHES;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_SIGNATURE;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.BROKER_REQUEST;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.CALLER_CACHEKEY_PREFIX;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.CLIENT_TLS_NOT_SUPPORTED;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.CliTelemInfo.RT_AGE;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_ERROR;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_SUBERROR;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.CliTelemInfo.SPE_RING;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.REDIRECT_PREFIX;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.REDIRECT_SSL_PREFIX;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.USERDATA_CALLER_CACHEKEYS;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.USERDATA_UID_KEY;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.ACTION_CANCEL;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.REQUEST_ID;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.REQUEST_MESSAGE;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.RESPONSE_ERROR_CODE;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.RESPONSE_FINAL_URL;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.WEBVIEW_INVALID_REQUEST;
import static com.microsoft.aad.adal.AuthenticationConstants.ENCODING_UTF8;
import static com.microsoft.aad.adal.AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME;
import static com.microsoft.aad.adal.AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE;
import static com.microsoft.aad.adal.AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR;
import static com.microsoft.aad.adal.AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE;

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
    private final IWebRequestHandler mWebRequestHandler = new WebRequestHandler();
    private final JWSBuilder mJWSBuilder = new JWSBuilder();
    private boolean mPkeyAuthRedirect = false;
    private StorageHelper mStorageHelper;
    private UIEvent mUIEvent = null;

    // Broadcast receiver is needed to cancel outstanding AuthenticationActivity
    // for this AuthenticationContext since each instance of context can have
    // one active activity
    private class ActivityBroadcastReceiver extends android.content.BroadcastReceiver {

        private int mWaitingRequestId = -1;

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String methodName = ":onReceive";

            Logger.verbose(TAG + methodName, "ActivityBroadcastReceiver onReceive");

            if (null != intent.getAction() && intent.getAction().equalsIgnoreCase(ACTION_CANCEL)) {
                Logger.verbose(
                        TAG + methodName,
                        "ActivityBroadcastReceiver onReceive action is for cancelling Authentication Activity"
                );

                int cancelRequestId = intent.getIntExtra(REQUEST_ID, 0);

                if (cancelRequestId == mWaitingRequestId) {
                    Logger.verbose(
                            TAG + methodName,
                            "Waiting requestId is same and cancelling this activity"
                    );

                    AuthenticationActivity.this.finish();
                    // no need to send result back to activity. It is
                    // cancelled
                    // and callback will be called after this request.
                }
            }
        }
    }

    // Turn off the deprecation warning for CookieSyncManager.  It was deprecated in API 21, but
    // is still necessary for API level 20 and below.
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final String methodName = ":onCreate";
        super.onCreate(savedInstanceState);

        setContentView(
                this.getResources()
                        .getIdentifier(
                                "activity_authentication",
                                "layout",
                                this.getPackageName()
                        )
        );

        CookieSyncManager.createInstance(getApplicationContext());
        CookieSyncManager.getInstance().sync();
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        Logger.verbose(TAG + methodName, "AuthenticationActivity was created.");

        // Get the message from the intent
        mAuthRequest = getAuthenticationRequestFromIntent(getIntent());

        if (mAuthRequest == null) {
            Logger.warn(
                    TAG + methodName,
                    "Intent for Authentication Activity doesn't have the request details, returning to caller"
            );

            final Intent resultIntent = new Intent();
            resultIntent.putExtra(RESPONSE_ERROR_CODE, WEBVIEW_INVALID_REQUEST);
            resultIntent.putExtra(RESPONSE_ERROR_MESSAGE, "Intent does not have request details");
            returnToCaller(BROWSER_CODE_ERROR, resultIntent);

            return;
        }

        if (mAuthRequest.getAuthority() == null || mAuthRequest.getAuthority().isEmpty()) {
            returnError(ADALError.ARGUMENT_EXCEPTION, ACCOUNT_AUTHORITY);
            return;
        }

        if (mAuthRequest.getResource() == null || mAuthRequest.getResource().isEmpty()) {
            returnError(ADALError.ARGUMENT_EXCEPTION, ACCOUNT_RESOURCE);
            return;
        }

        if (mAuthRequest.getClientId() == null || mAuthRequest.getClientId().isEmpty()) {
            returnError(ADALError.ARGUMENT_EXCEPTION, ACCOUNT_CLIENTID_KEY);
            return;
        }

        if (mAuthRequest.getRedirectUri() == null || mAuthRequest.getRedirectUri().isEmpty()) {
            returnError(ADALError.ARGUMENT_EXCEPTION, ACCOUNT_REDIRECT);
            return;
        }

        mRedirectUrl = mAuthRequest.getRedirectUri();

        Telemetry.getInstance().startEvent(mAuthRequest.getTelemetryRequestId(), EventStrings.UI_EVENT);
        mUIEvent = new UIEvent(EventStrings.UI_EVENT);
        mUIEvent.setRequestId(mAuthRequest.getTelemetryRequestId());
        mUIEvent.setCorrelationId(mAuthRequest.getCorrelationId().toString());

        // Create the Web View to show the page
        mWebView = findViewById(
                this.getResources()
                        .getIdentifier(
                                "webView1",
                                "id",
                                this.getPackageName()
                        )
        );

        // Disable hardware acceleration in WebView if needed
        if (!AuthenticationSettings.INSTANCE.getDisableWebViewHardwareAcceleration()) {
            mWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

            Logger.warn(TAG + methodName, "Hardware acceleration is disabled in WebView");
        }

        mStartUrl = "about:blank";

        try {
            final Oauth2 oauth = new Oauth2(mAuthRequest);
            mStartUrl = oauth.getCodeRequestUrl();
        } catch (UnsupportedEncodingException e) {
            Logger.error(TAG + methodName, "Encoding format is not supported. ", e);

            final Intent resultIntent = new Intent();
            resultIntent.putExtra(RESPONSE_REQUEST_INFO, mAuthRequest);
            returnToCaller(BROWSER_CODE_ERROR, resultIntent);

            return;
        }

        // Create the broadcast receiver for cancel
        Logger.verbose(
                TAG + methodName,
                "Init broadcastReceiver with request. "
                        + "RequestId:"
                        + mAuthRequest.getRequestId()
        );

        Logger.verbosePII(TAG + methodName, mAuthRequest.getLogInfo());

        mReceiver = new ActivityBroadcastReceiver();
        mReceiver.mWaitingRequestId = mAuthRequest.getRequestId();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(ACTION_CANCEL));

        String userAgent = mWebView.getSettings().getUserAgentString();
        mWebView.getSettings().setUserAgentString(userAgent + CLIENT_TLS_NOT_SUPPORTED);
        userAgent = mWebView.getSettings().getUserAgentString();

        Logger.verbosePII(TAG + methodName, "UserAgent:" + userAgent);

        if (isBrokerRequest(getIntent())) {
            // This activity is started from calling app and running in
            // Authenticator's process
            mCallingPackage = getCallingPackage();

            if (mCallingPackage == null) {
                Logger.verbose(
                        TAG + methodName,
                        "Calling package is null, startActivityForResult is not used to call this activity"
                );

                final Intent resultIntent = new Intent();
                resultIntent.putExtra(RESPONSE_ERROR_CODE, WEBVIEW_INVALID_REQUEST);
                resultIntent.putExtra(RESPONSE_ERROR_MESSAGE, "startActivityForResult is not used to call this activity");
                returnToCaller(BROWSER_CODE_ERROR, resultIntent);

                return;
            }
            Logger.info(
                    TAG + methodName,
                    "It is a broker request for package:" + mCallingPackage
            );

            mAccountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

            if (mAccountAuthenticatorResponse != null) {
                mAccountAuthenticatorResponse.onRequestContinued();
            }

            final PackageHelper info = new PackageHelper(AuthenticationActivity.this);
            mCallingPackage = getCallingPackage();
            mCallingUID = info.getUIDForPackage(mCallingPackage);

            final String signatureDigest = info.getCurrentSignatureForPackage(mCallingPackage);
            mStartUrl = getBrokerStartUrl(mStartUrl, mCallingPackage, signatureDigest);

            if (!isCallerBrokerInstaller()) {
                Logger.verbose(
                        TAG + methodName,
                        "Caller needs to be verified using special redirectUri"
                );

                mRedirectUrl = PackageHelper.getBrokerRedirectUrl(mCallingPackage, signatureDigest);
            }

            Logger.verbosePII(
                    TAG + methodName,
                    "Broker redirectUrl: " + mRedirectUrl
                            + " The calling package is: " + mCallingPackage
                            + " Signature hash for calling package is: " + signatureDigest
                            + " Current context package: " + getPackageName()
                            + " Start url: " + mStartUrl
            );
        } else {
            Logger.verbose(TAG + methodName, "Non-broker request for package " + getCallingPackage());
            Logger.verbosePII(TAG + methodName, "Start url: " + mStartUrl);
        }

        mRegisterReceiver = false;
        final String postUrl = mStartUrl;
        Logger.infoPII(
                TAG + methodName,
                "Device info:" + android.os.Build.VERSION.RELEASE
                        + " "
                        + android.os.Build.MANUFACTURER + android.os.Build.MODEL
        );

        mStorageHelper = new StorageHelper(getApplicationContext());
        setupWebView();

        // Also log correlation id
        if (mAuthRequest.getCorrelationId() == null) {
            Logger.verbose(TAG + methodName, "Null correlation id in the request.");
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Correlation id for request sent is:"
                            + mAuthRequest.getCorrelationId().toString()
            );
        }

        if (savedInstanceState == null) {
            mWebView.post(new Runnable() {

                @Override
                public void run() {
                    // load blank first to avoid error for not loading webview
                    Logger.verbose(TAG + methodName, "Launching webview for acquiring auth code.");
                    mWebView.loadUrl("about:blank");
                    mWebView.loadUrl(postUrl);
                }

            });
        } else {
            Logger.verbose(TAG + methodName, "Reuse webview");
        }
    }

    private boolean isCallerBrokerInstaller() {
        // Allow intune's signature check
        final String methodName = ":isCallerBrokerInstaller";
        final PackageHelper info = new PackageHelper(AuthenticationActivity.this);
        final String packageName = getCallingPackage();

        if (!StringExtensions.isNullOrBlank(packageName)) {
            if (packageName.equals(AuthenticationSettings.INSTANCE.getBrokerPackageName())) {
                Logger.verbose(TAG + methodName, "Same package as broker.");

                return true;
            }

            final String signature = info.getCurrentSignatureForPackage(packageName);

            Logger.verbose(TAG + methodName, "Checking broker signature.");
            Logger.verbosePII(
                    TAG + methodName,
                    "Check signature for " + packageName
                            + " signature:" + signature
                            + " brokerSignature:" + AuthenticationSettings.INSTANCE.getBrokerSignature()
            );

            return signature.equals(AuthenticationSettings.INSTANCE.getBrokerSignature())
                    || signature
                    .equals(AZURE_AUTHENTICATOR_APP_SIGNATURE);
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the state of the WebView
        mWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restore the state of the WebView
        mWebView.restoreState(savedInstanceState);
    }

    private void setupWebView() {
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.requestFocus(View.FOCUS_DOWN);

        // Set focus to the view for touch event
        mWebView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                int action = event.getAction();
                if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) && !view.hasFocus()) {
                    view.requestFocus();
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

    private AuthenticationRequest getAuthenticationRequestFromIntent(final Intent callingIntent) {
        final String methodName = ":getAuthenticationRequestFromIntent";
        AuthenticationRequest authRequest = null;

        if (isBrokerRequest(callingIntent)) {
            Logger.verbose(
                    TAG + methodName,
                    "It is a broker request. Get request info from bundle extras."
            );

            final String authority = callingIntent.getStringExtra(ACCOUNT_AUTHORITY);
            final String resource = callingIntent.getStringExtra(ACCOUNT_RESOURCE);
            final String redirect = callingIntent.getStringExtra(ACCOUNT_REDIRECT);
            final String loginhint = callingIntent.getStringExtra(ACCOUNT_LOGIN_HINT);
            final String accountName = callingIntent.getStringExtra(ACCOUNT_NAME);
            final String clientidKey = callingIntent.getStringExtra(ACCOUNT_CLIENTID_KEY);
            final String correlationId = callingIntent.getStringExtra(ACCOUNT_CORRELATIONID);
            final String prompt = callingIntent.getStringExtra(ACCOUNT_PROMPT);

            PromptBehavior promptBehavior = PromptBehavior.Auto;

            if (!StringExtensions.isNullOrBlank(prompt)) {
                promptBehavior = PromptBehavior.valueOf(prompt);
            }

            mWaitingRequestId = callingIntent.getIntExtra(REQUEST_ID, 0);

            UUID correlationIdParsed = null;

            if (!StringExtensions.isNullOrBlank(correlationId)) {

                try {
                    correlationIdParsed = UUID.fromString(correlationId);
                } catch (final IllegalArgumentException ex) {
                    Logger.error(
                            TAG + methodName,
                            "CorrelationId is malformed: " + correlationId,
                            ex
                    );
                }
            }

            authRequest = new AuthenticationRequest(
                    authority,
                    resource,
                    clientidKey,
                    redirect,
                    loginhint,
                    correlationIdParsed,
                    false // isExtendedLifetimeEnabled
            );

            authRequest.setBrokerAccountName(accountName);
            authRequest.setPrompt(promptBehavior);
            authRequest.setRequestId(mWaitingRequestId);
        } else {
            Serializable request = callingIntent.getSerializableExtra(REQUEST_MESSAGE);

            if (request instanceof AuthenticationRequest) {
                authRequest = (AuthenticationRequest) request;
            }
        }
        return authRequest;
    }

    /**
     * Return error to caller and finish this activity.
     */
    private void returnError(final ADALError errorCode, final String argument) {
        // Set result back to account manager call
        Logger.warn(TAG, "Argument error:" + argument);

        final Intent resultIntent = new Intent();
        // TODO only send adalerror from activity side as int
        resultIntent.putExtra(RESPONSE_ERROR_CODE, errorCode.name());
        resultIntent.putExtra(RESPONSE_ERROR_MESSAGE, argument);

        if (mAuthRequest != null) {
            resultIntent.putExtra(REQUEST_ID, mWaitingRequestId);
            resultIntent.putExtra(RESPONSE_REQUEST_INFO, mAuthRequest);
        }

        this.setResult(BROWSER_CODE_ERROR, resultIntent);
        this.finish();
    }

    private String getBrokerStartUrl(final String loadUrl,
                                     final String packageName,
                                     final String signatureDigest) {
        if (!StringExtensions.isNullOrBlank(packageName)
                && !StringExtensions.isNullOrBlank(signatureDigest)) {
            try {
                return loadUrl + "&package_name="
                        + URLEncoder.encode(packageName, ENCODING_UTF8)
                        + "&signature="
                        + URLEncoder.encode(signatureDigest, ENCODING_UTF8);
            } catch (final UnsupportedEncodingException e) {
                // This encoding issue will happen at the beginning of API call,
                // if it is not supported on this device. ADAL uses one encoding
                // type.
                Logger.error(TAG, "Unsupported encoding", e);
                Logger.error(TAG, "Exception details", e);
            }
        }
        return loadUrl;
    }

    private boolean isBrokerRequest(final Intent callingIntent) {
        // Intent should have a flag and activity is hosted inside broker
        return callingIntent != null
                && !StringExtensions.isNullOrBlank(callingIntent.getStringExtra(BROKER_REQUEST));
    }

    /**
     * Activity sets result to go back to the caller.
     *
     * @param resultCode result code to be returned to the called
     * @param data       intent to be returned to the caller
     */
    private void returnToCaller(final int resultCode, Intent data) {
        final String methodName = ":returnToCaller";
        Logger.verbose(TAG + methodName, "Return To Caller:" + resultCode);

        displaySpinner(false);

        if (data == null) {
            data = new Intent();
        }

        if (mAuthRequest == null) {
            Logger.warn(TAG + methodName, "Request object is null");
        } else {
            // set request id related to this response to send the delegateId
            Logger.verbose(
                    TAG + methodName,
                    "Set request id related to response. "
                            + "REQUEST_ID for caller returned to:"
                            + mAuthRequest.getRequestId()
            );

            data.putExtra(REQUEST_ID, mAuthRequest.getRequestId());
        }

        setResult(resultCode, data);
        this.finish();
    }

    @Override
    protected void onPause() {
        final String methodName = ":onPause";
        Logger.verbose(TAG + methodName, "AuthenticationActivity onPause unregister receiver");

        super.onPause();

        // Unregister the cancel action listener from the local broadcast
        // manager since activity is not visible
        if (mReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }

        mRegisterReceiver = true;

        if (mSpinner != null) {
            Logger.verbose(TAG + methodName, "Spinner at onPause will dismiss");
            mSpinner.dismiss();
        }

        hideKeyBoard();
    }

    @Override
    protected void onResume() {
        final String methodName = ":onResume";
        super.onResume();
        // It can come here from onCreate, onRestart or onPause.
        // Don't load url again since it will send another 2FA request
        if (mRegisterReceiver) {
            Logger.verbose(TAG + methodName, "Webview onResume will register receiver.");

            Logger.verbosePII(TAG + methodName, "StartUrl: " + mStartUrl);

            if (mReceiver != null) {
                Logger.verbose(
                        TAG + methodName,
                        "Webview onResume register broadcast receiver for request. "
                                + "RequestId: "
                                + mReceiver.mWaitingRequestId
                );

                LocalBroadcastManager
                        .getInstance(this)
                        .registerReceiver(
                                mReceiver,
                                new IntentFilter(ACTION_CANCEL)
                        );
            }
        }
        mRegisterReceiver = false;

        // Spinner dialog to show some message while it is loading
        mSpinner = new ProgressDialog(this);
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage(
                this.getText(
                        this.getResources()
                                .getIdentifier(
                                        "app_loading",
                                        "string",
                                        this.getPackageName()
                                )
                )
        );
    }

    @Override
    protected void onRestart() {
        Logger.verbose(TAG, "AuthenticationActivity onRestart");
        super.onRestart();
        mRegisterReceiver = true;
    }

    @Override
    public void onBackPressed() {
        Logger.verbose(TAG, "Back button is pressed");

        // User should be able to click back button to cancel in case pkeyauth
        // happen.
        if (mPkeyAuthRedirect || !mWebView.canGoBackOrForward(BACK_PRESSED_CANCEL_DIALOG_STEPS)) {
            // counting blank page as well
            cancelRequest(null);
        } else {
            // Don't use default back pressed action, since user can go back in
            // webview
            mWebView.goBack();
        }
    }

    private void cancelRequest(@Nullable Intent errorIntent) {
        Logger.verbose(TAG, "Sending intent to cancel authentication activity");
        int resultCode;
        Intent resultIntent = errorIntent;
        if (resultIntent == null) {
            resultIntent = new Intent();
            resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL;
            if (mUIEvent != null) {
                mUIEvent.setUserCancel();
            }
        } else {
            resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR;
        }

        returnToCaller(resultCode, resultIntent);
    }

    private void prepareForBrokerResume() {
        final String methodName = ":prepareForBrokerResume";
        Logger.verbose(
                TAG + methodName,
                "Return to caller with BROKER_REQUEST_RESUME, and waiting for result."
        );

        final Intent resultIntent = new Intent();
        returnToCaller(BROKER_REQUEST_RESUME, resultIntent);
    }

    private void hideKeyBoard() {
        if (mWebView != null) {
            InputMethodManager imm = (InputMethodManager) this.getSystemService(Service.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mWebView.getApplicationWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mUIEvent != null) {
            Telemetry.getInstance()
                    .stopEvent(
                            mAuthRequest.getTelemetryRequestId(),
                            mUIEvent,
                            EventStrings.UI_EVENT
                    );
        }
    }

    private class CustomWebViewClient extends BasicWebViewClient {

        CustomWebViewClient() {
            super(AuthenticationActivity.this, mRedirectUrl, mAuthRequest, mUIEvent);
        }

        public void processRedirectUrl(final WebView view, final String url) {
            final String methodName = ":processRedirectUrl";

            if (!isBrokerRequest(getIntent())) {
                // It is pointing to redirect. Final url can be processed to
                // get the code or error.
                Logger.info(TAG + methodName, "It is not a broker request");

                final Intent resultIntent = new Intent();
                resultIntent.putExtra(RESPONSE_FINAL_URL, url);
                resultIntent.putExtra(RESPONSE_REQUEST_INFO, mAuthRequest);
                returnToCaller(BROWSER_CODE_COMPLETE, resultIntent);

                view.stopLoading();
            } else {
                Logger.info(TAG + methodName, "It is a broker request");

                displaySpinnerWithMessage(
                        AuthenticationActivity.this.getText(
                                AuthenticationActivity.this
                                        .getResources()
                                        .getIdentifier(
                                                "broker_processing",
                                                "string",
                                                getPackageName()
                                        )
                        )
                );

                view.stopLoading();

                // do async task and show spinner while exchanging code for
                // access token
                new TokenTask(mWebRequestHandler, mAuthRequest, mCallingPackage, mCallingUID).execute(url);
            }
        }

        public boolean processInvalidUrl(final WebView view, final String url) {
            final String methodName = ":processInvalidUrl";

            if (isBrokerRequest(getIntent()) && url.startsWith(REDIRECT_PREFIX)) {
                Logger.error(
                        TAG + methodName,
                        "The RedirectUri is not as expected.",
                        null
                );
                Logger.errorPII(
                        TAG + methodName,
                        String.format("Received %s and expected %s", url, mRedirectUrl),
                        null
                );

                returnError(
                        ADALError.DEVELOPER_REDIRECTURI_INVALID,
                        String.format(
                                "The RedirectUri is not as expected. Received %s and expected %s", url,
                                mRedirectUrl
                        )
                );
                view.stopLoading();

                return true;
            }

            if (url.toLowerCase(Locale.US).equals("about:blank")) {
                Logger.verbose(TAG + methodName, "It is an blank page request");

                return true;
            }

            if (!url.toLowerCase(Locale.US).startsWith(REDIRECT_SSL_PREFIX)) {
                final String errMsg = "The webview was redirected to an unsafe URL.";
                Logger.error(TAG + methodName, errMsg, null);

                returnError(ADALError.WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED, errMsg);

                view.stopLoading();

                return true;
            }

            return false;
        }

        public void showSpinner(final boolean status) {
            displaySpinner(status);
        }

        @Override
        public void sendResponse(final int returnCode, final Intent responseIntent) {
            returnToCaller(returnCode, responseIntent);
        }

        @Override
        public void cancelWebViewRequest(@Nullable Intent errorIntent) {
            cancelRequest(errorIntent);
        }

        @Override
        public void prepareForBrokerResumeRequest() {
            prepareForBrokerResume();
        }

        @Override
        public void setPKeyAuthStatus(final boolean status) {
            mPkeyAuthRedirect = status;
        }

        @Override
        public void postRunnable(final Runnable item) {
            mWebView.post(item);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceivedClientCertRequest(final WebView view,
                                                final ClientCertRequest request) {
            final String methodName = ":onReceivedClientCertRequest";
            Logger.verbose(TAG + methodName, "Webview receives client TLS request.");

            final Principal[] acceptableCertIssuers = request.getPrincipals();

            // When ADFS server sends null or empty issuers, we'll continue with cert prompt.
            if (acceptableCertIssuers != null) {
                for (final Principal issuer : acceptableCertIssuers) {
                    if (issuer.getName().contains("CN=MS-Organization-Access")) {
                        //Checking if received acceptable issuers contain "CN=MS-Organization-Access"
                        Logger.verbose(
                                TAG + methodName,
                                "Cancelling the TLS request, not respond to TLS challenge triggered by device authentication."
                        );
                        request.cancel();
                        return;
                    }
                }
            }

            KeyChain.choosePrivateKeyAlias(
                    AuthenticationActivity.this,
                    new KeyChainAliasCallback() {

                        @Override
                        public void alias(final String alias) {
                            if (alias == null) {
                                Logger.verbose(
                                        TAG + methodName,
                                        "No certificate chosen by user, cancelling the TLS request."
                                );
                                request.cancel();
                                return;
                            }

                            try {
                                final X509Certificate[] certChain = KeyChain.getCertificateChain(getApplicationContext(), alias);
                                final PrivateKey privateKey = KeyChain.getPrivateKey(getCallingContext(), alias);

                                Logger.verbose(
                                        TAG + methodName,
                                        "Certificate is chosen by user, proceed with TLS request."
                                );
                                request.proceed(privateKey, certChain);
                                return;
                            } catch (final KeyChainException e) {
                                Logger.error(TAG + methodName, "Keychain exception", null);
                                Logger.errorPII(TAG + methodName, "Exception details:", e);
                            } catch (final InterruptedException e) {
                                Logger.error(TAG + methodName, "InterruptedException exception", e);
                            }

                            request.cancel();
                        }
                    },
                    request.getKeyTypes(),
                    request.getPrincipals(),
                    request.getHost(),
                    request.getPort(),
                    null
            );
        }
    }

    /**
     * handle spinner display.
     *
     * @param show True if spinner needs to be displayed, False otherwise
     */
    private void displaySpinner(final boolean show) {
        final String methodName = ":displaySpinner";

        if (!AuthenticationActivity.this.isFinishing()
                && !AuthenticationActivity.this.isChangingConfigurations() && mSpinner != null) {
            // Used externally to verify web view processing.
            Logger.verbose(
                    TAG + methodName,
                    "DisplaySpinner:" + show
                            + " showing:" + mSpinner.isShowing()
            );

            if (show && !mSpinner.isShowing()) {
                mSpinner.show();
            }

            if (!show && mSpinner.isShowing()) {
                mSpinner.dismiss();
            }
        }
    }

    private void displaySpinnerWithMessage(final CharSequence charSequence) {
        if (!AuthenticationActivity.this.isFinishing() && mSpinner != null) {
            mSpinner.show();
            mSpinner.setMessage(charSequence);
        }
    }

    private void returnResult(final int resultcode, final Intent intent) {
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
            Logger.verbose(TAG, "It is a broker request");

            if (mAuthenticatorResultBundle == null) {
                mAccountAuthenticatorResponse.onError(
                        AccountManager.ERROR_CODE_CANCELED,
                        "canceled"
                );
            } else {
                mAccountAuthenticatorResponse.onResult(mAuthenticatorResultBundle);
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
     *               AbstractAccountAuthenticator request
     */
    private void setAccountAuthenticatorResult(final Bundle result) {
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

        private String mPackageName;
        private int mAppCallingUID;
        private AuthenticationRequest mRequest;
        private AccountManager mAccountManager;
        private IWebRequestHandler mRequestHandler;

        public TokenTask() {
            // Intentionally left blank
        }

        public TokenTask(final IWebRequestHandler webHandler,
                         final AuthenticationRequest request,
                         final String packageName,
                         final int callingUID) {
            mRequestHandler = webHandler;
            mRequest = request;
            mPackageName = packageName;
            mAppCallingUID = callingUID;
            mAccountManager = AccountManager.get(AuthenticationActivity.this);
        }

        @Override
        protected TokenTaskResult doInBackground(final String... urlItems) {
            final Oauth2 oauthRequest = new Oauth2(mRequest, mRequestHandler, mJWSBuilder);
            final TokenTaskResult result = new TokenTaskResult();

            try {
                result.mTaskResult = oauthRequest.getToken(urlItems[0]);
                Logger.verbosePII(
                        TAG,
                        "Process result returned from TokenTask. "
                                + mRequest.getLogInfo()
                );
            } catch (final IOException | AuthenticationException exc) {
                Logger.error(TAG, "Error in processing code to get a token.", exc);
                Logger.errorPII(TAG, mRequest.getLogInfo(), null);

                result.mTaskException = exc;
            }

            if (result.mTaskResult != null && result.mTaskResult.getAccessToken() != null) {
                Logger.verbosePII(
                        TAG,
                        "Token task successfully returns access token. "
                                + mRequest.getLogInfo()
                );

                // Record account in the AccountManager service
                try {
                    setAccount(result);
                } catch (final GeneralSecurityException | IOException exc) {
                    Logger.error(TAG, "Error in setting the account.", null);
                    Logger.errorPII(TAG, mRequest.getLogInfo(), exc);

                    result.mTaskException = exc;
                }
            }

            return result;
        }

        private String getBrokerAppCacheKey(final String cacheKey)
                throws NoSuchAlgorithmException, UnsupportedEncodingException {
            // include UID in the key for broker to store caches for different
            // apps under same account entry
            final String digestKey = StringExtensions.createHash(
                    USERDATA_UID_KEY
                            + mAppCallingUID
                            + cacheKey
            );

            Logger.verbose(TAG, "Get broker app cache key.");

            Logger.verbosePII(
                    TAG,
                    "Key hash is:" + digestKey
                            + " calling app UID:" + mAppCallingUID
                            + " Key is: " + cacheKey
            );

            return digestKey;
        }

        @SuppressLint("MissingPermission")
        private void appendAppUIDToAccount(final Account account)
                throws GeneralSecurityException, IOException {
            final String methodName = ":appendAppUIDToAccount";

            String appIdList = mAccountManager.getUserData(account, ACCOUNT_UID_CACHES);

            if (appIdList == null) {
                appIdList = "";
            } else {
                try {
                    appIdList = mStorageHelper.decrypt(appIdList);
                } catch (final GeneralSecurityException | IOException ex) {
                    Logger.error(TAG + methodName, "appUIDList failed to decrypt", null);

                    Logger.errorPII(TAG + methodName, "appIdList:" + appIdList, ex);

                    appIdList = "";
                    Logger.info(TAG + methodName, "Reset the appUIDlist");
                }
            }

            Logger.info(TAG + methodName, "Add calling UID.");

            Logger.infoPII(
                    TAG + methodName,
                    "App UID: " + mAppCallingUID
                            + "appIdList:" + appIdList
            );

            if (!appIdList.contains(USERDATA_UID_KEY + mAppCallingUID)) {
                Logger.info(TAG + methodName, "Account has new calling UID.");
                Logger.infoPII(TAG + methodName, "App UID: " + mAppCallingUID);

                final String encryptedValue = mStorageHelper.encrypt(
                        appIdList
                                + USERDATA_UID_KEY
                                + mAppCallingUID
                );

                mAccountManager.setUserData(account, ACCOUNT_UID_CACHES, encryptedValue);
            }
        }

        @SuppressLint("MissingPermission")
        private void setAccount(final TokenTaskResult result)
                throws GeneralSecurityException, IOException {
            // TODO Add token logging
            // TODO update for new cache logic

            final String methodName = ":setAccount";
            // Authenticator sets the account here and stores the tokens.
            final String name = mRequest.getBrokerAccountName();
            final Account[] accountList = mAccountManager.getAccountsByType(BROKER_ACCOUNT_TYPE);

            if (accountList.length != 1) {
                result.mTaskResult = null;
                result.mTaskException = new AuthenticationException(ADALError.BROKER_SINGLE_USER_EXPECTED);

                return;
            }

            final Account newAccount = accountList[0];

            // Single user in authenticator is already created.
            // This is only registering UID for the app
            final UserInfo userinfo = result.mTaskResult.getUserInfo();

            if (userinfo == null || StringExtensions.isNullOrBlank(userinfo.getUserId())) {
                // return userid in the userinfo and use only account name
                // for all fields
                Logger.info(TAG + methodName, "Set userinfo from account");

                result.mTaskResult.setUserInfo(new UserInfo(name, name, "", "", name));
                mRequest.setLoginHint(name);
            } else {
                Logger.info(TAG + methodName, "Saving userinfo to account");
                mAccountManager.setUserData(newAccount, ACCOUNT_USERINFO_USERID, userinfo.getUserId());
                mAccountManager.setUserData(newAccount, ACCOUNT_USERINFO_GIVEN_NAME, userinfo.getGivenName());
                mAccountManager.setUserData(newAccount, ACCOUNT_USERINFO_FAMILY_NAME, userinfo.getFamilyName());
                mAccountManager.setUserData(newAccount, ACCOUNT_USERINFO_IDENTITY_PROVIDER, userinfo.getIdentityProvider());
                mAccountManager.setUserData(newAccount, ACCOUNT_USERINFO_USERID_DISPLAYABLE, userinfo.getDisplayableId());
            }

            result.mAccountName = name;

            Logger.info(TAG + methodName, "Setting account in account manager.");

            Logger.infoPII(
                    TAG + methodName,
                    "Package: " + mPackageName
                            + " calling app UID:" + mAppCallingUID
                            + " Account name: " + name
            );

            // Cache logic will be changed based on latest logic
            // This is currently keeping accesstoken and MRRT separate
            // Encrypted Results are saved to AccountManager Service
            // sqllite database. Only Authenticator and similar UID can
            // access.
            final Gson gson = new Gson();

            Logger.infoPII(
                    TAG + methodName,
                    "app context:" + getApplicationContext().getPackageName()
                            + " context:" + AuthenticationActivity.this.getPackageName()
                            + " calling packagename:" + getCallingPackage()
            );

            if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
                Logger.info(TAG + methodName, "Calling app doesn't provide the secret key.");
            }

            final TokenCacheItem item = TokenCacheItem.createRegularTokenCacheItem(
                    mRequest.getAuthority(),
                    mRequest.getResource(),
                    mRequest.getClientId(),
                    result.mTaskResult
            );

            String json = gson.toJson(item);
            String encrypted = mStorageHelper.encrypt(json);

            // Single user and cache is stored per account
            String key = CacheKey.createCacheKeyForRTEntry(
                    mAuthRequest.getAuthority(),
                    mAuthRequest.getResource(),
                    mAuthRequest.getClientId(),
                    null
            );

            saveCacheKey(key, newAccount, mAppCallingUID);
            mAccountManager.setUserData(newAccount, getBrokerAppCacheKey(key), encrypted);

            if (result.mTaskResult.getIsMultiResourceRefreshToken()) {
                // ADAL stores MRRT refresh token separately
                final TokenCacheItem itemMRRT = TokenCacheItem.createMRRTTokenCacheItem(
                        mRequest.getAuthority(),
                        mRequest.getClientId(),
                        result.mTaskResult
                );

                json = gson.toJson(itemMRRT);
                encrypted = mStorageHelper.encrypt(json);

                key = CacheKey.createCacheKeyForMRRT(
                        mAuthRequest.getAuthority(),
                        mAuthRequest.getClientId(),
                        null
                );

                saveCacheKey(key, newAccount, mAppCallingUID);
                mAccountManager.setUserData(newAccount, getBrokerAppCacheKey(key), encrypted);
            }

            // Record calling UID for this account so that app can get token
            // in the background call without requiring server side
            // validation
            Logger.info(TAG + methodName, "Set calling uid:" + mAppCallingUID);

            appendAppUIDToAccount(newAccount);
        }

        @SuppressLint("MissingPermission")
        private void saveCacheKey(final String key,
                                  final Account cacheAccount,
                                  final int callingUID) {
            final String methodName = ":saveCacheKey";

            Logger.verbose(TAG + methodName, "Get CacheKeys for account");

            // Store cachekeys for each UID
            // Activity has access to packagename and UID, but background call
            // in getAuthToken only knows about UID
            String keylist = mAccountManager.getUserData(cacheAccount, USERDATA_CALLER_CACHEKEYS + callingUID);

            if (keylist == null) {
                keylist = "";
            }

            if (!keylist.contains(CALLER_CACHEKEY_PREFIX + key)) {
                Logger.verbose(
                        TAG + methodName,
                        "Account does not have the cache key. Saving it to account for the caller."
                );

                Logger.verbosePII(
                        TAG + methodName,
                        "callerUID: " + callingUID
                                + "The key to be saved is: " + key
                );

                keylist += CALLER_CACHEKEY_PREFIX + key;
                mAccountManager.setUserData(cacheAccount, USERDATA_CALLER_CACHEKEYS + callingUID, keylist);

                Logger.verbose(TAG + methodName, "Cache key saved into key list for the caller.");
                Logger.verbosePII(TAG + methodName, "keylist:" + keylist);
            }
        }

        @Override
        protected void onPostExecute(final TokenTaskResult result) {
            Logger.verbose(TAG, "Token task returns the result");
            displaySpinner(false);
            Intent intent = new Intent();

            if (result.mTaskResult == null) {
                Logger.verbose(TAG, "Token task has exception");
                returnError(ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN, result.mTaskException.getMessage());

                return;
            }

            if (result.mTaskResult.getStatus().equals(AuthenticationStatus.Succeeded)) {
                intent.putExtra(REQUEST_ID, mWaitingRequestId);
                intent.putExtra(ACCOUNT_ACCESS_TOKEN, result.mTaskResult.getAccessToken());
                intent.putExtra(ACCOUNT_NAME, result.mAccountName);

                if (result.mTaskResult.getExpiresOn() != null) {
                    intent.putExtra(ACCOUNT_EXPIREDATE, result.mTaskResult.getExpiresOn().getTime());
                }

                if (result.mTaskResult.getTenantId() != null) {
                    intent.putExtra(ACCOUNT_USERINFO_TENANTID, result.mTaskResult.getTenantId());
                }

                final UserInfo userinfo = result.mTaskResult.getUserInfo();

                if (userinfo != null) {
                    intent.putExtra(ACCOUNT_USERINFO_USERID, userinfo.getUserId());
                    intent.putExtra(ACCOUNT_USERINFO_GIVEN_NAME, userinfo.getGivenName());
                    intent.putExtra(ACCOUNT_USERINFO_FAMILY_NAME, userinfo.getFamilyName());
                    intent.putExtra(ACCOUNT_USERINFO_IDENTITY_PROVIDER, userinfo.getIdentityProvider());
                    intent.putExtra(ACCOUNT_USERINFO_USERID_DISPLAYABLE, userinfo.getDisplayableId());
                }

                if (null != result.mTaskResult.getCliTelemInfo()) {
                    final TelemetryUtils.CliTelemInfo cliTelemInfo = result.mTaskResult.getCliTelemInfo();
                    intent.putExtra(SPE_RING, cliTelemInfo.getSpeRing());
                    intent.putExtra(RT_AGE, cliTelemInfo.getRefreshTokenAge());
                    intent.putExtra(SERVER_ERROR, cliTelemInfo.getServerErrorCode());
                    intent.putExtra(SERVER_SUBERROR, cliTelemInfo.getServerSubErrorCode());
                }

                returnResult(TOKEN_BROKER_RESPONSE, intent);
            } else {
                returnError(
                        ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN,
                        result.mTaskResult.getErrorDescription()
                );
            }
        }
    }

    class TokenTaskResult {
        private AuthenticationResult mTaskResult;
        private Exception mTaskException;
        private String mAccountName;
    }
}
