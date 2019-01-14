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

import android.support.test.runner.AndroidJUnit4;

import com.microsoft.aad.adal.Logger.ILogger;
import com.microsoft.aad.adal.Logger.LogLevel;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LoggerTest extends AndroidTestHelper {

    private static final String TAG = LoggerTest.class.getSimpleName();

    @Test
    public void testSetCallback() {

        final List<TestLogResponse> logResponses = new ArrayList<>();
        int index = 0;
        Logger.getInstance().setEnablePII(true);
        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                            ADALError errorCode) {
                TestLogResponse response = new TestLogResponse();
                response.setTag(tag);
                response.setMessage(message);
                response.setAdditionalMessage(additionalMessage);
                response.setLevel(level);
                response.setErrorCode(errorCode);
                logResponses.add(response);
            }
        });

        // set to v
        Logger.getInstance().setLogLevel(Logger.LogLevel.Verbose);
        Logger.v("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        verifyLogMessage(logResponses);
        logResponses.clear();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Debug);
        Logger.e("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        verifyLogMessage(logResponses);
        logResponses.clear();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Error);
        Logger.e("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        verifyLogMessage(logResponses);
        logResponses.clear();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Debug);
        Logger.d("test", "testmessage");

        assertEquals("same log tag", "test", logResponses.get(0).getTag());
        assertTrue("same log message", logResponses.get(0).getMessage().contains("testmessage"));
        logResponses.clear();

        // set to warn
        Logger.getInstance().setLogLevel(Logger.LogLevel.Warn);
        Logger.v("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        assertEquals(0, logResponses.size());
    }

    @Test
    public void testCallbackNullMessages() {

        final TestLogResponse response = new TestLogResponse();
        Logger.getInstance().setEnablePII(true);
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

        Logger.getInstance().setLogLevel(Logger.LogLevel.Debug);

        Logger.d(null, "someMessage234");
        assertNull("null log tag since not logging this", response.getTag());
        assertTrue("log message", response.getMessage().contains("someMessage234"));
        assertNull("null log detail message", response.getAdditionalMessage());
        response.reset();

        Logger.d(null, null);

        assertNull("null log tag", response.getTag());
        assertNull("null log message", response.getMessage());
        assertNull("null log detail message", response.getAdditionalMessage());
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Warn);
        Logger.w(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.getTag());
        assertNull("null log detail message", response.getAdditionalMessage());
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Info);
        Logger.i(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.getTag());
        assertNull("null log detail message", response.getAdditionalMessage());
        response.reset();

        Logger.getInstance().setLogLevel(Logger.LogLevel.Error);
        Logger.e(null, null, null, ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("null log tag", response.getTag());
        assertNull("null log detail message", response.getAdditionalMessage());
        response.reset();
    }

    @Test
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

    private void verifyLogMessage(final List<TestLogResponse> responses) {
        for (final TestLogResponse response : responses) {
            assertEquals("same log tag", "test", response.getTag());
            assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.getErrorCode());
        }

        assertTrue("same log message", responses.get(0).getMessage().contains("testmessage"));
        assertTrue("same log detail message", responses.get(1).getMessage().contains("additionalMessage"));
    }
}
