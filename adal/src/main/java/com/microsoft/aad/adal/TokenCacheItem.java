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

    private Date mExtendedExpiresOn;

    /**
     * Default constructor for cache item.
     */
    public TokenCacheItem() {
        // Intentionally left blank
    }
    
    TokenCacheItem(final TokenCacheItem tokenCacheItem) {
        mAuthority = tokenCacheItem.getAuthority();
        mResource = tokenCacheItem.getResource();
        mClientId = tokenCacheItem.getClientId();
        mAccessToken = tokenCacheItem.getAccessToken();
        mRefreshtoken = tokenCacheItem.getRefreshToken();
        mRawIdToken = tokenCacheItem.getRawIdToken();
        mUserInfo = tokenCacheItem.getUserInfo();
        mExpiresOn = tokenCacheItem.getExpiresOn();
        mIsMultiResourceRefreshToken = tokenCacheItem.getIsMultiResourceRefreshToken();
        mTenantId = tokenCacheItem.getTenantId();
        mFamilyClientId = tokenCacheItem.getFamilyClientId();
        mExtendedExpiresOn = tokenCacheItem.getExtendedExpiresOn();
    }
    
    /**
     * Construct cache item with given authority and returned auth result. 
     */
    private TokenCacheItem(final String authority, final AuthenticationResult authenticationResult) {
        if (authenticationResult == null) {
            throw new IllegalArgumentException("authenticationResult");
        }
        
        if (StringExtensions.isNullOrBlank(authority)) {
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
        mExtendedExpiresOn = authenticationResult.getExtendedExpiresOn();
    }

    /**
     * Create regular RT token cache item. 
     * 
     * @param authority required authority identifier.
     * @param resource required resource identifier.
     * @param clientId required client identifier.
     * @param authResult required authentication result to create regular token cache item.
     *                   
     * @return TokenCacheItem
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
     * 
     * @param authority required authority identifier.
     * @param clientId required client identifier.
     * @param authResult required authentication result to create multi-resource refresh token cache item.
     * 
     * @return TokenCacheItem
     */
    public static TokenCacheItem createMRRTTokenCacheItem(final String authority, final String clientId, final AuthenticationResult authResult) {
        final TokenCacheItem item = new TokenCacheItem(authority, authResult);
        item.setClientId(clientId);
        
        return item;
    }

    /**
     * Create FRT token cache entry. 
     * Will not store clientId, resource and AT. 
     * 
     * @param authority required authority identifier.
     * @param authResult required authentication result to create FRRT refresh token cache item.
     *                   
     * @return TokenCacheItem
     */
    public static TokenCacheItem createFRRTTokenCacheItem(final String authority, final AuthenticationResult authResult) {
        return new TokenCacheItem(authority, authResult);
    }

    /**
     * Get the user information.
     * 
     * @return UserInfo object.
     */
    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    /**
     * Set the user information.
     * 
     * @param info UserInfo object which contains user information.
     */
    public void setUserInfo(UserInfo info) {
        mUserInfo = info;
    }

    /**
     * Get the resource.
     * 
     * @return resource String.
     */
    public String getResource() {
        return mResource;
    }

    /**
     * Set the resource.
     * 
     * @param resource resource identifier.
     */
    public void setResource(String resource) {
        mResource = resource;
    }

    /**
     * Get the authority.
     * 
     * @return authority url string.
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * Set the authority.
     * 
     * @param authority String authority url.
     */
    public void setAuthority(String authority) {
        mAuthority = authority;
    }

    /**
     * Get the client identifier.
     * 
     * @return client identifier string.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Set the client identifier.
     * 
     * @param clientId client identifier string.
     */
    public void setClientId(String clientId) {
        mClientId = clientId;
    }

    /**
     * Get the access token.
     * 
     * @return the access token string.
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Set the access token string.
     * 
     * @param accessToken the access token string.
     */
    public void setAccessToken(String accessToken) {
        mAccessToken = accessToken;
    }

    /**
     * Get the refresh token string.
     * 
     * @return the refresh token string.
     */
    public String getRefreshToken() {
        return mRefreshtoken;
    }

    /**
     * Set the fresh token string.
     * 
     * @param refreshToken the refresh token string.
     */
    public void setRefreshToken(String refreshToken) {
        mRefreshtoken = refreshToken;
    }

    /**
     * Get the expire date.
     * 
     * @return the time the token get expired.
     */
    public Date getExpiresOn() {
        return Utility.getImmutableDateObject(mExpiresOn);
    }

    /**
     * Set the expire date.
     * 
     * @param expiresOn the expire time.
     */
    public void setExpiresOn(final Date expiresOn) {
        mExpiresOn = Utility.getImmutableDateObject(expiresOn);
    }

    /**
     * Get the multi-resource refresh token flag.
     * 
     * @return true if the token is a multi-resource refresh token, else return false.
     */
    public boolean getIsMultiResourceRefreshToken() {
        return mIsMultiResourceRefreshToken;
    }

    /**
     * Set the multi-resource refresh token flag.
     * 
     * @param isMultiResourceRefreshToken true if the token is a multi-resource refresh token.
     */
    public void setIsMultiResourceRefreshToken(boolean isMultiResourceRefreshToken) {
        mIsMultiResourceRefreshToken = isMultiResourceRefreshToken;
    }

    /**
     * Get tenant identifier.
     * 
     * @return the tenant identifier string.
     */
    public String getTenantId() {
        return mTenantId;
    }

    /**
     * Set tenant identifier.
     * 
     * @param tenantId the tenant identifier string.
     */
    public void setTenantId(String tenantId) {
        mTenantId = tenantId;
    }

    /**
     * Get raw ID token.
     * 
     * @return raw ID token string.
     */
    public String getRawIdToken() {
        return mRawIdToken;
    }

    /**
     * Set raw ID token.
     * 
     * @param rawIdToken raw ID token string.
     */
    public void setRawIdToken(String rawIdToken) {
        mRawIdToken = rawIdToken;
    }

    /**
     * Get family client identifier.
     * 
     * @return the family client ID string.
     */
    public final String getFamilyClientId() {
        return mFamilyClientId;
    }

    /**
     * Set family client identifier.
     * 
     * @param familyClientId the family client ID string.
     */
    public final void setFamilyClientId(final String familyClientId) {
        mFamilyClientId = familyClientId;
    }

    /**
     * Set the extended expired time.
     * 
     * @param extendedExpiresOn extended expired date.
     */
    public final void setExtendedExpiresOn(final Date extendedExpiresOn) {
        mExtendedExpiresOn = Utility.getImmutableDateObject(extendedExpiresOn);
    }

    /**
     * Get the extended expired time.
     * 
     * @return the extended expired date.
     */
    public final Date getExtendedExpiresOn() {
        return Utility.getImmutableDateObject(mExtendedExpiresOn);
    }

    /**
     * Verify if the token cache token is valid for the extended expired time. 
     * 
     * @return true if the access token is not null and is not expired in the extended 
     *         expired time, else return false.
     */
    public final boolean isExtendedLifetimeValid() {
        //extended lifetime is only valid if it contains an access token
        if (mExtendedExpiresOn != null && !StringExtensions.isNullOrBlank(mAccessToken)) {
            return !isTokenExpired(mExtendedExpiresOn);
        }
        
        return false;
    }

    /**
     * Checks expiration time.
     * 
     * @param expiresOn the time in type Date to check if it is expired
     * 
     * @return true if expired
     */
    public static boolean isTokenExpired(final Date expiresOn) {
        Calendar calendarWithBuffer = Calendar.getInstance();
        calendarWithBuffer.add(Calendar.SECOND,
                AuthenticationSettings.INSTANCE.getExpirationBuffer());
        Date validity = calendarWithBuffer.getTime();
        Logger.v(TAG, "expiresOn:" + expiresOn + " timeWithBuffer:" + calendarWithBuffer.getTime()
                + " Buffer:" + AuthenticationSettings.INSTANCE.getExpirationBuffer());

        return expiresOn != null && expiresOn.before(validity);
    }
    
    /**
     * @return {@link TokenEntryType} based on the fields stored in the 
     * {@link TokenCacheItem}. 
     * 1) Only item stored for regular token entry has resource stored. 
     * 2) Item stored for FRT entry won't have client Id stored. 
     */
    TokenEntryType getTokenEntryType() {
        if (!StringExtensions.isNullOrBlank(this.getResource())) {
            // Only regular token cache entry is storing resouce. 
            return TokenEntryType.REGULAR_TOKEN_ENTRY;
        } else if (StringExtensions.isNullOrBlank(this.getClientId())) {
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
        return !StringExtensions.isNullOrBlank(mFamilyClientId);
    }
}

/**
 * Internal class representing the entry type for stored {@link TokenCacheItem}.
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
