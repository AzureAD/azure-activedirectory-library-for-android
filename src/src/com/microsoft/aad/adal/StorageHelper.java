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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

/**
 * Shared preferences store clear text. This class helps to encrypt/decrypt text
 * to store. API SDK >= 18 has more security with AndroidKeyStore
 */
public class StorageHelper {

    private static final String MAC_KEY_HASH_ALGORITHM = "SHA256";

    private static final String KEY_STORE_CERT_ALIAS = "AdalKey";

    private static final String ADALKS = "adalks";

    private static final String KEYSPEC_ALGORITHM = "AES";

    private static final String WRAP_ALGORITHM = "RSA/ECB/PKCS1Padding";

    private static final String TAG = "StorageHelper";

    /**
     * AES is 16 bytes (128 bits), thus PKCS#5 padding should not work, but in
     * Java AES/CBC/PKCS5Padding is default(!) algorithm name, thus PKCS5 here
     * probably doing PKCS7. We decide to go with Java default string.
     */
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String MAC_ALGORITHM = "HmacSHA256";

    private final SecureRandom mRandom;

    private static final int KEY_SIZE = 256;

    /** IV Key length for AES-128 */
    public static final int DATA_KEY_LENGTH = 16;

    /**
     * 256 bits output for signing message
     */
    public static final int MAC_LENGTH = 32;

    /**
     * it is needed for AndroidKeyStore
     */
    private KeyPair mKeyPair;

    private Context mContext;

    public static final String VERSION_ANDROID_KEY_STORE = "A001";

    public static final String VERSION_USER_DEFINED = "U001";

    private static final int KEY_VERSION_BLOB_LENGTH = 4;

    /**
     * To keep track of encoding version and related flags
     */
    private static final String ENCODE_VERSION = "E1";

    private static final int ENCODE_VERSION_LENGTH = 2;

    private static final Object lockObject = new Object();

    private static String sBlobVersion;

    /**
     * Load only once
     */
    private static SecretKey sKey = null, sMacKey = null;

    private static SecretKey sSecretKeyFromAndroidKeyStore = null;

    public StorageHelper(Context ctx) throws NoSuchAlgorithmException, NoSuchPaddingException {
        mContext = ctx;
        mRandom = new SecureRandom();
    }

    /**
     * Get Secret Key based on API level to use in encryption. Decryption key
     * depends on version# since user can migrate to new Android.OS
     * 
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    final private void loadSecretKeyForAPI() throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        // Loading key only once for performance. If API is upgraded, it will
        // restart the device anyway. It will load the correct key for new API.
        if (sKey != null && sMacKey != null)
            return;

        synchronized (lockObject) {
            if (Build.VERSION.SDK_INT >= 18) {
                try {
                    // androidKeyStore can store app specific self signed cert.
                    // Asymmetric cryptography is used to protect the session
                    // key
                    // used for Encryption and HMac
                    sKey = getSecretKeyFromAndroidKeyStore();
                    sMacKey = getMacKey(sKey);
                    sBlobVersion = VERSION_ANDROID_KEY_STORE;
                    return;
                } catch (Exception e) {
                    Logger.e(TAG, "Failed to get private key from AndroidKeyStore", "",
                            ADALError.ANDROIDKEYSTORE_FAILED, e);
                }
            }

            sKey = getSecretKey(AuthenticationSettings.INSTANCE.getSecretKeyData());
            sMacKey = getMacKey(sKey);
            sBlobVersion = VERSION_USER_DEFINED;
        }
    }

    /**
     * load key based on version for migration
     * 
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws UnrecoverableEntryException
     * @throws IOException
     */
    private SecretKey getKeyForVersion(String keyVersion) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException, KeyStoreException,
            CertificateException, NoSuchProviderException, InvalidAlgorithmParameterException,
            UnrecoverableEntryException, IOException {

        if (keyVersion.equals(VERSION_USER_DEFINED)) {
            return getSecretKey(AuthenticationSettings.INSTANCE.getSecretKeyData());
        }

        if (keyVersion.equals(VERSION_ANDROID_KEY_STORE)) {
            if (Build.VERSION.SDK_INT >= 18) {
                try {
                    // androidKeyStore can store app specific self signed cert.
                    // Asymmetric cryptography is used to protect the session
                    // key
                    // used for Encryption and HMac
                    return getSecretKeyFromAndroidKeyStore();
                } catch (Exception e) {
                    Logger.e(TAG, "Failed to get private key from AndroidKeyStore", "",
                            ADALError.ANDROIDKEYSTORE_FAILED, e);
                }
            } else {
                throw new IllegalArgumentException(
                        String.format(
                                "keyVersion '%s' is not supported in this SDK. AndroidKeyStore is supported API18 and above.",
                                keyVersion));
            }
        }

        throw new IllegalArgumentException("keyVersion");
    }

    private SecretKey getSecretKey(byte[] rawBytes) {
        if (rawBytes != null)
            return new SecretKeySpec(rawBytes, KEYSPEC_ALGORITHM);

        throw new IllegalArgumentException("rawBytes");
    }

    /**
     * Derive mac key from given key
     * 
     * @param key
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private SecretKey getMacKey(SecretKey key) throws NoSuchAlgorithmException {
        // Some keys may not produce byte[] with getEncoded
        byte[] encodedKey = key.getEncoded();
        if (encodedKey != null) {
            MessageDigest digester = MessageDigest.getInstance(MAC_KEY_HASH_ALGORITHM);
            return new SecretKeySpec(digester.digest(encodedKey), KEYSPEC_ALGORITHM);
        }
        return key;
    }

    /**
     * load key pair from AndroidKeyStore. If not present, it will create one
     * for this app.
     * 
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws IOException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws UnrecoverableEntryException
     */
    private void loadKeyPair() throws NoSuchAlgorithmException, NoSuchPaddingException,
            KeyStoreException, CertificateException, IOException, NoSuchProviderException,
            InvalidAlgorithmParameterException, UnrecoverableEntryException {
        // AndroidKeyStore is used for API >=18
        if (Build.VERSION.SDK_INT >= 18) {
            // Key pair only needed for API>=18

            if (mKeyPair == null) {
                mKeyPair = getKeyPairFromAndroidKeyStore();
            }
        }
    }

    /**
     * encrypt text with current key based on API level
     * 
     * @param clearText
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException
     * @throws NoSuchPaddingException
     */
    public String encrypt(String clearText) throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException, IOException, NoSuchPaddingException {

        Logger.d(TAG, "Starting encryption");

        if (StringExtensions.IsNullOrBlank(clearText)) {
            throw new IllegalArgumentException("input is empty or null");
        }

        // load key for encryption if not loaded
        loadSecretKeyForAPI();
        Logger.v(TAG, "Encrypt version:"+sBlobVersion);
        final byte[] blobVersion = sBlobVersion.getBytes(AuthenticationConstants.ENCODING_UTF8);
        final byte[] bytes = clearText.getBytes(AuthenticationConstants.ENCODING_UTF8);

        // IV: Initialization vector that is needed to start CBC
        byte[] iv = new byte[DATA_KEY_LENGTH];
        mRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Set to encrypt mode
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        Mac mac = Mac.getInstance(MAC_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, sKey, ivSpec);

        byte[] encrypted = cipher.doFinal(bytes);

        // Mac output to sign encryptedData+IV. Keyversion is not included
        // in the digest. It defines what to use for Mac Key.
        mac.init(sMacKey);
        mac.update(blobVersion);
        mac.update(encrypted);
        mac.update(iv);
        byte[] macDigest = mac.doFinal();

        // Init array to store blobVersion, encrypted data, iv, macdigest
        byte[] blobVerAndEncryptedDataAndIVAndMacDigest = new byte[blobVersion.length
                + encrypted.length + iv.length + macDigest.length];
        System.arraycopy(blobVersion, 0, blobVerAndEncryptedDataAndIVAndMacDigest, 0,
                blobVersion.length);
        System.arraycopy(encrypted, 0, blobVerAndEncryptedDataAndIVAndMacDigest,
                blobVersion.length, encrypted.length);
        System.arraycopy(iv, 0, blobVerAndEncryptedDataAndIVAndMacDigest, blobVersion.length
                + encrypted.length, iv.length);
        System.arraycopy(macDigest, 0, blobVerAndEncryptedDataAndIVAndMacDigest, blobVersion.length
                + encrypted.length + iv.length, macDigest.length);

        String encryptedText = new String(Base64.encode(blobVerAndEncryptedDataAndIVAndMacDigest,
                Base64.NO_WRAP), AuthenticationConstants.ENCODING_UTF8);
        Logger.d(TAG, "Finished encryption");

        return ENCODE_VERSION + encryptedText;
    }

    public String decrypt(String value) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException, KeyStoreException,
            CertificateException, NoSuchProviderException, InvalidAlgorithmParameterException,
            UnrecoverableEntryException, IOException, InvalidKeyException, DigestException,
            IllegalBlockSizeException, BadPaddingException {

        Logger.d(TAG, "Starting decryption");

        if (StringExtensions.IsNullOrBlank(value)) {
            throw new IllegalArgumentException("input is empty or null");
        }

        if (!value.substring(0, ENCODE_VERSION_LENGTH).equals(ENCODE_VERSION)) {
            throw new IllegalArgumentException("Encode version does not match");
        }

        final byte[] bytes = Base64.decode(value.substring(ENCODE_VERSION_LENGTH), Base64.DEFAULT);

        // get key version used for this data. If user upgraded to different
        // API level, data needs to be updated
        String keyVersionCheck = new String(bytes, 0, KEY_VERSION_BLOB_LENGTH,
                AuthenticationConstants.ENCODING_UTF8);

        SecretKey versionKey = getKeyForVersion(keyVersionCheck);
        SecretKey versionMacKey = getMacKey(versionKey);

        // byte input array: encryptedData-iv-macDigest
        int ivIndex = bytes.length - DATA_KEY_LENGTH - MAC_LENGTH;
        int macIndex = bytes.length - MAC_LENGTH;
        int encryptedLength = ivIndex - KEY_VERSION_BLOB_LENGTH;
        if (ivIndex < 0 || macIndex < 0 || encryptedLength < 0) {
            throw new IllegalArgumentException(
                    "Given value is smaller than the IV vector and MAC length");
        }

        // Calculate digest again and compare to the appended value
        // incoming message: version+encryptedData+IV+Digest
        // Digest of EncryptedData+IV excluding key Version and digest
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        Mac mac = Mac.getInstance(MAC_ALGORITHM);
        mac.init(versionMacKey);
        mac.update(bytes, 0, macIndex);
        byte[] macDigest = mac.doFinal();

        // Compare digest of input message and calculated digest
        assertMac(bytes, macIndex, bytes.length, macDigest);

        // Get IV related bytes from the end and set to decrypt mode with
        // that IV.
        // It is using same cipher for different version since version# change
        // will mean upgrade to AndroidKeyStore and new Key.
        cipher.init(Cipher.DECRYPT_MODE, versionKey, new IvParameterSpec(bytes, ivIndex,
                DATA_KEY_LENGTH));

        // Decrypt data bytes from 0 to ivindex
        String decrypted = new String(cipher.doFinal(bytes, KEY_VERSION_BLOB_LENGTH,
                encryptedLength), AuthenticationConstants.ENCODING_UTF8);
        Logger.d(TAG, "Finished decryption");
        return decrypted;
    }

    private void assertMac(byte[] digest, int start, int end, byte[] calculated)
            throws DigestException {
        if (calculated.length != (end - start)) {
            throw new IllegalArgumentException("Unexpected MAC length");
        }

        byte result = 0;
        // It does not fail fast on the first not equal byte to protect against
        // timing attack.
        for (int i = start; i < end; i++) {
            result |= calculated[i - start] ^ digest[i];
        }

        if (result != 0) {
            throw new DigestException();
        }
    }

    /**
     * generate secretKey to store after wrapping with KeyStore
     * 
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    final private SecretKey generateSecretKey() throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        KeyGenerator keygen = KeyGenerator.getInstance(KEYSPEC_ALGORITHM);
        keygen.init(KEY_SIZE, mRandom);
        return keygen.generateKey();
    }

    /**
     * Supported API >= 18 PrivateKey is stored in AndroidKeyStore. Loads key
     * from the file if it exists. If not exist, it will generate one.
     * 
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    @TargetApi(18)
    final synchronized private SecretKey getSecretKeyFromAndroidKeyStore() throws IOException,
            GeneralSecurityException {

        // Loading file and unwrapping this key is causing performance issue.
        if (sSecretKeyFromAndroidKeyStore != null) {
            return sSecretKeyFromAndroidKeyStore;
        }

        // Store secret key in a file after wrapping
        File keyFile = new File(mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                ADALKS);

        loadKeyPair();
        Cipher wrapCipher = Cipher.getInstance(WRAP_ALGORITHM);
        // If keyfile does not exist, it needs to generate one
        if (!keyFile.exists()) {
            Logger.v(TAG, "Key file does not exists");
            final SecretKey key = generateSecretKey();
            Logger.v(TAG, "Wrapping SecretKey");
            final byte[] keyWrapped = wrap(wrapCipher, key);
            Logger.v(TAG, "Writing SecretKey");
            writeKeyData(keyFile, keyWrapped);
            Logger.v(TAG, "Finished writing SecretKey");
        }

        // Read from file again
        Logger.v(TAG, "Reading SecretKey");
        final byte[] encryptedKey = readKeyData(keyFile);
        sSecretKeyFromAndroidKeyStore = unwrap(wrapCipher, encryptedKey);
        Logger.v(TAG, "Finished reading SecretKey");
        return sSecretKeyFromAndroidKeyStore;
    }

    /**
     * Get key pair
     * 
     * @return
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws UnrecoverableEntryException
     */
    @TargetApi(18)
    private synchronized KeyPair getKeyPairFromAndroidKeyStore() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException, NoSuchProviderException,
            InvalidAlgorithmParameterException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_STORE_CERT_ALIAS)) {
            Logger.v(TAG, "Key entry is not available");
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 100);

            // self signed cert stored in AndroidKeyStore to asym. encrypt key
            // to a file
            String certInfo = String.format("CN=%s, OU=%s", KEY_STORE_CERT_ALIAS,
                    mContext.getPackageName());
            final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
                    .setAlias(KEY_STORE_CERT_ALIAS).setSubject(new X500Principal(certInfo))
                    .setSerialNumber(BigInteger.ONE).setStartDate(start.getTime())
                    .setEndDate(end.getTime()).build();

            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA",
                    "AndroidKeyStore");
            generator.initialize(spec);
            generator.generateKeyPair();
            Logger.v(TAG, "Key entry is generated for cert " + certInfo);
        } else {
            Logger.v(TAG, "Key entry is available");
        }

        // Read key pair again
        Logger.v(TAG, "Reading Key entry");
        final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(
                KEY_STORE_CERT_ALIAS, null);
        return new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
    }

    @TargetApi(18)
    private byte[] wrap(Cipher wrapCipher, SecretKey key) throws GeneralSecurityException {
        wrapCipher.init(Cipher.WRAP_MODE, mKeyPair.getPublic());
        return wrapCipher.wrap(key);
    }

    @TargetApi(18)
    private SecretKey unwrap(Cipher wrapCipher, byte[] keyBlob) throws GeneralSecurityException {
        wrapCipher.init(Cipher.UNWRAP_MODE, mKeyPair.getPrivate());
        return (SecretKey)wrapCipher.unwrap(keyBlob, KEYSPEC_ALGORITHM, Cipher.SECRET_KEY);
    }

    private static void writeKeyData(File file, byte[] data) throws IOException {
        Logger.d(TAG, "Writing key data to a file");
        final OutputStream out = new FileOutputStream(file);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    private static byte[] readKeyData(File file) throws IOException {
        Logger.d(TAG, "Reading key data from a file");
        final InputStream in = new FileInputStream(file);

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }

            return bytes.toByteArray();
        } finally {
            in.close();
        }
    }
}
