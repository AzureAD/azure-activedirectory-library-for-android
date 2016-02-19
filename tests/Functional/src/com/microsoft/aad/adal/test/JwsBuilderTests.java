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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;

import javax.security.auth.x500.X500Principal;

import junit.framework.Assert;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.Logger;

public class JwsBuilderTests extends AndroidTestHelper {

    private static final String TEST_CERT_ALIAS = "My Key Chain";

    private static final String PKCS12_PASS = "changeit";

    static final String TAG = "JwsBuilderTests";

    static final String PKCS12_FILENAME = "keychain.p12";

    static final String JWS_ALGORITHM = "SHA256withRSA";

    public void testGenerateSignedJWT_positive() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, UnrecoverableKeyException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            InvalidKeyException, SignatureException {
        KeyStore keystore = loadTestCertificate();
        Key key = keystore.getKey(TEST_CERT_ALIAS, PKCS12_PASS.toCharArray());
        RSAPrivateKey privKey = (RSAPrivateKey)key;
        Certificate cert = keystore.getCertificate(TEST_CERT_ALIAS);
        RSAPublicKey publicKey = (RSAPublicKey)cert.getPublicKey();

        testSignedJWT(true, "nonce", "https://someurl", privKey, publicKey, (X509Certificate)cert);
    }

    public void testGenerateSignedJWT_negative() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Object jwsBuilder = getInstance();
        Method m = ReflectionUtils.getTestMethod(jwsBuilder, "generateSignedJWT", String.class,
                String.class, RSAPrivateKey.class, RSAPublicKey.class, X509Certificate.class);

        try {
            m.invoke(jwsBuilder, null, "https://someurl", null, null, null);
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument excetpion", ex.getCause().getMessage().contains("nonce"));
        }

        try {
            m.invoke(jwsBuilder, "nonce", null, null, null, null);
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("audience"));
        }

        try {
            m.invoke(jwsBuilder, "nonce", "url", null, null, null);
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("privateKey"));
        }
    }

    /**
     * send invalid public and private key
     * 
     * @throws SignatureException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws InvalidKeyException
     * @throws IOException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     */
    public void testGenerateSignedJWT_KeyPair() throws InvalidKeyException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchAlgorithmException,
            SignatureException, UnrecoverableKeyException, KeyStoreException, CertificateException,
            IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey)keyGen.genKeyPair().getPrivate();

        testSignedJWT(false, "invalid key pairs", "https://someurl", privateKey, publicKey,
                (X509Certificate)loadTestCertificate().getCertificateChain("My Key Chain")[0]);
    }

    private void testSignedJWT(boolean validSignature, String nonce, String url,
            RSAPrivateKey privKey, RSAPublicKey publicKey, X509Certificate cert)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
            InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException,
            SignatureException {
        Object jwsBuilder = getInstance();
        Method m = ReflectionUtils.getTestMethod(jwsBuilder, "generateSignedJWT", String.class,
                String.class, RSAPrivateKey.class, RSAPublicKey.class, X509Certificate.class);
        String jws = (String)m.invoke(jwsBuilder, nonce, url, privKey, publicKey, cert);
        Logger.v(TAG, "Generated JWS:" + jws);
        verify(validSignature, jws, publicKey, nonce, url);
    }

    private void verify(boolean validSignature, final String jws, RSAPublicKey publicKey,
            final String nonce, final String submiturl) throws NoSuchAlgorithmException,
            InvalidKeyException, UnsupportedEncodingException, SignatureException {
        int dot1 = jws.indexOf(".");
        assertFalse("Serialization error", dot1 == -1);
        int dot2 = jws.indexOf(".", dot1 + 1);
        assertFalse("Serialization error", dot2 == -1);

        String header = jws.substring(0, dot1);
        String body = jws.substring(dot1 + 1, dot2);
        String signature = jws.substring(dot2 + 1);
        byte[] signatureBytes = Base64.decode(signature, Base64.NO_WRAP | Base64.URL_SAFE);
        byte[] signInput = (header + "." + body).getBytes(AuthenticationConstants.ENCODING_UTF8);
        Signature verifier = Signature.getInstance(JWS_ALGORITHM);
        verifier.initVerify(publicKey);
        verifier.update(signInput);
        assertEquals("Signature verify", validSignature, verifier.verify(signatureBytes));

        String headerText = new String(Base64.decode(header, Base64.DEFAULT),
                AuthenticationConstants.ENCODING_UTF8);
        String bodyText = new String(Base64.decode(body, Base64.DEFAULT),
                AuthenticationConstants.ENCODING_UTF8);
        assertTrue("Header has alg field", headerText.contains("alg\":\"RS256\""));
        assertTrue("Header has type field", headerText.contains("typ\":\"JWT\""));
        assertTrue("Header has type field", headerText.contains("x5c\""));
        assertTrue("Body has nonce field", bodyText.contains("nonce\":\"" + nonce + "\""));
        assertTrue("Body has submiturl field", bodyText.contains("aud\":\"" + submiturl + "\""));
        assertTrue("Body has iat field", bodyText.contains("iat\":"));
    }

    private KeyStore loadTestCertificate() throws IOException, CertificateException,
            UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        Context ctx = getInstrumentation().getContext();
        KeyStore caKs = KeyStore.getInstance("PKCS12");

        BufferedInputStream stream = new BufferedInputStream(ctx.getAssets().open(PKCS12_FILENAME));
        caKs.load(stream, PKCS12_PASS.toCharArray());

        // CertificateFactory cf = CertificateFactory.getInstance("X.509");

        for (Enumeration<String> e = caKs.aliases(); e.hasMoreElements();) {
            String alias = e.nextElement();
            Log.v(TAG, "--- Entry Alias: \"" + alias + "\" ---");
            if (caKs.isKeyEntry(alias)) {
                Log.v(TAG, "Key Entry:");
                Certificate[] certs = caKs.getCertificateChain(alias);
                Log.v(TAG, "Cert Chain: (length " + certs.length + ")");
                for (int i = 0; i < certs.length; i++) {
                    X509Certificate cert = (X509Certificate)certs[i];
                    X500Principal subject = cert.getSubjectX500Principal();
                    Log.v(TAG,
                            "Encoded:"
                                    + new String(Base64.encode(cert.getEncoded(), Base64.DEFAULT),
                                            "utf-8"));
                    Log.v(TAG, "Subject:" + subject.toString());
                }

            } else if (caKs.isCertificateEntry(alias)) {
                Log.v(TAG, "Trusted Certificate Entry:");
                Certificate cert = caKs.getCertificate(alias);
                Log.v(TAG, cert.toString());
            }
        }

        return caKs;
    }

    public static String getThumbPrintFromCert(X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return hexify(digest);
    }

    private static String hexify(byte bytes[]) {
        char[] hexDigits = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        };
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
        }
        return buf.toString();
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
