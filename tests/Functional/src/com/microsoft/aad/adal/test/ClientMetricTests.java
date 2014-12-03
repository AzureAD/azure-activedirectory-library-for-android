
package com.microsoft.aad.adal.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

public class ClientMetricTests extends AndroidTestHelper {

    public void testADFSBehavior() throws ClassNotFoundException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, MalformedURLException, NoSuchFieldException {

        Object clientMetrics = getInstance();
        final URL endpointAdfs = new URL("https://fs.ade2eadfs30.com/adfs");
        UUID correlationId = UUID.randomUUID();
        HashMap<String, String> headers = new HashMap<String, String>();
        Method method = ReflectionUtils.getTestMethod(clientMetrics, "beginClientMetricsRecord",
                URL.class, UUID.class, headers.getClass());
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
        HashMap<String, String> headers = new HashMap<String, String>();
        Method beginMethod = ReflectionUtils.getTestMethod(clientMetrics,
                "beginClientMetricsRecord", URL.class, UUID.class, headers.getClass());
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
