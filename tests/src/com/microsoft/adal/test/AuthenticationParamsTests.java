
package com.microsoft.adal.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;
import android.util.Log;

import com.microsoft.adal.AuthenticationParameters;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.AuthenticationParameters.AuthenticationParamCallback;

public class AuthenticationParamsTests extends AndroidTestHelper {

    protected static final String TAG = "AuthenticationParamsTests";

    public void testGetAuthority() {
        AuthenticationParameters param = new AuthenticationParameters();
        assertTrue("authority should be null", param.getAuthority() == null);
    }

    public void testGetResource() {
        AuthenticationParameters param = new AuthenticationParameters();
        assertTrue("resource should be null", param.getResource() == null);
    }

    public void testCreateFromResourceUrlInvalidFormat() {
        Log.d(TAG, "test:" + getName() + "thread:" + android.os.Process.myTid());

        final TestResponse testResponse = new TestResponse();
        setupAsyncParamRequest("http://www.cnn.com", testResponse);

        assertNotNull("Exception null", testResponse.exception);
        assertNull("Parameter is not null", testResponse.param);
        assertTrue(
                "Check header exception",
                testResponse.exception.getMessage() == AuthenticationParameters.AUTH_HEADER_WRONG_STATUS);
    }

    /**
     * test external service deployed at Azure
     */
    public void testCreateFromResourceUrlPositive() {
        Log.d(TAG, "test:" + getName() + "thread:" + android.os.Process.myTid());

        final TestResponse testResponse = new TestResponse();
        setupAsyncParamRequest("https://testapi007.azurewebsites.net/api/WorkItem", testResponse);

        assertNull("Exception is not null", testResponse.exception);
        assertNotNull("Check parameter", testResponse.param);
        Log.d(TAG, "test:" + getName() + "authority:" + testResponse.param.getAuthority());
        assertEquals("https://login.windows.net/omercantest.onmicrosoft.com", testResponse.param
                .getAuthority().trim());
    }

    /**
     * test private method to make sure parsing is right
     * 
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     */
    public void testParseResponseNegative() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        callParseResponseForException(new HttpWebResponse(200, null, null),
                AuthenticationParameters.AUTH_HEADER_WRONG_STATUS);

        callParseResponseForException(new HttpWebResponse(401, null, null),
                AuthenticationParameters.AUTH_HEADER_MISSING);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate", "v")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate",
                        "Bearer nonsense")), AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate", " Bearer")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate", " Bearer ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate", "\t Bearer  ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate", "Bearer foo ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate",
                        "Bear gets=honey ")), AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate", "Bearer =,=,")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate",
                        "Bearer authorization_uri= ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate",
                        "Bearerauthorization_uri=something")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getInvalidHeader("WWW-Authenticate",
                        "Bearer authorization_uri=,something=a ")),
                AuthenticationParameters.AUTH_HEADER_MISSING_AUTHORITY);
    }

    private Method getParseResponseMethod() throws ClassNotFoundException {
        Method m = null;
        try {
            m = AuthenticationParameters.class.getDeclaredMethod("parseResponse",
                    Class.forName("com.microsoft.adal.HttpWebResponse"));
        } catch (NoSuchMethodException e) {
            assertTrue("parseResponse is not found", false);
        }

        m.setAccessible(true);
        return m;
    }

    private void callParseResponseForException(Object response, String message)
            throws ClassNotFoundException {
        Method m = getParseResponseMethod();
        AuthenticationParameters param = null;

        try {
            param = (AuthenticationParameters)m.invoke(null, response);
            assertTrue("expected to fail", false);
        } catch (Exception exception) {
            assertNotNull("Exception is not null", exception);
            assertNull("Param is expected to be null", param);
            assertTrue("Check header exception", exception.getCause().getMessage() == message);
        }
    }

    public void testcreateFromResourceUrlNoCallback() throws MalformedURLException {

        final URL url = new URL("https://www.something.com");
        assertThrowsException(IllegalArgumentException.class, "callback", new Runnable() {

            @Override
            public void run() {
                AuthenticationParameters.createFromResourceUrl(url, null);
            }
        });

    }

    class TestResponse {
        AuthenticationParameters param;

        Exception exception;
    }

    /**
     * This will call createFromResourceUrl for given url and update the
     * testresponse obj it helps to wait for the callback and handles
     * interruptions
     * 
     * @param requestUrl
     * @param testResponse
     */
    private void setupAsyncParamRequest(final String requestUrl, final TestResponse testResponse) {
        final CountDownLatch signal = new CountDownLatch(1);
        final AuthenticationParamCallback callback = new AuthenticationParamCallback() {
            @Override
            public void onCompleted(Exception exception, AuthenticationParameters param) {
                testResponse.param = param;
                testResponse.exception = exception;
                Log.d(TAG, "test " + android.os.Process.myTid());
                signal.countDown();
            }
        };

        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                try {
                    AuthenticationParameters.createFromResourceUrl(new URL(requestUrl), callback);
                } catch (MalformedURLException e) {
                    Assert.fail("unexpected url error");
                    signal.countDown();
                }
            }
        }, true);
    }

    private HashMap<String, List<String>> getInvalidHeader(String key, String value) {
        HashMap<String, List<String>> dummy = new HashMap<String, List<String>>();
        dummy.put(key, Arrays.asList(value, "s2", "s3"));
        return dummy;
    }
};
