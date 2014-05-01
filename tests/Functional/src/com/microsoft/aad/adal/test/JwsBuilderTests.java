
package com.microsoft.aad.adal.test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationException;

import android.content.Context;
import android.util.Base64;

public class JwsBuilderTests extends AndroidTestHelper {

    private static final String TEST_CERT_ALIAS = "testCertAlias";

    private static final String PKCS12_PASS = "changeit";

    static final String TAG = "JwsBuilderTests";

    static final String PKCS12_FILENAME = "keychain.p12";

    static final String JWS_ALGORITHM = "SHA256withRSA";

    public void testGenerateSignedJWT_positive() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, UnrecoverableKeyException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            InvalidKeyException, SignatureException {
        Object jwsBuilder = getInstance();
        Method m = ReflectionUtils.getTestMethod(jwsBuilder, "generateSignedJWT", String.class,
                String.class, RSAPrivateKey.class, RSAPublicKey.class, String.class);
        KeyStore keystore = loadTestCertificate();
        // load the key entry from the keystore
        Key key = keystore.getKey(TEST_CERT_ALIAS, PKCS12_PASS.toCharArray());
        RSAPrivateKey privKey = (RSAPrivateKey)key;
        Certificate cert = keystore.getCertificate(TEST_CERT_ALIAS);
        RSAPublicKey publicKey = (RSAPublicKey)cert.getPublicKey();

        String jws = (String)m.invoke(jwsBuilder, "nonce", "https://someurl", privKey, publicKey);

        verify(jws, publicKey, "nonce", "https://someurl");
    }

    private void verify(final String jws, RSAPublicKey publicKey, final String nonce,
            final String submiturl) throws NoSuchAlgorithmException, InvalidKeyException,
            UnsupportedEncodingException, SignatureException {
        Signature verifier = Signature.getInstance(JWS_ALGORITHM);
        verifier.initVerify(publicKey);
        int dot1 = jws.indexOf(".");
        assertFalse("Serialization error", dot1 == -1);
        int dot2 = jws.indexOf(".", dot1 + 1);
        assertFalse("Serialization error", dot2 == -1);
        int dot3 = jws.indexOf(".", dot2 + 1);
        assertFalse("Serialization error", dot3 == -1);

        String header = jws.substring(0, dot1);
        String body = jws.substring(dot1 + 1, dot2);
        String signature = jws.substring(dot2 + 1);
        byte[] signInput = (header + "." + body).getBytes(AuthenticationConstants.ENCODING_UTF8);
        verifier.update(signInput);
        assertTrue("Signature verify",
                verifier.verify(signature.getBytes(AuthenticationConstants.ENCODING_UTF8)));

        String headerText = new String(Base64.decode(header, Base64.NO_WRAP | Base64.URL_SAFE),
                AuthenticationConstants.ENCODING_UTF8);
        String bodyText = new String(Base64.decode(body, Base64.NO_WRAP | Base64.URL_SAFE),
                AuthenticationConstants.ENCODING_UTF8);
        assertTrue("Header has alg field", headerText.contains("alg:\"RS256\""));
        assertTrue("Header has type field", headerText.contains("typ:\"jwt\""));
        assertTrue("Body has nonce field", bodyText.contains("nonce:\"" + nonce + "\""));
        assertTrue("Body has submiturl field", bodyText.contains("aud:\"" + submiturl + "\""));
    }

    private KeyStore loadTestCertificate() throws IOException, CertificateException,
            UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        Context ctx = getInstrumentation().getContext();
        KeyStore caKs = KeyStore.getInstance("PKCS12");
        caKs.load(null);
        BufferedInputStream stream = new BufferedInputStream(ctx.getAssets().open(PKCS12_FILENAME));
        caKs.load(stream, PKCS12_PASS.toCharArray());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(stream);
        caKs.setCertificateEntry(TEST_CERT_ALIAS, cert);
        return caKs;
    }

    private Object getInstance() throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException {
        Class clazz = Class.forName("com.microsoft.aad.adal.JWSBuilder");
        Constructor<?> constructorParams = clazz.getDeclaredConstructor();
        constructorParams.setAccessible(true);
        return constructorParams.newInstance();
    }
}
