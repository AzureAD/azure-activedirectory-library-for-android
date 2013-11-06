
package com.microsoft.adal;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import android.util.Base64;

/**
 * represent request and keeps authorization code and similar info
 * 
 * @author omercan
 */
class AuthenticationRequest implements Serializable {

    private final static long serialVersionUID = 1L;

    private String mCode = null;

    private String mAuthority = null;

    private String mRedirectUri = null;

    private String mResource = null;

    private String mClientId = null;

    private String mResponseType = null;

    private String mLoginHint = null;

    private int mRequestCode;

    private UUID mCorrelationId;

    private String mExtraQueryParamsAuthentication;
    
    public AuthenticationRequest() {

    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
            String loginhint) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;

    }

    public AuthenticationRequest(String authority, String resource, String clientid,
            String redirectUri) {
        mAuthority = authority;
        mResource = resource;
        mClientId = clientid;
        mRedirectUri = redirectUri;
    }

    public AuthenticationRequest(String authority, String clientId, String resource) {
        mAuthority = authority;
        mClientId = clientId;
        mResource = resource;
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

  

    private int getRequestCode() {
        return mRequestCode;
    }

    private void setRequestCode(int mRequestCode) {
        this.mRequestCode = mRequestCode;
    }

    public UUID getCorrelationId() {
        return this.mCorrelationId;
    }

    public void setCorrelationId(UUID val) {
        this.mCorrelationId = val;
    }

    public String getExtraQueryParamsAuthentication() {
        return mExtraQueryParamsAuthentication;
    }

    public void setExtraQueryParamsAuthentication(String mExtraQueryParamsAuthentication) {
        this.mExtraQueryParamsAuthentication = mExtraQueryParamsAuthentication;
    }
}
