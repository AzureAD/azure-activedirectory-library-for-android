
package com.microsoft.adal;


public class AuthenticationCancelError extends AuthenticationException {
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
