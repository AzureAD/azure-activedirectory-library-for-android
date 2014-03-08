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

import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import android.os.Build;

import com.microsoft.adal.AuthenticationConstants.AAD;

public class WebRequestHandler implements IWebRequestHandler {

    private final static String TAG = "WebRequestHandler";

    public final static String HEADER_ACCEPT = "Accept";

    public final static String HEADER_ACCEPT_JSON = "application/json";

    private UUID mRequestCorrelationId = null;

    public WebRequestHandler() {

    }

    @Override
    public HttpWebResponse sendGet(URL url, HashMap<String, String> headers) {
        Logger.d(TAG, "WebRequestHandler thread" + android.os.Process.myTid());

        HttpWebRequest request = new HttpWebRequest(url);
        request.setRequestMethod(HttpWebRequest.REQUEST_METHOD_GET);
        headers = updateHeaders(headers);
        addHeadersToRequest(headers, request);
        return request.send();
    }

    @Override
    public HttpWebResponse sendPost(URL url, HashMap<String, String> headers, byte[] content,
            String contentType) {
        Logger.d(TAG, "WebRequestHandler thread" + android.os.Process.myTid());

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

    public void setRequestCorrelationId(UUID mRequestCorrelationId) {
        this.mRequestCorrelationId = mRequestCorrelationId;
    }
}
