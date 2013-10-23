/**
 * ---------------------------------------------------------------- 
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 * ----------------------------------------------------------------
 */

package com.microsoft.adal;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import android.test.suitebuilder.TestSuiteBuilder.FailedToCreateTests;


/**
 * Result class to keep code, token and other info Serializable properties Mark
 * temp properties as Transient if you dont want to keep them in serialization
 * 
 * @author omercan
 */
public class AuthenticationResult implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int TOKEN_VALIDITY_WINDOW_MINUTES = 5;
    private String mAccessToken;
    private String mCode;
    private String mAuthority;
    private String mAuthorizationEndpoint;
    private String mTokenEndpoint;
    private String mRefreshToken;
    private String mResource;
    private String mClientId;
    private String mScope;
    private String mAccessTokenType;
    private Date mExpires;
    private String mResponseType;
    private String mErrorCode; // Oauth
    private String mErrorDescription; // Oauth
    private boolean mBroadRefreshToken = false;
    private UserInfo mIdToken;
    AuthenticationStatus mStatus = AuthenticationStatus.Failed;

    public enum AuthenticationStatus
    {
        Cancelled, Failed, Succeeded,
    }

    public AuthenticationResult() {
        mAccessToken = null;
        mRefreshToken = null;
        setBroadRefreshToken(false);
        mStatus = AuthenticationStatus.Succeeded;
        }

    public AuthenticationResult(String authority, String clientId, String resource,
            String redirectUri)
    {
        mAccessToken = null;
        mRefreshToken = null;
        mAuthority = authority;
        mClientId = clientId;
        mResource = resource;
        setBroadRefreshToken(false);
        mStatus = AuthenticationStatus.Succeeded;
    }

    public AuthenticationResult(AuthenticationContext authenticationContext, String resource) {
        mAccessToken = null;
        mRefreshToken = null;
        mAuthority = authenticationContext.getAuthority();
        mResource = resource;
        setBroadRefreshToken(false);
        mStatus = AuthenticationStatus.Succeeded;
    }

    public AuthenticationResult(AuthenticationRequest request)
    {
        mAuthority = request.getAuthority();
        mResource = request.getResource();
        mClientId = request.getClientId(); // Asking token from different clientid can ask for confirmaiton
        setBroadRefreshToken(false);
        mStatus = AuthenticationStatus.Succeeded;
    }

    public AuthenticationResult(String errocode, String errDescription) {
        setErrorCode(errocode);
        setErrorDescription(errDescription);
        mStatus = AuthenticationStatus.Failed;
    }

    /**
     * Returns key that helps to find access token info resource and scope
     * 
     * @return
     */
    public String getCacheKey() {
        return new AuthenticationRequest(mAuthority, mClientId, null, mResource).getCacheKey();
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String mAccessToken) {
        this.mAccessToken = mAccessToken;
    }

    public String getCode() {
        return mCode;
    }

    public void setCode(String mCode) {
        this.mCode = mCode;
    }

    public String getAuthorizationEndpoint() {
        return mAuthorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String mAuthorizationEndpoint) {
        this.mAuthorizationEndpoint = mAuthorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return mTokenEndpoint;
    }

    public void setTokenEndpoint(String mTokenEndpoint) {
        this.mTokenEndpoint = mTokenEndpoint;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public void setRefreshToken(String mRefreshToken) {
        this.mRefreshToken = mRefreshToken;
    }

    public String getResource() {
        return mResource;
    }

    public void setResource(String mResource) {
        this.mResource = mResource;
    }

    public String getScope() {
        return mScope;
    }

    public void setScope(String mScope) {
        this.mScope = mScope;
    }

    public String getAccessTokenType() {
        return mAccessTokenType;
    }

    public void setAccessTokenType(String mAccessTokenType) {
        this.mAccessTokenType = mAccessTokenType;
    }

    public Date getExpires() {
        return mExpires;
    }

    public void setExpires(Date mExpires) {
        this.mExpires = mExpires;
    }

    public String getResponseType() {
        return mResponseType;
    }

    public void setResponseType(String mResponseType) {
        this.mResponseType = mResponseType;
    }

    /**
     * Get the time from now plus TOKEN_VALIDITY_WINDOW_MINUTES
     * 
     * @return the latest Time that the token is considered to be valid.
     */
    static private Calendar getTokenValidityTime() {
        Calendar timeAhead = Calendar.getInstance();
        timeAhead.roll(Calendar.MINUTE, TOKEN_VALIDITY_WINDOW_MINUTES);
        return timeAhead;
    }

    public boolean isExpired() {
        Date validity = getTokenValidityTime().getTime();

        if (mExpires.before(validity))
            return true;

        return false;
    }

    public boolean isRefreshable() {
        return null != mRefreshToken;
    }

    public boolean IsBroadRefreshToken() {
        return mBroadRefreshToken;
    }

    public void setBroadRefreshToken(boolean mBroadRefreshToken) {
        this.mBroadRefreshToken = mBroadRefreshToken;
    }

    public UserInfo getIdToken() {
        return mIdToken;
    }

    public void setIdToken(UserInfo mIdToken) {
        this.mIdToken = mIdToken;
    }

    public AuthenticationStatus getStatus() {
        // TODO Auto-generated method stub
        if(mStatus != AuthenticationStatus.Failed && 
                (getAccessToken() != null || getCode() != null))
            return AuthenticationStatus.Succeeded;
        
        return AuthenticationStatus.Failed;
    }

    public String getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(String mErrorCode) {
        this.mErrorCode = mErrorCode;
    }

    public String getErrorDescription() {
        return mErrorDescription;
    }

    public void setErrorDescription(String mErrorDescription) {
        this.mErrorDescription = mErrorDescription;
    }
}
