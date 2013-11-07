/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
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

    public enum PromptBehavior {
        /**
         * Acquire token will prompt the user for credentials only when
         * necessary.
         */
        Auto,

        /**
         * The user will be prompted for credentials even if it is available in
         * the cache or in the form of refresh token. New acquired access token
         * and refresh token will be used to replace previous value. If Settings
         * switched to Auto, new request will use this latest token from cache.
         */
        Always,

        /**
         * Don't show UI
         */
        Never
    }

    private final static String TAG = "AuthenticationContext";

    private Context mAppContext;

    private String mAuthority;

    private boolean mValidateAuthority;
    
    private ITokenCacheStore mTokenCacheStore;

    private ITokenCacheStore mCache;

    private transient ActivityDelegate mActivityDelegate;

    private AuthenticationCallback<AuthenticationResult> mExternalCallback;

    /**
     * Instance validation related calls are serviced inside Discovery as a
     * module
     */
    private IDiscovery mDiscovery = new Discovery();

    /**
     * webrequest handler interface to test behaviors
     */
    private IWebRequestHandler mWebRequestHandler = new WebRequestHandler();

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
        mAppContext = appContext;
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

    public AuthenticationContext(Context appContext, String authority,
            boolean validateAuthority, ITokenCacheStore tokenCacheStore) {
        mAppContext = appContext;
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
        mAppContext = appContext;
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
            String redirectUri, String userId, AuthenticationCallback callback) {

        if (mAppContext == null) {
            throw new AuthenticationException(ADALError.DEVELOPER_APP_CONTEXT_IS_NOT_SET);
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

        setActivity(activity);
        setActivityDelegate(activity);
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
            AuthenticationCallback callback) {
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
            String redirectUri, PromptBehavior prompt, AuthenticationCallback callback) {
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
            AuthenticationCallback callback) {
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
            AuthenticationCallback callback) {
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
            AuthenticationCallback callback) {
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
                mExternalCallback.onError(new AuthenticationException(
                        ADALError.ON_ACTIVITY_RESULT_INTENT_NULL_));
                mExternalCallback = null;
            } else {
                Bundle extras = data.getExtras();
                if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL) {
                    // User cancelled the flow
                    mExternalCallback.onError(new AuthenticationCancelError());
                    mExternalCallback = null;
                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR) {
                    // Certificate error or similar
                    AuthenticationRequest errRequest = (AuthenticationRequest)extras
                            .getSerializable(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO);
                    String errCode = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE);
                    String errMessage = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE);

                    mExternalCallback.onError(new AuthenticationException(
                            ADALError.SERVER_INVALID_REQUEST, errCode + " " + errMessage));

                    mExternalCallback = null;

                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE) {
                    // Browser has the url and finished the processing to get token
                    final AuthenticationRequest authenticationRequest = (AuthenticationRequest)extras
                            .getSerializable(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO);
                    
                    String endingUrl = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL);

                    if (endingUrl.isEmpty()) {
                        Log.d(TAG, "Ending url is empty");
                        mExternalCallback
                                .onError(new IllegalArgumentException("Final url is empty"));
                        mExternalCallback = null;
                    } else {
                        Oauth oauthRequest = new Oauth(authenticationRequest, mWebRequestHandler);
                        Log.d(TAG, "Process url:" + endingUrl);
                        
                        oauthRequest.getToken(endingUrl, new AuthenticationCallback<AuthenticationResult>() {

                            @Override
                            public void onSuccess(AuthenticationResult result) {
                                setCachedResult(authenticationRequest, result);
                                mExternalCallback.onSuccess(result);
                                mExternalCallback = null;                                
                            }

                            @Override
                            public void onError(Exception exc) {
                                Log.d(TAG, "Error in processing code to get token");
                                mExternalCallback.onError(exc);
                                mExternalCallback = null;                                
                            }
                        });
                    }
                }
            }
        }
    }

    
    private void setCachedResult(AuthenticationRequest request, AuthenticationResult result) {
        // TODO Auto-generated method stub

    }

    interface ResponseCallback {
        public void onRequestComplete(HashMap<String, String> response);
    }

    private TokenCacheItem getCachedResult(CacheKey key) {
        return null;
    }

    // temp
    private static boolean isExpired(TokenCacheItem item) {
        return true;
    }

    /**
     * only gets token from activity defined in this package
     * 
     * @param activity
     * @param request
     * @param prompt
     * @param callback
     */
    private void acquireTokenLocal(Activity activity, final AuthenticationRequest request,
            PromptBehavior prompt, AuthenticationCallback callback) {

        // TODO: Check cached authorization object

        // TODO: refresh if required

        // start activity if other options are not available
        if (mExternalCallback == null) {
            mExternalCallback = callback;
        } else {
            throw new AuthenticationException(ADALError.DEVELOPER_ONLY_ONE_LOGIN_IS_ALLOWED);
        }

        setActivity(activity);
        setActivityDelegate(activity);
        if (!startAuthenticationActivity(request)) {
            throw new AuthenticationException(ADALError.DEVELOPER_ONLY_ONE_LOGIN_IS_ALLOWED);
        }
    }

    private String getRedirectFromPackage() {
        return mAppContext.getApplicationContext().getPackageName();
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

        ResolveInfo resolveInfo = mAppContext.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return false;
        }
        return true;
    }

    /**
     * get intent to start authentication activity
     * 
     * @param request
     * @return intent for autentication activity
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
