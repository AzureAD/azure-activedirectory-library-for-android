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

import android.os.Build;
import android.util.Base64;

import com.microsoft.aad.adal.ChallangeResponseBuilder.ChallangeResponse;

/**
 * Base Oauth class.
 */
class Oauth2 {

    private AuthenticationRequest mRequest;

    private IWebRequestHandler mWebRequestHandler;

    private IJWSBuilder mJWSBuilder = new JWSBuilder();

    private final static String TAG = "Oauth";

    private final static String DEFAULT_AUTHORIZE_ENDPOINT = "/oauth2/v2.0/authorize";

    private final static String DEFAULT_TOKEN_ENDPOINT = "/oauth2/v2.0/token";

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
        String requestUrl = String.format("response_type=%s&client_id=%s&scope=%s&redirect_uri=%s",
                AuthenticationConstants.OAuth2.CODE, URLEncoder.encode(mRequest.getClientId(),
                        AuthenticationConstants.ENCODING_UTF8), URLEncoder.encode(StringExtensions
                        .createStringFromArray(mRequest.getDecoratedScopeConsent(), " "),
                        AuthenticationConstants.ENCODING_UTF8), URLEncoder.encode(
                        mRequest.getRedirectUri(), AuthenticationConstants.ENCODING_UTF8));

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
        
        if (!StringExtensions.IsNullOrBlank(mRequest.getPolicy())) {
            requestUrl = String.format("%s&%s=%s", requestUrl,
                    AuthenticationConstants.AAD.QUERY_POLICY, URLEncoder.encode(
                            mRequest.getPolicy(),
                            AuthenticationConstants.ENCODING_UTF8));
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
        
        if (!StringExtensions.IsNullOrBlank(mRequest.getPolicy())) {
            message = String.format("%s&%s=%s", message, AuthenticationConstants.AAD.QUERY_POLICY,
                    URLEncoder.encode(mRequest.getPolicy(), AuthenticationConstants.ENCODING_UTF8));
        }
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

        String scope = StringExtensions.createStringFromArray(mRequest.getDecoratedScopeRequest(),
                " ");
        if (!StringExtensions.IsNullOrBlank(scope)) {
            message = String.format("%s&%s=%s", message, AuthenticationConstants.AAD.SCOPE,
                    StringExtensions.URLFormEncode(scope));
        }

        if (!StringExtensions.IsNullOrBlank(mRequest.getPolicy())) {
            message = String.format("%s&%s=%s", message, AuthenticationConstants.AAD.QUERY_POLICY,
                    URLEncoder.encode(mRequest.getPolicy(), AuthenticationConstants.ENCODING_UTF8));
        }
        
        return message;
    }

    public AuthenticationResult processUIResponseParams(HashMap<String, String> response) {

        AuthenticationResult result = null;

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
        } else if (response.containsKey(AuthenticationConstants.OAuth2.ACCESS_TOKEN) ||
                response.containsKey(AuthenticationConstants.OAuth2.ID_TOKEN)) {
            // Token response
            boolean isMultiResourcetoken = false;
            
            // AccessToken/Idtoken expiresIn
            String expiresInLookUp = "expires_in";
            String token = response.get(AuthenticationConstants.OAuth2.ACCESS_TOKEN);
            if(mRequest.isIdTokenRequest()){
                expiresInLookUp = "idtoken_expires_in";
                token = response.get(AuthenticationConstants.OAuth2.ID_TOKEN);
            }
            
            String expires_in = response.get(expiresInLookUp);
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
            String rawProfileInfo = null;
            if (response.containsKey(AuthenticationConstants.OAuth2.PROFILE_INFO)) {
                // IDtoken is related to Azure AD and returned with token
                // response. ADFS does not return that.
                rawProfileInfo = response.get(AuthenticationConstants.OAuth2.PROFILE_INFO);
                if (!StringExtensions.IsNullOrBlank(rawProfileInfo)) {
                    ProfileInfo tokenParsed = parseProfileInfo(rawProfileInfo);
                    if (tokenParsed != null) {
                        tenantId = tokenParsed.mTenantId;
                        userinfo = new UserInfo(tokenParsed);
                    }
                } else {
                    Logger.v(TAG, "ProfileInfo is not provided");
                }
            }

            result = new AuthenticationResult(token,
                    response.get(AuthenticationConstants.OAuth2.REFRESH_TOKEN), expires.getTime(),
                    isMultiResourcetoken, userinfo, tenantId, rawProfileInfo);
            result.setScopeInResponse(response.get(AuthenticationConstants.OAuth2.SCOPE));
        }

        return result;
    }

    /**
     * parse user id token string.
     * 
     * @param idtoken
     * @return UserInfo
     */
    private static ProfileInfo parseProfileInfo(String profileInfo) {
        try {
            // Message Base64 encoded text
             

            if (!StringExtensions.IsNullOrBlank(profileInfo)) {
                // URL_SAFE: Encoder/decoder flag bit to use
                // "URL and filename safe" variant of Base64
                // (see RFC 3548 section 4) where - and _ are used in place of +
                // and /.
                byte[] data = Base64.decode(profileInfo, Base64.URL_SAFE);
                String decodedBody = new String(data, "UTF-8");

                HashMap<String, String> responseItems = new HashMap<String, String>();
                extractJsonObjects(responseItems, decodedBody);
                if (responseItems != null && !responseItems.isEmpty()) {
                    ProfileInfo idtokenInfo = new ProfileInfo();
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
        } catch (Exception ex) {
            Logger.e(TAG, "Error in parsing user id token", null,
                    ADALError.IDTOKEN_PARSING_FAILURE, ex);
        }
        return null;
    }

    private static void extractJsonObjects(HashMap<String, String> responseItems, String jsonStr)
            throws JSONException {
        final JSONObject jsonObject = new JSONObject(jsonStr);

        @SuppressWarnings("unchecked")
        final Iterator<String> i = jsonObject.keys();

        while (i.hasNext()) {
            final String key = i.next();
            responseItems.put(key, jsonObject.getString(key));
        }
    }

    public AuthenticationResult refreshToken(String refreshToken) throws Exception {
        String requestMessage = null;
        if (mWebRequestHandler == null) {
            Logger.v(TAG, "Web request is not set correctly");
            throw new IllegalArgumentException("webRequestHandler");
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
        headers.put(AuthenticationConstants.Broker.CHALLANGE_TLS_INCAPABLE,
                AuthenticationConstants.Broker.CHALLANGE_TLS_INCAPABLE_VERSION);
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
    public AuthenticationResult getToken(String authorizationUrl) throws Exception {

        if (StringExtensions.IsNullOrBlank(authorizationUrl)) {
            throw new IllegalArgumentException("authorizationUrl");
        }

        // Success
        HashMap<String, String> parameters = StringExtensions.getUrlParameters(authorizationUrl);

        AuthenticationResult result = processUIResponseParams(parameters);

        // Check if we have code
        if (result != null && result.getCode() != null && !result.getCode().isEmpty()) {

            // Get token and use external callback to set result
            return getTokenForCode(result.getCode());
        }

        return result;
    }

    /**
     * get code and exchange for token.
     * 
     * @param code
     * @return Token in the AuthenticationResult
     * @throws Exception
     */
    public AuthenticationResult getTokenForCode(String code) throws Exception {

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
            throws Exception {
        URL authority = null;
        AuthenticationResult result = null;
        authority = StringExtensions.getUrl(getTokenEndpoint());
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
                                AuthenticationConstants.Broker.CHALLANGE_REQUEST_HEADER)) {

                    // Device certificate challenge will send challenge request
                    // in 401 header.
                    String challangeHeader = response.getResponseHeaders()
                            .get(AuthenticationConstants.Broker.CHALLANGE_REQUEST_HEADER).get(0);
                    Logger.v(TAG, "Device certificate challange request:" + challangeHeader);
                    if (!StringExtensions.IsNullOrBlank(challangeHeader)) {

                        // Handle each specific challenge header
                        if (StringExtensions.hasPrefixInHeader(challangeHeader,
                                AuthenticationConstants.Broker.CHALLANGE_RESPONSE_TYPE)) {
                            Logger.v(TAG, "Challange is related to device certificate");
                            ChallangeResponseBuilder certHandler = new ChallangeResponseBuilder(
                                    mJWSBuilder);
                            Logger.v(TAG, "Processing device challange");
                            final ChallangeResponse challangeResponse = certHandler
                                    .getChallangeResponseFromHeader(challangeHeader,
                                            authority.toString());
                            headers.put(AuthenticationConstants.Broker.CHALLANGE_RESPONSE_HEADER,
                                    challangeResponse.mAuthorizationHeaderValue);
                            Logger.v(TAG, "Sending request with challenge response");
                            response = mWebRequestHandler.sendPost(authority, headers,
                                    requestMessage.getBytes(AuthenticationConstants.ENCODING_UTF8),
                                    "application/x-www-form-urlencoded");
                        }
                    } else {
                        throw new AuthenticationException(
                                ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                                "Challange header is empty");
                    }
                } else {

                    // AAD server returns 401 response for wrong request
                    // messages
                    Logger.v(TAG, "401 http status code is returned without authorization header");
                }
            }

            if (response.getBody() != null) {

                // Protocol related errors will read the error stream and report
                // the error and error description
                Logger.v(TAG, "Token request does not have exception");
                result = processTokenResponse(response);
                ClientMetrics.INSTANCE.setLastError(null);
            }

            if (result == null) {
                // non-protocol related error
                String errMessage = null;
                byte[] message = response.getBody();
                if (message != null) {
                    errMessage = new String(message);
                } else {
                    errMessage = "Status code:" + String.valueOf(response.getStatusCode());
                }

                Logger.v(TAG, "Server error message:" + errMessage);
                if (response.getResponseException() != null) {
                    throw response.getResponseException();
                }
            } else {
                ClientMetrics.INSTANCE.setLastErrorCodes(result.getErrorCodes());
            }
        } catch (IllegalArgumentException e) {
            ClientMetrics.INSTANCE.setLastError(null);
            Logger.e(TAG, e.getMessage(), "", ADALError.ARGUMENT_EXCEPTION, e);
            throw e;
        } catch (UnsupportedEncodingException e) {
            ClientMetrics.INSTANCE.setLastError(null);
            Logger.e(TAG, e.getMessage(), "", ADALError.ENCODING_IS_NOT_SUPPORTED, e);
            throw e;
        } catch (Exception e) {
            ClientMetrics.INSTANCE.setLastError(null);
            Logger.e(TAG, e.getMessage(), "", ADALError.SERVER_ERROR, e);
            throw e;
        } finally {
            ClientMetrics.INSTANCE.endClientMetricsRecord(ClientMetricsEndpointType.TOKEN,
                    mRequest.getCorrelationId());
        }

        return result;
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
    private AuthenticationResult processTokenResponse(HttpWebResponse webResponse) {
        AuthenticationResult result = new AuthenticationResult();
        HashMap<String, String> responseItems = new HashMap<String, String>();
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

        if (webResponse.getBody() != null && webResponse.getBody().length > 0) {

            // invalid refresh token calls has error related items in the body.
            // Status is 400 for those.
            try {
                String jsonStr = new String(webResponse.getBody());
                extractJsonObjects(responseItems, jsonStr);
                result = processUIResponseParams(responseItems);
            } catch (final Exception ex) {
                // There is no recovery possible here, so
                // catch the
                // generic Exception
                Logger.e(TAG, ex.getMessage(), "", ADALError.SERVER_INVALID_JSON_RESPONSE, ex);
                result = new AuthenticationResult(JSON_PARSING_ERROR, ex.getMessage(), null);
            }
        } else {
            String errMessage = null;
            byte[] message = webResponse.getBody();
            if (message != null) {
                errMessage = new String(message);
            } else {
                errMessage = "Status code:" + String.valueOf(webResponse.getStatusCode());
            }
            Logger.v(TAG, "Server error message:" + errMessage);
            result = new AuthenticationResult(String.valueOf(webResponse.getStatusCode()),
                    errMessage, null);
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
            } catch (Exception ex) {
                Logger.e(TAG, "Wrong format of the correlation ID:" + correlationIdInHeader, "",
                        ADALError.CORRELATION_ID_FORMAT, ex);
            }
        }

        return result;
    }
}
