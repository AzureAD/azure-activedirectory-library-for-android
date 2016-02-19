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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

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
        private int value;

        private LogLevel(int val) {
            this.value = val;
        }
    };

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
     * set custom logger.
     * 
     * @param externalLogger
     */
    public void setExternalLogger(ILogger customLogger) {
        this.mExternalLogger = customLogger;
    }

    private static String addMoreInfo(String message) {
        if (message != null) {
            return GetUTCdatetimeAsString() + "-" + getInstance().mCorrelationId + "-" + message
                    + " ver:" + AuthenticationContext.getVersionName();
        }

        return GetUTCdatetimeAsString() + "-" + getInstance().mCorrelationId + "- ver:"
                + AuthenticationContext.getVersionName();
    }

    public void debug(String tag, String message) {
        if (mLogLevel.compareTo(LogLevel.Debug) < 0 || StringExtensions.IsNullOrBlank(message)) {
            return;
        }

        if (mAndroidLogEnabled) {
            Log.d(tag, message);
        }

        logCommon(tag, message, "", LogLevel.Info, null);
    }

    public void verbose(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mLogLevel.compareTo(LogLevel.Verbose) < 0) {
            return;
        }

        if (mAndroidLogEnabled) {
            Log.v(tag, getLogMessage(message, additionalMessage, errorCode));
        }

        logCommon(tag, message, additionalMessage, LogLevel.Verbose, errorCode);
    }

    public void inform(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mLogLevel.compareTo(LogLevel.Info) < 0) {
            return;
        }

        if (mAndroidLogEnabled) {
            Log.i(tag, getLogMessage(message, additionalMessage, errorCode));
        }

        logCommon(tag, message, additionalMessage, LogLevel.Info, errorCode);
    }

    public void warn(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mLogLevel.compareTo(LogLevel.Warn) < 0) {
            return;
        }

        if (mAndroidLogEnabled) {
            Log.w(tag, getLogMessage(message, additionalMessage, errorCode));
        }

        logCommon(tag, message, additionalMessage, LogLevel.Warn, errorCode);
    }

    public void error(String tag, String message, String additionalMessage, ADALError errorCode) {
        if (mAndroidLogEnabled) {
            Log.e(tag, getLogMessage(message, additionalMessage, errorCode));
        }

        logCommon(tag, message, additionalMessage, LogLevel.Error, errorCode);
    }

    public void error(String tag, String message, String additionalMessage, ADALError errorCode,
            Throwable err) {
        if (mAndroidLogEnabled) {
            Log.e(tag, getLogMessage(message, additionalMessage, errorCode), err);
        }

        logCommon(tag, message, additionalMessage, LogLevel.Error, errorCode);
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

    private static String getLogMessage(String message, String additionalMessage,
            ADALError errorCode) {
        StringBuilder msg = new StringBuilder();
        if (errorCode != null) {
            msg.append(getCodeName(errorCode)).append(":");
        }
        if (message != null) {
            message = addMoreInfo(message);
            msg.append(message);
        }
        if (additionalMessage != null) {
            msg.append(" ").append(additionalMessage);
        }

        return msg.toString();
    }

    public static void d(String tag, String message) {
        Logger.getInstance().debug(tag, message);
    }

    public static void i(String tag, String message, String additionalMessage) {
        Logger.getInstance().inform(tag, message, additionalMessage, null);
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

    public static void setCorrelationId(UUID correlation) {
        Logger.getInstance().mCorrelationId = "";
        if (correlation != null) {
            Logger.getInstance().mCorrelationId = correlation.toString();
        }
    }

    public boolean isAndroidLogEnabled() {
        return mAndroidLogEnabled;
    }

    public void setAndroidLogEnabled(boolean androidLogEnable) {
        this.mAndroidLogEnabled = androidLogEnable;
    }

    private static String getCodeName(ADALError code) {
        if (code != null) {
            return code.name();
        }

        return "";
    }

    private static String GetUTCdatetimeAsString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = dateFormat.format(new Date());

        return utcTime;
    }

    public String getCorrelationId() {
        return mCorrelationId;
    }
}
