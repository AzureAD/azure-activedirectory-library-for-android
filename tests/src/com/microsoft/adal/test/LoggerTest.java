
package com.microsoft.adal.test;

import com.microsoft.adal.Logger;
import com.microsoft.adal.ErrorCodes.ADALError;
import com.microsoft.adal.Logger.ILogger;
import com.microsoft.adal.Logger.LogLevel;

public class LoggerTest extends AndroidTestHelper {

    private static final String TAG = "DiscoveryTests";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    class TestResponse {
        String tag;

        String message;

        String additionalMessage;

        LogLevel level;

        ADALError errorCode;
    }

    public void testSetCallback() {

        final TestResponse response = new TestResponse();
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
        assertEquals("same log message", "testmessage", response.message);
        clearLogMessage(response);

        // set to warn
        response.tag = null;
        response.message = null;
        Logger.getInstance().setLogLevel(Logger.LogLevel.Warn);
        Logger.v("test", "testmessage", "additionalMessage", ADALError.AUTH_FAILED_BAD_STATE);

        assertNull("not logged", response.tag);
        assertNull("not logged", response.message);
    }

    private void verifyLogMessage(final TestResponse response) {
        assertEquals("same log tag", "test", response.tag);
        assertEquals("same log message", "testmessage", response.message);
        assertEquals("same log detail message", "additionalMessage", response.additionalMessage);
        assertEquals("same log error code", ADALError.AUTH_FAILED_BAD_STATE, response.errorCode);
        clearLogMessage(response);
    }

    private void clearLogMessage(final TestResponse response) {
        response.tag = null;
        response.message = null;
        response.additionalMessage = null;
        response.errorCode = null;
    }
}
