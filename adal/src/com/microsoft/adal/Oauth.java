
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

import org.json.JSONObject;

import com.microsoft.adal.ErrorCodes.ADALError;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;

/**
 * not part of API
 * 
 * @author omercan
 */
class Oauth {

    private AuthenticationRequest mRequest;

    /**
     * for mocking webrequests
     */
    private IWebRequestHandler mWebRequestHandler;

    /**
     * RequestAuthEndpoint to append in authority url
     */
    private final static String AUTH_ENDPOINT_APPEND = "/oauth2/authorize";

    /**
     * RequesttokenEndpoint to append in authority url
     */
    private final static String TOKEN_ENDPOINT_APPEND = "/oauth2/token";

    private final static String TAG = "Oauth";

    private final static String JSON_PARSING_ERROR = "It failed to parse response as json";

    Oauth(AuthenticationRequest request) {
        mRequest = request;
        mWebRequestHandler = null;
    }

    Oauth(AuthenticationRequest request, IWebRequestHandler webRequestHandler) {
        mRequest = request;
        mWebRequestHandler = webRequestHandler;
    }

    public String getAuthorizationEndpoint() {
        return mRequest.getAuthority() + AUTH_ENDPOINT_APPEND;
    }

    public String getTokenEndpoint() {
        return mRequest.getAuthority() + TOKEN_ENDPOINT_APPEND;
    }

    public String getCodeRequestUrl() throws UnsupportedEncodingException {

        String requestUrl = String
                .format("%s?response_type=%s&client_id=%s&resource=%s&redirect_uri=%s&state=%s",
                        mRequest.getAuthority() + AUTH_ENDPOINT_APPEND,
                        AuthenticationConstants.OAuth2.CODE, mRequest.getClientId(), URLEncoder
                                .encode(mRequest.getResource(),
                                        AuthenticationConstants.ENCODING_UTF8), URLEncoder.encode(
                                mRequest.getRedirectUri(), AuthenticationConstants.ENCODING_UTF8),
                        encodeProtocolState());

        if (mRequest.getLoginHint() != null && !mRequest.getLoginHint().isEmpty()) {
            requestUrl = String.format("%s&%s=%s", requestUrl,
                    AuthenticationConstants.AAD.LOGIN_HINT, URLEncoder.encode(
                            mRequest.getLoginHint(), AuthenticationConstants.ENCODING_UTF8));
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
                    mRequest.getLoginHint());
        }

        return message;
    }

    public static AuthenticationResult processUIResponseParams(HashMap<String, String> response) {

        AuthenticationResult result = new AuthenticationResult();

        if (response.containsKey(AuthenticationConstants.OAuth2.ERROR)) {
            // Error response from the server
            // TODO: Should we kill the authorization object?
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

            result = new AuthenticationResult(
                    response.get(AuthenticationConstants.OAuth2.ACCESS_TOKEN),
                    response.get(AuthenticationConstants.OAuth2.REFRESH_TOKEN), expires.getTime(),
                    isMultiResourcetoken);

        }

        return result;
    }

    /**
     * parse final url for code(normal flow) or token(implicit flow) and proceed
     * to next step.
     * 
     * @param finalUrl browser reached to this final url and it has code or
     *            token for next step
     * @param authenticationCallback
     */
    public void processWebViewResponse(String finalUrl,
            AuthenticationCallback<AuthenticationResult> authenticationCallback) {

        if (StringExtensions.IsNullOrBlank(finalUrl)) {
            authenticationCallback.onError(new IllegalArgumentException("finalUrl"));
            return;
        }

        // Success
        HashMap<String, String> parameters = getUrlParameters(finalUrl);
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
                        exchangeCodeForToken(result.getCode(), authenticationCallback);

                    } else if (!StringExtensions.IsNullOrBlank(result.getAccessToken())) {
                        // We have token directly with implicit flow
                        authenticationCallback.onSuccess(result);
                    } else {
                        authenticationCallback.onError(new AuthenticationException(
                                ADALError.AUTH_FAILED_NO_TOKEN));
                    }
                } else {
                    authenticationCallback.onError(new AuthenticationException(result
                            .getErrorCode(), result.getErrorDescription()));
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
    public void exchangeCodeForToken(String code,
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
            Log.e(TAG, encoding.getMessage(), encoding);
            authenticationCallback.onError(encoding);
            return;
        }

        URL authority = null;

        try {
            authority = new URL(getTokenEndpoint());
        } catch (MalformedURLException e1) {
            Log.e(TAG, e1.getMessage(), e1);
            authenticationCallback.onError(e1);
            return;
        }

        // Async post
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        try {
            mWebRequestHandler.sendAsyncPost(authority, headers,
                    requestMessage.getBytes(AuthenticationConstants.ENCODING_UTF8),
                    "application/x-www-form-urlencoded", new HttpWebRequestCallback() {

                        @Override
                        public void onComplete(HttpWebResponse response, Exception exception) {
                            if (exception != null) {
                                Log.e(TAG, exception.getMessage(), exception);
                                authenticationCallback.onError(exception);
                            } else {
                                Log.d(TAG, "token request does not have errors");
                                AuthenticationResult result = processTokenResponse(response);

                                if (result != null
                                        && result.getStatus() == AuthenticationResult.AuthenticationStatus.Succeeded) {

                                    authenticationCallback.onSuccess(result);
                                } else {
                                    // did not get token
                                    authenticationCallback.onError(new AuthenticationException(
                                            result.getErrorCode(), result.getErrorDescription()));
                                }
                            }
                        }
                    });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage(), e);
            authenticationCallback.onError(e);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
            authenticationCallback.onError(e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
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

    private String encodeProtocolState() {
        String state = String.format("a=%s&r=%s", mRequest.getAuthority(), mRequest.getResource());
        return Base64.encodeToString(state.getBytes(), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private AuthenticationResult processTokenResponse(HttpWebResponse webResponse) {
        AuthenticationResult result = new AuthenticationResult();
        HashMap<String, String> responseItems = new HashMap<String, String>();

        if (webResponse.getStatusCode() <= 400) {

            if (webResponse.getBody() != null && webResponse.getBody().length > 0) {
                try {
                    final JSONObject jsonObject = new JSONObject(new String(webResponse.getBody()));

                    @SuppressWarnings("unchecked")
                    final Iterator<String> i = jsonObject.keys();

                    while (i.hasNext()) {
                        final String key = i.next();
                        responseItems.put(key, jsonObject.getString(key));
                    }

                    result = processUIResponseParams(responseItems);
                } catch (final Exception ex) {
                    // There is no recovery possible here, so
                    // catch the
                    // generic Exception
                    Log.e(TAG, ex.getMessage(), ex);
                    result = new AuthenticationResult(JSON_PARSING_ERROR, ex.getMessage());
                }
            }
        } else {
            result = new AuthenticationResult(String.valueOf(webResponse.getStatusCode()),
                    new String(webResponse.getBody()));
        }

        return result;
    }
}
