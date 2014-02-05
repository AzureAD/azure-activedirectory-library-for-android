
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
