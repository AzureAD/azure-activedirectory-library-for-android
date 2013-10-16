package com.microsoft.adal;

import org.apache.http.auth.AuthenticationException;

public class AuthException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AuthenticationRequest pendingRequest;
    private String mErrorCode;
    private String mErrorDescription;
    
    public AuthException(AuthenticationRequest request, String errCode, String errMessage)
    {
        pendingRequest = request;
        mErrorCode = errCode;
        mErrorDescription = errMessage;
    }
    
    public String getErrorCode()
    {
        return mErrorCode;
    }
    
    public String getErrorDescription()
    {
        return mErrorDescription;
    }
}
