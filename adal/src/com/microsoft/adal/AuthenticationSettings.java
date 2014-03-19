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

package com.microsoft.adal;

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
}
