
package com.microsoft.adal;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import android.os.AsyncTask;

/**
 * Webrequest interface to send one time async requests Methods return generic
 * interface to send cancel request and check cancel request. Results are posted
 * at callback.
 * 
 * @author omercan
 */
public interface IWebRequestHandler {

    /**
     * send get request
     * 
     * @param url
     * @param headers
     * @param callback
     * @return Generic async task for cancel
     * @throws IllegalArgumentException
     * @throws IOException
     */
    AsyncTask<?, ?, ?> sendAsyncGet(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback) throws IllegalArgumentException, IOException;

    /**
     * send delete request
     * 
     * @param url
     * @param headers
     * @param callback
     * @return Generic async task for cancel
     * @throws IllegalArgumentException
     * @throws IOException
     */
    AsyncTask<?, ?, ?> sendAsyncDelete(URL url, HashMap<String, String> headers,
            HttpWebRequestCallback callback) throws IllegalArgumentException, IOException;

    /**
     * send async put request
     * 
     * @param url
     * @param headers
     * @param content
     * @param contentType
     * @param callback
     * @return Generic async task for cancel
     * @throws IllegalArgumentException
     * @throws IOException
     */
    AsyncTask<?, ?, ?> sendAsyncPut(URL url, HashMap<String, String> headers, byte[] content,
            String contentType, HttpWebRequestCallback callback) throws IllegalArgumentException,
            IOException;

    /**
     * send async post
     * 
     * @param url
     * @param headers
     * @param content
     * @param contentType
     * @param callback
     * @return Generic async task for cancel
     * @throws IllegalArgumentException
     * @throws IOException
     */
    AsyncTask<?, ?, ?> sendAsyncPost(URL url, HashMap<String, String> headers, byte[] content,
            String contentType, HttpWebRequestCallback callback) throws IllegalArgumentException,
            IOException;
    
    public void setRequestCorrelationId(UUID mRequestCorrelationId);

}
