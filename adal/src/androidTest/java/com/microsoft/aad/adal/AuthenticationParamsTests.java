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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.microsoft.aad.adal.AuthenticationParameters.AuthenticationParamCallback;
import com.microsoft.aad.adal.Logger.ILogger;
import com.microsoft.aad.adal.Logger.LogLevel;
import android.util.Log;
import junit.framework.Assert;
import org.mockito.Mockito;

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

    public void testCreateFromResourceUrlInvalidFormat() throws IOException {
        Log.d(TAG, "test:" + getName() + "thread:" + android.os.Process.myTid());

        //mock http response
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(
                Util.createInputStream(Util.getSuccessTokenResponse(false, false)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        final TestResponse testResponse = new TestResponse();
        setupAsyncParamRequest("http://www.cnn.com", testResponse);

        assertNotNull("Exception null", testResponse.getException());
        assertNull("Parameter is not null", testResponse.getParam());
        assertTrue(
                "Check header exception",
                testResponse.getException().getMessage() == AuthenticationParameters.AUTH_HEADER_WRONG_STATUS);
    }

    public void testCreateFromResponseAuthenticateHeader() {
        assertThrowsException(ResourceAuthenticationChallengeException.class,
                AuthenticationParameters.AUTH_HEADER_MISSING.toLowerCase(), new ThrowableRunnable() {

                    @Override
                    public void run() throws ResourceAuthenticationChallengeException {
                        AuthenticationParameters.createFromResponseAuthenticateHeader(null);
                    }
                });

        // empty value inside the authorization_uri will throw exception
        assertThrowsException(ResourceAuthenticationChallengeException.class,
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT.toLowerCase(), new ThrowableRunnable() {

                    @Override
                    public void run() throws ResourceAuthenticationChallengeException {
                        AuthenticationParameters
                                .createFromResponseAuthenticateHeader("Bearer\t resource=\"is=outer, space=ornot\",\t\t  authorization_uri=\"\"");
                    }
                });
    }

    /**
     * test external service deployed at Azure
     */
    public void testCreateFromResourceUrlPositive() throws IOException {
        Log.d(TAG, "test:" + getName() + "thread:" + android.os.Process.myTid());

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        final String response = "Bearer authorization_uri=\"https://login.windows.net/test.onmicrosoft.com\", resource=\"testresource\"";
        Mockito.when(mockedConnection.getInputStream()).thenReturn(
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(
                HttpURLConnection.HTTP_UNAUTHORIZED);

        Mockito.when(mockedConnection.getHeaderFields()).thenReturn(Collections.singletonMap(
                AuthenticationParameters.AUTHENTICATE_HEADER, Collections.singletonList(response)));

        final TestResponse testResponse = new TestResponse();
        setupAsyncParamRequest("https://testapi007.azurewebsites.net/api/WorkItem", testResponse);

        assertNull("Exception is not null", testResponse.getException());
        assertNotNull("Check parameter", testResponse.getParam());
        Log.d(TAG, "test:" + getName() + "authority:" + testResponse.getParam().getAuthority());
        assertEquals("https://login.windows.net/test.onmicrosoft.com", testResponse.getParam()
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

        assertTrue("Has warning for redudant items", callback.isCalled());
        Logger.getInstance().setExternalLogger(null);
    }

    private void verifyAuthenticationParam(Method m, String headerValue, String authorizationUri,
                                           String resource) throws IllegalAccessException, InvocationTargetException {
        AuthenticationParameters param = (AuthenticationParameters) m.invoke(null,
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", headerValue)));
        assertNotNull("Parsed ok", param);
        assertEquals("Verify authorization uri", authorizationUri, param.getAuthority());
        assertEquals("Verify resource", resource, param.getResource());
    }

    /**
     * test private method to make sure parsing is right
     */
    public void testParseResponseNegative() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        callParseResponseForException(new HttpWebResponse(HttpURLConnection.HTTP_OK, null, null),
                AuthenticationParameters.AUTH_HEADER_WRONG_STATUS);

        callParseResponseForException(new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, null),
                AuthenticationParameters.AUTH_HEADER_MISSING);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", "v")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", "Bearer nonsense")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", "Bearer")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", "Bearer ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", " Bearer")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", " Bearer ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", "\t Bearer  ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", "Bearer test ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", "Bear gets=honey ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate", "Bearer =,=,")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate",
                        "Bearer some text here,")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate",
                        "Bearer authorization_uri= ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate",
                        "Bearerauthorization_uri=something")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate",
                        "Bearerauthorization_uri=\"https://www.something.com\"")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);

        callParseResponseForException(
                new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, getHeader("WWW-Authenticate",
                        "Bearer    \t authorization_uri=,something=a ")),
                AuthenticationParameters.AUTH_HEADER_INVALID_FORMAT);
    }

    class LogCallback implements ILogger {
        private boolean mCalled = false;

        private ADALError mCheckCode;

        public LogCallback(ADALError errorCode) {
            mCheckCode = errorCode;
            mCalled = false;
        }

        @Override
        public void Log(String tag, String message, String additionalMessage, LogLevel level, ADALError errorCode) {
            if (errorCode == mCheckCode) {
                mCalled = true;
            }
        }

        public boolean isCalled() {
            return mCalled;
        }
    }

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
            param = (AuthenticationParameters) m.invoke(null, response);
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
        private AuthenticationParameters mParam;

        private Exception mException;

        public AuthenticationParameters getParam() {
            return mParam;
        }

        public void setParam(AuthenticationParameters param) {
            this.mParam = param;
        }

        public Exception getException() {
            return mException;
        }

        public void setException(Exception exception) {
            this.mException = exception;
        }
    }

    /**
     * This will call createFromResourceUrl for given url and update the
     * testresponse obj it helps to wait for the callback and handles
     * interruptions
     */
    private void setupAsyncParamRequest(final String requestUrl, final TestResponse testResponse) {
        final CountDownLatch signal = new CountDownLatch(1);
        final AuthenticationParamCallback callback = new AuthenticationParamCallback() {
            @Override
            public void onCompleted(Exception exception, AuthenticationParameters param) {
                testResponse.setParam(param);
                testResponse.setException(exception);
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
