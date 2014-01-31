
package com.microsoft.adal;

import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.microsoft.adal.AuthenticationConstants.AAD;

/**
 * It uses one time async task. WebRequest are wrapped here to prevent multiple
 * reuses for same tasks. Each request returns a handler for cancel action. Call
 * this from UI thread to correctly create async task and execute.
 * 
 * @author omercan
 */
public class WebRequestHandler implements IWebRequestHandler {

    private final static String TAG = "WebRequestHandler";

    public final static String HEADER_ACCEPT = "Accept";

    public final static String HEADER_ACCEPT_JSON = "application/json";

    private UUID mRequestCorrelationId = null;

    public WebRequestHandler() {

    }

    @Override
    public AsyncTask<?, ?, ?> sendAsyncGet(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        Log.d(TAG, "WebRequestHandler thread" + android.os.Process.myTid());

        HttpWebRequest request = new HttpWebRequest(url);
        headers = updateHeaders(headers);
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
        headers = updateHeaders(headers);
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
        headers = updateHeaders(headers);
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
        headers = updateHeaders(headers);
        addHeadersToRequest(headers, request);
        request.sendAsyncPost(content, contentType, callback);
        return request;
    }

    private void addHeadersToRequest(HashMap<String, String> headers, HttpWebRequest request) {
        if (headers != null && !headers.isEmpty()) {
            request.getRequestHeaders().putAll(headers);
        }
    }

    private HashMap<String, String> updateHeaders(HashMap<String, String> headers) {

        if (headers == null) {
            headers = new HashMap<String, String>();
        }

        if (mRequestCorrelationId != null) {
            headers.put(AAD.CLIENT_REQUEST_ID, mRequestCorrelationId.toString());
        }

        headers.put(AAD.ADAL_ID_PLATFORM, "Android");
        headers.put(AAD.ADAL_ID_VERSION, AuthenticationContext.getVersionName());
        headers.put(AAD.ADAL_ID_OS_VER, "" + Build.VERSION.SDK_INT);
        headers.put(AAD.ADAL_ID_DM, android.os.Build.MODEL);

        return headers;
    }

    public void setRequestCorrelationId(UUID mRequestCorrelationId) {
        this.mRequestCorrelationId = mRequestCorrelationId;
    }

}
