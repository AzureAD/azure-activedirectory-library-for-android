package com.microsoft.aad.adal;

/**
 * ADAL exception for device challenge processing
 */
class AuthenticationServerProtocolException extends AuthenticationException {

    static final long serialVersionUID = 1;

    public AuthenticationServerProtocolException(String detailMessage) {
        super(ADALError.DEVICE_CHALLENGE_FAILURE, detailMessage);
    }
}

