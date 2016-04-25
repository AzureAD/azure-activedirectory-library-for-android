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

import com.google.gson.Gson;
import com.microsoft.aad.adal.AuthenticationResult.AuthenticationStatus;

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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ClientCertRequest;
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
    private StorageHelper mStorageHelper;

    // Broadcast receiver is needed to cancel outstanding AuthenticationActivity
    // for this AuthenticationContext since each instance of context can have
    // one active activity
    private class ActivityBroadcastReceiver extends android.content.BroadcastReceiver {

        private int mWaitingRequestId = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.v(TAG, "ActivityBroadcastReceiver onReceive");

            if (intent.getAction().equalsIgnoreCase(AuthenticationConstants.Browser.ACTION_CANCEL)) {
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
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        final String methodName = ":onCreate";
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
        Logger.v(TAG, "OnCreate redirectUrl:" + mRedirectUrl);
        // Create the Web View to show the page
        mWebView = (WebView)findViewById(this.getResources().getIdentifier("webView1", "id",
                this.getPackageName()));
        
        // Disable hardware acceleration in WebView if needed
        if (!AuthenticationSettings.INSTANCE.getDisableWebViewHardwareAcceleration()) {
            mWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
            Log.d(TAG, "Hardware acceleration is disabled in WebView");
        }

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

        if (isBrokerRequest(getIntent())) 
        {
            // This activity is started from calling app and running in
            // Authenticator's process
            mCallingPackage = getCallingPackage();
            Logger.i(TAG, "It is a broker request for package:" + mCallingPackage, "");

            if (mCallingPackage == null) {
                Logger.v(TAG, "startActivityForResult is not used to call this activity");
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
            Logger.v(TAG, "OnCreate redirectUrl:" + mRedirectUrl + " startUrl:" + mStartUrl
                    + " calling package:" + mCallingPackage + " signatureDigest:" + signatureDigest
                    + " current Context Package: " + getPackageName());
        }
        else
        {
            Logger.v(TAG + methodName, "Non-broker request for package " + getCallingPackage());
        }
        
        mRegisterReceiver = false;
        final String postUrl = mStartUrl;
        Logger.i(TAG, "OnCreate startUrl:" + mStartUrl + " calling package:" + mCallingPackage,
                " device:" + android.os.Build.VERSION.RELEASE + " " + android.os.Build.MANUFACTURER
                        + android.os.Build.MODEL);

        mStorageHelper = new StorageHelper(getApplicationContext());
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
            return signature.equals(AuthenticationSettings.INSTANCE.getBrokerSignature())
                    || signature
                            .equals(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_SIGNATURE);
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

    private boolean isBrokerRequest(Intent callingIntent) 
    {
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
        
        hideKeyBoard();
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
    
    private void prepareForBrokerResume ()
    {
        final String methodName = ":prepareForBrokerResume";
        Logger.v(TAG + methodName, "Return to caller with BROKER_REQUEST_RESUME, and waiting for result.");
        
        final Intent resultIntent = new Intent();
        returnToCaller(AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME, resultIntent);
    }

    private void hideKeyBoard() {
        if (mWebView != null) {
            InputMethodManager imm = (InputMethodManager)this
                    .getSystemService(Service.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mWebView.getApplicationWindowToken(), 0);
        }
    }
    
    class CustomWebViewClient extends BasicWebViewClient {

        public CustomWebViewClient(){
            super(AuthenticationActivity.this, mRedirectUrl, mQueryParameters, mAuthRequest);
        }
                
        public void processRedirectUrl(final WebView view, String url){
            if (!isBrokerRequest(getIntent())) {
                // It is pointing to redirect. Final url can be processed to
                // get the code or error.
                Logger.i(TAG, "It is not a broker request", "");
                Intent resultIntent = new Intent();
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, url);
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                        mAuthRequest);
                returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE,
                        resultIntent);
                view.stopLoading();
            } else {
                Logger.i(TAG, "It is a broker request", "");
                displaySpinnerWithMessage(AuthenticationActivity.this
                        .getText(AuthenticationActivity.this.getResources().getIdentifier(
                                "broker_processing", "string", getPackageName())));

                view.stopLoading();

                // do async task and show spinner while exchanging code for
                // access token
                new TokenTask(mWebRequestHandler, mAuthRequest, mCallingPackage, mCallingUID)
                        .execute(url);
            }
        }
        
        public boolean processInvalidUrl(final WebView view, String url) {
        	final String methodName = ":processInvalidUrl";
            if (isBrokerRequest(getIntent())
                    && url.startsWith(AuthenticationConstants.Broker.REDIRECT_PREFIX)) 
            {
            	Logger.e(TAG + methodName, String.format(
                        "The RedirectUri is not as expected. Received %s and expected %s", url,
                        mRedirectUrl), "", ADALError.DEVELOPER_REDIRECTURI_INVALID);
                returnError(ADALError.DEVELOPER_REDIRECTURI_INVALID, String.format(
                        "The RedirectUri is not as expected. Received %s and expected %s", url,
                        mRedirectUrl));
                view.stopLoading();
                return true;
            }
            
            //check if the redirect URL is under SSL protected
            if(!url.toLowerCase(Locale.US).startsWith(AuthenticationConstants.Broker.REDIRECT_SSL_PREFIX))
            {
            	Logger.e(TAG + methodName, "The webview was redirected to an unsafe URL.", "", ADALError.WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED);
            	returnError(ADALError.WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED, "The webview was redirected to an unsafe URL.");
            	view.stopLoading();
            	return true;
            }

            return false;
        }
        
        public void showSpinner(boolean status){
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
        public void prepareForBrokerResumeRequest ()
        {
            prepareForBrokerResume();
        }

        @Override
        public void setPKeyAuthStatus(boolean status) {
            mPkeyAuthRedirect = status;            
        }

        @Override
        public void postRunnable(Runnable item) {
            mWebView.post(item);            
        }
        
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceivedClientCertRequest (WebView view, final ClientCertRequest request)
        {
            final String methodName = ":onReceivedClientCertRequest";
            Logger.v(TAG + methodName, "Webview receives client TLS request.");
            
            final Principal[] acceptableCertIssuers = request.getPrincipals();
            
            // When ADFS server sends null or empty issuers, we'll continue with cert prompt.
            if (acceptableCertIssuers != null)
            {
                for (Principal issuer : acceptableCertIssuers)
                {
                    if (issuer.getName().contains("CN=MS-Organization-Access"))
                    {
                        //Checking if received acceptable issuers contain "CN=MS-Organization-Access"
                        Logger.v(TAG + methodName, "Cancelling the TLS request, not respond to TLS challenge triggered by device authenticaton.");
                        request.cancel();
                        return;
                    }
                }
            }
            
            KeyChain.choosePrivateKeyAlias(AuthenticationActivity.this, new KeyChainAliasCallback() {

                @Override
                public void alias(String alias) {
                    if (alias == null) {
                        Logger.v(TAG + methodName, "No certificate chosen by user, cancelling the TLS request.");
                        request.cancel();
                        return;
                    }

                    try {
                        final X509Certificate[] certChain = KeyChain.getCertificateChain(
                                getApplicationContext(), alias);
                        final PrivateKey privateKey = KeyChain.getPrivateKey(mCallingContext, alias);

                        Logger.v(TAG + methodName, "Certificate is chosen by user, proceed with TLS request.");
                        request.proceed(privateKey, certChain);
                        return;
                    } catch (KeyChainException e) {
                        Log.e(TAG, "KeyChain exception", e);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "InterruptedException exception", e);
                    }

                    request.cancel();
                }
            }, request.getKeyTypes(), request.getPrincipals(), request.getHost(), request.getPort(), null);
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
            } catch (IOException | AuthenticationException exc) {
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
                } catch (GeneralSecurityException | IOException exc) {
                    Logger.e(TAG, "Error in setting the account" + mRequest.getLogInfo(), "",
                            ADALError.BROKER_ACCOUNT_SAVE_FAILED, exc);
                    result.taskException = exc;
                }
            }

            return result;
        }

        private String getBrokerAppCacheKey(String cacheKey)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
            // include UID in the key for broker to store caches for different
            // apps under same account entry
            String digestKey = StringExtensions
                    .createHash(AuthenticationConstants.Broker.USERDATA_UID_KEY + mAppCallingUID
                            + cacheKey);
            Logger.v(TAG, "Cache key original:" + cacheKey + " digestKey:" + digestKey
                    + " calling app UID:" + mAppCallingUID);
            return digestKey;
        }

        private void appendAppUIDToAccount(Account account)
            throws GeneralSecurityException, IOException {
            String appIdList = mAccountManager.getUserData(account,
                    AuthenticationConstants.Broker.ACCOUNT_UID_CACHES);
            if (appIdList == null) {
                appIdList = "";
            } else {
                try {
                    appIdList = mStorageHelper.decrypt(appIdList);
                } catch (GeneralSecurityException | IOException ex) {
                    Logger.e(TAG, "appUIDList failed to decrypt", "appIdList:" + appIdList,
                            ADALError.ENCRYPTION_FAILED, ex);
                    appIdList = "";
                    Logger.i(TAG, "Reset the appUIDlist", "");
                }
            }

            Logger.i(TAG, "Add calling UID:" + mAppCallingUID, "appIdList:" + appIdList);
            if (!appIdList.contains(AuthenticationConstants.Broker.USERDATA_UID_KEY
                    + mAppCallingUID)) {
                Logger.i(TAG, "Account has new calling UID:" + mAppCallingUID, "");
                String encryptedValue = mStorageHelper.encrypt(appIdList
                        + AuthenticationConstants.Broker.USERDATA_UID_KEY
                        + mAppCallingUID);
                mAccountManager
                        .setUserData(
                        account,
                        AuthenticationConstants.Broker.ACCOUNT_UID_CACHES,
                        encryptedValue);
            }
        }

        private void setAccount(final TokenTaskResult result)
                throws GeneralSecurityException, IOException {
            // TODO Add token logging
            // TODO update for new cache logic

            // Authenticator sets the account here and stores the tokens.
            String name = mRequest.getBrokerAccountName();
            Account[] accountList = mAccountManager
                    .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);

            if (accountList == null || accountList.length != 1) {
                result.taskResult = null;
                result.taskException = new AuthenticationException(
                        ADALError.BROKER_SINGLE_USER_EXPECTED);
                return;
            }

            Account newaccount = accountList[0];

            // Single user in authenticator is already created.
            // This is only registering UID for the app
            UserInfo userinfo = result.taskResult.getUserInfo();
            if (userinfo == null || StringExtensions.IsNullOrBlank(userinfo.getUserId())) {
                // return userid in the userinfo and use only account name
                // for all fields
                Logger.i(TAG, "Set userinfo from account", "");
                result.taskResult.setUserInfo(new UserInfo(name, name, "", "", name));
                mRequest.setLoginHint(name);
            } else {
                Logger.i(TAG, "Saving userinfo to account", "");
                mAccountManager.setUserData(newaccount,
                        AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID,
                        userinfo.getUserId());
                mAccountManager.setUserData(newaccount,
                        AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME,
                        userinfo.getGivenName());
                mAccountManager.setUserData(newaccount,
                        AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME,
                        userinfo.getFamilyName());
                mAccountManager.setUserData(newaccount,
                        AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER,
                        userinfo.getIdentityProvider());
                mAccountManager.setUserData(newaccount,
                        AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE,
                        userinfo.getDisplayableId());
            }
            result.accountName = name;
            Logger.i(TAG, "Setting account. Account name: " + name + " package:"
                    + mCallingPackage + " calling app UID:" + mAppCallingUID, "");


            // Cache logic will be changed based on latest logic
            // This is currently keeping accesstoken and MRRT separate
            // Encrypted Results are saved to AccountManager Service
            // sqllite database. Only Authenticator and similar UID can
            // access.
            Gson gson = new Gson();
            Logger.i(TAG, "app context:" + getApplicationContext().getPackageName()
                    + " context:" + AuthenticationActivity.this.getPackageName()
                    + " calling packagename:" + getCallingPackage(), "");
            if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
                Logger.i(TAG, "setAccount: user key is null", "");
            }

            TokenCacheItem item = new TokenCacheItem(mRequest, result.taskResult, false);
            String json = gson.toJson(item);
            String encrypted = mStorageHelper.encrypt(json);

            // Single user and cache is stored per account
            String key = CacheKey.createCacheKey(mRequest, null);
            saveCacheKey(key, newaccount, mAppCallingUID);
            mAccountManager.setUserData(
                    newaccount,
                    getBrokerAppCacheKey(key),
                    encrypted);

            if (result.taskResult.getIsMultiResourceRefreshToken()) {
                // ADAL stores MRRT refresh token separately
                TokenCacheItem itemMRRT = new TokenCacheItem(mRequest, result.taskResult, true);
                json = gson.toJson(itemMRRT);
                encrypted = mStorageHelper.encrypt(json);
                key = CacheKey.createMultiResourceRefreshTokenKey(mRequest, null);
                saveCacheKey(key, newaccount, mAppCallingUID);
                mAccountManager.setUserData(
                        newaccount,
                        getBrokerAppCacheKey(key),
                        encrypted);
            }

            // Record calling UID for this account so that app can get token
            // in the background call without requiring server side
            // validation
            Logger.i(TAG, "Set calling uid:" + mAppCallingUID, "");
            appendAppUIDToAccount(newaccount);
        }

        private void saveCacheKey(String key, Account cacheAccount, int callingUID) {
            Logger.v(TAG, "Get CacheKeys for account");
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

                if (result.taskResult.getStatus().equals(AuthenticationStatus.Succeeded)) {
                    intent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, mWaitingRequestId);
                    intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN,
                            result.taskResult.getAccessToken());
                    intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_NAME, result.accountName);

                    if (result.taskResult.getExpiresOn() != null) {
                        intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE,
                                result.taskResult.getExpiresOn().getTime());
                    }
                    
                    if (result.taskResult.getTenantId() != null) {
                    	intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID, 
                    			result.taskResult.getTenantId());
                    }

                    UserInfo userinfo = result.taskResult.getUserInfo();
                    if (userinfo != null) {
                        intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID,
                                userinfo.getUserId());
                        intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME,
                                userinfo.getGivenName());
                        intent.putExtra(
                                AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME,
                                userinfo.getFamilyName());
                        intent.putExtra(
                                AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER,
                                userinfo.getIdentityProvider());
                        intent.putExtra(
                                AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE,
                                userinfo.getDisplayableId());
                    }
                    
                    returnResult(AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE, intent);
                } else {
                    returnError(ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN,
                            result.taskResult.getErrorDescription());
                }
            } else {
                Logger.v(TAG, "Token task has exception");
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
