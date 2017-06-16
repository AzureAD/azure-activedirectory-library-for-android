// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.test.filters.Suppress;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class StorageHelperTests extends AndroidTestHelper {

    private static final String TAG = "StorageHelperTests";

    private static final int MIN_SDK_VERSION = 18;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null && Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            Log.d(TAG, "setup key at settings");
            setSecretKeyData();
        }
    }

    public void testEncryptDecrypt() throws GeneralSecurityException, IOException, AuthenticationException {
        String clearText = "SomeValue1234";
        encryptDecrypt(clearText);
    }

    public void testEncryptDecryptNullEmpty() {

        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        storageHelper.encrypt(null);
                    }
                });
        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        storageHelper.encrypt("");
                    }
                });

        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException, AuthenticationException {
                        storageHelper.decrypt(null);
                    }
                });

        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException, AuthenticationException {
                        storageHelper.decrypt("");
                    }
                });
    }

    public void testDecryptInvalidInput() throws
            IOException, GeneralSecurityException {
        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        assertThrowsException(
                IllegalArgumentException.class,
                "is not valid, it must be greater of equal to 0",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException, AuthenticationException {
                        storageHelper.decrypt("E1bad64");
                    }
                });

        assertThrowsException(
                IllegalArgumentException.class,
                "bad base-64",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException, AuthenticationException {
                        storageHelper.decrypt("cE1bad64");
                    }
                });

        // The following test is using the user provided key
        setSecretKeyData();
        assertThrowsException(
                DigestException.class,
                null,
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException, AuthenticationException {
                        storageHelper.decrypt("cE1" + new String(Base64.encode(
                                "U001thatShouldFail1234567890123456789012345678901234567890"
                                        .getBytes("UTF-8"), Base64.NO_WRAP), "UTF-8"));
                    }
                });
    }

    /**
     * test different size messges
     */
    public void testEncryptDecryptDifferentSizes() throws GeneralSecurityException, IOException, AuthenticationException {
        Log.d(TAG, "Starting testEncryptDecrypt_differentSizes");
        // try different block sizes
        final int sizeRange = 1000;
        StringBuilder buf = new StringBuilder(sizeRange);
        for (int i = 0; i < sizeRange; i++) {
            encryptDecrypt(buf.append("a").toString());
        }
        Log.d(TAG, "Finished testEncryptDecrypt_differentSizes");
    }

    private void encryptDecrypt(String clearText) throws GeneralSecurityException, IOException, AuthenticationException {
        Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        String encrypted = storageHelper.encrypt(clearText);
        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartex", encrypted.equals(clearText));
        String decrypted = storageHelper.decrypt(encrypted);
        assertEquals("Same as initial text", clearText, decrypted);
    }

    public void testEncryptSameText() throws GeneralSecurityException, IOException, AuthenticationException {
        // access code
        Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        String clearText = "AAAAAAAA2pILN0mn3wlYIlWk7lqOZ5qjRWXHRnqDdzsq0s4aaUVgnMQo6oXfEUYL4fAxqVQ6dXh9sMAieFDjXVhTkp3mnL2gHSnAHJFwmj9mnlgaU7kVcoujXRA3Je23PEtoqEQMQPaurakVcEl7jOsjUGWD7JdaAHsYTujd1KHoTUdBJQQ-jz4t6Cish25zn9BPocJzN56rLUqgX3dnoA1z-hY4FS_EIn_Xdvqnil29t4etVHLDZD5RJbc5R3p5MaUKqPBF8sAQvJcgW-f9ebPHzO8L87RrsVNu4keagKmOnP139KSuORBhNaD57nmEvecJWtWTIAA&redirect_uri=https%3a%2f%2fworkaad.com%2fdemoclient1&client_id=dba19db4-53de-441d-9c63-da8d6f229e5a";
        Log.d(TAG, "Starting testEncryptSameText");
        String encrypted = storageHelper.encrypt(clearText);
        String encrypted2 = storageHelper.encrypt(clearText);
        String encrypted3 = storageHelper.encrypt(clearText);

        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartex", encrypted.equals(clearText));
        assertFalse("encrypted string is not same as another encrypted call",
                encrypted.equals(encrypted2));
        assertFalse("encrypted string is not same as another encrypted call",
                encrypted.equals(encrypted3));

        String decrypted = storageHelper.decrypt(encrypted);
        String decrypted2 = storageHelper.decrypt(encrypted);
        String decrypted3 = storageHelper.decrypt(encrypted);
        assertEquals("Same as initial text", clearText, decrypted);
        assertEquals("Same as initial text", decrypted, decrypted2);
        assertEquals("Same as initial text", decrypted, decrypted3);
        Log.d(TAG, "Finished testEncryptSameText");
    }

    public void testTampering() throws GeneralSecurityException, IOException, AuthenticationException {
        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        String clearText = "AAAAAAAA2pILN0mn3wlYIlWk7lqOZ5qjRWXH";
        String encrypted =  storageHelper.encrypt(clearText);
        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartex", encrypted.equals(clearText));

        String decrypted =  storageHelper.decrypt(encrypted);
        assertTrue("Same without Tampering", decrypted.equals(clearText));
        final String flagVersion = encrypted.substring(0, 3);
        final byte[] bytes = Base64.decode(encrypted.substring(3), Base64.DEFAULT);
        final int randomlyChosenByte = 15;
        bytes[randomlyChosenByte]++;
        final String modified = new String(Base64.encode(bytes, Base64.NO_WRAP), "UTF-8");
        assertThrowsException(DigestException.class, null, new ThrowableRunnable() {
            @Override
            public void run() throws Exception {
                storageHelper.decrypt(flagVersion + modified);
            }
        });
    }

    /**
     * Make sure that version sets correctly. It needs to be tested at different
     * emulator(18 and before 18).
     * 
     */
    public void testVersion() throws GeneralSecurityException, IOException {

        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        String value = "anvaERSgvhdfgkhrebgagagfdgadfgaadfgadfgadfg435gerhawdeADFGb #$%#gf3$%1234";
        String encrypted = storageHelper.encrypt(value);
        final int knownEncryptedSubstringStart = 1;
        final int knownEncryptedSubstringEnd = 3;
        String encodeVersion = encrypted.substring(knownEncryptedSubstringStart, knownEncryptedSubstringEnd);
        assertEquals("Encode version is same", "E1", encodeVersion);
        final byte[] bytes = Base64.decode(encrypted.substring(3), Base64.DEFAULT);

        // get key version used for this data. If user upgraded to different
        // API level, data needs to be updated
        final int keyVersionLength = 4;
        String keyVersionCheck = new String(bytes, 0, keyVersionLength, "UTF-8");
        Log.v(TAG, "Key version check:" + keyVersionCheck);
        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION || AuthenticationSettings.INSTANCE.getSecretKeyData() != null) {
            assertEquals("It should use user defined", "U001", keyVersionCheck);
        } else {
            assertEquals("It should use user defined", "A001", keyVersionCheck);
        }
    }


    //Github issue #580. Suppress this unit test as we cannot make it work consistently.
    @Suppress
    @TargetApi(MIN_SDK_VERSION)
    public void testKeyPair() throws
            GeneralSecurityException, IOException {
        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            return;
        }
        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        SecretKey kp = storageHelper.loadSecretKeyForEncryption();

        assertNotNull("Keypair is not null", kp);

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        assertTrue("Keystore has the alias", keyStore.containsAlias("AdalKey"));
    }

    @TargetApi(MIN_SDK_VERSION)
    public void testMigration() throws
            GeneralSecurityException, IOException, AuthenticationException {
        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            return;
        }
        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        setSecretKeyData();
        String expectedDecrypted = "SomeValue1234";
        String encryptedInAPI17 = "cE1VTAwMb4ChefrTHHblCg0DYaK1UR456nW3q6+hqA9Cs2uB+bqcfsLzukiI+KOCdBGJV+JqhRJHBIDCOl68TYkLQAz65g=";
        String decrypted = storageHelper.decrypt(encryptedInAPI17);
        assertEquals("Expected clear text as same", expectedDecrypted, decrypted);
    }

    @TargetApi(MIN_SDK_VERSION)
    public void testGetSecretKeyFromAndroidKeyStore() throws IOException, GeneralSecurityException {

        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            return;
        }

        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);

        File keyFile = new File(context.getDir(context.getPackageName(),
                Context.MODE_PRIVATE), "adalks");
        if (keyFile.exists()) {
            keyFile.delete();
        }

        SecretKey key = storageHelper.loadSecretKeyForEncryption();
        assertNotNull("Key is not null", key);

        SecretKey key2 = storageHelper.loadSecretKeyForEncryption();
        Log.d(TAG, "Key1:" + key.toString());
        Log.d(TAG, "Key1:" + key2.toString());
        assertTrue("Key info is same", key.toString().equals(key2.toString()));
    }
    
    private void setSecretKeyData() throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        // use same key for tests
        SecretKeyFactory keyFactory = SecretKeyFactory
                .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        final int iterations = 100;
        final int keySize = 256;
        SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                "abcdedfdfd".getBytes("UTF-8"), iterations, keySize));
        SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
        AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
    }
}
