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

import android.annotation.SuppressLint;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Android log output can. If externalLogger is set, it will use that as well.
 * Usage: Logger.v(tag, message, additionalMessage, errorCode) to log. Set
 * custom logger: Logger.setExternalLogger(..);
 */
public class Logger {
    // Turn on the verbose level logging by default.
    private LogLevel mLogLevel  = LogLevel.Verbose;

    private static final String CUSTOM_LOG_ERROR = "Custom log failed to log message:%s";

    static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * one callback logger.
     */
    private AtomicReference<ILogger> mExternalLogger = new AtomicReference<>(null);;

    // enabled by default
    private boolean mLogcatLogEnabled = false;

    private static Logger sInstance = new Logger();

    private String mCorrelationId = null;

    // Disable to log PII by default.
    private boolean mEnablePII = false;

    /**
     * Enum class for LogLevel that the sdk recognizes.
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
     * @return The single instance of {@link Logger}.
     */
    public static Logger getInstance() {
        return sInstance;
    }

    /**
     * Interface for apps to configure the external logging.
     */
    public interface ILogger {
        /**
         * Interface method for apps to hand off each log message as it's generated.
         *
         * @param tag                 The TAG for the log message.
         * @param message             The detailed message. Will not contain any PII info.
         * @param additionalMessage   The additional message.
         * @param level               The {@link Logger.LogLevel} for the generated message.
         * @param errorCode           The error code.
         */
        void Log(String tag, String message, String additionalMessage, LogLevel level,
                ADALError errorCode);
    }

    public LogLevel getLogLevel() {
        return mLogLevel;
    }

    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level logging.
     * @param logLevel The {@link LogLevel} to be enabled for the diagnostic logging.
     */
    public void setLogLevel(final LogLevel logLevel) {
        this.mLogLevel = logLevel;
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk enables it.
     *
     * @param enableLogcatLog True if enabling the logcat logging, false otherwise.
     */
    public void setEnableLogcatLog(final boolean enableLogcatLog) {
        mLogcatLogEnabled = enableLogcatLog;
    }

    /**
     * Enable log message with PII (personal identifiable information) info. By default, ADAL doesn't log any PII.
     * @param enablePII True if enabling PII info to be logged, false otherwise.
     */
    public void setEnablePII(final boolean enablePII) {
        mEnablePII = enablePII;
    }

    /**
     * Gets the state of the PII/OII enable flag.
     * @return true if the PII?OII is allowed. False otherwise.
     */
    public boolean getEnablePII() {
        return mEnablePII;
    }

    /**
     * Set the custom logger. Configures external logging to configure a callback that
     * the sdk will use to pass each log message. Overriding the logger callback is not allowed.
     *
     * @param externalLogger The reference to the {@link ILogger} that can
     *                       output the logs to the designated places.
     * @throws IllegalStateException if external logger is already set, and the caller is trying to set it again.
     */
    public void setExternalLogger(ILogger externalLogger) {
        if (externalLogger == null) {
            return;
        }

        if (mExternalLogger.get() != null) {
            throw new IllegalStateException("External logger is already set, cannot be set again.");
        }

        mExternalLogger.set(externalLogger);
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

        if (mLogcatLogEnabled) {
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

        if (mLogcatLogEnabled) {
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

        if (mLogcatLogEnabled) {
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

        if (mLogcatLogEnabled) {
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
        if (mLogcatLogEnabled) {
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
        if (mLogcatLogEnabled) {
            Log.e(tag, getLogMessage(message, additionalMessage, errorCode), err);
        }

        logCommon(tag, message, additionalMessage, LogLevel.Error, errorCode, err);
    }

    private void logCommon(String tag, String message, String additionalMessage, LogLevel level,
                           ADALError errorCode) {
        message = addMoreInfo(message);

        if (mExternalLogger.get() != null) {
            try {
                if (mEnablePII) {
                    mExternalLogger.get().Log(tag, message, additionalMessage, level, errorCode);
                } else {
                    mExternalLogger.get().Log(tag, message, "", level, errorCode);
                }
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
    /**
     * Send logs to logcat as the default logging if developer doesn't turn off the logcat logging.
     */
    private String getLogMessage(String message, String additionalMessage,
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

    public static void w(String tag, String msg) {
        Logger.getInstance().warn(tag, msg, null, null);
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

    public static void e(String tag, String msg, String additionalMsg, Throwable tr) {
        Logger.getInstance().error(tag, msg, additionalMsg, null, tr);
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
    public boolean isLogcatLogEnabled() {
        return mLogcatLogEnabled;
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
        final String utcTime = dateFormat.format(new Date());

        return utcTime;
    }

    /**
     *
     * @return the correlation id for the logger.
     */
    public String getCorrelationId() {
        return mCorrelationId;
    }
}
