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
import java.security.NoSuchAlgorithmException;

public final class StringExtensions {
    /**
     * The Constant ENCODING_UTF8.
     */
    public static final String ENCODING_UTF8 = "UTF_8";

    private StringExtensions() {
        // Intentionally left blank
    }

    /**
     * checks if string is null or empty.
     *
     * @param param String to check for null or blank
     * @return boolean if the string was null or blank
     */
    public static boolean isNullOrBlank(String param) {
        return com.microsoft.identity.common.adal.internal.util.StringExtensions.isNullOrBlank(
                param);
    }

    public static String createHash(String msg)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return com.microsoft.identity.common.adal.internal.util.StringExtensions.createHash(msg);
    }

    public static String encodeBase64URLSafeString(final byte[] bytes)
            throws UnsupportedEncodingException {
        return com.microsoft.identity.common.adal.internal.util.StringExtensions
                .encodeBase64URLSafeString(bytes);
    }
}
