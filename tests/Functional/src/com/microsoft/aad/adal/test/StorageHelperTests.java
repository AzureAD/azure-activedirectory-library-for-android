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

package com.microsoft.aad.adal.test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.DigestException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import junit.framework.Assert;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

import com.microsoft.aad.adal.AuthenticationSettings;

public class StorageHelperTests extends AndroidTestCase {

    private static final String TAG = "StorageHelperTests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Log.d(TAG, "setup key at settings");
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            // use same key for tests
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes("UTF-8"), 100, 256));
            SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }
    }

    public void testEncryptDecrypt() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        String clearText = "SomeValue1234";
        encryptDecrypt(clearText);
    }

    public void testEncryptDecrypt_NullEmpty() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Object storageHelper = getStorageHelper();
        Method mDecrypt = ReflectionUtils.getTestMethod(storageHelper, "decrypt", String.class);
        Method mEncrypt = ReflectionUtils.getTestMethod(storageHelper, "encrypt", String.class);
        assertThrows(mDecrypt, storageHelper, null, "Input is empty or null");
        assertThrows(mDecrypt, storageHelper, "", "Input is empty or null");

        assertThrows(mEncrypt, storageHelper, null, "Input is empty or null");
        assertThrows(mEncrypt, storageHelper, "", "Input is empty or null");
    }

    public void testDecrypt_InvalidInput() throws NoSuchMethodException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            UnsupportedEncodingException {
        Object storageHelper = getStorageHelper();
        Method mDecrypt = ReflectionUtils.getTestMethod(storageHelper, "decrypt", String.class);
        Method mEncrypt = ReflectionUtils.getTestMethod(storageHelper, "encrypt", String.class);
        assertThrows(mDecrypt, storageHelper, "E1bad64", "Encode version length is invalid");
        assertThrows(mDecrypt, storageHelper, "cE1bad64", "bad base-64");
        assertThrowsType(
                mDecrypt,
                storageHelper,
                "cE1"
                        + new String(Base64.encode(
                                "U001thatShouldFail1234567890123456789012345678901234567890"
                                        .getBytes("UTF-8"), Base64.NO_WRAP), "UTF-8"),
                DigestException.class);

    }

    /**
     * test different size messges
     * 
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws UnsupportedEncodingException
     */
    public void testEncryptDecrypt_DifferentSizes() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, UnsupportedEncodingException {
        Log.d(TAG, "Starting testEncryptDecrypt_differentSizes");
        // try different block sizes
        StringBuilder buf = new StringBuilder(1000);
        for (int i = 0; i < 1000; i++) {
            encryptDecrypt(buf.append("a").toString());
        }
        Log.d(TAG, "Finished testEncryptDecrypt_differentSizes");
    }

    private void encryptDecrypt(String clearText) throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Object storageHelper = getStorageHelper();
        Method mEncrypt = ReflectionUtils.getTestMethod(storageHelper, "encrypt", String.class);
        Method mDecrypt = ReflectionUtils.getTestMethod(storageHelper, "decrypt", String.class);
        String encrypted = (String)mEncrypt.invoke(storageHelper, clearText);
        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartex", encrypted.equals(clearText));
        String decrypted = (String)mDecrypt.invoke(storageHelper, encrypted);
        assertEquals("Same as initial text", clearText, decrypted);
    }

    public void testEncryptSameText() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        // access code
        String clearText = "AAAAAAAA2pILN0mn3wlYIlWk7lqOZ5qjRWXHRnqDdzsq0s4aaUVgnMQo6oXfEUYL4fAxqVQ6dXh9sMAieFDjXVhTkp3mnL2gHSnAHJFwmj9mnlgaU7kVcoujXRA3Je23PEtoqEQMQPaurakVcEl7jOsjUGWD7JdaAHsYTujd1KHoTUdBJQQ-jz4t6Cish25zn9BPocJzN56rLUqgX3dnoA1z-hY4FS_EIn_Xdvqnil29t4etVHLDZD5RJbc5R3p5MaUKqPBF8sAQvJcgW-f9ebPHzO8L87RrsVNu4keagKmOnP139KSuORBhNaD57nmEvecJWtWTIAA&redirect_uri=https%3a%2f%2fworkaad.com%2fdemoclient1&client_id=dba19db4-53de-441d-9c63-da8d6f229e5a";
        Log.d(TAG, "Starting testEncryptSameText");
        Object storageHelper = getStorageHelper();
        Method mEncrypt = ReflectionUtils.getTestMethod(storageHelper, "encrypt", String.class);
        Method mDecrypt = ReflectionUtils.getTestMethod(storageHelper, "decrypt", String.class);
        String encrypted = (String)mEncrypt.invoke(storageHelper, clearText);
        String encrypted2 = (String)mEncrypt.invoke(storageHelper, clearText);
        String encrypted3 = (String)mEncrypt.invoke(storageHelper, clearText);

        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartex", encrypted.equals(clearText));
        assertFalse("encrypted string is not same as another encrypted call",
                encrypted.equals(encrypted2));
        assertFalse("encrypted string is not same as another encrypted call",
                encrypted.equals(encrypted3));

        String decrypted = (String)mDecrypt.invoke(storageHelper, encrypted);
        String decrypted2 = (String)mDecrypt.invoke(storageHelper, encrypted2);
        String decrypted3 = (String)mDecrypt.invoke(storageHelper, encrypted3);
        assertEquals("Same as initial text", clearText, decrypted);
        assertEquals("Same as initial text", decrypted, decrypted2);
        assertEquals("Same as initial text", decrypted, decrypted3);
        Log.d(TAG, "Finished testEncryptSameText");
    }

    public void testTampering() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, UnsupportedEncodingException {
        String clearText = "AAAAAAAA2pILN0mn3wlYIlWk7lqOZ5qjRWXH";
        Object storageHelper = getStorageHelper();
        Method mEncrypt = ReflectionUtils.getTestMethod(storageHelper, "encrypt", String.class);
        Method mDecrypt = ReflectionUtils.getTestMethod(storageHelper, "decrypt", String.class);
        String encrypted = (String)mEncrypt.invoke(storageHelper, clearText);
        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartex", encrypted.equals(clearText));

        String decrypted = (String)mDecrypt.invoke(storageHelper, encrypted);
        assertTrue("Same without Tampering", decrypted.equals(clearText));
        String flagVersion = encrypted.substring(0, 3);
        final byte[] bytes = Base64.decode(encrypted.substring(3), Base64.DEFAULT);
        bytes[15]++;
        String modified = new String(Base64.encode(bytes, Base64.NO_WRAP), "UTF-8");

        assertThrowsType(mDecrypt, storageHelper, flagVersion + modified, DigestException.class);
    }

    /**
     * Make sure that version sets correctly. It needs to be tested at different
     * emulator(18 and before 18).
     * 
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws UnsupportedEncodingException
     * @throws NoSuchFieldException
     */
    public void testVersion() throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, UnsupportedEncodingException, NoSuchFieldException {

        String value = "anvaERSgvhdfgkhrebgagagfdgadfgaadfgadfgadfg435gerhawdeADFGb #$%#gf3$%1234";
        Object storageHelper = getStorageHelper();
        ReflectionUtils.setFieldValue(storageHelper, "sKey", null);
        ReflectionUtils.setFieldValue(storageHelper, "sMacKey", null);
        Method m = ReflectionUtils.getTestMethod(storageHelper, "encrypt", String.class);
        String encrypted = (String)m.invoke(storageHelper, value);
        String encodeVersion = encrypted.substring(1, 3);
        assertEquals("Encode version is same", "E1", encodeVersion);
        final byte[] bytes = Base64.decode(encrypted.substring(3), Base64.DEFAULT);

        // get key version used for this data. If user upgraded to different
        // API level, data needs to be updated
        String keyVersionCheck = new String(bytes, 0, 4, "UTF-8");
        Log.v(TAG, "Key version check:" + keyVersionCheck);
        if (Build.VERSION.SDK_INT < 18) {
            assertEquals("It should use user defined", "U001", keyVersionCheck);
        } else {
            assertEquals("It should use user defined", "A001", keyVersionCheck);
        }
    }

    @TargetApi(18)
    public void testMigration() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {
        if (Build.VERSION.SDK_INT < 18) {
            return;
        }

        String expectedDecrypted = "SomeValue1234";
        String encryptedInAPI17 = "cE1VTAwMb4ChefrTHHblCg0DYaK1UR456nW3q6+hqA9Cs2uB+bqcfsLzukiI+KOCdBGJV+JqhRJHBIDCOl68TYkLQAz65g=";
        Object storageHelper = getStorageHelper();
        Method decrypt = ReflectionUtils.getTestMethod(storageHelper, "decrypt", String.class);
        String decrypted = (String)decrypt.invoke(storageHelper, encryptedInAPI17);
        assertEquals("Expected clear text as same", expectedDecrypted, decrypted);
    }

    @TargetApi(18)
    public void testKeyPair() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {
        if (Build.VERSION.SDK_INT < 18) {
            return;
        }

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        Object storageHelper = getStorageHelper();
        Method m = ReflectionUtils.getTestMethod(storageHelper, "getKeyPairFromAndroidKeyStore");

        KeyPair kp = (KeyPair)m.invoke(storageHelper);

        assertNotNull("Keypair is not null", kp);
        keyStore.load(null);

        assertTrue("Keystore has the alias", keyStore.containsAlias("AdalKey"));
    }

    @TargetApi(18)
    public void testGetSecretKeyFromAndroidKeyStore() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        if (Build.VERSION.SDK_INT < 18) {
            return;
        }

        File keyFile = new File(getContext().getDir(getContext().getPackageName(),
                Context.MODE_PRIVATE), "adalks");
        if (keyFile.exists()) {
            keyFile.delete();
        }

        Object storageHelper = getStorageHelper();
        Method m = ReflectionUtils.getTestMethod(storageHelper, "getSecretKeyFromAndroidKeyStore");

        SecretKey key = (SecretKey)m.invoke(storageHelper);
        assertNotNull("Key is not null", key);

        SecretKey key2 = (SecretKey)m.invoke(storageHelper);
        Log.d(TAG, "Key1:" + key.toString());
        Log.d(TAG, "Key1:" + key2.toString());
        assertTrue("Key info is same", key.toString().equals(key2.toString()));
    }

    private Object getStorageHelper() throws NoSuchMethodException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> constructorParams = Class.forName("com.microsoft.aad.adal.StorageHelper")
                .getDeclaredConstructor(Context.class);
        constructorParams.setAccessible(true);
        Object storageHelper = constructorParams.newInstance(getContext());
        return storageHelper;
    }

    private void assertThrows(Method m, Object obj, String input, String msg) {
        try {
            m.invoke(obj, input);
            Assert.fail("Supposed to throw");
        } catch (Exception e) {
            if (e.getMessage() == null) {
                assertTrue("Exception message", e.getCause().getMessage().contains(msg));
            } else {
                assertTrue("Exception message", e.getMessage().contains(msg));

            }
        }
    }

    private void assertThrowsType(Method m, Object obj, String input,
            final Class<? extends Exception> expected) {
        try {
            m.invoke(obj, input);
            Assert.fail("Supposed to throw");
        } catch (Exception e) {
            assertTrue(expected.isInstance(e) || expected.isInstance(e.getCause()));
        }
    }
}
