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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.microsoft.aad.adal.ChallengeResponseBuilder.ChallengeResponse;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

/**
 * Base Oauth class.
 */
class Oauth2 {

    private AuthenticationRequest mRequest;

    private IWebRequestHandler mWebRequestHandler;

    private IJWSBuilder mJWSBuilder = new JWSBuilder();

    private final static String TAG = "Oauth";

    private final static String DEFAULT_AUTHORIZE_ENDPOINT = "/oauth2/authorize";

    private final static String DEFAULT_TOKEN_ENDPOINT = "/oauth2/token";

    private final static String JSON_PARSING_ERROR = "It failed to parse response as json";

    Oauth2(AuthenticationRequest request) {
        mRequest = request;
        mWebRequestHandler = null;
        mJWSBuilder = null;
    }

    public Oauth2(AuthenticationRequest request, IWebRequestHandler webRequestHandler) {
        mRequest = request;
        mWebRequestHandler = webRequestHandler;
        mJWSBuilder = null;
    }

    public Oauth2(AuthenticationRequest request, IWebRequestHandler webRequestHandler,
            IJWSBuilder jwsMessageBuilder) {
        mRequest = request;
        mWebRequestHandler = webRequestHandler;
        mJWSBuilder = jwsMessageBuilder;
    }

    public String getAuthorizationEndpoint() {
        return mRequest.getAuthority() + DEFAULT_AUTHORIZE_ENDPOINT;
    }

    public String getTokenEndpoint() {
        return mRequest.getAuthority() + DEFAULT_TOKEN_ENDPOINT;
    }

    public String getAuthorizationEndpointQueryParameters() throws UnsupportedEncodingException {
        String requestUrl = String
                .format("response_type=%s&client_id=%s&resource=%s&redirect_uri=%s&state=%s",
                        AuthenticationConstants.OAuth2.CODE, URLEncoder.encode(
                                mRequest.getClientId(), AuthenticationConstants.ENCODING_UTF8),
                        URLEncoder.encode(mRequest.getResource(),
                                AuthenticationConstants.ENCODING_UTF8), URLEncoder.encode(
                                mRequest.getRedirectUri(), AuthenticationConstants.ENCODING_UTF8),
                        encodeProtocolState());

        if (mRequest.getLoginHint() != null && !mRequest.getLoginHint().isEmpty()) {
            requestUrl = String.format("%s&%s=%s", requestUrl,
                    AuthenticationConstants.AAD.LOGIN_HINT, URLEncoder.encode(
                            mRequest.getLoginHint(), AuthenticationConstants.ENCODING_UTF8));
        }

        requestUrl = String.format("%s&%s=%s", requestUrl,
                AuthenticationConstants.AAD.ADAL_ID_PLATFORM, "Android");
        requestUrl = String.format("%s&%s=%s", requestUrl,
                AuthenticationConstants.AAD.ADAL_ID_VERSION, URLEncoder.encode(
                        AuthenticationContext.getVersionName(),
                        AuthenticationConstants.ENCODING_UTF8));
        requestUrl = String.format("%s&%s=%s", requestUrl,
                AuthenticationConstants.AAD.ADAL_ID_OS_VER, URLEncoder.encode(""
                        + Build.VERSION.SDK_INT, AuthenticationConstants.ENCODING_UTF8));
        requestUrl = String.format("%s&%s=%s", requestUrl, AuthenticationConstants.AAD.ADAL_ID_DM,
                URLEncoder.encode("" + android.os.Build.MODEL,
                        AuthenticationConstants.ENCODING_UTF8));

        if (mRequest.getCorrelationId() != null) {
            requestUrl = String.format("%s&%s=%s", requestUrl,
                    AuthenticationConstants.AAD.CLIENT_REQUEST_ID, URLEncoder.encode(mRequest
                            .getCorrelationId().toString(), AuthenticationConstants.ENCODING_UTF8));
        }

        // Setting prompt behavior to always will skip the cookies for webview.
        // It is added to authorization url.
        if (mRequest.getPrompt() == PromptBehavior.Always) {
            requestUrl = String.format("%s&%s=%s", requestUrl,
                    AuthenticationConstants.AAD.QUERY_PROMPT, URLEncoder.encode(
                            AuthenticationConstants.AAD.QUERY_PROMPT_VALUE,
                            AuthenticationConstants.ENCODING_UTF8));
        } else if (mRequest.getPrompt() == PromptBehavior.REFRESH_SESSION) {
            requestUrl = String.format("%s&%s=%s", requestUrl,
                    AuthenticationConstants.AAD.QUERY_PROMPT, URLEncoder.encode(
                            AuthenticationConstants.AAD.QUERY_PROMPT_REFRESH_SESSION_VALUE,
                            AuthenticationConstants.ENCODING_UTF8));
        }

        if (!StringExtensions.IsNullOrBlank(mRequest.getExtraQueryParamsAuthentication())) {
            String params = mRequest.getExtraQueryParamsAuthentication();
            if (!params.startsWith("&")) {
                params = "&" + params;
            }
            requestUrl = requestUrl + params;
        }
        return requestUrl;

    }

    public String getCodeRequestUrl() throws UnsupportedEncodingException {
        String requestUrl = String.format("%s?%s", getAuthorizationEndpoint(),
                getAuthorizationEndpointQueryParameters());
        return requestUrl;
    }

    public String buildTokenRequestMessage(String code) throws UnsupportedEncodingException {
        String message = String.format("%s=%s&%s=%s&%s=%s&%s=%s",
                AuthenticationConstants.OAuth2.GRANT_TYPE,
                StringExtensions.URLFormEncode(AuthenticationConstants.OAuth2.AUTHORIZATION_CODE),

                AuthenticationConstants.OAuth2.CODE, StringExtensions.URLFormEncode(code),

                AuthenticationConstants.OAuth2.CLIENT_ID,
                StringExtensions.URLFormEncode(mRequest.getClientId()),

                AuthenticationConstants.OAuth2.REDIRECT_URI,
                StringExtensions.URLFormEncode(mRequest.getRedirectUri()));
        return message;
    }

    public String buildRefreshTokenRequestMessage(String refreshToken)
            throws UnsupportedEncodingException {
        String message = String.format("%s=%s&%s=%s&%s=%s",
                AuthenticationConstants.OAuth2.GRANT_TYPE,
                StringExtensions.URLFormEncode(AuthenticationConstants.OAuth2.REFRESH_TOKEN),

                AuthenticationConstants.OAuth2.REFRESH_TOKEN,
                StringExtensions.URLFormEncode(refreshToken),

                AuthenticationConstants.OAuth2.CLIENT_ID,
                StringExtensions.URLFormEncode(mRequest.getClientId()));

        if (!StringExtensions.IsNullOrBlank(mRequest.getResource())) {
            message = String.format("%s&%s=%s", message, AuthenticationConstants.AAD.RESOURCE,
                    StringExtensions.URLFormEncode(mRequest.getResource()));
        }

        return message;
    }

    public static AuthenticationResult processUIResponseParams(HashMap<String, String> response) {

        final AuthenticationResult result;

        // Protocol error related
        if (response.containsKey(AuthenticationConstants.OAuth2.ERROR)) {
            // Error response from the server
            // CorrelationID will be same as in request headers. This is
            // retrieved in result in case it was not set.
            UUID correlationId = null;
            String correlationInResponse = response.get(AuthenticationConstants.AAD.CORRELATION_ID);
            if (!StringExtensions.IsNullOrBlank(correlationInResponse)) {
                try {
                    correlationId = UUID.fromString(correlationInResponse);
                    Logger.setCorrelationId(correlationId);
                } catch (IllegalArgumentException ex) {
                    correlationId = null;
                    Logger.e(TAG, "CorrelationId is malformed: " + correlationInResponse, "",
                            ADALError.CORRELATION_ID_FORMAT);
                }
            }

            Logger.v(
                    TAG,
                    "OAuth2 error:" + response.get(AuthenticationConstants.OAuth2.ERROR)
                            + " Description:"
                            + response.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION));

            result = new AuthenticationResult(response.get(AuthenticationConstants.OAuth2.ERROR),
                    response.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION),
                    response.get(AuthenticationConstants.OAuth2.ERROR_CODES));

        } else if (response.containsKey(AuthenticationConstants.OAuth2.CODE)) {
            result = new AuthenticationResult(response.get(AuthenticationConstants.OAuth2.CODE));
        } 
        else if (response.containsKey(AuthenticationConstants.OAuth2.ACCESS_TOKEN)) {
            // Token response
            boolean isMultiResourcetoken = false;
            String expires_in = response.get("expires_in");
            Calendar expires = new GregorianCalendar();

            // Compute token expiration
            expires.add(
                    Calendar.SECOND,
                    expires_in == null || expires_in.isEmpty() ? AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC
                            : Integer.parseInt(expires_in));

            if (response.containsKey(AuthenticationConstants.AAD.RESOURCE)) {
                isMultiResourcetoken = true;
            }

            UserInfo userinfo = null;
            String tenantId = null;
            String rawIdToken = null;
            if (response.containsKey(AuthenticationConstants.OAuth2.ID_TOKEN)) {
                // IDtoken is related to Azure AD and returned with token
                // response. ADFS does not return that.
                rawIdToken = response.get(AuthenticationConstants.OAuth2.ID_TOKEN);
                if (!StringExtensions.IsNullOrBlank(rawIdToken)) {
                    IdToken tokenParsed = parseIdToken(rawIdToken);
                    if (tokenParsed != null) {
                        tenantId = tokenParsed.mTenantId;
                        userinfo = new UserInfo(tokenParsed);
                    }
                } else {
                    Logger.v(TAG, "IdToken is not provided");
                }
            }
            
            String familyClientId = null;
            if (response.containsKey(AuthenticationConstants.OAuth2.ADAL_CLIENT_FAMILY_ID)) {
                familyClientId = response.get(AuthenticationConstants.OAuth2.ADAL_CLIENT_FAMILY_ID);
            }

            result = new AuthenticationResult(
                    response.get(AuthenticationConstants.OAuth2.ACCESS_TOKEN),
                    response.get(AuthenticationConstants.OAuth2.REFRESH_TOKEN), expires.getTime(),
                    isMultiResourcetoken, userinfo, tenantId, rawIdToken);
            
            //Set family client id on authentication result for TokenCacheItem to pick up
            result.setFamilyClientId(familyClientId);
        } else {
            result = null;
        }

        return result;
    }

    /**
     * parse user id token string.
     * 
     * @param idtoken
     * @return UserInfo
     */
    private static IdToken parseIdToken(String idtoken) {
        try {
            // Message segments: Header.Body.Signature
            int firstDot = idtoken.indexOf(".");
            int secondDot = idtoken.indexOf(".", firstDot + 1);
            int invalidDot = idtoken.indexOf(".", secondDot + 1);

            if (invalidDot == -1 && firstDot > 0 && secondDot > 0) {
                String idbody = idtoken.substring(firstDot + 1, secondDot);
                // URL_SAFE: Encoder/decoder flag bit to use
                // "URL and filename safe" variant of Base64
                // (see RFC 3548 section 4) where - and _ are used in place of +
                // and /.
                byte[] data = Base64.decode(idbody, Base64.URL_SAFE);
                String decodedBody = new String(data, "UTF-8");

                HashMap<String, String> responseItems = new HashMap<String, String>();
                extractJsonObjects(responseItems, decodedBody);
                if (responseItems != null && !responseItems.isEmpty()) {
                    IdToken idtokenInfo = new IdToken();
                    idtokenInfo.mSubject = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_SUBJECT);
                    idtokenInfo.mTenantId = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_TENANTID);
                    idtokenInfo.mUpn = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_UPN);
                    idtokenInfo.mEmail = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_EMAIL);
                    idtokenInfo.mGivenName = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_GIVEN_NAME);
                    idtokenInfo.mFamilyName = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_FAMILY_NAME);
                    idtokenInfo.mIdentityProvider = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_IDENTITY_PROVIDER);
                    idtokenInfo.mObjectId = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_OBJECT_ID);
                    String expiration = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_PASSWORD_EXPIRATION);
                    if (!StringExtensions.IsNullOrBlank(expiration)) {
                        idtokenInfo.mPasswordExpiration = Long.parseLong(expiration);
                    }
                    idtokenInfo.mPasswordChangeUrl = responseItems
                            .get(AuthenticationConstants.OAuth2.ID_TOKEN_PASSWORD_CHANGE_URL);
                    Logger.v(TAG, "IdToken is extracted from token response");
                    return idtokenInfo;
                }
            }
        } catch (JSONException | UnsupportedEncodingException ex) {
            Logger.e(TAG, "Error in parsing user id token", null,
                    ADALError.IDTOKEN_PARSING_FAILURE, ex);
        }
        return null;
    }

    private static void extractJsonObjects(HashMap<String, String> responseItems, String jsonStr)
            throws JSONException {
        final JSONObject jsonObject = new JSONObject(jsonStr);

        final Iterator<?> i = jsonObject.keys();

        while (i.hasNext()) {
            final String key = (String) i.next();
            responseItems.put(key, jsonObject.getString(key));
        }
    }

    public AuthenticationResult refreshToken(String refreshToken) throws IOException, AuthenticationException {
        String requestMessage = null;
        if (mWebRequestHandler == null) {
            Logger.v(TAG, "Web request is not set correctly");
            throw new IllegalArgumentException("webRequestHandler is null.");
        }

        // Token request message
        try {
            requestMessage = buildRefreshTokenRequestMessage(refreshToken);
        } catch (UnsupportedEncodingException encoding) {
            Logger.e(TAG, encoding.getMessage(), "", ADALError.ENCODING_IS_NOT_SUPPORTED, encoding);
            return null;
        }

        HashMap<String, String> headers = getRequestHeaders();

        // Refresh token endpoint needs to send header field for device
        // challenge
        headers.put(AuthenticationConstants.Broker.CHALLENGE_TLS_INCAPABLE,
                AuthenticationConstants.Broker.CHALLENGE_TLS_INCAPABLE_VERSION);
        return postMessage(requestMessage, headers);
    }

    /**
     * parse final url for code(normal flow) or token(implicit flow) and then it
     * proceeds to next step.
     * 
     * @param authorizationUrl browser reached to this final url and it has code
     *            or token for next step
     * @return Token in the AuthenticationResult. Null result if response does
     *         not have protocol error.
     * @throws Exception
     */
    public AuthenticationResult getToken(String authorizationUrl)
            throws IOException, AuthenticationServerProtocolException, AuthenticationException {

        if (StringExtensions.IsNullOrBlank(authorizationUrl)) {
            throw new IllegalArgumentException("authorizationUrl");
        }

        // Success
        HashMap<String, String> parameters = StringExtensions.getUrlParameters(authorizationUrl);
        String encodedState = parameters.get("state");
        String state = decodeProtocolState(encodedState);

        if (!StringExtensions.IsNullOrBlank(state)) {

            // We have encoded state at the end of the url
            Uri stateUri = Uri.parse("http://state/path?" + state);
            String authorizationUri = stateUri.getQueryParameter("a");
            String resource = stateUri.getQueryParameter("r");

            if (!StringExtensions.IsNullOrBlank(authorizationUri)
                    && !StringExtensions.IsNullOrBlank(resource)
                    && resource.equalsIgnoreCase(mRequest.getResource())) {

                AuthenticationResult result = processUIResponseParams(parameters);

                // Check if we have code
                if (result != null && result.getCode() != null && !result.getCode().isEmpty()) {

                    // Get token and use external callback to set result
                    return getTokenForCode(result.getCode());
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
     * @param code
     * @return Token in the AuthenticationResult
     * @throws Exception
     */
    public AuthenticationResult getTokenForCode(String code) throws IOException, AuthenticationException {

        String requestMessage = null;
        if (mWebRequestHandler == null) {
            throw new IllegalArgumentException("webRequestHandler");
        }

        // Token request message
        try {
            requestMessage = buildTokenRequestMessage(code);
        } catch (UnsupportedEncodingException encoding) {
            Logger.e(TAG, encoding.getMessage(), "", ADALError.ENCODING_IS_NOT_SUPPORTED, encoding);
            return null;
        }

        HashMap<String, String> headers = getRequestHeaders();
        return postMessage(requestMessage, headers);
    }

    private AuthenticationResult postMessage(String requestMessage, HashMap<String, String> headers)
            throws IOException, AuthenticationException {
        AuthenticationResult result = null;
        final URL authority = StringExtensions.getUrl(getTokenEndpoint());
        if (authority == null) {
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL);
        }

        try {
            mWebRequestHandler.setRequestCorrelationId(mRequest.getCorrelationId());
            ClientMetrics.INSTANCE.beginClientMetricsRecord(authority, mRequest.getCorrelationId(),
                    headers);
            HttpWebResponse response = mWebRequestHandler.sendPost(authority, headers,
                    requestMessage.getBytes(AuthenticationConstants.ENCODING_UTF8),
                    "application/x-www-form-urlencoded");

            if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                if (response.getResponseHeaders() != null
                        && response.getResponseHeaders().containsKey(
                                AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER)) {

                    // Device certificate challenge will send challenge request
                    // in 401 header.
                    String challengeHeader = response.getResponseHeaders()
                            .get(AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER).get(0);
                    Logger.v(TAG, "Device certificate challenge request:" + challengeHeader);
                    if (!StringExtensions.IsNullOrBlank(challengeHeader)) {

                        // Handle each specific challenge header
                        if (StringExtensions.hasPrefixInHeader(challengeHeader,
                                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE)) {
                            Logger.v(TAG, "Challenge is related to device certificate");
                            ChallengeResponseBuilder certHandler = new ChallengeResponseBuilder(
                                    mJWSBuilder);
                            Logger.v(TAG, "Processing device challenge");
                            final ChallengeResponse challengeResponse = certHandler
                                    .getChallengeResponseFromHeader(challengeHeader,
                                            authority.toString());
                            headers.put(AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER,
                                    challengeResponse.mAuthorizationHeaderValue);
                            Logger.v(TAG, "Sending request with challenge response");
                            response = mWebRequestHandler.sendPost(authority, headers,
                                    requestMessage.getBytes(AuthenticationConstants.ENCODING_UTF8),
                                    "application/x-www-form-urlencoded");
                        }
                    } else {
                        throw new AuthenticationException(
                                ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                                "Challenge header is empty");
                    }
                } else {
                    // AAD server returns 401 response for wrong request
                    // messages
                    Logger.v(TAG, "401 http status code is returned without authorization header");
                }
            }

            boolean isBodyEmpty = TextUtils.isEmpty(response.getBody());
            if (!isBodyEmpty) {
                // Protocol related errors will read the error stream and report
                // the error and error description
                Logger.v(TAG, "Token request does not have exception");
                result = processTokenResponse(response);
                ClientMetrics.INSTANCE.setLastError(null);
            }
            if (result == null) {
                // non-protocol related error
                String errMessage = isBodyEmpty ? "Status code:" + response.getStatusCode() : response.getBody();
                Logger.e(TAG, "Server error message", errMessage, ADALError.SERVER_ERROR);
                throw new AuthenticationException(ADALError.SERVER_ERROR, errMessage);
            } else {
                ClientMetrics.INSTANCE.setLastErrorCodes(result.getErrorCodes());
            }
        } catch (UnsupportedEncodingException e) {
            ClientMetrics.INSTANCE.setLastError(null);
            Logger.e(TAG, e.getMessage(), "", ADALError.ENCODING_IS_NOT_SUPPORTED, e);
            throw e;
        } catch (IOException e) {
            ClientMetrics.INSTANCE.setLastError(null);
            Logger.e(TAG, e.getMessage(), "", ADALError.SERVER_ERROR, e);
            throw e;
        } finally {
            ClientMetrics.INSTANCE.endClientMetricsRecord(ClientMetricsEndpointType.TOKEN,
                    mRequest.getCorrelationId());
        }

        return result;
    }

    public static String decodeProtocolState(String encodedState) {

        if (!StringExtensions.IsNullOrBlank(encodedState)) {
            byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING | Base64.URL_SAFE);

            return new String(stateBytes);
        }

        return null;
    }

    public String encodeProtocolState() {
        String state = String.format("a=%s&r=%s", mRequest.getAuthority(), mRequest.getResource());
        return Base64.encodeToString(state.getBytes(), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        return headers;
    }

    /**
     * extract AuthenticationResult object from response body if available
     * 
     * @param webResponse
     * @return
     */
    private AuthenticationResult processTokenResponse(HttpWebResponse webResponse) throws AuthenticationException {
        AuthenticationResult result;
        String correlationIdInHeader = null;
        if (webResponse.getResponseHeaders() != null
                && webResponse.getResponseHeaders().containsKey(
                        AuthenticationConstants.AAD.CLIENT_REQUEST_ID)) {
            // headers are returning as a list
            List<String> listOfHeaders = webResponse.getResponseHeaders().get(
                    AuthenticationConstants.AAD.CLIENT_REQUEST_ID);
            if (listOfHeaders != null && listOfHeaders.size() > 0) {
                correlationIdInHeader = listOfHeaders.get(0);
            }
        }

        final int statusCode = webResponse.getStatusCode();
        switch (statusCode) {
        case HttpURLConnection.HTTP_OK:
        case HttpURLConnection.HTTP_BAD_REQUEST:
        case HttpURLConnection.HTTP_UNAUTHORIZED:
            try {
                result = parseJsonResponse(webResponse.getBody());
            } catch (JSONException e) {
                throw new AuthenticationException(ADALError.SERVER_INVALID_JSON_RESPONSE, "Can't parse server response " + webResponse.getBody(), e);
            }

        break;
        default: 
            throw new AuthenticationException(ADALError.SERVER_ERROR, "Unexpected server response " + webResponse.getBody());
        }

        // Set correlationId in the result
        if (correlationIdInHeader != null && !correlationIdInHeader.isEmpty()) {
            try {
                UUID correlation = UUID.fromString(correlationIdInHeader);
                if (!correlation.equals(mRequest.getCorrelationId())) {
                    Logger.w(TAG, "CorrelationId is not matching", "",
                            ADALError.CORRELATION_ID_NOT_MATCHING_REQUEST_RESPONSE);
                }

                Logger.v(TAG, "Response correlationId:" + correlationIdInHeader);
            } catch (IllegalArgumentException ex) {
                Logger.e(TAG, "Wrong format of the correlation ID:" + correlationIdInHeader, "",
                        ADALError.CORRELATION_ID_FORMAT, ex);
            }
        }

        return result;
    }
    
    private AuthenticationResult parseJsonResponse(final String responseBody) throws JSONException {
        HashMap<String, String> responseItems = new HashMap<String, String>();
        extractJsonObjects(responseItems, responseBody);
        return processUIResponseParams(responseItems);
    }
}
