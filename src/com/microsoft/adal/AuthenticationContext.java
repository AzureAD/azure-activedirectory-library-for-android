/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.microsoft.adal.AuthenticationOptions.Endpoint;
import com.microsoft.adal.AuthenticationOptions.PromptBehavior;

/*
 *TODO: error messages define
 *TODO: serialization to restore the context onresume inside the client's app so that user does not need to authenticate again
 *TODO: Browser flow add
 *TODO: 
 */

/**
 * ADAL context to get access token, refresh token
 * 
 * @author omercan
 */
public class AuthenticationContext {

    /**
     * TAG to check messages
     */
    private final static String TAG = "AuthenticationContext";
    private final static String BROKER_APP_PACKAGE = "com.contoso.brokertest";
    private final static String BROKER_APP_TOKEN_ACTION = "android.intent.action.VIEW";
    static final int GET_AUTHORIZATION = 1;

    private String mAuthority;
    private String mClientId;
    private String mRedirectUri;
    private String mLoginHint;
    private String mBroadRefreshToken;
    private Context mContext;

    private transient ActivityDelegate mActivityDelegate;
    private transient AuthenticationCallback mExternalCallback;
    private HashMap<Endpoint, String> mExtraQueryParams;
    private PromptBehavior mPromptBehaviour;
    private boolean mValidateAuthority;

    /**
     * Delegate to use for starting browser flow from activity's context
     */
    public interface ActivityDelegate {
        public void startActivityForResult(Intent intent, int requestCode);

        public void startActivity(Intent intent);

        public Activity getActivityContext();
    }

    /**
     * Constructs context to use with known authority to get the token
     * 
     * @param contextFromMainThread It needs to have handle to the context to
     *            use the SharedPreferences as a Default cache storage.
     * @param authority Authority url to send code and token requests
     */
    public AuthenticationContext(Context contextFromMainThread, String authority)
    {
        setContext(contextFromMainThread);
        mAuthority = authority;
    }
    
    /**
     * constructs using context, authority url, and cache object
     * @param contextFromMainThread
     * @param authority
     * @param cache
     */
    public AuthenticationContext(Context contextFromMainThread, String authority, ITokenCache cache)
    {
        
    }
    
    public static ITokenCache getCache()
    {
        throw new UnsupportedOperationException();
    }

    

    /**
     * acquire Token will start interactive flow if needed. It checks the cache
     * to return existing result if not expired. It tries to use refresh token
     * if available. If it fails to get token with refresh token, it will remove
     * this refresh token from cache and return error without trying interactive
     * flow.
     * 
     * @param activityContext
     * @param clientId
     * @param resource
     * @param redirectUri Optional: If not set, it will use package info.
     * @param loginHint Optional: Username to pre-fill in the form.
     * @param options Optional: Prompt behaviour, Extra query strings
     * @param callback
     */
    public void acquireToken(Context activityContext, String clientId, String resource,
            String redirectUri, String loginHint, AuthenticationOptions options,
            AuthenticationCallback callback) {

        verifyParams(activityContext, callback);
        setContext(activityContext);
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mLoginHint = loginHint;
        setupOptions(options);
        boolean hasbroker = false;

        boolean askforinstall = options.getAskForBrokerDownload();
        boolean checkForBroker = options.getCheckForBrokerApp();

        if (checkForBroker)
        {
            hasbroker = appInstalledOrNot(BROKER_APP_PACKAGE);
        }

        setTokenActivityDelegate(activityContext);
        final AuthenticationRequest request = new AuthenticationRequest(this, clientId,
                redirectUri, resource);
        mExternalCallback = callback;

        if (checkForBroker)
        {
            if (hasbroker)
            {
                Log.d(TAG, "Device has the broker app");

                // Broker app needs to expose token activity to call from other apps
                Log.d(TAG, "start token activity");
                startTokenActivity(request);
            }
            else if (askforinstall)
            {
                Log.d(TAG, "Ask user to install broker app");
                askUserToInstallBroker();
            }
            else
            {
                Log.d(TAG, "User does not have broker and not required to install");
                acquireTokenLocal(activityContext, request, options, callback);
            }
        }
        else
        {
            // local token flow that uses storage within this process
            acquireTokenLocal(activityContext, request, options, callback);
        }
    }

    /**
     * Acquire token with provided credentials. It does not launch webview.
     * 
     * @param resource
     * @param credential
     * @param options
     * @param callback
     */
    public void acquireToken(String resource, ICredential credential,
            AuthenticationOptions options, AuthenticationCallback callback)
    {
        throw new UnsupportedOperationException("come back later");
    }

    /**
     * Acquire token with externally provided authorization code. You can use
     * full browser to get auth code or by other means.
     * 
     * @param activityContext
     * @param clientId
     * @param resource
     * @param redirectUri
     * @param loginHint
     * @param options
     * @param callback
     */
    public void acquireTokenByAuthorizationCode(String clientId, String resource,
            String redirectUri, String loginHint, AuthenticationOptions options,
            AuthenticationCallback callback)
    {
        throw new UnsupportedOperationException("come back later");
    }

    /**
     * Acquire token with externally provided authorization code. You can use
     * full browser to get auth code or by other means.
     * 
     * @param code
     * @param resource
     * @param credential
     * @param options
     * @param callback
     */
    public void acquireTokenByAuthorizationCode(String code, String resource,
            ICredential credential,
            AuthenticationOptions options, AuthenticationCallback callback)
    {
        throw new UnsupportedOperationException("come back later");
    }

    /**
     * acquire token using refresh code if cache is not used. Otherwise, use
     * acquireToken to let the ADAL handle the cache lookup and refresh token
     * request.
     * 
     * @param activityContext
     * @param clientId
     * @param resource
     * @param redirectUri
     * @param loginHint
     * @param options
     * @param callback
     */
    public void acquireTokenByRefreshCode(String clientId, String resource,
            String redirectUri, String loginHint, AuthenticationOptions options,
            AuthenticationCallback callback)
    {
        throw new UnsupportedOperationException("come back later");
    }

    /**
     * acquire token using refresh code if cache is not used. Otherwise, use
     * acquireToken to let the ADAL handle the cache lookup and refresh token
     * request.
     * 
     * @param code
     * @param resource
     * @param credential
     * @param options
     * @param callback
     */
    public void acquireTokenByRefreshCode(String code, String resource, ICredential credential,
            AuthenticationOptions options, AuthenticationCallback callback)
    {
        throw new UnsupportedOperationException("come back later");
    }

    /**
     * Blocking request to get token from cache. It does not do any refresh or
     * browser flow. It only checks cache and returns token if available.
     * 
     * @param clientid
     * @param resourceId
     * @return Access Token
     */
    public String getTokenFromCache(String clientid, String resourceId)
    {
        // TODO: check if clientid is null.
        ITokenCache cache = getCache();
        if (cache != null)
        {
            AuthenticationRequest request = new AuthenticationRequest(this, clientid, null,
                    resourceId);

            // Check cached authorization object
            final AuthenticationResult cachedResult = getCachedResult(request.getCacheKey());

            if (cachedResult != null) {
                if (!cachedResult.isExpired()) {
                    return cachedResult.getAccessToken();
                }
            }
        }
        return null;
    }

    /**
     * Remove tokens from cache and clear cookies. If clientid is not provided,
     * only resource is used to match tokens in cache.
     * 
     * @param clientId Optional to target tokens for one clientid.
     * @param resource
     */
    public void signOut(String clientId, String resource)
    {
        // TODO:
        // Clear all browser cookies
        if (getContext() != null)
        {
            CookieSyncManager.createInstance(getContext());
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null)
                cookieManager.removeAllCookie();
        }

        resetTokens(clientId, resource);
    }

    /**
     * Remove tokens from cache without touching cookies. Acquiring token by
     * credentials does not use browser flow. Application can clear existing
     * tokens to retry the token requests for backend services.
     * 
     * @param clientId filter by clientid if provided
     * @param resource filter by clientid(if provided) and then resource
     */
    public void resetTokens(String clientId, String resource)
    {
        // TODO:
        ITokenCache cache = getCache();
        if (cache != null)
        {
            cache.removeAll();
        }
    }

    /**
     * Broker related
     * @param clientId
     * @param resource
     */
    public void resetAllTokens()
    {
        ITokenCache cache = getCache();
        if (cache != null)
        {
            cache.removeAll();
        }
    }

    
    /**
     * Call from your onActivityResult method inside your activity that started
     * token request
     * 
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AuthenticationConstants.UIRequest.BROWSER_FLOW)
        {
            Bundle extras = data.getExtras();
            if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL)
            {
                mExternalCallback.onCancelled();
            } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR)
            {
                AuthenticationRequest errRequest = (AuthenticationRequest) extras
                        .getSerializable(AuthenticationConstants.BROWSER_RESPONSE_REQUEST_INFO);
                String errCode = extras
                        .getString(AuthenticationConstants.BROWSER_RESPONSE_ERROR_CODE);
                String errMessage = extras
                        .getString(AuthenticationConstants.BROWSER_RESPONSE_ERROR_MESSAGE);
                mExternalCallback.onError(new AuthException(errRequest, errCode, errMessage));
            } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE)
            {
                AuthenticationRequest authenticationRequest = (AuthenticationRequest) extras
                        .getSerializable(AuthenticationConstants.BROWSER_RESPONSE_REQUEST_INFO);

                String endingUrl = extras
                        .getString(AuthenticationConstants.BROWSER_RESPONSE_FINAL_URL);
                if (endingUrl.isEmpty())
                {
                    Log.d(TAG, "ending url is empty");
                    mExternalCallback.onError(new IllegalArgumentException("Final url is empty"));
                }

                Log.d(TAG, "Process this url");
                processUIResponse(authenticationRequest, endingUrl);
            }
        }
        else if (requestCode == AuthenticationConstants.UIRequest.TOKEN_FLOW)
        {
            Bundle extras = data.getExtras();
            if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL)
            {
                mExternalCallback.onCancelled();
            } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR)
            {
                AuthenticationRequest errRequest = (AuthenticationRequest) extras
                        .getSerializable(AuthenticationConstants.BROWSER_RESPONSE_REQUEST_INFO);
                String errCode = extras
                        .getString(AuthenticationConstants.BROWSER_RESPONSE_ERROR_CODE);
                String errMessage = extras
                        .getString(AuthenticationConstants.BROWSER_RESPONSE_ERROR_MESSAGE);
                mExternalCallback.onError(new AuthException(errRequest, errCode, errMessage));
            } else
            {
                AuthenticationResult resultback = (AuthenticationResult) data
                        .getSerializableExtra(AuthenticationConstants.BROKER_RESPONSE);
                if (resultback != null)
                {
                    mExternalCallback.onCompleted(resultback);
                }
                else
                {
                    mExternalCallback.onError(new Exception("hello damn..broker did not work"));
                }
            }
        }
    }

    /**
     * Broker related
     * @return
     */
    public HashMap<String, AuthenticationResult> getAllTokens()
    {
        if (getSettings().getEnableTokenCaching())
        {
            ITokenCache cache = getCache();
            if (cache != null)
            {
                return cache.getAllResults();
            }
        }
        
        return null;
    }
    
    
    /**
     * TODO: investigating broker call
     * @param activity
     * @param resource
     * @param correlationID
     * @param callback
     */
    private void acquireTokenLocal(Context activity, final AuthenticationRequest request,
            AuthenticationOptions options, AuthenticationCallback callback) {

        verifyParams(activity, callback);

        // Check cached authorization object
        final AuthenticationResult cachedResult = getCachedResult(request.getCacheKey());

        if (cachedResult != null && options.getPromptBehaviour() != PromptBehavior.Always) {
            if (!cachedResult.isExpired()) {
                callback.onCompleted(cachedResult);
            } else if (cachedResult.isRefreshable()) {
                // refreshToken will try to refresh. If it fails, it removes
                // authorization object and returns error
                refreshToken(cachedResult, request, callback);
            }
        } else {
            if (options.getShowLoginScreen())
            {
                mExternalCallback = callback;
                setTokenActivityDelegate(activity);
                setContext(activity.getApplicationContext());
                startLoginActivity(request);
                // Activity starts in the background and comes to foreground.
                // Any code after this will execute.
            }
            else
            {
                callback.onCompleted(null);
            }
        }

    }

    /**
     * Response listener for web requests
     */
    interface OnResponseListener {

        /**
         * Called when response is received
         * 
         * @param values on success, contains the values returned by the dialog
         * @param error on an error, contains an exception describing the error
         */
        void onComplete(HashMap<String, String> response);
    }

    /**
     * Callback to use for web ui login completed
     */
    interface WebLoginCallback {
        /**
         * Method to call if the operation finishes successfully
         * 
         * @param url The final login URL
         * @param e An exception
         */
        void onCompleted(String url, Exception exception);

        /*
         * User or UI cancelled login
         */
        void onCancelled();
    }

    public static AuthenticationSettings getSettings() {
        return AuthenticationSettings.getInstance();
    }

    public String getAuthority() {
        return mAuthority;
    }

    public String getClientId() {
        return mClientId;
    }

    public String getRedirectUri() {
        return mRedirectUri;
    }

    /*
     * Private methods
     */
    private void verifyParams(Context context, AuthenticationCallback callback) {
        if (callback == null)
            throw new IllegalArgumentException("listener is null");

        if (context == null)
            throw new IllegalArgumentException("context is null");

        // Check authority url
        ExtractUrl();

        // If user has enabled validation, it will call the discovery service to
        // verify the instance
        ValidateAuthority();
    }

    private void setTokenActivityDelegate(Context context)
    {
        final Context callingActivity = context;
        mActivityDelegate = new ActivityDelegate() {
            @Override
            public void startActivityForResult(Intent intent, int requestCode) {
                Log.d(TAG, "Delegate calling startActivityForResult");
                ((Activity) callingActivity).startActivityForResult(intent, requestCode);
            }

            @Override
            public Activity getActivityContext() {
                return (Activity) callingActivity;
            }

            @Override
            public void startActivity(Intent intent) {
                Log.d(TAG, "Delegate calling startActivity");
                callingActivity.startActivity(intent);
            }
        };
    }

    private ActivityDelegate getTokenActivityDelegate() {
        return mActivityDelegate;
    }

    private boolean startLoginActivity(AuthenticationRequest request) {
        Intent intent = getLoginActivityIntent(request);

        if (!resolveIntent(intent)) {
            return false;
        }

        try {
            // Start activity from callers context so that caller can intercept
            // when it is done
            getTokenActivityDelegate().startActivityForResult(intent,
                    AuthenticationConstants.UIRequest.BROWSER_FLOW);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "Activity login not found");
            return false;
        }

        return true;
    }

    /**
     * Resolve activity from the package
     * 
     * @param intent
     * @return
     */
    private boolean resolveIntent(Intent intent) {

        ResolveInfo resolveInfo = getContext().getPackageManager().resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return false;
        }
        return true;
    }

    private Intent getLoginActivityIntent(AuthenticationRequest request) {
        Intent intent = new Intent();
        intent.setClass(getContext(), LoginActivity.class);
        intent.putExtra(AuthenticationConstants.BROWSER_REQUEST_MESSAGE, request);
        return intent;
    }

    /*
     * It targets broker app's Token Activity. If it is not exposed in the
     * broker app, it will fail.
     */
    private Intent getTokenActivityIntent(AuthenticationRequest request) {
        Intent intent = new Intent(BROKER_APP_TOKEN_ACTION);
        intent.setPackage(BROKER_APP_PACKAGE);
        intent.putExtra(AuthenticationConstants.BROWSER_REQUEST_MESSAGE, request);
        return intent;
    }

    /**
     * Check if app installed on this device
     * 
     * @param uri
     * @return
     */
    private boolean appInstalledOrNot(String name)
    {
        PackageManager pm = getContext().getPackageManager();
        boolean app_installed = false;
        try
        {
            pm.getPackageInfo(name, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e)
        {
            app_installed = false;
        }
        return app_installed;
    }

    private void ExtractUrl()
    {
        // Authorization server URL is like
        // "https://login.windows.net/somewhere.onmicrosoft.com"
        // - must not be empty
        // - must be absolute
        // - must not have query or fragment
        // - must be https
        if (mAuthority == null || mAuthority.isEmpty())
            throw new IllegalArgumentException("authorizationServer");

        Uri uri = Uri.parse(mAuthority);

        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("authorizationServer");
        }
        if (!uri.getScheme().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("authorizationServer");
        }
        if (uri.getFragment() != null || uri.getQuery() != null) {
            throw new IllegalArgumentException("authorizationServer has query or fragments");
        }

        // Normalize authority url to remove extra url parts
        int thirdSlash = mAuthority.indexOf("/", 8); // exclude starting
                                                     // https:// or http://
        if (thirdSlash >= 0)
        {
            if (thirdSlash != (mAuthority.length() - 1))
            {
                // Extract url
                int fourthSlash = mAuthority.indexOf("/", thirdSlash + 1);
                if (fourthSlash > thirdSlash + 1)
                {
                    mAuthority.substring(0, fourthSlash);
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("Authority url");
        }
    }

    private void ValidateAuthority()
    {
        if (mValidateAuthority)
        {
            if (!getSettings().getDiscovery().IsValidAuthority(mAuthority))
            {
                throw new IllegalArgumentException("Authority is not valid");
            }
        }
    }

    /**
     * Refresh token based on cached result and targetresource.
     * 
     * @param cachedResult ClientId, Refreshtoken, RedirectUri are used from
     *            this result obj
     * @param request
     * @param targetResource Resource to ask for refresh token
     * @param callback Callback to be called for results
     */
    private void refreshToken(AuthenticationResult cachedResult, AuthenticationRequest request,
            AuthenticationCallback callback) {
        // Same authority
        HashMap<String, String> tokenRequestMessage = buildRefreshTokenRequestMessage(cachedResult,
                request);
        final AuthenticationCallback externalCallback = callback;
        final AuthenticationRequest fRequest = request;
        Log.d(TAG, "Calling sendrequest for refreshtoken");

        sendRequest(request.getTokenEndpoint(),
                tokenRequestMessage, new OnResponseListener() {
                    @Override
                    public void onComplete(HashMap<String, String> response) {
                        Log.d(TAG, "sendRequest onComplete");
                        AuthenticationResult result = processUIResponseParams(response, fRequest);

                        if (result.getStatus() == com.microsoft.adal.AuthenticationResult.AuthenticationStatus.Succeeded) {
                            setCachedResult(fRequest.getCacheKey(), result);
                            externalCallback.onCompleted(result);
                        } else {
                            // did not get token

                            externalCallback
                                    .onError(new AuthException(fRequest, result
                                            .getErrorCode(), result
                                            .getErrorDescription()));
                        }
                    }
                });
    }

    /*
     * Return cache from settings if it was set. Otherwise, return default impl.
     */
    private ITokenCache getCacheInternal()
    {
        // Default cache uses shared preferences and needs to be connected to
        // the context
        // Shared pref. handles synchronization

        ITokenCache cache = null;
        if (getSettings().getEnableTokenCaching())
        {
            cache = getSettings().getCache();
            if (cache == null)
            {
                // Context should be passed in
                if (getContext() == null)
                    throw new IllegalArgumentException("Context");

                cache = new TokenCache(getContext());
            }
        }

        return cache;
    }

    private AuthenticationResult getCachedResult(String cacheKey) {
        if (getSettings().getEnableTokenCaching())
        {
            if (getCacheInternal() != null)
            {
                return getCacheInternal().getResult(cacheKey);
            }
        }

        return null;
    }

    private void setCachedResult(String cacheKey, AuthenticationResult result) {
        if (getSettings().getEnableTokenCaching())
        {
            if (getCacheInternal() != null)
            {
                getCacheInternal().putResult(cacheKey, result);
            }
        }
    }

    private void removeCachedResult(String cacheKey) {
        if (getSettings().getEnableTokenCaching())
        {
            if (!getCacheInternal().removeResult(cacheKey))
            {
                if (!getCacheInternal().removeResult(cacheKey))
                {
                    Log.e(TAG, "Cache remove failed!");
                }
            }
        }
    }

    /**
     * TODO move token request message to outside Build token request message
     * which uses code to get token
     * 
     * @param code
     * @param result Authentication result which has code
     * @return Hashmap request params
     */
    private HashMap<String, String> buildTokenRequestMessage(
            AuthenticationRequest request, String code) {
        HashMap<String, String> reqParameters = new HashMap<String, String>();

        reqParameters.put(AuthenticationConstants.OAuth2.GRANT_TYPE,
                AuthenticationConstants.OAuth2.AUTHORIZATION_CODE);

        reqParameters.put(AuthenticationConstants.OAuth2.CODE, code);

        reqParameters.put(AuthenticationConstants.OAuth2.CLIENT_ID, request.getClientId());

        reqParameters.put(AuthenticationConstants.OAuth2.REDIRECT_URI, request.getRedirectUri());

        return reqParameters;
    }

    /*
     * If refresh token is broad refresh token, this request will include
     * resource in the message.
     */
    private HashMap<String, String> buildRefreshTokenRequestMessage(
            AuthenticationResult cachedResult, AuthenticationRequest request) {
        HashMap<String, String> reqParameters = new HashMap<String, String>();

        if (TextUtils.isEmpty(cachedResult.getRefreshToken()))
        {
            throw new IllegalArgumentException("Refresh token is required");
        }

        if (TextUtils.isEmpty(request.getRedirectUri()))
        {
            throw new IllegalArgumentException("RedirectUri is required");
        }

        if (TextUtils.isEmpty(request.getClientId()))
        {
            throw new IllegalArgumentException("ClientId is required");
        }

        reqParameters.put(AuthenticationConstants.OAuth2.GRANT_TYPE,
                AuthenticationConstants.OAuth2.REFRESH_TOKEN);

        reqParameters.put(AuthenticationConstants.OAuth2.REFRESH_TOKEN, cachedResult
                .getRefreshToken());

        reqParameters.put(AuthenticationConstants.OAuth2.REDIRECT_URI,
                request.getRedirectUri());

        reqParameters.put(AuthenticationConstants.OAuth2.CLIENT_ID,
                request.getClientId());

        if (cachedResult.IsBroadRefreshToken())
        {
            reqParameters.put(AuthenticationConstants.AAD.RESOURCE,
                    request.getResource());
        }
        return reqParameters;
    }

    /**
     * TODO: cleanup
     * 
     * @param endUrl
     */
    private void processUIResponse(AuthenticationRequest request, String endUrl) {
        // Success
        Uri response = Uri.parse(endUrl);
        HashMap<String, String> parameters = UriExtensions
                .getFragmentParameters(response);

        if (parameters == null || parameters.isEmpty())
            parameters = UriExtensions.getQueryParameters(response);

        // TODO: No encoded state
        String encodedState = parameters.get("state");
        String state = AuthenticationRequest.decodeProtocolState(encodedState);

        if (null != state && !state.isEmpty()) {
            // We have encoded state, crack it open
            Uri stateUri = Uri.parse("http://state/path?" + state);
            String authorizationUri = stateUri.getQueryParameter("a");
            String resource = stateUri.getQueryParameter("r");
            String scope = stateUri.getQueryParameter("s");

            // TODO add more verification for the state if needed
            if (null != authorizationUri && !authorizationUri.isEmpty()
                    && null != resource && !resource.isEmpty()) {

                // ToDo: if token, ask to token endpoint and post the message
                AuthenticationResult result = processUIResponseParams(
                        parameters, request);

                if (result.getStatus() == com.microsoft.adal.AuthenticationResult.AuthenticationStatus.Succeeded) {
                    if (!result.getCode().isEmpty()) {
                        // Need to get exchange code to get token
                        HashMap<String, String> tokenRequestMessage = buildTokenRequestMessage(
                                request, result.getCode());

                        final AuthenticationRequest requestInfo = request;

                        sendRequest(request.getTokenEndpoint(),
                                tokenRequestMessage, new OnResponseListener() {
                                    @Override
                                    public void onComplete(
                                            HashMap<String, String> response) {
                                        AuthenticationResult result = processUIResponseParams(
                                                response, requestInfo);

                                        if (result.getStatus() == com.microsoft.adal.AuthenticationResult.AuthenticationStatus.Succeeded) {
                                            setCachedResult(requestInfo
                                                    .getCacheKey(), result);
                                            mExternalCallback.onCompleted(result);
                                        } else {
                                            // did not get token
                                            mExternalCallback
                                                    .onError(new AuthException(requestInfo, result
                                                            .getErrorCode(), result
                                                            .getErrorDescription()));
                                        }
                                    }
                                });
                    }
                    else
                    {
                        // We have token directly with implicit flow

                    }
                }
            } else {

                mExternalCallback.onError(new AzureException(
                        AuthenticationConstants.AUTH_FAILED_BAD_STATE));
            }
        } else {
            // The response from the server had no state
            // information
            mExternalCallback.onError(new AzureException(
                    AuthenticationConstants.AUTH_FAILED_NO_STATE));
        }
    }

    /**
     * TODO: abstract sendrequest to set headers, method type, data Sends a
     * request with the specified parameters to the given endpoint TODO: add
     * correlationid headers
     */
    private static void sendRequest(final String endpoint,
            HashMap<String, String> requestData,
            final OnResponseListener callback) {
        HashMap<String, String> response = null;

        try {
            String body = HashMapExtensions.URLFormEncode(requestData);
            HttpWebRequest webRequest = new HttpWebRequest(new URL(endpoint));

            webRequest.getRequestHeaders().put("Accept", "application/json");

            webRequest.sendAsyncPost(
                    body.getBytes(AuthenticationConstants.ENCODING_UTF8),
                    "application/x-www-form-urlencoded",
                    new HttpWebRequestCallback() {
                        @Override
                        public void onComplete(Exception exception,
                                HttpWebResponse webResponse) {
                            HashMap<String, String> response = new HashMap<String, String>();
                            // Response should have Oauth2 token
                            if (exception != null) {
                                Log.e(TAG, ExceptionExtensions
                                        .getExceptionMessage(exception));

                                response.put(
                                        AuthenticationConstants.OAuth2.ERROR,
                                        AuthenticationConstants.AUTH_FAILED);
                                response.put(
                                        AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                                        ExceptionExtensions
                                                .getExceptionMessage(exception));
                            } else if (webResponse.getStatusCode() <= 400) {
                                try {
                                    JSONObject jsonObject = new JSONObject(
                                            new String(webResponse.getBody()));

                                    @SuppressWarnings("unchecked")
                                    Iterator<String> i = jsonObject.keys();

                                    while (i.hasNext()) {
                                        String key = i.next();

                                        response.put(key,
                                                jsonObject.getString(key));
                                    }
                                } catch (Exception ex) {
                                    // There is no recovery possible here, so
                                    // catch the generic Exception
                                    Log.e(TAG, ExceptionExtensions
                                            .getExceptionMessage(ex));

                                    response.put(
                                            AuthenticationConstants.OAuth2.ERROR,
                                            AuthenticationConstants.AUTH_FAILED);
                                    response.put(
                                            AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                                            ExceptionExtensions
                                                    .getExceptionMessage(ex));
                                }
                            } else {
                                response.put(
                                        AuthenticationConstants.OAuth2.ERROR,
                                        AuthenticationConstants.AUTH_FAILED);
                                response.put(
                                        AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                                        new String(webResponse.getBody()));
                            }

                            // Run the callback
                            Log.d(TAG, "Callback complete call");
                            callback.onComplete(response);
                        }
                    });
        } catch (Exception ex) {
            // There is no recovery possible here, so catch the generic
            // Exception
            Log.e(TAG, ExceptionExtensions.getExceptionMessage(ex));

            response = new HashMap<String, String>();
            response.put(AuthenticationConstants.OAuth2.ERROR,
                    AuthenticationConstants.AUTH_FAILED);
            response.put(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                    ExceptionExtensions.getExceptionMessage(ex));
        }

        Log.d(TAG, "sendRequest at the end");
        if (response != null) {
            callback.onComplete(response);
        }
    }

    private static AuthenticationResult processUIResponseParams(
            HashMap<String, String> response, AuthenticationRequest request) {

        AuthenticationResult result = new AuthenticationResult(request);

        if (response.containsKey(AuthenticationConstants.OAuth2.ERROR)) {
            // Error response from the server
            // TODO: Should we kill the authorization object?
            result = new AuthenticationResult(
                    response.get(AuthenticationConstants.OAuth2.ERROR),
                    response.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION));
        } else if (response.containsKey(AuthenticationConstants.OAuth2.CODE)) {
            // Code response
            Calendar expires = new GregorianCalendar();
            expires.add(Calendar.SECOND, 300); // TODO check .net ADAL for skew
                                               // time
            result.setAccessTokenType(null);
            result.setAccessTokenType(null);
            result.setCode(response
                    .get(AuthenticationConstants.OAuth2.CODE));
            result.setExpires(expires.getTime());
            result.setRefreshToken(null);
        } else if (response
                .containsKey(AuthenticationConstants.OAuth2.ACCESS_TOKEN)) {
            // Token response
            String expires_in = response.get("expires_in");
            Calendar expires = new GregorianCalendar();

            // Compute token expiration
            expires.add(
                    Calendar.SECOND,
                    expires_in == null || expires_in.isEmpty() ? AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC
                            : Integer.parseInt(expires_in));

            result.setAccessToken(response
                    .get(AuthenticationConstants.OAuth2.ACCESS_TOKEN));
            result.setAccessTokenType(response
                    .get(AuthenticationConstants.OAuth2.TOKEN_TYPE));
            result.setCode(null);
            result.setExpires(expires.getTime());

            if (response
                    .containsKey(AuthenticationConstants.OAuth2.REFRESH_TOKEN)) {
                result.setRefreshToken(response
                        .get(AuthenticationConstants.OAuth2.REFRESH_TOKEN));

                // TODO test broad refresh token
                // TODO how to use this broad token
                if (response.containsKey(AuthenticationConstants.AAD.RESOURCE))
                {
                    result.setBroadRefreshToken(true);
                }
            }

        } else {

            result = new AuthenticationResult(
                    AuthenticationConstants.AUTH_FAILED,
                    AuthenticationConstants.AUTH_FAILED_SERVER_ERROR);
        }

        return result;
    }

    private void startTokenActivity(AuthenticationRequest request) {
        // TODO Auto-generated method stub
        Intent intent = getTokenActivityIntent(request);

        if (!resolveIntent(intent)) {
            mExternalCallback.onError(new AuthException(request, "intent resolve failure",
                    "failed to resolve intent"));
        }

        try {
            // Start activity from callers context so that caller can intercept
            // when it is done
            getTokenActivityDelegate().startActivityForResult(intent,
                    AuthenticationConstants.UIRequest.TOKEN_FLOW);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "Activity login not found");
            mExternalCallback.onError(e);
        }
    }

    private void askUserToInstallBroker() {
        // implement prompt dialog asking user to download the package
        
        final AuthenticationCallback callbackToActivity = mExternalCallback;
        
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(getContext());
        downloadDialog.setTitle("Download that bro");
        downloadDialog.setMessage("Really get that now!");
        downloadDialog.setPositiveButton("yes",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        
                        Uri uri = Uri
                                .parse("market://search?q=pname:"+BROKER_APP_PACKAGE);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        try
                        {
                            // When user installs the app, their login process is interrupted
                            // TODO: think about how to handle this situation
                            callbackToActivity.onCompleted(null);
                            getTokenActivityDelegate().startActivity(intent);
                        }
                        catch (ActivityNotFoundException e)
                        {
                            Log.d(TAG, "ERROR Google Play Market not found!");
                            callbackToActivity.onError(e);
                        }
                    }
                });
        
        downloadDialog.setNegativeButton("no",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int i)
                    {
                        dialog.dismiss();
                        callbackToActivity.onCancelled();
                    }
                });
        downloadDialog.show();
    }

    private void setupOptions(AuthenticationOptions options)
    {
        if (options != null)
        {
            mValidateAuthority = options.getValidateAuthority();
            // add others
        }
    }

    private Context getContext() {
        return this.mContext;
    }
    
    private void setContext(Context context) {
        this.mContext = context;
    }
    
    
    public void updateContextConfigChange(Context mContext) {
        this.mContext = mContext;
        setTokenActivityDelegate(mContext);
    }
}
