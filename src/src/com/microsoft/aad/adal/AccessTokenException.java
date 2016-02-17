package com.microsoft.aad.adal;

/**
 * ADAL exception for the case when server doesn't provide access token
 * but UI prompt isn't allowed
 */
public class AccessTokenException extends AuthenticationException{
    static final long serialVersionUID = 1;

    public AccessTokenException(ADALError code, String details) {
        super(code, details);
    }
}
