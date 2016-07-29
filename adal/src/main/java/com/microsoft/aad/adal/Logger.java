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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.util.Log;

/**
 * Android log output can. If externalLogger is set, it will use that as well.
 * Usage: Logger.v(TAG, message, additionalMessage, errorCode) to log. Set
 * custom logger: Logger.setExternalLogger(..);
 */
public class Logger {

    private LogLevel mLogLevel;

    private static final String CUSTOM_LOG_ERROR = "Custom log failed to log message:%s";

    static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Log level.
     */
    public enum LogLevel {
        /**
         * Error.
         */
        Error(0),
        /**
         * Warning.
         */
        Warn(1),
        /**
         * Info.
         */
        Info(2),
        /**
         * Verbose.
         */
        Verbose(3),
        /**
         * Debug level only.
         * 
         * @deprecated
         */
        Debug(4);

        @SuppressWarnings("unused")
        private int mValue;

        LogLevel(int val) {
            mValue = val;
        }
    }

    /**
     * one callback logger.
     */
    private ILogger mExternalLogger = null;

    // enabled by default
    private boolean mAndroidLogEnabled = true;

    private static Logger sInstance = new Logger();

    private String mCorrelationId = null;

    /**
     * @return logger
     */
    public static Logger getInstance() {
        return sInstance;
    }

    Logger() {
        mLogLevel = LogLevel.Debug;
    }

    public interface ILogger {
        /**
         * Interface method for apps to get the log message.
         *
         * @param tag tag for the log message
         * @param message body of the log message
         * @param additionalMessage additional parameters
         * @param level log level of the message
         * @param errorCode ADAL error code being logged
         */
        void Log(String tag, String message, String additionalMessage, LogLevel level,
                ADALError errorCode);
    }

    /**
     * The log level being used by the logger.
     *
     * @return Log level being used
     */
    public LogLevel getLogLevel() {
        return mLogLevel;
    }

    /**
     * Set log level.
     *
     * @param level log level to set.
     */
    public void setLogLevel(LogLevel level) {
        this.mLogLevel = level;
    }

    /**
     * set custom logger.
     * 
     * @param customLogger reference of the ILogger interface to use
     */
    public void setExternalLogger(ILogger customLogger) {
        this.mExternalLogger = customLogger;
    }

    private static String addMoreInfo(String message) {
        if (message != null) {
            return getUTCDateTimeAsString() + "-" + getInstance().mCorrelationId + "-" + message
                    + " ver:" + AuthenticationContext.getVersionName();
        }

        return getUTCDateTimeAsString() + "-" + getInstance().mCorrelationId + "- ver:"
                + AuthenticationContext.getVersionName();
    }

    /**
     *
     * @param tag TAG for the log message
     * @param message Body of the message
     */
    public void debug(String tag, String message) {
        if (mLogLevel.compareTo(LogLevel.Debug) < 0 || StringExtensions.isNullOrBlank(message)) {
            return;
        }

        if (mAndroidLogEnabled) {
            Log.d(tag, message);
        }

        logCommon(tag, message, "", LogLevel.Info, null);
    }

    /**
     *
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode ADAL error code being logged
     */
    public void verbose(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mLogLevel.compareTo(LogLevel.Verbose) < 0) {
            return;
        }

        if (mAndroidLogEnabled) {
            Log.v(tag, getLogMessage(message, additionalMessage, errorCode));
        }

        logCommon(tag, message, additionalMessage, LogLevel.Verbose, errorCode);
    }

    /**
     *
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode ADAL error code being logged
     */
    public void inform(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mLogLevel.compareTo(LogLevel.Info) < 0) {
            return;
        }

        if (mAndroidLogEnabled) {
            Log.i(tag, getLogMessage(message, additionalMessage, errorCode));
        }

        logCommon(tag, message, additionalMessage, LogLevel.Info, errorCode);
    }

    /**
     *
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode ADAL error code being logged
     */
    public void warn(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mLogLevel.compareTo(LogLevel.Warn) < 0) {
            return;
        }

        if (mAndroidLogEnabled) {
            Log.w(tag, getLogMessage(message, additionalMessage, errorCode));
        }

        logCommon(tag, message, additionalMessage, LogLevel.Warn, errorCode);
    }

    /**
     *
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode ADAL error code being logged
     */
    public void error(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mAndroidLogEnabled) {
            Log.e(tag, getLogMessage(message, additionalMessage, errorCode));
        }

        logCommon(tag, message, additionalMessage, LogLevel.Error, errorCode);
    }

    /**
     *
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode ADAL error code being logged
     * @param err Exception being logged
     */
    public void error(String tag, String message, String additionalMessage, ADALError errorCode,
            Throwable err) {
        if (mAndroidLogEnabled) {
            Log.e(tag, getLogMessage(message, additionalMessage, errorCode), err);
        }

        logCommon(tag, message, additionalMessage, LogLevel.Error, errorCode, err);
    }

    private void logCommon(String tag, String message, String additionalMessage, LogLevel level,
            ADALError errorCode) {
        message = addMoreInfo(message);

        if (mExternalLogger != null) {
            try {
                mExternalLogger.Log(tag, message, additionalMessage, level, errorCode);
            } catch (Exception e) {
                // log message as warning to report callback error issue
                Log.w(tag, String.format(CUSTOM_LOG_ERROR, message));
            }
        }
    }

    private void logCommon(String tag, String message, String additionalMessage, LogLevel level,
                           ADALError errorCode, Throwable throwable) {
        StringBuilder msg = new StringBuilder();
        if (additionalMessage != null) {
            msg.append(additionalMessage);
        }
        if (throwable != null) {
            msg.append(' ').append(Log.getStackTraceString(throwable));
        }
        logCommon(tag, message, msg.toString(), level, errorCode);
    }


    private static String getLogMessage(String message, String additionalMessage,
            ADALError errorCode) {
        StringBuilder msg = new StringBuilder();
        if (errorCode != null) {
            msg.append(getCodeName(errorCode)).append(':');
        }
        if (message != null) {
            message = addMoreInfo(message);
            msg.append(message);
        }
        if (additionalMessage != null) {
            msg.append(' ').append(additionalMessage);
        }

        return msg.toString();
    }

    /**
     * Logs debug message.
     *
     * @param tag tag for the log message
     * @param message body of the log message
     */
    public static void d(String tag, String message) {
        Logger.getInstance().debug(tag, message);
    }

    /**
     * Logs informational message.
     *
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     */
    public static void i(String tag, String message, String additionalMessage) {
        Logger.getInstance().inform(tag, message, additionalMessage, null);
    }

    /**
     * Logs informational messages with error codes.
     *
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode ADAL error code being logged
     */
    public static void i(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().inform(tag, message, additionalMessage, errorCode);
    }

    /**
     * Logs verbose message.
     *
     * @param tag tag for the log message
     * @param message body of the log message
     */
    public static void v(String tag, String message) {
        Logger.getInstance().verbose(tag, message, null, null);
    }

    /**
     * Logs verbose message with error code.
     *
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode ADAL error code being logged
     */
    public static void v(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().verbose(tag, message, additionalMessage, errorCode);
    }

    /**
     * Logs warning message.
     *
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode ADAL error code being logged
     */
    public static void w(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().warn(tag, message, additionalMessage, errorCode);
    }

    /**
     * Logs error message.
     * @param tag tag for the log message
     * @param message body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode ADAL error code being logged
     */
    public static void e(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().error(tag, message, additionalMessage, errorCode);
    }

    /**
     * @param tag Tag for the log
     * @param message Message to add to the log
     * @param additionalMessage any additional parameters
     * @param errorCode ADAL error code
     * @param err Throwable
     */
    public static void e(String tag, String message, String additionalMessage, ADALError errorCode,
            Throwable err) {
        Logger.getInstance().error(tag, message, additionalMessage, errorCode, err);
    }

    /**
     * Sets the correlation id for the logger.
     *
     * @param correlation Correlation ID to be used
     */
    public static void setCorrelationId(UUID correlation) {
        Logger.getInstance().mCorrelationId = "";
        if (correlation != null) {
            Logger.getInstance().mCorrelationId = correlation.toString();
        }
    }

    /**
     *
     * @return True if log if enabled, False otherwise
     */
    public boolean isAndroidLogEnabled() {
        return mAndroidLogEnabled;
    }

    /**
     *
     * @param androidLogEnable True if log needs to be enables, false otherwise
     */
    public void setAndroidLogEnabled(boolean androidLogEnable) {
        this.mAndroidLogEnabled = androidLogEnable;
    }

    private static String getCodeName(ADALError code) {
        if (code != null) {
            return code.name();
        }

        return "";
    }

    @SuppressLint("SimpleDateFormat")
    private static String getUTCDateTimeAsString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());

    }

    /**
     *
     * @return the correlation id for the logger.
     */
    public String getCorrelationId() {
        return mCorrelationId;
    }
}
