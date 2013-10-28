/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import android.content.Context;

import com.microsoft.adal.ErrorCodes.ADALError;

/**
 */
public class AuthenticationError extends RuntimeException {
    static final long serialVersionUID = 1;

    private ADALError mCode;
    
    
    /**
     * Constructs a new AuthenticationError.
     */
    public AuthenticationError() {        
    }

    public AuthenticationError(ADALError code) {
    	mCode = code;
    }
    
    /**
     * Constructs a new AuthenticationError.
     * 
     * @param code
     * @param details the detail message of this exception
     * @param throwable the cause of this exception
     */
    public AuthenticationError(ADALError code, String details,
            Throwable throwable) {
        throw new UnsupportedOperationException("come back later");
    }

    /**
     * Constructs a new AuthenticationError.
     * 
     * @param throwable the cause of this exception
     */
    public AuthenticationError(ADALError code, Throwable throwable) {
        throw new UnsupportedOperationException("come back later");
    }

    /**
     * @param appcontext Application context
     * @param errorCode Resource file related error code. Message will be
     *            derived from resource with using app context
     * @param details Details related to the error such as query string, request
     *            info
     */
    public AuthenticationError(ADALError errorCode, String details) {
        throw new UnsupportedOperationException("come back later");
    }

    public ADALError getCode() {
        return mCode;
    }

    /**
     * gets message from strings.xml file related to this error code.
     * 
     * @param appContext Context is needed to access resource files
     * @return Translated message
     */
    public String getMessage(Context appContext) {
        throw new UnsupportedOperationException("come back later");
    }

    public String getDetails() {
        throw new UnsupportedOperationException("come back later");
    }
}
