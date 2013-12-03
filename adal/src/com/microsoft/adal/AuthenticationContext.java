/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import java.util.HashMap;
import java.util.UUID;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import com.microsoft.adal.ErrorCodes.ADALError;

/*
 */

/**
 * ADAL context to get access token, refresh token, and lookup from cache
 * 
 * @author omercan
 */
public class AuthenticationContext {

    private final static String TAG = "AuthenticationContext";

    private Context mContext;

    private String mAuthority;

    private boolean mValidateAuthority;

    private ITokenCacheStore mTokenCacheStore;

    private transient ActivityDelegate mActivityDelegate;

    /**
     * only one authorization can happen for user.
     */
    private AuthenticationCallback<AuthenticationResult> mAuthorizationCallback;

    /**
     * Instance validation related calls are serviced inside Discovery as a
     * module
     */
    private IDiscovery mDiscovery = new Discovery();

    /**
     * Web request handler interface to test behaviors
     */
    private IWebRequestHandler mWebRequest = new WebRequestHandler();

    private Activity mActivity;

    /**
     * Correlationid set by user
     */
    private UUID mRequestCorrelationId = null;

    /**
     * Constructs context to use with known authority to get the token. It uses
     * default cache.
     * 
     * @param appContext It needs to have handle to the context to use the
     *            SharedPreferences as a Default cache storage. It does not need
     *            to be activity.
     * @param authority Authority url to send code and token requests
     * @param validateAuthority validate authority before sending token request
     */
    public AuthenticationContext(Context appContext, String authority, boolean validateAuthority) {
        mContext = appContext;
        mAuthority = authority;
        mValidateAuthority = validateAuthority;
        mTokenCacheStore = new DefaultTokenCacheStore(appContext);
    }

    /**
     * @param appContext
     * @param authority
     * @param validateAuthority
     * @param cache Set to null if you don't want cache.
     */
    public AuthenticationContext(Context appContext, String authority, boolean validateAuthority,
            ITokenCacheStore tokenCacheStore) {
        mContext = appContext;
        mAuthority = authority;
        mValidateAuthority = validateAuthority;
        mTokenCacheStore = tokenCacheStore;
    }

    /**
     * It will verify the authority and use the given cache. If cache is null,
     * it will not use cache.
     * 
     * @param appContext
     * @param authority
     * @param cache
     */
    public AuthenticationContext(Context appContext, String authority,
            ITokenCacheStore tokenCacheStore) {
        mContext = appContext;
        mAuthority = authority;
        mValidateAuthority = true;
        mTokenCacheStore = tokenCacheStore;
    }

    /**
     * returns referenced cache. You can use default cache, which uses
     * SharedPreferences and handles synchronization by itself.
     * 
     * @return
     */
    public ITokenCacheStore getCache() {
        return mTokenCacheStore;
    }

    /**
     * gets authority that is used for this object of AuthenticationContext
     * 
     * @return
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * acquire Token will start interactive flow if needed. It checks the cache
     * to return existing result if not expired. It tries to use refresh token
     * if available. If it fails to get token with refresh token, it will remove
     * this refresh token from cache and start authentication.
     * 
     * @param activity required to launch authentication activity.
     * @param resource required resource identifier.
     * @param clientId required client identifier
     * @param redirectUri Optional. It will use package name info if not
     *            provided.
     * @param userId Optional.
     * @param callback required
     */
    public void acquireToken(Activity activity, String resource, String clientId,
            String redirectUri, String userId, AuthenticationCallback<AuthenticationResult> callback) {

        redirectUri = checkInputParameters(activity, resource, clientId, redirectUri, callback);

        final AuthenticationRequest request = new AuthenticationRequest(mAuthority, resource,
                clientId, redirectUri, userId, PromptBehavior.Auto, null);

        acquireTokenLocal(activity, request, callback);
    }

    /**
     * acquire Token will start interactive flow if needed. It checks the cache
     * to return existing result if not expired. It tries to use refresh token
     * if available. If it fails to get token with refresh token, it will remove
     * this refresh token from cache and fall back on the UI.
     * 
     * @param activity Calling activity
     * @param resource
     * @param clientId
     * @param redirectUri
     * @param userId Optional. This parameter will be used to pre-populate the
     *            username field in the authentication form. Please note that
     *            the end user can still edit the username field and
     *            authenticate as a different user. This parameter can be null.
     * @param extraQueryParameters Optional. This parameter will be appended as
     *            is to the query string in the HTTP authentication request to
     *            the authority. The parameter can be null.
     * @param callback
     */
    public void acquireToken(Activity activity, String resource, String clientId,
            String redirectUri, String userId, String extraQueryParameters,
            AuthenticationCallback<AuthenticationResult> callback) {

        redirectUri = checkInputParameters(activity, resource, clientId, redirectUri, callback);

        final AuthenticationRequest request = new AuthenticationRequest(mAuthority, resource,
                clientId, redirectUri, userId, PromptBehavior.Auto, extraQueryParameters);

        acquireTokenLocal(activity, request, callback);
    }

    /**
     * acquire Token will start interactive flow if needed. It checks the cache
     * to return existing result if not expired. It tries to use refresh token
     * if available. If it fails to get token with refresh token, behavior will
     * depend on options. If promptbehavior is AUTO, it will remove this refresh
     * token from cache and fall back on the UI.
     * If promptbehavior is NEVER, It will remove this refresh token from cache
     * and return error. Default is AUTO.
     * if promptbehavior is Always, it will display prompt screen.
     * 
     * @param activity
     * @param resource
     * @param clientId
     * @param redirectUri
     * @param prompt
     * @param callback
     */
    public void acquireToken(Activity activity, String resource, String clientId,
            String redirectUri, PromptBehavior prompt,
            AuthenticationCallback<AuthenticationResult> callback) {

        redirectUri = checkInputParameters(activity, resource, clientId, redirectUri, callback);

        final AuthenticationRequest request = new AuthenticationRequest(mAuthority, resource,
                clientId, redirectUri, null, prompt, null);

        acquireTokenLocal(activity, request, callback);
    }

    /**
     * acquire Token will start interactive flow if needed. It checks the cache
     * to return existing result if not expired. It tries to use refresh token
     * if available. If it fails to get token with refresh token, behavior will
     * depend on options. If promptbehavior is AUTO, it will remove this refresh
     * token from cache and fall back on the UI if activitycontext is not null.
     * If promptbehavior is NEVER, It will remove this refresh token from cache
     * and(or not, depending on the promptBehavior values. Default is AUTO.
     * 
     * @param activity
     * @param resource
     * @param clientId
     * @param redirectUri
     * @param prompt added as query parameter to authorization url
     * @param extraQueryParameters added to authorization url
     * @param callback
     */
    public void acquireToken(Activity activity, String resource, String clientId,
            String redirectUri, PromptBehavior prompt, String extraQueryParameters,
            AuthenticationCallback<AuthenticationResult> callback) {

        redirectUri = checkInputParameters(activity, resource, clientId, redirectUri, callback);

        final AuthenticationRequest request = new AuthenticationRequest(mAuthority, resource,
                clientId, redirectUri, null, prompt, extraQueryParameters);

        acquireTokenLocal(activity, request, callback);
    }

    private String checkInputParameters(Activity activity, String resource, String clientId,
            String redirectUri, AuthenticationCallback<AuthenticationResult> callback) {
        if (mContext == null) {
            throw new AuthenticationException(ADALError.DEVELOPER_CONTEXT_IS_NOT_PROVIDED);
        }

        if (activity == null) {
            throw new IllegalArgumentException("activity");
        }

        if (StringExtensions.IsNullOrBlank(resource)) {
            throw new IllegalArgumentException("resource");
        }

        if (StringExtensions.IsNullOrBlank(clientId)) {
            throw new IllegalArgumentException("clientId");
        }

        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        if (StringExtensions.IsNullOrBlank(redirectUri)) {
            redirectUri = getRedirectFromPackage();
        }
        
        return redirectUri;
    }

    /**
     * acquire token using refresh token if cache is not used. Otherwise, use
     * acquireToken to let the ADAL handle the cache lookup and refresh token
     * request.
     * 
     * @param refreshToken Required.
     * @param clientId Required.
     * @param callback Required
     */
    public void acquireTokenByRefreshToken(String refreshToken, String clientId,
            AuthenticationCallback<AuthenticationResult> callback) {
        refreshTokenWithoutCache(refreshToken, clientId, null, callback);
    }

    /**
     * acquire token using refresh token if cache is not used. Otherwise, use
     * acquireToken to let the ADAL handle the cache lookup and refresh token
     * request.
     * 
     * @param refreshToken Required.
     * @param clientId Required.
     * @param resource
     * @param callback Required
     */
    public void acquireTokenByRefreshToken(String refreshToken, String clientId, String resource,
            AuthenticationCallback<AuthenticationResult> callback) {
        refreshTokenWithoutCache(refreshToken, clientId, resource, callback);
    }

    /**
     * Call from your onActivityResult method inside your activity that started
     * token request. This is needed to process the call when credential screen
     * completes. This method wraps the implementation for onActivityResult at
     * the related Activity class.
     * 
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // This is called at UI thread.
        if (requestCode == AuthenticationConstants.UIRequest.BROWSER_FLOW) {
            if (data == null) {
                mAuthorizationCallback.onError(new AuthenticationException(
                        ADALError.ON_ACTIVITY_RESULT_INTENT_NULL));
                mAuthorizationCallback = null;
            } else {
                Bundle extras = data.getExtras();
                if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL) {
                    // User cancelled the flow
                    mAuthorizationCallback.onError(new AuthenticationCancelError());
                    mAuthorizationCallback = null;
                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR) {
                    String errCode = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE);
                    String errMessage = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE);

                    mAuthorizationCallback.onError(new AuthenticationException(
                            ADALError.SERVER_INVALID_REQUEST, errCode + " " + errMessage));

                    mAuthorizationCallback = null;

                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE) {
                    // Browser has the url and finished the processing to get
                    // token
                    final AuthenticationRequest authenticationRequest = (AuthenticationRequest)extras
                            .getSerializable(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO);

                    String endingUrl = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL);

                    if (endingUrl.isEmpty()) {
                        Log.d(TAG, "Ending url is empty");
                        mAuthorizationCallback.onError(new IllegalArgumentException(
                                "Final url is empty"));
                        mAuthorizationCallback = null;
                    } else {
                        Oauth oauthRequest = new Oauth(authenticationRequest, mWebRequest);
                        Log.d(TAG, "Process url:" + endingUrl);

                        oauthRequest.getToken(endingUrl,
                                new AuthenticationCallback<AuthenticationResult>() {

                                    @Override
                                    public void onSuccess(AuthenticationResult result) {
                                        setCachedResult(authenticationRequest, result);
                                        mAuthorizationCallback.onSuccess(result);
                                        mAuthorizationCallback = null;
                                    }

                                    @Override
                                    public void onError(Exception exc) {
                                        Log.d(TAG, "Error in processing code to get token");
                                        mAuthorizationCallback.onError(exc);
                                        mAuthorizationCallback = null;
                                    }
                                });
                    }
                }
            }
        }
    }

    private void setCachedResult(AuthenticationRequest request, AuthenticationResult result) {
        if (mTokenCacheStore != null) {
            mTokenCacheStore.setItem(new TokenCacheItem(request, result));
        }
    }

    interface ResponseCallback {
        public void onRequestComplete(HashMap<String, String> response);
    }

    /**
     * only gets token from activity defined in this package
     * 
     * @param activity
     * @param request
     * @param prompt
     * @param callback
     */
    private void acquireTokenLocal(final Activity activity, final AuthenticationRequest request,
            final AuthenticationCallback<AuthenticationResult> externalCall) {

        final URL authorityUrl;
        try {
            authorityUrl = new URL(mAuthority);
        } catch (MalformedURLException e) {
            externalCall.onError(new AuthenticationException(
                    ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL));
            return;
        }

        if (mValidateAuthority) {
            validateAuthority(authorityUrl, new AuthenticationCallback<Boolean>() {

                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        acquireTokenAfterValidation(activity, request, externalCall);
                    } else {
                        Log.v(TAG, "Call external callback since instance is invalid"
                                + authorityUrl.toString());
                        externalCall.onError(new AuthenticationException(
                                ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE));
                    }
                }

                @Override
                public void onError(Exception exc) {
                    externalCall.onError(new AuthenticationException(
                            ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE));
                }

            });
        } else {
            acquireTokenAfterValidation(activity, request, externalCall);
        }
    }

    private void acquireTokenAfterValidation(final Activity activity,
            final AuthenticationRequest request,
            final AuthenticationCallback<AuthenticationResult> externalCall) {
        Log.d(TAG, "Token request is started");

        AuthenticationResult cachedItem = getItemFromCache(request);
        if (request.getPrompt() != PromptBehavior.Always && isValidCache(cachedItem)) {
            externalCall.onSuccess(cachedItem);
        } else if (request.getPrompt() != PromptBehavior.Always && isRefreshable(cachedItem)) {
            refreshToken(activity, request, cachedItem.getRefreshToken(), true, externalCall);
        } else {

            if (request.getPrompt() != PromptBehavior.Never) {
                // start activity if other options are not available
                // Authorization has one reference of callback
                // Authentication callback does not have restrictions
                if (mAuthorizationCallback == null) {
                    mAuthorizationCallback = externalCall;
                } else {
                    Log.e(TAG, "Webview is active for another session");
                    externalCall.onError(new AuthenticationException(
                            ADALError.DEVELOPER_ONLY_ONE_LOGIN_IS_ALLOWED));
                    return;
                }

                // Set Activity for authorization flow
                setActivity(activity);
                setActivityDelegate(activity);

                if (!startAuthenticationActivity(request)) {
                    mAuthorizationCallback.onError(new AuthenticationException(
                            ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED));
                }
            } else {
                // it can come here if user set to never for the prompt and
                // refresh token failed.
                mAuthorizationCallback.onError(new AuthenticationException(
                        ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED));

            }
        }
    }

    protected boolean isRefreshable(AuthenticationResult cachedItem) {
        return cachedItem != null && !StringExtensions.IsNullOrBlank(cachedItem.getRefreshToken());
    }

    private boolean isValidCache(AuthenticationResult cachedItem) {
        if (cachedItem != null && !StringExtensions.IsNullOrBlank(cachedItem.getAccessToken())
                && !isExpired(cachedItem.getExpiresOn())) {
            return true;
        }
        return false;
    }

    private boolean isExpired(Date expires) {
        Date validity = getCurrentTime().getTime();

        if (expires != null && expires.before(validity))
            return true;

        return false;
    }

    private static Calendar getCurrentTime() {
        Calendar timeAhead = Calendar.getInstance();
        return timeAhead;
    }

    private AuthenticationResult getItemFromCache(final AuthenticationRequest request) {
        if (mTokenCacheStore != null) {
            TokenCacheItem item = mTokenCacheStore.getItem(CacheKey.createCacheKey(request));

            if (item != null) {
                AuthenticationResult result = new AuthenticationResult(item.getAccessToken(),
                        item.getRefreshToken(), item.getExpiresOn(),
                        item.getIsMultiResourceRefreshToken());
                return result;
            }
        }
        return null;
    }

    class CacheRequest {
        boolean result = false;

        Runnable runner;
    }

    private void setItemToCache(final AuthenticationRequest request, AuthenticationResult result)
            throws AuthenticationException {
        if (mTokenCacheStore != null) {
            mTokenCacheStore.setItem(new TokenCacheItem(request, result));
        }
    }

    private void removeItemFromCache(final AuthenticationRequest request)
            throws AuthenticationException {
        if (mTokenCacheStore != null) {
            mTokenCacheStore.removeItem(CacheKey.createCacheKey(request));
        }
    }

    /**
     * refresh token if possible. if fails, call acquire token if prompt is
     * allowed.
     * 
     * @param activity Activity to use in case refresh token does not succeed
     *            and prompt is not set to never.
     * @param request incoming request
     * @param refreshToken refresh token
     * @param prompt if set to never, it should not attempt to launch
     *            authorization
     * @param externalCallback
     */
    private void refreshToken(final Activity activity, final AuthenticationRequest request,
            String refreshToken, final boolean useCache,
            final AuthenticationCallback<AuthenticationResult> externalCallback) {

        Log.d(TAG, "Process refreshToken for " + request.getLogInfo());

        // Removes refresh token from cache, when this call is complete. Request
        // may be interrupted, if app is shutdown by user.
        Oauth oauthRequest = new Oauth(request, mWebRequest);
        oauthRequest.refreshToken(refreshToken, new AuthenticationCallback<AuthenticationResult>() {

            @Override
            public void onSuccess(AuthenticationResult result) {

                if (result == null || StringExtensions.IsNullOrBlank(result.getAccessToken())) {

                    // remove item from cache to avoid same usage of
                    // refresh token. This may cause infinite loop if
                    // bad refresh token is not cleared.
                    if (useCache) {
                        removeItemFromCache(request);
                    }
                    
                    if (request.getPrompt() != PromptBehavior.Never) {
                        Log.w(TAG,
                                "Refresh token is failed and prompt is allowed for "
                                        + request.getLogInfo());
                        acquireTokenLocal(activity, request, externalCallback);
                    } else {
                        Log.w(TAG, "Refresh token is failed and prompt is NOT allowed for "
                                + request.getLogInfo());
                        externalCallback.onError(new AuthenticationException(
                                ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED));
                    }
                } else {
                    Log.v(TAG, "Refresh token is finished for " + request.getLogInfo());
                    if (useCache) {
                        setItemToCache(request, result);
                    }
                    externalCallback.onSuccess(result);
                }
            }

            @Override
            public void onError(Exception exc) {
                // remove item from cache
                removeItemFromCache(request);
                externalCallback.onError(exc);
            }
        });
    }

    private void validateAuthority(final URL authorityUrl,
            final AuthenticationCallback<Boolean> authenticationCallback) {

        if (mDiscovery != null) {
            mDiscovery.isValidAuthority(authorityUrl, new AuthenticationCallback<Boolean>() {

                @Override
                public void onSuccess(Boolean result) {
                    Log.v(TAG, "Instance validation is successfull. Result:" + result.toString());
                    authenticationCallback.onSuccess(result);
                }

                @Override
                public void onError(Exception exc) {
                    Log.e(TAG, "Instance validation returned error", exc);
                    authenticationCallback.onError(exc);
                }
            });

        }
    }

    private String getRedirectFromPackage() {
        return mContext.getApplicationContext().getPackageName();
    }

    private Activity getActivity() {
        return this.mActivity;
    }

    private void setActivity(Activity activity) {
        this.mActivity = activity;
    }

    private ActivityDelegate getActivityDelegate() {
        return mActivityDelegate;
    }

    /**
     * delegate to start activity
     * 
     * @param context
     */
    private void setActivityDelegate(Activity activity) {
        final Activity callingActivity = activity;
        mActivityDelegate = new ActivityDelegate() {
            @Override
            public void startActivityForResult(Intent intent, int requestCode) {
                Log.d(TAG, "Delegate calling startActivityForResult");
                callingActivity.startActivityForResult(intent, requestCode);
            }

            @Override
            public Activity getActivityContext() {
                return callingActivity;
            }

            @Override
            public void startActivity(Intent intent) {
                Log.d(TAG, "Delegate calling startActivity");
                callingActivity.startActivity(intent);
            }
        };
    }

    /**
     * @param request
     * @return false: if intent is not resolved or error in starting. true: if
     *         intent is sent to start the activity.
     */
    private boolean startAuthenticationActivity(AuthenticationRequest request) {
        Intent intent = getAuthenticationActivityIntent(request);

        if (!resolveIntent(intent)) {
            return false;
        }

        try {
            // Start activity from callers context so that caller can intercept
            // when it is done
            getActivityDelegate().startActivityForResult(intent,
                    AuthenticationConstants.UIRequest.BROWSER_FLOW);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Activity login is not found after resolving intent");
            return false;
        }

        return true;
    }

    /**
     * Resolve activity from the package. If developer did not declare the
     * activity, it will not resolve.
     * 
     * @param intent
     * @return true if activity is defined in the package.
     */
    final private boolean resolveIntent(Intent intent) {

        ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return false;
        }
        return true;
    }

    /**
     * get intent to start authentication activity
     * 
     * @param request
     * @return intent for authentication activity
     */
    final private Intent getAuthenticationActivityIntent(AuthenticationRequest request) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), AuthenticationActivity.class);
        intent.putExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE, request);
        return intent;
    }

    /**
     * get the CorrelationId set by user
     * 
     * @return
     */
    public UUID getRequestCorrelationId() {
        return mRequestCorrelationId;
    }

    /**
     * set correlationid to requests
     * 
     * @param mRequestCorrelationId
     */
    public void setRequestCorrelationId(UUID mRequestCorrelationId) {
        this.mRequestCorrelationId = mRequestCorrelationId;
    }

    /**
     * Delegate to use for starting browser flow from activity's context
     */
    private interface ActivityDelegate {
        public void startActivityForResult(Intent intent, int requestCode);

        public void startActivity(Intent intent);

        public Activity getActivityContext();
    }

    /**
     * Developer is using refresh token call to do refresh without cache usage.
     * App context or activity is not needed. Async requests are created,so this
     * needs to be called at UI thread.
     */
    private void refreshTokenWithoutCache(final String refreshToken, String clientId,
            String resource, final AuthenticationCallback<AuthenticationResult> callback) {
        Log.v(TAG, "Refresh token request");

        if (mAuthority == null) {
            throw new IllegalArgumentException("Authority is not provided");
        }

        if (StringExtensions.IsNullOrBlank(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is not provided");
        }

        if (StringExtensions.IsNullOrBlank(clientId)) {
            throw new IllegalArgumentException("ClientId is not provided");
        }

        if (callback == null) {
            throw new IllegalArgumentException("Callback is not provided");
        }

        final URL authorityUrl;
        try {
            authorityUrl = new URL(mAuthority);
        } catch (MalformedURLException e) {
            Logger.e(TAG, "Authority is invalid:" + mAuthority, null,
                    ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL);
            callback.onError(new AuthenticationException(
                    ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL));
            return;
        }

        final AuthenticationRequest request = new AuthenticationRequest(mAuthority, null, clientId,
                null, null);
        // It is not using cache and refresh is not expected to show
        // authentication activity.
        request.setPrompt(PromptBehavior.Never);

        if (mValidateAuthority) {
            Log.v(TAG, "Validating authority");
            validateAuthority(authorityUrl, new AuthenticationCallback<Boolean>() {

                // These methods are called at UI thread. Async Task calls the
                // callback at onPostExecute which happens at UI thread.
                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        // it does one attempt
                        refreshToken(null, request, refreshToken, false, callback);
                    } else {
                        Log.v(TAG,
                                "Call callback since instance is invalid:"
                                        + authorityUrl.toString());
                        callback.onError(new AuthenticationException(
                                ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE));
                    }
                }

                @Override
                public void onError(Exception exc) {
                    callback.onError(exc);
                }
            });
        } else {
            Log.v(TAG, "Skip authority validation");
            refreshToken(null, request, refreshToken, false, callback);
        }
    }
}
