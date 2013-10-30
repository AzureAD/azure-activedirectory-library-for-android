
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

    /**
     * RequestAuthEndpoint to append in authority url
     */
    private final static String AUTH_ENDPOINT_APPEND = "/oauth2/authorize";

    /**
     * RequesttokenEndpoint to append in authority url
     */
    private final static String TOKEN_ENDPOINT_APPEND = "/oauth2/token";

    private String mCode = null;

    private String mAuthority = null;

    private String mAuthorizationEndpoint = null;

    private String mTokenEndpoint = null;

    private String mRedirectUri = null;

    private String mResource = null;

    private String mClientId = null;

    private String mResponseType = null;

    private String mLoginHint = null;

    private int mRequestCode;

    private UUID mCorrelationId;

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

    public String getCodeRequestUrl() throws UnsupportedEncodingException {

        StringBuffer requestUrlBuffer = new StringBuffer();
        requestUrlBuffer.append(String.format(
                "%s?response_type=%s&client_id=%s&resource=%s&redirect_uri=%s&state=%s",
                getAuthority() + AUTH_ENDPOINT_APPEND, AuthenticationConstants.OAuth2.CODE,
                getClientId(),
                URLEncoder.encode(getResource(), AuthenticationConstants.ENCODING_UTF8),
                URLEncoder.encode(getRedirectUri(), AuthenticationConstants.ENCODING_UTF8),
                encodeProtocolState()));

        if (getLoginHint() != null && !getLoginHint().isEmpty()) {
            requestUrlBuffer.append(String.format("&%s=%s", AuthenticationConstants.AAD.LOGIN_HINT,
                    URLEncoder.encode(getLoginHint(), AuthenticationConstants.ENCODING_UTF8)));
        }

        return requestUrlBuffer.toString();
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
        if (mAuthorizationEndpoint == null || mAuthorizationEndpoint.isEmpty()) {
            mAuthorizationEndpoint = mAuthority + AUTH_ENDPOINT_APPEND;
        }
        return mAuthorizationEndpoint;
    }

    public String getTokenEndpoint() {
        if (mTokenEndpoint == null || mTokenEndpoint.isEmpty()) {
            mTokenEndpoint = mAuthority + TOKEN_ENDPOINT_APPEND;
        }
        return mTokenEndpoint;
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

    public static String decodeProtocolState(String encodedState) {
        byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING | Base64.URL_SAFE);

        return new String(stateBytes);
    }

    private String encodeProtocolState() {
        String state = String.format("a=%s&r=%s", mAuthority, mResource);
        return Base64.encodeToString(state.getBytes(), Base64.NO_PADDING | Base64.URL_SAFE);
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
}
