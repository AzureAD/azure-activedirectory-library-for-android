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

/**
 * usage authentication error.
 */
public class UsageAuthenticationException extends AuthenticationException {
    static final long serialVersionUID = 1;

    /**
     * Constructs a new AuthenticationCancelError.
     */
    public UsageAuthenticationException() {
        super();
    }

    /**
     * Constructs a new AuthenticationCancelError with message.
     * 
     * @param msg Message for cancel request
     */
    public UsageAuthenticationException(ADALError code, String msg) {
        super(code, msg);
    }
    
    /**
     * Constructs a new AuthenticationCancelError with message and the cause exception
     * 
     * @param code Resource file related error code. Message will be derived
     *            from resource with using app context
     * @param details Details related to the error such as query string, request
     *            info
     * @param throwable {@link Throwable}
     */
    public UsageAuthenticationException(ADALError code, String msg, Throwable throwable) {
        super(code, msg, throwable);
    }
}
