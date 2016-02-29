package com.microsoft.aad.adal;

/**
 * Developer authentication error.
 */
public class DeveloperAuthenticationException extends AuthenticationException {
    static final long serialVersionUID = 1;

    /**
     * Constructs a new AuthenticationCancelError.
     */
    public DeveloperAuthenticationException() {
        super();
    }

    /**
     * Constructs a new AuthenticationCancelError with message.
     * 
     * @param msg Message for cancel request
     */
    public DeveloperAuthenticationException(ADALError code, String msg) {
        super(code, msg);
    }
}
