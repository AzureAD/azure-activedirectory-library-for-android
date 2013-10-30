
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

import android.test.AndroidTestCase;
import android.util.Log;

import com.microsoft.adal.AuthenticationParameters;
import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.WebRequestHandler;
import com.microsoft.adal.AuthenticationParameters.AuthenticationParamCallback;
import com.microsoft.adal.test.WebRequestHandlerTests.TestResponse;

import com.microsoft.adal.HttpWebResponse;

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

        assertNotNull(testResponse.exception);
        assertNull(testResponse.param);
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

        assertNull(testResponse.exception);
        assertNotNull(testResponse.param);
        Log.d(TAG, "test:" + getName() + "authority:" + testResponse.param.getAuthority());
        assertSame("https://login.windows.net/omercantest.onmicrosoft.com",
                testResponse.param.getAuthority());
    }

    /**
     * test private method to make sure parsing is right
     */
    public void testParseResponseWrongStatus() {
        // send wrong status

        Method m = null;
        try {
            m = AuthenticationParameters.class.getDeclaredMethod("parseResponse",
                    HttpWebResponse.class);
        } catch (NoSuchMethodException e) {
            assertTrue("parseResponse is not found", false);
        }

        m.setAccessible(true);
        AuthenticationParameters param = null;

        try {
            param = (AuthenticationParameters)m.invoke(null, new HttpWebResponse(200, null, null));
            assertTrue("expected to fail", false);
        } catch (Exception exception) {
            assertNotNull(exception);
            assertNull(param);
            assertTrue(
                    "Check header exception",
                    exception.getCause().getMessage() == AuthenticationParameters.AUTH_HEADER_WRONG_STATUS);
        }

        // correct status
        try {
            param = (AuthenticationParameters)m.invoke(null, new HttpWebResponse(401, null, null));
            assertTrue("expected to fail", false);
        } catch (Exception exception) {
            assertNull(param);
            assertTrue(
                    "Check header exception",
                    exception.getCause().getMessage() == AuthenticationParameters.AUTH_HEADER_MISSING);
        }

        // correct status, but incorrect header
        try {
            param = (AuthenticationParameters)m.invoke(null, new HttpWebResponse(401, null,
                    getInvalidHeader("WWW-Authenticate", "v")));
            assertTrue("expected to fail", false);
        } catch (Exception exception) {
            assertNull(param);
            assertTrue(
                    "Check header exception",
                    exception.getCause().getMessage() == AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);
        }

        // correct status, but incorrect authorization param
        try {
            param = (AuthenticationParameters)m.invoke(null, new HttpWebResponse(401, null,
                    getInvalidHeader("WWW-Authenticate", "Bearer nonsense")));
            assertTrue("expected to fail", false);
        } catch (Exception exception) {
            assertNull(param);
            assertTrue(
                    "Check header exception",
                    exception.getCause().getMessage() == AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);
        }

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

        testAsyncNoException(signal, new Runnable() {
            @Override
            public void run() {
                try {
                    AuthenticationParameters.createFromResourceUrl(new URL(requestUrl), callback);
                } catch (MalformedURLException e) {
                    Assert.fail("unexpected url error");
                    signal.countDown();
                }
            }
        });
    }

    private HashMap<String, List<String>> getInvalidHeader(String key, String value) {
        HashMap<String, List<String>> dummy = new HashMap<String, List<String>>();
        dummy.put(key, Arrays.asList(value, "s2", "s3"));
        return dummy;
    }
};
