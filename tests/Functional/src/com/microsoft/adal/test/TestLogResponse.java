package com.microsoft.adal.test;

import com.microsoft.adal.ADALError;
import com.microsoft.adal.Logger.LogLevel;

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
}