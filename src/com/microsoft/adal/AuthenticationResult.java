package com.microsoft.adal;
import java.io.Serializable;
import java.util.Date;

/**
 * ---------------------------------------------------------------- 
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 * ----------------------------------------------------------------
 */

/**
 * Serializable properties
 * Mark temp properties as Transient if you dont want to keep them in serialization
 * @author omercan
 *
 */
public class AuthenticationResult implements Serializable {

	private String mAccessToken;
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
	private String mCode;
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
}


