/**
 * ---------------------------------------------------------------- 
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 * ----------------------------------------------------------------
 */

package com.microsoft.adal;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import android.test.suitebuilder.TestSuiteBuilder.FailedToCreateTests;

/**
 * Result class to keep code, token and other info Serializable properties Mark
 * temp properties as Transient if you dont want to keep them in serialization
 * 
 * @author omercan
 */
public class AuthenticationResult implements Serializable {

    public enum AuthenticationStatus {
        /**
         * User cancelled login activity
         */
        Cancelled,
        /**
         * request has errors
         */
        Failed,
        /**
         * token is acquired
         */
        Succeeded,
    }

    AuthenticationResult() {
        throw new UnsupportedOperationException("come back later");
    }

    AuthenticationResult(String authority, String clientId, String resource,
            String redirectUri, String accessToken, String refreshToken,
            Date expires, String scope) {
        throw new UnsupportedOperationException("come back later");
    }

    AuthenticationResult(String errorCode, String errDescription) {
        throw new UnsupportedOperationException("come back later");
    }

    public String createAuthorizationHeader() {
        throw new UnsupportedOperationException("come back later");
    }

    public String getAccessToken() {
        throw new UnsupportedOperationException("come back later");
    }

    public String getRefreshToken() {
        throw new UnsupportedOperationException("come back later");
    }

    public String getAccessTokenType() {
        throw new UnsupportedOperationException("come back later");
    }

    public Date getExpiresOn() {
        throw new UnsupportedOperationException("come back later");
    }

    public boolean getIsMultiResourceRefreshToken() {
        throw new UnsupportedOperationException("come back later");
    }

    public UserInfo getUserInfo() {
        throw new UnsupportedOperationException("come back later");
    }

    public String getTenantId(){
        throw new UnsupportedOperationException("come back later");
    }
    
    public AuthenticationStatus getStatus() {
        throw new UnsupportedOperationException("come back later");
    }
}
