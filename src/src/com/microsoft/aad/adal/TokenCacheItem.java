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
import java.util.Calendar;
import java.util.Date;

import com.microsoft.aad.adal.RefreshItem.KeyEntryType;

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
            final KeyEntryType keyEntryType) {
        mAuthority = request.getAuthority();
           
        // Do not store client id for token stored in family token entry
        if (keyEntryType != KeyEntryType.FAMILY_REFRESH_TOKEN_ENTRY) {
            mClientId = request.getClientId();
        }

        if (keyEntryType == KeyEntryType.REGULAR_REFRESH_TOKEN_ENTRY) {
            // Only regular token cache entry should stoure resource
            mResource = request.getResource();
        }

        if (result != null) {
            mExpiresOn = result.getExpiresOn();
            mIsMultiResourceRefreshToken = result.getIsMultiResourceRefreshToken();
            mTenantId = result.getTenantId();
            mUserInfo = result.getUserInfo();
            mRawIdToken = result.getIdToken();

            if (keyEntryType == KeyEntryType.REGULAR_REFRESH_TOKEN_ENTRY) {
                // Only store AT for regular token cache entry.
                // Don't store AT for MRRT token cache entry or FRT entry
                mAccessToken = result.getAccessToken();
            } 

            mRefreshtoken = result.getRefreshToken();
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
