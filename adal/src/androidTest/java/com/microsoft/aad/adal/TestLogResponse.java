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

import java.util.concurrent.CountDownLatch;

import com.microsoft.aad.adal.Logger.ILogger;
import com.microsoft.aad.adal.Logger.LogLevel;

public class TestLogResponse {
    private String mTag;

    private String mMessage;

    private String mAdditionalMessage;

    private LogLevel mLevel;

    private ADALError mErrorCode;

    public void reset() {
        this.mTag = null;
        this.mMessage = null;
        this.mAdditionalMessage = null;
        this.mErrorCode = null;
    }

    public void listenForLogMessage(final String msg, final CountDownLatch signal) {
        final TestLogResponse response = this;

        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                    ADALError errorCode) {

                if (message.contains(msg + " ver:" + AuthenticationContext.getVersionName())) {
                    response.mTag = tag;
                    response.mMessage = message;
                    response.mAdditionalMessage = additionalMessage;
                    response.mLevel = level;
                    response.mErrorCode = errorCode;
                    if (signal != null) {
                        signal.countDown();
                    }
                }
            }
        });
    }

    /**
     * Check log message for segments since some of the responses include server generated traceid, timeStamp etc.
     * @param msgs
     */
    public void listenLogForMessageSegments(final String... msgs) {
        final TestLogResponse response = this;

        Logger.getInstance().setExternalLogger(new ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                    ADALError errorCode) {
                for (String msg : msgs) {
                    if (message.contains(msg) || additionalMessage.contains(msg)) {
                        response.mTag = tag;
                        response.mMessage = message;
                        response.mAdditionalMessage = additionalMessage;
                        response.mLevel = level;
                        response.mErrorCode = errorCode;
                    } else {
                        break;
                    }
                }
            }
        });

    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        mTag = tag;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public String getAdditionalMessage() {
        return mAdditionalMessage;
    }

    public void setAdditionalMessage(String additionalMessage) {
        mAdditionalMessage = additionalMessage;
    }

    public void setLevel(LogLevel level) {
        mLevel = level;
    }

    public ADALError getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(ADALError errorCode) {
        mErrorCode = errorCode;
    }
}
