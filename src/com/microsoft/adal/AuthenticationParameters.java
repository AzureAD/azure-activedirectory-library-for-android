
package com.microsoft.adal;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//TODO add Dotnet style exceptions
public class AuthenticationParameters {

    private final static String AUTHENTICATE_HEADER = "WWW-Authenticate";
    private final static String BEARER = "bearer";
    private final static String AUTHORITY_KEY = "authorization_uri";
    private final static String RESOURCE_KEY = "resource_id";

   
    
    
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
     * returns authenticationParam object in callback if webresponse returns
     * challange response and authorization endpoin
     */
    public static void createFromResourceUrl(URL resourceUrl,
            AuthenticationParamCallback callback) {

        if (callback == null)
        {
            return;
        }

        HttpWebRequest webRequest = new HttpWebRequest(resourceUrl);
        webRequest.getRequestHeaders().put("Accept", "application/json");
        final AuthenticationParamCallback externalCallback = callback;
        webRequest.sendAsync(
                new HttpWebRequestCallback() {
                    @Override
                    public void onComplete(Exception exception,
                            HttpWebResponse webResponse) {

                        if (exception == null)
                            parseResponse(webResponse, externalCallback);
                        else
                            externalCallback.onCompleted(exception, null);
                    }
                });
    }

    private static void parseResponse(HttpWebResponse webResponse,
            AuthenticationParamCallback callback)
    {
        // Depending on the service side implementation for this resource
        if (webResponse.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Map<String, List<String>> responseHeaders = webResponse.getResponseHeaders();
            if (responseHeaders.containsKey(AUTHENTICATE_HEADER))
            {
                // HttpUrlConnection sends a list of header values for same key
                // if exists
                List<String> headers = responseHeaders.get(AUTHENTICATE_HEADER);
                createFromResponseAuthenticateHeader(headers.get(0), callback);
            }
            else
            {
                // TODO add Dotnet style exceptions
                callback.onCompleted(new IllegalArgumentException(ErrorMessages.AUTH_HEADER_MISSING),
                        null);
            }
        }
        else
        {
            callback.onCompleted(new IllegalArgumentException(
                    ErrorMessages.AUTH_HEADER_WRONG_STATUS), null);
        }
    }

    public static void createFromResponseAuthenticateHeader(
            String authenticateHeader, AuthenticationParamCallback callback) {

        if (StringExtensions.IsNullOrBlank(authenticateHeader))
        {
            callback.onCompleted(new IllegalArgumentException(ErrorMessages.AUTH_HEADER_MISSING), null);
        }
        else
        {
            //TODO what should be the correct locale here?
            authenticateHeader = authenticateHeader.trim().toLowerCase(Locale.US);

            if (!authenticateHeader.startsWith(BEARER))
            {
                callback.onCompleted(new IllegalArgumentException(ErrorMessages.AUTH_HEADER_INVALID_FORMAT),
                        null);
            }
            else
            {
                HashMap<String, String> headerItems = HashMapExtensions.URLFormDecodeData(
                        authenticateHeader, ",");
                if (headerItems != null && !headerItems.isEmpty())
                {
                    AuthenticationParameters authParams = new AuthenticationParameters(
                            headerItems.get(AUTHORITY_KEY), headerItems.get(RESOURCE_KEY));
                    callback.onCompleted(null, authParams);
                }
                else
                {
                    callback.onCompleted(new IllegalArgumentException(
                            ErrorMessages.AUTH_HEADER_INVALID_FORMAT),
                            null);
                }
            }
        }
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
