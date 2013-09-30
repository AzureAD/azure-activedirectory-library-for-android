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
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.util.Base64;

/**
 * Request related info
 * @author omercan
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
    private UUID mCorrelationId;
    
    public AuthenticationRequest(String authority, String client, String resource, String scope,
            String redirect, String loginhint)
    {
        mAuthority = authority;
        mClientId = client;
        mResource = resource;
        mScope = scope;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
    }

    public AuthenticationRequest(AuthenticationContext authenticationContext, String clientid, String redirectUri, String resource) {
        mAuthority = authenticationContext.getAuthority();
        mClientId = clientid;
        mResource = resource;
        mRedirectUri = redirectUri;
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
        if (mAuthorizationEndpoint == null || mAuthorizationEndpoint.isEmpty())
        {
            mAuthorizationEndpoint = mAuthority
                    + AuthenticationSettings.getInstance().getOauthRequestAuthEndpoint();
        }
        return mAuthorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String mAuthorizationEndpoint) {
        this.mAuthorizationEndpoint = mAuthorizationEndpoint;
    }

    public String getTokenEndpoint() {
        if (mTokenEndpoint == null || mTokenEndpoint.isEmpty())
        {
            mTokenEndpoint = mAuthority
                    + AuthenticationSettings.getInstance().getOauthRequestTokenEndpoint();
        }
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
                "%s?response_type=%s&client_id=%s&resource=%s&redirect_uri=%s&state=%s",
                getAuthority() + authorizationEndpoint,
                AuthenticationConstants.OAuth2.CODE,
                getClientId(),
                URLEncoder.encode(getResource(),
                        AuthenticationConstants.ENCODING_UTF8),
                URLEncoder.encode(getRedirectUri(),
                        AuthenticationConstants.ENCODING_UTF8),
                encodeProtocolState()
                ));

        if (getLoginHint() != null && !getLoginHint().isEmpty())
        {
            requestUrlBuffer.append(String.format("&%s=%s", AuthenticationConstants.AAD.LOGIN_HINT,
                    URLEncoder.encode(getLoginHint(),
                            AuthenticationConstants.ENCODING_UTF8)));
        }

        return requestUrlBuffer.toString();
    }

    public static String decodeProtocolState(String encodedState) {
        byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING
                | Base64.URL_SAFE);

        return new String(stateBytes);
    }

    private String encodeProtocolState() {
        // Note that a null scope is always encoded as "" for the protocol state
        String state = String.format("a=%s&r=%s&s=%s", mAuthority,
                mResource, null == mScope ? "" : mScope);

        return Base64.encodeToString(state.getBytes(), Base64.NO_PADDING
                | Base64.URL_SAFE);
    }

    /**
     * Returns key that helps to find access token info resource and scope
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

    public UUID getCorrelationId() {
        // TODO Auto-generated method stub
        return this.mCorrelationId;
    }
    
    public void setCorrelationId(UUID val) {
        // TODO Auto-generated method stub
        this.mCorrelationId = val;
    }
}
