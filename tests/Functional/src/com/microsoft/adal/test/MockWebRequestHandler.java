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

package com.microsoft.adal.test;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.IWebRequestHandler;

/**
 * handler to return mock responses
 */
class MockWebRequestHandler implements IWebRequestHandler {

    private URL mRequestUrl;

    private String mRequestContent;

    private UUID mCorrelationId;

    private HashMap<String, String> mRequestHeaders;

    private HttpWebResponse mReturnResponse;

    private String mReturnException;

    @Override
    public HttpWebResponse sendGet(URL url, HashMap<String, String> headers)
            throws IllegalArgumentException, IOException {
        mRequestUrl = url;
        mRequestHeaders = headers;
        if (mReturnException != null) {
            throw new IllegalArgumentException(mReturnException);
        }

        return mReturnResponse;
    }

    @Override
    public HttpWebResponse sendPost(URL url, HashMap<String, String> headers, byte[] content,
            String contentType) throws IllegalArgumentException, IOException {
        mRequestUrl = url;
        mRequestHeaders = headers;
        if (content != null) {
            mRequestContent = new String(content, "UTF-8");
        }

        if (mReturnException != null) {
            throw new IllegalArgumentException(mReturnException);
        }

        return mReturnResponse;
    }

    public URL getRequestUrl() {
        return mRequestUrl;
    }

    public HashMap<String, String> getRequestHeaders() {
        return mRequestHeaders;
    }

    public void setReturnResponse(HttpWebResponse mReturnResponse) {
        this.mReturnResponse = mReturnResponse;
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
