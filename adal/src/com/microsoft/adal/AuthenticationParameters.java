
package com.microsoft.adal;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.util.Log;

/**
 * Matching to ADAL.NET It provides helper methods to get the
 * authorization_endpoint from resource address
 */
public class AuthenticationParameters {

    public final static String AUTH_HEADER_MISSING_AUTHORITY = "WWW-Authenticate header is missing authorization_uri.";

    public final static String AUTH_HEADER_INVALID_FORMAT = "Invalid authentication header format";

    public final static String AUTH_HEADER_MISSING = "WWW-Authenticate header was expected in the response";

    public final static String AUTH_HEADER_WRONG_STATUS = "Unauthorized http response (status code 401) was expected";

    public final static String AUTHENTICATE_HEADER = "WWW-Authenticate";

    public final static String BEARER = "bearer ";

    public final static String AUTHORITY_KEY = "authorization_uri";

    public final static String RESOURCE_KEY = "resource_id";

    private final static String TAG = "AuthenticationParameters";

    private String mAuthority;

    private String mResource;

    /**
     * get authority
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * get resource
     */
    public String getResource() {
        return mResource;
    }

    public AuthenticationParameters() {
    }

    AuthenticationParameters(String authority, String resource) {
        mAuthority = authority;
        mResource = resource;
    }

    public interface AuthenticationParamCallback {
        public void onCompleted(Exception exception, AuthenticationParameters param);
    }

    /**
     * ADAL will make the call to get authority and resource info
     */
    public static void createFromResourceUrl(URL resourceUrl, AuthenticationParamCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        Log.d(TAG, "createFromResourceUrl");

        HttpWebRequest webRequest = new HttpWebRequest(resourceUrl);
        webRequest.getRequestHeaders().put(WebRequestHandler.HEADER_ACCEPT,
                WebRequestHandler.HEADER_ACCEPT_JSON);
        final AuthenticationParamCallback externalCallback = callback;

        webRequest.sendAsyncGet(new HttpWebRequestCallback() {
            @Override
            public void onComplete(HttpWebResponse webResponse, Exception exception) {

                if (webResponse != null) {
                    try {
                        externalCallback.onCompleted(null, parseResponse(webResponse));
                    } catch (IllegalArgumentException exc) {
                        externalCallback.onCompleted(exc, null);
                    }
                } else
                    externalCallback.onCompleted(exception, null);
            }
        });
    }

    /**
     * ADAL will parse the header response to get the authority and the resource
     * info
     */
    public static AuthenticationParameters createFromResponseAuthenticateHeader(
            String authenticateHeader) {
        AuthenticationParameters authParams = null;

        if (StringExtensions.IsNullOrBlank(authenticateHeader)) {
            throw new IllegalArgumentException(AUTH_HEADER_MISSING);
        } else {

            authenticateHeader = authenticateHeader.trim().toLowerCase(Locale.US);

            // bearer should be first one
            if (!authenticateHeader.startsWith(BEARER)) {
                throw new IllegalArgumentException(AUTH_HEADER_INVALID_FORMAT);
            } else {
                authenticateHeader = authenticateHeader.substring(BEARER.length());
                ArrayList<String> queryPairs = splitWithQuotes(authenticateHeader, ',');
                HashMap<String, String> headerItems = new HashMap<String, String>();
                for (String queryPair : queryPairs) {
                    ArrayList<String> pair = splitWithQuotes(queryPair, '=');

                    if (pair.size() == 2 && !StringExtensions.IsNullOrBlank(pair.get(0))
                            && !StringExtensions.IsNullOrBlank(pair.get(1))) {
                        String key = pair.get(0);
                        String value = pair.get(1);

                        try {
                            key = StringExtensions.URLFormDecode(key);
                            value = StringExtensions.URLFormDecode(value);
                        } catch (UnsupportedEncodingException e) {
                            Log.d(TAG, e.getMessage());
                        }

                        key = key.trim();
                        value = removeQuoteInHeaderValue(value.trim());

                        if (headerItems.containsKey(key)) {
                            Logger.w(TAG, String.format(
                                    "Key/value pair list contains redundant key '{0}'.", key), "",
                                    ADALError.DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS);
                        }

                        headerItems.put(key, value);
                    } else {
                        // invalid format
                        throw new IllegalArgumentException(AUTH_HEADER_INVALID_FORMAT);
                    }
                }

                String authority = headerItems.get(AUTHORITY_KEY);
                if (!StringExtensions.IsNullOrBlank(authority)) {
                    authParams = new AuthenticationParameters(removeQuoteInHeaderValue(authority),
                            removeQuoteInHeaderValue(headerItems.get(RESOURCE_KEY)));
                } else {
                    // invalid format
                    throw new IllegalArgumentException(AUTH_HEADER_MISSING_AUTHORITY);
                }
            }
        }

        return authParams;
    }

    static ArrayList<String> splitWithQuotes(String input, char delimiter) {
        ArrayList<String> items = new ArrayList<String>();

        if (StringExtensions.IsNullOrBlank(input)) {
            return items;
        }

        int startIndex = 0;
        boolean insideString = false;
        String item;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == delimiter && !insideString) {
                item = input.substring(startIndex, i);
                if (!StringExtensions.IsNullOrBlank(item.trim())) {
                    items.add(item);
                }

                startIndex = i + 1;
            } else if (input.charAt(i) == '"') {
                insideString = !insideString;
            }
        }

        if (startIndex < input.length()) {
            item = input.substring(startIndex);
            if (!StringExtensions.IsNullOrBlank(item.trim())) {
                items.add(item);
            }
        }

        return items;
    }

    private static String removeQuoteInHeaderValue(String value) {
        if (!StringExtensions.IsNullOrBlank(value)) {
            return value.replace("\"", "");
        }
        return null;
    }

    private static AuthenticationParameters parseResponse(HttpWebResponse webResponse) {
        // Depending on the service side implementation for this resource
        if (webResponse.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Map<String, List<String>> responseHeaders = webResponse.getResponseHeaders();
            if (responseHeaders != null && responseHeaders.containsKey(AUTHENTICATE_HEADER)) {
                // HttpUrlConnection sends a list of header values for same key
                // if exists
                List<String> headers = responseHeaders.get(AUTHENTICATE_HEADER);
                if (headers != null && headers.size() > 0) {
                    return createFromResponseAuthenticateHeader(headers.get(0));
                }
            }

            throw new IllegalArgumentException(AUTH_HEADER_MISSING);
        }
        throw new IllegalArgumentException(AUTH_HEADER_WRONG_STATUS);
    }
}
