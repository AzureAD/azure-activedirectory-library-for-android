
package com.microsoft.adal;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

/**
 * Shared preferences store clear text. This class helps to encrypt/decrypt text to store.
 * API SDK >= 18 has more security with AndroidKeyStore
 * @author omercan
 */
class StorageHelper {

    private static final String TAG = "StorageHelper";

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";

    private static final byte[] SALT = "a075150a-4307-4132-a027-02f9aa48c694".getBytes();

    /**
     * expects 16 bytes
     */
    private static final byte[] IV = "0123456abcdhtf73".getBytes();

    private static final int NUM_OF_ITERATIONS = 20;

    private static final int KEY_SIZE = 256;

    private Context mContext;

    public StorageHelper(Context ctx) {
        mContext = ctx;
    }

    protected String encrypt(String clearText) {
        try {
            Logger.d(TAG, "Started encrypt");
            final byte[] bytes = clearText != null ? clearText.getBytes() : new byte[0];
            SecretKey secretKey = getSecretKeyForAPI();

            Cipher pbeCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            pbeCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            String encrypted = new String(Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP),
                    AuthenticationConstants.ENCODING_UTF8);
            Logger.d(TAG, "Finished encryption");
            return encrypted;
        } catch (Exception e) {
            Logger.e(TAG, "It failed to use encryption", "", ADALError.ENCRYPTION_FAILED);
        }

        return clearText;
    }

    protected String decrypt(String value) {
        try {
            Logger.d(TAG, "Started decrypt");
            final byte[] bytes = value != null ? Base64.decode(value, Base64.DEFAULT) : new byte[0];
            SecretKey secretKey = getSecretKeyForAPI();

            Cipher pbeCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            pbeCipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            String decrypted = new String(pbeCipher.doFinal(bytes),
                    AuthenticationConstants.ENCODING_UTF8);
            Logger.d(TAG, "Finished decryption");
            return decrypted;
        } catch (Exception e) {
            Logger.e(TAG, "It failed to use encryption", "", ADALError.ENCRYPTION_FAILED);
        }

        return value;
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

        return getSecretKey(new String(getPassword()));

    }

    private static String getDeviceSerialNumber() {
        String num = Build.SERIAL;
        if (!StringExtensions.IsNullOrBlank(num)) {
            return num;
        }

        return "c5b1b988-955d-45ca-acd1-b52a1017bc48";
    }

    /**
     * Pre Android API 18 does not have storage for password
     * 
     * @return
     */
    final private byte[] getPassword() {
        return (mContext.getPackageName() + getDeviceSerialNumber() + Build.SERIAL + "B58AD341-0D4B-46B8-A81B-552B86051BDD")
                .getBytes();
    }

    final private SecretKey getSecretKey(String password) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
        SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray(), SALT,
                NUM_OF_ITERATIONS, KEY_SIZE));
        SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
        return secretKey;
    }

    /**
     * Supported API >= 18 PrivateKey is stored in AndroidKeyStore.
     * 
     * @return
     * @throws NoSuchProviderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws UnrecoverableEntryException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     */
    @TargetApi(18)
    final private SecretKey getSecretKeyFromAndroidKeyStore() throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException, CertificateException,
            IOException, KeyStoreException, UnrecoverableEntryException, InvalidKeySpecException,
            NoSuchPaddingException {

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias("AdalKey")) {
            Logger.v(TAG, "Key entry is not available");
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 100);

            // self signed cert to use privatekey as salt
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

        // read again
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry("AdalKey",
                null);
        // use private key info as password to generate secret key
        String password = entry.getPrivateKey().toString();
        if (password.length() > 150) {
            // get after modulus
            password = password.substring(100, 150);
        }
        return getSecretKey(password);
    }
}
