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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientMetricTests extends AndroidTestHelper {

    public void testADFSBehavior() throws ClassNotFoundException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, MalformedURLException, NoSuchFieldException {

        Object clientMetrics = getInstance();
        final URL endpointAdfs = new URL("https://fs.ade2eadfs30.com/adfs");
        UUID correlationId = UUID.randomUUID();
        Map<String, String> headers = new HashMap<String, String>();
        Method method = ReflectionUtils.getTestMethod(clientMetrics, "beginClientMetricsRecord",
                URL.class, UUID.class, Map.class);
        method.invoke(clientMetrics, endpointAdfs, correlationId, headers);

        assertTrue("Expecting empty header", headers.isEmpty());
        assertEquals("CorrelationId is empty", null,
                ReflectionUtils.getFieldValue(clientMetrics, "mLastCorrelationId"));
    }

    public void testPendingMetrics() throws ClassNotFoundException, IllegalArgumentException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, MalformedURLException, NoSuchFieldException {
        Object clientMetrics = getInstance();
        final URL endpointAdfs = new URL("https://login.windwos.com/testtenant");
        UUID correlationId = UUID.randomUUID();
        Map<String, String> headers = new HashMap<String, String>();
        Method beginMethod = ReflectionUtils.getTestMethod(clientMetrics,
                "beginClientMetricsRecord", URL.class, UUID.class, Map.class);
        beginMethod.invoke(clientMetrics, endpointAdfs, correlationId, headers);

        Method endMethod = ReflectionUtils.getTestMethod(clientMetrics, "endClientMetricsRecord",
                String.class, UUID.class);
        endMethod.invoke(clientMetrics, "instance", correlationId);

        Method lastErr = ReflectionUtils.getTestMethod(clientMetrics, "setLastError", String.class);
        lastErr.invoke(clientMetrics, "lastErrorTest");
        // next call will report error
        assertEquals("CorrelationId is empty", correlationId,
                ReflectionUtils.getFieldValue(clientMetrics, "mLastCorrelationId"));
    }

    private Object getInstance() throws ClassNotFoundException {
        // Full package name
        Class<?> c = Class.forName("com.microsoft.aad.adal.ClientMetrics");
        Object[] consts = c.getEnumConstants();
        return consts[0];
    }
}
