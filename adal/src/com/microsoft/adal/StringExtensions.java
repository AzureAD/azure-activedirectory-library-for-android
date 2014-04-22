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

package com.microsoft.adal;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Base64;

final class StringExtensions {
    /** The Constant ENCODING_UTF8. */
    public static final String ENCODING_UTF8 = "UTF_8";

    private static final String TAG = "StringExtensions";

    private static final String TOKEN_HASH_ALGORITHM = "SHA256";

    /**
     * checks if string is null or empty
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
     * replace + to space and decode
     * 
     * @param source
     * @return
     * @throws UnsupportedEncodingException
     */
    static final String URLFormDecode(String source) throws UnsupportedEncodingException {

        // Decode everything else
        return URLDecoder.decode(source, ENCODING_UTF8);
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
}
