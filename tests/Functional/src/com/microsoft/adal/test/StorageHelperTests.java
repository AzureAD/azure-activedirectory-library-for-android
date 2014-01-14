
package com.microsoft.adal.test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

import com.microsoft.adal.AuthenticationSettings;

public class StorageHelperTests extends AndroidTestCase {

    private static final String TAG = "StorageHelperTests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (Build.VERSION.SDK_INT < 18) {
            Log.d(TAG, "setup key at settings");
            if (AuthenticationSettings.INSTANCE.getSecretKey() == null) {
                KeyGenerator keygen = KeyGenerator.getInstance("AES");
                keygen.init(256, new SecureRandom());
                SecretKey key = keygen.generateKey();
                AuthenticationSettings.INSTANCE.setSecretKey(key);
            }
        }

    }

    public void testEncryptDecrypt() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        String clearText = "SomeValue1234";
        encryptDecrypt(clearText);
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
    public void testEncryptDecrypt_differentSizes() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, UnsupportedEncodingException {
        Log.d(TAG, "Starting testEncryptDecrypt_differentSizes");
        SecureRandom random = new SecureRandom();
        // try different block sizes
        for (int i = 5; i < 100; i++) {
            byte[] msg = new byte[i];
            random.nextBytes(msg);
            encryptDecrypt(new String(Base64.encode(msg, Base64.NO_WRAP), "UTF-8"));
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

        final byte[] bytes = Base64.decode(encrypted, Base64.DEFAULT);
        bytes[15]++;
        String modified = new String(Base64.encode(bytes, Base64.NO_WRAP), "UTF-8");

        decrypted = (String)mDecrypt.invoke(storageHelper, modified);
        assertFalse("not same text", decrypted.equals(clearText));
        assertTrue("Failed decryption returns same thing", modified.equals(decrypted));
    }

    @TargetApi(18)
    public void testKeyPair() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {
        if (Build.VERSION.SDK_INT < 18) {
            return;
        }

        Object storageHelper = getStorageHelper();
        Method m = ReflectionUtils.getTestMethod(storageHelper, "getKeyPairFromAndroidKeyStore");

        KeyPair kp = (KeyPair)m.invoke(storageHelper);

        assertNotNull("Keypair is not null", kp);
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
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

        File keyFile = new File(getContext().getFilesDir(), "adal.secret.key");
        if (keyFile.exists()) {
            keyFile.delete();
        }

        Object storageHelper = getStorageHelper();
        Method load = ReflectionUtils.getTestMethod(storageHelper, "loadKey");
        load.invoke(storageHelper);
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
        Constructor<?> constructorParams = Class.forName("com.microsoft.adal.StorageHelper")
                .getDeclaredConstructor(Context.class);
        constructorParams.setAccessible(true);
        Object storageHelper = constructorParams.newInstance(getContext());
        return storageHelper;
    }
}
