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

import com.microsoft.aad.adal.AuthenticationResult.AuthenticationStatus;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal class handling the interaction with {@link AcquireTokenSilentHandler} and {@link ITokenCacheStore}. 
 */
class TokenCacheAccessor {
    private static final String TAG = TokenCacheAccessor.class.getSimpleName();
    
    private final ITokenCacheStore mTokenCacheStore;
    private final String mAuthority;
    private final String mTelemetryRequestId;
    
    TokenCacheAccessor(final ITokenCacheStore tokenCacheStore, final String authority, final String telemetryRequestId) {
        if (tokenCacheStore == null) {
            throw new IllegalArgumentException("tokenCacheStore");
        }
        
        if (StringExtensions.isNullOrBlank(authority)) {
            throw new IllegalArgumentException("authority");
        }

        if (StringExtensions.isNullOrBlank(telemetryRequestId)) {
            throw new IllegalArgumentException("requestId");
        }

        mTokenCacheStore = tokenCacheStore;
        mAuthority = authority;
        mTelemetryRequestId = telemetryRequestId;
    }
    
    /**
     * @return Access token from cache. Could be null if AT does not exist or expired. 
     * This will be a strict match with the user passed in, could be unique userid, 
     * displayable id, or null user. 
     * @throws AuthenticationException 
     */
    TokenCacheItem getATFromCache(final String resource, final String clientId, final String user) 
            throws AuthenticationException {
        final TokenCacheItem accessTokenItem = getRegularRefreshTokenCacheItem(resource, clientId, user);
        if (accessTokenItem == null) {
            Logger.v(TAG, "No access token exists.");
            return null;
        }
        
        if (!StringExtensions.isNullOrBlank(accessTokenItem.getAccessToken())) {
            if (TokenCacheItem.isTokenExpired(accessTokenItem.getExpiresOn())) {
                Logger.v(TAG, "Access token exists, but already expired.");
                return null;
            }
            
            // To support backward-compatibility, for old token entry, user stored in 
            // token cache item could be different from the one in cachekey. 
            if (isUserMisMatch(user, accessTokenItem)) {
                throw new AuthenticationException(ADALError.AUTH_FAILED_USER_MISMATCH);
            }
        }
        
        return accessTokenItem;
    }

    /**
     * @return {@link TokenCacheItem} for regular token cache entry.  
     */
    TokenCacheItem getRegularRefreshTokenCacheItem(final String resource, final String clientId, final String user) {
        final CacheEvent cacheEvent = startCacheTelemetryRequest(EventStrings.TOKEN_TYPE_RT);

        final String cacheKey = CacheKey.createCacheKeyForRTEntry(mAuthority, resource, clientId, user);
        final TokenCacheItem item =  mTokenCacheStore.getItem(cacheKey);

        if (item != null) {
            cacheEvent.setTokenTypeRT(true);
        }
        Telemetry.getInstance().stopEvent(mTelemetryRequestId, cacheEvent, EventStrings.TOKEN_CACHE_LOOKUP);

        return item;
    }
    
    /**
     * @return {@link TokenCacheItem} for MRRT token cache entry.  
     */
    TokenCacheItem getMRRTItem(final String clientId, final String user) {
        final CacheEvent cacheEvent = startCacheTelemetryRequest(EventStrings.TOKEN_TYPE_MRRT);
        final String cacheKey = CacheKey.createCacheKeyForMRRT(mAuthority, clientId, user);
        final TokenCacheItem item = mTokenCacheStore.getItem(cacheKey);

        if (item != null) {
            cacheEvent.setTokenTypeMRRT(true);
            cacheEvent.setTokenTypeFRT(item.isFamilyToken());
        }
        Telemetry.getInstance().stopEvent(mTelemetryRequestId, cacheEvent, EventStrings.TOKEN_CACHE_LOOKUP);

        return item;
    }
    
    /**
     * @return {@link TokenCacheItem} for FRT token cache entry.  
     */
    TokenCacheItem getFRTItem(final String familyClientId, final String user) {
        final CacheEvent cacheEvent = startCacheTelemetryRequest(EventStrings.TOKEN_TYPE_FRT);
        if (StringExtensions.isNullOrBlank(user)) {
            Telemetry.getInstance().stopEvent(mTelemetryRequestId, cacheEvent, EventStrings.TOKEN_CACHE_LOOKUP);
            return null;
        }
        
        final String cacheKey = CacheKey.createCacheKeyForFRT(mAuthority, familyClientId, user);
        final TokenCacheItem item = mTokenCacheStore.getItem(cacheKey);

        if (item != null) {
            cacheEvent.setTokenTypeFRT(true);
        }
        Telemetry.getInstance().stopEvent(mTelemetryRequestId, cacheEvent, EventStrings.TOKEN_CACHE_LOOKUP);

        return item;
    }

    TokenCacheItem getStaleToken(AuthenticationRequest authRequest) {
        final TokenCacheItem accessTokenItem = getRegularRefreshTokenCacheItem(authRequest.getResource(),
                authRequest.getClientId(), authRequest.getUserFromRequest());
        if (accessTokenItem != null
                && !StringExtensions.isNullOrBlank(accessTokenItem.getAccessToken())
                && accessTokenItem.getExtendedExpiresOn() != null
                && !TokenCacheItem.isTokenExpired(accessTokenItem.getExtendedExpiresOn())) {
            Logger.i(TAG, "The stale access token is returned.", "");
            return accessTokenItem;
        } 
        
        Logger.i(TAG, "The stale access token is not found.", "");
        return null;
    }

    /**
     * Update token cache with returned auth result.
     * @throws AuthenticationException 
     * @throws IllegalArgumentException If {@link AuthenticationResult} is null. 
     */
    void updateCachedItemWithResult(final String resource, final String clientId, final AuthenticationResult result, 
            final TokenCacheItem cachedItem) throws AuthenticationException {
        if (result == null) {
            Logger.v(TAG, "AuthenticationResult is null, cannot update cache.");
            throw new IllegalArgumentException("result");
        }
        
        if (result.getStatus() == AuthenticationStatus.Succeeded) {
            Logger.v(TAG, "Save returned AuthenticationResult into cache.");
            if (cachedItem != null && cachedItem.getUserInfo() != null && result.getUserInfo() == null) {
                result.setUserInfo(cachedItem.getUserInfo());
                result.setIdToken(cachedItem.getRawIdToken());
                result.setTenantId(cachedItem.getTenantId());
            }
            
            updateTokenCache(resource, clientId, result);
        } else if (AuthenticationConstants.OAuth2ErrorCode.INVALID_GRANT.equalsIgnoreCase(result.getErrorCode())) {
            // remove Item if oauth2_error is invalid_grant
            Logger.v(TAG, "Received INVALID_GRANT error code, remove existing cache entry.");
            removeTokenCacheItem(cachedItem, resource);
        }
    }
    
    /**
     * Update token cache with returned auth result.
     */
    void updateTokenCache(final String resource, final String clientId, final AuthenticationResult result) {
        if (result == null || StringExtensions.isNullOrBlank(result.getAccessToken())) {
            return;
        }
        
        if (result.getUserInfo() != null) {
            // update cache entry with displayableId
            if (!StringExtensions.isNullOrBlank(result.getUserInfo().getDisplayableId())) {
                setItemToCacheForUser(resource, clientId, result, result.getUserInfo().getDisplayableId());
            }
            
            // update cache entry with userId
            if (!StringExtensions.isNullOrBlank(result.getUserInfo().getUserId())) {
                setItemToCacheForUser(resource, clientId, result, result.getUserInfo().getUserId());
            }
        }
        
        // update for empty userid
        setItemToCacheForUser(resource, clientId, result, null);
    }
    
    /**
     * Remove token from cache.
     * 1) If refresh with resource specific token cache entry, clear RT with key(R,C,U,A)
     * 2) If refresh with MRRT, clear RT (C,U,A) and (R,C,U,A)
     * 3) if refresh with FRT, clear RT with (U,A) 
     * @throws AuthenticationException 
     */
    void removeTokenCacheItem(final TokenCacheItem tokenCacheItem, final String resource)
            throws AuthenticationException {
        final CacheEvent cacheEvent = new CacheEvent(EventStrings.TOKEN_CACHE_DELETE);
        cacheEvent.setRequestId(mTelemetryRequestId);
        Telemetry.getInstance().startEvent(mTelemetryRequestId, EventStrings.TOKEN_CACHE_DELETE);

        final List<String> keys;
        final TokenEntryType tokenEntryType = tokenCacheItem.getTokenEntryType();
        switch (tokenEntryType) {
        case REGULAR_TOKEN_ENTRY :
            cacheEvent.setTokenTypeRT(true);
            Logger.v(TAG, "Regular RT was used to get access token, remove entries "
                    + "for regular RT entries.");
            keys = getKeyListToRemoveForRT(tokenCacheItem);
            break;
        case MRRT_TOKEN_ENTRY :
            // We delete both MRRT and RT in this case.
            cacheEvent.setTokenTypeMRRT(true);
            Logger.v(TAG, "MRRT was used to get access token, remove entries for both "
                    + "MRRT entries and regular RT entries.");
            keys = getKeyListToRemoveForMRRT(tokenCacheItem);
            
            final TokenCacheItem regularRTItem = new TokenCacheItem(tokenCacheItem);
            regularRTItem.setResource(resource);
            keys.addAll(getKeyListToRemoveForRT(regularRTItem));
            break;
        case FRT_TOKEN_ENTRY :
            cacheEvent.setTokenTypeFRT(true);
            Logger.v(TAG, "FRT was used to get access token, remove entries for "
                    + "FRT entries.");
            keys = getKeyListToRemoveForFRT(tokenCacheItem);
            break;
        default : 
            throw new AuthenticationException(ADALError.INVALID_TOKEN_CACHE_ITEM);
        }
        
        for (final String key : keys) {
            mTokenCacheStore.removeItem(key);
        }
        Telemetry.getInstance().stopEvent(mTelemetryRequestId, cacheEvent,
                EventStrings.TOKEN_CACHE_DELETE);
    }
    
    /**
     * Update token cache for a given user. If token is MRRT, store two separate entries for regular RT entry and MRRT entry. 
     * Ideally, if returned token is MRRT, we should not store RT along with AT. However, there may be caller taking dependency
     * on RT. 
     * If the token is FRT, store three separate entries. 
     */
    private void setItemToCacheForUser(final String resource, final String clientId, final AuthenticationResult result, final String userId) {
        logReturnedToken(result);
        Logger.v(TAG, "Save regular token into cache.");

        final CacheEvent cacheEvent = new CacheEvent(EventStrings.TOKEN_CACHE_WRITE);
        cacheEvent.setRequestId(mTelemetryRequestId);
        Telemetry.getInstance().startEvent(mTelemetryRequestId, EventStrings.TOKEN_CACHE_WRITE);

        mTokenCacheStore.setItem(CacheKey.createCacheKeyForRTEntry(mAuthority, resource, clientId, userId), 
                TokenCacheItem.createRegularTokenCacheItem(mAuthority, resource, clientId, result));
        cacheEvent.setTokenTypeRT(true);
        // Store separate entries for MRRT.  
        if (result.getIsMultiResourceRefreshToken()) {
            Logger.v(TAG, "Save Multi Resource Refresh token to cache");
            mTokenCacheStore.setItem(CacheKey.createCacheKeyForMRRT(mAuthority, clientId, userId),
                    TokenCacheItem.createMRRTTokenCacheItem(mAuthority, clientId, result));
            cacheEvent.setTokenTypeMRRT(true);
        }
        
        // Store separate entries for FRT.
        if (!StringExtensions.isNullOrBlank(result.getFamilyClientId()) && !StringExtensions.isNullOrBlank(userId)) {
            Logger.v(TAG, "Save Family Refresh token into cache");
            final TokenCacheItem familyTokenCacheItem = TokenCacheItem.createFRRTTokenCacheItem(mAuthority, result);
            mTokenCacheStore.setItem(CacheKey.createCacheKeyForFRT(mAuthority, result.getFamilyClientId(), userId), familyTokenCacheItem);
            cacheEvent.setTokenTypeFRT(true);
        }
        Telemetry.getInstance().stopEvent(mTelemetryRequestId, cacheEvent,
                EventStrings.TOKEN_CACHE_WRITE);
    }
    
    /**
     * @return List of keys to remove when using regular RT to send refresh token request. 
     */
    private List<String> getKeyListToRemoveForRT(final TokenCacheItem cachedItem) {
        final List<String> keysToRemove = new ArrayList<>();
        keysToRemove.add(CacheKey.createCacheKeyForRTEntry(mAuthority, cachedItem.getResource(), cachedItem.getClientId(), null));
        if (cachedItem.getUserInfo() != null) {
            keysToRemove.add(CacheKey.createCacheKeyForRTEntry(mAuthority, cachedItem.getResource(), cachedItem.getClientId(), cachedItem.getUserInfo().getDisplayableId()));
            keysToRemove.add(CacheKey.createCacheKeyForRTEntry(mAuthority, cachedItem.getResource(), cachedItem.getClientId(), cachedItem.getUserInfo().getUserId()));
        }
        
        return keysToRemove;
    }
    
    /**
     * @return List of keys to remove when using MRRT to send refresh token request. 
     */
    private List<String> getKeyListToRemoveForMRRT(final TokenCacheItem cachedItem) {
        final List<String> keysToRemove = new ArrayList<>();
        
        keysToRemove.add(CacheKey.createCacheKeyForMRRT(mAuthority, cachedItem.getClientId(), null));
        if (cachedItem.getUserInfo() != null) {
            keysToRemove.add(CacheKey.createCacheKeyForMRRT(mAuthority, cachedItem.getClientId(), cachedItem.getUserInfo().getDisplayableId()));
            keysToRemove.add(CacheKey.createCacheKeyForMRRT(mAuthority, cachedItem.getClientId(), cachedItem.getUserInfo().getUserId()));
        }

        return keysToRemove;
    }
    
    /**
     * @return List of keys to remove when using FRT to send refresh token request. 
     */
    private List<String> getKeyListToRemoveForFRT(final TokenCacheItem cachedItem) {
        final List<String> keysToRemove = new ArrayList<>();
        if (cachedItem.getUserInfo() != null) {
            keysToRemove.add(CacheKey.createCacheKeyForFRT(mAuthority, cachedItem.getFamilyClientId(), cachedItem.getUserInfo().getDisplayableId()));
            keysToRemove.add(CacheKey.createCacheKeyForFRT(mAuthority, cachedItem.getFamilyClientId(), cachedItem.getUserInfo().getUserId()));
        }
        
        return keysToRemove;
    }
    
    private boolean isUserMisMatch(final String user, final TokenCacheItem tokenCacheItem) {
        // If user is not passed in the request or userInfo does not exist in the token cache item, 
        // it's a match case. We do wildcard find, return whatever match with cache key. 
        if (StringExtensions.isNullOrBlank(user) || tokenCacheItem.getUserInfo() == null) {
            return false;
        }
        
        // If user if provided, it needs to match either displayId or userId. 
        return !user.equalsIgnoreCase(tokenCacheItem.getUserInfo().getDisplayableId()) 
                && !user.equalsIgnoreCase(tokenCacheItem.getUserInfo().getUserId());
    }
    
    /**
     * Calculate hash for accessToken and log that.
     */
    private void logReturnedToken(final AuthenticationResult result) {
        if (result != null && result.getAccessToken() != null) {
            String accessTokenHash = getTokenHash(result.getAccessToken());
            String refreshTokenHash = getTokenHash(result.getRefreshToken());
            Logger.v(TAG, String.format(
                    "Access TokenID %s and Refresh TokenID %s returned.",
                    accessTokenHash, refreshTokenHash));
        }
    }
    
    private String getTokenHash(String token) {
        try {
            return StringExtensions.createHash(token);
        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG, "Digest error", "", ADALError.DEVICE_NO_SUCH_ALGORITHM, e);
        } catch (UnsupportedEncodingException e) {
            Logger.e(TAG, "Digest error", "", ADALError.ENCODING_IS_NOT_SUPPORTED, e);
        }

        return "";
    }

    private CacheEvent startCacheTelemetryRequest(String tokenType) {
        final CacheEvent cacheEvent = new CacheEvent(EventStrings.TOKEN_CACHE_LOOKUP);
        cacheEvent.setTokenType(tokenType);
        cacheEvent.setRequestId(mTelemetryRequestId);
        Telemetry.getInstance().startEvent(mTelemetryRequestId, EventStrings.TOKEN_CACHE_LOOKUP);

        return cacheEvent;
    }
}
