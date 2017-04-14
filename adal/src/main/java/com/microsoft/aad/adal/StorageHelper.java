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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

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
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * Shared preferences store clear text. This class helps to encrypt/decrypt text
 * to store. API SDK >= 18 has more security with AndroidKeyStore.
 * Note: {@link StorageHelper} is designed for the ADAL internal encryption/decryption.
 * Don't take dependency on it for external use. 
 */
public class StorageHelper {
    private static final String TAG = "StorageHelper";
    
    /**
     * HMac key hashing alogirighm.
     */
    private static final String HMAC_KEY_HASH_ALGORITHM = "SHA256";

    /**
     * Cert alias persisting the keypair in AndroidKeyStore. 
     */
    private static final String KEY_STORE_CERT_ALIAS = "AdalKey";

    /**
     * Name of the file contains the symmetric key used for encryption/decryption.
     */
    private static final String ADALKS = "adalks";

    /**
     * Key spec algorithm.
     */
    private static final String KEYSPEC_ALGORITHM = "AES";

    /**
     * Algorithm for key wrapping.
     */
    private static final String WRAP_ALGORITHM = "RSA/ECB/PKCS1Padding";

    /**
     * AES is 16 bytes (128 bits), thus PKCS#5 padding should not work, but in
     * Java AES/CBC/PKCS5Padding is default(!) algorithm name, thus PKCS5 here
     * probably doing PKCS7. We decide to go with Java default string.
     */
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final int KEY_SIZE = 256;

    /**
     * IV Key length for AES-128.
     */
    public static final int DATA_KEY_LENGTH = 16;

    /**
     * 256 bits output for signing message.
     */
    public static final int HMAC_LENGTH = 32;

    /**
     * Indicate that token item is encrypted with the key persisted in AndroidKeyStore.
     */
    public static final String VERSION_ANDROID_KEY_STORE = "A001";

    /**
     * Indicate that the token item is encrypted with the user provided key.
     */
    public static final String VERSION_USER_DEFINED = "U001";

    private static final int KEY_VERSION_BLOB_LENGTH = 4;

    /**
     * To keep track of encoding version and related flags.
     */
    private static final String ENCODE_VERSION = "E1";
    
    private static final int KEY_FILE_SIZE = 1024;

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private final Context mContext;
    private final SecureRandom mRandom;

    /**
     * Public and private keys that are generated in AndroidKeyStore. 
     */
    private KeyPair mKeyPair;
    private String mBlobVersion;
    private SecretKey mKey = null;
    private SecretKey mHMACKey = null;
    private SecretKey mSecretKeyFromAndroidKeyStore = null;

    /**
     * Constructor for {@link StorageHelper}.
     * @param context The {@link Context} to create {@link StorageHelper}.
     */
    public StorageHelper(Context context) {
        mContext = context;
        mRandom = new SecureRandom();
    }

    /**
     * Encrypt text with current key based on API level.
     *
     * @param clearText Clear text to encrypt. 
     * @return Encrypted blob.
     * @throws GeneralSecurityException for key related exceptions.
     * @throws IOException For general IO related exceptions.
     */
    public String encrypt(final String clearText)
            throws GeneralSecurityException, IOException {
        Logger.v(TAG, "Starting encryption");

        if (StringExtensions.isNullOrBlank(clearText)) {
            throw new IllegalArgumentException("Input is empty or null");
        }

        // load key for encryption if not loaded
        mKey = loadSecretKeyForEncryption();
        mHMACKey = getHMacKey(mKey);
        
        Logger.v(TAG, "Encrypt version:" + mBlobVersion);
        final byte[] blobVersion = mBlobVersion.getBytes(AuthenticationConstants.ENCODING_UTF8);
        final byte[] bytes = clearText.getBytes(AuthenticationConstants.ENCODING_UTF8);

        // IV: Initialization vector that is needed to start CBC
        final byte[] iv = new byte[DATA_KEY_LENGTH];
        mRandom.nextBytes(iv);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Set to encrypt mode
        final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, mKey, ivSpec);

        final byte[] encrypted = cipher.doFinal(bytes);

        // Mac output to sign encryptedData+IV. Keyversion is not included
        // in the digest. It defines what to use for Mac Key.
        mac.init(mHMACKey);
        mac.update(blobVersion);
        mac.update(encrypted);
        mac.update(iv);
        final byte[] macDigest = mac.doFinal();

        // Init array to store blobVersion, encrypted data, iv, macdigest
        final byte[] blobVerAndEncryptedDataAndIVAndMacDigest = new byte[blobVersion.length
                + encrypted.length + iv.length + macDigest.length];
        System.arraycopy(blobVersion, 0, blobVerAndEncryptedDataAndIVAndMacDigest, 0,
                blobVersion.length);
        System.arraycopy(encrypted, 0, blobVerAndEncryptedDataAndIVAndMacDigest,
                blobVersion.length, encrypted.length);
        System.arraycopy(iv, 0, blobVerAndEncryptedDataAndIVAndMacDigest, blobVersion.length
                + encrypted.length, iv.length);
        System.arraycopy(macDigest, 0, blobVerAndEncryptedDataAndIVAndMacDigest, blobVersion.length
                + encrypted.length + iv.length, macDigest.length);

        final String encryptedText = new String(Base64.encode(blobVerAndEncryptedDataAndIVAndMacDigest,
                Base64.NO_WRAP), AuthenticationConstants.ENCODING_UTF8);
        Logger.v(TAG, "Finished encryption");

        return getEncodeVersionLengthPrefix() + ENCODE_VERSION + encryptedText;
    }

    /**
     * Decrypt encrypted blob with either user provided key or key persisted in AndroidKeyStore. 
     * @param encryptedBlob The blob to decrypt
     * @return Decrypted clear text.
     * @throws GeneralSecurityException for key related exceptions.
     * @throws IOException For general IO related exceptions.
     */
    public String decrypt(final String encryptedBlob)
            throws GeneralSecurityException, IOException {
        Logger.v(TAG, "Starting decryption");

        if (StringExtensions.isNullOrBlank(encryptedBlob)) {
            throw new IllegalArgumentException("Input is empty or null");
        }

        int encodeVersionLength = encryptedBlob.charAt(0) - 'a';
        if (encodeVersionLength <= 0) {
            throw new IllegalArgumentException(String.format(
                    "Encode version length: '%s' is not valid, it must be greater of equal to 0",
                    encodeVersionLength));
        }
        if (!encryptedBlob.substring(1, 1 + encodeVersionLength).equals(ENCODE_VERSION)) {
            throw new IllegalArgumentException(String.format(
                    "Encode version received was: '%s', Encode version supported is: '%s'", encryptedBlob,
                    ENCODE_VERSION));
        }

        final byte[] bytes = Base64
                .decode(encryptedBlob.substring(1 + encodeVersionLength), Base64.DEFAULT);

        // get key version used for this data. If user upgraded to different
        // API level, data needs to be updated
        final String keyVersion = new String(bytes, 0, KEY_VERSION_BLOB_LENGTH,
                AuthenticationConstants.ENCODING_UTF8);
        Logger.v(TAG, "Encrypt version:" + keyVersion);

        final SecretKey secretKey = getKey(keyVersion);
        final SecretKey hmacKey = getHMacKey(secretKey);

        // byte input array: encryptedData-iv-macDigest
        final int ivIndex = bytes.length - DATA_KEY_LENGTH - HMAC_LENGTH;
        final int macIndex = bytes.length - HMAC_LENGTH;
        final int encryptedLength = ivIndex - KEY_VERSION_BLOB_LENGTH;
        if (ivIndex < 0 || macIndex < 0 || encryptedLength < 0) {
            throw new IOException("Invalid byte array input for decryption.");
        }

        // Calculate digest again and compare to the appended value
        // incoming message: version+encryptedData+IV+Digest
        // Digest of EncryptedData+IV excluding key Version and digest
        final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(hmacKey);
        mac.update(bytes, 0, macIndex);
        final byte[] macDigest = mac.doFinal();

        // Compare digest of input message and calculated digest
        assertHMac(bytes, macIndex, bytes.length, macDigest);

        // Get IV related bytes from the end and set to decrypt mode with
        // that IV.
        // It is using same cipher for different version since version# change
        // will mean upgrade to AndroidKeyStore and new Key.
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(bytes, ivIndex,
                DATA_KEY_LENGTH));

        // Decrypt data bytes from 0 to ivindex
        final String decrypted = new String(cipher.doFinal(bytes, KEY_VERSION_BLOB_LENGTH,
                encryptedLength), AuthenticationConstants.ENCODING_UTF8);
        Logger.v(TAG, "Finished decryption");
        return decrypted;
    }

    /**
     * Get Secret Key based on API level to use in encryption. Decryption key
     * depends on version# since user can migrate to new Android.OS
     *
     * @return SecretKey Get Secret Key based on API level to use in encryption.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    synchronized SecretKey loadSecretKeyForEncryption() throws IOException,
            GeneralSecurityException {
        // Loading key only once for performance. If API is upgraded, it will
        // restart the device anyway. It will load the correct key for new API.
        if (mKey != null && mHMACKey != null) {
            return mKey;
        }

        final byte[] secretKeyData = AuthenticationSettings.INSTANCE.getSecretKeyData();
        if (secretKeyData == null) {
            mBlobVersion = VERSION_ANDROID_KEY_STORE;
        } else {
            mBlobVersion = VERSION_USER_DEFINED;
        }

        return getKeyOrCreate(mBlobVersion);
    }

    /**
     * For API <18 or user provide the key, will return the user supplied key.
     * Supported API >= 18 PrivateKey is stored in AndroidKeyStore. Loads key
     * from the file if it exists. If not exist, it will generate one.
     * @param keyVersion The key type of the keys used to encrypt data, could be user provided key
     *                   or key persisted in the keystore.
     * @return The {@link SecretKey} used to encrypt data.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private synchronized SecretKey getKeyOrCreate(final String keyVersion)
            throws GeneralSecurityException, IOException {
        if (VERSION_USER_DEFINED.equals(keyVersion)) {
            return getSecretKey(AuthenticationSettings.INSTANCE.getSecretKeyData());
        }

        try {
            mSecretKeyFromAndroidKeyStore = getKey(keyVersion);
        } catch (final IOException | GeneralSecurityException exception) {
            Logger.v(TAG, "Key does not exist in AndroidKeyStore, try to generate new keys.");
        }

        if (mSecretKeyFromAndroidKeyStore == null) {
            // If encountering exception for reading keys, try to generate new keys
            mKeyPair = generateKeyPairFromAndroidKeyStore();
            
            // Also generate new secretkey
            mSecretKeyFromAndroidKeyStore = generateSecretKey();
            final byte[] keyWrapped = wrap(mSecretKeyFromAndroidKeyStore);
            writeKeyData(keyWrapped);
        }

        return mSecretKeyFromAndroidKeyStore;
    }

    /**
     * Get the saved key. Will only do read operation. 
     * @param keyVersion whether the key is user defined or in Android key store
     * @return SecretKey
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private synchronized SecretKey getKey(final String keyVersion) throws GeneralSecurityException, IOException {
        switch (keyVersion) {
        case VERSION_USER_DEFINED : 
            return getSecretKey(AuthenticationSettings.INSTANCE.getSecretKeyData());
        case VERSION_ANDROID_KEY_STORE :

            if (mSecretKeyFromAndroidKeyStore != null) {
                return mSecretKeyFromAndroidKeyStore;
            }
            // androidKeyStore can store app specific self signed cert.
            // Asymmetric cryptography is used to protect the session key
            // used for Encryption and HMac
            mKeyPair = readKeyPair();
            mSecretKeyFromAndroidKeyStore = getUnwrappedSecretKey();
            return mSecretKeyFromAndroidKeyStore;
        default :
            throw new IOException("Unknown keyVersion.");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized KeyPair generateKeyPairFromAndroidKeyStore()
            throws GeneralSecurityException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);

        Logger.v(TAG, "Generate KeyPair from AndroidKeyStore");
        final Calendar start = Calendar.getInstance();
        final Calendar end = Calendar.getInstance();
        final int certValidYears = 100;
        end.add(Calendar.YEAR, certValidYears);

        // self signed cert stored in AndroidKeyStore to asym. encrypt key
        // to a file
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA",
                ANDROID_KEY_STORE);
        generator.initialize(getKeyPairGeneratorSpec(mContext, start.getTime(), end.getTime()));
        try {
            return generator.generateKeyPair();
        } catch (final IllegalStateException exception) {
            // There is an issue with AndroidKeyStore when attempting to generate keypair
            // if user doesn't have pin/passphrase setup for their lock screen. 
            // Issue 177459 : AndroidKeyStore KeyPairGenerator fails to generate 
            // KeyPair after toggling lock type, even without setting the encryptionRequired 
            // flag on the KeyPairGeneratorSpec. 
            // https://code.google.com/p/android/issues/detail?id=177459
            // The thrown exception in this case is: 
            // java.lang.IllegalStateException: could not generate key in keystore
            // To avoid app crashing, re-throw as checked exception
            throw new KeyStoreException(exception);
        }
    }

    /**
     * Read KeyPair from AndroidKeyStore. 
     */
    private synchronized KeyPair readKeyPair() throws GeneralSecurityException, IOException {
        if (!doesKeyPairExist()) {
            throw new KeyStoreException("KeyPair entry does not exist.");
        }

        Logger.v(TAG, "Reading Key entry");
        final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);

        final KeyStore.PrivateKeyEntry entry;
        try {
            entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                    KEY_STORE_CERT_ALIAS, null);
        } catch (final RuntimeException e) {
            // There is an issue in android keystore that resets keystore
            // Issue 61989:  AndroidKeyStore deleted after changing screen lock type
            // https://code.google.com/p/android/issues/detail?id=61989
            // in this case getEntry throws
            // java.lang.RuntimeException: error:0D07207B:asn1 encoding routines:ASN1_get_object:header too long
            // handle it as regular KeyStoreException
            throw new KeyStoreException(e);
        }
        
        return new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
    }
    
    /**
     * Check if KeyPair exists on AndroidKeyStore. 
     */
    private synchronized boolean doesKeyPairExist() throws GeneralSecurityException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        
        final boolean isKeyStoreCertAliasExisted;
        try {
            isKeyStoreCertAliasExisted = keyStore.containsAlias(KEY_STORE_CERT_ALIAS);
        } catch (final NullPointerException exception) {
            // There is an issue with Android Keystore when remote service attempts 
            // to access Keystore. 
            // Changeset found for google source to address the related issue with 
            // remote service accessing keystore :
            // https://android.googlesource.com/platform/external/sepolicy/+/0e30164b17af20f680635c7c6c522e670ecc3df3
            // The thrown exception in this case is:
            // java.lang.NullPointerException: Attempt to invoke interface method 
            // 'int android.security.IKeystoreService.exist(java.lang.String, int)' on a null object reference
            // To avoid app from crashing, re-throw as checked exception
            throw new KeyStoreException(exception);
        }
        
        return isKeyStoreCertAliasExisted;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AlgorithmParameterSpec getKeyPairGeneratorSpec(final Context context, final Date start, final Date end) {
        final String certInfo = String.format(Locale.ROOT, "CN=%s, OU=%s", KEY_STORE_CERT_ALIAS,
                context.getPackageName());
        return new KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEY_STORE_CERT_ALIAS)
                .setSubject(new X500Principal(certInfo))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start)
                .setEndDate(end)
                .build();
    }

    private SecretKey getSecretKey(final byte[] rawBytes) {
        if (rawBytes == null) {
            throw new IllegalArgumentException("rawBytes");
        }

        return new SecretKeySpec(rawBytes, KEYSPEC_ALGORITHM);
    }

    /**
     * Derive HMAC key from given key.
     * 
     * @param key SecretKey from which HMAC key has to be derived
     * @return SecretKey
     * @throws NoSuchAlgorithmException
     */
    private SecretKey getHMacKey(final SecretKey key) throws NoSuchAlgorithmException {
        // Some keys may not produce byte[] with getEncoded
        final byte[] encodedKey = key.getEncoded();
        if (encodedKey != null) {
            final MessageDigest digester = MessageDigest.getInstance(HMAC_KEY_HASH_ALGORITHM);
            return new SecretKeySpec(digester.digest(encodedKey), KEYSPEC_ALGORITHM);
        }
 
        return key;
    }

    private char getEncodeVersionLengthPrefix() {
        return (char) ('a' + ENCODE_VERSION.length());
    }

    private void assertHMac(final byte[] digest, final int start, final int end, final byte[] calculated)
            throws DigestException {
        if (calculated.length != (end - start)) { //NOPMD
            throw new IllegalArgumentException("Unexpected HMAC length");
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
     * generate secretKey to store after wrapping with KeyStore.
     * 
     * @return SecretKey
     * @throws NoSuchAlgorithmException
     */
    private SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        final KeyGenerator keygen = KeyGenerator.getInstance(KEYSPEC_ALGORITHM);
        keygen.init(KEY_SIZE, mRandom);
        return keygen.generateKey();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized SecretKey getUnwrappedSecretKey()
            throws GeneralSecurityException, IOException {
        Logger.v(TAG, "Reading SecretKey");

        final SecretKey unwrappedSecretKey;
        try {
            final byte[] wrappedSecretKey = readKeyData();
            unwrappedSecretKey = unwrap(wrappedSecretKey);
            Logger.v(TAG, "Finished reading SecretKey");
        } catch (final GeneralSecurityException | IOException ex) {
            // Reset KeyPair info so that new request will generate correct KeyPairs.
            // All tokens with previous SecretKey are not possible to decrypt.
            Logger.e(TAG, "Unwrap failed for AndroidKeyStore", "",
                    ADALError.ANDROIDKEYSTORE_FAILED, ex);
            mKeyPair = null;
            deleteKeyFile();
            resetKeyPairFromAndroidKeyStore();
            Logger.v(TAG, "Removed previous key pair info.");
            throw ex;
        }
        
        return unwrappedSecretKey;
    }

    private void deleteKeyFile() {
        // Store secret key in a file after wrapping
        final File keyFile = new File(mContext.getDir(mContext.getPackageName(),
                Context.MODE_PRIVATE), ADALKS);
        if (keyFile.exists()) {
            Logger.v(TAG, "Delete KeyFile");
            if (!keyFile.delete()) {
                Logger.v(TAG, "Delete KeyFile failed");
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized void resetKeyPairFromAndroidKeyStore() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        keyStore.deleteEntry(KEY_STORE_CERT_ALIAS);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("GetInstance")
    private byte[] wrap(final SecretKey key) throws GeneralSecurityException {
        Logger.v(TAG, "Wrap secret key.");
        final Cipher wrapCipher = Cipher.getInstance(WRAP_ALGORITHM);
        wrapCipher.init(Cipher.WRAP_MODE, mKeyPair.getPublic());
        return wrapCipher.wrap(key);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("GetInstance")
    private SecretKey unwrap(final byte[] keyBlob) throws GeneralSecurityException {
        final Cipher wrapCipher = Cipher.getInstance(WRAP_ALGORITHM);
        wrapCipher.init(Cipher.UNWRAP_MODE, mKeyPair.getPrivate());
        try {
            return (SecretKey) wrapCipher.unwrap(keyBlob, KEYSPEC_ALGORITHM, Cipher.SECRET_KEY);
        } catch (final IllegalArgumentException exception) {
            // There is issue with Android KeyStore when lock screen type is changed which could 
            // potentially wipe out keystore. 
            // Here are the two top exceptions that could be thrown due to the above issue:
            // 1) Caused by: java.security.InvalidKeyException: javax.crypto.BadPaddingException: 
            //    error:0407106B:rsa routines:RSA_padding_check_PKCS1_type_2:block type is not 02
            // 2) Caused by: java.lang.IllegalArgumentException: key.length == 0
            // Issue 61989: AndroidKeyStore deleted after changing screen lock type
            // https://code.google.com/p/android/issues/detail?id=61989
            // To avoid app crashing from 2), re-throw it as checked exception
            throw new KeyStoreException(exception);
        }
    }

    private void writeKeyData(final byte[] data) throws IOException {
        Logger.v(TAG, "Writing key data to a file");
        final File keyFile = new File(mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                ADALKS);
        final OutputStream out = new FileOutputStream(keyFile);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    private byte[] readKeyData() throws IOException {
        final File keyFile = new File(mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                ADALKS);
        if (!keyFile.exists()) {
            throw new IOException("Key file to read does not exist");
        }
        
        Logger.v(TAG, "Reading key data from a file");
        final InputStream in = new FileInputStream(keyFile);
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final byte[] buffer = new byte[KEY_FILE_SIZE];
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