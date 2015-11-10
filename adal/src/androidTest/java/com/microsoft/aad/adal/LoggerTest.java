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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.Logger;
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

                response.tag = tag;
                response.message = message;
                response.additionalMessage = additionalMessage;
                response.level = level;
                response.errorCode = errorCode;
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

        assertEquals("same log tag", "test", response.tag);
        assertTrue("same log message", response.message.contains("testmessage"));
        response.reset();

        // set to warn
        response.tag = null;
        response.message = null;
        Logger.getInstance().setLogLevel(Logger.LogLevel.Warn);
        Logger.v("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("not logged", response.tag);
        assertNull("not logged", response.message);
    }

    public void testCallbackNullMessages() {

        final TestLogResponse response = new TestLogResponse();
        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                    ADALError errorCode) {
                response.tag = tag;
                response.message = message;
                response.additionalMessage = additionalMessage;
                response.level = level;
                response.errorCode = errorCode;
            }
        });

        // set to v
        Logger.getInstance().setLogLevel(Logger.LogLevel.Verbose);
        Logger.v(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.tag);
        assertTrue(response.message.contains(" ver:" + AuthenticationContext.getVersionName()));
        assertNull("null log detail message", response.additionalMessage);
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.errorCode);
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Debug);
        Logger.d("TAG", null);
        assertNull("null log tag since not logging this", response.tag);
        assertNull("null log message", response.message);
        assertNull("null log detail message", response.additionalMessage);
        response.reset();

        Logger.d(null, "someMessage234");
        assertNull("null log tag since not logging this", response.tag);
        assertTrue("log message", response.message.contains("someMessage234"));
        assertEquals("empty log detail message", "", response.additionalMessage);
        response.reset();

        Logger.d(null, null);

        assertNull("null log tag", response.tag);
        assertNull("null log message", response.message);
        assertNull("null log detail message", response.additionalMessage);
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Warn);
        Logger.w(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.tag);
        assertNotNull("log message has version", response.message);
        assertNull("null log detail message", response.additionalMessage);
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.errorCode);
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Info);
        Logger.i(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.tag);
        assertNotNull("log message", response.message);
        assertNull("null log detail message", response.additionalMessage);
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.errorCode);
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Error);
        Logger.e(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.tag);
        assertNotNull("null log message", response.message);
        assertNull("null log detail message", response.additionalMessage);
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.errorCode);
        response.reset();
    }

    public void testCallbackThrowsError() {

        final TestLogResponse response = new TestLogResponse();
        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                    ADALError errorCode) {
                response.message = message;
                throw new IllegalArgumentException(message);
            }
        });

        // set to v
        Logger.getInstance().setLogLevel(Logger.LogLevel.Verbose);
        UUID testId = UUID.randomUUID();
        Logger.setCorrelationId(testId);
        Logger.v(null, "testMessage", null, ADALError.AUTH_FAILED_BAD_STATE);

        assertTrue("Expected to come here", true);
        assertTrue("same log message", response.message.contains("testMessage") && response.message.contains(testId.toString()));
    }

    public void testLogMessage() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Method m = ReflectionUtils.getStaticTestMethod(Logger.class, "getLogMessage", String.class,
                String.class, ADALError.class);

        String msg = (String)m.invoke(null, "logMsg", "logAdditionalMsg", ADALError.AUTH_FAILED);

        assertTrue("Verify msg", msg.startsWith(ADALError.AUTH_FAILED.name()));

        msg = (String)m.invoke(null, "logMsg", null, ADALError.AUTH_FAILED);

        assertTrue("Verify message", msg.contains(ADALError.AUTH_FAILED.name()));
    }

    private void verifyLogMessage(final TestLogResponse response) {
        assertEquals("same log tag", "test", response.tag);
        assertTrue("same log message", response.message.contains("testmessage"));
        assertEquals("same log detail message", "additionalMessage", response.additionalMessage);
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.errorCode);
        response.reset();
    }
}
