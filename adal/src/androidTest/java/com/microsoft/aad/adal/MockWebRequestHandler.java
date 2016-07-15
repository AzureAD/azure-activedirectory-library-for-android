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
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import junit.framework.Assert;

/**
 * handler to return mock responses
 */
class MockWebRequestHandler implements IWebRequestHandler {

    private URL mRequestUrl;

    private String mRequestContent;

    private UUID mCorrelationId;

    private Map<String, String> mRequestHeaders;

    private HttpWebResponse mReturnResponse;

    private String mReturnException;

    @Override
    public HttpWebResponse sendGet(URL url, Map<String, String> headers) throws IOException {
        mRequestUrl = url;
        mRequestHeaders = headers;
        if (mReturnException != null) {
            throw new IllegalArgumentException(mReturnException);
        }

        return mReturnResponse;
    }

    @Override
    public HttpWebResponse sendPost(URL url, Map<String, String> headers, byte[] content,
            String contentType) throws IOException {
        mRequestUrl = url;
        mRequestHeaders = headers;
        if (content != null) {
            try {
                mRequestContent = new String(content, "UTF-8");
            } catch (final IOException e) {
                Assert.fail("IOException");
            }
        }

        if (mReturnException != null) {
            throw new IllegalArgumentException(mReturnException);
        }

        return mReturnResponse;
    }

    public URL getRequestUrl() {
        return mRequestUrl;
    }

    public Map<String, String> getRequestHeaders() {
        return mRequestHeaders;
    }

    public void setReturnResponse(HttpWebResponse returnResponse) {
        this.mReturnResponse = returnResponse;
    }

    public void setReturnException(String exception) {
        this.mReturnException = exception;
    }

    public String getRequestContent() {
        return mRequestContent;
    }

    @Override
    public void setRequestCorrelationId(UUID correlationId) {
        mCorrelationId = correlationId;
    }
}
