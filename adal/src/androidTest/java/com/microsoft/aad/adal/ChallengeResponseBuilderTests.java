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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.JWSBuilder;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@RunWith(AndroidJUnit4.class)
public class ChallengeResponseBuilderTests extends AndroidTestHelper {

    static final String TAG = "ClientCertHandlerTests";

    private static final String CERT_REDIRECT = AuthenticationConstants.Broker.PKEYAUTH_REDIRECT;

    private static final String CERT_AUTH_TYPE =
            AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE;

    @Test
    public void testGetChallengeResponseFromHeaderPositive()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
                    NoSuchFieldException, NoSuchAlgorithmException, ClientException {
        final KeyPair keyPair = getKeyPair();
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        final String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        final String nonce = "123123-123213-123";
        final String context = "ABcdeded";
        final String thumbPrint = "thumbprint23432432";
        final X509Certificate mockCert = mock(X509Certificate.class);

        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        MockDeviceCertProxy.setThumbPrint(thumbPrint);
        MockDeviceCertProxy.setPrivateKey(privateKey);
        MockDeviceCertProxy.setPublicKey(publicKey);
        MockDeviceCertProxy.setCertificate(mockCert);

        final JWSBuilder mockJwsBuilder = mock(JWSBuilder.class);

        when(mockJwsBuilder.generateSignedJWT(nonce, submitUrl, privateKey, publicKey, mockCert))
                .thenReturn("signedJwtHere");

        final Object handler = getInstance(mockJwsBuilder);
        final Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeResponseFromHeader", // method name
                        String.class,
                        String.class);

        final String redirectURI =
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE
                        + " Nonce=\""
                        + nonce
                        + "\",CertThumbprint=\"ABC\",Version=\"1.0\",Context=\""
                        + context
                        + "\"";

        // act
        final Object response = m.invoke(handler, redirectURI, submitUrl);

        // assert
        final String authHeaderValue =
                (String) ReflectionUtils.getFieldValue(response, "mAuthorizationHeaderValue");

        assertTrue(
                authHeaderValue.contains(
                        String.format(
                                "%s AuthToken=\"%s\",Context=\"%s\"",
                                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE,
                                "signedJwtHere",
                                context)));
    }

    /**
     * Test for verifying cert authorities could be used to pick up right certificate.
     */
    @Test
    public void testGetChallengeResponseFromHeaderCertAuthorityPresent()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
                    NoSuchFieldException, NoSuchAlgorithmException, ClientException {
        final KeyPair keyPair = getKeyPair();
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        final String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        final String nonce = "123123-123213-123";
        final String context = "ABcdeded";
        final X509Certificate mockCert = mock(X509Certificate.class);

        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        MockDeviceCertProxy.setPrivateKey(privateKey);
        MockDeviceCertProxy.setPublicKey(publicKey);
        MockDeviceCertProxy.setCertificate(mockCert);

        final JWSBuilder mockJwsBuilder = mock(JWSBuilder.class);

        when(mockJwsBuilder.generateSignedJWT(nonce, submitUrl, privateKey, publicKey, mockCert))
                .thenReturn("signedJwtHere");

        final Object handler = getInstance(mockJwsBuilder);

        final Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeResponseFromHeader", // method name
                        String.class,
                        String.class);

        final String authorizationHeader =
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE
                        + " Nonce=\""
                        + nonce
                        + "\",CertAuthorities=\"ABC\",Version=\"1.0\",Context=\""
                        + context
                        + "\"";

        final Object response = m.invoke(handler, authorizationHeader, submitUrl);

        final String authHeaderValue =
                (String)
                        ReflectionUtils.getFieldValue(
                                response, "mAuthorizationHeaderValue" // field name
                                );

        assertTrue(
                authHeaderValue.contains(
                        String.format(
                                "%s AuthToken=\"%s\",Context=\"%s\"",
                                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE,
                                "signedJwtHere",
                                context)));
    }

    /**
     * Verify no error thrown out if device is not workplace joined even neither cert thumbprint
     * nor cert authority is returned from pkeyauth challenge.
     */
    @Test
    public void testGetChallengeFromHeaderNotWorkPlaceJoinedNoCertThumbprintNoCertAuthority()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
                    NoSuchFieldException {
        final String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        final String nonce = "123123-123213-123";
        final String context = "ABcdeded";

        final Object handler = getInstance(null);
        final Field f =
                com.microsoft.identity.common.java.AuthenticationSettings.INSTANCE
                        .getClass()
                        .getDeclaredField("mClazzDeviceCertProxy");
        f.setAccessible(true);
        f.set(com.microsoft.identity.common.java.AuthenticationSettings.INSTANCE, null);

        final Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeResponseFromHeader", // method name
                        String.class,
                        String.class);

        final String authorizationHeader =
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE
                        + " Nonce=\""
                        + nonce
                        + "\",Version=\"1.0\",Context=\""
                        + context
                        + "\"";

        Object response = null;

        try {
            response = m.invoke(handler, authorizationHeader, submitUrl);
        } catch (final Exception exception) {
            fail("No exception should be thrown ." + exception.getCause().getMessage());
        }

        assertNotNull(response);

        final String authHeaderValue =
                (String)
                        ReflectionUtils.getFieldValue(
                                response, "mAuthorizationHeaderValue" // field name
                                );

        assertTrue(
                authHeaderValue.contains(
                        String.format(
                                "%s Context=\"%s\"",
                                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, context)));
    }

    /**
     * Test for verifying correct error thrown out when challenge header doesn't contain both thumbprint and cert
     * authorities
     * if device is already workplace joined.
     */
    @Test
    public void testGetChallengeResponseFromHeaderBothThumbprintCertAuthorityNotPresent()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
                    NoSuchAlgorithmException {
        final KeyPair keyPair = getKeyPair();
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        X509Certificate mockCert = mock(X509Certificate.class);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        MockDeviceCertProxy.setPrivateKey(privateKey);
        MockDeviceCertProxy.setPublicKey(publicKey);
        MockDeviceCertProxy.setCertificate(mockCert);

        final String nonce = "123123-123213-123";
        final String context = "ABcdeded";

        Object handler = getInstance(null);
        Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeRequestFromHeader", // method name
                        String.class);

        final String authorizationHeader =
                CERT_AUTH_TYPE
                        + " Nonce=\""
                        + nonce
                        + "\",Version=\"1.0\",Context=\""
                        + context
                        + "\"";

        Object response = null;
        try {
            response = m.invoke(handler, authorizationHeader);
            Assert.fail("expected exception");
        } catch (Exception ex) {
            assertEquals(
                    "Error code check", // message
                    ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    ((AuthenticationException) ex.getCause()).getCode());

            assertEquals(
                    "Error mesage check", // message
                    "Both certThumbprint and certauthorities are not present", // expected
                    ex.getCause().getMessage());
        }
    }

    @Test
    public void testGetChallengeResponseFromHeaderNegative()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        Object handler = getInstance(null);

        Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeRequestFromHeader", // method name
                        String.class);

        String redirectURI = CERT_AUTH_TYPE + " Nonce = a =b, Pair = c =invalidFormat";

        // act
        try {
            m.invoke(handler, redirectURI);
            Assert.fail("expected exception");
        } catch (final Exception ex) {
            assertEquals(
                    "Error code check",
                    ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    ((AuthenticationException) ex.getCause()).getCode());
        }
    }

    @Test
    public void testGetChallengeResponseInvalidIssuer()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
                    NoSuchFieldException {
        final Object mockJwsBuilder = mock(JWSBuilder.class);
        final Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(false);

        final Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeResponseFromUri", // method name
                        String.class);

        final String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        final String nonce = "123123-123213-123";
        final String context = "ABcdeded";
        final String redirectURI =
                CERT_REDIRECT
                        + "?Nonce="
                        + nonce
                        + "&CertAuthorities=ABC&Version=1.0&SubmitUrl="
                        + submitUrl
                        + "&Context="
                        + context;

        Object response = m.invoke(handler, redirectURI);

        verifyChallengeResponse(response, null, context, submitUrl);
    }

    @Test
    public void testGetChallengeResponseNoDeviceCertProxy()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
                    NoSuchFieldException {
        final Object mockJwsBuilder = mock(JWSBuilder.class);
        final Object handler = getInstance(mockJwsBuilder);
        final Field f =
                com.microsoft.identity.common.java.AuthenticationSettings.INSTANCE
                        .getClass()
                        .getDeclaredField("mClazzDeviceCertProxy");
        f.setAccessible(true);
        f.set(com.microsoft.identity.common.java.AuthenticationSettings.INSTANCE, null);

        final Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeResponseFromUri", // method name
                        String.class);

        final Object response =
                m.invoke(
                        handler,
                        CERT_REDIRECT
                                + "?Nonce=2&CertAuthorities=ABC&Version=1.0&SubmitUrl=1&Context=1");

        // assert
        final String authHeaderValue =
                (String)
                        ReflectionUtils.getFieldValue(
                                response, "mAuthorizationHeaderValue" // field name
                                );

        assertTrue(
                authHeaderValue.contains(
                        String.format(
                                "%s Context=\"%s\",Version=\"1.0\"",
                                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, "1")));
    }

    // TODO: Fix Test
    @Ignore
    @Test
    public void testGetChallengeResponseInvalidRedirect()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        final Object mockJwsBuilder = mock(JWSBuilder.class);
        final Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(false);

        final Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeResponseFromUri", // method name
                        String.class);

        try {
            m.invoke(handler, "");
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue(
                    "Argument exception", // message
                    ex.getCause().getMessage().contains("redirectUri"));
        }

        try {
            m.invoke(
                    handler,
                    CERT_REDIRECT
                            + "?Noncemissing=2&CertAuthorities=ABC&Version=1.0&SubmitUrl=1&Context=1");

            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("Nonce"));
        }

        try {
            m.invoke(
                    handler,
                    CERT_REDIRECT + "?Nonce=2&CertAuthorities=&Version=1.0&SubmitUrl=1&Context=1");
        } catch (final Exception ex) {
            Assert.fail("No exception");
        }

        try {
            m.invoke(
                    handler,
                    CERT_REDIRECT
                            + "?Nonce=2&CertAuthorities=ABC&Version=1.0&SubmitUrlMissing=1&Context=1");
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue(
                    "Argument exception", // message
                    ex.getCause().getMessage().contains("SubmitUrl"));
        }

        try {
            m.invoke(
                    handler,
                    CERT_REDIRECT
                            + "?Nonce=2&CertAuthorities=ABC&Versionmiss=1.0&SubmitUrl=1&Context=1");
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue(
                    "Argument exception", // message
                    ex.getCause().getMessage().contains("Version"));
        }

        try {
            m.invoke(
                    handler,
                    CERT_REDIRECT
                            + "?Nonce=2&CertAuthorities=ABC&Version=1.0&SubmitUrl=1&Contextmiss=1");
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue(
                    "Argument exception", // message
                    ex.getCause().getMessage().contains("Context"));
        }
    }

    @Test
    public void testGetChallengeResponseValidIssuerNullKey()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        final Object mockJwsBuilder = mock(JWSBuilder.class);
        final Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);

        final Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeResponseFromUri", // method name
                        String.class);

        final String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        final String nonce = "123123-123213-123";
        final String context = "ABcdeded";
        final String redirectURI =
                CERT_REDIRECT
                        + "?Nonce="
                        + nonce
                        + "&CertAuthorities=ABC&Version=1.0&SubmitUrl="
                        + submitUrl
                        + "&Context="
                        + context;

        try {
            m.invoke(handler, redirectURI);
        } catch (final Exception e) {
            assertTrue(
                    "argument exception for key", // message
                    e.getCause().getMessage().contains("private key"));
        }
    }

    @Test
    public void testGetChallengeResponsePositive()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
                    NoSuchFieldException, NoSuchAlgorithmException, ClientException {
        final KeyPair keyPair = getKeyPair();
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        final String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        final String nonce = "123123-123213-123";
        final String context = "ABcdeded";
        final X509Certificate mockCert = mock(X509Certificate.class);

        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        MockDeviceCertProxy.setPrivateKey(privateKey);
        MockDeviceCertProxy.setPublicKey(publicKey);
        MockDeviceCertProxy.setCertificate(mockCert);

        final JWSBuilder mockJwsBuilder = mock(JWSBuilder.class);
        when(mockJwsBuilder.generateSignedJWT(nonce, submitUrl, privateKey, publicKey, mockCert))
                .thenReturn("signedJwtHere");

        final Object handler = getInstance(mockJwsBuilder);

        final Method m =
                ReflectionUtils.getTestMethod(
                        handler,
                        "getChallengeResponseFromUri", // method name
                        String.class);

        final String redirectURI =
                CERT_REDIRECT
                        + "?Nonce="
                        + nonce
                        + "&CertAuthorities=ABC&Version=1.0&SubmitUrl="
                        + submitUrl
                        + "&Context="
                        + context;

        final Object response = m.invoke(handler, redirectURI);

        verifyChallengeResponse(response, "signedJwtHere", context, submitUrl);
    }

    private KeyPair getKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        final int keySize = 1024;
        keyGen.initialize(keySize);
        final KeyPair keyPair = keyGen.genKeyPair();
        return keyPair;
    }

    private void verifyChallengeResponse(
            final Object response, final String auth, final String context, final String url)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final String submitUrl =
                (String)
                        ReflectionUtils.getFieldValue(
                                response, "mSubmitUrl" // submit url
                                );

        final String authHeaderValue =
                (String) ReflectionUtils.getFieldValue(response, "mAuthorizationHeaderValue");
        assertTrue("Contains url", submitUrl.contains(url));

        if (auth != null) {
            assertTrue(
                    authHeaderValue.contains(
                            String.format(
                                    "%s AuthToken=\"%s\",Context=\"%s\"",
                                    CERT_AUTH_TYPE, auth, context)));
        } else {
            assertTrue(
                    authHeaderValue.contains(
                            String.format("%s Context=\"%s\"", CERT_AUTH_TYPE, context)));
        }
    }

    /**
     * Gets instance of ChallengeResponseHandler and sets deviceCertificateProxy
     * class to load
     *
     * @param mockJwsBuilder Mock JWS builder with predefined behavior
     */
    private Object getInstance(final Object mockJwsBuilder)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        // mock JWSBuilder and pass here
        AuthenticationSettings.INSTANCE.setDeviceCertificateProxyClass(MockDeviceCertProxy.class);
        final Class clazz = Class.forName("com.microsoft.aad.adal.ChallengeResponseBuilder");
        final Constructor<?> constructorParams =
                clazz.getDeclaredConstructor(
                        Class.forName("com.microsoft.identity.common.java.util.JWSBuilder"));

        constructorParams.setAccessible(true);
        return constructorParams.newInstance(mockJwsBuilder);
    }
}
