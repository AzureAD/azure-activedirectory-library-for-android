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

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.ILoggerCallback;
import com.microsoft.identity.common.internal.logging.IRequestContext;
import com.microsoft.identity.common.internal.logging.RequestContext;

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
                com.microsoft.identity.common.internal.logging.Logger.getInstance()
                        .setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.ERROR);
                break;
            case Warn:
                com.microsoft.identity.common.internal.logging.Logger.getInstance()
                        .setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.WARN);
                break;
            case Info:
                com.microsoft.identity.common.internal.logging.Logger.getInstance()
                        .setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.INFO);
                break;
            case Verbose:
                com.microsoft.identity.common.internal.logging.Logger.getInstance()
                        .setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.VERBOSE);
                break;
            case Debug:
                //The debug level is deprecated and removed in common core.
                com.microsoft.identity.common.internal.logging.Logger.getInstance()
                        .setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.INFO);
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
     */
    public synchronized void setExternalLogger(ILogger externalLogger) {
        com.microsoft.identity.common.internal.logging.Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(String tag, com.microsoft.identity.common.internal.logging.Logger.LogLevel logLevel, String message, boolean containsPII) {
                if (mExternalLogger != null) {
                    if (!com.microsoft.identity.common.internal.logging.Logger.getAllowPii() && containsPII) {
                        return;
                    } else {

                        ADALError adalError = mapMessageToAdalError(message);

                        switch (logLevel) {
                            case ERROR:
                                mExternalLogger.Log(tag, message, null, LogLevel.Error, adalError);
                                break;
                            case WARN:
                                mExternalLogger.Log(tag, message, null, LogLevel.Warn, adalError);
                                break;
                            case VERBOSE:
                                mExternalLogger.Log(tag, message, null, LogLevel.Verbose, adalError);
                                break;
                            case INFO:
                                mExternalLogger.Log(tag, message, null, LogLevel.Info, adalError);
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown logLevel");
                        }
                    }
                }
            }

            private ADALError mapMessageToAdalError(final String message) {
                ADALError mappedError = null;

                for (final ADALError adalError : ADALError.values()) {
                    if (null != message && message.contains(adalError.name() + ":")) {
                        mappedError = adalError;
                        break;
                    }
                }

                return mappedError;
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
        com.microsoft.identity.common.internal.logging.Logger.setAllowLogcat(androidLogEnabled);
    }

    /**
     * ADAL provides logging callbacks that assist in diagnostics. The callback has two parameters,
     * message and additionalMessage. All user information is put into additionalMessage.
     * ADAL will clear this data unless the {@link com.microsoft.identity.common.internal.logging.Logger#mAllowPii} is called with true.
     * By default the library will not return any messages with user information in them.
     *
     * @param enablePII True if enabling PII info to be logged, false otherwise.
     */
    public void setEnablePII(final boolean enablePII) {
        com.microsoft.identity.common.internal.logging.Logger.setAllowPii(enablePII);
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
                                   ADALError errorCode, Throwable throwable) {
        switch (logLevel) {
            case Error:
                if (!StringExtensions.isNullOrBlank(message)) {
                    com.microsoft.identity.common.internal.logging.Logger.error(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + formatMessage(message), null);
                }

                if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                    com.microsoft.identity.common.internal.logging.Logger.errorPII(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + formatMessage(additionalMessage), throwable);
                }
                break;
            case Warn:
                if (!StringExtensions.isNullOrBlank(message)) {
                    com.microsoft.identity.common.internal.logging.Logger.warn(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + formatMessage(message));
                }

                if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                    com.microsoft.identity.common.internal.logging.Logger.warnPII(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + formatMessage(additionalMessage));
                }
                break;
            case Info:
                if (!StringExtensions.isNullOrBlank(message)) {
                    com.microsoft.identity.common.internal.logging.Logger.info(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + formatMessage(message));
                }

                if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                    com.microsoft.identity.common.internal.logging.Logger.infoPII(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + formatMessage(additionalMessage));
                }
                break;
            case Verbose:
                if (!StringExtensions.isNullOrBlank(message)) {
                    com.microsoft.identity.common.internal.logging.Logger.verbose(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + formatMessage(message));
                }

                if (!StringExtensions.isNullOrBlank(additionalMessage)) {
                    com.microsoft.identity.common.internal.logging.Logger.verbosePII(tag, Logger.getInstance().getCorrelationId(),
                            (errorCode == null ? "" : errorCode.name() + ":") + formatMessage(additionalMessage));
                }
                break;
            case Debug:
                //The debug level is deprecated and removed in common core.
                com.microsoft.identity.common.internal.logging.Logger.info(tag, Logger.getInstance().mCorrelationId, formatMessage(message));
                break;
            default:
                throw new IllegalArgumentException("Unknown loglevel");
        }
    }

    /**
     * @param tag     tag for the log message
     * @param message body of the log message
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#info(String, String, String)} instead.
     * <p>
     * Logs debug message.
     */
    @Deprecated
    public static void d(String tag, String message) {
        if (StringExtensions.isNullOrBlank(message)) {
            return;
        }

        Logger.getInstance().commonCoreWrapper(tag, message, null, LogLevel.Debug, null, null);
    }

    /**
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#info(String, String, String)}
     * if the log message does not contain any PII information.
     * use {@link com.microsoft.identity.common.internal.logging.Logger#infoPII(String, String, String)}
     * if the log message contains any PII information.
     * <p>
     * Logs informational message.
     */
    @Deprecated
    public static void i(String tag, String message, String additionalMessage) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Info, null, null);
    }

    /**
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode         ADAL error code being logged
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#info(String, String, String)}
     * if the log message does not contain any PII information.
     * use {@link com.microsoft.identity.common.internal.logging.Logger#infoPII(String, String, String)}
     * if the log message contains any PII information.
     * <p>
     * Logs informational messages with error codes.
     */
    @Deprecated
    public static void i(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Info, errorCode, null);
    }

    /**
     * @param tag     tag for the log message
     * @param message body of the log message
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#verbose(String, String, String)}
     * if the log message does not contain any PII information.
     * use {@link com.microsoft.identity.common.internal.logging.Logger#verbosePII(String, String, String)}
     * if the log message contains any PII information.
     * <p>
     * Logs verbose message.
     */
    @Deprecated
    public static void v(String tag, String message) {
        Logger.getInstance().commonCoreWrapper(tag, message, null, LogLevel.Verbose, null, null);
    }

    /**
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode         ADAL error code being logged
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#verbose(String, String, String)}
     * if the log message does not contain any PII information.
     * use {@link com.microsoft.identity.common.internal.logging.Logger#verbosePII(String, String, String)}
     * if the log message contains any PII information.
     * <p>
     * Logs verbose message with error code.
     */
    @Deprecated
    public static void v(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Verbose, errorCode, null);
    }

    /**
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode         ADAL error code being logged
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#warn(String, String, String)}
     * if the log message does not contain any PII information.
     * use {@link com.microsoft.identity.common.internal.logging.Logger#warnPII(String, String, String)}
     * if the log message contains any PII information.
     * <p>
     * Logs warning message.
     */
    @Deprecated
    public static void w(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Warn, errorCode, null);
    }

    /**
     * @param tag     tag for the log message
     * @param message body of the log message
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#warn(String, String, String)}
     * if the log message does not contain any PII information.
     * use {@link com.microsoft.identity.common.internal.logging.Logger#warnPII(String, String, String)}
     * if the log message contains any PII information.
     * <p>
     * Logs warning message.
     */
    @Deprecated
    public static void w(String tag, String message) {
        Logger.getInstance().commonCoreWrapper(tag, message, null, LogLevel.Warn, null, null);
    }

    /**
     * @param tag               tag for the log message
     * @param message           body of the log message
     * @param additionalMessage additional parameters
     * @param errorCode         ADAL error code being logged
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#error(String, String, String, Throwable)}
     * if the log message does not contain any PII information.
     * use {@link com.microsoft.identity.common.internal.logging.Logger#errorPII(String, String, String, Throwable)}
     * if the log message contains any PII information.
     * <p>
     * Logs error message.
     */
    @Deprecated
    public static void e(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Error, errorCode, null);
    }

    /**
     * @param tag               Tag for the log
     * @param message           Message to add to the log
     * @param additionalMessage any additional parameters
     * @param errorCode         ADAL error code
     * @param throwable         Throwable
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#error(String, String, String, Throwable)}
     * if the log message does not contain any PII information.
     * use {@link com.microsoft.identity.common.internal.logging.Logger#errorPII(String, String, String, Throwable)}
     * if the log message contains any PII information.
     * <p>
     * Logs error message.
     */
    @Deprecated
    public static void e(String tag, String message, String additionalMessage, ADALError errorCode,
                  Throwable throwable) {
        Logger.getInstance().commonCoreWrapper(tag, message, additionalMessage, LogLevel.Error, errorCode, throwable);
    }

    /**
     * @param tag       Tag for the log
     * @param message   Message to add to the log
     * @param throwable Throwable
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#error(String, String, String, Throwable)}
     * if the log message does not contain any PII information.
     * use {@link com.microsoft.identity.common.internal.logging.Logger#errorPII(String, String, String, Throwable)}
     * if the log message contains any PII information.
     * <p>
     * Logs error message.
     */
    @Deprecated
    public static void e(String tag, String message, Throwable throwable) {
        Logger.getInstance().commonCoreWrapper(tag, message, null, LogLevel.Error, null, throwable);
    }

    /**
     * Logs error message.
     *
     * @param tag Tag for the log
     * @param message Message to add to the log
     */
    public static void e(String tag, String message) {
        Logger.getInstance().commonCoreWrapper(tag, message, null, LogLevel.Error, null, null);
    }

    /**
     * Sets the correlation id for the logger.
     *
     * @param correlation Correlation ID to be used
     */
    public static void setCorrelationId(UUID correlation) {
        Logger.getInstance().mCorrelationId = "";
        if (correlation != null) {
            final String correlationId = correlation.toString();
            Logger.getInstance().mCorrelationId = correlationId;

            // Update to use the new common cache. The correlationId will be set on the
            // DiagnosticContext.
            final IRequestContext requestContext = new RequestContext();
            requestContext.put("correlation_id", correlationId);

            DiagnosticContext.setRequestContext(requestContext);
        }
    }

    /**
     * @return the correlation id for the logger.
     */
    public String getCorrelationId() {
        return mCorrelationId;
    }

    /**
     * Append the version name into the log message.
     *
     * @param message Log message
     */
    private String formatMessage(final String message) {
        return message + " ver:" + AuthenticationContext.getVersionName();
    }
}
