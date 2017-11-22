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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestException extends AuthenticationException {
    static final long serialVersionUID = 1;

    /**
     * Gets the status code returned from http layer.
     * You can use this code for purposes such as implementing retry logic or error investigation.
     */
    private int mStatusCode;

    /**
     * Contains all http headers returned from the http error response.
     */
    private Map<String, List<String>> mHeaders;

    public int getStatusCode() {
        return mStatusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return mHeaders;
    }

    public HttpRequestException(HttpWebResponse response, String errorMessage, ADALError errorCode, Throwable throwable) {
        super(errorCode, errorMessage, throwable);

        if (response != null) {
            mStatusCode = response.getStatusCode();
            mHeaders = response.getResponseHeaders();
        }
    }

    public HttpRequestException(HttpWebResponse response, String errorMessage, ADALError errorCode) {
        super(errorCode, errorMessage);

        if (response != null) {
            mStatusCode = response.getStatusCode();
            mHeaders = response.getResponseHeaders();
        }
    }

    public HttpRequestException(HttpWebResponse response, ADALError errorCode) {
        super(errorCode);

        if (response != null) {
            mStatusCode = response.getStatusCode();
            mHeaders = response.getResponseHeaders();
        }
    }
}
