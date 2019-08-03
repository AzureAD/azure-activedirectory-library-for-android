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

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class JwsBuilderTests extends AndroidTestHelper {

    static final String TEST_CERT_ALIAS = "My Key Chain";

    static final String PKCS12_PASS = "changeit";

    static final String TAG = "JwsBuilderTests";

    static final String PKCS12_FILENAME = "keychain.p12";

    static final String JWS_ALGORITHM = "SHA256withRSA";

    @Test
    public void testGenerateSignedJWTPositive() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, UnrecoverableKeyException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            InvalidKeyException, SignatureException {
        final KeyStore keystore = loadTestCertificate(getInstrumentation().getContext());
        final Key key = keystore.getKey(TEST_CERT_ALIAS, PKCS12_PASS.toCharArray());
        final RSAPrivateKey privKey = (RSAPrivateKey) key;
        final Certificate cert = keystore.getCertificate(TEST_CERT_ALIAS);
        final RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();

        testSignedJWT(
                true, // valid signature flag
                "nonce",
                "https://someurl",
                privKey,
                publicKey,
                (X509Certificate) cert
        );
    }

    @Test
    public void testGenerateSignedJWTNegative() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        final Object jwsBuilder = getInstance();
        final Method m = ReflectionUtils.getTestMethod(jwsBuilder, "generateSignedJWT", String.class,
                String.class, RSAPrivateKey.class, RSAPublicKey.class, X509Certificate.class);

        try {
            m.invoke(jwsBuilder, null, "https://someurl", null, null, null);
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("nonce"));
        }

        try {
            m.invoke(jwsBuilder, "nonce", null, null, null, null);
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("audience"));
        }

        try {
            m.invoke(jwsBuilder, "nonce", "url", null, null, null);
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("privateKey"));
        }
    }

    /**
     * send invalid public and private key
     */
    @Test
    public void testGenerateSignedJWTKeyPair() throws InvalidKeyException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchAlgorithmException,
            SignatureException, UnrecoverableKeyException, KeyStoreException, CertificateException,
            IOException {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        final int keySize = 1024;
        keyGen.initialize(keySize);
        final KeyPair keyPair = keyGen.genKeyPair();
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey privateKey = (RSAPrivateKey) keyGen.genKeyPair().getPrivate();

        testSignedJWT(
                false,
                "invalid key pairs",
                "https://someurl",
                privateKey,
                publicKey,
                (X509Certificate) loadTestCertificate(
                        getInstrumentation()
                                .getContext()
                ).getCertificateChain("My Key Chain")[0]
        );
    }

    private void testSignedJWT(boolean validSignature,
                               final String nonce,
                               final String url,
                               final RSAPrivateKey privKey,
                               final RSAPublicKey publicKey,
                               final X509Certificate cert) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, InvalidKeyException,
            NoSuchAlgorithmException, UnsupportedEncodingException, SignatureException {
        final Object jwsBuilder = getInstance();

        final Method m = ReflectionUtils.getTestMethod(
                jwsBuilder,
                "generateSignedJWT", // method name
                String.class,
                String.class,
                RSAPrivateKey.class,
                RSAPublicKey.class,
                X509Certificate.class
        );

        final String jws = (String) m.invoke(jwsBuilder, nonce, url, privKey, publicKey, cert);

        Logger.i(TAG, "Generated JWS. ", "JWS: " + jws);
        verify(validSignature, jws, publicKey, nonce, url);
    }

    private void verify(boolean validSignature,
                        final String jws,
                        final RSAPublicKey publicKey,
                        final String nonce,
                        final String submiturl) throws NoSuchAlgorithmException,
            InvalidKeyException, UnsupportedEncodingException, SignatureException {
        int dot1 = jws.indexOf(".");
        assertFalse("Serialization error", dot1 == -1);
        int dot2 = jws.indexOf(".", dot1 + 1);
        assertFalse("Serialization error", dot2 == -1);

        final String header = jws.substring(0, dot1);
        final String body = jws.substring(dot1 + 1, dot2);
        final String signature = jws.substring(dot2 + 1);

        final byte[] signatureBytes = Base64.decode(
                signature,
                Base64.NO_WRAP | Base64.URL_SAFE
        );

        final byte[] signInput = (header + "." + body).getBytes(StandardCharsets.UTF_8);

        final Signature verifier = Signature.getInstance(JWS_ALGORITHM);
        verifier.initVerify(publicKey);
        verifier.update(signInput);
        assertEquals("Signature verify", validSignature, verifier.verify(signatureBytes));

        final String headerText = new String(
                Base64.decode(header, Base64.DEFAULT),
                StandardCharsets.UTF_8
        );

        final String bodyText = new String(
                Base64.decode(body, Base64.DEFAULT),
                StandardCharsets.UTF_8
        );

        assertTrue("Header has alg field", headerText.contains("alg\":\"RS256\""));
        assertTrue("Header has type field", headerText.contains("typ\":\"JWT\""));
        assertTrue("Header has type field", headerText.contains("x5c\""));
        assertTrue("Body has nonce field", bodyText.contains("nonce\":\"" + nonce + "\""));
        assertTrue("Body has submiturl field", bodyText.contains("aud\":\"" + submiturl + "\""));
        assertTrue("Body has iat field", bodyText.contains("iat\":"));
    }

    static KeyStore loadTestCertificate(final Context ctx) throws IOException, CertificateException,
            KeyStoreException, NoSuchAlgorithmException {
        final KeyStore caKs = KeyStore.getInstance("PKCS12");

        final BufferedInputStream stream = new BufferedInputStream(
                ctx.getAssets().open(PKCS12_FILENAME)
        );

        caKs.load(stream, PKCS12_PASS.toCharArray());

        // CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final Enumeration<String> e = caKs.aliases();
        while (e.hasMoreElements()) {
            final String alias = e.nextElement();
            Logger.i(TAG, "", "--- Entry Alias: \"" + alias + "\" ---");
            if (caKs.isKeyEntry(alias)) {
                Logger.v(TAG, "Key Entry.");
                final Certificate[] certs = caKs.getCertificateChain(alias);
                Logger.v(TAG, "Cert Chain: (length " + certs.length + ")");
                for (int i = 0; i < certs.length; i++) {
                    final X509Certificate cert = (X509Certificate) certs[i];
                    final X500Principal subject = cert.getSubjectX500Principal();

                    Logger.v(
                            TAG,
                            "",
                            "Encoded:"
                                    + new String(Base64.encode(cert.getEncoded(), Base64.DEFAULT),
                                    StandardCharsets.UTF_8), null
                    );

                    Logger.v(
                            TAG,
                            "",
                            "Subject:" + subject.toString(), null
                    );
                }
            } else if (caKs.isCertificateEntry(alias)) {
                final Certificate cert = caKs.getCertificate(alias);
                Logger.i(TAG, "Trusted Certificate Entry.", cert.toString());
            }
        }

        return caKs;
    }

    public static String getThumbPrintFromCert(final X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        final byte[] der = cert.getEncoded();
        md.update(der);
        final byte[] digest = md.digest();
        return bytesToHexString(digest);
    }

    private static String bytesToHexString(final byte[] bytes) {
        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < bytes.length; ++i) {
            buf.append(String.format("%02x", bytes[i]));
        }
        return buf.toString();
    }

    private Object getInstance() throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException {
        final Class clazz = Class.forName("com.microsoft.identity.common.adal.internal.JWSBuilder");
        final Constructor<?> constructorParams = clazz.getDeclaredConstructor();
        constructorParams.setAccessible(true);
        return constructorParams.newInstance();
    }
}
