
package com.microsoft.adal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Build;
import android.util.Base64;

/**
 * base oauth class
 * 
 * @author omercan
 */
class Oauth2 {

    private AuthenticationRequest mRequest;

    /**
     * for mocking webrequests
     */
    private IWebRequestHandler mWebRequestHandler;

    private final static String TAG = "Oauth";

    private final static String DEFAULT_AUTHORIZE_ENDPOINT = "/oauth2/authorize";

    private final static String DEFAULT_TOKEN_ENDPOINT = "/oauth2/token";

    private final static String JSON_PARSING_ERROR = "It failed to parse response as json";

    Oauth2(AuthenticationRequest request) {
        mRequest = request;
        mWebRequestHandler = null;
    }

    Oauth2(AuthenticationRequest request, IWebRequestHandler webRequestHandler) {
        mRequest = request;
        mWebRequestHandler = webRequestHandler;
    }

    public String getAuthorizationEndpoint() {
        return mRequest.getAuthority() + DEFAULT_AUTHORIZE_ENDPOINT;
    }

    public String getTokenEndpoint() {
        return mRequest.getAuthority() + DEFAULT_TOKEN_ENDPOINT;
    }

    public String getCodeRequestUrl() throws UnsupportedEncodingException {

        String requestUrl = String
                .format("%s?response_type=%s&client_id=%s&resource=%s&redirect_uri=%s&state=%s",
                        getAuthorizationEndpoint(), AuthenticationConstants.OAuth2.CODE, URLEncoder
                                .encode(mRequest.getClientId(),
                                        AuthenticationConstants.ENCODING_UTF8), URLEncoder.encode(
                                mRequest.getResource(), AuthenticationConstants.ENCODING_UTF8),
                        URLEncoder.encode(mRequest.getRedirectUri(),
                                AuthenticationConstants.ENCODING_UTF8), encodeProtocolState());

        if (mRequest.getLoginHint() != null && !mRequest.getLoginHint().isEmpty()) {
            requestUrl = String.format("%s&%s=%s", requestUrl,
                    AuthenticationConstants.AAD.LOGIN_HINT, URLEncoder.encode(
                            mRequest.getLoginHint(), AuthenticationConstants.ENCODING_UTF8));
        }

        requestUrl = String.format("%s&%s=%s", requestUrl,
                AuthenticationConstants.AAD.INFO_ADAL_PRODUCT, "Android");
        requestUrl = String.format("%s&%s=%s", requestUrl,
                AuthenticationConstants.AAD.INFO_ADAL_VERSION, URLEncoder.encode(
                        AuthenticationContext.getVersionName(),
                        AuthenticationConstants.ENCODING_UTF8));
        requestUrl = String.format("%s&%s=%s", requestUrl, AuthenticationConstants.AAD.INFO_OS,
                URLEncoder
                        .encode("" + Build.VERSION.SDK_INT, AuthenticationConstants.ENCODING_UTF8));
        requestUrl = String.format("%s&%s=%s", requestUrl, AuthenticationConstants.AAD.INFO_DM,
                URLEncoder.encode("" + android.os.Build.MODEL,
                        AuthenticationConstants.ENCODING_UTF8));
        
        // Setting prompt behavior to always will skip the cookies for webview.
        // It is added to authorization url.
        if (mRequest.getPrompt() == PromptBehavior.Always) {
            requestUrl = String.format("%s&%s=%s", requestUrl,
                    AuthenticationConstants.AAD.QUERY_PROMPT, URLEncoder.encode(
                            AuthenticationConstants.AAD.QUERY_PROMPT_VALUE,
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

    public String buildTokenRequestMessage(String code) throws UnsupportedEncodingException {

        String message = String.format("%s=%s&%s=%s&%s=%s&%s=%s",
                AuthenticationConstants.OAuth2.GRANT_TYPE,
                StringExtensions.URLFormEncode(AuthenticationConstants.OAuth2.AUTHORIZATION_CODE),

                AuthenticationConstants.OAuth2.CODE, StringExtensions.URLFormEncode(code),

                AuthenticationConstants.OAuth2.CLIENT_ID,
                StringExtensions.URLFormEncode(mRequest.getClientId()),

                AuthenticationConstants.OAuth2.REDIRECT_URI,
                StringExtensions.URLFormEncode(mRequest.getRedirectUri()));

        if (!StringExtensions.IsNullOrBlank(mRequest.getLoginHint())) {
            message = String.format("%s&%s=%s", message, AuthenticationConstants.AAD.LOGIN_HINT,
                    URLEncoder.encode(mRequest.getLoginHint(),
                            AuthenticationConstants.ENCODING_UTF8));
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

        if (!StringExtensions.IsNullOrBlank(mRequest.getResource())) {
            message = String.format("%s&%s=%s", message, AuthenticationConstants.AAD.RESOURCE,
                    StringExtensions.URLFormEncode(mRequest.getResource()));
        }

        return message;
    }

    public static AuthenticationResult processUIResponseParams(HashMap<String, String> response) {

        AuthenticationResult result = new AuthenticationResult();

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
                    response.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION), correlationId);

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
            if (response.containsKey(AuthenticationConstants.OAuth2.ID_TOKEN)) {
                // IDtoken is related to Azure AD and returned with token
                // response. ADFS does not return that.
                String idToken = response.get(AuthenticationConstants.OAuth2.ID_TOKEN);
                if (!StringExtensions.IsNullOrBlank(idToken)) {
                    userinfo = parseIdToken(idToken);
                }
            }

            result = new AuthenticationResult(
                    response.get(AuthenticationConstants.OAuth2.ACCESS_TOKEN),
                    response.get(AuthenticationConstants.OAuth2.REFRESH_TOKEN), expires.getTime(),
                    isMultiResourcetoken, userinfo);

        }

        return result;
    }

    /**
     * parse user id token string
     * 
     * @param idtoken
     * @return UserInfo
     */
    private static UserInfo parseIdToken(String idtoken) {
        UserInfo userinfo = null;
        if (!StringExtensions.IsNullOrBlank(idtoken)) {
            Logger.v(TAG, "IdToken is not provided");
        }

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

                    userinfo = new UserInfo(idtokenInfo);
                    Logger.v(
                            TAG,
                            "IdToken is extracted from token response for userid"
                                    + userinfo.getUserId());
                }
            }

        } catch (Exception ex) {
            Logger.e(TAG, "Error in parsing user id token", null,
                    ADALError.IDTOKEN_PARSING_FAILURE, ex);
        }
        return userinfo;
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

    public void refreshToken(String refreshToken,
            final AuthenticationCallback<AuthenticationResult> authenticationCallback) {

        String requestMessage = null;
        if (mWebRequestHandler == null) {
            Logger.v(TAG, "Web request is not set correctly");
            authenticationCallback.onError(new IllegalArgumentException("webRequestHandler"));
            return;
        }

        // Token request message
        try {
            requestMessage = buildRefreshTokenRequestMessage(refreshToken);
        } catch (UnsupportedEncodingException encoding) {
            Logger.e(TAG, encoding.getMessage(), "", ADALError.ENCODING_IS_NOT_SUPPORTED, encoding);
            authenticationCallback.onError(encoding);
            return;
        }

        Logger.v(TAG, "Refresh token request message:" + requestMessage);

        postMessage(requestMessage, authenticationCallback);
    }

    /**
     * parse final url for code(normal flow) or token(implicit flow) and then it
     * proceeds to next step.
     * 
     * @param authorizationUrl browser reached to this final url and it has code
     *            or token for next step
     * @param authenticationCallback
     */
    public void getToken(String authorizationUrl,
            AuthenticationCallback<AuthenticationResult> authenticationCallback) {

        if (StringExtensions.IsNullOrBlank(authorizationUrl)) {
            authenticationCallback.onError(new IllegalArgumentException("finalUrl"));
            return;
        }

        // Success
        HashMap<String, String> parameters = getUrlParameters(authorizationUrl);
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

                // Check if we have token or code
                if (result.getStatus() == AuthenticationResult.AuthenticationStatus.Succeeded) {
                    if (!result.getCode().isEmpty()) {

                        // Get token and use external callback to set result
                        getTokenForCode(result.getCode(), authenticationCallback);

                    } else if (!StringExtensions.IsNullOrBlank(result.getAccessToken())) {
                        // We have token directly with implicit flow
                        authenticationCallback.onSuccess(result);
                    } else {
                        authenticationCallback.onError(new AuthenticationException(
                                ADALError.AUTH_FAILED_NO_TOKEN));
                    }
                } else {
                    authenticationCallback.onError(new AuthenticationException(
                            ADALError.AUTH_FAILED_NO_TOKEN, result.getErrorCode() + " "
                                    + result.getErrorDescription()));
                }

            } else {
                authenticationCallback.onError(new AuthenticationException(
                        ADALError.AUTH_FAILED_BAD_STATE));
            }
        } else {

            // The response from the server had no state
            authenticationCallback.onError(new AuthenticationException(
                    ADALError.AUTH_FAILED_NO_STATE));
        }
    }

    /**
     * get code and exchange for token
     * 
     * @param request
     * @param code
     * @param authenticationCallback
     */
    public void getTokenForCode(String code,
            final AuthenticationCallback<AuthenticationResult> authenticationCallback)
            throws IllegalArgumentException {

        String requestMessage = null;
        if (mWebRequestHandler == null) {
            throw new IllegalArgumentException("webRequestHandler");
        }

        // Token request message
        try {
            requestMessage = buildTokenRequestMessage(code);
        } catch (UnsupportedEncodingException encoding) {
            Logger.e(TAG, encoding.getMessage(), "", ADALError.ENCODING_IS_NOT_SUPPORTED, encoding);
            authenticationCallback.onError(encoding);
            return;
        }

        postMessage(requestMessage, authenticationCallback);
    }

    private void postMessage(String requestMessage,
            final AuthenticationCallback<AuthenticationResult> authenticationCallback) {
        URL authority = null;

        try {
            authority = new URL(getTokenEndpoint());
        } catch (MalformedURLException e1) {
            Logger.e(TAG, e1.getMessage(), "", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, e1);
            authenticationCallback.onError(e1);
            return;
        }

        HashMap<String, String> headers = getRequestHeaders();
        try {
            mWebRequestHandler.setRequestCorrelationId(mRequest.getCorrelationId());
            mWebRequestHandler.sendAsyncPost(authority, headers,
                    requestMessage.getBytes(AuthenticationConstants.ENCODING_UTF8),
                    "application/x-www-form-urlencoded", new HttpWebRequestCallback() {

                        @Override
                        public void onComplete(HttpWebResponse response, Exception exception) {

                            if (exception != null
                                    && (response == null || response.getBody() == null)) {
                                Logger.e(TAG, exception.getMessage(), "", ADALError.SERVER_ERROR,
                                        exception);
                                authenticationCallback.onError(exception);
                            } else {
                                Logger.v(TAG, "Token request does not have errors");
                                try {
                                    AuthenticationResult result = processTokenResponse(response);
                                    authenticationCallback.onSuccess(result);
                                } catch (Exception ex) {
                                    Logger.e(TAG, exception.getMessage(), "",
                                            ADALError.SERVER_ERROR, exception);
                                    authenticationCallback.onError(exception);
                                    return;
                                }
                            }
                        }
                    });

        } catch (IllegalArgumentException e) {
            Logger.e(TAG, e.getMessage(), "", ADALError.ARGUMENT_EXCEPTION, e);
            authenticationCallback.onError(e);
        } catch (UnsupportedEncodingException e) {
            Logger.e(TAG, e.getMessage(), "", ADALError.ENCODING_IS_NOT_SUPPORTED, e);
            authenticationCallback.onError(e);
        } catch (IOException e) {
            Logger.e(TAG, e.getMessage(), "", ADALError.IO_EXCEPTION, e);
            authenticationCallback.onError(e);
        }
    }

    public static String decodeProtocolState(String encodedState) {

        if (!StringExtensions.IsNullOrBlank(encodedState)) {
            byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING | Base64.URL_SAFE);

            return new String(stateBytes);
        }

        return null;
    }

    private HashMap<String, String> getUrlParameters(String finalUrl) {
        Uri response = Uri.parse(finalUrl);
        String fragment = response.getFragment();
        HashMap<String, String> parameters = HashMapExtensions.URLFormDecode(fragment);

        if (parameters == null || parameters.isEmpty()) {
            String queryParameters = response.getQuery();
            parameters = HashMapExtensions.URLFormDecode(queryParameters);
        }
        return parameters;
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

        return result;
    }
}
