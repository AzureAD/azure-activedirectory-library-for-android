// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

/**
 * Settings to be used in AuthenticationContext.
 */
public enum AuthenticationSettings {
    /**
     * Singleton setting instance.
     */
    INSTANCE;

    private static final int SECRET_RAW_KEY_LENGTH = 32;

    private byte[] mSecretKeyData = null;

    private String mBrokerPackageName = AuthenticationConstants.Broker.PACKAGE_NAME;

    private String mBrokerSignature = AuthenticationConstants.Broker.SIGNATURE;

    private Class<?> mClazzDeviceCertProxy;

    private String mActivityPackageName;

    /**
     * SharedPreference package name to load this file from different context.
     */
    private String mSharedPrefPackageName;

    private boolean mSkipBroker = false;

    /**
     * Expiration buffer in seconds.
     */
    private int mExpirationBuffer = 300;

    private int mConnectTimeOut = 30000;

    private int mReadTimeOut = 30000;

    /**
     * Get bytes to derive secretKey to use in encrypt/decrypt.
     * 
     * @return byte[] secret data
     */
    public byte[] getSecretKeyData() {
        return mSecretKeyData;
    }

    /**
     * set raw bytes to derive secretKey to use in encrypt/decrypt. KeySpec
     * algorithm is AES.
     * 
     * @param rawKey App related key to use in encrypt/decrypt
     */
    public void setSecretKey(byte[] rawKey) {
        if (rawKey == null || rawKey.length != SECRET_RAW_KEY_LENGTH) {
            throw new IllegalArgumentException("rawKey");
        }

        mSecretKeyData = rawKey;
    }

    /**
     * Gets packagename for broker app that installed authenticator.
     * 
     * @return packagename
     */
    public String getBrokerPackageName() {
        return mBrokerPackageName;
    }

    /**
     * Sets package name for broker app that installed authenticator.
     * 
     * @param packageName package name related to broker
     */
    public void setBrokerPackageName(String packageName) {
        mBrokerPackageName = packageName;
    }

    /**
     * Gets broker signature for broker app that installed authenticator.
     * 
     * @return signature
     */
    public String getBrokerSignature() {
        return mBrokerSignature;
    }

    /**
     * Sets broker app info for ADAL to use.
     * 
     * @param brokerSignature Signature for broker
     */
    public void setBrokerSignature(String brokerSignature) {
        this.mBrokerSignature = brokerSignature;
    }

    /**
     * set class for work place join related API. This is only used from
     * Authenticator side.
     * 
     * @param <T>
     * @param clazz
     */
    public void setDeviceCertificateProxyClass(Class clazz) {
        if (IDeviceCertificate.class.isAssignableFrom(clazz)) {
            mClazzDeviceCertProxy = clazz;
        } else {
            throw new IllegalArgumentException("clazz");
        }
    }

    /**
     * get class for work place join related API. This is only used from
     * Authenticator side.
     * 
     * @return
     */
    public Class<?> getDeviceCertificateProxy() {
        return mClazzDeviceCertProxy;
    }

    /**
     * get package name to setup intent for AuthenticationActivity.
     * 
     * @return Package name for activity
     */
    public String getActivityPackageName() {
        return mActivityPackageName;
    }

    /**
     * set package name to setup intent for AuthenticationActivity.
     * 
     * @param activityPackageName activity to use from different package
     */
    public void setActivityPackageName(String activityPackageName) {
        this.mActivityPackageName = activityPackageName;
    }

    /**
     * Skip broker usage.
     * 
     * @return true if broker is not used.
     */
    public boolean getSkipBroker() {
        return mSkipBroker;
    }

    /**
     * Sets flag to skip broker.
     * 
     * @param skip True to not use broker
     */
    public void setSkipBroker(boolean skip) {
        mSkipBroker = skip;
    }

    /**
     * Sets package name to use {@link DefaultTokenCacheStore} with sharedUserId
     * apps.
     * 
     * @param packageNameForSharedFile Package name of other app
     */
    public void setSharedPrefPackageName(String packageNameForSharedFile) {
        this.mSharedPrefPackageName = packageNameForSharedFile;
    }

    /**
     * Gets package name provided for shared preferences.
     * 
     * @return package name provided for shared preferences
     */
    public String getSharedPrefPackageName() {
        return mSharedPrefPackageName;
    }

    /**
     * Gets expiration buffer.
     * 
     * @return
     */
    public int getExpirationBuffer() {
        return mExpirationBuffer;
    }

    /**
     * When checking access token expiration, it will check if the time to
     * expiration is less than this value(in seconds). Example: Set to 300 to
     * give 5min buffer. Token with Expiry time of 12:04 will say expired when
     * actual time is 12:00 with 5min buffer.
     * 
     * @param expirationBuffer
     */
    public void setExpirationBuffer(int expirationBuffer) {
        this.mExpirationBuffer = expirationBuffer;
    }

    public int getConnectTimeOut() {
        return mConnectTimeOut;
    }

    /**
     * Sets the maximum time in milliseconds to wait while connecting.
     * Connecting to a server will fail with a SocketTimeoutException if the
     * timeout elapses before a connection is established. Default value is
     * 30000 milliseconds.
     * 
     * @param timeOutMillis the connect timeout in milliseconds. Non-negative
     * @throws IllegalArgumentException if timeoutMillis < 0.
     */
    public void setConnectTimeOut(int timeOutMillis) {
        if (timeOutMillis < 0) {
            throw new IllegalArgumentException("Invalid timeOutMillis");
        }
        this.mConnectTimeOut = timeOutMillis;
    }

    public int getReadTimeOut() {
        return mReadTimeOut;
    }

    /**
     * Sets the maximum time to wait for an input stream read to complete before
     * giving up. Reading will fail with a SocketTimeoutException if the timeout
     * elapses before data becomes available. The default value is 30000.
     * 
     * @param timeoutMillis the read timeout in milliseconds. Non-negative
     */
    public void setReadTimeOut(int timeOutMillis) {
        if (timeOutMillis < 0) {
            throw new IllegalArgumentException("Invalid timeOutMillis");
        }

        this.mReadTimeOut = timeOutMillis;
    }
}
