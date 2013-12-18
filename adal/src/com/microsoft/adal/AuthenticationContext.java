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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import android.util.SparseArray;

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

    private boolean mAuthorityValidated = false;

    private ITokenCacheStore mTokenCacheStore;

    private final static ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private final static Lock readLock = rwl.readLock();

    private final static Lock writeLock = rwl.writeLock();

    /**
     * delegate map is needed to handle activity recreate without asking
     * developer to handle context instance for config changes.
     */
    static SparseArray<AuthenticationRequestState> mDelegateMap = new SparseArray<AuthenticationRequestState>();

    /**
     * last set authorization callback
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

    /**
     * CorrelationId set by user
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

    public boolean getValidateAuthority() {
        return mValidateAuthority;
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
     * @param redirectUri Optional. It will use packagename and provided suffix
     *            for this.
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
     * token from cache and fall back on the UI. If promptbehavior is NEVER, It
     * will remove this refresh token from cache and return error. Default is
     * AUTO. if promptbehavior is Always, it will display prompt screen.
     * 
     * @param activity
     * @param resource
     * @param clientId
     * @param redirectUri Optional. It will use packagename and provided suffix
     *            for this.
     * @param prompt Optional. added as query parameter to authorization url
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
     * @param redirectUri Optional. It will use packagename and provided suffix
     *            for this.
     * @param prompt Optional. added as query parameter to authorization url
     * @param extraQueryParameters Optional. added to authorization url
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
     * @param redirectUri Optional. It will use packagename and provided suffix
     *            for this.
     * @param userId Optional. It is used for cache and as a loginhint at
     *            authentication.
     * @param prompt Optional. added as query parameter to authorization url
     * @param extraQueryParameters Optional. added to authorization url
     * @param callback
     */
    public void acquireToken(Activity activity, String resource, String clientId,
            String redirectUri, String userId, PromptBehavior prompt, String extraQueryParameters,
            AuthenticationCallback<AuthenticationResult> callback) {

        redirectUri = checkInputParameters(activity, resource, clientId, redirectUri, callback);

        final AuthenticationRequest request = new AuthenticationRequest(mAuthority, resource,
                clientId, redirectUri, userId, prompt, extraQueryParameters);

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
                // If data is null, RequestId is unknown. It could not find
                // callback to respond to this request.
                Logger.e(TAG, "onActivityResult BROWSER_FLOW data is null", null,
                        ADALError.ON_ACTIVITY_RESULT_INTENT_NULL);
            } else {
                Bundle extras = data.getExtras();
                final int requestId = extras.getInt(AuthenticationConstants.Browser.REQUEST_ID);
                Logger.d(TAG, "onActivityResult RequestId:" + requestId);
                final AuthenticationRequestState waitingRequest = getWaitingRequest(requestId);

                if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL) {
                    // User cancelled the flow
                    waitingRequestOnError(waitingRequest, requestId,
                            new AuthenticationCancelError());

                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR) {
                    String errCode = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE);
                    String errMessage = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE);

                    waitingRequestOnError(waitingRequest, requestId, new AuthenticationException(
                            ADALError.SERVER_INVALID_REQUEST, errCode + " " + errMessage));

                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE) {
                    // Browser has the url and finished the processing to get
                    // token
                    final AuthenticationRequest authenticationRequest = (AuthenticationRequest)extras
                            .getSerializable(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO);

                    String endingUrl = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL);

                    if (endingUrl.isEmpty()) {
                        Log.d(TAG, "Ending url is empty");
                        waitingRequestOnError(waitingRequest, requestId,
                                new IllegalArgumentException("Final url is empty"));

                    } else {
                        Oauth2 oauthRequest = new Oauth2(authenticationRequest, mWebRequest);
                        Log.d(TAG, "Process url:" + endingUrl);

                        oauthRequest.getToken(endingUrl,
                                new AuthenticationCallback<AuthenticationResult>() {

                                    @Override
                                    public void onSuccess(AuthenticationResult result) {
                                        setItemToCache(authenticationRequest, result);
                                        if (waitingRequest != null
                                                && waitingRequest.mDelagete != null) {
                                            Log.v(TAG, "Sending result to callback...");
                                            waitingRequest.mDelagete.onSuccess(result);
                                        }
                                        removeWaitingRequest(requestId);
                                    }

                                    @Override
                                    public void onError(Exception exc) {
                                        Log.d(TAG, "Error in processing code to get token");
                                        waitingRequestOnError(waitingRequest, requestId, exc);
                                    }
                                });
                    }
                }
            }
        }
    }

    private void waitingRequestOnError(final AuthenticationRequestState waitingRequest,
            int requestId, Exception exc) {
        if (waitingRequest != null && waitingRequest.mDelagete != null) {
            Log.v(TAG, "Sending error to callback...");
            waitingRequest.mDelagete.onError(exc);
        }
        removeWaitingRequest(requestId);
    }

    private void removeWaitingRequest(int requestId) {
        Log.v(TAG, "Remove Waiting Request: " + requestId);

        writeLock.lock();
        try {
            mDelegateMap.remove(requestId);
        } finally {
            writeLock.unlock();
        }
    }

    private AuthenticationRequestState getWaitingRequest(int requestId) {

        Log.v(TAG, "Get Waiting Request: " + requestId);
        AuthenticationRequestState request = null;

        readLock.lock();
        try {
            request = mDelegateMap.get(requestId);
        } finally {
            readLock.unlock();
        }

        if (request == null && mAuthorizationCallback != null
                && requestId == mAuthorizationCallback.hashCode()) {
            // it does not have the caller callback. It will check the last
            // callback if set
            Logger.e(TAG, "Request callback is not available for requestid:" + requestId
                    + ". It will use last callback.", "", ADALError.CALLBACK_IS_NOT_FOUND);
            request = new AuthenticationRequestState(0, null, mAuthorizationCallback);
        }

        return request;
    }

    private void putWaitingRequest(int requestId, AuthenticationRequestState requestState) {
        Log.v(TAG, "Put Waiting Request: " + requestId);
        if (requestId > 0 && requestState != null) {
            writeLock.lock();

            try {
                mDelegateMap.put(requestId, requestState);
            } finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * Active authentication activity can be cancelled if it exists. It may not
     * be cancelled if activity is not launched yet. RequestId is the hashcode
     * of your AuthenticationCallback.
     * 
     * @return true: if there is a valid waiting request and cancel message send
     *         successfully. false: Request does not exist or cancel message not
     *         send
     */
    public boolean cancelAuthenticationActivity(int requestId) {

        AuthenticationRequestState request = getWaitingRequest(requestId);

        if (request == null || request.mDelagete == null) {
            // there is not any waiting callback
            Log.v(TAG, "Current callback is empty. There is not any active authentication.");
            return true;
        }

        Log.v(TAG, "Current callback is not empty. There is an active authentication.");

        // intent to cancel. Authentication activity registers for this message
        // at onCreate event.
        final Intent intent = new Intent(AuthenticationConstants.Browser.ACTION_CANCEL);
        final Bundle extras = new Bundle();
        intent.putExtras(extras);
        intent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, requestId);
        // send intent to cancel any active authentication activity.
        // it may not cancel it, if activity takes some time to launch.

        boolean cancelResult = LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        if (cancelResult) {
            // clear callback if broadcast message was successful
            Log.v(TAG, "Cancel broadcast message was successful.");
            request.mCancelled = true;
            request.mDelagete.onError(new AuthenticationCancelError());
        } else {
            // Activity is not launched yet or receiver is not registered
            Log.w(TAG, "Cancel broadcast message was not successful.");
        }

        return cancelResult;
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

        if (mValidateAuthority && !mAuthorityValidated) {
            validateAuthority(authorityUrl, new AuthenticationCallback<Boolean>() {

                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        mAuthorityValidated = true;
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

        // Lookup access token from cache
        AuthenticationResult cachedItem = getItemFromCache(request);
        if (request.getPrompt() != PromptBehavior.Always && isValidCache(cachedItem)) {
            externalCall.onSuccess(cachedItem);
            return;
        }

        RefreshItem refreshItem = getRefreshToken(request);

        if (request.getPrompt() != PromptBehavior.Always && refreshItem != null
                && !StringExtensions.IsNullOrBlank(refreshItem.mRefreshToken)) {
            refreshToken(activity, request, refreshItem, true, externalCall);
        } else {

            if (request.getPrompt() != PromptBehavior.Never) {
                // start activity if other options are not available
                // delegate map is used to remember callback if another
                // instance of authenticationContext is created for config
                // change or similar at client app.
                mAuthorizationCallback = externalCall;
                request.setRequestId(externalCall.hashCode());
                Log.v(TAG, "Set hash code for callback:" + externalCall.hashCode());
                putWaitingRequest(externalCall.hashCode(), new AuthenticationRequestState(
                        externalCall.hashCode(), request, externalCall));

                if (!startAuthenticationActivity(activity, request)) {
                    mAuthorizationCallback.onError(new AuthenticationException(
                            ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED));
                }
            } else {
                // it can come here if user set to never for the prompt and
                // refresh token failed.
                externalCall.onError(new AuthenticationException(
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

    /**
     * get token from cache to return it, if not expired
     * 
     * @param request
     * @return
     */
    private AuthenticationResult getItemFromCache(final AuthenticationRequest request) {
        if (mTokenCacheStore != null) {
            // get token if resourceid matches to cache key.
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

    /**
     * If refresh token fails, this needs to be removed from cache to not use
     * this again for next try. Error in refreshToken call will result in
     * another call to acquireToken. It may try multi resource refresh token for
     * second attempt.
     */
    private class RefreshItem {
        String mRefreshToken;

        String mKey;

        public RefreshItem(String keyInCache, String refreshTokenValue) {
            this.mKey = keyInCache;
            this.mRefreshToken = refreshTokenValue;
        }
    }

    private RefreshItem getRefreshToken(final AuthenticationRequest request) {
        RefreshItem refreshItem = null;

        if (mTokenCacheStore != null) {
            // target refreshToken for this resource first. CacheKey will
            // include the resourceId in the cachekey
            String keyUsed = CacheKey.createCacheKey(request);
            TokenCacheItem item = mTokenCacheStore.getItem(keyUsed);

            if (item == null || StringExtensions.IsNullOrBlank(item.getRefreshToken())) {
                // if not present, check multiResource item in cache. Cache key
                // will not include resourceId in the cache key string.
                keyUsed = CacheKey.createMultiResourceRefreshTokenKey(request);
                item = mTokenCacheStore.getItem(keyUsed);
            }

            if (item != null && !StringExtensions.IsNullOrBlank(item.getRefreshToken())) {
                refreshItem = new RefreshItem(keyUsed, item.getRefreshToken());
            }
        }

        return refreshItem;
    }

    private void setItemToCache(final AuthenticationRequest request, AuthenticationResult result)
            throws AuthenticationException {
        if (mTokenCacheStore != null) {
            // Store token
            mTokenCacheStore.setItem(CacheKey.createCacheKey(request), new TokenCacheItem(request,
                    result, false));

            // Store broad refresh token if available
            if (result.getIsMultiResourceRefreshToken()) {
                mTokenCacheStore.setItem(CacheKey.createMultiResourceRefreshTokenKey(request),
                        new TokenCacheItem(request, result, true));
            }
        }
    }

    private void removeItemFromCache(final RefreshItem refreshItem) throws AuthenticationException {
        if (mTokenCacheStore != null) {
            Logger.v(TAG, "Remove refresh item from cache:" + refreshItem.mKey);
            mTokenCacheStore.removeItem(refreshItem.mKey);
        }
    }

    /**
     * refresh token if possible. if it fails, it calls acquire token after
     * removing refresh token from cache.
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
            final RefreshItem refreshItem, final boolean useCache,
            final AuthenticationCallback<AuthenticationResult> externalCallback) {

        Log.d(TAG, "Process refreshToken for " + request.getLogInfo());

        // Removes refresh token from cache, when this call is complete. Request
        // may be interrupted, if app is shutdown by user.

        Oauth2 oauthRequest = new Oauth2(request, mWebRequest);
        oauthRequest.refreshToken(refreshItem.mRefreshToken,
                new AuthenticationCallback<AuthenticationResult>() {

                    @Override
                    public void onSuccess(AuthenticationResult result) {

                        if (result == null
                                || StringExtensions.IsNullOrBlank(result.getAccessToken())) {

                            if (useCache) {
                                // remove item from cache to avoid same usage of
                                // refresh token in next acquireTokenLocal call
                                removeItemFromCache(refreshItem);
                                acquireTokenLocal(activity, request, externalCallback);
                            } else {
                                // User is not using cache and explicitly
                                // calling with refresh token
                                externalCallback.onError(new AuthenticationException(
                                        ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED));
                            }
                        } else {
                            Log.v(TAG, "Refresh token is finished for " + request.getLogInfo());
                            if (useCache) {
                                // it replaces multi resource refresh token as
                                // well with the new one since it is not stored
                                // with resource.
                                setItemToCache(request, result);
                            }

                            externalCallback.onSuccess(result);
                        }
                    }

                    @Override
                    public void onError(Exception exc) {
                        // remove item from cache
                        if (useCache) {
                            removeItemFromCache(refreshItem);
                        }

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

    /**
     * @param activity
     * @param request
     * @return false: if intent is not resolved or error in starting. true: if
     *         intent is sent to start the activity.
     */
    private boolean startAuthenticationActivity(final Activity activity,
            AuthenticationRequest request) {
        Intent intent = getAuthenticationActivityIntent(activity, request);

        if (!resolveIntent(intent)) {
            return false;
        }

        try {
            // Start activity from callers context so that caller can intercept
            // when it is done
            ActivityDelegate activityDelegate = new ActivityDelegate() {
                @Override
                public void startActivityForResult(Intent intent, int requestCode) {
                    Log.d(TAG, "Delegate calling startActivityForResult");
                    activity.startActivityForResult(intent, requestCode);
                }

                @Override
                public Activity getActivityContext() {
                    return activity;
                }

                @Override
                public void startActivity(Intent intent) {
                    Log.d(TAG, "Delegate calling startActivity");
                    activity.startActivity(intent);
                }
            };

            activityDelegate.startActivityForResult(intent,
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
    final private Intent getAuthenticationActivityIntent(Activity activity,
            AuthenticationRequest request) {
        Intent intent = new Intent();
        intent.setClass(activity, AuthenticationActivity.class);
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
     * set CorrelationId to requests
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

        final AuthenticationRequest request = new AuthenticationRequest(mAuthority, resource,
                clientId);
        // It is not using cache and refresh is not expected to show
        // authentication activity.
        request.setPrompt(PromptBehavior.Never);
        final RefreshItem refreshItem = new RefreshItem("", refreshToken);

        if (mValidateAuthority) {
            Log.v(TAG, "Validating authority");
            validateAuthority(authorityUrl, new AuthenticationCallback<Boolean>() {

                // These methods are called at UI thread. Async Task calls the
                // callback at onPostExecute which happens at UI thread.
                @Override
                public void onSuccess(Boolean result) {
                    if (result) {
                        // it does one attempt since it is not using cache
                        refreshToken(null, request, refreshItem, false, callback);
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
            refreshToken(null, request, refreshItem, false, callback);
        }
    }
}
