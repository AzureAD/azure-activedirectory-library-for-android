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

import java.io.IOException;
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
         * @param param     {@link AuthenticationParameters}
         */
        void onCompleted(Exception exception, AuthenticationParameters param);
    }

    /**
     * ADAL will make the call to get authority and resource info.
     *
     * @param context     {@link Context}
     * @param resourceUrl Url for resource to query for 401 response.
     * @param callback    {@link AuthenticationParamCallback}
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

    /**
     * ADAL will parse the header response to get the authority and the resource
     * info.
     *
     * @param authenticateHeader Header to check authority and resource.
     * @return {@link AuthenticationParameters}
     * @throws {@link ResourceAuthenticationChallengeException}
     */
    public static AuthenticationParameters createFromResponseAuthenticateHeader(final String authenticateHeader)
            throws ResourceAuthenticationChallengeException {
        final String methodName = ":createFromResponseAuthenticateHeader";
        if (StringExtensions.isNullOrBlank(authenticateHeader)) {
            Logger.w(TAG + methodName, "authenticateHeader was null/empty.");
            throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING);
        }

        Logger.v(TAG + methodName, "Parsing challenges - BEGIN");
        final List<Challenge> challenges = Challenge.parseChallenges(authenticateHeader);
        Logger.v(TAG + methodName, "Parsing challenge - END");

        // Grab the Bearer challenge
        Challenge bearerChallenge = null;

        Logger.v(TAG + methodName, "Looking for Bearer challenge.");
        for (final Challenge challenge : challenges) {
            if (BEARER.equalsIgnoreCase(challenge.getScheme())) {
                Logger.v(TAG + methodName, "Found Bearer challenge.");
                bearerChallenge = challenge;
                break;
            }
        }

        if (null != bearerChallenge) {
            final Map<String, String> challengeParams = bearerChallenge.getParameters();
            String authority = challengeParams.get(AUTHORITY_KEY);
            String resource = challengeParams.get(RESOURCE_KEY);
            Logger.i(TAG + methodName, "Bearer authority", "[" + authority + "]");
            Logger.i(TAG + methodName, "Bearer resource", "[" + resource + "]");

            if (StringExtensions.isNullOrBlank(authority)) {
                Logger.w(TAG + methodName, "Null/empty authority.");
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING_AUTHORITY);
            }

            // Remove wrapping quotes (if present)
            Logger.v(TAG + methodName, "Parsing leading/trailing \"\"'s (authority)");
            authority = authority.replaceAll("^\"|\"$", "");
            Logger.i(TAG + methodName, "Sanitized authority value", "[" + authority + "]");

            if (StringExtensions.isNullOrBlank(authority)) {
                Logger.w(TAG + methodName, "Sanitized authority is null/empty.");
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING_AUTHORITY);
            }

            if (!StringExtensions.isNullOrBlank(resource)) {
                Logger.v(TAG + methodName, "Parsing leading/trailing \"\"'s (resource)");
                resource = resource.replaceAll("^\"|\"$", "");
                Logger.i(TAG + methodName, "Sanitized resource value", "[" + authority + "]");
            }

            return new AuthenticationParameters(authority, resource);
        }

        Logger.w(TAG + methodName, "Did not locate Bearer challenge.");
        throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
    }

    /**
     * An authentication challenge.
     * Format follows <a href="https://tools.ietf.org/html/rfc7235#section-4.1">RFC-7235</a>.
     *
     * @see <a href-"https://tools.ietf.org/html/rfc7617">RFC-7617</a>
     * @see <a href="https://tools.ietf.org/html/rfc6750">RFC-6750</a>
     */
    private static class Challenge {

        /**
         * Regex sequence intended to be prefixed with another value. Whichever value precedes it
         * will be parsed/grouped-out, assuming it is not bounded by a par of double-quotes ("").
         */
        private static final String REGEX_UNQUOTED_LOOKAHEAD = "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

        /**
         * Regex sequence to parse unquoted equals (=) signs.
         */
        private static final String REGEX_SPLIT_UNQUOTED_EQUALS = "=" + REGEX_UNQUOTED_LOOKAHEAD;

        /**
         * Regex sequence to parse unquoted commas (,).
         */
        private static final String REGEX_SPLIT_UNQUOTED_COMMA = "," + REGEX_UNQUOTED_LOOKAHEAD;

        /**
         * Regex sequence to parse schemes from WWW-Authenticate header values.
         */
        private static final String REGEX_STRING_TOKEN_WITH_SCHEME = "^([^\\s|^=]+)[\\s|\\t]+([^=]*=[^=]*)+$";

        /**
         * Comma+space suffix used to preserve formatting during parsing.
         */
        private static final String SUFFIX_COMMA = ", ";

        /**
         * The authentication scheme of this challenge (ex. Basic, Bearer).
         */
        private String mScheme;

        /**
         * The parameters of this scheme (ex. realm, authoritization_uri, scope).
         */
        private Map<String, String> mParameters;

        /**
         * Constructs a new Challenge.
         *
         * @param scheme The scheme used by this challenge.
         * @param params The params which accompany this challenge.
         */
        private Challenge(final String scheme, final Map<String, String> params) {
            mScheme = scheme;
            mParameters = params;
        }

        /**
         * Gets the scheme.
         *
         * @return The scheme to get.
         */
        public String getScheme() {
            return mScheme;
        }

        /**
         * Gets the parameters.
         *
         * @return The parameters to get.
         */
        public Map<String, String> getParameters() {
            return mParameters;
        }

        /**
         * Parses a single challenge String, typically the value field of a WWW-Authenticate header.
         *
         * @param challenge The challenge String to parse.
         * @return The Challenge object derived from challenge.
         * @throws ResourceAuthenticationChallengeException If a parsing error is encountered or
         *                                                  the String is malformed.
         */
        static Challenge parseChallenge(final String challenge) throws ResourceAuthenticationChallengeException {
            final String methodName = ":parseChallenge";
            if (StringExtensions.isNullOrBlank(challenge)) {
                Logger.w(TAG + methodName, "Cannot parse null/empty challenge.");
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING);
            }
            final String scheme = parseScheme(challenge);
            Logger.i(TAG + methodName, "Parsing scheme", "Scheme value [" + scheme + "]");
            Logger.i(TAG + methodName, "Removing scheme from source challenge", "[" + challenge + "]");
            Logger.v(TAG + methodName, "Parsing challenge substr. Total length: " + challenge.length() + " Scheme index: " + scheme.length() + 1);
            final String challengeSansScheme = challenge.substring(scheme.length() + 1);
            final Map<String, String> params = parseParams(challengeSansScheme);
            return new Challenge(scheme, params);
        }

        /**
         * Parses the scheme of a challenge String.
         *
         * @param challenge The challenge String to parse.
         * @return The scheme portion of the challenge String.
         * @throws ResourceAuthenticationChallengeException If a parsing error is encountered or
         *                                                  the String is malformed.
         */
        private static String parseScheme(String challenge) throws ResourceAuthenticationChallengeException {
            final String methodName = ":parseScheme";

            if (StringExtensions.isNullOrBlank(challenge)) {
                Logger.w(TAG + methodName, "Cannot parse an empty/blank challenge");
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING);
            }

            final int indexOfFirstSpace = challenge.indexOf(' ');
            final int indexOfFirstTab = challenge.indexOf('\t');
            // We want to grab the lesser of these values so long as they're > -1...
            if (indexOfFirstSpace < 0 && indexOfFirstTab < 0) {
                Logger.w(TAG + methodName, "Couldn't locate space/tab char - returning input String");
                return challenge;
            }

            Logger.v(TAG + methodName, "Parsing scheme with indices: indexOfFirstSpace[" + indexOfFirstSpace + "] indexOfFirstTab[" + indexOfFirstTab + "]");

            // If there is a space and it occurs before the first tab character.
            if (indexOfFirstSpace > -1 && (indexOfFirstSpace < indexOfFirstTab || indexOfFirstTab < 0)) {
                return challenge.substring(0, indexOfFirstSpace);
            }

            // If there is a tab character and it occurs before the first space character.
            if (indexOfFirstTab > -1 && (indexOfFirstTab < indexOfFirstSpace || indexOfFirstSpace < 0)) {
                return challenge.substring(0, indexOfFirstTab);
            }

            Logger.w(TAG + methodName, "Unexpected/malformed/missing scheme.");
            throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
        }

        /**
         * Parses the parameters of a challenge String which has had its scheme removed
         * (from its prefix).
         *
         * @param challengeSansScheme The challenge String, minus the scheme.
         * @return A Map of the keys/values in the parsed parameters.
         * @throws ResourceAuthenticationChallengeException If a parsing error is encountered or
         *                                                  the String is malformed.
         */
        private static Map<String, String> parseParams(String challengeSansScheme) throws ResourceAuthenticationChallengeException {
            final String methodName = ":parseParams";
            if (StringExtensions.isNullOrBlank(challengeSansScheme)) {
                Logger.w(TAG + methodName, "ChallengeSansScheme was null/empty");
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
            }

            // Split on unquoted commas
            final Map<String, String> params = new HashMap<>();
            Logger.i(TAG + methodName, "Splitting on unquoted commas...", "in-value [" + challengeSansScheme + "]");
            final String[] splitOnUnquotedCommas = challengeSansScheme.split(REGEX_SPLIT_UNQUOTED_COMMA, -1);
            Logger.i(TAG + methodName, "Splitting on unquoted commas...", "out-value [" + Arrays.toString(splitOnUnquotedCommas) + "]");
            for (final String paramSet : splitOnUnquotedCommas) {
                // Split keys/values by the '='
                Logger.i(TAG + methodName, "Splitting on unquoted equals...", "in-value [" + paramSet + "]");
                final String[] splitOnUnquotedEquals = paramSet.split(REGEX_SPLIT_UNQUOTED_EQUALS, -1);
                Logger.i(TAG + methodName, "Splitting on unquoted equals...", "out-value [" + Arrays.toString(splitOnUnquotedEquals) + "]");

                // We should now have a left-side and right-side
                if (splitOnUnquotedEquals.length != 2) {
                    Logger.w(TAG + methodName, "Splitting on equals yielded mismatched key/value.");
                    throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
                }

                // Create the keys/values, trimming off any excess whitespace
                Logger.v(TAG + methodName, "Trimming split-string whitespace");
                final String key = splitOnUnquotedEquals[0].trim();
                final String value = splitOnUnquotedEquals[1].trim();
                Logger.i(TAG + methodName, "", "key[" + key + "]");
                Logger.i(TAG + methodName, "", "value[" + value + "]");

                // if there is already a mapping for this key, we've seen this value before
                // and should log a warning that this header looks fishy....
                if (params.containsKey(key)) {
                    Logger.w(TAG,
                            "Key/value pair list contains redundant key. ",
                            "Redundant key: " + key,
                            ADALError.DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS);
                }

                // Add the key/value to the Map
                Logger.i(TAG + methodName, "", "put(" + key + ", " + value + ")");
                params.put(key, value);
            }

            if (params.isEmpty()) { // To match the existing expected behavior, an Exception is thrown.
                Logger.w(TAG + methodName, "Parsed params were empty.");
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
            }

            return params;
        }

        /**
         * Parses multiple challenges in a single String, typically the value field of a WWW-Authenticate header.
         *
         * @param strChallenges The challenge String to parse.
         * @return The Challenge object derived from challenge.
         * @throws ResourceAuthenticationChallengeException If a parsing error is encountered or
         *                                                  the String is malformed.
         */
        static List<Challenge> parseChallenges(final String strChallenges) throws ResourceAuthenticationChallengeException {
            final String methodName = ":parseChallenges";

            if (StringExtensions.isNullOrBlank(strChallenges)) {
                Logger.w(TAG + methodName, "Cannot parse empty/blank challenges.");
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING);
            }

            // Initialize and out-List for our result.
            final List<Challenge> challenges = new ArrayList<>();

            try { // Separate the challenges.
                Logger.i(TAG + methodName, "Separating challenges...", "input[" + strChallenges + "]");
                List<String> strChallengesList = separateChallenges(strChallenges);

                // Add each to the out-List
                for (final String challenge : strChallengesList) {
                    challenges.add(parseChallenge(challenge));
                }
            } catch (ResourceAuthenticationChallengeException e) {
                Logger.w(TAG + methodName, "Encountered error during parsing...", e.getMessage(), null);
                throw e;
            } catch (Exception e) {
                Logger.w(TAG + methodName, "Encountered error during parsing...", e.getMessage(), null);
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
            }

            return challenges;
        }

        /**
         * For multiple challenges in a WWW-Authenticate header value, separate them into multiple
         * Strings for parsing.
         *
         * @param challenges The challenge values to parse.
         * @return A List of separated challenges.
         */
        private static List<String> separateChallenges(final String challenges) throws ResourceAuthenticationChallengeException {
            final String methodName = ":separateChallenges";

            if (StringExtensions.isNullOrBlank(challenges)) {
                Logger.w(TAG + methodName, "Input String was null");
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
            }

            // Split the supplied String on those commas which are not constrained by quotes
            Logger.i(TAG + methodName, "Splitting input String on unquoted commas", "input[" + challenges + "]");
            String[] splitOnUnquotedCommas = challenges.split(REGEX_SPLIT_UNQUOTED_COMMA, -1);
            Logger.i(TAG + methodName, "Splitting input String on unquoted commas", "output[" + Arrays.toString(splitOnUnquotedCommas) + "]");
            sanitizeWhitespace(splitOnUnquotedCommas);
            List<String> tokensContainingScheme = extractTokensContainingScheme(splitOnUnquotedCommas);

            // init an array to store the out-values
            String[] outStrings = new String[tokensContainingScheme.size()];
            for (int ii = 0; ii < outStrings.length; ii++) {
                outStrings[ii] = "";
            }

            writeParsedChallenges(splitOnUnquotedCommas, tokensContainingScheme, outStrings);

            // Remove the suffix comma from the last element of each list...
            sanitizeParsedSuffixes(outStrings);

            // Collapse the results to a single list...
            return Arrays.asList(outStrings);
        }

        /**
         * Writes the parsed challenges to an output array.
         *
         * @param splitOnUnquotedCommas  The challenge String, split on unquoted commas.
         * @param tokensContainingScheme String tokens in the target challenge which contain a
         *                               scheme element.
         * @param outStrings             The output array, modified in-place.
         */
        private static void writeParsedChallenges(String[] splitOnUnquotedCommas, List<String> tokensContainingScheme, String[] outStrings) {
            int ii = -1; // Out-value index
            for (final String token : splitOnUnquotedCommas) {
                if (tokensContainingScheme.contains(token)) {
                    // this is the start of a challenge...
                    outStrings[++ii] = token + SUFFIX_COMMA;
                } else {
                    outStrings[ii] += token + SUFFIX_COMMA;
                }
            }
        }

        /**
         * Removes trailing (suffixed) comma values from Strings in the supplied array.
         *
         * @param outStrings The String array to sanitize.
         */
        private static void sanitizeParsedSuffixes(String[] outStrings) {
            for (int jj = 0; jj < outStrings.length; jj++) {
                if (outStrings[jj].endsWith(SUFFIX_COMMA)) {
                    outStrings[jj] = outStrings[jj].substring(0, outStrings[jj].length() - 2);
                }
            }
        }

        /**
         * Extract a List of String tokens containing scheme elements from the supplied array.
         *
         * @param strArry The String array to inspect.
         * @return A List of scheme-containing String tokens.
         */
        private static List<String> extractTokensContainingScheme(final String[] strArry) throws ResourceAuthenticationChallengeException {
            final List<String> tokensContainingScheme = new ArrayList<>();

            for (final String token : strArry) {
                if (containsScheme(token)) {
                    tokensContainingScheme.add(token);
                }
            }

            return tokensContainingScheme;
        }

        /**
         * Heuristically check if a given String contains a scheme element.
         *
         * @param token The String token to inspect.
         * @return True, if it contains a scheme. False otherwise.
         */
        private static boolean containsScheme(final String token) throws ResourceAuthenticationChallengeException {
            final String methodName = ":containsScheme";

            if (StringExtensions.isNullOrBlank(token)) {
                Logger.w(TAG + methodName, "Null/blank potential scheme token");
                throw new ResourceAuthenticationChallengeException(AUTH_HEADER_INVALID_FORMAT);
            }

            Logger.i(TAG + methodName, "Testing token contains scheme", "input[" + token + "]");
            final Pattern startWithScheme = Pattern.compile(REGEX_STRING_TOKEN_WITH_SCHEME);
            final Matcher matcher = startWithScheme.matcher(token);
            final boolean match = matcher.matches();
            Logger.i(TAG + methodName, "Testing String contains scheme", "Matches? [" + match + "]");
            return match;
        }

        /**
         * Trim whitespace from each array element.
         *
         * @param strArray The target String[].
         */
        private static void sanitizeWhitespace(String[] strArray) {
            final String methodName = ":sanitizeWhitespace";
            Logger.v(TAG + methodName, "Sanitizing whitespace");
            for (int ii = 0; ii < strArray.length; ii++) {
                strArray[ii] = strArray[ii].trim();
            }
        }
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
                    return createFromResponseAuthenticateHeader(headers.get(0));
                }
            }

            throw new ResourceAuthenticationChallengeException(AUTH_HEADER_MISSING);
        }
        throw new ResourceAuthenticationChallengeException(AUTH_HEADER_WRONG_STATUS);
    }
}
