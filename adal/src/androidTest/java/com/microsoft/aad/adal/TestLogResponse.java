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

import java.util.concurrent.CountDownLatch;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.Logger.ILogger;
import com.microsoft.aad.adal.Logger.LogLevel;

public class TestLogResponse {
    String tag;

    String message;

    String additionalMessage;

    LogLevel level;

    ADALError errorCode;

    public void reset() {
        this.tag = null;
        this.message = null;
        this.additionalMessage = null;
        this.errorCode = null;
    }

    public void listenForLogMessage(final String msg, final CountDownLatch signal) {
        final TestLogResponse response = this;

        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                    ADALError errorCode) {

                if (message.contains(msg + " ver:" + AuthenticationContext.getVersionName())) {
                    response.tag = tag;
                    response.message = message;
                    response.additionalMessage = additionalMessage;
                    response.level = level;
                    response.errorCode = errorCode;
                    if (signal != null) {
                        signal.countDown();
                    }
                }
            }
        });
    }

    /**
     * Check log message for segments since some of the responses include server generated traceid, timeStamp etc.
     * @param signal
     * @param msgs
     */
    public void listenLogForMessageSegments(final CountDownLatch signal, final String... msgs) {
        final TestLogResponse response = this;

        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                    ADALError errorCode) {
                boolean hasAll = true;
                for (String msg : msgs) {
                    if (message.contains(msg)) {
                        response.tag = tag;
                        response.message = message;
                        response.additionalMessage = additionalMessage;
                        response.level = level;
                        response.errorCode = errorCode;
                    } else {
                        hasAll = false;
                        break;
                    }
                }

                if (signal != null && hasAll) {
                    signal.countDown();
                }
            }
        });

    }
}
