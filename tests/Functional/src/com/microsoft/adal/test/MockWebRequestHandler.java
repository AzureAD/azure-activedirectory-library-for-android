
package com.microsoft.adal.test;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.IWebRequestHandler;

import android.os.AsyncTask;

/**
 * handler to return mock responses
 */
class MockWebRequestHandler implements IWebRequestHandler {

    private URL mRequestUrl;

    private String mRequestContent;

    private UUID mCorrelationId;

    private HashMap<String, String> mRequestHeaders;

    private HttpWebResponse mReturnResponse;

    private Exception mReturnException;

    @Override
    public AsyncTask<?, ?, ?> sendAsyncGet(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback) throws IllegalArgumentException, IOException {
        mRequestUrl = url;
        mRequestHeaders = headers;
        callback.onComplete(mReturnResponse, mReturnException);
        return null;
    }

    @Override
    public AsyncTask<?, ?, ?> sendAsyncDelete(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback) throws IllegalArgumentException, IOException {
        mRequestUrl = url;
        mRequestHeaders = headers;
        callback.onComplete(mReturnResponse, mReturnException);
        return null;
    }

    @Override
    public AsyncTask<?, ?, ?> sendAsyncPut(URL url, HashMap<String, String> headers,
            byte[] content, String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException {
        mRequestUrl = url;
        mRequestHeaders = headers;
        if (content != null) {
            mRequestContent = new String(content, "UTF-8");
        }
        callback.onComplete(mReturnResponse, mReturnException);
        return null;
    }

    @Override
    public AsyncTask<?, ?, ?> sendAsyncPost(URL url, HashMap<String, String> headers,
            byte[] content, String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException {
        mRequestUrl = url;
        mRequestHeaders = headers;
        if (content != null) {
            mRequestContent = new String(content, "UTF-8");
        }
        callback.onComplete(mReturnResponse, mReturnException);
        return null;
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

    public void setReturnException(Exception mReturnException) {
        this.mReturnException = mReturnException;
    }

    public String getRequestContent() {
        return mRequestContent;
    }

    @Override
    public void setRequestCorrelationId(UUID correlationId) {
        mCorrelationId = correlationId;
    }
}
