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

import java.io.Serializable;
import java.util.Locale;

/**
 * CacheKey will be the object for key.
 */
public final class CacheKey implements Serializable {

    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 8067972995583126404L;
    
    static final String FRT_ENTRY_PREFIX = "foci-";

    private String mAuthority;

    private String mResource;

    private String mClientId;

    private String mUserId;

    private String mFamilyClientId;
    
    private boolean mIsMultipleResourceRefreshToken;

    private CacheKey() {
    }

    @Override
    public String toString() {
        // only family token cache item will have the family client id as the key
        if (StringExtensions.isNullOrBlank(mFamilyClientId)) {
            return String.format(Locale.US, "%s$%s$%s$%s$%s", mAuthority, mResource, mClientId,
                    (mIsMultipleResourceRefreshToken ? "y" : "n"), mUserId);
        }
        
        return String.format(Locale.US, "%s$%s$%s$%s$%s$%s", mAuthority, mResource, mClientId,
                (mIsMultipleResourceRefreshToken ? "y" : "n"), mUserId, mFamilyClientId);
    }

    /**
     * @param authority URL of the authenticating authority
     * @param resource resource identifier
     * @param clientId client identifier
     * @param isMultiResourceRefreshToken true/false for refresh token type
     * @param userId userid provided from {@link UserInfo}
     * @param familyClientId Family client Id of the app. FoCI feature only applies to Microsoft
     *                       apps now, by default the id will be "1".
     * @return CacheKey to use in saving token
     */
    public static String createCacheKey(final String authority, final String resource, final String clientId,
            final boolean isMultiResourceRefreshToken, final String userId, final String familyClientId) {

        if (authority == null) {
            throw new IllegalArgumentException("authority");
        }
        
        // For family token cache entry, client id will be foci-familyId
        // When we receive family token from server response, will use whatever
        // server returned as familyId; for caching look up, will hardcode "1"
        // for now since only FoCI feature is only supported for Microsoft first
        // party apps, and server returns "1" for Microsoft family apps. 
        if (clientId == null && familyClientId == null) {
            throw new IllegalArgumentException("both clientId and familyClientId are null");
        }
        
        final CacheKey key = new CacheKey();
        
        if (!isMultiResourceRefreshToken) {
            if (resource == null) {
                throw new IllegalArgumentException("resource");
            }

            // MultiResource token items will be stored without resource
            key.mResource = resource;
        }
        
        key.mAuthority = authority.toLowerCase(Locale.US);
        if (key.mAuthority.endsWith("/")) {
            key.mAuthority = (String) key.mAuthority.subSequence(0, key.mAuthority.length() - 1);
        }

        if (clientId != null) {
            key.mClientId = clientId.toLowerCase(Locale.US);
        }
        
        if (familyClientId != null) {
            final String prefixedFamilyClient = FRT_ENTRY_PREFIX + familyClientId;
            key.mFamilyClientId =  prefixedFamilyClient.toLowerCase(Locale.US);
        }

        key.mIsMultipleResourceRefreshToken = isMultiResourceRefreshToken;

        // optional
        if (!StringExtensions.isNullOrBlank(userId)) {
            key.mUserId = userId.toLowerCase(Locale.US);
        }

        return key.toString();
    }

    /**
     * Create cachekey from {@link TokenCacheItem}. It will use {@link UserInfo#getUserId()} 
     * as the user for cachekey if present. 
     * @param item {@link TokenCacheItem} that is used to create the cache key. 
     * @return String value of the {@link CacheKey} to save token. 
     * @throws AuthenticationException 
     */
    public static String createCacheKey(TokenCacheItem item) throws AuthenticationException {
        if (item == null) {
            throw new IllegalArgumentException("TokenCacheItem");
        }

        String userid = null;
        if (item.getUserInfo() != null) {
            userid = item.getUserInfo().getUserId();
        }
        
        final TokenEntryType tokenEntryType = item.getTokenEntryType();
        switch (tokenEntryType) {
        case REGULAR_TOKEN_ENTRY: 
            return createCacheKeyForRTEntry(item.getAuthority(), item.getResource(), 
                    item.getClientId(), userid);
        case MRRT_TOKEN_ENTRY:
            return createCacheKeyForMRRT(item.getAuthority(), item.getClientId(), userid);
        case FRT_TOKEN_ENTRY:
            return createCacheKeyForFRT(item.getAuthority(), item.getFamilyClientId(), userid);
        default: 
            throw new AuthenticationException(ADALError.INVALID_TOKEN_CACHE_ITEM, "Cannot create cachekey from given token item");
        }
    }

    /**
     * Create cache key for regular RT entry.
     * @param authority Authority for the key to store regular RT entry.
     * @param resource Resource for the key to store regular RT entry.
     * @param clientId Client id for the key to store regular RT entry.
     * @param userId User id for the key to store regular RT entry.
     * @return The cache key for regular RT entry.
     */
    public static String createCacheKeyForRTEntry(final String authority, final String resource,
                                                  final String clientId, final String userId) {
        return createCacheKey(authority, resource, clientId, false, userId, null);
    }

    /**
     * Create cache key for MRRT entry.
     * @param authority The authority used to create the cache key.
     * @param clientId The client id used to create the cache key.
     * @param userId The user id used to create the cache key.
     * @return The cache key for MRRT entry.
     */
    public static String createCacheKeyForMRRT(final String authority, final String clientId, final String userId) {
        return createCacheKey(authority, null, clientId, true, userId, null);
    }
    
    /**
     * Create cache key for FRT entry.
     * @param authority The authority of the cache key.
     * @param familyClientId The family client id of the FRT entry cache key.
     * @param userId The user id of the cache key.
     * @return The cache key for FRT entry.
     */
    public static String createCacheKeyForFRT(final String authority, final String familyClientId, final String userId) {
        return createCacheKey(authority, null, null, true, userId, familyClientId);
    }

    /**
     * Gets Authority.
     * @return Authority
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * Gets Resource.
     * @return Resource
     */
    public String getResource() {
        return mResource;
    }

    /**
     * Gets ClientId.
     * @return ClientId
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Gets UserId.
     * @return UserId
     */
    public String getUserId() {
        return mUserId;
    }

    /**
     * Gets status for multi resource refresh token.
     * @return status for multi resource refresh token
     */
    public boolean getIsMultipleResourceRefreshToken() {
        return mIsMultipleResourceRefreshToken;
    }
}
