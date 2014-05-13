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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.os.Handler;

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

    public final static String BEARER = "bearer";

    public final static String AUTHORITY_KEY = "authorization_uri";

    public final static String RESOURCE_KEY = "resource_id";

    private final static String TAG = "AuthenticationParameters";

    private String mAuthority;

    private String mResource;

    /**
     * Web request handler interface to test behaviors
     */
    private static IWebRequestHandler sWebRequest = new WebRequestHandler();

    /**
     * Singled threaded Executor for async work
     */
    private static ExecutorService sThreadExecutor = Executors.newSingleThreadExecutor();

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
    public static void createFromResourceUrl(Context context, final URL resourceUrl,
            final AuthenticationParamCallback callback) {

        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        Logger.d(TAG, "createFromResourceUrl");
        final Handler handler = new Handler(context.getMainLooper());

        sThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put(WebRequestHandler.HEADER_ACCEPT, WebRequestHandler.HEADER_ACCEPT_JSON);

                try {
                    HttpWebResponse webResponse = sWebRequest.sendGet(resourceUrl, headers);

                    if (webResponse != null) {
                        try {
                            onCompleted(null, parseResponse(webResponse));
                        } catch (IllegalArgumentException exc) {
                            onCompleted(exc, null);
                        }
                    }
                } catch (Exception exception) {
                    onCompleted(exception, null);
                }
            }

            void onCompleted(final Exception exception, final AuthenticationParameters param) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onCompleted(exception, param);
                        return;
                    }
                });
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
            if (!authenticateHeader.startsWith(BEARER)
                    || authenticateHeader.length() < BEARER.length() + 2
                    || !Character.isWhitespace(authenticateHeader.charAt(BEARER.length()))) {
                throw new IllegalArgumentException(AUTH_HEADER_INVALID_FORMAT);
            } else {
                authenticateHeader = authenticateHeader.substring(BEARER.length());
                ArrayList<String> queryPairs = StringExtensions.splitWithQuotes(authenticateHeader, ',');
                HashMap<String, String> headerItems = new HashMap<String, String>();
                for (String queryPair : queryPairs) {
                    ArrayList<String> pair = StringExtensions.splitWithQuotes(queryPair, '=');

                    if (pair.size() == 2 && !StringExtensions.IsNullOrBlank(pair.get(0))
                            && !StringExtensions.IsNullOrBlank(pair.get(1))) {
                        String key = pair.get(0);
                        String value = pair.get(1);

                        try {
                            key = StringExtensions.URLFormDecode(key);
                            value = StringExtensions.URLFormDecode(value);
                        } catch (UnsupportedEncodingException e) {
                            Logger.d(TAG, e.getMessage());
                        }

                        key = key.trim();
                        value = StringExtensions.removeQuoteInHeaderValue(value.trim());

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
                    authParams = new AuthenticationParameters(StringExtensions.removeQuoteInHeaderValue(authority),
                            StringExtensions.removeQuoteInHeaderValue(headerItems.get(RESOURCE_KEY)));
                } else {
                    // invalid format
                    throw new IllegalArgumentException(AUTH_HEADER_MISSING_AUTHORITY);
                }
            }
        }

        return authParams;
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
