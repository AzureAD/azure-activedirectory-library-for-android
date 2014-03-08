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
