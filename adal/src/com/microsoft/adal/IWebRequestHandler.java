package com.microsoft.adal;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import android.os.Handler;

/**
 * Webrequest interface to send one time async requests
 * @author omercan
 */
public interface IWebRequestHandler {

    /**
     * 
     * @param url
     * @param headers
     * @param callback
     * @throws IllegalArgumentException
     * @throws IOException
     */
    void sendAsyncGet(URL url, HashMap<String, String> headers, HttpWebRequestCallback callback) throws IllegalArgumentException,
            IOException;

    void sendAsyncDelete(URL url, HashMap<String, String> headers, HttpWebRequestCallback callback) throws IllegalArgumentException,
            IOException;

    void sendAsyncPut(URL url, HashMap<String, String> headers, byte[] content, String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException;

    void sendAsyncPost(URL url, HashMap<String, String> headers, byte[] content, String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException;

}
