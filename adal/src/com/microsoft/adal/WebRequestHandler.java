
package com.microsoft.adal;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

/**
 * It uses one time async task. WebRequest are wrapped here to prevent multiple
 * reuses for same tasks.
 * 
 * @author omercan
 */
public class WebRequestHandler implements IWebRequestHandler {

    @Override
    public void sendAsyncGet(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback) throws IllegalArgumentException,
            IOException {
        HttpWebRequest request = new HttpWebRequest(url);
        addHeadersToRequest(headers, request);
        request.sendAsyncGet(callback);
    }

    @Override
    public void sendAsyncDelete(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback) throws IllegalArgumentException,
            IOException {
        HttpWebRequest request = new HttpWebRequest(url);
        addHeadersToRequest(headers, request);
        request.sendAsyncDelete(callback);
    }

    @Override
    public void sendAsyncPut(URL url, HashMap<String, String> headers,
            byte[] content, String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException {
        HttpWebRequest request = new HttpWebRequest(url);
        addHeadersToRequest(headers, request);
        request.sendAsyncPut(content, contentType, callback);
    }

    @Override
    public void sendAsyncPost(URL url, HashMap<String, String> headers,
            byte[] content, String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException {
        HttpWebRequest request = new HttpWebRequest(url);
        addHeadersToRequest(headers, request);
        request.sendAsyncPost(content, contentType, callback);
    }

    private void addHeadersToRequest(HashMap<String, String> headers, HttpWebRequest request) {
        if (headers != null && !headers.isEmpty())
        {
            request.getRequestHeaders().putAll(headers);
        }
    }
}
