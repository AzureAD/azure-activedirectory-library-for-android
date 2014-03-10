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

package com.microsoft.adal;

import java.io.Serializable;
import java.util.UUID;

/**
 * represent request and keeps authorization code and similar info
 * 
 * @author omercan
 */
public class AuthenticationRequest implements Serializable {

    private final static long serialVersionUID = 1L;

    private int mRequestId = 0;

    private String mAuthority = null;

    private String mRedirectUri = null;

    private String mResource = null;

    private String mClientId = null;

    private String mLoginHint = null;
    
    private String mBrokerAccountName = null;

    private UUID mCorrelationId;

    private String mExtraQueryParamsAuthentication;

    private PromptBehavior mPrompt;

    public AuthenticationRequest() {

    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
            String loginhint, PromptBehavior prompt, String extraQueryParams, UUID correlationId) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
        mPrompt = prompt;
        mExtraQueryParamsAuthentication = extraQueryParams;
        mCorrelationId = correlationId;
    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
            String loginhint, UUID requestCorrelationId) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
        mCorrelationId = requestCorrelationId;
    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
            String loginhint) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
    }

    public AuthenticationRequest(String authority, String resource, String clientid) {
        mAuthority = authority;
        mResource = resource;
        mClientId = clientid;
    } 
    
    public AuthenticationRequest(String authority, String resource, String clientid,
            String redirectUri) {
        mAuthority = authority;
        mResource = resource;
        mClientId = clientid;
        mRedirectUri = redirectUri;
    } 
    
    public AuthenticationRequest(String authority, String resource, String clientId, UUID correlationId) {
        mAuthority = authority;
        mClientId = clientId;
        mResource = resource;
        mCorrelationId = correlationId;
    }

    public String getAuthority() {
        return mAuthority;
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
        // directly access values without getter to make it fast
        String correlation = "";
        if(mCorrelationId != null){
            correlation = mCorrelationId.toString();
        }
        
        return String.format("Request authority:%s resource:%s clientid:%s correlationId:%s", mAuthority, mResource,
                mClientId, correlation);
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

    public void setBrokerAccountName(String mBrokerAccountName) {
        this.mBrokerAccountName = mBrokerAccountName;
    }
}
