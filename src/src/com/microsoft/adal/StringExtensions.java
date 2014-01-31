/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

final class StringExtensions {
    /** The Constant ENCODING_UTF8. */
    public static final String ENCODING_UTF8 = "UTF_8";

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
}
