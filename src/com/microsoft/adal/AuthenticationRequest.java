/**
 * ---------------------------------------------------------------- 
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 * ----------------------------------------------------------------
 */
package com.microsoft.adal;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;



/**
 * @author omercan
 *
 */
public class AuthenticationRequest implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String mCode;
    private String mAuthority;
    private String mAuthorizationEndpoint;
    private String mTokenEndpoint;
    private String mRedirectUri;
    private String mResource;
    private String mScope;
    private String mClientId;
    private String mResponseType;
    private String mLoginHint;
    private int mRequestCode;
   
    
    
    
    public AuthenticationRequest(String authority, String client, String resource, String scope, String redirect, String loginhint)
    {
        mAuthority = authority;
        mClientId = client;
        mResource = resource;
        mScope = scope;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
    }
    
    public AuthenticationRequest(AuthenticationContext authenticationContext, String resource) {
        mAuthority = authenticationContext.getAuthority();
        mClientId = authenticationContext.getClientId();
        mResource = resource;
        mRedirectUri = authenticationContext.getRedirectUri();
    }
    
    
    public String getCode() {
        return mCode;
    }
    public void setCode(String mCode) {
        this.mCode = mCode;
    }
    public String getAuthority() {
        return mAuthority;
    }
    public void setAuthority(String mAuthority) {
        this.mAuthority = mAuthority;
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
    public String getResponseType() {
        return mResponseType;
    }
    public void setResponseType(String mResponseType) {
        this.mResponseType = mResponseType;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public void setLoginHint(String mLoginHint) {
        this.mLoginHint = mLoginHint;
    }
    
    public String getCodeRequestUrl() throws UnsupportedEncodingException {
        String authorizationEndpoint = AuthenticationSettings.getInstance()
                .getOauthRequestAuthEndpoint();
        StringBuffer requestUrlBuffer = new StringBuffer();
        requestUrlBuffer.append(String.format(
                "%s?response_type=%s&client_id=%s&resource=%s&redirect_uri=%s&state=*",
                getAuthority() + authorizationEndpoint,
                AuthenticationConstants.OAuth2.CODE,
                getClientId(),
                URLEncoder.encode(getResource(),
                        AuthenticationConstants.ENCODING_UTF8),
                URLEncoder.encode(getRedirectUri(),
                        AuthenticationConstants.ENCODING_UTF8)
                ));
        
        if (!getLoginHint().isEmpty())
        {
            requestUrlBuffer.append(String.format("&%s=%s", AuthenticationConstants.AAD.LOGIN_HINT,
                    URLEncoder.encode(getLoginHint(),
                            AuthenticationConstants.ENCODING_UTF8)));
        }
        
        return requestUrlBuffer.toString();
    }
   
    /**
     * Returns key that helps to find access token info
     * resource and scope
     * 
     * @return
     */
    public String getCacheKey() {
        return String.format("%s:%s:%s:%s:%s", mAuthority, mResource, mClientId, mRedirectUri,
                (mScope == null || mScope.isEmpty()) ? "" : mScope);
    }

    private int getRequestCode() {
        return mRequestCode;
    }

    private void setRequestCode(int mRequestCode) {
        this.mRequestCode = mRequestCode;
    }
}
