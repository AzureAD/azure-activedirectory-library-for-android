
package com.microsoft.adal.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.test.AndroidTestCase;

import com.microsoft.adal.AuthenticationParameters;
import com.microsoft.adal.AuthenticationParameters.AuthenticationParamCallback;

import com.microsoft.adal.HttpWebResponse;

public class AuthenticationParamsTests extends AndroidTestCase {

    public void testGetAuthority() {
        AuthenticationParameters param = new AuthenticationParameters();
        assertTrue("authority should be null", param.getAuthority() == null);
    }

    public void testGetResource() {
        AuthenticationParameters param = new AuthenticationParameters();
        assertTrue("resource should be null", param.getResource() == null);
    }

    public void testCreateFromResourceUrlInvalidFormat() {

        final CountDownLatch signal = new CountDownLatch(1);
        try {
            AuthenticationParameters.createFromResourceUrl(new URL(
                    "http://www.bing.com"), new AuthenticationParamCallback() {

                @Override
                public void onCompleted(Exception exception,
                        AuthenticationParameters param) {
                    assertNotNull(exception);
                    assertNull(param);
                    assertTrue(
                            "Check header exception",
                            exception.getMessage() == AuthenticationParameters.AUTH_HEADER_WRONG_STATUS);
                    signal.countDown();
                }
            });
            signal.await();
        } catch (MalformedURLException e) {
            assertTrue("MalformedURLException is not expected", false);
        } catch (InterruptedException e) {
            assertTrue("interruption is not expected", false);
        }

    }

    public void testParseResponseWrongStatus() {
        // send wrong status

        Method m = null;
        try {
            m = AuthenticationParameters.class
                    .getDeclaredMethod("parseResponse", HttpWebResponse.class);
        } catch (NoSuchMethodException e) {
            assertTrue("parseResponse is not found", false);
        }

        m.setAccessible(true);
        AuthenticationParameters param = null;

        try {
            param = (AuthenticationParameters) m.invoke(null,
                    new HttpWebResponse(200, null, null));
            assertTrue("expected to fail", false);
        } catch (Exception exception)
        {
            assertNotNull(exception);
            assertNull(param);
            assertTrue(
                    "Check header exception",
                    exception.getCause().getMessage() == AuthenticationParameters.AUTH_HEADER_WRONG_STATUS);
        }

        // correct status
        try {
            param = (AuthenticationParameters) m.invoke(null,
                    new HttpWebResponse(401, null, null));
            assertTrue("expected to fail", false);
        } catch (Exception exception)
        {
            assertNull(param);
            assertTrue(
                    "Check header exception",
                    exception.getCause().getMessage() == AuthenticationParameters.AUTH_HEADER_MISSING);
        }

        // correct status, but incorrect header
        try {
            param = (AuthenticationParameters) m.invoke(null,
                    new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate", "v")));
            assertTrue("expected to fail", false);
        } catch (Exception exception)
        {
            assertNull(param);
            assertTrue(
                    "Check header exception",
                    exception.getCause().getMessage() == AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);
        }

        // correct status, but incorrect authorization param
        try {
            param = (AuthenticationParameters) m.invoke(
                    null,
                    new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate",
                            "Bearer nonsense")));
            assertTrue("expected to fail", false);
        } catch (Exception exception)
        {
            assertNull(param);
            assertTrue(
                    "Check header exception",
                    exception.getCause().getMessage() == AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);
        }

    }

    private HashMap<String, List<String>> getInvalidHeader(String key, String value)
    {
        HashMap<String, List<String>> dummy = new HashMap<String, List<String>>();
        dummy.put(key, Arrays.asList(value, "s2", "s3"));
        return dummy;
    }
};
