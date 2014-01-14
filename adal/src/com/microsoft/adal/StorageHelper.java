
package com.microsoft.adal;

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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.x500.X500Principal;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

/**
 * Shared preferences store clear text. This class helps to encrypt/decrypt text
 * to store. API SDK >= 18 has more security with AndroidKeyStore
 * 
 * @author omercan
 */
public class StorageHelper {

    private static final String TAG = "StorageHelper";

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecureRandom mRandom;

    private static final int KEY_SIZE = 256;

    /** IV Key length for AES-128 */
    public static final int DATA_KEY_LENGTH = 16;

    public static final int MAC_LENGTH = 32;
    /**
     * it is needed for AndroidKeyStore
     */
    private KeyPair mKeyPair;

    private Cipher mCipher, mWrapCipher;

    private Context mContext;

    private static final Object lockObject = new Object();

    /**
     * Load only once
     */
    private static SecretKey sKey = null;

    public StorageHelper(Context ctx) throws NoSuchAlgorithmException, NoSuchPaddingException {
        mContext = ctx;
        mRandom = new SecureRandom();
        mCipher = Cipher.getInstance(CIPHER_ALGORITHM);

    }

    private void loadKey() throws NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, KeyStoreException, CertificateException,
            NoSuchProviderException, InvalidAlgorithmParameterException,
            UnrecoverableEntryException, IOException {
        synchronized (lockObject) {
            if (sKey == null) {
                loadKeyPair();

                sKey = getSecretKeyForAPI();
            }
        }

        if (sKey == null) {
            throw new IllegalArgumentException("key");
        }
    }

    private void loadKeyPair() throws NoSuchAlgorithmException, NoSuchPaddingException,
            KeyStoreException, CertificateException, IOException, NoSuchProviderException,
            InvalidAlgorithmParameterException, UnrecoverableEntryException {
        if (Build.VERSION.SDK_INT >= 18) {
            // Key pair only needed for API>=18
            mWrapCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            mKeyPair = getKeyPairFromAndroidKeyStore();
        }
    }

    protected String encrypt(String clearText) {
        try {
            Logger.d(TAG, "Starting encryption");

            // Get key for encryption
            loadKey();
            final byte[] bytes = clearText != null ? clearText
                    .getBytes(AuthenticationConstants.ENCODING_UTF8) : new byte[0];

            // IV: Initialization vector that is needed to start CBC
            byte[] iv = new byte[DATA_KEY_LENGTH];
            mRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Set to encrypt mode
            mCipher.init(Cipher.ENCRYPT_MODE, sKey, ivSpec);
            byte[] dataAndIV = appendBytes(mCipher.doFinal(bytes), iv);
            
            // Mac output
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(sKey);
            m.update(dataAndIV);
            byte[] macDigest = m.doFinal();
           
            // add digest to end
            byte[] data_IV_mac = appendBytes(dataAndIV, macDigest);
            String encrypted = new String(Base64.encode(data_IV_mac, Base64.NO_WRAP),
                    AuthenticationConstants.ENCODING_UTF8);
            Logger.d(TAG, "Finished encryption");
            return encrypted;
        } catch (Exception e) {
            Logger.e(TAG, "It failed to use encryption", "", ADALError.ENCRYPTION_FAILED, e);
        }

        return clearText;
    }

    protected String decrypt(String value) {
        try {
            Logger.d(TAG, "Starting decryption");
            loadKey();
            final byte[] bytes = value != null ? Base64.decode(value, Base64.DEFAULT) : new byte[0];

            // byte array order as data-iv-mac
            int ivIndex = bytes.length - DATA_KEY_LENGTH - MAC_LENGTH;
            int macIndex = bytes.length - MAC_LENGTH;
            
            // Calculate digest again and compare to the appended value
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(sKey);
            m.update(bytes, 0, macIndex );
            byte[] macDigest = m.doFinal();
            //compare digest
            assertMac(bytes, macIndex, bytes.length, macDigest);
            
            // get IV related bytes from the end and set to decryptmode with
            // that IV
            mCipher.init(Cipher.DECRYPT_MODE, sKey, new IvParameterSpec(bytes, ivIndex,
                    DATA_KEY_LENGTH));

            // Decrypt data bytes from 0 to ivindex
            String decrypted = new String(mCipher.doFinal(bytes, 0, ivIndex),
                    AuthenticationConstants.ENCODING_UTF8);
            Logger.d(TAG, "Finished decryption");
            return decrypted;
        } catch (Exception e) {
            Logger.e(TAG, "It failed to use encryption", "", ADALError.ENCRYPTION_FAILED, e);
        }

        return value;
    }

    private void assertMac(byte[] digest, int start, int end, byte[] calculated) throws DigestException {
        if (calculated.length != (end-start)) {
            throw new IllegalArgumentException("Unexpected MAC length");
        }
        byte result = 0;
        for (int i = start; i < end; i++) {
            result |= calculated[i-start] ^ digest[i];
        }
        if (result != 0) {
            throw new DigestException();
        }
    }

    private static byte[] appendBytes(byte[] data, byte[] iv) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(data);
        os.write(iv);
        return os.toByteArray();
    }

    final private SecretKey getSecretKeyForAPI() throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                // androidKeyStore can hold app specific self signed cert.
                // Private key from generated cert can be used to generate more
                // secure
                // secret key.
                return getSecretKeyFromAndroidKeyStore();
            } catch (Exception e) {
                Logger.e(TAG, "Failed to get private key from AndroidKeyStore", "",
                        ADALError.ANDROIDKEYSTORE_FAILED);
            }
        }

        return AuthenticationSettings.INSTANCE.getSecretKey();
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
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(KEY_SIZE, mRandom);
        return keygen.generateKey();
    }

    /**
     * Supported API >= 18 PrivateKey is stored in AndroidKeyStore.
     * 
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    @TargetApi(18)
    final private SecretKey getSecretKeyFromAndroidKeyStore() throws IOException,
            GeneralSecurityException {

        // Store secret key in a file after wrapping
        File keyFile = new File(mContext.getFilesDir(), "adal.secret.key");
        loadKeyPair();
        // If keyfile does not exist, it needs to generate one
        if (!keyFile.exists()) {
            Logger.v(TAG, "Key file does not exists");
            final SecretKey key = generateSecretKey();
            Logger.v(TAG, "Wrapping SecretKey");
            final byte[] keyWrapped = wrap(key);
            Logger.v(TAG, "Writing SecretKey");
            writeKeyData(keyFile, keyWrapped);
            Logger.v(TAG, "Finished writing SecretKey");
        }

        // Read from file again
        Logger.v(TAG, "Reading SecretKey");
        final byte[] encryptedKey = readKeyData(keyFile);
        final SecretKey key = unWrap(encryptedKey);
        Logger.v(TAG, "Finished reading SecretKey");
        return key;
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
    private KeyPair getKeyPairFromAndroidKeyStore() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException, NoSuchProviderException,
            InvalidAlgorithmParameterException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias("AdalKey")) {
            Logger.v(TAG, "Key entry is not available");
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 100);

            // self signed cert stored in AndroidKeyStore to asym. encrypt key
            // to a file
            final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
                    .setAlias("AdalKey")
                    .setSubject(
                            new X500Principal(String.format("CN=%s, OU=%s", "AdalKey",
                                    mContext.getPackageName()))).setSerialNumber(BigInteger.ONE)
                    .setStartDate(start.getTime()).setEndDate(end.getTime()).build();

            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA",
                    "AndroidKeyStore");
            generator.initialize(spec);
            generator.generateKeyPair();
            Logger.v(TAG, "Key entry is generated");
        } else {
            Logger.v(TAG, "Key entry is available");
        }

        // Read key pair again
        Logger.v(TAG, "Reading Key entry");
        final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(
                "AdalKey", null);
        return new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
    }

    @TargetApi(18)
    private byte[] wrap(SecretKey key) throws GeneralSecurityException {
        mWrapCipher.init(Cipher.WRAP_MODE, mKeyPair.getPublic());
        return mWrapCipher.wrap(key);
    }

    @TargetApi(18)
    private SecretKey unWrap(byte[] keyBlob) throws GeneralSecurityException {
        mWrapCipher.init(Cipher.UNWRAP_MODE, mKeyPair.getPrivate());
        return (SecretKey)mWrapCipher.unwrap(keyBlob, "AES", Cipher.SECRET_KEY);
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
