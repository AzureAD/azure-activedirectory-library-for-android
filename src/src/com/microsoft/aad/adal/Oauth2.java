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

import com.microsoft.aad.adal.ChallangeResponseBuilder.ChallangeResponse;

import android.net.Uri;
import android.os.Build;
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
                            + response.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION)
                            + "CorrelationId:" + correlationId);

            result = new AuthenticationResult(response.get(AuthenticationConstants.OAuth2.ERROR),
                    response.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION));

        } else if (response.containsKey(AuthenticationConstants.OAuth2.CODE)) {
            result = new AuthenticationResult(response.get(AuthenticationConstants.OAuth2.CODE));
        } else if (response.containsKey(AuthenticationConstants.OAuth2.ACCESS_TOKEN)) {
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

            result = new AuthenticationResult(
                    response.get(AuthenticationConstants.OAuth2.ACCESS_TOKEN),
                    response.get(AuthenticationConstants.OAuth2.REFRESH_TOKEN), expires.getTime(),
                    isMultiResourcetoken, userinfo, tenantId, rawIdToken);
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
     * @return Token in the AuthenticationResult. Null result if response does not have protocol error.
     * @throws Exception
     */
    public AuthenticationResult getToken(String authorizationUrl) throws Exception {

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
                if (result != null && !result.getCode().isEmpty()) {

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
                if(response.getResponseException() != null){
                    throw response.getResponseException();
                }
            }
        } catch (IllegalArgumentException e) {
            Logger.e(TAG, e.getMessage(), "", ADALError.ARGUMENT_EXCEPTION, e);
            throw e;
        } catch (UnsupportedEncodingException e) {
            Logger.e(TAG, e.getMessage(), "", ADALError.ENCODING_IS_NOT_SUPPORTED, e);
            throw e;
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage(), "", ADALError.SERVER_ERROR, e);
            throw e;
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
                result = new AuthenticationResult(JSON_PARSING_ERROR, ex.getMessage());
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
                    errMessage);
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
