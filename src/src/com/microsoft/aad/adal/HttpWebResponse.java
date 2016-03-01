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
