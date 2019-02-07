//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.aad.adal;

import android.support.test.runner.AndroidJUnit4;

import com.google.gson.Gson;
import com.microsoft.identity.common.internal.broker.BrokerErrorResponse;
import com.microsoft.identity.common.internal.broker.BrokerResult;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class BrokerResultSerializationTest {

    @Test
    public void brokerResultSerializesAndDeserializesCorrectlyWhenRequestDataIsPresent() {
        final BrokerErrorResponse errorResponse = new BrokerErrorResponse();
        errorResponse.setStatusCode(429);
        errorResponse.setError("too_many_requests");
        errorResponse.setOAuthSubError("retry after: 4000");
        errorResponse.setResponseHeadersJson("{\"Content-Type\" : \"application/json\"}");
        final BrokerResult result = new BrokerResult(errorResponse);

        // Turn the object into JSON
        final String json = new Gson().toJson(result);

        // Transform it back...
        final BrokerResult reserializedResult = new Gson().fromJson(json, BrokerResult.class);

        assertEquals(
                errorResponse.getStatusCode(),
                reserializedResult.getErrorResponse().getStatusCode()
        );

        assertNotNull(
                reserializedResult
                        .getErrorResponse()
                        .getResponseHeadersJson()
        );
    }
}
