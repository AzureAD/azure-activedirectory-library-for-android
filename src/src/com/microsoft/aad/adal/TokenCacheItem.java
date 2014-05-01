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
import java.util.Date;

/**
 * Extended result to store more info Queries will be performed over this item
 * not the key
 */
public class TokenCacheItem implements Serializable {

    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    private UserInfo mUserInfo;

    private String mResource;

    private String mAuthority;

    private String mClientId;

    private String mAccessToken;

    private String mRefreshtoken;

    /**
     * this time is GMT
     */
    private Date mExpiresOn;

    private boolean mIsMultiResourceRefreshToken;

    private String mTenantId;

    public TokenCacheItem() {

    }

    public TokenCacheItem(AuthenticationRequest request, AuthenticationResult result,
            boolean storeMultiResourceRefreshToken) {
        if (request != null) {
            mAuthority = request.getAuthority();
            mClientId = request.getClientId();
            if (!storeMultiResourceRefreshToken) {
                // Cache item will not store resource info for Multi Resource
                // Refresh Token
                mResource = request.getResource();
            }
        }

        if (result != null) {
            mRefreshtoken = result.getRefreshToken();
            mExpiresOn = result.getExpiresOn();
            mIsMultiResourceRefreshToken = storeMultiResourceRefreshToken;
            mTenantId = result.getTenantId();
            mUserInfo = result.getUserInfo();
            if (!storeMultiResourceRefreshToken) {
                // Cache item will not store accesstoken for Multi
                // Resource Refresh Token               
                mAccessToken = result.getAccessToken();
            }
        }
    }

    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    public void setUserInfo(UserInfo info) {
        this.mUserInfo = info;
    }

    public String getResource() {
        return mResource;
    }

    public void setResource(String resource) {
        this.mResource = resource;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(String authority) {
        this.mAuthority = authority;
    }

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String clientId) {
        this.mClientId = clientId;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String accessToken) {
        this.mAccessToken = accessToken;
    }

    public String getRefreshToken() {
        return mRefreshtoken;
    }

    public void setRefreshToken(String refreshToken) {
        this.mRefreshtoken = refreshToken;
    }

    public Date getExpiresOn() {
        return mExpiresOn;
    }

    public void setExpiresOn(Date expiresOn) {
        this.mExpiresOn = expiresOn;
    }

    public boolean getIsMultiResourceRefreshToken() {
        return mIsMultiResourceRefreshToken;
    }

    public void setIsMultiResourceRefreshToken(boolean mIsMultiResourceRefreshToken) {
        this.mIsMultiResourceRefreshToken = mIsMultiResourceRefreshToken;
    }

    public String getTenantId() {
        return mTenantId;
    }

    public void setTenantId(String mTenantId) {
        this.mTenantId = mTenantId;
    }
}
