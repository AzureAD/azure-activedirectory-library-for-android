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

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import com.microsoft.aad.adal.ChallengeResponseBuilder.ChallengeResponse;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;
import com.microsoft.identity.common.adal.internal.net.IWebRequestHandler;
import com.microsoft.identity.common.adal.internal.util.HashMapExtensions;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.aad.adal.TelemetryUtils.CliTelemInfo;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.HeaderField.X_MS_CLITELEM;

/**
 * Base Oauth class.
 */
class Oauth2 {

    private AuthenticationRequest mRequest;

    private IWebRequestHandler mWebRequestHandler;

    private IJWSBuilder mJWSBuilder = new JWSBuilder();

    private String mTokenEndpoint;

    private static final String TAG = "Oauth";

    private boolean mRetryOnce = true;

    private static final int DELAY_TIME_PERIOD = 1000;

    private static final int MAX_RESILIENCY_ERROR_CODE = 599;

    private static final String DEFAULT_AUTHORIZE_ENDPOINT = "/oauth2/authorize";

    private static final String DEFAULT_TOKEN_ENDPOINT = "/oauth2/token";

    private static final String HTTPS_PROTOCOL_STRING = "https";

    private static final String STRING_FORMAT_QUERY_PARAM = "%s&%s=%s";

    private String mBrokerClientVersion = "";

    private String mClientVersion = "";

    Oauth2(AuthenticationRequest request) {
        mRequest = request;
        mWebRequestHandler = null;
        mJWSBuilder = null;
        setTokenEndpoint(mRequest.getAuthority() + DEFAULT_TOKEN_ENDPOINT);
    }

    Oauth2(AuthenticationRequest request, IWebRequestHandler webRequestHandler) {
        mRequest = request;
        mWebRequestHandler = webRequestHandler;
        mJWSBuilder = null;
        setTokenEndpoint(mRequest.getAuthority() + DEFAULT_TOKEN_ENDPOINT);
    }

    Oauth2(AuthenticationRequest request, IWebRequestHandler webRequestHandler,
           IJWSBuilder jwsMessageBuilder) {
        mRequest = request;
        mWebRequestHandler = webRequestHandler;
        mJWSBuilder = jwsMessageBuilder;
        setTokenEndpoint(mRequest.getAuthority() + DEFAULT_TOKEN_ENDPOINT);
    }

    public void setClientVersion(String version) {
        mClientVersion = version;
        if (mWebRequestHandler != null) {
            mWebRequestHandler.setClientVersion(version);
        }
    }

    public void setBrokerClientVersion(String version) {
        mBrokerClientVersion = version;
    }

    public String getAuthorizationEndpoint() {
        return mRequest.getAuthority() + DEFAULT_AUTHORIZE_ENDPOINT;
    }

    public String getTokenEndpoint() {
        return mTokenEndpoint;
    }

    public String getAuthorizationEndpointQueryParameters() throws UnsupportedEncodingException {
        final Uri.Builder queryParameter = new Uri.Builder();
        queryParameter.appendQueryParameter(AuthenticationConstants.OAuth2.RESPONSE_TYPE,
                AuthenticationConstants.OAuth2.CODE)
                .appendQueryParameter(AuthenticationConstants.OAuth2.CLIENT_ID,
                        URLEncoder.encode(mRequest.getClientId(),
                                AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.AAD.RESOURCE,
                        URLEncoder.encode(mRequest.getResource(),
                                AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.OAuth2.REDIRECT_URI,
                        URLEncoder.encode(mRequest.getRedirectUri(),
                                AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.OAuth2.STATE, encodeProtocolState());

        if (!StringExtensions.isNullOrBlank(mRequest.getLoginHint())) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.LOGIN_HINT,
                    URLEncoder.encode(mRequest.getLoginHint(),
                            AuthenticationConstants.ENCODING_UTF8));
        }

        if (StringExtensions.isNullOrBlank(mClientVersion)) {
            mClientVersion = AuthenticationContext.getVersionName();
        }

        // append device and platform info in the query parameters
        queryParameter.appendQueryParameter(AuthenticationConstants.AAD.ADAL_ID_PLATFORM,
                AuthenticationConstants.AAD.ADAL_ID_PLATFORM_VALUE)

                .appendQueryParameter(AuthenticationConstants.AAD.ADAL_ID_VERSION,
                        URLEncoder.encode(mClientVersion, AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.AAD.ADAL_ID_OS_VER,
                        URLEncoder.encode(String.valueOf(Build.VERSION.SDK_INT),
                                AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.AAD.ADAL_ID_DM,
                        URLEncoder.encode(android.os.Build.MODEL,
                                AuthenticationConstants.ENCODING_UTF8));

        if (!StringExtensions.isNullOrBlank(mBrokerClientVersion)) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.ADAL_BROKER_VERSION,
                    URLEncoder.encode(mBrokerClientVersion, AuthenticationConstants.ENCODING_UTF8));
        }

        if (mRequest.getCorrelationId() != null) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.CLIENT_REQUEST_ID,
                    URLEncoder.encode(mRequest.getCorrelationId().toString(),
                            AuthenticationConstants.ENCODING_UTF8));
        }

        // Setting prompt behavior to always will skip the cookies for webview.
        // It is added to authorization url.
        if (mRequest.getPrompt() == PromptBehavior.Always) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.QUERY_PROMPT,
                    URLEncoder.encode(AuthenticationConstants.AAD.QUERY_PROMPT_VALUE,
                            AuthenticationConstants.ENCODING_UTF8));
        } else if (mRequest.getPrompt() == PromptBehavior.REFRESH_SESSION) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.QUERY_PROMPT,
                    URLEncoder.encode(
                            AuthenticationConstants.AAD.QUERY_PROMPT_REFRESH_SESSION_VALUE,
                            AuthenticationConstants.ENCODING_UTF8));
        }

        // reading extra qp supplied by developer
        final String extraQP = mRequest.getExtraQueryParamsAuthentication();
        // append haschrome=1 if developer does not pass as extra qp
        if (StringExtensions.isNullOrBlank(extraQP)
                || !extraQP.contains(AuthenticationConstants.OAuth2.HAS_CHROME)) {
            queryParameter.appendQueryParameter(AuthenticationConstants.OAuth2.HAS_CHROME, "1");
        }

        // Claims challenge are opaque to the sdk, we're not going to do any merging if both extra qp and claims parameter
        // contain it. Also, if developer sends it in both places, server will fail it.
        if (!StringExtensions.isNullOrBlank(mRequest.getClaimsChallenge())
                || mRequest.getClientCapabilities() != null) {
            queryParameter.appendQueryParameter(AuthenticationConstants.OAuth2.CLAIMS,
                    URLEncoder.encode(AuthenticationContext.mergeClaimsWithClientCapabilities(
                                    mRequest.getClaimsChallenge(),
                                    mRequest.getClientCapabilities()
                            ),
                            AuthenticationConstants.ENCODING_UTF8)
            );
        }

        if (!StringExtensions.isNullOrBlank(mRequest.getAppName())) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.APP_PACKAGE_NAME, mRequest.getAppName());
        }

        if (!StringExtensions.isNullOrBlank(mRequest.getAppVersion())) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.APP_VERSION, mRequest.getAppVersion());
        }

        String requestUrl = queryParameter.build().getQuery();
        if (!StringExtensions.isNullOrBlank(extraQP)) {
            String parsedQP = extraQP;
            if (!extraQP.startsWith("&")) {
                parsedQP = "&" + parsedQP;
            }
            requestUrl += parsedQP;
        }

        return requestUrl;
    }

    public String getCodeRequestUrl() throws UnsupportedEncodingException {
        return String.format("%s?%s", getAuthorizationEndpoint(),
                getAuthorizationEndpointQueryParameters());
    }

    public String buildTokenRequestMessage(String code) throws UnsupportedEncodingException {
        Logger.v(TAG, "Building request message for redeeming token with auth code.");

        String message = String.format("%s=%s&%s=%s&%s=%s&%s=%s&%s=%s",
                AuthenticationConstants.OAuth2.GRANT_TYPE,
                StringExtensions.urlFormEncode(AuthenticationConstants.OAuth2.AUTHORIZATION_CODE),

                AuthenticationConstants.OAuth2.CODE,
                StringExtensions.urlFormEncode(code),

                AuthenticationConstants.OAuth2.CLIENT_ID,
                StringExtensions.urlFormEncode(mRequest.getClientId()),

                AuthenticationConstants.OAuth2.REDIRECT_URI,
                StringExtensions.urlFormEncode(mRequest.getRedirectUri()),

                // Request client_info
                AuthenticationConstants.OAuth2.CLIENT_INFO,
                AuthenticationConstants.OAuth2.CLIENT_INFO_TRUE
        );

        if (!StringExtensions.isNullOrBlank(mRequest.getClaimsChallenge()) ||
                mRequest.getClientCapabilities() != null) {
            message = String.format(STRING_FORMAT_QUERY_PARAM, message, AuthenticationConstants.OAuth2.CLAIMS,
                    StringExtensions.urlFormEncode(AuthenticationContext.mergeClaimsWithClientCapabilities(
                            mRequest.getClaimsChallenge(),
                            mRequest.getClientCapabilities()
                    )));
        }

        if (!StringExtensions.isNullOrBlank(mRequest.getAppName())) {
            message = String.format(STRING_FORMAT_QUERY_PARAM, message, AuthenticationConstants.AAD.APP_PACKAGE_NAME,
                    StringExtensions.urlFormEncode(mRequest.getAppName()));
        }

        if (!StringExtensions.isNullOrBlank(mRequest.getAppVersion())) {
            message = String.format(STRING_FORMAT_QUERY_PARAM, message, AuthenticationConstants.AAD.APP_VERSION,
                    StringExtensions.urlFormEncode(mRequest.getAppVersion()));
        }
        return message;

    }

    public String buildRefreshTokenRequestMessage(String refreshToken)
            throws UnsupportedEncodingException {
        Logger.v(TAG, "Building request message for redeeming token with refresh token.");
        String message = String.format("%s=%s&%s=%s&%s=%s&%s=%s",
                AuthenticationConstants.OAuth2.GRANT_TYPE,
                StringExtensions.urlFormEncode(AuthenticationConstants.OAuth2.REFRESH_TOKEN),

                AuthenticationConstants.OAuth2.REFRESH_TOKEN,
                StringExtensions.urlFormEncode(refreshToken),

                AuthenticationConstants.OAuth2.CLIENT_ID,
                StringExtensions.urlFormEncode(mRequest.getClientId()),

                AuthenticationConstants.OAuth2.CLIENT_INFO,
                AuthenticationConstants.OAuth2.CLIENT_INFO_TRUE
        );

        if (!StringExtensions.isNullOrBlank(mRequest.getResource())) {
            message = String.format(STRING_FORMAT_QUERY_PARAM, message, AuthenticationConstants.AAD.RESOURCE,
                    StringExtensions.urlFormEncode(mRequest.getResource()));
        }

        // sending redirect uri for the refresh token request if it's provided
        if (!StringExtensions.isNullOrBlank(mRequest.getRedirectUri())
                && !mRequest.getClientId().equalsIgnoreCase(AuthenticationConstants.Broker.BROKER_CLIENT_ID)) {
            message = String.format(STRING_FORMAT_QUERY_PARAM, message, AuthenticationConstants.OAuth2.REDIRECT_URI,
                    StringExtensions.urlFormEncode(mRequest.getRedirectUri()));
        }

        if (!StringExtensions.isNullOrBlank(mRequest.getClaimsChallenge()) ||
                mRequest.getClientCapabilities() != null) {
            message = String.format(STRING_FORMAT_QUERY_PARAM, message, AuthenticationConstants.OAuth2.CLAIMS,
                    StringExtensions.urlFormEncode(AuthenticationContext.mergeClaimsWithClientCapabilities(
                                    mRequest.getClaimsChallenge(),
                                    mRequest.getClientCapabilities()
                            )
                    )
            );
        }

        if (!StringExtensions.isNullOrBlank(mRequest.getAppName())) {
            message = String.format(STRING_FORMAT_QUERY_PARAM, message, AuthenticationConstants.AAD.APP_PACKAGE_NAME,
                    StringExtensions.urlFormEncode(mRequest.getAppName()));
        }

        if (!StringExtensions.isNullOrBlank(mRequest.getAppVersion())) {
            message = String.format(STRING_FORMAT_QUERY_PARAM, message, AuthenticationConstants.AAD.APP_VERSION,
                    StringExtensions.urlFormEncode(mRequest.getAppVersion()));
        }
        return message;
    }

    public AuthenticationResult processUIResponseParams(Map<String, String> response) throws AuthenticationException {

        final AuthenticationResult result;

        // Protocol error related
        if (response.containsKey(AuthenticationConstants.OAuth2.ERROR)) {
            // Error response from the server
            // CorrelationID will be same as in request headers. This is
            // retrieved in result in case it was not set.
            String correlationInResponse = response.get(AuthenticationConstants.AAD.CORRELATION_ID);
            if (!StringExtensions.isNullOrBlank(correlationInResponse)) {
                try {
                    final UUID correlationId = UUID.fromString(correlationInResponse);
                    Logger.setCorrelationId(correlationId);
                } catch (IllegalArgumentException ex) {
                    Logger.e(TAG, "CorrelationId is malformed: " + correlationInResponse, "",
                            ADALError.CORRELATION_ID_FORMAT);
                }
            }

            Logger.i(TAG,
                    "OAuth2 error:" + response.get(AuthenticationConstants.OAuth2.ERROR),
                    " Description:" + response.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION));

            result = new AuthenticationResult(response.get(AuthenticationConstants.OAuth2.ERROR),
                    response.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION),
                    response.get(AuthenticationConstants.OAuth2.ERROR_CODES));

            if (null != response.get(AuthenticationConstants.OAuth2.HTTP_RESPONSE_BODY)) {
                HashMap<String, String> responseBody = null;
                try {
                    extractJsonObjects(responseBody, response.get(AuthenticationConstants.OAuth2.HTTP_RESPONSE_BODY));
                    result.setHttpResponseBody(responseBody);
                } catch (final JSONException exception) {
                    Logger.e(TAG, "Json exception", ExceptionExtensions.getExceptionMessage(exception), ADALError.SERVER_INVALID_JSON_RESPONSE);
                }
            }

            if (null != response.get(AuthenticationConstants.OAuth2.HTTP_RESPONSE_HEADER)) {
                HashMap<String, List<String>> responseHeaders = null;
                try {
                    responseHeaders = HashMapExtensions.jsonStringAsMapList(response.get(AuthenticationConstants.OAuth2.HTTP_RESPONSE_HEADER));
                    result.setHttpResponseHeaders(responseHeaders);
                } catch (final JSONException exception) {
                    Logger.e(TAG, "Json exception", ExceptionExtensions.getExceptionMessage(exception), ADALError.SERVER_INVALID_JSON_RESPONSE);
                }
            }

            if (null != response.get(AuthenticationConstants.OAuth2.HTTP_STATUS_CODE)) {
                result.setServiceStatusCode(Integer.parseInt(response.get(AuthenticationConstants.OAuth2.HTTP_STATUS_CODE)));
            }
        } else if (response.containsKey(AuthenticationConstants.OAuth2.CODE)) {
            // The header cloud_instance_host_name points to the right sovereign cloud to use for the given user
            // Using this host name we construct the authority that will get the token request and we use this authority
            // to save the token in the cache. The app should reinitialize AuthenticationContext with this authority for
            // all subsequent requests.
            result = new AuthenticationResult(mRequest.getClientId(), response.get(AuthenticationConstants.OAuth2.CODE));
            final String cloudInstanceHostName = response.get(AuthenticationConstants.OAuth2.CLOUD_INSTANCE_HOST_NAME);
            if (!StringExtensions.isNullOrBlank(cloudInstanceHostName)) {

                final URL authorityUrl = StringExtensions.getUrl(mRequest.getAuthority());
                final String newAuthorityUrlString = new Uri.Builder().scheme(HTTPS_PROTOCOL_STRING)
                        .authority(cloudInstanceHostName)
                        .path(authorityUrl.getPath())
                        .build().toString();

                setTokenEndpoint(newAuthorityUrlString + DEFAULT_TOKEN_ENDPOINT);
                result.setAuthority(newAuthorityUrlString);
            }
        } else if (response.containsKey(AuthenticationConstants.OAuth2.ACCESS_TOKEN)) {
            // Token response
            boolean isMultiResourceToken = false;
            String expiresIn = response.get(AuthenticationConstants.OAuth2.EXPIRES_IN);
            Long expiresInLong;
            Calendar expires = new GregorianCalendar();

            expiresInLong = (expiresIn == null || expiresIn.isEmpty() ? ((long) AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC) : Long.parseLong(expiresIn));

            // Compute token expiration
            expires.add(
                    Calendar.SECOND,
                    expiresIn == null || expiresIn.isEmpty() ? AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC
                            : Integer.parseInt(expiresIn));

            final String refreshToken = response.get(AuthenticationConstants.OAuth2.REFRESH_TOKEN);

            String resource = null;
            if (response.containsKey(AuthenticationConstants.AAD.RESOURCE)
                    && !StringExtensions.isNullOrBlank(refreshToken)) {
                isMultiResourceToken = true;
                resource = response.get(AuthenticationConstants.AAD.RESOURCE);
            }

            UserInfo userinfo = null;
            String tenantId = null;
            String rawIdToken = null;
            if (response.containsKey(AuthenticationConstants.OAuth2.ID_TOKEN)) {
                // IDtoken is related to Azure AD and returned with token
                // response. ADFS does not return that.
                rawIdToken = response.get(AuthenticationConstants.OAuth2.ID_TOKEN);
                if (!StringExtensions.isNullOrBlank(rawIdToken)) {
                    Logger.v(TAG, "Id token was returned, parsing id token.");
                    final IdToken tokenParsed = new IdToken(rawIdToken);
                    if (tokenParsed != null) {
                        tenantId = tokenParsed.getTenantId();
                        userinfo = new UserInfo(tokenParsed);
                    }
                } else {
                    Logger.v(TAG, "IdToken was not returned from token request.");
                }
            }

            String familyClientId = null;
            if (response.containsKey(AuthenticationConstants.OAuth2.ADAL_CLIENT_FAMILY_ID)) {
                familyClientId = response.get(AuthenticationConstants.OAuth2.ADAL_CLIENT_FAMILY_ID);
            }

            ClientInfo clientInfo = null;
            if (response.containsKey(AuthenticationConstants.OAuth2.CLIENT_INFO)) {
                final String rawClientInfo = response.get(AuthenticationConstants.OAuth2.CLIENT_INFO);
                try {
                    clientInfo = new ClientInfo(rawClientInfo);
                } catch (ServiceException e) {
                    Logger.w(TAG, "ClientInfo decoding/parsing failed.");
                }
            }

            result = new AuthenticationResult(
                    response.get(AuthenticationConstants.OAuth2.ACCESS_TOKEN),
                    refreshToken,
                    expires.getTime(),
                    isMultiResourceToken,
                    userinfo,
                    tenantId,
                    rawIdToken,
                    null,
                    mRequest.getClientId()
            );

            result.setResource(resource);
            result.setClientInfo(clientInfo);
            result.setExpiresIn(expiresInLong);
            result.setResponseReceived(System.currentTimeMillis());

            if (response.containsKey(AuthenticationConstants.OAuth2.EXT_EXPIRES_IN)) {
                final String extendedExpiresIn = response.get(AuthenticationConstants.OAuth2.EXT_EXPIRES_IN);
                final Calendar extendedExpires = new GregorianCalendar();
                // Compute extended token expiration
                extendedExpires.add(
                        Calendar.SECOND,
                        StringExtensions.isNullOrBlank(extendedExpiresIn) ? AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC
                                : Integer.parseInt(extendedExpiresIn));
                result.setExtendedExpiresOn(extendedExpires.getTime());
            }

            //Set family client id on authentication result for TokenCacheItem to pick up
            result.setFamilyClientId(familyClientId);
        } else {
            result = null;
        }

        return result;
    }

    public static void extractJsonObjects(Map<String, String> responseItems, String jsonStr)
            throws JSONException {
        final JSONObject jsonObject = new JSONObject(jsonStr);

        final Iterator<?> i = jsonObject.keys();

        while (i.hasNext()) {
            final String key = (String) i.next();
            responseItems.put(key, jsonObject.getString(key));
        }
    }

    public AuthenticationResult refreshToken(String refreshToken) throws IOException,
            AuthenticationException {
        final String requestMessage;
        if (mWebRequestHandler == null) {
            Logger.v(TAG, "Web request is not set correctly.");
            throw new IllegalArgumentException("webRequestHandler is null.");
        }

        // Token request message
        try {
            requestMessage = buildRefreshTokenRequestMessage(refreshToken);
        } catch (UnsupportedEncodingException encoding) {
            Logger.e(TAG,
                    ADALError.ENCODING_IS_NOT_SUPPORTED.getDescription(),
                    encoding.getMessage(),
                    ADALError.ENCODING_IS_NOT_SUPPORTED,
                    encoding);
            return null;
        }

        final Map<String, String> headers = getRequestHeaders();

        // Refresh token endpoint needs to send header field for device
        // challenge
        headers.put(AuthenticationConstants.Broker.CHALLENGE_TLS_INCAPABLE,
                AuthenticationConstants.Broker.CHALLENGE_TLS_INCAPABLE_VERSION);
        Logger.v(TAG, "Sending request to redeem token with refresh token.");
        return postMessage(requestMessage, headers);
    }

    /**
     * parse final url for code(normal flow) or token(implicit flow) and then it
     * proceeds to next step.
     *
     * @param authorizationUrl browser reached to this final url and it has code
     *                         or token for next step
     * @return Token in the AuthenticationResult. Null result if response does
     * not have protocol error.
     * @throws IOException
     * @throws AuthenticationException
     */
    public AuthenticationResult getToken(String authorizationUrl)
            throws IOException, AuthenticationException {

        if (StringExtensions.isNullOrBlank(authorizationUrl)) {
            throw new IllegalArgumentException("authorizationUrl");
        }

        // Success
        HashMap<String, String> parameters = StringExtensions.getUrlParameters(authorizationUrl);
        String encodedState = parameters.get("state");
        String state = decodeProtocolState(encodedState);

        if (!StringExtensions.isNullOrBlank(state)) {

            // We have encoded state at the end of the url
            Uri stateUri = Uri.parse("http://state/path?" + state);
            String authorizationUri = stateUri.getQueryParameter("a");
            String resource = stateUri.getQueryParameter("r");

            if (!StringExtensions.isNullOrBlank(authorizationUri)
                    && !StringExtensions.isNullOrBlank(resource)
                    && resource.equalsIgnoreCase(mRequest.getResource())) {

                AuthenticationResult result = processUIResponseParams(parameters);

                // Check if we have code
                if (result != null && result.getCode() != null && !result.getCode().isEmpty()) {

                    // Get token and use external callback to set result
                    final AuthenticationResult tokenResult = getTokenForCode(result.getCode());
                    if (!StringExtensions.isNullOrBlank(result.getAuthority())) {
                        tokenResult.setAuthority(result.getAuthority());
                    } else {
                        tokenResult.setAuthority(mRequest.getAuthority());
                    }
                    return tokenResult;
                }

                return result;
            } else {
                throw new AuthenticationException(ADALError.AUTH_FAILED_BAD_STATE);
            }
        } else {
            // The response from the server had no state
            throw new AuthenticationException(ADALError.AUTH_FAILED_NO_STATE);
        }
    }

    /**
     * get code and exchange for token.
     *
     * @param code the authorization code for which Authentication result is needed
     * @return AuthenticationResult
     * @throws IOException
     * @throws AuthenticationException
     */
    public AuthenticationResult getTokenForCode(String code) throws IOException, AuthenticationException {
        final String methodName = ":getTokenForCode";
        final String requestMessage;
        if (mWebRequestHandler == null) {
            throw new IllegalArgumentException("webRequestHandler");
        }

        // Token request message
        try {
            requestMessage = buildTokenRequestMessage(code);
        } catch (UnsupportedEncodingException encoding) {
            Logger.e(TAG + methodName,
                    ADALError.ENCODING_IS_NOT_SUPPORTED.getDescription(),
                    encoding.getMessage(),
                    ADALError.ENCODING_IS_NOT_SUPPORTED,
                    encoding);
            return null;
        }

        final Map<String, String> headers = getRequestHeaders();

        Logger.v(TAG + methodName, "Sending request to redeem token with auth code.");
        return postMessage(requestMessage, headers);
    }

    private AuthenticationResult postMessage(String requestMessage, Map<String, String> headers)
            throws IOException, AuthenticationException {
        final String methodName = ":postMessage";
        AuthenticationResult result = null;
        final HttpEvent httpEvent = startHttpEvent();

        final URL authority = StringExtensions.getUrl(getTokenEndpoint());
        if (authority == null) {
            stopHttpEvent(httpEvent);
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL);
        }

        httpEvent.setHttpPath(authority);

        try {
            mWebRequestHandler.setRequestCorrelationId(mRequest.getCorrelationId());
            ClientMetrics.INSTANCE.beginClientMetricsRecord(authority, mRequest.getCorrelationId(),
                    headers);
            HttpWebResponse response = mWebRequestHandler.sendPost(authority, headers,
                    requestMessage.getBytes(AuthenticationConstants.ENCODING_UTF8),
                    "application/x-www-form-urlencoded");

            httpEvent.setResponseCode(response.getStatusCode());
            httpEvent.setCorrelationId(mRequest.getCorrelationId().toString());
            stopHttpEvent(httpEvent);

            if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                if (response.getResponseHeaders() != null
                        && response.getResponseHeaders().containsKey(
                        AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER)) {

                    // Device certificate challenge will send challenge request
                    // in 401 header.
                    String challengeHeader = response.getResponseHeaders()
                            .get(AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER).get(0);
                    Logger.i(TAG + methodName, "Device certificate challenge request. ",
                            "Challenge header: " + challengeHeader);
                    if (!StringExtensions.isNullOrBlank(challengeHeader)) {

                        // Handle each specific challenge header
                        if (StringExtensions.hasPrefixInHeader(challengeHeader,
                                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE)) {
                            final HttpEvent challengeHttpEvent = startHttpEvent();
                            challengeHttpEvent.setHttpPath(authority);
                            Logger.v(TAG + methodName, "Received pkeyAuth device challenge.");
                            ChallengeResponseBuilder certHandler = new ChallengeResponseBuilder(
                                    mJWSBuilder);
                            Logger.v(TAG + methodName, "Processing device challenge.");
                            final ChallengeResponse challengeResponse = certHandler
                                    .getChallengeResponseFromHeader(challengeHeader,
                                            authority.toString());
                            headers.put(AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER,
                                    challengeResponse.getAuthorizationHeaderValue());
                            Logger.v(TAG + methodName, "Sending request with challenge response.");
                            response = mWebRequestHandler.sendPost(authority, headers,
                                    requestMessage.getBytes(AuthenticationConstants.ENCODING_UTF8),
                                    "application/x-www-form-urlencoded");
                            challengeHttpEvent.setResponseCode(response.getStatusCode());
                            challengeHttpEvent.setCorrelationId(mRequest.getCorrelationId().toString());
                            stopHttpEvent(challengeHttpEvent);
                        }
                    } else {
                        throw new AuthenticationException(
                                ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                                "Challenge header is empty", response);
                    }
                } else {
                    // AAD server returns 401 response for wrong request
                    // messages
                    Logger.v(TAG + methodName, "401 http status code is returned without authorization header.");
                }
            }

            boolean isBodyEmpty = TextUtils.isEmpty(response.getBody());
            if (!isBodyEmpty) {
                // Protocol related errors will read the error stream and report
                // the error and error description
                Logger.v(TAG + methodName, "Token request does not have exception.");
                try {
                    result = processTokenResponse(response, httpEvent);
                } catch (final ServerRespondingWithRetryableException e) {
                    result = retry(requestMessage, headers);
                    if (result != null) {
                        return result;
                    }

                    if (mRequest.getIsExtendedLifetimeEnabled()) {
                        Logger.v(TAG + methodName, "WebResponse is not a success due to: " + response.getStatusCode());
                        throw e;
                    } else {
                        Logger.v(TAG + methodName, "WebResponse is not a success due to: " + response.getStatusCode());
                        throw new AuthenticationException(ADALError.SERVER_ERROR, "WebResponse is not a success due to: " + response.getStatusCode(), response);
                    }
                }
                ClientMetrics.INSTANCE.setLastError(null);
            }
            if (result == null) {
                // non-protocol related error
                String errMessage = isBodyEmpty ? "Status code:" + response.getStatusCode() : response.getBody();
                Logger.e(TAG + methodName, ADALError.SERVER_ERROR.getDescription(), errMessage, ADALError.SERVER_ERROR);
                throw new AuthenticationException(ADALError.SERVER_ERROR, errMessage, response);
            } else {
                ClientMetrics.INSTANCE.setLastErrorCodes(result.getErrorCodes());
            }
        } catch (final UnsupportedEncodingException e) {
            ClientMetrics.INSTANCE.setLastError(null);
            Logger.e(TAG + methodName,
                    ADALError.ENCODING_IS_NOT_SUPPORTED.getDescription(),
                    e.getMessage(),
                    ADALError.ENCODING_IS_NOT_SUPPORTED, e);
            throw e;
        } catch (final SocketTimeoutException e) {
            result = retry(requestMessage, headers);
            if (result != null) {
                return result;
            }

            ClientMetrics.INSTANCE.setLastError(null);
            if (mRequest.getIsExtendedLifetimeEnabled()) {
                Logger.e(TAG + methodName, ADALError.SERVER_ERROR.getDescription(), e.getMessage(), ADALError.SERVER_ERROR, e);
                throw new ServerRespondingWithRetryableException(e.getMessage(), e);
            } else {
                Logger.e(TAG + methodName, ADALError.SERVER_ERROR.getDescription(), e.getMessage(), ADALError.SERVER_ERROR, e);
                throw e;
            }
        } catch (final IOException e) {
            ClientMetrics.INSTANCE.setLastError(null);
            Logger.e(TAG + methodName, ADALError.SERVER_ERROR.getDescription(), e.getMessage(), ADALError.SERVER_ERROR, e);
            throw e;
        } finally {
            ClientMetrics.INSTANCE.endClientMetricsRecord(ClientMetricsEndpointType.TOKEN,
                    mRequest.getCorrelationId());
        }
        return result;
    }

    private AuthenticationResult retry(String requestMessage, Map<String, String> headers) throws IOException, AuthenticationException {
        final String methodName = ":retry";
        //retry once if there is an observation of a network timeout by the client
        if (mRetryOnce) {
            mRetryOnce = false;
            try {
                Thread.sleep(DELAY_TIME_PERIOD);
            } catch (final InterruptedException exception) {
                Logger.v(TAG + methodName, "The thread is interrupted while it is sleeping. ");
            }

            Logger.v(TAG + methodName, "Try again...");
            return postMessage(requestMessage, headers);
        }

        return null;
    }

    public static String decodeProtocolState(String encodedState) throws UnsupportedEncodingException {

        if (!StringExtensions.isNullOrBlank(encodedState)) {
            byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING | Base64.URL_SAFE);

            return new String(stateBytes, "UTF-8");
        }

        return null;
    }

    public String encodeProtocolState() throws UnsupportedEncodingException {
        String state = String.format("a=%s&r=%s", mRequest.getAuthority(), mRequest.getResource());
        return Base64.encodeToString(state.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private Map<String, String> getRequestHeaders() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        return headers;
    }

    /**
     * Extract AuthenticationResult object from response body if available.
     *
     * @param webResponse the web response from which authentication result will be constructed
     * @return AuthenticationResult
     */
    private AuthenticationResult processTokenResponse(HttpWebResponse webResponse, final HttpEvent httpEvent)
            throws AuthenticationException {
        final String methodName = ":processTokenResponse";
        AuthenticationResult result;
        String correlationIdInHeader = null;
        String speRing = null;
        if (webResponse.getResponseHeaders() != null) {
            if (webResponse.getResponseHeaders().containsKey(
                    AuthenticationConstants.AAD.CLIENT_REQUEST_ID)) {
                // headers are returning as a list
                List<String> listOfHeaders = webResponse.getResponseHeaders().get(
                        AuthenticationConstants.AAD.CLIENT_REQUEST_ID);
                if (listOfHeaders != null && listOfHeaders.size() > 0) {
                    correlationIdInHeader = listOfHeaders.get(0);
                }
            }

            if (webResponse.getResponseHeaders().containsKey(AuthenticationConstants.AAD.REQUEST_ID_HEADER)) {
                // headers are returning as a list
                List<String> listOfHeaders = webResponse.getResponseHeaders().get(
                        AuthenticationConstants.AAD.REQUEST_ID_HEADER);
                if (listOfHeaders != null && listOfHeaders.size() > 0) {
                    Logger.v(TAG + methodName, "Set request id header. " + "x-ms-request-id: " + listOfHeaders.get(0));
                    httpEvent.setRequestIdHeader(listOfHeaders.get(0));
                }
            }

            if (null != webResponse.getResponseHeaders().get(X_MS_CLITELEM) && !webResponse.getResponseHeaders().get(X_MS_CLITELEM).isEmpty()) {
                final CliTelemInfo cliTelemInfo =
                        TelemetryUtils.parseXMsCliTelemHeader(
                                webResponse.getResponseHeaders()
                                        .get(X_MS_CLITELEM).get(0)
                        );

                if (null != cliTelemInfo) {
                    httpEvent.setXMsCliTelemData(cliTelemInfo);
                    speRing = cliTelemInfo.getSpeRing();
                }
            }
        }

        final int statusCode = webResponse.getStatusCode();

        if (statusCode == HttpURLConnection.HTTP_OK
                || statusCode == HttpURLConnection.HTTP_BAD_REQUEST
                || statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            try {
                result = parseJsonResponse(webResponse.getBody());
                if (result != null) {
                    if (null != result.getErrorCode()) {
                        result.setHttpResponse(webResponse);
                    }

                    final CliTelemInfo cliTelemInfo = new CliTelemInfo();
                    cliTelemInfo._setSpeRing(speRing);
                    result.setCliTelemInfo(cliTelemInfo);
                    httpEvent.setOauthErrorCode(result.getErrorCode());
                }
            } catch (final JSONException jsonException) {
                throw new AuthenticationException(ADALError.SERVER_INVALID_JSON_RESPONSE,
                        "Can't parse server response. " + webResponse.getBody(),
                        webResponse, jsonException);
            }
        } else if (statusCode >= HttpURLConnection.HTTP_INTERNAL_ERROR && statusCode <= MAX_RESILIENCY_ERROR_CODE) {
            throw new ServerRespondingWithRetryableException("Server Error " + statusCode + " "
                    + webResponse.getBody(), webResponse);
        } else {
            throw new AuthenticationException(ADALError.SERVER_ERROR,
                    "Unexpected server response " + statusCode + " " + webResponse.getBody(),
                    webResponse);
        }

        // Set correlationId in the result
        if (correlationIdInHeader != null && !correlationIdInHeader.isEmpty()) {
            try {
                UUID correlation = UUID.fromString(correlationIdInHeader);
                if (!correlation.equals(mRequest.getCorrelationId())) {
                    Logger.w(TAG + methodName, "CorrelationId is not matching", "",
                            ADALError.CORRELATION_ID_NOT_MATCHING_REQUEST_RESPONSE);
                }

                Logger.v(TAG + methodName, "Response correlationId:" + correlationIdInHeader);
            } catch (IllegalArgumentException ex) {
                Logger.e(TAG + methodName, "Wrong format of the correlation ID:" + correlationIdInHeader, "",
                        ADALError.CORRELATION_ID_FORMAT, ex);
            }
        }

        if (null != webResponse.getResponseHeaders()) {
            final List<String> xMsCliTelemValues = webResponse.getResponseHeaders().get(X_MS_CLITELEM);
            if (null != xMsCliTelemValues && !xMsCliTelemValues.isEmpty()) {
                // Only one value is expected to be present, so we'll grab the first element...
                final String speValue = xMsCliTelemValues.get(0);
                final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speValue);
                if (result != null) {
                    result.setCliTelemInfo(cliTelemInfo);
                }
            }
        }

        return result;
    }

    private AuthenticationResult parseJsonResponse(final String responseBody)
            throws JSONException,
            AuthenticationException {
        final Map<String, String> responseItems = new HashMap<>();
        extractJsonObjects(responseItems, responseBody);
        return processUIResponseParams(responseItems);
    }

    private HttpEvent startHttpEvent() {
        final HttpEvent httpEvent = new HttpEvent(EventStrings.HTTP_EVENT);
        httpEvent.setRequestId(mRequest.getTelemetryRequestId());
        httpEvent.setMethod(EventStrings.HTTP_METHOD_POST);
        Telemetry.getInstance().startEvent(mRequest.getTelemetryRequestId(), EventStrings.HTTP_EVENT);
        return httpEvent;
    }

    private void stopHttpEvent(final HttpEvent httpEvent) {
        Telemetry.getInstance().stopEvent(mRequest.getTelemetryRequestId(), httpEvent,
                EventStrings.HTTP_EVENT);
    }

    public void setTokenEndpoint(final String tokenEndpoint) {
        mTokenEndpoint = tokenEndpoint;
    }
}