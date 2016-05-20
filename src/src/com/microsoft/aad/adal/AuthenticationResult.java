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
import java.util.Date;

/**
 * Result class to keep code, token and other info Serializable properties Mark
 * temp properties as Transient if you dont want to keep them in serialization.
 */
public class AuthenticationResult implements Serializable {

    /**
     * Serial version number for serialization.
     */
    private static final long serialVersionUID = 2243372613182536368L;

    /**
     * Status for authentication.
     */
    public enum AuthenticationStatus {
        /**
         * User cancelled login activity.
         */
        Cancelled,
        /**
         * request has errors.
         */
        Failed,
        /**
         * token is acquired.
         */
        Succeeded,
    }

    private String mCode;

    private String mAccessToken;

    private String mRefreshToken;

    private String mTokenType;

    private Date mExpiresOn;

    private String mErrorCode;

    private String mErrorDescription;

    private String mErrorCodes;

    private boolean mIsMultiResourceRefreshToken;

    private UserInfo mUserInfo;

    private String mTenantId;

    private String mIdToken;

    private AuthenticationStatus mStatus = AuthenticationStatus.Failed;

    private boolean mInitialRequest;
    
    private String mFamilyClientId;

    AuthenticationResult() {
        mCode = null;
    }

    AuthenticationResult(String code) {
        mCode = code;
        mStatus = AuthenticationStatus.Succeeded;
        mAccessToken = null;
        mRefreshToken = null;
    }

    AuthenticationResult(String accessToken, String refreshToken, Date expires, boolean isBroad,
            UserInfo userInfo, String tenantId, String idToken) {
        mCode = null;
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mExpiresOn = expires;
        mIsMultiResourceRefreshToken = isBroad;
        mStatus = AuthenticationStatus.Succeeded;
        mUserInfo = userInfo;
        mTenantId = tenantId;
        mIdToken = idToken;
    }

    AuthenticationResult(String accessToken, String refreshToken, Date expires, boolean isBroad) {
        mCode = null;
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mExpiresOn = expires;
        mIsMultiResourceRefreshToken = isBroad;
        mStatus = AuthenticationStatus.Succeeded;
    }

    AuthenticationResult(String errorCode, String errDescription, String errorCodes) {
        mErrorCode = errorCode;
        mErrorDescription = errDescription;
        mErrorCodes = errorCodes;
        mStatus = AuthenticationStatus.Failed;
    }

    /**
     * Creates result from {@link TokenCacheItem}.
     * 
     * @param cacheItem
     * @return AuthenticationResult
     */
    static AuthenticationResult createResult(final TokenCacheItem cacheItem) {

        if (cacheItem == null) {
            AuthenticationResult result = new AuthenticationResult();
            result.mStatus = AuthenticationStatus.Failed;
            return result;
        }

        return new AuthenticationResult(cacheItem.getAccessToken(), cacheItem.getRefreshToken(),
                cacheItem.getExpiresOn(), cacheItem.getIsMultiResourceRefreshToken(),
                cacheItem.getUserInfo(), cacheItem.getTenantId(), cacheItem.getRawIdToken());
    }

    static AuthenticationResult createResultForInitialRequest() {
        AuthenticationResult result = new AuthenticationResult();
        result.mInitialRequest = true;
        return result;
    }

    /**
     * Uses access token to create header for web requests.
     * 
     * @return AuthorizationHeader
     */
    public String createAuthorizationHeader() {
        return AuthenticationConstants.AAD.BEARER + " " + getAccessToken();
    }

    /**
     * Access token to send to the service in Authorization Header.
     * 
     * @return Access token
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Refresh token to get new tokens.
     * 
     * @return Refresh token
     */
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * Token type.
     * 
     * @return access token type
     */
    public String getAccessTokenType() {
        return mTokenType;
    }

    /**
     * Epoch time for expiresOn.
     * 
     * @return expiresOn {@link Date}
     */
    public Date getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * Multi-resource refresh tokens can be used to request token for another
     * resource.
     * 
     * @return multi resource refresh token status
     */
    public boolean getIsMultiResourceRefreshToken() {
        return mIsMultiResourceRefreshToken;
    }

    /**
     * UserInfo returned from IdToken.
     * 
     * @return {@link UserInfo}
     */
    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    /**
     * Set userinfo after refresh from previous idtoken.
     * 
     * @param userinfo
     */
    void setUserInfo(UserInfo userinfo) {
        mUserInfo = userinfo;
    }

    /**
     * Gets tenantId.
     * 
     * @return TenantId
     */
    public String getTenantId() {
        return mTenantId;
    }

    /**
     * Gets status.
     * 
     * @return {@link AuthenticationStatus}
     */
    public AuthenticationStatus getStatus() {
        return mStatus;
    }

    String getCode() {
        return mCode;
    }

    void setCode(String code) {
        mCode = code;
    }

    /**
     * Gets error code.
     * 
     * @return Error code
     */
    public String getErrorCode() {
        return mErrorCode;
    }

    /**
     * Gets error description.
     * 
     * @return error description
     */
    public String getErrorDescription() {
        return mErrorDescription;
    }

    /**
     * Gets error log info.
     * 
     * @return log info
     */
    public String getErrorLogInfo() {
        return " ErrorCode:" + getErrorCode() + " ErrorDescription:" + getErrorDescription();
    }

    /**
     * Checks expiration time.
     * 
     * @return true if expired
     */
    public boolean isExpired() {
        return TokenCacheItem.isTokenExpired(getExpiresOn());
    }

    String[] getErrorCodes() {
        return (mErrorCodes != null) ? mErrorCodes.replaceAll("[\\[\\]]", "").split("([^,]),") : null;
    }

    boolean isInitialRequest() {
        return mInitialRequest;
    }

    /**
     * Get raw idtoken.
     * 
     * @return IdToken
     */
    public String getIdToken() {
        return mIdToken;
    }

    void setIdToken(String idToken) {
        this.mIdToken = idToken;
    }

    void setTenantId(String tenantid) {
        mTenantId = tenantid;
    }
    
    void setRefreshToken(String refreshToken){
        mRefreshToken = refreshToken;
    }
    
    final String getFamilyClientId() {
        return mFamilyClientId;
    }
    
    final void setFamilyClientId (final String familyClientId) {
        mFamilyClientId = familyClientId;
    }
}
