/**
 * ---------------------------------------------------------------- 
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 * ----------------------------------------------------------------
 */

package com.microsoft.adal;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Result class to keep code, token and other info Serializable properties Mark
 * temp properties as Transient if you dont want to keep them in serialization
 * 
 * @author omercan
 */
public class AuthenticationResult implements Serializable {

    /**
     * Serial varsion number for serialization
     */
    private static final long serialVersionUID = 2243372613182536368L;

    public enum AuthenticationStatus {
        /**
         * User cancelled login activity
         */
        Cancelled,
        /**
         * request has errors
         */
        Failed,
        /**
         * token is acquired
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

    /**
     * Failed requests will have correlationid. Azure webservices are supposed
     * to follow the same protocol and return this.
     */
    private UUID mCorrelationId;

    private boolean mIsMultiResourceRefreshToken;

    private UserInfo mUserInfo;

    private String mTenantId;

    private AuthenticationStatus mStatus = AuthenticationStatus.Failed;

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
            UserInfo userInfo) {
        mCode = null;
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mExpiresOn = expires;
        mIsMultiResourceRefreshToken = isBroad;
        mStatus = AuthenticationStatus.Succeeded;
        mUserInfo = userInfo;
    }

    AuthenticationResult(String accessToken, String refreshToken, Date expires, boolean isBroad) {
        mCode = null;
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mExpiresOn = expires;
        mIsMultiResourceRefreshToken = isBroad;
        mStatus = AuthenticationStatus.Succeeded;
    }

    AuthenticationResult(String errorCode, String errDescription, UUID correlationId) {
        mErrorCode = errorCode;
        mErrorDescription = errDescription;
        mStatus = AuthenticationStatus.Failed;
        mCorrelationId = correlationId;
    }

    public String createAuthorizationHeader() {
        return AuthenticationConstants.AAD.BEARER + " " + getAccessToken();
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public String getAccessTokenType() {
        return mTokenType;
    }

    public Date getExpiresOn() {
        return mExpiresOn;
    }

    public boolean getIsMultiResourceRefreshToken() {
        return mIsMultiResourceRefreshToken;
    }

    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    public String getTenantId() {
        return mTenantId;
    }

    public AuthenticationStatus getStatus() {
        return mStatus;
    }

    String getCode() {
        return mCode;
    }

    void setCode(String code) {
        mCode = code;
    }

    public String getErrorCode() {
        return mErrorCode;
    }

    public String getErrorDescription() {
        return mErrorDescription;
    }

    public UUID getCorrelationId() {
        return mCorrelationId;
    }

    public String getErrorLogInfo() {
        return " CorrelationId:" + getCorrelationId() + " ErrorCode:" + getErrorCode()
                + " ErrorDescription:" + getErrorDescription();
    }

    public void setCorrelationId(UUID correlation) {
        mCorrelationId = correlation;
    }
}
