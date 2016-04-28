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

        // Client Id is optional, for FRT token cache entry, don't store client Id as key.
        if (clientId != null) {
            key.mClientId = clientId.toLowerCase(Locale.US);
        }
        
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
    
    public static String createFamilyRefreshTokenKey(final AuthenticationRequest authRequest, final String userId) {
        if (authRequest == null) {
            throw new IllegalArgumentException("authentication request is null");
        }
        
        return createCacheKey(authRequest.getAuthority(), null, null, true, userId);
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
