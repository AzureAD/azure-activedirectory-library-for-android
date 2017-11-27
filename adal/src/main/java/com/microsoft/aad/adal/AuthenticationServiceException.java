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

import java.util.List;
import java.util.Map;

/**
 * ADAL exception for STS error handling.
 */
public class AuthenticationServiceException extends AuthenticationException {
    static final long serialVersionUID = 1;

    private HttpWebResponse mResponse;

    /**
     * Constructor for {@link AuthenticationServiceException}.
     * @param httpWebResponse
     */
    public AuthenticationServiceException(final HttpWebResponse httpWebResponse) {
        super(ADALError.SERVER_ERROR, getResponseInfo(httpWebResponse));
        mResponse = httpWebResponse;
    }

    private static String getResponseInfo(final HttpWebResponse response) {
        if (null == response) {
            throw new IllegalArgumentException("HttpWebResponse could not be NULL.");
        } else {
            return "Unexpected server response " + response.getStatusCode() + " " + response.getBody();
        }
    }

    /**
     * Get http web response status code.
     * @return int status code
     */
    public int getStatusCode() {
        if (mResponse != null) {
            return mResponse.getStatusCode();
        } else {
            throw new IllegalArgumentException("HttpWebResponse could not be NULL.");
        }
    }

    /**
     * Get http web response body.
     * @return String The response body for the network call.
     */
    public String getBody() {
        if (mResponse != null) {
            return mResponse.getBody();
        } else {
            throw new IllegalArgumentException("HttpWebResponse could not be NULL.");
        }
    }

    /**
     * Get the http web response headers.
     * @return The response headers for the network call.
     */
    public Map<String, List<String>> getHeaders() {
        if (null == mResponse || null == mResponse.getResponseHeaders()) {
            throw new IllegalArgumentException("Illegal httpWebResponse.");
        } else {
            return mResponse.getResponseHeaders();
        }
    }

}
