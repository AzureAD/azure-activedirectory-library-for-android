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

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        Logger.v(TAG, "createFromResourceUrl");
        final Handler handler = new Handler(context.getMainLooper());

        sThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final Map<String, String> headers = new HashMap<>();
                headers.put(WebRequestHandler.HEADER_ACCEPT, WebRequestHandler.HEADER_ACCEPT_JSON);

                final HttpWebResponse webResponse;
                try {
                    webResponse = sWebRequest.sendGet(resourceUrl, headers);
                    try {
                        onCompleted(null, parseResponse(webResponse));
                    } catch (ResourceAuthenticationChallengeException exc) {
                        onCompleted(exc, null);
                    }
                } catch (IOException e) {
                    onCompleted(e, null);
                }
            }

            void onCompleted(final Exception exception, final AuthenticationParameters param) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onCompleted(exception, param);
                    }
                });
            }
        });
    }

    public static AuthenticationParameters createFromResponseAuthenticateHeader(final String headerValue)
            throws ResourceAuthenticationChallengeException {
        if (StringExtensions.isNullOrBlank(headerValue)) {
            throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING);
        }

        //testing...
        final List<Challenge> challenges = Challenge.parseChallenges(headerValue);

        // Grab the Bearer challenge
        Challenge bearerChallenge = null;

        for (final Challenge challenge : challenges) {
            if (challenge.getScheme().equalsIgnoreCase(BEARER)) {
                bearerChallenge = challenge;
                break;
            }
        }

        if (null != bearerChallenge) {
            final Map<String, String> challengeParams = bearerChallenge.getParameters();
            String authority = challengeParams.get(AUTHORITY_KEY);
            String resource = challengeParams.get(RESOURCE_KEY);

            if (StringExtensions.isNullOrBlank(authority)) {
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING_AUTHORITY);
            }

            // Remove wrapping quotes (if present)
            authority = authority.replaceAll("^\"|\"$", "");

            if (!StringExtensions.isNullOrBlank(resource)) {
                resource = resource.replaceAll("^\"|\"$", "");
            }

            return new AuthenticationParameters(authority, resource);
        }

        throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
    }

    private static class Challenge {

        public static final String TEST_PARSER = "TestParser";
        private String mScheme;

        private Map<String, String> mParameters;

        Challenge(final String scheme, final Map<String, String> params) {
            mScheme = scheme;
            mParameters = params;
        }

        public String getScheme() {
            return mScheme;
        }

        public Map<String, String> getParameters() {
            return mParameters;
        }

        static Challenge parseChallenge(final String challenge) throws ResourceAuthenticationChallengeException {
            final String scheme = parseScheme(challenge);
            Log.e(TEST_PARSER, "Parsing scheme: " + scheme);
            final String challengeSansScheme = challenge.substring(scheme.length() + 1);
            Log.e(TEST_PARSER, "Parsing schemeless params: " + challengeSansScheme);
            final Map<String, String> params = parseParams(challengeSansScheme);
            return new Challenge(scheme, params);
        }

        private static String parseScheme(String challenge) throws ResourceAuthenticationChallengeException {
            final int indexOfFirstSpace = challenge.indexOf(' ');
            final int indexOfFirstTab = challenge.indexOf('\t');
            // We want to grab the lesser of these values so long as they're > -1
            if (indexOfFirstSpace < 0 && indexOfFirstTab < 0) {
                return challenge;
            }

            if (indexOfFirstSpace > -1 && (indexOfFirstSpace < indexOfFirstTab || indexOfFirstTab < 0)) {
                return challenge.substring(0, indexOfFirstSpace);
            }

            if (indexOfFirstTab > -1 && (indexOfFirstTab < indexOfFirstSpace || indexOfFirstSpace < 0)) {
                return challenge.substring(0, indexOfFirstTab);
            }

            throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
        }

        private static Map<String, String> parseParams(String challengeSansScheme) throws ResourceAuthenticationChallengeException {
            // Split on unquoted commas
            final Map<String, String> params = new HashMap<>();
            final String[] splitOnUnquotedCommas = challengeSansScheme.split(REGEX_SPLIT_UNQUOTED_COMMA, -1);
            for (final String paramSet : splitOnUnquotedCommas) {
                // Split keys/values by the '='
                final String[] splitOnUnquotedEquals = paramSet.split(REGEX_SPLIT_UNQUOTED_EQUALS, -1);

                // We should now have a left-side and right-side
                if (splitOnUnquotedEquals.length != 2) {
                    // Is this really what you want?
                    throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
                    //continue; // If there's no key/value pair, skip this token
                }

                // Create the keys/values, trimming off any bogus whitespace
                final String key = splitOnUnquotedEquals[0].trim();
                final String value = splitOnUnquotedEquals[1].trim();

                // if there is already a mapping for this key, we've seen this value before
                // and should log a warning that this header looks fishy....
                if (params.containsKey(key)) {
                    Logger.w(TAG,
                            "Key/value pair list contains redundant key. ",
                            "Redundant key: " + key,
                            ADALError.DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS);
                }

                Log.e(TEST_PARSER, "put(" + key + ", " + value + ")");
                params.put(key, value);
            }
            return params;
        }

        static List<Challenge> parseChallenges(final String strChallenges) throws ResourceAuthenticationChallengeException {
            final List<Challenge> challenges = new ArrayList<>();
            List<String> strChallengesList = separateChallenges(strChallenges);
            //
            Log.e(TEST_PARSER, "Logging list contents");
            for (String s : strChallengesList){
                Log.e(TEST_PARSER, "\t" + s);
            }
            //
            for (final String challenge : strChallengesList) {
                challenges.add(parseChallenge(challenge));
            }

            return challenges;
        }

        private static final String REGEX_SPLIT_UNQUOTED_EQUALS = "=(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        private static final String REGEX_SPLIT_UNQUOTED_COMMA = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        private static final String REGEX_STRING_TOKEN_WITH_SCHEME = "^([^\\s|^=]+)[\\s|\\t]+([^=]*=[^=]*)+$";
        //private static final String REGEX_STRING_TOKEN_WITH_SCHEME = "^([^\\s|^=]+)\\s+([^=]*=[^=]*)+$";
        //private static final String REGEX_STRING_TOKEN_WITH_SCHEME = "^([^\\s]+)\\s+([^=]*=[^=]*)+$";
        private static final String SUFFIX_COMMA = ", ";

        private static List<String> separateChallenges(final String challenges) {
            // Split the supplied String on those commas which are not constrained by quotes
            String[] splitOnUnquotedCommas = challenges.split(REGEX_SPLIT_UNQUOTED_COMMA, -1);
            sanitizeWhitespace(splitOnUnquotedCommas);
            List<String> tokensContainingScheme = extractTokensContainingScheme(splitOnUnquotedCommas);

            // init an array to store the out-values
            String[] outStrings = new String[tokensContainingScheme.size()];
            for (int ii = 0; ii < outStrings.length; ii++) {
                outStrings[ii] = "";
            }

            int ii = -1;
            for (final String token : splitOnUnquotedCommas) {
                if (tokensContainingScheme.contains(token)) {
                    // this is the start of a challenge...
                    outStrings[++ii] = token + SUFFIX_COMMA;
                } else {
                    outStrings[ii] += token + SUFFIX_COMMA;
                }
            }

            // Remove the suffix comma from the last element of each list...
            for (int jj = 0; jj < outStrings.length; jj++) {
                if (outStrings[jj].endsWith(SUFFIX_COMMA)) {
                    outStrings[jj] = outStrings[jj].substring(0, outStrings[jj].length() - 2);
                }
            }

            // Collapse the results to a single list...
            return Arrays.asList(outStrings);
        }

        private static List<String> extractTokensContainingScheme(final String[] strArry) {
            final List<String> tokensContainingScheme = new ArrayList<>();

            for (final String token : strArry) {
                if (containsScheme(token)) {
                    tokensContainingScheme.add(token);
                }
            }

            return tokensContainingScheme;
        }

        private static boolean containsScheme(final String token) {
            final Pattern startWithScheme = Pattern.compile(REGEX_STRING_TOKEN_WITH_SCHEME);
            final Matcher matcher = startWithScheme.matcher(token);
            Log.e(TEST_PARSER, "Checking String:[" + token + "] containsScheme? " + matcher.matches());
            return matcher.matches();
        }

        private static void sanitizeWhitespace(String[] strArray) {
            for (int ii = 0; ii < strArray.length; ii++) {
                strArray[ii] = strArray[ii].trim();
            }
        }
    }

    /**
     * ADAL will parse the header response to get the authority and the resource
     * info.
     * @param authenticateHeader Header to check authority and resource.
     * @throws {@link ResourceAuthenticationChallengeException}
     * @return {@link AuthenticationParameters}
     */
    public static AuthenticationParameters createFromResponseAuthenticateHeader2(
            String authenticateHeader) throws ResourceAuthenticationChallengeException {
        final String methodName = ":createFromResponseAuthenticateHeader";
        final AuthenticationParameters authParams;

        if (StringExtensions.isNullOrBlank(authenticateHeader)) {
            throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING);
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
                Logger.v(TAG + methodName, "Parse the header response. ", "Values in here:" + headerSubFields, null);
                Matcher values = valuePattern.matcher(headerSubFields);
                final Map<String, String> headerItems = new HashMap<>();
                while (values.find()) {

                    // values.group(0) is matching string
                    if (!StringExtensions.isNullOrBlank(values.group(1))
                            && !StringExtensions.isNullOrBlank(values.group(2))) {
                        String key = values.group(1);
                        String value = values.group(2);

                        try {
                            key = StringExtensions.urlFormDecode(key);
                            value = StringExtensions.urlFormDecode(value);
                        } catch (UnsupportedEncodingException e) {
                            Logger.v(TAG + methodName, ADALError.ENCODING_IS_NOT_SUPPORTED.getDescription(), e.getMessage(), null);
                        }

                        key = key.trim();
                        value = StringExtensions.removeQuoteInHeaderValue(value.trim());

                        if (headerItems.containsKey(key)) {
                            Logger.w(TAG + methodName,
                                    "Key/value pair list contains redundant key. ",
                                    "Redundant key: " + key,
                                    ADALError.DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS);
                        }

                        headerItems.put(key, value);
                    } else {
                        // invalid format
                        throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
                    }
                }

                String authority = headerItems.get(AUTHORITY_KEY);
                if (!StringExtensions.isNullOrBlank(authority)) {
                    authParams = new AuthenticationParameters(
                            StringExtensions.removeQuoteInHeaderValue(authority),
                            StringExtensions.removeQuoteInHeaderValue(headerItems.get(RESOURCE_KEY)));
                } else {
                    // invalid format
                    throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING_AUTHORITY);
                }
            } else {
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
            }
        }

        return authParams;
    }

    private static AuthenticationParameters parseResponse(HttpWebResponse webResponse) throws ResourceAuthenticationChallengeException {
        // Depending on the service side implementation for this resource
        if (webResponse.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Map<String, List<String>> responseHeaders = webResponse.getResponseHeaders();
            if (responseHeaders != null && responseHeaders.containsKey(AUTHENTICATE_HEADER)) {
                // HttpUrlConnection sends a list of header values for same key
                // if exists
                List<String> headers = responseHeaders.get(AUTHENTICATE_HEADER);
                if (headers != null && headers.size() > 0) {
                    Log.e("TestParser", "Parsing..." + headers.get(0));
                    return createFromResponseAuthenticateHeader(headers.get(0));
                }
            }

            throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING);
        }
        throw new ResourceAuthenticationChallengeException(AUTH_HEADER_WRONG_STATUS);
    }
}
