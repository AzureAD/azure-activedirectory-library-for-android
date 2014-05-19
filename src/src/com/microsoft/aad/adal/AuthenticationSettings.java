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
 * Settings to be used in AuthenticationContext
 */
public enum AuthenticationSettings {
    /**
     * Singleton setting instance
     */
    INSTANCE;

    private final static int SECRET_RAW_KEY_LENGTH = 32;

    private byte[] mSecretKeyData = null;

    private String mBrokerPackageName = AuthenticationConstants.Broker.PACKAGE_NAME;

    private String mBrokerSignature = AuthenticationConstants.Broker.SIGNATURE;

    private Class<?> mClazzDeviceCertProxy;
    
    private String mActivityPackageName;
    
    /**
     * Get bytes to derive secretKey to use in encrypt/decrypt
     * 
     * @return byte[] secret data
     */
    public byte[] getSecretKeyData() {
        return mSecretKeyData;
    }

    /**
     * set raw bytes to derive secretKey to use in encrypt/decrypt
     * 
     * @param rawKey
     */
    public void setSecretKey(byte[] rawKey) {
        if (rawKey == null || rawKey.length != SECRET_RAW_KEY_LENGTH) {
            throw new IllegalArgumentException("rawKey");
        }

        mSecretKeyData = rawKey;
    }

    /**
     * Gets packagename for broker app that installed authenticator
     * 
     * @return packagename
     */
    public String getBrokerPackageName() {
        return mBrokerPackageName;
    }

    /**
     * Sets packagename for broker app that installed authenticator
     * 
     * @param packageName
     */
    public void setBrokerPackageName(String packageName) {
        mBrokerPackageName = packageName;
    }

    /**
     * Gets broker signature for broker app that installed authenticator
     * 
     * @return signature
     */
    public String getBrokerSignature() {
        return mBrokerSignature;
    }

    /**
     * Sets broker app info for ADAL to use
     * 
     * @param mBrokerSignature
     */
    public void setBrokerSignature(String mBrokerSignature) {
        this.mBrokerSignature = mBrokerSignature;
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
     * get package name to setup intent for AuthenticationActivity
     * @return
     */
    public String getActivityPackageName() {
        return mActivityPackageName;
    }

    /**
     * set package name to setup intent for AuthenticationActivity
     * @param mActivityPackageName
     */
    public void setActivityPackageName(String mActivityPackageName) {
        this.mActivityPackageName = mActivityPackageName;
    }
}
