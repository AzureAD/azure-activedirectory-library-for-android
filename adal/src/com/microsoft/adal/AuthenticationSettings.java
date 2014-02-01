
package com.microsoft.adal;


/**
 * Settings to be used in AuthenticationContext
 * 
 * @author omercan
 */
public enum AuthenticationSettings {
    INSTANCE;

    private final static int SECRET_RAW_KEY_LENGTH = 32;
    
    private byte[] mSecretKeyData = null;

    /**
     * Get bytes to derive secretKey to use in encrypt/decrypt
     * 
     * @return
     */
    public byte[] getSecretKeyData() {
        return mSecretKeyData;
    }

    /**
     * set raw bytes to derive secretKey to use in encrypt/decrypt
     * 
     * @param key
     */
    public void setSecretKey(byte[] rawKey) {
        if(rawKey == null || rawKey.length != SECRET_RAW_KEY_LENGTH){
            throw new IllegalArgumentException("rawKey");
        }
        
        mSecretKeyData = rawKey;
    }
}
