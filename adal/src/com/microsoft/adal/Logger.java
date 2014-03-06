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

package com.microsoft.adal;

import android.util.Log;


/**
 * Android log output can. If externalLogger is set, it will use that as well.
 * Usage: Logger.v(TAG, message, additionalMessage, errorCode) to log. Set
 * custom logger: Logger.setExternalLogger(..);
 * 
 * @author omercan
 */
public class Logger {

    private LogLevel mLogLevel;

    /**
     * error code: message. additionalMessage
     */
    private final static String LOG_FORMAT = "%s: %s. %s";

    private final static String CUSTOM_LOG_ERROR = "Custom log failed to log message:%s";

    public enum LogLevel {
        Error(0), Warn(1), Info(2), Verbose(3),
        /**
         * Debug level only.
         */
        Debug(4);

        @SuppressWarnings("unused")
        private int value;

        private LogLevel(int val) {
            this.value = val;
        }
    };

    /**
     * one callback logger
     */
    private ILogger mExternalLogger = null;

    // enabled by default
    private boolean mAndroidLogEnabled = true;

    private static Logger sInstance = new Logger();

    public static Logger getInstance() {
        return sInstance;
    }

    Logger() {
        mLogLevel = LogLevel.Debug;
    }

    public interface ILogger {
        void Log(String tag, String message, String additionalMessage, LogLevel level,
                ADALError errorCode);
    }

    public LogLevel getLogLevel() {
        return mLogLevel;
    }

    public void setLogLevel(LogLevel level) {
        this.mLogLevel = level;
    }

    /**
     * set custom logger
     * 
     * @param externalLogger
     */
    public void setExternalLogger(ILogger customLogger) {
        this.mExternalLogger = customLogger;
    }

    public void debug(String tag, String message) {
        if (mLogLevel.compareTo(LogLevel.Debug) < 0 || StringExtensions.IsNullOrBlank(message))
            return;

        if (mAndroidLogEnabled) {
            Log.d(tag, message);
        }

        if (mExternalLogger != null) {
            try {
                mExternalLogger.Log(tag, message, null, LogLevel.Debug, null);
            } catch (Exception e) {
                // log message as warning to report callback error issue
                Log.w(tag, String.format(CUSTOM_LOG_ERROR, message));
            }
        }
    }

    public void verbose(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mLogLevel.compareTo(LogLevel.Verbose) < 0)
            return;

        if (mAndroidLogEnabled) {
            Log.v(tag,
                    String.format(LOG_FORMAT, getCodeName(errorCode), message, additionalMessage));
        }

        if (mExternalLogger != null) {
            try {
                mExternalLogger.Log(tag, message, additionalMessage, LogLevel.Verbose, errorCode);
            } catch (Exception e) {
                // log message as warning to report callback error issue
                Log.w(tag, String.format(CUSTOM_LOG_ERROR, message));
            }
        }
    }

    public void inform(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mLogLevel.compareTo(LogLevel.Info) < 0)
            return;

        if (mAndroidLogEnabled) {
            Log.i(tag,
                    String.format(LOG_FORMAT, getCodeName(errorCode), message, additionalMessage));
        }

        if (mExternalLogger != null) {
            try {
                mExternalLogger.Log(tag, message, additionalMessage, LogLevel.Info, errorCode);
            } catch (Exception e) {
                // log message as warning to report callback error issue
                Log.w(tag, String.format(CUSTOM_LOG_ERROR, message));
            }
        }
    }

    public void warn(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mLogLevel.compareTo(LogLevel.Warn) < 0)
            return;

        if (mAndroidLogEnabled) {
            Log.w(tag,
                    String.format(LOG_FORMAT, getCodeName(errorCode), message, additionalMessage));
        }

        if (mExternalLogger != null) {
            try {
                mExternalLogger.Log(tag, message, additionalMessage, LogLevel.Warn, errorCode);
            } catch (Exception e) {
                // log message as warning to report callback error issue
                Log.w(tag, String.format(CUSTOM_LOG_ERROR, message));
            }
        }
    }

    public void error(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mAndroidLogEnabled) {
            Log.e(tag,
                    String.format(LOG_FORMAT, getCodeName(errorCode), message, additionalMessage));
        }

        if (mExternalLogger != null) {
            try {
                mExternalLogger.Log(tag, message, additionalMessage, LogLevel.Error, errorCode);
            } catch (Exception e) {
                // log message as warning to report callback error issue
                Log.w(tag, String.format(CUSTOM_LOG_ERROR, message));
            }
        }
    }

    public void error(String tag, String message, String additionalMessage, ADALError errorCode,
            Throwable err) {
        if (mAndroidLogEnabled) {
            Log.e(tag,
                    String.format(LOG_FORMAT, getCodeName(errorCode), message, additionalMessage),
                    err);
        }

        if (mExternalLogger != null) {
            try {
                mExternalLogger.Log(tag, message, additionalMessage, LogLevel.Error, errorCode);
            } catch (Exception e) {
                // log message as warning to report callback error issue
                Log.w(tag, String.format(CUSTOM_LOG_ERROR, message));
            }
        }
    }

    public static void d(String tag, String message) {
        Logger.getInstance().debug(tag, message);
    }

    public static void i(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().inform(tag, message, additionalMessage, errorCode);
    }

    public static void v(String tag, String message) {
        Logger.getInstance().verbose(tag, message, null, null);
    }

    public static void v(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().verbose(tag, message, additionalMessage, errorCode);
    }

    public static void w(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().warn(tag, message, additionalMessage, errorCode);
    }

    public static void e(String tag, String message, String additionalMessage, ADALError errorCode) {
        Logger.getInstance().error(tag, message, additionalMessage, errorCode);
    }

    /**
     * @param tag
     * @param message
     * @param additionalMessage
     * @param errorCode
     * @param err
     */
    public static void e(String tag, String message, String additionalMessage, ADALError errorCode,
            Throwable err) {
        Logger.getInstance().error(tag, message, additionalMessage, errorCode, err);
    }

    public boolean isAndroidLogEnabled() {
        return mAndroidLogEnabled;
    }

    public void setAndroidLogEnabled(boolean androidLogEnable) {
        this.mAndroidLogEnabled = androidLogEnable;
    }

    private String getCodeName(ADALError code) {
        if (code != null) {
            return code.name();
        }

        return "";
    }
}
