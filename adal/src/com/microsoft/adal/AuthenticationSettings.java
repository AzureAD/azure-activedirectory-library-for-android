
package com.microsoft.adal;

import javax.crypto.SecretKey;

/**
 * Settings to be used in AuthenticationContext
 * 
 * @author omercan
 */
public enum AuthenticationSettings {
    INSTANCE;

    private SecretKey mSecretKey = null;

    /**
     * Get secretkey to use in encrypt/decrypt
     * 
     * @return
     */
    public SecretKey getSecretKey() {
        return mSecretKey;
    }

    /**
     * set secret key to use in encrypt/decrypt
     * 
     * @param key
     */
    public void setSecretKey(SecretKey key) {
        mSecretKey = key;
    }
}
