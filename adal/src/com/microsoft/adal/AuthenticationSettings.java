
package com.microsoft.adal;

import javax.crypto.SecretKey;

/**
 * Settings to be used in AuthenticationContext
 * 
 * @author omercan
 */
public enum AuthenticationSettings {
    INSTANCE;

    private final static String DEFAULT_AUTHORIZE_ENDPOINT = "/oauth2/authorize";

    private final static String DEFAULT_TOKEN_ENDPOINT = "/oauth2/token";

    private String mAuthorizeEndpoint = DEFAULT_AUTHORIZE_ENDPOINT;

    private String mTokenEndpoint = DEFAULT_TOKEN_ENDPOINT;

    private SecretKey mSecretKey = null;
    /**
     * default is /oauth2/token
     * 
     * @param value forward slash is added as prefix
     */
    public void setTokenEndpoint(String value) {
        if (value == null) {
            throw new IllegalArgumentException("tokenEndpoint");
        }

        value = ensureStartsWithSlash(value);
        mTokenEndpoint = value;
    }

    public String getTokenEndpoint() {
        return mTokenEndpoint;
    }

    /**
     * set authorize endpoint value
     * 
     * @param value forward slash is added as prefix
     */
    public void setAuthorizeEndpoint(String value) {
        if (value == null) {
            throw new IllegalArgumentException("authorizeEndpoint");
        }

        value = ensureStartsWithSlash(value);
        mAuthorizeEndpoint = value;
    }

    public String getAuthorizeEndpoint() {
        return mAuthorizeEndpoint;
    }

    private String ensureStartsWithSlash(String value) {
        if (!StringExtensions.IsNullOrBlank(value)) {
            if (!value.startsWith("/")) {
                return "/" + value;
            }
        }

        return value;
    }

    /**
     * Get secretkey to use in encrypt/decrypt
     * @return
     */
    public SecretKey getSecretKey() {
        return mSecretKey;
    }
    
    /**
     * set secret key to use in encrypt/decrypt
     * @param key
     */
    public void setSecretKey(SecretKey key){
        mSecretKey = key;
    }
}
