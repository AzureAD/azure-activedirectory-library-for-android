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
import java.util.Map;
import java.util.UUID;

final class ClientMetricsEndpointType {
    public static final String TOKEN = "token";

    public static final String INSTANCE_DISCOVERY = "instance";

    private ClientMetricsEndpointType() {
        // Intentionally left blank
    }
}

enum ClientMetrics {

    /**
     * Singleton instance.
     */
    INSTANCE;

    private static final String CLIENT_METRICS_HEADER_LAST_ERROR = "x-client-last-error";

    private static final String CLIENT_METRICS_HEADER_LAST_REQUEST = "x-client-last-request";

    private static final String CLIENT_METRICS_HEADER_LAST_RESPONSE_TIME = "x-client-last-response-time";

    private static final String CLIENT_METRICS_HEADER_LAST_ENDPOINT = "x-client-last-endpoint";

    private long mStartTimeMillis = 0;

    private String mLastError;

    private UUID mLastCorrelationId;

    private long mLastResponseTime;

    private String mLastEndpoint;

    private boolean mIsPending = false;

    private URL mQueryUrl;

    public void beginClientMetricsRecord(URL queryUrl, UUID correlationId,
            Map<String, String> headers) {
        if (UrlExtensions.isADFSAuthority(queryUrl)) {
            // Don't add for ADFS endpoint
            mLastCorrelationId = null;
            return;
        }

        if (mIsPending) {
            addClientMetricsHeadersToRequest(headers);
        }

        mStartTimeMillis = System.currentTimeMillis();
        mQueryUrl = queryUrl;
        mLastCorrelationId = correlationId;
        mLastError = "";
        mIsPending = false;
    }

    public void endClientMetricsRecord(String endpoint, UUID correlationId) {
        if (UrlExtensions.isADFSAuthority(mQueryUrl)) {
            // Don't send to ADFS endpoint
            return;
        }

        mLastEndpoint = endpoint;

        if (mStartTimeMillis != 0) {
            mLastResponseTime = System.currentTimeMillis() - mStartTimeMillis;
            mLastCorrelationId = correlationId;
        }

        mIsPending = true;
    }

    public void setLastError(String errorCode) {
        mLastError = (errorCode == null) ? "" : errorCode.replaceAll("[\\[\\]]", "");
    }

    public void setLastErrorCodes(String[] errorCodes) {
        mLastError = (errorCodes == null) ? null : android.text.TextUtils.join(",", errorCodes);
    }

    private void addClientMetricsHeadersToRequest(Map<String, String> headers) {

        if (mLastError != null) {
            headers.put(CLIENT_METRICS_HEADER_LAST_ERROR, mLastError);
        }

        if (mLastCorrelationId != null) {
            headers.put(CLIENT_METRICS_HEADER_LAST_REQUEST, mLastCorrelationId.toString());
        }

        headers.put(CLIENT_METRICS_HEADER_LAST_RESPONSE_TIME, Long.toString(mLastResponseTime));
        headers.put(CLIENT_METRICS_HEADER_LAST_ENDPOINT, mLastEndpoint);
    }
}
