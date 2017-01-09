// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represent request and keeps authorization code and similar info.
 */
class AuthenticationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int DELIM_NOT_FOUND = -1;

    private static final String UPN_DOMAIN_SUFFIX_DELIM = "@";

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

    private boolean mIsExtendedLifetimeEnabled = false;

    private String mTelemetryRequestId;

    /**
     * Developer can use acquireToken(with loginhint) or acquireTokenSilent(with
     * userid), so this sets the type of the request.
     */
    enum UserIdentifierType {
        UniqueId, LoginHint, NoUser
    }

    public AuthenticationRequest() {
        mIdentifierType = UserIdentifierType.NoUser;
    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
                                 String loginhint, PromptBehavior prompt, String extraQueryParams, UUID correlationId, boolean isExtendedLifetimeEnabled) {
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
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
                                 String loginhint, UUID requestCorrelationId, boolean isExtendedLifetimeEnabled) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
        mBrokerAccountName = mLoginHint;
        mCorrelationId = requestCorrelationId;
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
    }

    public AuthenticationRequest(String authority, String resource, String client, String redirect,
                                 String loginhint, boolean isExtendedLifetimeEnabled) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
        mBrokerAccountName = mLoginHint;
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
    }

    public AuthenticationRequest(String authority, String resource, String clientid, boolean isExtendedLifetimeEnabled) {
        mAuthority = authority;
        mResource = resource;
        mClientId = clientid;
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
    }

    /**
     * Cache usage and refresh token requests.
     *
     * @param authority     Authority URL
     * @param resource      Resource that is requested
     * @param clientid      ClientId for the app
     * @param userid        user id
     * @param correlationId for logging
     */
    public AuthenticationRequest(String authority, String resource, String clientid, String userid,
                                 UUID correlationId, boolean isExtendedLifetimeEnabled) {
        mAuthority = authority;
        mResource = resource;
        mClientId = clientid;
        mUserId = userid;
        mCorrelationId = correlationId;
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
    }

    public AuthenticationRequest(String authority, String resource, String clientId,
                                 UUID correlationId, boolean isExtendedLifetimeEnabled) {
        mAuthority = authority;
        mClientId = clientId;
        mResource = resource;
        mCorrelationId = correlationId;
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
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

    public boolean getIsExtendedLifetimeEnabled() {
        return mIsExtendedLifetimeEnabled;
    }

    /**
     * Get either loginhint or user id based what's passed in the request.
     */
    String getUserFromRequest() {
        if (UserIdentifierType.LoginHint == mIdentifierType) {
            return mLoginHint;
        } else if (UserIdentifierType.UniqueId == mIdentifierType) {
            return mUserId;
        }

        return null;
    }

    /**
     * Gets the domain suffix of User Principal Name.
     *
     * @return the domain suffix or null if unavailable
     */
    @Nullable
    String getUpnSuffix() {
        final String hint = getLoginHint();
        String suffix = null;
        if (hint != null) {
            final int dIndex = hint.lastIndexOf(UPN_DOMAIN_SUFFIX_DELIM);
            suffix = DELIM_NOT_FOUND == dIndex ? null : hint.substring(dIndex + 1);
        }
        return suffix;
    }

    void setTelemetryRequestId(final String telemetryRequestId) {
        mTelemetryRequestId = telemetryRequestId;
    }

    String getTelemetryRequestId() {
        return mTelemetryRequestId;
    }
}
