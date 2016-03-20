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

package com.microsoft.aad.adal.test;

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
