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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Handler;

/**
 * Matching to ADAL.NET It provides helper methods to get the
 * authorization_endpoint from resource address.
 */
public class AuthenticationParameters {

    /**
     * WWW-Authenticate header is missing authorization_uri.
     */
    public static final String AUTH_HEADER_MISSING_AUTHORITY = "WWW-Authenticate header is missing authorization_uri.";

    /**
     * Invalid authentication header format.
     */
    public static final String AUTH_HEADER_INVALID_FORMAT = "Invalid authentication header format";

    /**
     * WWW-Authenticate header was expected in the response.
     */
    public static final String AUTH_HEADER_MISSING = "WWW-Authenticate header was expected in the response";

    /**
     * Unauthorized http response (status code 401) was expected.
     */
    public static final String AUTH_HEADER_WRONG_STATUS = "Unauthorized http response (status code 401) was expected";

    /**
     * Constant Authenticate header: WWW-Authenticate.
     */
    public static final String AUTHENTICATE_HEADER = "WWW-Authenticate";

    /**
     * Constant Bearer.
     */
    public static final String BEARER = "bearer";

    /**
     * Constant Authority key.
     */
    public static final String AUTHORITY_KEY = "authorization_uri";

    /**
     * Constant Resource key.
     */
    public static final String RESOURCE_KEY = "resource_id";

    private static final String TAG = "AuthenticationParameters";

    private static final String REGEX = "^Bearer\\s+([^,\\s=\"]+?)=\"([^\"]*?)\"\\s*(?:,\\s*([^,\\s=\"]+?)=\"([^\"]*?)\"\\s*)*$";

    private static final String REGEX_VALUES = "\\s*([^,\\s=\"]+?)=\"([^\"]*?)\"";

    private String mAuthority;

    private String mResource;

    /**
     * Web request handler interface to test behaviors.
     */
    private static IWebRequestHandler sWebRequest = new WebRequestHandler();

    /**
     * Singled threaded Executor for async work.
     */
    private static ExecutorService sThreadExecutor = Executors.newSingleThreadExecutor();

    /**
     * get authority from the header.
     * 
     * @return Authority extracted from the header.
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * get resource from the header.
     * 
     * @return resource from the header.
     */
    public String getResource() {
        return mResource;
    }

    /**
     * Creates AuthenticationParameters.
     */
    public AuthenticationParameters() {
    }

    AuthenticationParameters(String authority, String resource) {
        mAuthority = authority;
        mResource = resource;
    }

    /**
     * Callback to use for async request.
     */
    public interface AuthenticationParamCallback {

        /**
         * @param exception {@link Exception}
         * @param param {@link AuthenticationParameters}
         */
        void onCompleted(Exception exception, AuthenticationParameters param);
    }

    /**
     * ADAL will make the call to get authority and resource info.
     * 
     * @param context {@link Context}
     * @param resourceUrl Url for resource to query for 401 response.
     * @param callback  {@link AuthenticationParamCallback}
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
     * info.
     * @param authenticateHeader Header to check authority and resource.
     * @return {@link AuthenticationParameters}
     */
    public static AuthenticationParameters createFromResponseAuthenticateHeader(
            String authenticateHeader) {
        AuthenticationParameters authParams = null;

        if (StringExtensions.IsNullOrBlank(authenticateHeader)) {
            throw new IllegalArgumentException(AUTH_HEADER_MISSING);
        } else {
            Pattern p = Pattern.compile(REGEX);
            Matcher m = p.matcher(authenticateHeader);

            // If the header is in the right format, REGEX_VALUES will extract
            // individual
            // name-value pairs. This regex is not as exclusive, so it relies on
            // the previous check to guarantee correctness:
            if (m.matches()) {

                // Get matching value pairs inside the header value
                Pattern valuePattern = Pattern.compile(REGEX_VALUES);
                String headerSubFields = authenticateHeader.substring(BEARER.length());
                Logger.v(TAG, "Values in here:" + headerSubFields);
                Matcher values = valuePattern.matcher(headerSubFields);
                HashMap<String, String> headerItems = new HashMap<String, String>();
                while (values.find()) {

                    // values.group(0) is matching string
                    if (!StringExtensions.IsNullOrBlank(values.group(1))
                            && !StringExtensions.IsNullOrBlank(values.group(2))) {
                        String key = values.group(1);
                        String value = values.group(2);

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
                    authParams = new AuthenticationParameters(
                            StringExtensions.removeQuoteInHeaderValue(authority),
                            StringExtensions.removeQuoteInHeaderValue(headerItems.get(RESOURCE_KEY)));
                } else {
                    // invalid format
                    throw new IllegalArgumentException(AUTH_HEADER_MISSING_AUTHORITY);
                }
            } else {
                throw new IllegalArgumentException(AUTH_HEADER_INVALID_FORMAT);
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
