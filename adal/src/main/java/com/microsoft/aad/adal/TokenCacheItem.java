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
     * Default constructor for cache item.
     */
    public TokenCacheItem() {}
    
    TokenCacheItem(final TokenCacheItem tokenCacheItem) {
        this.mAuthority = tokenCacheItem.getAuthority();
        this.mResource = tokenCacheItem.getResource();
        this.mClientId = tokenCacheItem.getClientId();
        this.mAccessToken = tokenCacheItem.getAccessToken();
        this.mRefreshtoken = tokenCacheItem.getRefreshToken();
        this.mRawIdToken = tokenCacheItem.getRawIdToken();
        this.mUserInfo = tokenCacheItem.getUserInfo();
        this.mExpiresOn = tokenCacheItem.getExpiresOn();
        this.mIsMultiResourceRefreshToken = tokenCacheItem.getIsMultiResourceRefreshToken();
        this.mTenantId = tokenCacheItem.getTenantId();
        this.mFamilyClientId = tokenCacheItem.getFamilyClientId();
    }
    
    /**
     * Construct cache item with given authority and returned auth result. 
     */
    private TokenCacheItem(final String authority, final AuthenticationResult authenticationResult) {
        if (authenticationResult == null) {
            throw new IllegalArgumentException("authenticationResult");
        }
        
        if (StringExtensions.IsNullOrBlank(authority)) {
            throw new IllegalArgumentException("authority");
        }
        
        mAuthority = authority;
        mExpiresOn = authenticationResult.getExpiresOn();
        // Multi-resource refresh token won't have resource recorded. To support back-compability
        // for existing token cache item.
        mIsMultiResourceRefreshToken = authenticationResult.getIsMultiResourceRefreshToken();
        mTenantId = authenticationResult.getTenantId();
        mUserInfo = authenticationResult.getUserInfo();
        mRawIdToken = authenticationResult.getIdToken();
        mRefreshtoken = authenticationResult.getRefreshToken();
        mFamilyClientId = authenticationResult.getFamilyClientId();
    }
    
    /**
     * Create regular RT token cache item. 
     */
    public static TokenCacheItem createRegularTokenCacheItem(final String authority, final String resource, final String clientId, final AuthenticationResult authResult) {
        final TokenCacheItem item = new TokenCacheItem(authority, authResult);
        item.setClientId(clientId);
        item.setResource(resource);
        item.setAccessToken(authResult.getAccessToken());
        return item;
    }
    
    /**
     * Create MRRT token cache item. 
     * Will not store AT and resource in the token cache.
     */
    public static TokenCacheItem createMRRTTokenCacheItem(final String authority, final String clientId, final AuthenticationResult authResult) {
        final TokenCacheItem item = new TokenCacheItem(authority, authResult);
        item.setClientId(clientId);
        
        return item;
    }
    
    /**
     * Create FRT token cache entry. 
     * Will not store clientId, resource and AT. 
     */
    public static TokenCacheItem createFRRTTokenCacheItem(final String authority, final AuthenticationResult authResult) {
        return new TokenCacheItem(authority, authResult);
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
    
    /**
     * @return {@link TokenEntryType} based on the fields stored in the 
     * {@link TokenCacheItem}. 
     * 1) Only item stored for regular token entry has resource stored. 
     * 2) Item stored for FRT entry won't have client Id stored. 
     */
    TokenEntryType getTokenEntryType() {
        if (!StringExtensions.IsNullOrBlank(this.getResource())) {
            // Only regular token cache entry is storing resouce. 
            return TokenEntryType.REGULAR_TOKEN_ENTRY;
        } else if (StringExtensions.IsNullOrBlank(this.getClientId())) {
            // Family token cache item does not store clientId
            return TokenEntryType.FRT_TOKEN_ENTRY;
        } else {
            return TokenEntryType.MRRT_TOKEN_ENTRY;
        }
    }
    
    /**
     * @return True if the {@link TokenCacheItem} has FoCI flag, false otherwise. 
     */
    boolean isFamilyToken() {
        return !StringExtensions.IsNullOrBlank(mFamilyClientId);
    }
}

/**
 * Internal class representing the entry type for stored {@link TokenCacheItem}
 */
enum TokenEntryType {
    /**
     * Represents the regular token entry. 
     * {@link TokenCacheItem} stored for regular token entry will have resource, 
     * access token, client id store. 
     * If it's also a MRRT item, MRRT flag will be marked as true. 
     * If it's also a FRT item, FoCI field will be populated with the family client Id 
     * server returned. 
     */
    REGULAR_TOKEN_ENTRY, 
    
    /**
     * Represents the MRRT token entry. 
     * {@link TokenCacheItem} stored for MRRT token entry will not have resource 
     * and access token store. 
     * MRRT flag will be set as true. 
     * If it's also a FRT item, FoCI field will be populated with the family client Id 
     * server returned. 
     */
    MRRT_TOKEN_ENTRY, 
    
    /**
     * Represents the FRT token entry. 
     * {@link TokenCacheItem} stored for FRT token entry will not have resource, access token
     * and client id stored. FoCI field be will populated with the value server returned. 
     */
    FRT_TOKEN_ENTRY
}