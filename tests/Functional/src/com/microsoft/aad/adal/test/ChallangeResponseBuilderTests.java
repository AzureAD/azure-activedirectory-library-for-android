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

import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import junit.framework.Assert;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.IJWSBuilder;

public class ChallangeResponseBuilderTests extends AndroidTestHelper {

    static final String TAG = "ClientCertHandlerTests";

    public void testGetChallangeResponseFromHeader_Positive() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException,
            NoSuchAlgorithmException {
        KeyPair keyPair = getKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        String thumbPrint = "thumbprint23432432";
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.sValidIssuer = true;
        MockDeviceCertProxy.sThumbPrint = thumbPrint;
        MockDeviceCertProxy.sPrivateKey = privateKey;
        MockDeviceCertProxy.sPublicKey = publicKey;
        IJWSBuilder mockJwsBuilder = mock(IJWSBuilder.class);
        when(mockJwsBuilder.generateSignedJWT(nonce, "", privateKey, publicKey, thumbPrint))
                .thenReturn("signedJwtHere");
        Object handler = getInstance(mockJwsBuilder);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeResponseFromHeader",
                String.class);
        String redirectURI = "CertAuth Nonce=\"" + nonce
                + "\",Issuer=\"ABC\",Version=\"1.0\",Context=\"" + context + "\"";

        // act
        Object response = m.invoke(handler, redirectURI);

        // assert
        String authHeaderValue = (String)ReflectionUtils.getFieldValue(response,
                "mAuthorizationHeaderValue");
        assertTrue(authHeaderValue.contains(String.format(
                "CertAuth AuthToken=\"%s\",Context=\"%s\"", "signedJwtHere", context)));
    }

    public void testGetChallangeResponseFromHeader_Negative() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException,
            NoSuchAlgorithmException {
        Object handler = getInstance(null);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeRequestFromHeader",
                String.class);
        String redirectURI = "CertAuth Nonce = a =b, Pair = c =invalidFormat";

        // act
        try {
            m.invoke(handler, redirectURI);
            Assert.fail("expected exception");
        } catch (Exception ex) {
            assertEquals("Error code check", ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    ((AuthenticationException)ex.getCause()).getCode());
        }
    }

    public void testGetChallangeResponse_InvalidIssuer() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.sValidIssuer = false;
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeResponseFromUri",
                String.class);
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        String redirectURI = "urn:http-auth:CertAuth?Nonce=" + nonce
                + "&CertAuthorities=ABC&Version=1.0&SubmitUrl=" + submitUrl + "&Context=" + context;

        Object response = m.invoke(handler, redirectURI);

        verifyChallangeResponse(response, null, context, submitUrl);
    }

    public void testGetChallangeResponse_NoDeviceCertProxy() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        Field f = AuthenticationSettings.INSTANCE.getClass().getDeclaredField(
                "mClazzDeviceCertProxy");
        f.setAccessible(true);
        f.set(AuthenticationSettings.INSTANCE, null);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeResponseFromUri",
                String.class);

        try {
            m.invoke(handler,
                    "urn:http-auth:CertAuth?Nonce=2&CertAuthorities=ABC&Version=1.0&SubmitUrl=1&Context=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertEquals("API exception", ADALError.DEVICE_CERTIFICATE_API_EXCEPTION,
                    ((AuthenticationException)ex.getCause()).getCode());
        }
    }

    public void testGetChallangeResponse_InvalidRedirect() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.sValidIssuer = false;
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeResponseFromUri",
                String.class);

        try {
            m.invoke(handler, "");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("redirectUri"));
        }

        try {
            m.invoke(handler,
                    "urn:http-auth:CertAuth?Noncemissing=2&CertAuthorities=ABC&Version=1.0&SubmitUrl=1&Context=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("Nonce"));
        }

        try {
            m.invoke(handler,
                    "urn:http-auth:CertAuth?Nonce=2&CertAuthoritiesMissing=ABC&Version=1.0&SubmitUrl=1&Context=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("CertAuthorities"));
        }

        try {
            m.invoke(handler,
                    "urn:http-auth:CertAuth?Nonce=2&CertAuthorities=ABC&Version=1.0&SubmitUrlMissing=1&Context=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("SubmitUrl"));
        }

        try {
            m.invoke(handler,
                    "urn:http-auth:CertAuth?Nonce=2&CertAuthorities=ABC&Versionmiss=1.0&SubmitUrl=1&Context=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("Version"));
        }

        try {
            m.invoke(handler,
                    "urn:http-auth:CertAuth?Nonce=2&CertAuthorities=ABC&Version=1.0&SubmitUrl=1&Contextmiss=1");
            Assert.fail("No exception");
        } catch (Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("Context"));
        }
    }

    public void testGetChallangeResponse_ValidIssuer_NullKey() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.sValidIssuer = true;
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeResponseFromUri",
                String.class);
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        String redirectURI = "urn:http-auth:CertAuth?Nonce=" + nonce
                + "&CertAuthorities=ABC&Version=1.0&SubmitUrl=" + submitUrl + "&Context=" + context;

        try {
            m.invoke(handler, redirectURI);
        } catch (Exception e) {
            assertTrue("argument exception for key",
                    e.getCause().getMessage().contains("private key"));
        }
    }

    public void testGetChallangeResponse_Positive() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException,
            NoSuchAlgorithmException {
        KeyPair keyPair = getKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        String thumbPrint = "thumbprint23432432";
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.sValidIssuer = true;
        MockDeviceCertProxy.sThumbPrint = thumbPrint;
        MockDeviceCertProxy.sPrivateKey = privateKey;
        MockDeviceCertProxy.sPublicKey = publicKey;
        IJWSBuilder mockJwsBuilder = mock(IJWSBuilder.class);
        when(mockJwsBuilder.generateSignedJWT(nonce, submitUrl, privateKey, publicKey, thumbPrint))
                .thenReturn("signedJwtHere");
        Object handler = getInstance(mockJwsBuilder);
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeResponseFromUri",
                String.class);
        String redirectURI = "urn:http-auth:CertAuth?Nonce=" + nonce
                + "&CertAuthorities=ABC&Version=1.0&SubmitUrl=" + submitUrl + "&Context=" + context;

        Object response = m.invoke(handler, redirectURI);

        verifyChallangeResponse(response, "signedJwtHere", context, submitUrl);
    }

    private KeyPair getKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();
        return keyPair;
    }

    private void verifyChallangeResponse(Object response, String auth, String context, String url)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        String submitUrl = (String)ReflectionUtils.getFieldValue(response, "mSubmitUrl");
        String authHeaderValue = (String)ReflectionUtils.getFieldValue(response,
                "mAuthorizationHeaderValue");
        assertTrue("Contains url", submitUrl.contains(url));

        if (auth != null) {
            assertTrue(authHeaderValue.contains(String.format(
                    "CertAuth AuthToken=\"%s\",Context=\"%s\"", auth, context)));
        } else {
            assertTrue(authHeaderValue.contains(String.format("CertAuth Context=\"%s\"", context)));
        }
    }

    /**
     * Gets instance of ChallangeResponseHandler and sets deviceCertificateProxy
     * class to load
     * 
     * @param mockJwsBuilder Mock JWS builder with predefined behavior
     * @return
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    private Object getInstance(Object mockJwsBuilder) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException {
        // mock JWSBuilder and pass here
        AuthenticationSettings.INSTANCE.setDeviceCertificateProxyClass(MockDeviceCertProxy.class);
        Class clazz = Class.forName("com.microsoft.aad.adal.ChallangeResponseBuilder");
        Constructor<?> constructorParams = clazz.getDeclaredConstructor(Class
                .forName("com.microsoft.aad.adal.IJWSBuilder"));
        constructorParams.setAccessible(true);
        return constructorParams.newInstance(mockJwsBuilder);
    }
}
