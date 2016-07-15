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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import junit.framework.Assert;

public class ChallengeResponseBuilderTests extends AndroidTestHelper {

    static final String TAG = "ClientCertHandlerTests";

    private static final String CERT_REDIRECT = AuthenticationConstants.Broker.PKEYAUTH_REDIRECT;

    private static final String CERT_AUTH_TYPE = AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE;

    public void testGetChallengeResponseFromHeaderPositive() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException,
            NoSuchAlgorithmException, AuthenticationException {
        KeyPair keyPair = getKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        String thumbPrint = "thumbprint23432432";
        X509Certificate mockCert = mock(X509Certificate.class);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        MockDeviceCertProxy.setThumbPrint(thumbPrint);
        MockDeviceCertProxy.setPrivateKey(privateKey);
        MockDeviceCertProxy.setPublicKey(publicKey);
        MockDeviceCertProxy.setCertificate(mockCert);
        IJWSBuilder mockJwsBuilder = mock(IJWSBuilder.class);
        when(mockJwsBuilder.generateSignedJWT(nonce, submitUrl, privateKey, publicKey, mockCert))
                .thenReturn("signedJwtHere");
        Object handler = getInstance(mockJwsBuilder);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeResponseFromHeader",
                String.class, String.class);
        String redirectURI = AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE + " Nonce=\""
                + nonce + "\",CertThumbprint=\"ABC\",Version=\"1.0\",Context=\"" + context + "\"";

        // act
        Object response = m.invoke(handler, redirectURI, submitUrl);

        // assert
        String authHeaderValue = (String) ReflectionUtils.getFieldValue(response,
                "mAuthorizationHeaderValue");
        assertTrue(authHeaderValue.contains(String.format("%s AuthToken=\"%s\",Context=\"%s\"",
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, "signedJwtHere", context)));
    }

    /**
     * Test for verifying cert authorities could be used to pick up right certificate.
     */
    public void testGetChallengeResponseFromHeaderCertAuthorityPresent() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException,
            NoSuchAlgorithmException, AuthenticationException {
        KeyPair keyPair = getKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        X509Certificate mockCert = mock(X509Certificate.class);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        MockDeviceCertProxy.setPrivateKey(privateKey);
        MockDeviceCertProxy.setPublicKey(publicKey);
        MockDeviceCertProxy.setCertificate(mockCert);
        IJWSBuilder mockJwsBuilder = mock(IJWSBuilder.class);
        when(mockJwsBuilder.generateSignedJWT(nonce, submitUrl, privateKey, publicKey, mockCert))
                .thenReturn("signedJwtHere");
        Object handler = getInstance(mockJwsBuilder);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeResponseFromHeader",
                String.class, String.class);
        String authorizationHeader = AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE + " Nonce=\""
                + nonce + "\",CertAuthorities=\"ABC\",Version=\"1.0\",Context=\"" + context + "\"";

        Object response = m.invoke(handler, authorizationHeader, submitUrl);

        String authHeaderValue = (String) ReflectionUtils.getFieldValue(response,
                "mAuthorizationHeaderValue");
        assertTrue(authHeaderValue.contains(String.format("%s AuthToken=\"%s\",Context=\"%s\"",
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, "signedJwtHere", context)));
    }

    /**
     * Verify no error thrown out if device is not workplace joined even neither cert thumbprint nor cert authority is
     * returned from
     * pkeyauth challenge.
     */
    public void testGetChallengeFromHeaderNotWorkPlaceJoinedNoCertThumbprintNoCertAuthority() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException,
            NoSuchAlgorithmException {
        final String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        final String nonce = "123123-123213-123";
        final String context = "ABcdeded";

        Object handler = getInstance(null);
        Field f = AuthenticationSettings.INSTANCE.getClass().getDeclaredField(
                "mClazzDeviceCertProxy");
        f.setAccessible(true);
        f.set(AuthenticationSettings.INSTANCE, null);

        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeResponseFromHeader", String.class, String.class);

        String authorizationHeader = AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE + " Nonce=\""
                + nonce + "\",Version=\"1.0\",Context=\"" + context + "\"";

        Object response = null;
        try {
            response = m.invoke(handler, authorizationHeader, submitUrl);
        } catch (final Exception exception) {
            fail("No exception should be thrown ." + exception.getCause().getMessage());
        }

        assertNotNull(response);

        final String authHeaderValue = (String) ReflectionUtils.getFieldValue(response,
                "mAuthorizationHeaderValue");
        assertTrue(authHeaderValue.contains(String.format("%s Context=\"%s\"",
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, context)));

    }

    /**
     * Test for verifying correct error thrown out when challenge header doesn't contain both thumbprint and cert
     * authorities
     * if device is already workplace joined.
     */
    public void testGetChallengeResponseFromHeaderBothThumbprintCertAuthorityNotPresent() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException,
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
        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeRequestFromHeader", String.class);

        final String authorizationHeader = CERT_AUTH_TYPE + " Nonce=\""
                + nonce + "\",Version=\"1.0\",Context=\"" + context + "\"";

        Object response = null;
        try {
            response = m.invoke(handler, authorizationHeader);
            Assert.fail("expected exception");
        } catch (Exception ex) {
            assertEquals("Error code check", ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    ((AuthenticationException) ex.getCause()).getCode());
            assertEquals("Error mesage check", "Both certThumbprint and certauthorities are not present",
                    ((AuthenticationException) ex.getCause()).getMessage());
        }
    }

    public void testGetChallengeResponseFromHeaderNegative() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException,
            NoSuchAlgorithmException {
        Object handler = getInstance(null);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeRequestFromHeader",
                String.class);
        String redirectURI = CERT_AUTH_TYPE + " Nonce = a =b, Pair = c =invalidFormat";

        // act
        try {
            m.invoke(handler, redirectURI);
            Assert.fail("expected exception");
        } catch (Exception ex) {
            assertEquals("Error code check", ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    ((AuthenticationException) ex.getCause()).getCode());
        }
    }

    public void testGetChallengeResponseInvalidIssuer() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(false);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeResponseFromUri",
                String.class);
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        String redirectURI = CERT_REDIRECT + "?Nonce=" + nonce
                + "&CertAuthorities=ABC&Version=1.0&SubmitUrl=" + submitUrl + "&Context=" + context;

        Object response = m.invoke(handler, redirectURI);

        verifyChallengeResponse(response, null, context, submitUrl);
    }

    public void testGetChallengeResponseNoDeviceCertProxy() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        Field f = AuthenticationSettings.INSTANCE.getClass().getDeclaredField(
                "mClazzDeviceCertProxy");
        f.setAccessible(true);
        f.set(AuthenticationSettings.INSTANCE, null);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeResponseFromUri",
                String.class);

        Object response = m.invoke(handler, CERT_REDIRECT
                + "?Nonce=2&CertAuthorities=ABC&Version=1.0&SubmitUrl=1&Context=1");

        // assert
        String authHeaderValue = (String) ReflectionUtils.getFieldValue(response,
                "mAuthorizationHeaderValue");

        assertTrue(authHeaderValue.contains(String.format("%s Context=\"%s\",Version=\"1.0\"",
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, "1")));
    }

    public void testGetChallengeResponseInvalidRedirect() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(false);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeResponseFromUri",
                String.class);

        try {
            m.invoke(handler, "");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("redirectUri"));
        }

        try {
            m.invoke(handler, CERT_REDIRECT
                    + "?Noncemissing=2&CertAuthorities=ABC&Version=1.0&SubmitUrl=1&Context=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("Nonce"));
        }

        try {
            m.invoke(handler, CERT_REDIRECT
                    + "?Nonce=2&CertAuthoritiesMissing=ABC&Version=1.0&SubmitUrl=1&Context=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("CertAuthorities"));
        }

        try {
            m.invoke(handler, CERT_REDIRECT
                    + "?Nonce=2&CertAuthorities=ABC&Version=1.0&SubmitUrlMissing=1&Context=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("SubmitUrl"));
        }

        try {
            m.invoke(handler, CERT_REDIRECT
                    + "?Nonce=2&CertAuthorities=ABC&Versionmiss=1.0&SubmitUrl=1&Context=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("Version"));
        }

        try {
            m.invoke(handler, CERT_REDIRECT
                    + "?Nonce=2&CertAuthorities=ABC&Version=1.0&SubmitUrl=1&Contextmiss=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("Context"));
        }
    }

    public void testGetChallengeResponseValidIssuerNullKey() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeResponseFromUri",
                String.class);
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        String redirectURI = CERT_REDIRECT + "?Nonce=" + nonce
                + "&CertAuthorities=ABC&Version=1.0&SubmitUrl=" + submitUrl + "&Context=" + context;

        try {
            m.invoke(handler, redirectURI);
        } catch (Exception e) {
            assertTrue("argument exception for key",
                    e.getCause().getMessage().contains("private key"));
        }
    }

    public void testGetChallengeResponsePositive() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException,
            NoSuchAlgorithmException, AuthenticationException {
        KeyPair keyPair = getKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        X509Certificate mockCert = mock(X509Certificate.class);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        MockDeviceCertProxy.setPrivateKey(privateKey);
        MockDeviceCertProxy.setPublicKey(publicKey);
        MockDeviceCertProxy.setCertificate(mockCert);
        IJWSBuilder mockJwsBuilder = mock(IJWSBuilder.class);
        when(mockJwsBuilder.generateSignedJWT(nonce, submitUrl, privateKey, publicKey, mockCert))
                .thenReturn("signedJwtHere");
        Object handler = getInstance(mockJwsBuilder);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallengeResponseFromUri",
                String.class);
        String redirectURI = CERT_REDIRECT + "?Nonce=" + nonce
                + "&CertAuthorities=ABC&Version=1.0&SubmitUrl=" + submitUrl + "&Context=" + context;

        Object response = m.invoke(handler, redirectURI);

        verifyChallengeResponse(response, "signedJwtHere", context, submitUrl);
    }

    private KeyPair getKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        final int keySize = 1024;
        keyGen.initialize(keySize);
        KeyPair keyPair = keyGen.genKeyPair();
        return keyPair;
    }

    private void verifyChallengeResponse(Object response, String auth, String context, String url)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        String submitUrl = (String) ReflectionUtils.getFieldValue(response, "mSubmitUrl");
        String authHeaderValue = (String) ReflectionUtils.getFieldValue(response,
                "mAuthorizationHeaderValue");
        assertTrue("Contains url", submitUrl.contains(url));

        if (auth != null) {
            assertTrue(authHeaderValue.contains(String.format("%s AuthToken=\"%s\",Context=\"%s\"",
                    CERT_AUTH_TYPE, auth, context)));
        } else {
            assertTrue(authHeaderValue.contains(String.format("%s Context=\"%s\"", CERT_AUTH_TYPE, context)));
        }
    }

    /**
     * Gets instance of ChallengeResponseHandler and sets deviceCertificateProxy
     * class to load
     *
     * @param mockJwsBuilder Mock JWS builder with predefined behavior
     */
    private Object getInstance(Object mockJwsBuilder) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException {
        // mock JWSBuilder and pass here
        AuthenticationSettings.INSTANCE.setDeviceCertificateProxyClass(MockDeviceCertProxy.class);
        Class clazz = Class.forName("com.microsoft.aad.adal.ChallengeResponseBuilder");
        Constructor<?> constructorParams = clazz.getDeclaredConstructor(Class
                .forName("com.microsoft.aad.adal.IJWSBuilder"));
        constructorParams.setAccessible(true);
        return constructorParams.newInstance(mockJwsBuilder);
    }
}
