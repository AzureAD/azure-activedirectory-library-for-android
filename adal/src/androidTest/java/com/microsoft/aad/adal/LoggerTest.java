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
import java.util.UUID;

import com.microsoft.aad.adal.Logger.ILogger;
import com.microsoft.aad.adal.Logger.LogLevel;

public class LoggerTest extends AndroidTestHelper {

    private static final String TAG = "DiscoveryTests";

    public void testSetCallback() {

        final TestLogResponse response = new TestLogResponse();
        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                            ADALError errorCode) {

                response.setTag(tag);
                response.setMessage(message);
                response.setAdditionalMessage(additionalMessage);
                response.setLevel(level);
                response.setErrorCode(errorCode);
            }
        });

        // set to v
        Logger.getInstance().setLogLevel(Logger.LogLevel.Verbose);
        Logger.v("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        verifyLogMessage(response);

        Logger.getInstance().setLogLevel(Logger.LogLevel.Debug);
        Logger.e("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        verifyLogMessage(response);

        Logger.getInstance().setLogLevel(Logger.LogLevel.Error);
        Logger.e("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        verifyLogMessage(response);

        Logger.getInstance().setLogLevel(Logger.LogLevel.Debug);
        Logger.d("test", "testmessage");

        assertEquals("same log tag", "test", response.getTag());
        assertTrue("same log message", response.getMessage().contains("testmessage"));
        response.reset();

        // set to warn
        response.setTag(null);
        response.setMessage(null);
        Logger.getInstance().setLogLevel(Logger.LogLevel.Warn);
        Logger.v("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("not logged", response.getTag());
        assertNull("not logged", response.getMessage());
    }

    public void testCallbackNullMessages() {

        final TestLogResponse response = new TestLogResponse();
        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                            ADALError errorCode) {
                response.setTag(tag);
                response.setMessage(message);
                response.setAdditionalMessage(additionalMessage);
                response.setLevel(level);
                response.setErrorCode(errorCode);
            }
        });

        // set to v
        Logger.getInstance().setLogLevel(Logger.LogLevel.Verbose);
        Logger.v(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.getTag());
        assertTrue(response.getMessage().contains(" ver:" + AuthenticationContext.getVersionName()));
        assertNull("null log detail message", response.getAdditionalMessage());
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.getErrorCode());
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Debug);
        Logger.d("TAG", null);
        assertNull("null log tag since not logging this", response.getTag());
        assertNull("null log message", response.getMessage());
        assertNull("null log detail message", response.getAdditionalMessage());
        response.reset();

        Logger.d(null, "someMessage234");
        assertNull("null log tag since not logging this", response.getTag());
        assertTrue("log message", response.getMessage().contains("someMessage234"));
        assertEquals("empty log detail message", "", response.getAdditionalMessage());
        response.reset();

        Logger.d(null, null);

        assertNull("null log tag", response.getTag());
        assertNull("null log message", response.getMessage());
        assertNull("null log detail message", response.getAdditionalMessage());
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Warn);
        Logger.w(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.getTag());
        assertNotNull("log message has version", response.getMessage());
        assertNull("null log detail message", response.getAdditionalMessage());
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.getErrorCode());
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Info);
        Logger.i(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.getTag());
        assertNotNull("log message", response.getMessage());
        assertNull("null log detail message", response.getAdditionalMessage());
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.getErrorCode());
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Error);
        Logger.e(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.getTag());
        assertNotNull("null log message", response.getMessage());
        assertNull("null log detail message", response.getAdditionalMessage());
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.getErrorCode());
        response.reset();
    }

    public void testCallbackThrowsError() {

        final TestLogResponse response = new TestLogResponse();
        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                            ADALError errorCode) {
                response.setMessage(message);
                throw new IllegalArgumentException(message);
            }
        });

        // set to v
        Logger.getInstance().setLogLevel(Logger.LogLevel.Verbose);
        UUID testId = UUID.randomUUID();
        Logger.setCorrelationId(testId);
        Logger.v(null, "testMessage", null, ADALError.AUTH_FAILED_BAD_STATE);

        assertTrue("Expected to come here", true);
        assertTrue("same log message", response.getMessage().contains("testMessage") && response.getMessage().contains(testId.toString()));
    }

    public void testLogMessage() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Method m = ReflectionUtils.getStaticTestMethod(Logger.class, "getLogMessage", String.class,
                String.class, ADALError.class);

        String msg = (String) m.invoke(null, "logMsg", "logAdditionalMsg", ADALError.AUTH_FAILED);

        assertTrue("Verify msg", msg.startsWith(ADALError.AUTH_FAILED.name()));

        msg = (String) m.invoke(null, "logMsg", null, ADALError.AUTH_FAILED);

        assertTrue("Verify message", msg.contains(ADALError.AUTH_FAILED.name()));
    }

    private void verifyLogMessage(final TestLogResponse response) {
        assertEquals("same log tag", "test", response.getTag());
        assertTrue("same log message", response.getMessage().contains("testmessage"));
        assertEquals("same log detail message", "additionalMessage", response.getAdditionalMessage());
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.getErrorCode());
        response.reset();
    }
}
