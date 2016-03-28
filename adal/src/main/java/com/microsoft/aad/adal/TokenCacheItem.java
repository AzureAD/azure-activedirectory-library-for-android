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
import java.util.Calendar;
import java.util.Date;

/**
 * Extended result to store more info Queries will be performed over this item
 * not the key.
 */
public class TokenCacheItem implements Serializable {

    /**
     * Serial version.
     */
    private static final long serialVersionUID = 1L;

    private static final String TAG = "TokenCacheItem";

    private UserInfo mUserInfo;

    private String mResource;

    private String mAuthority;

    private String mClientId;

    private String mAccessToken;

    private String mRefreshtoken;

    private String mRawIdToken;

    /**
     * This time is GMT.
     */
    private Date mExpiresOn;

    private boolean mIsMultiResourceRefreshToken;

    private String mTenantId;
    
    private String mFamilyClientId;

    /**
     * Construct default cache item.
     */
    public TokenCacheItem() {

    }

    TokenCacheItem(final AuthenticationRequest request, final AuthenticationResult result,
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
            mRawIdToken = result.getIdToken();
            if (!storeMultiResourceRefreshToken) {
                // Cache item will not store accesstoken for Multi
                // Resource Refresh Token
                mAccessToken = result.getAccessToken();
            }
            
            mFamilyClientId = result.getFamilyClientId();
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

    public void setIsMultiResourceRefreshToken(boolean isMultiResourceRefreshToken) {
        this.mIsMultiResourceRefreshToken = isMultiResourceRefreshToken;
    }

    public String getTenantId() {
        return mTenantId;
    }

    public void setTenantId(String tenantId) {
        this.mTenantId = tenantId;
    }

    public String getRawIdToken() {
        return mRawIdToken;
    }

    public void setRawIdToken(String rawIdToken) {
        this.mRawIdToken = rawIdToken;
    }
    
    public final String getFamilyClientId() {
        return mFamilyClientId;
    }
    
    public final void setFamilyClientId(final String familyClientId) {
        this.mFamilyClientId = familyClientId;
    }

    /**
     * Checks expiration time.
     * 
     * @return true if expired
     */
    public static boolean isTokenExpired(Date expiresOn) {
        Calendar calendarWithBuffer = Calendar.getInstance();
        calendarWithBuffer.add(Calendar.SECOND,
                AuthenticationSettings.INSTANCE.getExpirationBuffer());
        Date validity = calendarWithBuffer.getTime();
        Logger.v(TAG, "expiresOn:" + expiresOn + " timeWithBuffer:" + calendarWithBuffer.getTime()
                + " Buffer:" + AuthenticationSettings.INSTANCE.getExpirationBuffer());

        if (expiresOn != null && expiresOn.before(validity)) {
            return true;
        }

        return false;
    }
}
