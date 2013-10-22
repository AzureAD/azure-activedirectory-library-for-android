
package com.microsoft.adal;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//TODO add Dotnet style exceptions
public class AuthenticationParameters {

    public final static String AUTHENTICATE_HEADER = "WWW-Authenticate";
    public final static String BEARER = "bearer";
    public final static String AUTHORITY_KEY = "authorization_uri";
    public final static String RESOURCE_KEY = "resource_id";

    private String mAuthority;
    private String mResource;

    public AuthenticationParameters()
    {
    }

    public AuthenticationParameters(String authority, String resource)
    {
        mAuthority = authority;
        mResource = resource;
    }

    public interface AuthenticationParamCallback {
        public void onCompleted(Exception exception, AuthenticationParameters param);
    }

    /*
     * returns authenticationParam object in callback, if webresponse returns
     * challange response and authorization endpoint
     */
    public static void createFromResourceUrl(URL resourceUrl, AuthenticationParamCallback callback) {

        if (callback == null)
        {
            return;
        }

        HttpWebRequest webRequest = new HttpWebRequest(resourceUrl);
        webRequest.getRequestHeaders().put("Accept", "application/json");
        final AuthenticationParamCallback externalCallback = callback;

        try {
            webRequest.sendAsyncGet(
                    new HttpWebRequestCallback() {
                        @Override
                        public void onComplete(Exception exception,
                                HttpWebResponse webResponse) {

                            if (exception == null)
                            {
                                try {
                                    externalCallback.onCompleted(null, parseResponse(webResponse));
                                } catch (IllegalArgumentException exc)
                                {
                                    externalCallback.onCompleted(exc, null);
                                }
                            }
                            else
                                externalCallback.onCompleted(exception, null);
                        }
                    });
        } catch (Exception e) {
            callback.onCompleted(e, null);
        }
    }

    private static AuthenticationParameters parseResponse(HttpWebResponse webResponse)
    {
        // Depending on the service side implementation for this resource
        if (webResponse.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Map<String, List<String>> responseHeaders = webResponse.getResponseHeaders();
            if (responseHeaders != null && responseHeaders.containsKey(AUTHENTICATE_HEADER))
            {
                // HttpUrlConnection sends a list of header values for same key
                // if exists
                List<String> headers = responseHeaders.get(AUTHENTICATE_HEADER);

                return createFromResponseAuthenticateHeader(headers.get(0));
            }

            throw new IllegalArgumentException(ErrorMessages.AUTH_HEADER_MISSING);
        }
        throw new IllegalArgumentException(ErrorMessages.AUTH_HEADER_WRONG_STATUS);
    }

    public static AuthenticationParameters createFromResponseAuthenticateHeader(
            String authenticateHeader) {

        AuthenticationParameters authParams = null;
        if (StringExtensions.IsNullOrBlank(authenticateHeader))
        {
            throw new IllegalArgumentException(ErrorMessages.AUTH_HEADER_MISSING);
        }
        else
        {
            // TODO what should be the correct locale here?
            authenticateHeader = authenticateHeader.trim().toLowerCase(Locale.US);

            if (!authenticateHeader.startsWith(BEARER))
            {
                throw new IllegalArgumentException(ErrorMessages.AUTH_HEADER_INVALID_FORMAT);
            }
            else
            {
                HashMap<String, String> headerItems = HashMapExtensions.URLFormDecodeData(
                        authenticateHeader, ",");
                if (headerItems != null && !headerItems.isEmpty())
                {
                    authParams = new AuthenticationParameters(
                            headerItems.get(AUTHORITY_KEY), headerItems.get(RESOURCE_KEY));
                }
                else
                {
                    throw new IllegalArgumentException(ErrorMessages.AUTH_HEADER_INVALID_FORMAT);
                }
            }
        }

        return authParams;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(String mAuthority) {
        this.mAuthority = mAuthority;
    }

    public String getResource() {
        return mResource;
    }

    public void setResource(String mResource) {
        this.mResource = mResource;
    }
}
