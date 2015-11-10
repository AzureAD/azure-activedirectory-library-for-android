// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represent request and keeps authorization code and similar info.
 */
class AuthenticationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private int mRequestId = 0;

    private String mAuthority = null;

    private String mRedirectUri = null;

    private String mResource = null;

    private String mClientId = null;

    private String mLoginHint = null;

    private String mUserId = null;

    private String mBrokerAccountName = null;

    private UUID mCorrelationId;

    private String mExtraQueryParamsAuthentication;

    private PromptBehavior mPrompt;

    private boolean mSilent = false;

    private String mVersion = null;

    private UserIdentifierType mIdentifierType;

    /**
     * Developer can use acquiretoken(with loginhint) or acquireTokenSilent(with
     * userid), so this sets the type of the request.
     */
    enum UserIdentifierType {
        UniqueId, LoginHint, NoUser
    }

    public AuthenticationRequest() {
        mIdentifierType = UserIdentifierType.NoUser;
    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
            String loginhint, PromptBehavior prompt, String extraQueryParams, UUID correlationId) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
        mBrokerAccountName = mLoginHint;
        mPrompt = prompt;
        mExtraQueryParamsAuthentication = extraQueryParams;
        mCorrelationId = correlationId;
        mIdentifierType = UserIdentifierType.NoUser;
    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
            String loginhint, UUID requestCorrelationId) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
        mBrokerAccountName = mLoginHint;
        mCorrelationId = requestCorrelationId;
    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
            String loginhint) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
        mBrokerAccountName = mLoginHint;
    }

    public AuthenticationRequest(String authority, String resource, String clientid) {
        mAuthority = authority;
        mResource = resource;
        mClientId = clientid;
    }

    /**
     * Cache usage and refresh token requests.
     * 
     * @param authority
     * @param resource
     * @param clientid
     * @param userid
     * @param correlationId
     */
    public AuthenticationRequest(String authority, String resource, String clientid, String userid,
            UUID correlationId) {
        mAuthority = authority;
        mResource = resource;
        mClientId = clientid;
        mUserId = userid;
        mCorrelationId = correlationId;
    }

    public AuthenticationRequest(String authority, String resource, String clientId,
            UUID correlationId) {
        mAuthority = authority;
        mClientId = clientId;
        mResource = resource;
        mCorrelationId = correlationId;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(String authority) {
        mAuthority = authority;
    }

    public String getRedirectUri() {
        return mRedirectUri;
    }

    public String getResource() {
        return mResource;
    }

    public String getClientId() {
        return mClientId;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public UUID getCorrelationId() {
        return this.mCorrelationId;
    }

    public String getExtraQueryParamsAuthentication() {
        return mExtraQueryParamsAuthentication;
    }

    public String getLogInfo() {
        return String.format("Request authority:%s resource:%s clientid:%s", mAuthority, mResource,
                mClientId);
    }

    public PromptBehavior getPrompt() {
        return mPrompt;
    }

    public void setPrompt(PromptBehavior prompt) {
        this.mPrompt = prompt;
    }

    /**
     * @return the mRequestId related to the delegate
     */
    public int getRequestId() {
        return mRequestId;
    }

    /**
     * @param requestId the requestId to set
     */
    public void setRequestId(int requestId) {
        this.mRequestId = requestId;
    }

    public String getBrokerAccountName() {
        return mBrokerAccountName;
    }

    public void setBrokerAccountName(String brokerAccountName) {
        this.mBrokerAccountName = brokerAccountName;
    }

    void setLoginHint(String name) {
        mLoginHint = name;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String userId) {
        this.mUserId = userId;
    }

    public boolean isSilent() {
        return mSilent;
    }

    public void setSilent(boolean silent) {
        this.mSilent = silent;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        this.mVersion = version;
    }

    public UserIdentifierType getUserIdentifierType() {
        return mIdentifierType;
    }

    public void setUserIdentifierType(UserIdentifierType user) {
        mIdentifierType = user;
    }
}
