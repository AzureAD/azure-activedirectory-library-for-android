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
