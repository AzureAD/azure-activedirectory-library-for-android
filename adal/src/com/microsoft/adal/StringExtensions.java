/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import android.net.Uri;

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
        // Encode everything except spaces
        String target = URLEncoder.encode(source, ENCODING_UTF8);

        // Encode spaces to +
        return target.replace(' ', '+');
    }

    /**
     * replace + to space and decode
     * 
     * @param source
     * @return
     * @throws UnsupportedEncodingException
     */
    static final String URLFormDecode(String source) throws UnsupportedEncodingException {
        // Decode + to spaces
        String target = source.replace('+', ' ');

        // Decode everything else
        return URLDecoder.decode(target, ENCODING_UTF8);
    }
}
