/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

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

    private Context mAppContext;

    private String mAuthority;

    private boolean mValidateAuthority;
    
    private ITokenCacheStore mTokenCacheStore;

    /**
     * Constructs context to use with known authority to get the token. It uses
     * default cache.
     * 
     * @param contextFromMainThread It needs to have handle to the context to
     *            use the SharedPreferences as a Default cache storage. It does
     *            not need to be activity.
     * @param authority Authority url to send code and token requests
     * @param validateAuthority validate authority before sending token request
     */
    public AuthenticationContext(Context contextFromMainThread, String authority,
            boolean validateAuthority) {

        mAppContext = contextFromMainThread;
        mAuthority = authority;
        mValidateAuthority = validateAuthority;
        mTokenCacheStore = new DefaultTokenCacheStore(contextFromMainThread);
    }

    /**
     * @param contextFromMainThread
     * @param authority
     * @param validateAuthority
     * @param cache Set to null if you don't want cache.
     */
    public AuthenticationContext(Context contextFromMainThread, String authority,
            boolean validateAuthority, ITokenCacheStore tokenCacheStore) {
        mAppContext = contextFromMainThread;
        mAuthority = authority;
        mValidateAuthority = validateAuthority;
        mTokenCacheStore = tokenCacheStore;
    }

    /**
     * It will verify the authority and use the given cache. If cache is null,
     * it will not use cache.
     * 
     * @param contextFromMainThread
     * @param authority
     * @param cache
     */
    public AuthenticationContext(Context contextFromMainThread, String authority,
            ITokenCacheStore tokenCacheStore) {
        mAppContext = contextFromMainThread;
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
     * @param activity
     * @param resource
     * @param clientId
     * @param redirectUri Optional. It will use package name info if not
     *            provided.
     * @param userId Optional.
     * @param callback
     */
    public void acquireToken(Activity activity, String resource, String clientId,
            String redirectUri, String userId, AuthenticationCallback callback) {
        throw new UnsupportedOperationException("come back later");
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
        throw new UnsupportedOperationException("come back later");
    }
}
