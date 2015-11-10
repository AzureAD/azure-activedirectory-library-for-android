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

    private String mAuthority;

    private String mResource;

    private String mClientId;

    private String mUserId;

    private boolean mIsMultipleResourceRefreshToken;

    private CacheKey() {
        mAuthority = null;
        mResource = null;
        mClientId = null;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s$%s$%s$%s$%s", mAuthority, mResource, mClientId,
                (mIsMultipleResourceRefreshToken ? "y" : "n"), mUserId);
    }

    /**
     * @param authority URL of the authenticating authority
     * @param resource resource identifier
     * @param clientId client identifier
     * @param isMultiResourceRefreshToken true/false for refresh token type
     * @param userId userid provided from {@link UserInfo}
     * @return CacheKey to use in saving token
     */
    public static String createCacheKey(String authority, String resource, String clientId,
            boolean isMultiResourceRefreshToken, String userId) {

        if (authority == null) {
            throw new IllegalArgumentException("authority");
        }

        if (clientId == null) {
            throw new IllegalArgumentException("clientId");
        }

        CacheKey key = new CacheKey();

        if (!isMultiResourceRefreshToken) {

            if (resource == null) {
                throw new IllegalArgumentException("resource");
            }

            // MultiResource token items will be stored without resource
            key.mResource = resource;
        }

        key.mAuthority = authority.toLowerCase(Locale.US);
        if (key.mAuthority.endsWith("/")) {
            key.mAuthority = (String)key.mAuthority.subSequence(0, key.mAuthority.length() - 1);
        }

        key.mClientId = clientId.toLowerCase(Locale.US);
        key.mIsMultipleResourceRefreshToken = isMultiResourceRefreshToken;

        // optional
        if (!StringExtensions.IsNullOrBlank(userId)) {
            key.mUserId = userId.toLowerCase(Locale.US);
        }

        return key.toString();
    }

    /**
     * @param item Token item in the cache
     * @return CacheKey to save token
     */
    public static String createCacheKey(TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("TokenCacheItem");
        }

        String userid = null;

        if (item.getUserInfo() != null) {
            userid = item.getUserInfo().getUserId();
        }

        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId(),
                item.getIsMultiResourceRefreshToken(), userid);
    }

    /**
     * @param item AuthenticationRequest item
     * @param cacheUserId UserId in the cache
     * @return CacheKey to save token
     */
    public static String createCacheKey(AuthenticationRequest item, String cacheUserId) {
        if (item == null) {
            throw new IllegalArgumentException("AuthenticationRequest");
        }

        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId(), false,
                cacheUserId);
    }

    /**
     * Store multi resource refresh tokens with different key. Key will not
     * include resource and set flag to y.
     * 
     * @param item AuthenticationRequest item
     * @param cacheUserId UserId in the cache
     * @return CacheKey to save token
     */
    public static String createMultiResourceRefreshTokenKey(AuthenticationRequest item,
            String cacheUserId) {
        if (item == null) {
            throw new IllegalArgumentException("AuthenticationRequest");
        }

        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId(), true,
                cacheUserId);
    }

    /**
     * Gets Authority.
     * 
     * @return Authority
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * Gets Resource.
     * 
     * @return Resource
     */
    public String getResource() {
        return mResource;
    }

    /**
     * Gets ClientId.
     * 
     * @return ClientId
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Gets UserId.
     * 
     * @return UserId
     */
    public String getUserId() {
        return mUserId;
    }

    /**
     * Gets status for multi resource refresh token.
     * 
     * @return status for multi resource refresh token
     */
    public boolean getIsMultipleResourceRefreshToken() {
        return mIsMultipleResourceRefreshToken;
    }
}
