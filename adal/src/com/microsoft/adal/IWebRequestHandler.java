
package com.microsoft.adal;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import android.os.AsyncTask;
import android.os.Handler;

/**
 * Webrequest interface to send one time async requests
 * 
 * @author omercan
 */
public interface IWebRequestHandler {

    /**
     * @param url
     * @param headers
     * @param callback
     * @throws IllegalArgumentException
     * @throws IOException
     */
    AsyncTask<?, ?, ?> sendAsyncGet(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback)
            throws IllegalArgumentException,
            IOException;

    AsyncTask<?, ?, ?> sendAsyncDelete(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback)
            throws IllegalArgumentException,
            IOException;

    AsyncTask<?, ?, ?> sendAsyncPut(URL url, HashMap<String, String> headers, byte[] content,
            String contentType,
            HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException;

    AsyncTask<?, ?, ?> sendAsyncPost(URL url, HashMap<String, String> headers, byte[] content,
            String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException;

}
