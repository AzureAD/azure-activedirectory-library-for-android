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

import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

/**
 * Webrequest interface to send web requests.
 */
public interface IWebRequestHandler {
    /**
     * Send the http GET request.
     * @param url {@link URL} for the GET request.
     * @param headers Non-null, and mutable Map of headers sent in the GET request.
     * @return {@link HttpWebResponse} containing the status code and response headers.
     * @throws IOException when error occurs on reading the http response.
     */
    HttpWebResponse sendGet(URL url, Map<String, String> headers) throws IOException;

    /**
     * Send the HTTP POST request.
     * @param url {@link URL} for the POST request.
     * @param headers Non-null, and mutable Map of headers sent int the POST request.
     * @param content The content sent as POST message.
     * @param contentType Content type of the POST request.
     * @return {@link HttpWebResponse} containing the status code and response header.
     * @throws IOException when error occurs on reading the http response.
     */
    HttpWebResponse sendPost(URL url, Map<String, String> headers, byte[] content,
            String contentType) throws IOException;

    /**
     * Set the correlation id for the web request.
     * @param requestCorrelationId {@link UUID} of the correlation id to set in the web request.
     */
    void setRequestCorrelationId(UUID requestCorrelationId);
}
