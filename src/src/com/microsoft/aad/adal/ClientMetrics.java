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

package com.microsoft.aad.adal;

import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

class ClientMetricsEndpointType {
    public static final String TOKEN = "token";

    public static final String INSTANCE_DISCOVERY = "instance";
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
            HashMap<String, String> headers) {
        if (UrlExtensions.isADFSAuthority(queryUrl)) {
            // Don't add for ADFS endpoint
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
        mLastError = (errorCode != null) ? errorCode.replaceAll("[\\[\\]]", "") : "";
    }

    public void setLastErrorCodes(String[] errorCodes) {
        mLastError = (errorCodes != null) ? android.text.TextUtils.join(",", errorCodes) : null;
    }

    private void addClientMetricsHeadersToRequest(HashMap<String, String> headers) {

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
