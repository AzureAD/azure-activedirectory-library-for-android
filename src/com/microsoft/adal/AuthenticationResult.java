/**
 * ---------------------------------------------------------------- 
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 * ----------------------------------------------------------------
 */
package com.microsoft.adal;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;



/**
 * Serializable properties Mark temp properties as Transient if you dont want to
 * keep them in serialization
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
    private String mRedirectUri;
    private String mRefreshToken;
    private String mResource;
    private String mScope;
    private String mClientId;
    private String mAccessTokenType;
    private Date mExpires;
    private String mResponseType;
    private String mErrorCode; //Oauth
    private String mErrorDescription; //Oauth
    
    public AuthenticationResult() {
        mAccessToken = null;
        mRefreshToken = null;
    }

    public AuthenticationResult(String authority, String clientId, String resource,
            String redirectUri)
    {
        mAccessToken = null;
        mRefreshToken = null;
        mAuthority = authority;
        mClientId = clientId;
        mResource = resource;
        mRedirectUri = redirectUri;
    }

    public AuthenticationResult(AuthenticationContext authenticationContext, String resource) {
        mAccessToken = null;
        mRefreshToken = null;
        mAuthority = authenticationContext.getAuthority();
        mClientId = authenticationContext.getClientId();
        mResource = resource;
        mRedirectUri = authenticationContext.getRedirectUri();
    }

   

    /**
     * Returns key that helps to find access token info
     * resource and scope
     * 
     * @return
     */
    public String getTokenCacheKey() {
        return String.format("%s:%s:%s:%s:%s", mAuthority, mResource, mClientId, mRedirectUri,
                (mScope == null || mScope.isEmpty()) ? "" : mScope);
    }
    
    public String getAccessToken() {
        return mAccessToken;
    }

    public void setmccessToken(String mAccessToken) {
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

    public String getRedirectUri() {
        return mRedirectUri;
    }

    public void setRedirectUri(String mRedirectUri) {
        this.mRedirectUri = mRedirectUri;
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

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String mClientId) {
        this.mClientId = mClientId;
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
}
