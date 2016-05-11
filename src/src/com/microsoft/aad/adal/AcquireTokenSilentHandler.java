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

import com.microsoft.aad.adal.AuthenticationRequest.UserIdentifierType;

import android.content.Context;

/**
 * Internal class handling the detailed acquiretoken silent logic, including cache lookup and also
 * interact with web request.
 */
class AcquireTokenSilentHandler {
    private static final String TAG = AcquireTokenSilentHandler.class.getSimpleName();
    
    private final Context mContext;
    private final TokenCacheAccessor mTokenCacheAccessor;
    private final AuthenticationRequest mAuthRequest;
    
    private boolean mAttemptedWithFRT = false;
    private TokenCacheItem mMrrtTokenCacheItem;

    /**
     * TODO: need to remove. {@link HttpUrlConnectionFactory} provides the possibility to 
     * mock the real connection. Needs to update the class to make different response based on 
     * post message. 
     */
    private IWebRequestHandler mWebRequestHandler = null;
    
    /**
     * Constructor for {@link AcquireTokenSilentHandler}. 
     * {@link TokenCacheAccessor} could be null. If null, won't handle with cache. 
     */
    AcquireTokenSilentHandler(final Context context, final AuthenticationRequest authRequest,
            final TokenCacheAccessor tokenCacheAccessor) {
        if (context == null) {
            throw new IllegalArgumentException("context");
        }
        
        if (authRequest == null) {
            throw new IllegalArgumentException("authRequest");
        }
        
        mContext = context;
        mAuthRequest = authRequest;
        mTokenCacheAccessor = tokenCacheAccessor;

        mWebRequestHandler = new WebRequestHandler();
    }
    
    /**
     * Request for access token by looking up cache. . 
     */
    AuthenticationResult getAccessToken() throws AuthenticationException {
        // If mTokenCacheAccessor is null, won't handle with token cache lookup. 
        if (mTokenCacheAccessor == null) {
            return null;
        }
        
        TokenCacheItem tokenCacheItem = mTokenCacheAccessor.getRegularTokenCacheItemWithAT(mAuthRequest.getResource(), 
                mAuthRequest.getClientId(), getUser());
        
        if (tokenCacheItem == null 
                && lookUpCachedTokenWithNoUserForADFS()) {
            // ADFS doesn't return idtoken back. If app is talking to ADFS server, and provide us the userId
            // or displayableId, since we didn't get idtoken back, we don't store user as part of key in this
            // case. Try again to find token with no user.
            tokenCacheItem = mTokenCacheAccessor.getRegularTokenCacheItemWithAT(mAuthRequest.getResource(), 
                    mAuthRequest.getClientId(), null);
            // If we still don't have anything to use then we should try to find if we have a MRRT that matches
            if (tokenCacheItem == null) {
                return tryMRRT();
            }
        }
        
        if (isValidCache(tokenCacheItem)) {
            if (isUserMisMatch(tokenCacheItem)) {
                Logger.v(TAG, "User in retrieved token cache item does not match the user provided in the request.");
                throw new AuthenticationException(ADALError.AUTH_FAILED_USER_MISMATCH);
            }
            
            Logger.v(TAG, "Valid token cache item, return AT back.");
            return AuthenticationResult.createResult(tokenCacheItem);
        }
        
        return tryRT();
    }
    
    /**
     * Send token request with grant_type as refresh_token to token endpoint for getting new access token. 
     */
    AuthenticationResult acquireTokenWithRefreshToken(final String refreshToken) 
            throws AuthenticationException {
        Logger.v(TAG, "Try to get new access token with the found refresh token.", 
                mAuthRequest.getLogInfo(), null);
        
        // @note: The original intention for checking network connection before sending RT request is to avoid RT removal
        // from cache if user turned on airplane mode or similar case. However, the updated cache logic only remove RT when 
        // receiving oauth2 error invalid_grant from server. 
        // The only reason to keep it here is in case there is caller taking dependency on the below error code. 
        final DefaultConnectionService connectionService = new DefaultConnectionService(mContext);
        if (!connectionService.isConnectionAvailable()) {
            AuthenticationException authenticationException = new AuthenticationException(
                    ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE,
                    "Connection is not available to refresh token");
            Logger.w(TAG, "Connection is not available to refresh token", mAuthRequest.getLogInfo(),
                    ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE);
            
            throw authenticationException;
        }
        
        final AuthenticationResult result;
        try {
            final JWSBuilder jwsBuilder = new JWSBuilder();
            final Oauth2 oauthRequest = new Oauth2(mAuthRequest, mWebRequestHandler, jwsBuilder);
            result = oauthRequest.refreshToken(refreshToken);
            if (result != null && StringExtensions.IsNullOrBlank(result.getRefreshToken())) {
                Logger.v(TAG, "Refresh token is not returned or empty");
                result.setRefreshToken(refreshToken);
            }
        } catch (final IOException | AuthenticationException exc) {
            // Server side error or similar
            Logger.e(TAG, "Error in refresh token for request:" + mAuthRequest.getLogInfo(),
                    ExceptionExtensions.getExceptionMessage(exc), ADALError.AUTH_FAILED_NO_TOKEN,
                    exc);

            AuthenticationException authException = new AuthenticationException(
                    ADALError.AUTH_FAILED_NO_TOKEN, ExceptionExtensions.getExceptionMessage(exc),
                    exc);
            throw authException;
        }

        return result;
    }
    
    /**
     * For testing purpose, inject the dependency. 
     */
    void setWebRequestHandler(final IWebRequestHandler webRequestHandler) {
        this.mWebRequestHandler = webRequestHandler;
    }
    
    /**
     * Attempt to get new access token with regular RT. 
     */
    private AuthenticationResult tryRT() throws AuthenticationException {
        final String user = getUser();
        final TokenCacheItem regularRTItem = mTokenCacheAccessor.getRegularTokenCacheItemWithAT(mAuthRequest.getResource(), 
                mAuthRequest.getClientId(), user);
        
        if (regularRTItem == null) {
            Logger.v(TAG, "Regular token cache entry does not exist, try with MRRT.");
            return tryMRRT(); 
        }
        
        // When MRRT is returned, we store separate entries for both regular RT entry and MRRT entry, 
        // we should look for MRRT entry if the token in regular RT entry is marked as MRRT.
        // However, the current cache implementation never mark the token stored in regular RT entry
        // as MRRT. To support the backward compatibility and improve cache lookup, when successfully
        // retrieved regular RT entry token and if the mrrt flag is false, check the existence of MRRT.
        if (regularRTItem.getIsMultiResourceRefreshToken() || isMRRTExisted()) {
            Logger.v(TAG, "Regular token cache entry exists but it's marked as MRRT, try with MRRT.");
            return tryMRRT();
        }
        
        Logger.v(TAG, "Send request to use regular RT for new AT.");
        return acquireTokenWithCachedItem(regularRTItem);
    }
    
    /**
     * Attempt to get new access token with MRRT. 
     */
    private AuthenticationResult tryMRRT() throws AuthenticationException {

        if (mMrrtTokenCacheItem == null) {
            // If we don't have MRRT item, try to get it from cache first
            mMrrtTokenCacheItem = mTokenCacheAccessor.getMRRTItem(mAuthRequest.getClientId(), getUser());
        }
        
        // ADFS doesn't return idtoken back. If app is talking to ADFS server, and provide us the userId
        // or displayableId, since we didn't get idtoken back, we don't store user as part of key in this
        // case. Try again to find token with no user.
        if (mMrrtTokenCacheItem == null) {
            if (lookUpCachedTokenWithNoUserForADFS()) {
                mMrrtTokenCacheItem = mTokenCacheAccessor.getMRRTItem(mAuthRequest.getClientId(), null);
                // ADFS doesn't support Family of Client Id, if we still cannot find anything, stop trying. 
                if (mMrrtTokenCacheItem == null) {
                    return null;
                }
            } else {
                Logger.v(TAG, "MRRT token does not exist, try with FRT");
                return tryFRT(AuthenticationConstants.MS_FAMILY_ID, null);
            }
        } 
        
        if (!StringExtensions.IsNullOrBlank(mMrrtTokenCacheItem.getFamilyClientId()) && !mAttemptedWithFRT) {
            Logger.v(TAG, "MRRT item exists but it's also a FRT, try with FRT.");
            return tryFRT(mMrrtTokenCacheItem.getFamilyClientId(), null);
        }
        
        Logger.v(TAG, "Send request to use MRRT for new AT.");
        AuthenticationResult mrrtResult = acquireTokenWithCachedItem(mMrrtTokenCacheItem);
        if (containOauthError(mrrtResult)) {
            // If MRRT fails, we still want to retry on FRT in case there is one there. 
            // MRRT may not be marked as FRT, hard-code it as "1" in this case. 
            final String familyClientId = StringExtensions.IsNullOrBlank(mMrrtTokenCacheItem.getFamilyClientId()) ? 
                    AuthenticationConstants.MS_FAMILY_ID : mMrrtTokenCacheItem.getFamilyClientId();

            // Rest mMrrtTokenCacheItem to avoid the fallback when FRT request fails.
            mMrrtTokenCacheItem = null;
            mrrtResult = tryFRT(familyClientId, mrrtResult);
        }
        
        return mrrtResult;
    }
    
    /**
     * Attempt to get access token with FRT. 
     */
    private AuthenticationResult tryFRT(final String familyClientId, final AuthenticationResult mrrtResult) 
            throws AuthenticationException {
        if (mAttemptedWithFRT) {
            return mrrtResult;
        }
        
        mAttemptedWithFRT = true;
        final TokenCacheItem frtTokenCacheItem = mTokenCacheAccessor.getFRTItem(familyClientId, getUser());
        
        if (frtTokenCacheItem  == null) {
            if (mMrrtTokenCacheItem != null) {
                // if mrrt token cache item is already used for token request, it will be reset as null
                Logger.v(TAG, "FRT cache item does not exist, fall back to try MRRT.");
                return tryMRRT();
            } else {
                // No FRT existed, and we've already tried to get new AT with MRRT, returning MRRT token request result in this case. 
                return mrrtResult;
            }
        }
        
        Logger.v(TAG, "Send request to use FRT for new AT.");
        AuthenticationResult frtResult = acquireTokenWithCachedItem(frtTokenCacheItem);
        if (containOauthError(frtResult)) {
            final AuthenticationResult retryMrrtResult = tryMRRT();
            frtResult = retryMrrtResult == null ? frtResult : retryMrrtResult;
        }
        
        return frtResult;
    }
    
    /**
     * Acquire token with retrieved token cache item and update cache. 
     */
    private AuthenticationResult acquireTokenWithCachedItem(final TokenCacheItem cachedItem)
            throws AuthenticationException {
        final AuthenticationResult result = acquireTokenWithRefreshToken(cachedItem.getRefreshToken());
        mTokenCacheAccessor.updateCachedItemToResult(mAuthRequest.getResource(), mAuthRequest.getClientId(), 
                result, cachedItem);

        return result;
    }
    
    /**
     * Old version of ADAL doesn't mark token stored in regular RT entry as MRRT even it is. The logic to look for 
     * MRRT is when RT is not found or found RT is also MRRT. To support the old behavior, do a separate check on
     * the existence for MRRT token entry. 
     */
    private boolean isMRRTExisted() {
        final TokenCacheItem mrrtItem = mTokenCacheAccessor.getMRRTItem(mAuthRequest.getClientId(), getUser());
        return mrrtItem != null && !StringExtensions.IsNullOrBlank(mrrtItem.getRefreshToken());
    }
    
    /**
     * Check if the {@link AuthenticationResult} contains oauth2 error. 
     */
    private boolean containOauthError(final AuthenticationResult result) {
        return result != null && !StringExtensions.IsNullOrBlank(result.getErrorCode());
    }
    
    /**
     * Check if the retrieved {@link TokenCacheItem} matches the user supplied in the request. 
     */
    private boolean isUserMisMatch(final TokenCacheItem result) {
        if (result.getUserInfo() != null
                && !StringExtensions.IsNullOrBlank(result.getUserInfo().getUserId())
                && !StringExtensions.IsNullOrBlank(mAuthRequest.getUserId())) {
            // Verify if IdToken is present and userid is specified
            return !mAuthRequest.getUserId().equalsIgnoreCase(result.getUserInfo().getUserId());
        }

        // it should verify loginhint as well if specified
        if (result.getUserInfo() != null
                && !StringExtensions.IsNullOrBlank(result.getUserInfo().getDisplayableId())
                && !StringExtensions.IsNullOrBlank(mAuthRequest.getLoginHint())) {
            // Verify if IdToken is present and userid is specified
            return !mAuthRequest.getLoginHint()
                    .equalsIgnoreCase(result.getUserInfo().getDisplayableId());
        }

        return false;
    }
    
    /**
     * Check if the retrieved cache item is valid. 
     */
    private boolean isValidCache(final TokenCacheItem cachedItem) {
        if (cachedItem != null && !StringExtensions.IsNullOrBlank(cachedItem.getAccessToken())
                && !cachedItem.isTokenExpired(cachedItem.getExpiresOn())) {
            return true;
        }

        return false;
    }
    
    /**
     * ADFS server doesn't return idtoken back. If id token is not returned, we won't be able to get 
     * userinfo back, ADAL then won't be able store token with displayableId or userId as cache key. 
     * In this case, when looking up token, we should try our best to find tokens with no user even user
     * pass the loginhint in the request. 
     */
    private boolean lookUpCachedTokenWithNoUserForADFS() {
        return UrlExtensions.isADFSAuthority(StringExtensions.getUrl(mAuthRequest.getAuthority()))
                && !StringExtensions.IsNullOrBlank(getUser());
    }
    
    /**
     * Get either loginhint or user id based what's passed in the request. 
     */
    private String getUser() {
        if (UserIdentifierType.LoginHint == mAuthRequest.getUserIdentifierType()) {
            return mAuthRequest.getLoginHint();
        } else if (UserIdentifierType.UniqueId == mAuthRequest.getUserIdentifierType()) {
            return mAuthRequest.getUserId();
        }
        
        return null;
    }
}
