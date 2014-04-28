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

package com.microsoft.aad.adal.test;

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

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationParameters;
import com.microsoft.aad.adal.AuthenticationParameters.AuthenticationParamCallback;
import com.microsoft.aad.adal.HttpWebResponse;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.Logger.ILogger;
import com.microsoft.aad.adal.Logger.LogLevel;

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

    public void testCreateFromResponseAuthenticateHeader() {
        assertThrowsException(IllegalArgumentException.class,
                AuthenticationParameters.AUTH_HEADER_MISSING.toLowerCase(), new Runnable() {

                    @Override
                    public void run() {
                        AuthenticationParameters.createFromResponseAuthenticateHeader(null);
                    }
                });

        assertThrowsException(IllegalArgumentException.class,
                AuthenticationParameters.AUTH_HEADER_MISSING_AUTHORITY.toLowerCase(), new Runnable() {

                    @Override
                    public void run() {
                        AuthenticationParameters
                                .createFromResponseAuthenticateHeader("Bearer\t resource=\"is=outer, space=ornot\",\t\t  authorization_uri=\"\"");
                    }
                });
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

    public void testParseResponsePositive() throws ClassNotFoundException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        Method m = getParseResponseMethod();

        verifyAuthenticationParam(
                m,
                "Bearer scope=\"blah=foo, foo=blah\" , authorization_uri=\"https://login.windows.net/tenant\"",
                "https://login.windows.net/tenant", null);

        verifyAuthenticationParam(
                m,
                "Bearer scope=\"is=outer, space=ornot\",\t\t  authorization_uri=\"https://login.windows.net/tenant\"",
                "https://login.windows.net/tenant", null);

        verifyAuthenticationParam(
                m,
                "Bearer\tscope=\"is=outer, space=ornot\",\t\t  authorization_uri=\"https://login.windows.net/tenant\" ,resource_id=\"blah=foo, foo=blah\"",
                "https://login.windows.net/tenant", "blah=foo, foo=blah");

        LogCallback callback = new LogCallback(ADALError.DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS);
        Logger.getInstance().setExternalLogger(callback);
        verifyAuthenticationParam(
                m,
                "Bearer   \t  scope=\"is=outer, space=ornot\",\t\t  authorization_uri=\"https://login.windows.net/tenant\", authorization_uri=\"https://login.windows.net/tenant\"",
                "https://login.windows.net/tenant", null);

        assertTrue("Has warning for redudant items", callback.called);
        Logger.getInstance().setExternalLogger(null);
    }

    private void verifyAuthenticationParam(Method m, String headerValue, String authorizationUri,
            String resource) throws IllegalAccessException, InvocationTargetException {
        AuthenticationParameters param = (AuthenticationParameters)m.invoke(null,
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", headerValue)));
        assertNotNull("Parsed ok", param);
        assertEquals("Verify authorization uri", authorizationUri, param.getAuthority());
        assertEquals("Verify resource", resource, param.getResource());
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
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", "v")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", "Bearer nonsense")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", "Bearer")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);
        
        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", "Bearer ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", " Bearer")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", " Bearer ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", "\t Bearer  ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", "Bearer foo ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", "Bear gets=honey ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate", "Bearer =,=,")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate",
                        "Bearer some text here,")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate",
                        "Bearer authorization_uri= ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate",
                        "Bearerauthorization_uri=something")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate",
                        "Bearerauthorization_uri=\"https://www.something.com\"")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(401, null, getHeader("WWW-Authenticate",
                        "Bearer    \t authorization_uri=,something=a ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);
    }

    class LogCallback implements ILogger {
        boolean called = false;

        ADALError checkCode;

        public LogCallback(ADALError errorCode) {
            checkCode = errorCode;
            called = false;
        }

        @Override
        public void Log(String tag, String message, String additionalMessage, LogLevel level,
                ADALError errorCode) {
            if (errorCode == checkCode) {
                called = true;
            }
        }
    };

    private Method getParseResponseMethod() throws ClassNotFoundException {
        Method m = null;
        try {
            m = AuthenticationParameters.class.getDeclaredMethod("parseResponse",
                    Class.forName("com.microsoft.aad.adal.HttpWebResponse"));
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
                AuthenticationParameters.createFromResourceUrl(getInstrumentation().getTargetContext(), url, null);
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
                    AuthenticationParameters.createFromResourceUrl(getInstrumentation().getTargetContext(), new URL(requestUrl), callback);
                } catch (MalformedURLException e) {
                    Assert.fail("unexpected url error");
                    signal.countDown();
                }
            }
        }, true);
    }

    private HashMap<String, List<String>> getHeader(String key, String value) {
        HashMap<String, List<String>> dummy = new HashMap<String, List<String>>();
        dummy.put(key, Arrays.asList(value, "s2", "s3"));
        return dummy;
    }
};
