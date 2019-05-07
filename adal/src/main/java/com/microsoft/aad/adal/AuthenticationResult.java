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

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;
import com.microsoft.identity.common.adal.internal.util.HashMapExtensions;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;

import org.json.JSONException;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.microsoft.aad.adal.TelemetryUtils.CliTelemInfo;

/**
 * Result class to keep code, token and other info Serializable properties Mark
 * temp properties as Transient if you dont want to keep them in serialization.
 */
public class AuthenticationResult implements Serializable {

    /**
     * Serial version number for serialization.
     */
    private static final long serialVersionUID = 2243372613182536368L;

    private ClientInfo mClientInfo;
    private String mResource;

    /**
     * Status for authentication.
     */
    public enum AuthenticationStatus {
        /**
         * User cancelled login activity.
         */
        Cancelled,
        /**
         * request has errors.
         */
        Failed,
        /**
         * token is acquired.
         */
        Succeeded,
    }

    private String mCode;

    protected String mAccessToken;

    protected String mRefreshToken;

    private String mTokenType;

    protected Date mExpiresOn;

    //Number of seconds the token is valid
    private Long mExpiresIn;

    //Number of milliseconds since the unix epoch
    private Long mResponseReceived;

    protected String mErrorCode;

    protected String mErrorDescription;

    protected String mErrorCodes;

    protected boolean mIsMultiResourceRefreshToken;

    protected UserInfo mUserInfo;

    protected String mTenantId;

    protected String mIdToken;

    protected AuthenticationStatus mStatus = AuthenticationStatus.Failed;

    protected boolean mInitialRequest;

    protected String mFamilyClientId;

    protected boolean mIsExtendedLifeTimeToken = false;

    protected Date mExtendedExpiresOn;

    protected String mAuthority;

    protected CliTelemInfo mCliTelemInfo;

    protected HashMap<String, String> mHttpResponseBody = null;

    protected int mServiceStatusCode = -1;

    protected HashMap<String, List<String>> mHttpResponseHeaders = null;

    private String mClientId;

    AuthenticationResult() {
        mCode = null;
    }

    AuthenticationResult(final String clientId, final String code) {
        mClientId = clientId;
        mCode = code;
        mStatus = AuthenticationStatus.Succeeded;
        mAccessToken = null;
        mRefreshToken = null;
    }

    AuthenticationResult(final String accessToken,
                         final String refreshToken,
                         final Date expires,
                         final boolean isBroad,
                         final UserInfo userInfo,
                         final String tenantId,
                         final String idToken,
                         final Date extendedExpires,
                         final String clientId) {
        mCode = null;
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mExpiresOn = expires;
        mIsMultiResourceRefreshToken = isBroad;
        mStatus = AuthenticationStatus.Succeeded;
        mUserInfo = userInfo;
        mTenantId = tenantId;
        mIdToken = idToken;
        mExtendedExpiresOn = extendedExpires;
        mClientId = clientId;
    }

    AuthenticationResult(String errorCode, String errDescription, String errorCodes) {
        mErrorCode = errorCode;
        mErrorDescription = errDescription;
        mErrorCodes = errorCodes;
        mStatus = AuthenticationStatus.Failed;
    }

    /**
     * Creates result from {@link TokenCacheItem}.
     *
     * @param cacheItem TokenCacheItem to be converted.
     * @return AuthenticationResult
     */
    static AuthenticationResult createResult(final TokenCacheItem cacheItem) {

        if (cacheItem == null) {
            AuthenticationResult result = new AuthenticationResult();
            result.mStatus = AuthenticationStatus.Failed;
            return result;
        }

        final AuthenticationResult result =
                new AuthenticationResult(
                        cacheItem.getAccessToken(),
                        cacheItem.getRefreshToken(),
                        cacheItem.getExpiresOn(),
                        cacheItem.getIsMultiResourceRefreshToken(),
                        cacheItem.getUserInfo(),
                        cacheItem.getTenantId(),
                        cacheItem.getRawIdToken(),
                        cacheItem.getExtendedExpiresOn(),
                        cacheItem.getClientId()
                );

        final CliTelemInfo cliTelemInfo = new CliTelemInfo();
        cliTelemInfo._setSpeRing(cacheItem.getSpeRing());
        result.setCliTelemInfo(cliTelemInfo);

        return result;
    }

    static AuthenticationResult createResultForInitialRequest(final String clientId) {
        AuthenticationResult result = new AuthenticationResult();
        result.mInitialRequest = true;
        result.mClientId = clientId;
        return result;
    }

    static AuthenticationResult createExtendedLifeTimeResult(final TokenCacheItem accessTokenItem) {
        final AuthenticationResult retryResult = createResult(accessTokenItem);
        retryResult.setExpiresOn(retryResult.getExtendedExpiresOn());
        retryResult.setIsExtendedLifeTimeToken(true);
        return retryResult;
    }

    /**
     * Uses access token to create header for web requests.
     *
     * @return AuthorizationHeader
     */
    public String createAuthorizationHeader() {
        return AuthenticationConstants.AAD.BEARER + " " + getAccessToken();
    }

    /**
     * Access token to send to the service in Authorization Header.
     *
     * @return Access token
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Refresh token to get new tokens.
     *
     * @return Refresh token
     */
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * Token type.
     *
     * @return access token type
     */
    public String getAccessTokenType() {
        return mTokenType;
    }

    /**
     * Epoch time for expiresOn.
     *
     * @return expiresOn {@link Date}
     */
    public Date getExpiresOn() {
        return DateExtensions.createCopy(mExpiresOn);
    }

    public Long getExpiresIn() {
        return mExpiresIn;
    }

    public void setExpiresIn(final Long expiresIn) {
        mExpiresIn = expiresIn;
    }

    public Long getResponseReceived() {
        return mResponseReceived;
    }

    public void setResponseReceived(final Long responseReceived) {
        mResponseReceived = responseReceived;
    }

    /**
     * Multi-resource refresh tokens can be used to request token for another
     * resource.
     *
     * @return multi resource refresh token status
     */
    public boolean getIsMultiResourceRefreshToken() {
        return mIsMultiResourceRefreshToken;
    }

    /**
     * UserInfo returned from IdToken.
     *
     * @return {@link UserInfo}
     */
    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    /**
     * Set userinfo after refresh from previous idtoken.
     *
     * @param userinfo latest user info.
     */
    void setUserInfo(UserInfo userinfo) {
        mUserInfo = userinfo;
    }

    /**
     * Gets tenantId.
     *
     * @return TenantId
     */
    public String getTenantId() {
        return mTenantId;
    }

    /**
     * Gets status.
     *
     * @return {@link AuthenticationStatus}
     */
    public AuthenticationStatus getStatus() {
        return mStatus;
    }

    String getCode() {
        return mCode;
    }

    void setCode(String code) {
        mCode = code;
    }

    /**
     * Gets error code.
     *
     * @return Error code
     */
    public String getErrorCode() {
        return mErrorCode;
    }

    /**
     * Gets error description.
     *
     * @return error description
     */
    public String getErrorDescription() {
        return mErrorDescription;
    }

    /**
     * Gets error log info.
     *
     * @return log info
     */
    public String getErrorLogInfo() {
        return " ErrorCode:" + getErrorCode();
    }

    /**
     * Checks expiration time.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        if (mIsExtendedLifeTimeToken) {
            return TokenCacheItem.isTokenExpired(getExtendedExpiresOn());
        }

        return TokenCacheItem.isTokenExpired(getExpiresOn());
    }

    /**
     * The token returned is cached with this authority as key.
     * We expect the subsequent requests to AcquireToken will use this authority as the authority parameter else
     * AcquireTokenSilent will fail
     *
     * @return Authority
     */
    public final String getAuthority() {
        return mAuthority;
    }

    String[] getErrorCodes() {
        return (mErrorCodes != null) ? mErrorCodes.replaceAll("[\\[\\]]", "").split("([^,]),") : null;
    }

    boolean isInitialRequest() {
        return mInitialRequest;
    }

    /**
     * Get raw idtoken.
     *
     * @return IdToken
     */
    public String getIdToken() {
        return mIdToken;
    }

    /**
     * Gets if the returned token is valid in terms of extended lifetime.
     *
     * @return True if the returned token is valid in terms of extended lifetime
     */
    public boolean isExtendedLifeTimeToken() {
        return mIsExtendedLifeTimeToken;
    }

    /**
     * Sets the flag to indicate whether the token being returned is a token only
     * valid in terms of extended lifetime.
     *
     * @param isExtendedLifeTimeToken
     */
    final void setIsExtendedLifeTimeToken(final boolean isExtendedLifeTimeToken) {
        mIsExtendedLifeTimeToken = isExtendedLifeTimeToken;
    }

    final void setExtendedExpiresOn(final Date extendedExpiresOn) {
        mExtendedExpiresOn = extendedExpiresOn;
    }

    final Date getExtendedExpiresOn() {
        return mExtendedExpiresOn;
    }

    final void setExpiresOn(final Date expiresOn) {
        mExpiresOn = expiresOn;
    }

    void setIdToken(String idToken) {
        this.mIdToken = idToken;
    }

    void setTenantId(String tenantid) {
        mTenantId = tenantid;
    }

    void setRefreshToken(String refreshToken) {
        mRefreshToken = refreshToken;
    }

    final String getFamilyClientId() {
        return mFamilyClientId;
    }

    final void setFamilyClientId(final String familyClientId) {
        mFamilyClientId = familyClientId;
    }

    public final void setAuthority(final String authority) {
        if (!StringExtensions.isNullOrBlank(authority)) {
            mAuthority = authority;
        }
    }

    public final CliTelemInfo getCliTelemInfo() {
        return mCliTelemInfo;
    }

    final void setCliTelemInfo(final CliTelemInfo cliTelemInfo) {
        mCliTelemInfo = cliTelemInfo;
    }

    void setHttpResponseBody(final HashMap<String, String> body) {
        mHttpResponseBody = body;
    }

    /**
     * Get Http response message.
     *
     * @return HttpResponseBody
     */
    public HashMap<String, String> getHttpResponseBody() {
        return mHttpResponseBody;
    }

    void setHttpResponseHeaders(final HashMap<String, List<String>> headers) {
        mHttpResponseHeaders = headers;
    }

    /**
     * Get Http response headers.
     *
     * @return HttpResponseHeaders
     */
    public HashMap<String, List<String>> getHttpResponseHeaders() {
        return mHttpResponseHeaders;
    }

    void setServiceStatusCode(int statusCode) {
        mServiceStatusCode = statusCode;
    }

    /**
     * Get service status code.
     *
     * @return ServiceStatusCode
     */
    public int getServiceStatusCode() {
        return mServiceStatusCode;
    }

    void setHttpResponse(final HttpWebResponse response) {
        if (null != response) {
            mServiceStatusCode = response.getStatusCode();

            if (null != response.getResponseHeaders()) {
                mHttpResponseHeaders = new HashMap<>(response.getResponseHeaders());
            }

            if (null != response.getBody()) {
                try {
                    mHttpResponseBody = new HashMap<>(HashMapExtensions.getJsonResponse(response));
                } catch (final JSONException exception) {
                    Logger.e(AuthenticationException.class.getSimpleName(), "Json exception",
                            ExceptionExtensions.getExceptionMessage(exception),
                            ADALError.SERVER_INVALID_JSON_RESPONSE);
                }
            }
        }
    }

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(final String clientId) {
        mClientId = clientId;
    }

    /**
     * Sets the ClientInfo.
     *
     * @param clientInfo The ClientInfo to set.
     */
    void setClientInfo(final ClientInfo clientInfo) {
        mClientInfo = clientInfo;
    }

    /**
     * Gets the {@link ClientInfo}.
     *
     * @return The ClientInfo to get or null (if the broker was used to acquire tokens).
     */
    public ClientInfo getClientInfo() {
        return mClientInfo;
    }

    /**
     * Sets the resource of this AuthenticationResult.
     *
     * @param resource The resource to set.
     */
    void setResource(String resource) {
        mResource = resource;
    }

    /**
     * Gets the resource of this AuthenticationResult.
     *
     * @return The resource to get.
     */
    public String getResource() {
        return mResource;
    }

}
