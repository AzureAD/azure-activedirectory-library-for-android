
package com.microsoft.aad.adal.test;

import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.IJWSBuilder;

public class ChallangeResponseBuilderTests extends AndroidTestHelper {

    static final String TAG = "ClientCertHandlerTests";

    public void testGetChallangeResponse_InvalidIssuer() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.sValidIssuer = false;
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeResponse", String.class);
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        String redirectURI = "urn:http-auth:CertAuth?Nonce=" + nonce
                + "&CertAuthorities=ABC&Version=1.0&SubmitUrl=" + submitUrl + "&Context=" + context;

        Object response = m.invoke(handler, redirectURI);

        verifyChallangeResponse(response, null, context, submitUrl);
    }

    public void testGetChallangeResponse_ValidIssuer_NullKey() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        Object mockJwsBuilder = mock(IJWSBuilder.class);
        Object handler = getInstance(mockJwsBuilder);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.sValidIssuer = true;
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeResponse", String.class);
        String submitUrl = "http://fs.contoso.com/adfs/services/trust";
        String nonce = "123123-123213-123";
        String context = "ABcdeded";
        String redirectURI = "urn:http-auth:CertAuth?Nonce=" + nonce
                + "&CertAuthorities=ABC&Version=1.0&SubmitUrl=" + submitUrl + "&Context=" + context;

        try {
            Object response = m.invoke(handler, redirectURI);
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
        Method m = ReflectionUtils.getTestMethod(handler, "getChallangeResponse", String.class);
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
