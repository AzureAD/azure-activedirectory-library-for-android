package com.microsoft.adal;
/**
 * ---------------------------------------------------------------- 
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 * ----------------------------------------------------------------
 */

/**
 * Request related properties are stored here
 * 
 * @author omercan
 * 
 */
public class TokenRequest {

	private String mClientId;
	private String mRedirectUri;
	private String mAuthority;
	private String mResource;
	private String mLoginHint;
	private String mScope;

	public String getClientId() {
		return mClientId;
	}

	public void setClientId(String clientId) {
		this.mClientId = clientId;
	}

	public String getRedirectUri() {
		return mRedirectUri;
	}

	public void setRedirectUri(String redirectUri) {
		this.mRedirectUri = redirectUri;
	}

	public String getAuthority() {
		return mAuthority;
	}

	public void setAuthority(String authority) {
		this.mAuthority = authority;
	}

	public String getResource() {
		return mResource;
	}

	public void setResource(String resource) {
		this.mResource = resource;
	}

	public String getLoginHint() {
		return mLoginHint;
	}

	public void setLoginHint(String loginHint) {
		this.mLoginHint = loginHint;
	}

	public String getScope() {
		return mScope;
	}

	public void setScope(String val) {
		this.mScope = val;
	}

	public void Validate() {
		// TODO pass in validate behaviour for different conditions
		if (this.mResource == null)
			throw new IllegalArgumentException("resource");

		if (this.mAuthority == null)
			throw new IllegalArgumentException("authority");

		if (this.mRedirectUri == null)
			throw new IllegalArgumentException("redirectUri");

	}

}