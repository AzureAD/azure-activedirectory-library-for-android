package com.microsoft.adal;

import org.apache.http.auth.AuthenticationException;

public class AuthException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public AuthenticationRequest pendingRequest;
    public String mErrorCode;
    public String mErrorDescription;
    
    public AuthException(AuthenticationRequest request, String errCode, String errMessage)
    {
        pendingRequest = request;
        mErrorCode = errCode;
        mErrorDescription = errMessage;
    }
}
