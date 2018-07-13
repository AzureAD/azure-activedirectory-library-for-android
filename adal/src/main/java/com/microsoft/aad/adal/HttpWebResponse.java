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

import java.util.List;
import java.util.Map;

/**
 * Web response to keep status, response body, headers and related exceptions.
 */
public class HttpWebResponse {
    private final int mStatusCode;
    private final String mResponseBody;
    private final Map<String, List<String>> mResponseHeaders;

    /**
     * Constructor for {@link HttpWebResponse}.
     * @param statusCode Status code returned for the http call.
     * @param responseBody Response body returned from the http network call.
     * @param responseHeaders Response header for the network call.
     */
    public HttpWebResponse(int statusCode, String responseBody, Map<String, List<String>> responseHeaders) {
        mStatusCode = statusCode;
        mResponseBody = responseBody;
        mResponseHeaders = responseHeaders;
    }

    /**
     * @return The status code for the network call.
     */
    public int getStatusCode() {
        return mStatusCode;
    }

    /**
     * @return The response headers for the network call.
     */
    public Map<String, List<String>> getResponseHeaders() {
        return mResponseHeaders;
    }

    /**
     * @return The response body for the network call.
     */
    public String getBody() {
        return mResponseBody;
    }
}