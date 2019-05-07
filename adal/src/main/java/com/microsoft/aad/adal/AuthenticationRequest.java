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

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.io.Serializable;
import java.util.List;
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

    private String mClaimsChallenge;

    private transient InstanceDiscoveryMetadata mInstanceDiscoveryMetadata;

    private boolean mForceRefresh = false;

    private boolean mSkipCache = false;

    private String mAppName;

    private String mAppVersion;

    private List<String> mClientCapabilities;

    /**
     * Developer can use acquireToken(with loginhint) or acquireTokenSilent(with
     * userid), so this sets the type of the request.
     */
    enum UserIdentifierType {
        UniqueId, LoginHint, NoUser
    }

    AuthenticationRequest() {
        mIdentifierType = UserIdentifierType.NoUser;
    }

    AuthenticationRequest(String authority, String resource, String client, String redirect,
                          String loginhint, PromptBehavior prompt, String extraQueryParams, UUID correlationId,
                          boolean isExtendedLifetimeEnabled, final String claimsChallenge) {
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
        mClaimsChallenge = claimsChallenge;
    }

    AuthenticationRequest(String authority, String resource, String client, String redirect,
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

    AuthenticationRequest(String authority, String resource, String client, String redirect,
                          String loginhint, boolean isExtendedLifetimeEnabled) {
        mAuthority = authority;
        mResource = resource;
        mClientId = client;
        mRedirectUri = redirect;
        mLoginHint = loginhint;
        mBrokerAccountName = mLoginHint;
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
    }

    AuthenticationRequest(String authority, String resource, String clientid, boolean isExtendedLifetimeEnabled) {
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
    AuthenticationRequest(String authority, String resource, String clientid, String userid,
                          UUID correlationId, boolean isExtendedLifetimeEnabled) {
        mAuthority = authority;
        mResource = resource;
        mClientId = clientid;
        mUserId = userid;
        mCorrelationId = correlationId;
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
    }

    AuthenticationRequest(String authority, String resource, String clientid, String userid,
                          UUID correlationId, boolean isExtendedLifetimeEnabled, boolean forceRefresh, String claimsChallenge) {
        mAuthority = authority;
        mResource = resource;
        mClientId = clientid;
        mUserId = userid;
        mCorrelationId = correlationId;
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
        mForceRefresh = forceRefresh;
        mClaimsChallenge = claimsChallenge;
    }

    AuthenticationRequest(String authority, String resource, String clientId,
                          UUID correlationId, boolean isExtendedLifetimeEnabled) {
        mAuthority = authority;
        mClientId = clientId;
        mResource = resource;
        mCorrelationId = correlationId;
        mIsExtendedLifetimeEnabled = isExtendedLifetimeEnabled;
    }

    public boolean isClaimsChallengePresent() {
        // if developer pass claims down through extra qp, we should also skip cache.
        return !StringExtensions.isNullOrBlank(this.getClaimsChallenge());
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
    
    public void setRedirectUri(final String redirectUri) {
        mRedirectUri = redirectUri;
    }

    public String getResource() {
        return mResource;
    }

    public String getClientId() {
        return mClientId;
    }
    
    public void setClientId(final String id) {
        mClientId = id;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public UUID getCorrelationId() {
        return this.mCorrelationId;
    }
    
    public void setCorrelationId(UUID correlationId) {
        mCorrelationId = correlationId;
    }

    public String getExtraQueryParamsAuthentication() {
        return mExtraQueryParamsAuthentication;
    }

    public void setExtraQueryParamsAuthentication(String queryParam) {
        mExtraQueryParamsAuthentication = queryParam;
    }
    
    public String getLogInfo() {
        return String.format("Request authority:%s clientid:%s", mAuthority, mClientId);
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

    public void setUserName(String name){
        mLoginHint = name;
        mBrokerAccountName = name;
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

    public void setResource(String resource) {
        this.mResource = resource;
    }

    public boolean getIsExtendedLifetimeEnabled() {
        return mIsExtendedLifetimeEnabled;
    }

    public void setClaimsChallenge(final String claimsChallenge) {
        mClaimsChallenge = claimsChallenge;
    }

    public String getClaimsChallenge() {
        return mClaimsChallenge;
    }

    void setSkipCache(final boolean skipCache) {
        mSkipCache = skipCache;
    }

    public boolean getSkipCache() {
        return mSkipCache;
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

    void setInstanceDiscoveryMetadata(final InstanceDiscoveryMetadata metadata) {
        mInstanceDiscoveryMetadata = metadata;
    }

    InstanceDiscoveryMetadata getInstanceDiscoveryMetadata() {
        return mInstanceDiscoveryMetadata;
    }

    public boolean getForceRefresh(){
        return mForceRefresh;
    }

    public void setForceRefresh(boolean forceRefresh){
        mForceRefresh = forceRefresh;
    }

    public String getAppName() {
        return mAppName;
    }

    public void setAppName(String appName) {
        mAppName = appName;
    }

    public String getAppVersion() {
        return mAppVersion;
    }

    public void setAppVersion(String appVersion) {
        mAppVersion = appVersion;
    }

    public List<String> getClientCapabilities() {
        return mClientCapabilities;
    }

    public void setClientCapabilities(final List<String> clientCapabilities) {
        this.mClientCapabilities = clientCapabilities;
    }
}