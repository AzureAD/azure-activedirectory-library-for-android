/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import android.content.Context;

import com.microsoft.adal.ErrorCodes.ADALError;

/**
 */
public class AuthenticationException extends RuntimeException {
    static final long serialVersionUID = 1;

    private ADALError mCode;

    private String mErrorMessage;

    private String mDetails;

    private Throwable mThrowable;

    /**
     * Constructs a new AuthenticationError.
     */
    public AuthenticationException() {
    }

    public AuthenticationException(ADALError code) {
        mCode = code;
        mErrorMessage = null;
    }

    /**
     * Constructs a new AuthenticationError.
     * 
     * @param code
     * @param details the detail message of this exception
     * @param throwable the cause of this exception
     */
    public AuthenticationException(ADALError code, String details, Throwable throwable) {
        mCode = code;
        mErrorMessage = null;
        mDetails = details;
        mThrowable = throwable;
    }

    /**
     * Constructs a new AuthenticationError.
     * 
     * @param throwable the cause of this exception
     */
    public AuthenticationException(ADALError code, Throwable throwable) {
        mCode = code;
        mErrorMessage = null;
        mThrowable = throwable;
    }

    /**
     * @param appcontext Application context
     * @param code Resource file related error code. Message will be derived
     *            from resource with using app context
     * @param details Details related to the error such as query string, request
     *            info
     */
    public AuthenticationException(ADALError code, String details) {
        mCode = code;
        mErrorMessage = null;
        mDetails = details;
    }

    public AuthenticationException(String errorCode, String errorDescription) {
        mCode = null;
        mErrorMessage = errorCode;
        mDetails = errorDescription;
    }

    public ADALError getCode() {
        return mCode;
    }

    /**
     * gets message from strings.xml file related to this error code if code is present.
     * 
     * @param appContext Context is needed to access resource files
     * @return Translated message
     */
    public String getMessage(Context appContext) {
        if (!StringExtensions.IsNullOrBlank(mErrorMessage)) {
            return mErrorMessage;

        }

        if (mCode != null) {
            return ErrorCodes.getMessage(appContext, mCode);
        }

        return null;
    }

    public String getDetails() {
        return mDetails;
    }
}
