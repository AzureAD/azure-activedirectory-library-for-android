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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Base64;

/**
 * Shared preferences store clear text. This class helps to encrypt/decrypt text
 * to store. API SDK >= 18 has more security with AndroidKeyStore.
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

    /**
     * IV Key length for AES-128.
     */
    public static final int DATA_KEY_LENGTH = 16;

    /**
     * 256 bits output for signing message.
     */
    public static final int MAC_LENGTH = 32;

    /**
     * it is needed for AndroidKeyStore.
     */
    private KeyPair mKeyPair;

    private Context mContext;

    public static final String VERSION_ANDROID_KEY_STORE = "A001";

    public static final String VERSION_USER_DEFINED = "U001";

    private static final int KEY_VERSION_BLOB_LENGTH = 4;

    /**
     * To keep track of encoding version and related flags.
     */
    private static final String ENCODE_VERSION = "E1";

    private static final Object LOCK_OBJ = new Object();

    private String mBlobVersion;

    private SecretKey mKey = null, mMacKey = null;

    private static SecretKey sSecretKeyFromAndroidKeyStore = null;

    public StorageHelper(Context ctx) {
        mContext = ctx;
        mRandom = new SecureRandom();
    }

    /**
     * Get Secret Key based on API level to use in encryption. Decryption key
     * depends on version# since user can migrate to new Android.OS
     * 
     * @return
     * @throws NoSuchAlgorithmException
     */
    final private void loadSecretKeyForAPI() throws NoSuchAlgorithmException {
        // Loading key only once for performance. If API is upgraded, it will
        // restart the device anyway. It will load the correct key for new API. 
        if (mKey != null && mMacKey != null)
            return;

        synchronized (LOCK_OBJ) {
            if (Build.VERSION.SDK_INT >= 18
                    && AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
                try {
                    // androidKeyStore can store app specific self signed cert.
                    // Asymmetric cryptography is used to protect the session
                    // key for Encryption and HMac.
                    // If user specifies secret key, it will use the provided
                    // key.
                    mKey = getSecretKeyFromAndroidKeyStore();
                    mMacKey = getMacKey(mKey);
                    mBlobVersion = VERSION_ANDROID_KEY_STORE;
                    return;
                } catch (IOException | GeneralSecurityException e) {
                    Logger.e(TAG, "Failed to get private key from AndroidKeyStore", "",
                            ADALError.ANDROIDKEYSTORE_FAILED, e);
                }
            }

            Logger.v(TAG, "Encryption will use secret key from Settings");
            mKey = getSecretKey(AuthenticationSettings.INSTANCE.getSecretKeyData());
            mMacKey = getMacKey(mKey);
            mBlobVersion = VERSION_USER_DEFINED;
        }
    }

    /**
     * load key based on version for migration
     * 
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private SecretKey getKeyForVersion(String keyVersion) throws GeneralSecurityException, IOException {

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
                } catch (IOException | GeneralSecurityException e) {
                    Logger.e(TAG, "Failed to get private key from AndroidKeyStore", "",
                            ADALError.ANDROIDKEYSTORE_FAILED, e);
                    throw e;
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

    public String decrypt(String value) throws GeneralSecurityException, IOException {

        Logger.v(TAG, "Starting decryption");

        if (StringExtensions.IsNullOrBlank(value)) {
            throw new IllegalArgumentException("Input is empty or null");
        }

        int encodeVersionLength = value.charAt(0) - 'a';
        if (encodeVersionLength <= 0) {
            throw new IllegalArgumentException(String.format(
                    "Encode version length: '%s' is not valid, it must be greater of equal to 0",
                    encodeVersionLength));
        }
        if (!value.substring(1, 1 + encodeVersionLength).equals(ENCODE_VERSION)) {
            throw new IllegalArgumentException(String.format(
                    "Encode version received was: '%s', Encode version supported is: '%s'", value,
                    ENCODE_VERSION));
        }

        final byte[] bytes = Base64
                .decode(value.substring(1 + encodeVersionLength), Base64.DEFAULT);

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
        Logger.v(TAG, "Finished decryption");
        return decrypted;
    }

    private char getEncodeVersionLengthPrefix() {
        return (char)('a' + ENCODE_VERSION.length());
    }

    /**
     * encrypt text with current key based on API level
     *
     * @param clearText
     * @return
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException
     */
    public String encrypt(String clearText)
            throws GeneralSecurityException, UnsupportedEncodingException {

        Logger.v(TAG, "Starting encryption");

        if (StringExtensions.IsNullOrBlank(clearText)) {
            throw new IllegalArgumentException("Input is empty or null");
        }

        // load key for encryption if not loaded
        loadSecretKeyForAPI();
        Logger.v(TAG, "Encrypt version:" + mBlobVersion);
        final byte[] blobVersion = mBlobVersion.getBytes(AuthenticationConstants.ENCODING_UTF8);
        final byte[] bytes = clearText.getBytes(AuthenticationConstants.ENCODING_UTF8);

        // IV: Initialization vector that is needed to start CBC
        byte[] iv = new byte[DATA_KEY_LENGTH];
        mRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Set to encrypt mode
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        Mac mac = Mac.getInstance(MAC_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, mKey, ivSpec);

        byte[] encrypted = cipher.doFinal(bytes);

        // Mac output to sign encryptedData+IV. Keyversion is not included
        // in the digest. It defines what to use for Mac Key.
        mac.init(mMacKey);
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
        Logger.v(TAG, "Finished encryption");

        return getEncodeVersionLengthPrefix() + ENCODE_VERSION + encryptedText;
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
    final private SecretKey generateSecretKey() throws NoSuchAlgorithmException {
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

        if (mKeyPair == null) {
            mKeyPair = getKeyPairFromAndroidKeyStore();
            Logger.v(TAG, "Retrived keypair from androidKeyStore");
        }

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
        try {
            final byte[] encryptedKey = readKeyData(keyFile);
            sSecretKeyFromAndroidKeyStore = unwrap(wrapCipher, encryptedKey);
            Logger.v(TAG, "Finished reading SecretKey");
        } catch (GeneralSecurityException | IOException ex) {
            // Reset KeyPair info so that new request will generate correct KeyPairs.
            // All tokens with previous SecretKey are not possible to decrypt.
            Logger.e(TAG, "Unwrap failed for AndroidKeyStore", "", ADALError.ANDROIDKEYSTORE_FAILED, ex);
            mKeyPair = null;
            sSecretKeyFromAndroidKeyStore = null;
            deleteKeyFile();
            resetKeyPairFromAndroidKeyStore();
            Logger.v(TAG, "Removed previous key pair info.");
            throw ex;
        }
        return sSecretKeyFromAndroidKeyStore;
    }

    private void deleteKeyFile() {
        // Store secret key in a file after wrapping
        File keyFile = new File(mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                ADALKS);
        if (keyFile.exists()) {
            Logger.v(TAG, "Delete KeyFile");
            keyFile.delete();
        }
    }

    /**
     * Get key pair from AndroidKeyStore.
     * 
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @TargetApi(18)
    private synchronized KeyPair getKeyPairFromAndroidKeyStore() throws GeneralSecurityException, IOException {
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
            final AlgorithmParameterSpec spec = (AlgorithmParameterSpec)getKeyPairGeneratorSpec(
                    new X500Principal(certInfo), start.getTime(), end.getTime());
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
    private Object getKeyPairGeneratorSpec(X500Principal subj, Date start, Date end) {
        // TODO: use real Spec.Builder instead of reflective methods
        Class<?> clazz;
        try {
            clazz = Class.forName("android.security.KeyPairGeneratorSpec$Builder");

            Constructor<?> constructor = clazz.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);
            Object builder = constructor.newInstance(mContext);
            Method setAlias = clazz.getDeclaredMethod("setAlias", String.class);
            Method setSubject = clazz.getDeclaredMethod("setSubject", X500Principal.class);
            Method setSerialNumber = clazz.getDeclaredMethod("setSerialNumber", BigInteger.class);
            Method setStartDate = clazz.getDeclaredMethod("setStartDate", Date.class);
            Method setEndDate = clazz.getDeclaredMethod("setEndDate", Date.class);
            Method build = clazz.getDeclaredMethod("build");
            builder = setAlias.invoke(builder, KEY_STORE_CERT_ALIAS);
            builder = setSubject.invoke(builder, subj);
            builder = setSerialNumber.invoke(builder, BigInteger.ONE);
            builder = setStartDate.invoke(builder, start);
            builder = setEndDate.invoke(builder, end);
            return build.invoke(builder);
        } catch (ClassNotFoundException e) {
            Logger.e(TAG, "android.security.KeyPairGeneratorSpec.Builder is not found", "",
                    ADALError.ANDROIDKEYSTORE_KEYPAIR_GENERATOR_FAILED, e);
        } catch (IllegalArgumentException e) {
            Logger.e(TAG, "android.security.KeyPairGeneratorSpec.Builder argument is not valid",
                    "", ADALError.ANDROIDKEYSTORE_KEYPAIR_GENERATOR_FAILED, e);
        } catch (InstantiationException e) {
            Logger.e(TAG, "android.security.KeyPairGeneratorSpec.Builder is not instantiated", "",
                    ADALError.ANDROIDKEYSTORE_KEYPAIR_GENERATOR_FAILED, e);
        } catch (IllegalAccessException e) {
            Logger.e(TAG, "android.security.KeyPairGeneratorSpec.Builder is not accessible", "",
                    ADALError.ANDROIDKEYSTORE_KEYPAIR_GENERATOR_FAILED, e);
        } catch (InvocationTargetException e) {
            Logger.e(TAG, "android.security.KeyPairGeneratorSpec.Builder's method invoke failed",
                    "", ADALError.ANDROIDKEYSTORE_KEYPAIR_GENERATOR_FAILED, e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
        } catch (NoSuchMethodException e) {
            Logger.e(TAG, "android.security.KeyPairGeneratorSpec.Builder is not found", "",
                    ADALError.ANDROIDKEYSTORE_KEYPAIR_GENERATOR_FAILED, e);
        }

        return null;
    }

    @TargetApi(18)
    private synchronized void resetKeyPairFromAndroidKeyStore() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry(KEY_STORE_CERT_ALIAS);
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
        Logger.v(TAG, "Writing key data to a file");
        final OutputStream out = new FileOutputStream(file);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    private static byte[] readKeyData(File file) throws IOException {
        Logger.v(TAG, "Reading key data from a file");
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
