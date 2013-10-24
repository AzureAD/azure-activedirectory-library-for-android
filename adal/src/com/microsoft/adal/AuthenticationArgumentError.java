/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

public class AuthenticationArgumentError extends AuthenticationError {
    static final long serialVersionUID = 1;

    /**
     * Constructs a new AuthenticationArgumentError.
     */
    public AuthenticationArgumentError() {
    }

    /**
     * Constructs a new AuthenticationArgumentError.
     * 
     * @param code
     * @param argument missing argument
     */
    public AuthenticationArgumentError(String argument) {
    }
}
