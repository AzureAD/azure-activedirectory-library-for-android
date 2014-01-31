/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;


/**
 */
public class AuthenticationException extends RuntimeException {
    static final long serialVersionUID = 1;

    private ADALError mCode;

    /**
     * Constructs a new AuthenticationError.
     */
    public AuthenticationException() {
    }

    public AuthenticationException(ADALError code) {
        mCode = code;
    }

    /**
     * @param appcontext Application context
     * @param code Resource file related error code. Message will be derived
     *            from resource with using app context
     * @param details Details related to the error such as query string, request
     *            info
     */
    public AuthenticationException(ADALError code, String details) {
        super(details);
        mCode = code;
    }
    
    public AuthenticationException(ADALError code, String details, Throwable throwable) {
        super(details, throwable);
        mCode = code;
    }
   
    public ADALError getCode() {
        return mCode;
    }

    @Override    
    public String getMessage() {
         
        if (!StringExtensions.IsNullOrBlank(super.getMessage())) {
            return super.getMessage();
        }

        if (mCode != null) {
            return mCode.getDescription();
        }

        return null;
    }
}
