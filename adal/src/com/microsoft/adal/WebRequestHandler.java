
package com.microsoft.adal;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import android.os.AsyncTask;
import android.util.Log;

/**
 * It uses one time async task. WebRequest are wrapped here to prevent multiple
 * reuses for same tasks. Each request returns a handler for cancel action.
 * Call this from UI thread to correctly create async task and execute.
 * @author omercan
 */
public class WebRequestHandler implements IWebRequestHandler {

    private final static String TAG = "WebRequestHandler";

    public final static String HEADER_ACCEPT = "Accept";
    public final static String HEADER_ACCEPT_JSON = "application/json";
    
    @Override
    public AsyncTask<?, ?, ?> sendAsyncGet(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        Log.d(TAG, "WebRequestHandler thread" + android.os.Process.myTid());

        HttpWebRequest request = new HttpWebRequest(url);
        addHeadersToRequest(headers, request);
        request.sendAsyncGet(callback);
        return request;
    }

    @Override
    public AsyncTask<?, ?, ?> sendAsyncDelete(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        Log.d(TAG, "WebRequestHandler thread" + android.os.Process.myTid());

        HttpWebRequest request = new HttpWebRequest(url);
        addHeadersToRequest(headers, request);
        request.sendAsyncDelete(callback);
        return request;
    }

    @Override
    public AsyncTask<?, ?, ?> sendAsyncPut(URL url, HashMap<String, String> headers,
            byte[] content, String contentType, HttpWebRequestCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        Log.d(TAG, "WebRequestHandler thread" + android.os.Process.myTid());

        HttpWebRequest request = new HttpWebRequest(url);
        addHeadersToRequest(headers, request);
        request.sendAsyncPut(content, contentType, callback);
        return request;
    }

    @Override
    public AsyncTask<?, ?, ?> sendAsyncPost(URL url, HashMap<String, String> headers,
            byte[] content, String contentType, HttpWebRequestCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        Log.d(TAG, "WebRequestHandler thread" + android.os.Process.myTid());

        HttpWebRequest request = new HttpWebRequest(url);
        addHeadersToRequest(headers, request);
        request.sendAsyncPost(content, contentType, callback);
        return request;
    }

    private void addHeadersToRequest(HashMap<String, String> headers, HttpWebRequest request) {
        if (headers != null && !headers.isEmpty()) {
            request.getRequestHeaders().putAll(headers);
        }
    }
}
