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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import android.net.Uri;
import android.util.Base64;

final class StringExtensions {
    /** The Constant ENCODING_UTF8. */
    public static final String ENCODING_UTF8 = "UTF_8";

    private static final String TAG = "StringExtensions";

    private static final String TOKEN_HASH_ALGORITHM = "SHA256";

    private StringExtensions() {
        // Intentionally left blank
    }
    /**
     * checks if string is null or empty.
     * 
     * @param param String to check for null or blank
     * @return boolean if the string was null or blank
     */
    static boolean isNullOrBlank(String param) {
        return param == null || param.trim().length() == 0; //NOPMD
    }

    public static String createHash(String msg) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        if (!StringExtensions.isNullOrBlank(msg)) {
            MessageDigest digester = MessageDigest.getInstance(TOKEN_HASH_ALGORITHM);
            final byte[] msgInBytes = msg.getBytes(AuthenticationConstants.ENCODING_UTF8);
            return new String(Base64.encode(digester.digest(msgInBytes), Base64.NO_WRAP),
                    AuthenticationConstants.ENCODING_UTF8);
        }
        return msg;
    }

    /**
     * encode string with url form encoding. Space will be +
     * 
     * @param source the string to encode
     * @return the decoded
     * @throws UnsupportedEncodingException
     */
    static String urlFormEncode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, ENCODING_UTF8);
    }

    /**
     * replace + to space and decode.
     * 
     * @param source the string to decode
     * @return the encoded string
     * @throws UnsupportedEncodingException
     */
    static String urlFormDecode(String source) throws UnsupportedEncodingException {

        // Decode everything else
        return URLDecoder.decode(source, ENCODING_UTF8);
    }

    static String encodeBase64URLSafeString(final byte[] bytes)
            throws UnsupportedEncodingException {
        return new String(
                Base64.encode(bytes, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                AuthenticationConstants.ENCODING_UTF8);
    }

    /**
     * create url from given endpoint. return null if format is not right.
     * 
     * @param endpoint url as a string
     * @return URL object for this string
     */
    static URL getUrl(String endpoint) {
        URL authority = null;
        try {
            authority = new URL(endpoint);
        } catch (MalformedURLException e1) {
            Logger.e(TAG, e1.getMessage(), "", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, e1);
        }

        return authority;
    }

    static HashMap<String, String> getUrlParameters(String finalUrl) {
        Uri response = Uri.parse(finalUrl);
        String fragment = response.getFragment();
        HashMap<String, String> parameters = HashMapExtensions.urlFormDecode(fragment);

        if (parameters == null || parameters.isEmpty()) {
            String queryParameters = response.getEncodedQuery();
            parameters = HashMapExtensions.urlFormDecode(queryParameters);
        }
        return parameters;
    }

    static List<String> getStringTokens(final String items, final String delimeter) {
        final StringTokenizer st = new StringTokenizer(items, delimeter);
        final List<String> itemList = new ArrayList<>();
        if (st.hasMoreTokens()) {
            while (st.hasMoreTokens()) {
                String name = st.nextToken();
                if (!StringExtensions.isNullOrBlank(name)) {
                    itemList.add(name);
                }
            }
        }
        return itemList;
    }
    
    static ArrayList<String> splitWithQuotes(String input, char delimiter) {
        final ArrayList<String> items = new ArrayList<>();

        int startIndex = 0;
        boolean insideString = false;
        String item;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == delimiter && !insideString) {
                item = input.substring(startIndex, i);
                if (!StringExtensions.isNullOrBlank(item.trim())) {
                    items.add(item);
                }

                startIndex = i + 1;
            } else if (input.charAt(i) == '"') {
                insideString = !insideString;
            }
        }

        item = input.substring(startIndex);
        if (!StringExtensions.isNullOrBlank(item.trim())) {
            items.add(item);
        }

        return items;
    }

    static String removeQuoteInHeaderValue(String value) {
        if (!StringExtensions.isNullOrBlank(value)) {
            return value.replace("\"", "");
        }
        return null;
    }

    /**
     * Checks if header value has this prefix. Prefix + whitespace is
     * acceptable.
     * 
     * @param value String to check
     * @param prefix prefix to check the above string
     * @return boolean true if the string starts with prefix and has some body after it.
     */
    static boolean hasPrefixInHeader(final String value, final String prefix) {
        return value.startsWith(prefix) && value.length() > prefix.length() + 2
                && Character.isWhitespace(value.charAt(prefix.length()));
    }
}