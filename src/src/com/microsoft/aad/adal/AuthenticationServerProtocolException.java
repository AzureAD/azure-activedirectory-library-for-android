package com.microsoft.aad.adal;

/**
 * ADAL exception for device challenge processing
 */
class AuthenticationServerProtocolException extends Exception {

    static final long serialVersionUID = 1;

    public AuthenticationServerProtocolException(String detailMessage) {
        super(detailMessage);
    }
}

