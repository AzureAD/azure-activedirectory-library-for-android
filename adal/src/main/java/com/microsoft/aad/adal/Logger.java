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

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Android log output can. If externalLogger is set, it will use that as well.
 * Usage: Logger.v(tag, message, additionalMessage, errorCode) to log. Set
 * custom logger: Logger.setExternalLogger(..);
 */
public class Logger {
    private static Logger sINSTANCE = new Logger();
    static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Turn on the verbose level logging by default.
    private LogLevel mLogLevel = LogLevel.Verbose;
    private ILogger mExternalLogger = null;
    private static final String CUSTOM_LOG_ERROR = "Custom log failed to log message:%s";
    private boolean mAndroidLogEnabled = false;
    private String mCorrelationId = null;

    // Disable to log PII by default.
    private boolean mEnablePII = false;
    private static boolean mEnableCommonCoreLog = false;

    /**
     * @return The single instance of {@link Logger}.
     */
    public static Logger getInstance() {
        return sINSTANCE;
    }

    /**
     * Enable/Disable the Common-Core logging. By default, the sdk disables it.
      * @param commonCoreLogEnabled True if enabling the Common-Core logging, false otherwise.
     */
    public void setEnableCommonCoreLog(final boolean commonCoreLogEnabled) {
        mEnableCommonCoreLog = commonCoreLogEnabled;
    }

    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level logging.
     *
     * @param logLevel The {@link LogLevel} to be enabled for the diagnostic logging.
     */
    public void setLogLevel(final LogLevel logLevel) {
        if (mEnableCommonCoreLog) {
            switch (logLevel) {
                case Error:
                    CommonCoreLogger.getInstance().setLogLevel(CommonCoreLogger.LogLevel.ERROR);
                    break;
                case Warn:
                    CommonCoreLogger.getInstance().setLogLevel(CommonCoreLogger.LogLevel.WARN);
                    break;
                case Info:
                    CommonCoreLogger.getInstance().setLogLevel(CommonCoreLogger.LogLevel.INFO);
                    break;
                case Verbose:
                    CommonCoreLogger.getInstance().setLogLevel(CommonCoreLogger.LogLevel.VERBOSE);
                    break;
                case Debug:
                    //The debug level is deprecated and removed in common core.
                    CommonCoreLogger.getInstance().setLogLevel(CommonCoreLogger.LogLevel.VERBOSE);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown logLevel");
            }
        }

        this.mLogLevel = logLevel;
    }

    /**
     * Set the custom logger. Configures external logging to configure a callback that
     * the sdk will use to pass each log message. Overriding the logger callback is not allowed.
     *
     * @param externalLogger The reference to the {@link ILogger} that can
     *                       output the logs to the designated places.
     */
    public synchronized void setExternalLogger(ILogger externalLogger) {
        mExternalLogger = externalLogger;
    }

    /**
     * Adapter API in ADAL to set the custom logger in the use of Common-Core.
     * @param externalLogger The reference to the {@link ILoggerCallback} that can
     *                       output the logs to the designated places.
     */
    public synchronized void setCommonCoreExternalLogger(ILoggerCallback externalLogger) {
        CommonCoreLogger.getInstance().setExternalLogger(externalLogger);

        //If the dev calls setCommonCoreExternalLogger, it is an explicit enable-flag for common-core logging.
        setEnableCommonCoreLog(true);
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk disables it.
     *
     * @param androidLogEnabled True if enabling the logcat logging, false otherwise.
     */
    public void setAndroidLogEnabled(final boolean androidLogEnabled) {
        if (mEnableCommonCoreLog) {
            CommonCoreLogger.getInstance().setLogcatLogEnabled(androidLogEnabled);
        }

        mAndroidLogEnabled = androidLogEnabled;
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
        if (mEnableCommonCoreLog) {
            CommonCoreLogger.getInstance().setPIIEnabled(enablePII);
        }

        mEnablePII = enablePII;
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

    /**
     * Send logs to logcat as the default logging if developer doesn't turn off the logcat logging.
     */
    private void sendLogcatLogs(final String tag, final LogLevel logLevel, final String message) {
        // Append additional message to the message part for logcat logging
        switch (logLevel) {
            case Error:
                Log.e(tag, message);
                break;
            case Warn:
                Log.w(tag, message);
                break;
            case Info:
                Log.i(tag, message);
                break;
            case Verbose:
                Log.v(tag, message);
                break;
            case Debug:
                Log.d(tag, message);
                break;
            default:
                throw new IllegalArgumentException("Unknown loglevel");
        }
    }

    private static String addMoreInfo(String message) {
        if (!StringExtensions.isNullOrBlank(message)) {
            return getUTCDateTimeAsString() + "-" + getInstance().mCorrelationId + "-" + message
                    + " ver:" + AuthenticationContext.getVersionName();
        }

        return getUTCDateTimeAsString() + "-" + getInstance().mCorrelationId + "- ver:"
                + AuthenticationContext.getVersionName();
    }

    /**
     * Format the log message. Depends on the developer setting, the log message could be sent to logcat
     * or the external logger set by the calling app.
     */
    private void log(String tag, String message, String additionalMessage, LogLevel logLevel,
                     ADALError errorCode, Throwable throwable) {
        if (logLevel.compareTo(mLogLevel) > 0) {
            return;
        }

        final StringBuilder logMessage = new StringBuilder();

        if (errorCode != null) {
            logMessage.append(getCodeName(errorCode)).append(':');
        }

        logMessage.append(addMoreInfo(message));

        // Developer turns off PII logging, if the log message contains any PII, we shouldn't send it.
        if (!StringExtensions.isNullOrBlank(additionalMessage) && mEnablePII) {
            logMessage.append(' ').append(additionalMessage);
        }

        // Adding stacktrace to message
        if (throwable != null) {
            logMessage.append('\n').append(Log.getStackTraceString(throwable));
        }

        if (mAndroidLogEnabled) {
            sendLogcatLogs(tag, logLevel, logMessage.toString());
        }

        if (mExternalLogger != null) {
            try {
                if (!StringExtensions.isNullOrBlank(additionalMessage) && mEnablePII) {
                    mExternalLogger.Log(tag, addMoreInfo(message), additionalMessage + (throwable == null ? "" : Log.getStackTraceString(throwable)), logLevel, errorCode);
                } else {
                    mExternalLogger.Log(tag, addMoreInfo(message), throwable == null ? null : Log.getStackTraceString(throwable), logLevel, errorCode);
                }
            } catch (Exception e) {
                // log message as warning to report callback error issue
                Log.w(tag, String.format(CUSTOM_LOG_ERROR, message));
            }
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

        if (mEnableCommonCoreLog) {
            CommonCoreLogger.verbose(tag, Logger.getInstance().mCorrelationId, message, null);
        } else {
            Logger.getInstance().log(tag, message, null, LogLevel.Debug, null, null);
        }
    }

    /**
     * Logs informational message.
     *
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     */
    public static void i(String tag, String message, String additionalMessage) {
        if (mEnableCommonCoreLog) {
            if (!StringExtensions.isNullOrBlank(message)) {
                CommonCoreLogger.info(tag, Logger.getInstance().getCorrelationId(), message, null);
            }

            if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                CommonCoreLogger.infoPII(tag, Logger.getInstance().getCorrelationId(), additionalMessage, null);
            }
        } else {
            Logger.getInstance().log(tag, message, additionalMessage, LogLevel.Info, null, null);
        }
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
        if (mEnableCommonCoreLog) {
            if (!StringExtensions.isNullOrBlank(message)) {
                CommonCoreLogger.info(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + message,
                        null);
            }

            if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                CommonCoreLogger.infoPII(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + additionalMessage,
                        null);
            }
        } else {
            Logger.getInstance().log(tag, message, additionalMessage, LogLevel.Info, errorCode, null);
        }
    }

    /**
     * Logs verbose message.
     *
     * @param tag     tag for the log message
     * @param message body of the log message
     */
    public static void v(String tag, String message) {
        if (mEnableCommonCoreLog) {
            CommonCoreLogger.verbose(tag, Logger.getInstance().mCorrelationId, message, null);
        } else {
            Logger.getInstance().log(tag, message, null, LogLevel.Verbose, null, null);
        }
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
        if (mEnableCommonCoreLog) {
            if (!StringExtensions.isNullOrBlank(message)) {
                CommonCoreLogger.verbose(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + message,
                        null);
            }

            if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                CommonCoreLogger.verbosePII(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + additionalMessage,
                        null);
            }
        } else {
            Logger.getInstance().log(tag, message, additionalMessage, LogLevel.Verbose, errorCode, null);
        }
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
        if (mEnableCommonCoreLog) {
            if (!StringExtensions.isNullOrBlank(message)) {
                CommonCoreLogger.warn(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + message,
                        null);
            }

            if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                CommonCoreLogger.warnPII(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + additionalMessage,
                        null);
            }
        } else {
            Logger.getInstance().log(tag, message, additionalMessage, LogLevel.Warn, errorCode, null);
        }
    }

    /**
     * Logs warning message.
     *
     * @param tag     tag for the log message
     * @param message body of the log message
     */
    public static void w(String tag, String message) {
        if (mEnableCommonCoreLog) {
            CommonCoreLogger.warn(tag, Logger.getInstance().mCorrelationId, message, null);
        } else {
            Logger.getInstance().log(tag, message, null, LogLevel.Warn, null, null);
        }
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
        if (mEnableCommonCoreLog) {
            if (!StringExtensions.isNullOrBlank(message)) {
                CommonCoreLogger.error(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + message,
                        null, null);
            }

            if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                CommonCoreLogger.errorPII(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + additionalMessage,
                        null, null);
            }
        } else {
            Logger.getInstance().log(tag, message, additionalMessage, LogLevel.Error, errorCode, null);
        }
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
        if (mEnableCommonCoreLog) {
            // TODO should logger in errorLevel without PII enable print throwable stacktrace?
            // In the current implementation of ADAL Logger, we do not print stacktrace when the PII is disabled.
            // That is why we pass null in the Throwable parameter field.
            if (!StringExtensions.isNullOrBlank(message)) {
                CommonCoreLogger.error(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + message,
                        null, null);
            }

            if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                CommonCoreLogger.errorPII(tag, Logger.getInstance().getCorrelationId(),
                        (errorCode == null ? "" : errorCode.name() + ":") + additionalMessage,
                        null, throwable);
            }
        } else {
            Logger.getInstance().log(tag, message, additionalMessage, LogLevel.Error, errorCode, throwable);
        }
    }

    /**
     * Logs error message.
     *
     * @param tag       Tag for the log
     * @param message   Message to add to the log
     * @param throwable Throwable
     */
    public static void e(String tag, String message, Throwable throwable) {
        if (mEnableCommonCoreLog) {
            CommonCoreLogger.error(tag, Logger.getInstance().mCorrelationId, message, null, null);
            CommonCoreLogger.errorPII(tag, Logger.getInstance().mCorrelationId, "", null, throwable);
        } else {
            Logger.getInstance().log(tag, message, null, LogLevel.Error, null, throwable);
        }
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

    private static String getCodeName(ADALError code) {
        if (code != null) {
            return code.name();
        }

        return "";
    }

    @SuppressLint("SimpleDateFormat")
    private static String getUTCDateTimeAsString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = dateFormat.format(new Date());

        return utcTime;
    }

    /**
     * @return the correlation id for the logger.
     */
    public String getCorrelationId() {
        return mCorrelationId;
    }
}
