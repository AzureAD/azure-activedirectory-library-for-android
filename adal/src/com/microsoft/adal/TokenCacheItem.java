/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.Serializable;
import java.util.Date;

/**
 * Extended result to store more info Queries will be performed over this item
 * not the key
 * 
 * @author omercan
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
            if (!storeMultiResourceRefreshToken) {
                // Cache item will not store accesstoken and userinfo for Multi
                // Resource Refresh Token
                mUserInfo = result.getUserInfo();
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
