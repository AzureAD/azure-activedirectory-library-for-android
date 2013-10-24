
package com.microsoft.adal;

import com.microsoft.adal.ErrorCodes.ADALError;

public class AuthenticationCancelError extends AuthenticationError {
    static final long serialVersionUID = 1;

    /**
     * Constructs a new AuthenticationCancelError.
     */
    public AuthenticationCancelError() {
    }

    /**
     * Constructs a new AuthenticationCancelError.
     * 
     * @param code Cancellation code such as certificate issue
     */
    public AuthenticationCancelError(ADALError cancelCode) {
    }
}
