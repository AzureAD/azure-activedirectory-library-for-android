/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import java.util.HashMap;

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
     * webrequest handler interface to test behaviors
     */
    private IWebRequestHandler mWebRequest = new WebRequestHandler();

    private Activity mActivity;

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
     * SharedPrefenreces and handles synchronization by itself.
     * 
     * @return
     */
    public ITokenCacheStore getCache() {
        return mTokenCacheStore;
    }

    public String getAuthority() {
        return mAuthority;
    }

    /**
     * acquire Token will start interactive flow if needed. It checks the cache
     * to return existing result if not expired. It tries to use refresh token
     * if available. If it fails to get token with refresh token, it will remove
     * this refresh token from cache and fall back on the UI.
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

        if (mContext == null) {
            throw new AuthenticationException(ADALError.DEVELOPER_CONTEXT_IS_NOT_PROVIDED);
        }

        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        if (activity == null) {
            throw new IllegalArgumentException("activity");
        }

        if (resource == null) {
            throw new IllegalArgumentException("resource");
        }

        if (clientId == null) {
            throw new IllegalArgumentException("clientId");
        }

        String redirectInfo = redirectUri;
        if (StringExtensions.IsNullOrBlank(redirectInfo)) {
            redirectInfo = getRedirectFromPackage();
        }

        final AuthenticationRequest request = new AuthenticationRequest(mAuthority, resource,
                clientId, redirectInfo, userId);

        acquireTokenLocal(activity, request, PromptBehavior.Auto, callback);
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
        throw new UnsupportedOperationException("come back later");
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
     * @param prompt
     * @param callback
     */
    public void acquireToken(Activity activity, String resource, String clientId,
            String redirectUri, PromptBehavior prompt, AuthenticationCallback<AuthenticationResult> callback) {
        throw new UnsupportedOperationException("come back later");
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
     * @param prompt
     * @param extraQueryParameters
     * @param callback
     */
    public void acquireToken(Activity activity, String resource, String clientId,
            String redirectUri, PromptBehavior prompt, String extraQueryParameters,
            AuthenticationCallback<AuthenticationResult> callback) {
        throw new UnsupportedOperationException("come back later");
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
        throw new UnsupportedOperationException("come back later");
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
        throw new UnsupportedOperationException("come back later");
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
                        ADALError.ON_ACTIVITY_RESULT_INTENT_NULL_));
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
            final PromptBehavior prompt, AuthenticationCallback<AuthenticationResult> callback) {

        URL authorityUrl;
        try {
            authorityUrl = new URL(mAuthority);
        } catch (MalformedURLException e) {
            callback.onError(new AuthenticationException(
                    ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL));
            return;
        }

        final AuthenticationCallback<AuthenticationResult> externalCall = callback;

        // Runnable to post depending on validation flag
        final Runnable requestRunnable = new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "Token request runnable is started");
                AuthenticationResult cachedItem = getItemFromCache(request);
                if (prompt != PromptBehavior.Always && isValidCache(cachedItem)) {
                    externalCall.onSuccess(cachedItem);
                } else if (prompt != PromptBehavior.Always && isRefreshable(cachedItem)) {
                    refreshToken(activity, request, cachedItem, prompt, externalCall);
                } else {
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
                }
            }
        };

        if (mValidateAuthority) {
            validateAuthority(authorityUrl, requestRunnable, externalCall);
        } else {
            requestRunnable.run();
        }
    }

    protected boolean isRefreshable(AuthenticationResult cachedItem) {
        return cachedItem != null && !StringExtensions.IsNullOrBlank(cachedItem.getRefreshToken());
    }

    private boolean isValidCache(AuthenticationResult cachedItem) {
        if (cachedItem != null && !StringExtensions.IsNullOrBlank(cachedItem.getAccessToken())
                && isExpired(cachedItem.getExpiresOn())) {
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
     * refresh token if possible. if fails, call acquire token with prompt
     * always flag.
     * 
     * @param activity Activity to use in case refresh token does not succeed
     *            and prompt is not set to never.
     * @param request incoming request
     * @param refreshItem refresh token info
     * @param prompt if set to never, it should not attempt to launch
     *            authorization
     * @param externalCallback
     */
    private void refreshToken(final Activity activity, final AuthenticationRequest request,
            AuthenticationResult refreshItem, final PromptBehavior prompt,
            final AuthenticationCallback<AuthenticationResult> externalCallback) {

        Log.d(TAG, "Process refreshToken for " + request.getLogInfo());

        // Removes refresh token from cache, when this call is complete. Request
        // may be interrupted, if app is shutdown by user.
        Oauth oauthRequest = new Oauth(request, mWebRequest);
        oauthRequest.refreshToken(refreshItem.getRefreshToken(),
                new AuthenticationCallback<AuthenticationResult>() {

                    @Override
                    public void onSuccess(AuthenticationResult result) {

                        if (result == null
                                || StringExtensions.IsNullOrBlank(result.getAccessToken())) {

                            removeItemFromCache(request);
                            // TODO: This may cause an issue if refresh token
                            // request was delayed and user moved to another
                            // screen.
                            acquireTokenLocal(activity, request, prompt, externalCallback);
                        } else {
                            Log.v(TAG, "Refresh token is finished for " + request.getLogInfo());
                            setItemToCache(request, result);
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

    private void validateAuthority(final URL authorityUrl, final Runnable requestRunnable,
            final AuthenticationCallback<AuthenticationResult> externalCall) {
        if (mDiscovery != null) {
            mDiscovery.isValidAuthority(authorityUrl, new AuthenticationCallback<Boolean>() {

                @Override
                public void onSuccess(Boolean result) {
                    Log.v(TAG, "Instance validation is successfull. Result:" + result.toString());
                    if (result) {
                        requestRunnable.run();
                    } else {
                        Log.v(TAG,
                                "Call callback since instance is invalid:"
                                        + authorityUrl.toString());
                        externalCall.onError(new AuthenticationException(
                                ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE));
                    }
                }

                @Override
                public void onError(Exception exc) {
                    Log.e(TAG, "Instance validation returned error", exc);
                    externalCall.onError(exc);
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
     * Delegate to use for starting browser flow from activity's context
     */
    private interface ActivityDelegate {
        public void startActivityForResult(Intent intent, int requestCode);

        public void startActivity(Intent intent);

        public Activity getActivityContext();
    }
}
