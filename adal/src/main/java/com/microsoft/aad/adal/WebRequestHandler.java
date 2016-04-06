// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import com.microsoft.aad.adal.AuthenticationConstants.AAD;

import android.os.Build;

/**
 * It uses one time async task. WebRequest are wrapped here to prevent multiple
 * reuses for same tasks. Each request returns a handler for cancel action. Call
 * this from UI thread to correctly create async task and execute.
 */
public class WebRequestHandler implements IWebRequestHandler {

    private static final String TAG = "WebRequestHandler";

    /**
     * Header for accept.
     */
    public static final String HEADER_ACCEPT = "Accept";

    /**
     * Header for json type.
     */
    public static final String HEADER_ACCEPT_JSON = "application/json";

    private UUID mRequestCorrelationId = null;

    /**
     * Creates http request.
     */
    public WebRequestHandler() {

    }

    @Override
    public HttpWebResponse sendGet(URL url, HashMap<String, String> headers) {
        Logger.v(TAG, "WebRequestHandler thread" + android.os.Process.myTid());

        HttpWebRequest request = new HttpWebRequest(url);
        request.setRequestMethod(HttpWebRequest.REQUEST_METHOD_GET);
        headers = updateHeaders(headers);
        addHeadersToRequest(headers, request);
        return request.send();
    }

    @Override
    public HttpWebResponse sendPost(URL url, HashMap<String, String> headers, byte[] content,
            String contentType) {
        Logger.v(TAG, "WebRequestHandler thread" + android.os.Process.myTid());

        HttpWebRequest request = new HttpWebRequest(url);
        request.setRequestMethod(HttpWebRequest.REQUEST_METHOD_POST);
        request.setRequestContentType(contentType);
        request.setRequestContent(content);
        headers = updateHeaders(headers);
        addHeadersToRequest(headers, request);
        return request.send();
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

    /**
     * Sets correlationId.
     * 
     * @param requestCorrelationId {@link UUID}
     */
    public void setRequestCorrelationId(UUID requestCorrelationId) {
        this.mRequestCorrelationId = requestCorrelationId;
    }
}
