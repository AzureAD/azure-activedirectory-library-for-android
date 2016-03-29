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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Web response to keep status, response body, headers and related exceptions.
 */
public class HttpWebResponse {
    private int mStatusCode;
    private byte[] mResponseBody;
    private Map<String, List<String>> mResponseHeaders;
    private IOException mResponseException = null;

    public HttpWebResponse() {
        mStatusCode = HttpURLConnection.HTTP_OK;
        mResponseBody = null;
    }

    public HttpWebResponse(int statusCode, byte[] responseBody,
            Map<String, List<String>> responseHeaders) {
        mStatusCode = statusCode;
        mResponseBody = responseBody;
        mResponseHeaders = responseHeaders;
    }

    public IOException getResponseException() {
        return mResponseException;
    }

    public void setResponseException(IOException responseException) {
        this.mResponseException = responseException;
    }

    HttpWebResponse(int statusCode) {
        mStatusCode = statusCode;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public void setStatusCode(int status) {
        mStatusCode = status;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return mResponseHeaders;
    }

    public void setResponseHeaders(Map<String, List<String>> headers) {
        mResponseHeaders = headers;
    }

    public byte[] getBody() {
        return mResponseBody;
    }

    public void setBody(byte[] body) {
        mResponseBody = body;
    }
}
