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

    /**
     * checks if string is null or empty.
     * 
     * @param param
     * @return
     */
    static boolean IsNullOrBlank(String param) {
        if (param == null || param.trim().length() == 0) {
            return true;
        }

        return false;
    }

    public static String createHash(String msg) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        if (!StringExtensions.IsNullOrBlank(msg)) {
            MessageDigest digester = MessageDigest.getInstance(TOKEN_HASH_ALGORITHM);
            final byte[] msgInBytes = msg.getBytes(AuthenticationConstants.ENCODING_UTF8);
            String hash = new String(Base64.encode(digester.digest(msgInBytes), Base64.NO_WRAP),
                    AuthenticationConstants.ENCODING_UTF8);
            return hash;
        }
        return msg;
    }

    /**
     * encode string with url form encoding. Space will be +
     * 
     * @param source
     * @return
     * @throws UnsupportedEncodingException
     */
    static final String URLFormEncode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, ENCODING_UTF8);
    }

    /**
     * replace + to space and decode.
     * 
     * @param source
     * @return
     * @throws UnsupportedEncodingException
     */
    static final String URLFormDecode(String source) throws UnsupportedEncodingException {

        // Decode everything else
        return URLDecoder.decode(source, ENCODING_UTF8);
    }

    static final String encodeBase64URLSafeString(final byte[] bytes)
            throws UnsupportedEncodingException {
        return new String(
                Base64.encode(bytes, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                AuthenticationConstants.ENCODING_UTF8);
    }

    /**
     * create url from given endpoint. return null if format is not right.
     * 
     * @param endpoint
     * @return
     * @throws MalformedURLException
     */
    static final URL getUrl(String endpoint) {
        URL authority = null;
        try {
            authority = new URL(endpoint);
        } catch (MalformedURLException e1) {
            Logger.e(TAG, e1.getMessage(), "", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, e1);
        }

        return authority;
    }

    static final HashMap<String, String> getUrlParameters(String finalUrl) {
        Uri response = Uri.parse(finalUrl);
        String fragment = response.getFragment();
        HashMap<String, String> parameters = HashMapExtensions.URLFormDecode(fragment);

        if (parameters == null || parameters.isEmpty()) {
            String queryParameters = response.getEncodedQuery();
            parameters = HashMapExtensions.URLFormDecode(queryParameters);
        }
        return parameters;
    }

    static final List<String> getStringTokens(final String items, final String delimeter) {
        StringTokenizer st = new StringTokenizer(items, delimeter);
        List<String> itemList = new ArrayList<String>();
        if (st.hasMoreTokens()) {
            while (st.hasMoreTokens()) {
                String name = st.nextToken();
                if (!StringExtensions.IsNullOrBlank(name)) {
                    itemList.add(name);
                }
            }
        }
        return itemList;
    }
    
    static ArrayList<String> splitWithQuotes(String input, char delimiter) {
        ArrayList<String> items = new ArrayList<String>();

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

        item = input.substring(startIndex);
        if (!StringExtensions.IsNullOrBlank(item.trim())) {
            items.add(item);
        }

        return items;
    }

    static String removeQuoteInHeaderValue(String value) {
        if (!StringExtensions.IsNullOrBlank(value)) {
            return value.replace("\"", "");
        }
        return null;
    }

    /**
     * Checks if header value has this prefix. Prefix + whitespace is
     * acceptable.
     * 
     * @param value
     * @param prefix
     * @return
     */
    static boolean hasPrefixInHeader(final String value, final String prefix) {
        return value.startsWith(prefix) && value.length() > prefix.length() + 2
                && Character.isWhitespace(value.charAt(prefix.length()));
    }
}