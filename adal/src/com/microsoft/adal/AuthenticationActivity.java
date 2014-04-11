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

package com.microsoft.adal;

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
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.google.gson.Gson;

/**
 * Activity to launch webview for authentication
 */
@SuppressLint("SetJavaScriptEnabled")
public class AuthenticationActivity extends Activity {

    private final String TAG = "AuthenticationActivity";

    private Button btnCancel;

    private boolean mRestartWebview = false;

    private WebView mWebView;

    private String mStartUrl;

    private ProgressDialog spinner;

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
        setContentView(R.layout.activity_authentication);

        // Get the message from the intent
        mAuthRequest = getAuthenticationRequestFromIntent(getIntent());
        if (mAuthRequest == null) {
            Log.d(TAG, "Request item is null, so it returns to caller");
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                    AuthenticationConstants.Browser.WEBVIEW_INVALID_REQUEST);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                    "Intent does not have request details");
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
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
        Log.d(TAG, "OnCreate redirect" + mRedirectUrl);

        setupWebView();
        Logger.v(TAG, "User agent:" + mWebView.getSettings().getUserAgentString());
        mStartUrl = "about:blank";

        try {
            mStartUrl = new Oauth2(mAuthRequest, null).getCodeRequestUrl();
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, e.getMessage());
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    mAuthRequest);
            ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
        }

        // Create the broadcast receiver for cancel
        Logger.v(TAG, "Init broadcastReceiver with requestId:" + mAuthRequest.getRequestId() + " "
                + mAuthRequest.getLogInfo());
        mReceiver = new ActivityBroadcastReceiver();
        mReceiver.mWaitingRequestId = mAuthRequest.getRequestId();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(AuthenticationConstants.Browser.ACTION_CANCEL));
        if (insideBroker()) {
            // This activity is started from calling app and running in
            // Authenticator's process
            mCallingPackage = getCallingPackage();
            if (mCallingPackage == null) {
                Log.d(TAG, "startActivityForResult is not used to call this activity");
                Intent resultIntent = new Intent();
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE,
                        AuthenticationConstants.Browser.WEBVIEW_INVALID_REQUEST);
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                        "startActivityForResult is not used to call this activity");
                ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
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
            mStartUrl = getBrokerUrl(mStartUrl, mCallingPackage, signatureDigest);
            Logger.v(TAG,
                    "OnCreate redirectUrl:" + mRedirectUrl + " startUrl:" + mStartUrl
                            + " calling package:" + mCallingPackage + " signatureDigest:"
                            + signatureDigest + " current Context Package: " + getPackageName()
                            + " accountName:" + mAuthRequest.getBrokerAccountName() + " loginHint:"
                            + mAuthRequest.getLoginHint());
        }
        mRestartWebview = false;
        final String postUrl = mStartUrl;
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("about:blank");// load blank first
                mWebView.loadUrl(postUrl);
            }
        });
    }

    private void setupWebView() {
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
        mWebView = (WebView)findViewById(R.id.webView1);
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
        if (insideBroker()) {
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
     * Return error to caller and finish this activity
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

    private String getBrokerUrl(String loadUrl, String packageName, String signatureDigest) {
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

    private boolean insideBroker() {
        return getPackageName().equals(AuthenticationSettings.INSTANCE.getBrokerPackageName());
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
        // manager since activity is not visible
        if (mReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }
        mRestartWebview = true;
        // restart webview when it comes back from onresume
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

            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("about:blank");// load blank first
                    mWebView.loadUrl(postUrl);
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
    public void onBackPressed() {
        Logger.d(TAG, "Back button is pressed");

        // Ask user if they rally want to cancel the flow, if navigation is not
        // possible
        // User may navigated to another page and does not need confirmation to
        // go back to previous page.
        if (!mWebView.canGoBack()) {
            confirmCancelRequest();
        } else {
            // Don't use default back pressed action, since user can go back in
            // webview
            mWebView.goBack();
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
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Logger.d(TAG, "shouldOverrideUrlLoading:url=" + url);
            displaySpinner(true);
            if (url.startsWith(mRedirectUrl)) {
                Logger.v(TAG, "Webview reached redirecturl");
                if (!insideBroker()) {
                    // It is pointing to redirect. Final url can be processed to
                    // get the code or error.
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, url);
                    resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                            mAuthRequest);
                    ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE,
                            resultIntent);
                    view.stopLoading();
                    return true;
                } else {
                    displaySpinnerWithMessage(AuthenticationActivity.this.getResources().getString(
                            R.string.broker_processing));
                    view.stopLoading();

                    // do async task and show spinner while exchanging code for
                    // access token
                    new TokenTask(mWebRequestHandler, mAuthRequest, mCallingPackage, mCallingUID)
                            .execute(url);
                    return true;
                }
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
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Logger.v(TAG, "Page finished:" + url);
            displaySpinner(false);
            /*
             * Once web view is fully loaded,set to visible
             */
            mWebView.setVisibility(View.VISIBLE);
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

    private void displaySpinnerWithMessage(String msg) {
        if (!AuthenticationActivity.this.isFinishing() && spinner != null) {
            spinner.show();
            spinner.setTitle("Processing ");
            spinner.setMessage(msg);
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
        if (insideBroker() && mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
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
            Oauth2 oauthRequest = new Oauth2(mRequest, mRequestHandler);
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
                result.accountName = name;
                Logger.v(TAG, "Setting account. Account name: " + name + " package:"
                        + mCallingPackage + " calling app UID:" + mAppCallingUID);
                Account newaccount = new Account(name,
                        AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
                Bundle userdata = new Bundle();
                // First add account. Account may exists before from another
                // app. Result will be added to userdata if account exists.
                mAccountManager.addAccountExplicitly(newaccount, "nopass", userdata);

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
                String key = CacheKey.createCacheKey(mRequest);
                saveCacheKey(key, newaccount, mAppCallingUID);
                mAccountManager.setUserData(newaccount, getBrokerAppCacheKey(cryptoHelper, key),
                        encrypted);

                if (result.taskResult.getIsMultiResourceRefreshToken()) {
                    // ADAL stores MRRT refresh token separately
                    TokenCacheItem itemMRRT = new TokenCacheItem(mRequest, result.taskResult, true);
                    json = gson.toJson(itemMRRT);
                    encrypted = cryptoHelper.encrypt(json);
                    key = CacheKey.createMultiResourceRefreshTokenKey(mRequest);
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
                Logger.v(TAG, "keylist:"+keylist);
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
                            result.taskResult.getUserInfo().getIsUserIdDisplayable());
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
