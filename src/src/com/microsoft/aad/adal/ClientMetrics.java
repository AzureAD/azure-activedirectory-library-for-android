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

import java.util.HashMap;
import java.util.UUID;
import android.text.TextUtils;
import com.microsoft.aad.adal.AuthenticationConstants.AAD;

class ClientMetricsEndpointType
{
    public static final String TOKEN = "token";
    public static final String INSTANCE_DISCOVERY = "instance";
}

class ClientMetrics {
    private static final String CLIENT_METRICS_HEADER_LAST_ERROR = "x-client-last-error";
    private static final String CLIENT_METRICS_HEADER_LAST_REQUEST = "x-client-last-request";
    private static final String CLIENT_METRICS_HEADER_LAST_RESPONSE_TIME = "x-client-last-response-time";
    private static final String CLIENT_METRICS_HEADER_LAST_ENDPOINT = "x-client-last-endpoint";	
	
    private static ClientMetrics mPendingClientMetrics;	
	
    private long mStartTimeMillis = 0;
    private String mLastError;
    private UUID mLastCorrelationId;
    private long mLastResponseTime;
    private String mLastEndpoint;    
    
    public void beginClientMetricsRecord(HashMap<String, String> headers)
    {
    	// TODO: Change the condition below to only send metrics to AAD (not ADFS)
	    if (true)
	    {
	    	addClientMetricsHeadersToRequest(headers);
	    	mStartTimeMillis = System.currentTimeMillis();
	    }                
    }

    public void endClientMetricsRecord(String endpoint, UUID correlationId)
    {
    	// TODO: Change the condition below to only send metrics to AAD (not ADFS)
        if (mStartTimeMillis != 0)
        {
            mLastResponseTime = System.currentTimeMillis() - mStartTimeMillis;
            mLastCorrelationId = correlationId;
            mLastEndpoint = endpoint;
            if (mPendingClientMetrics == null) 
            {
                mPendingClientMetrics = this;
            }
        }
    }

    public void setLastError(String[] errorCodes)
    {
        mLastError = (errorCodes != null) ? android.text.TextUtils.join(",", errorCodes) : null;
    }

    private static void addClientMetricsHeadersToRequest(HashMap<String, String> headers)
    {
        if (mPendingClientMetrics != null)
        {
            if (mPendingClientMetrics.mLastError != null)
            {
                headers.put(CLIENT_METRICS_HEADER_LAST_ERROR, mPendingClientMetrics.mLastError);
            }

            headers.put(CLIENT_METRICS_HEADER_LAST_REQUEST, mPendingClientMetrics.mLastCorrelationId.toString());
            headers.put(CLIENT_METRICS_HEADER_LAST_RESPONSE_TIME, Long.toString(mPendingClientMetrics.mLastResponseTime));
            headers.put(CLIENT_METRICS_HEADER_LAST_ENDPOINT, mPendingClientMetrics.mLastEndpoint);

            mPendingClientMetrics = null;
        }
    }
}
