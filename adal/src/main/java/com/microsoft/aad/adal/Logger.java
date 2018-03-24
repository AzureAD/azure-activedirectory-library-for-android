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

import android.util.Log;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.CommonCoreLogger;
import com.microsoft.identity.common.internal.logging.ILoggerCallback;
import com.microsoft.identity.common.internal.logging.LoggerSettings;

import java.util.UUID;

/**
 * Android log output can. If externalLogger is set, it will use that as well.
 * Usage: Logger.v(tag, message, additionalMessage, errorCode) to log. Set
 * custom logger: Logger.setExternalLogger(..);
 */
public class Logger {
    private static Logger sINSTANCE = new Logger();

    private ILogger mExternalLogger = null;

    private String mCorrelationId = null;

    /**
     * @return The single instance of {@link Logger}.
     */
    public static Logger getInstance() {
        return sINSTANCE;
    }


    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level logging.
     *
     * @param logLevel The {@link LogLevel} to be enabled for the diagnostic logging.
     */
    public void setLogLevel(final LogLevel logLevel) {
        switch (logLevel) {
            case Error:
                CommonCoreLogger.getInstance()
                        .setLogLevel(CommonCoreLogger.LogLevel.ERROR);
                break;
            case Warn:
                CommonCoreLogger.getInstance()
                        .setLogLevel(CommonCoreLogger.LogLevel.WARN);
                break;
            case Info:
                CommonCoreLogger.getInstance()
                        .setLogLevel(CommonCoreLogger.LogLevel.INFO);
                break;
            case Verbose:
                CommonCoreLogger.getInstance()
                        .setLogLevel(CommonCoreLogger.LogLevel.VERBOSE);
                break;
            case Debug:
                //The debug level is deprecated and removed in common core.
                CommonCoreLogger.getInstance()
                        .setLogLevel(CommonCoreLogger.LogLevel.INFO);
                break;
            default:
                throw new IllegalArgumentException("Unknown logLevel");
        }
    }

    /**
     * Set the custom logger. Configures external logging to configure a callback that
     * the sdk will use to pass each log message. Overriding the logger callback is not allowed.
     *
     * @param externalLogger The reference to the {@link ILogger} that can
     *                       output the logs to the designated places.
     *
     */
    public synchronized void setExternalLogger(ILogger externalLogger) {
        CommonCoreLogger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(String tag, CommonCoreLogger.LogLevel logLevel, String message, boolean containsPII) {
                if (mExternalLogger != null) {
                    if (!LoggerSettings.getInstance().getAllowPii() && containsPII) {
                        return;
                    } else {
                        switch (logLevel) {
                            case ERROR:
                                Log.d("Heidi", "The common core callback is called");
                                mExternalLogger.Log(tag, message, null, LogLevel.Error, null);
                                break;
                            case WARN:
                                Log.d("Heidi", "The common core callback is called");
                                mExternalLogger.Log(tag, message, null, LogLevel.Warn, null);
                                break;
                            case VERBOSE:
                                Log.d("Heidi", "The common core callback is called");
                                mExternalLogger.Log(tag, message, null, LogLevel.Verbose, null);
                                break;
                            case INFO:
                                Log.d("Heidi", "The common core callback is called");
                                mExternalLogger.Log(tag, message, null, LogLevel.Info, null);
                                break;
                            default:
                                Log.d("Heidi", "The common core callback is called");
                                throw new IllegalArgumentException("Unknown logLevel");
                        }
                    }
                }
            }
        });

        mExternalLogger = externalLogger;
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk disables it.
     *
     * @param androidLogEnabled True if enabling the logcat logging, false otherwise.
     */
    public void setAndroidLogEnabled(final boolean androidLogEnabled) {
        LoggerSettings.getInstance().setAllowLogcat(androidLogEnabled);
    }

    /**
     * ADAL provides logging callbacks that assist in diagnostics. The callback has two parameters,
     * message and additionalMessage. All user information is put into additionalMessage.
     * ADAL will clear this data unless the {@link #mEnablePII} is called with true.
     * By default the library will not return any messages with user information in them.
     *
     * @param enablePII True if enabling PII info to be logged, false otherwise.
     */
    public void setEnablePII(final boolean enablePII) {
        LoggerSettings.getInstance().setAllowPii(enablePII);
    }

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
     * Interface for apps to configure the external logging.
     */
    public interface ILogger {
        /**
         * Interface method for apps to hand off each log message as it's generated.
         *
         * @param tag               The TAG for the log message.
         * @param message           The detailed message. Will not contain any PII info.
         * @param additionalMessage The additional message.
         * @param level             The {@link Logger.LogLevel} for the generated message.
         * @param errorCode         The error code.
         */
        void Log(String tag, String message, String additionalMessage, LogLevel level,
                 ADALError errorCode);
    }

    private void commonCoreWrapper(String tag, String message, String additionalMessage, LogLevel logLevel,
                                   ADALError errorCode, Throwable throwable){
        switch (logLevel) {
            case Error:
                if (!StringExtensions.isNullOrBlank(message)) {
                    CommonCoreLogger.error(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + message, null);
                }

                if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                    CommonCoreLogger.errorPII(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + additionalMessage, throwable);
                }
                break;
            case Warn:
                if (!StringExtensions.isNullOrBlank(message)) {
                    CommonCoreLogger.warn(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + message);
                }

                if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                    CommonCoreLogger.warnPII(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + additionalMessage);
                }
                break;
            case Info:
                if (!StringExtensions.isNullOrBlank(message)) {
                    CommonCoreLogger.info(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + message);
                }

                if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                    CommonCoreLogger.infoPII(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + additionalMessage);
                }
                break;
            case Verbose:
                if (!StringExtensions.isNullOrBlank(message)) {
                    CommonCoreLogger.verbose(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + message);
                }

                if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                    CommonCoreLogger.verbosePII(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + additionalMessage);
                }
                break;
            case Debug:
                //The debug level is deprecated and removed in common core.
                CommonCoreLogger.info(tag, Logger.getInstance().mCorrelationId, message);
                break;
            default:
                throw new IllegalArgumentException("Unknown logLevel");
        }
    }

    /**
     * Logs debug message.
     *
     * @param tag     tag for the log message
     * @param message body of the log message
     */
    public static void d(String tag, String message) {
        if (StringExtensions.isNullOrBlank(message)) {
            return;
        }

        Logger.getInstance().commonCoreWrapper(tag, message, null, LogLevel.Debug, null, null);
    }

    /**
     * Logs informational message.
     *
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     */
    public static void i(String tag, String message, String additionalMessage) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Info, null, null);
    }

    /**
     * Logs informational messages with error codes.
     *
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode         ADAL error code being logged
     */
    public static void i(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Info, errorCode, null);
    }

    /**
     * Logs verbose message.
     *
     * @param tag     tag for the log message
     * @param message body of the log message
     */
    public static void v(String tag, String message) {
        Logger.getInstance().commonCoreWrapper(tag, message, null, LogLevel.Verbose, null, null);
    }

    /**
     * Logs verbose message with error code.
     *
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode         ADAL error code being logged
     */
    public static void v(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Verbose, errorCode, null);
    }

    /**
     * Logs warning message.
     *
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode         ADAL error code being logged
     */
    public static void w(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Warn, errorCode, null);
    }

    /**
     * Logs warning message.
     *
     * @param tag     tag for the log message
     * @param message body of the log message
     */
    public static void w(String tag, String message) {
        Logger.getInstance().commonCoreWrapper(tag, message, null, LogLevel.Warn, null, null);
    }

    /**
     * Logs error message.
     *
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode         ADAL error code being logged
     */
    public static void e(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Error, errorCode, null);
    }

    /**
     * Logs error message.
     *
     * @param tag               Tag for the log
     * @param message           Message to add to the log
     * @param additionalMessage any additional parameters
     * @param errorCode         ADAL error code
     * @param throwable         Throwable
     */
    public static void e(String tag, String message, String additionalMessage, ADALError errorCode,
                         Throwable throwable) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Error, errorCode, throwable);
    }

    /**
     * Logs error message.
     *
     * @param tag       Tag for the log
     * @param message   Message to add to the log
     * @param throwable Throwable
     */
    public static void e(String tag, String message, Throwable throwable) {
        Logger.getInstance().commonCoreWrapper(tag, message, null, LogLevel.Error, null, throwable);
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
     * @return the correlation id for the logger.
     */
    public String getCorrelationId() {
        return mCorrelationId;
    }
}
