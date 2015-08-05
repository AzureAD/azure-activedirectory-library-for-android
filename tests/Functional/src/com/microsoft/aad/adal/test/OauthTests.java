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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationConstants.AAD;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationResult.AuthenticationStatus;
import com.microsoft.aad.adal.UserIdentifier.UserIdentifierType;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.HttpWebResponse;
import com.microsoft.aad.adal.IJWSBuilder;
import com.microsoft.aad.adal.IWebRequestHandler;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.UserIdentifier;

@SuppressLint("TrulyRandom")
public class OauthTests extends AndroidTestCase {

    private static final String TEST_RETURNED_EXCEPTION = "test-returned-exception";

    private static final String TEST_AUTHORITY = "https://login.windows.net/common";

    private final static String DEFAULT_AUTHORIZE_ENDPOINT = "/oauth2/v2.0/authorize";

    private final static String DEFAULT_TOKEN_ENDPOINT = "/oauth2/v2.0/token";

    @SmallTest
    public void testParseIdTokenPositive() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, UnsupportedEncodingException {
        ProfileInfo idtoken = new ProfileInfo();
        Object actual = parseIdToken(idtoken.getIdToken());
        assertEquals("0DxnAlLi12IvGL", ReflectionUtils.getFieldValue(actual, "mSubject"));
        assertEquals("6fd1f5cd-a94c-4335-889b-6c598e6d8048",
                ReflectionUtils.getFieldValue(actual, "mTenantId"));
    }

    @SmallTest
    public void testDecoding() throws UnsupportedEncodingException {
        // check that Base64 UrlSafe flags behaves as expected
        String expected = "Ma~0";
        assertEquals("BAse64 url safe encode", expected,
                new String(Base64.decode("TWF-MA", Base64.URL_SAFE), "UTF-8"));
        assertEquals("BAse64 url safe encode", expected,
                new String(Base64.decode("TWF-MA", Base64.URL_SAFE), "UTF-8"));
        assertEquals("BAse64 url safe encode", expected,
                new String(Base64.decode("TWF+MA", Base64.DEFAULT), "UTF-8"));
        assertEquals("BAse64 url safe encode", expected,
                new String(Base64.decode("TWF+MA==", Base64.DEFAULT), "UTF-8"));
    }

    @SmallTest
    public void testParseIdTokenNegativeIncorrectMessage() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        String idToken = "eyJ0eXAiOi";
        Object actual = parseIdToken(idToken);
        assertNull("IdToken is null", actual);
    }

    @SmallTest
    public void testParseIdTokenNegativeInvalidEncodedTokens() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        String idToken = "..";
        Object actual = parseIdToken(idToken);
        assertNull("IdToken is null", actual);

        idToken = "sdf.sdf.";
        actual = parseIdToken(idToken);
        assertNull("IdToken is null", actual);

        idToken = "sdf.sdf.34";
        actual = parseIdToken(idToken);
        assertNull("IdToken is null", actual);

        idToken = "dfdf";
        actual = parseIdToken(idToken);
        assertNull("IdToken is null", actual);

        idToken = ".....";
        actual = parseIdToken(idToken);
        assertNull("IdToken is null", actual);
    }
    
    private static UserIdentifier getUserId(String login){
        return new UserIdentifier(login, UserIdentifierType.OptionalDisplayableId);
    }

    @SmallTest
    private Object parseIdToken(String idToken) throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Object request = createAuthenticationRequest("http://www.something.com", "resource",
                "client", "redirect", "loginhint@ggg.com", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "parseProfileInfo", String.class);
        return (Object)m.invoke(oauth, idToken);
    }

    @SmallTest
    public void testGetToken_Null_Code() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        // with login hint
        Object request = createAuthenticationRequest("http://www.something.com",
                "https://officeapps.live.com", "clientID123456789", "redirect123",
                "loginhint", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "getToken", String.class);

        AuthenticationResult actual = (AuthenticationResult)m.invoke(oauth, "http://www.nocodeurl.com?state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L2NvbW1vbiZyPWh0dHBzOi8vb2ZmaWNlYXBwcy5saXZlLmNvbQ");
        assertNull(actual);
        
        actual = (AuthenticationResult)m.invoke(oauth, "http://www.nocodeurl.com?error=testerr&error_description=errtestdecription&state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L2NvbW1vbiZyPWh0dHBzOi8vb2ZmaWNlYXBwcy5saXZlLmNvbQ");
        assertTrue(actual.getStatus() == AuthenticationStatus.Failed);
    }
    
    @SmallTest
    public void testGetCodeRequestUrl() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        // with login hint
        Object request = createAuthenticationRequest("http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890",
                "loginhint 1234567890-+=;'", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "getCodeRequestUrl");

        String actual = (String)m.invoke(oauth);
        assertTrue(
                "Matching message",
                actual.contains("http://www.something.com"
                        + DEFAULT_AUTHORIZE_ENDPOINT
                        + "?response_type=code&client_id=client+1234567890-%2B%3D%3B%21%23%24+++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&scope=openid+offline_access+resource%2520urn%3A%21%23%24++++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&redirect_uri=redirect+1234567890&login_hint=loginhint+1234567890-%2B%3D%3B%27"));
        assertTrue("Matching loginhint",
                actual.contains("login_hint=loginhint+1234567890-%2B%3D%3B%27"));
    }

    @SmallTest
    public void testGetCodeRequestUrl_clientTrace() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, UnsupportedEncodingException {
        // with login hint
        Object request = createAuthenticationRequest("http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890",
                "loginhint 1234567890-+=;'", null, null, null);

        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "getCodeRequestUrl");

        String actual = (String)m.invoke(oauth);
        assertTrue("Matching message", actual.contains(AAD.ADAL_ID_PLATFORM + "=Android"));
        assertTrue("Matching message",
                actual.contains(AAD.ADAL_ID_VERSION + "=" + AuthenticationContext.getVersionName()));
    }

    @SmallTest
    public void testBuildTokenRequestMessage() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        // with login hint
        Object request = createAuthenticationRequest("http://www.something.com", "resource%20 ",
                "client 1234567890-+=;'", "redirect 1234567890-+=;'", "loginhint@ggg.com", null,
                null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "buildTokenRequestMessage", String.class);

        String actual = (String)m.invoke(oauth, "authorizationcodevalue=");
        assertEquals(
                "Token request",
                "grant_type=authorization_code&code=authorizationcodevalue%3D&client_id=client+1234567890-%2B%3D%3B%27&redirect_uri=redirect+1234567890-%2B%3D%3B%27",
                actual);

        // without login hint
        Object requestWithoutLogin = createAuthenticationRequest("http://www.something.com",
                "resource%20 ", "client 1234567890-+=;'", "redirect 1234567890-+=;'", "", null,
                null, null);

        Object oauthWithoutLoginHint = createOAuthInstance(requestWithoutLogin);

        actual = (String)m.invoke(oauthWithoutLoginHint, "authorizationcodevalue=");
        assertEquals(
                "Token request",
                "grant_type=authorization_code&code=authorizationcodevalue%3D&client_id=client+1234567890-%2B%3D%3B%27&redirect_uri=redirect+1234567890-%2B%3D%3B%27",
                actual);
    }

    /**
     * check message encoding issues
     * 
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @SmallTest
    public void testRefreshTokenRequestMessage() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Object request = createAuthenticationRequest("http://www.something.com", "resource%20 ",
                "client 1234567890-+=;'", "redirect 1234567890-+=;'", "loginhint@ggg.com", null,
                null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "buildRefreshTokenRequestMessage",
                String.class);

        String actual = (String)m.invoke(oauth, "refreshToken23434=");
        assertEquals(
                "Token request",
                "grant_type=refresh_token&refresh_token=refreshToken23434%3D&client_id=client+1234567890-%2B%3D%3B%27&scope=openid+offline_access+resource%2520+",
                actual);

        // without resource
        Object requestWithoutResource = createAuthenticationRequest("http://www.something.com", "",
                "client 1234567890-+=;'", "redirect 1234567890-+=;'", "loginhint@ggg.com", null,
                null, null);

        Object oauthWithoutResource = createOAuthInstance(requestWithoutResource);

        actual = (String)m.invoke(oauthWithoutResource, "refreshToken234343455=");
        assertEquals(
                "Token request",
                "grant_type=refresh_token&refresh_token=refreshToken234343455%3D&client_id=client+1234567890-%2B%3D%3B%27&scope=openid+offline_access",
                actual);
    }

    /**
     * web request handler is empty.
     * 
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @SmallTest
    public void testRefreshTokenEmptyWebRequest() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        final String refreshToken = "refreshToken234343455=";

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(), null,
                refreshToken);

        // Verify that we have error for request handler
        assertTrue("web request argument error", testResult.mException.getCause().getMessage()
                .contains("webRequestHandler"));
        assertNull("Result is null", testResult.mResult);
    }

    @SmallTest
    public void testRefreshTokenMalformedUrl() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Object request = createAuthenticationRequest("malformedurl", "resource%20 ",
                "client 1234567890-+=;'", "redirect 1234567890-+=;'", "loginhint@ggg.com", null,
                null, null);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();

        // send request
        MockAuthenticationCallback testResult = refreshToken(request, webrequest, "test");

        // Verify that we have error for request handler
        assertTrue("web request argument error", testResult.mException.getCause().getMessage()
                .contains("url"));
        assertNull("Result is null", testResult.mResult);
    }

    @SmallTest
    public void testRefreshTokenWebResponseHasException() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        webrequest.setReturnException("request should return error");

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                webrequest, "test");

        // Verify that callback can receive this error
        assertTrue("callback receives error", testResult.mException.getCause().getMessage()
                .contains("request should return error"));
        assertNull("Result is null", testResult.mResult);
    }

    @SmallTest
    public void testRefreshTokenWebResponseInvalidStatus() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        webrequest.setReturnResponse(new HttpWebResponse(503, null, null));
        // Invalid status that cause some exception at webrequest
        webrequest.setReturnException(TEST_RETURNED_EXCEPTION);

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                webrequest, "test");

        // Verify that callback can receive this error
        assertNull("AuthenticationResult is null", testResult.mResult);
        assertNotNull("Exception is not null", testResult.mException);
        assertEquals("Exception has same error message", TEST_RETURNED_EXCEPTION, testResult.mException.getCause().getMessage());
    }

    @SmallTest
    public void testRefreshTokenWebResponsePositive() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String json = "{\"access_token\":\"sometokenhere\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshfasdfsdf435\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                webrequest, "test");

        // Verify that callback can receive this error
        assertNull("callback doesnot have error", testResult.mException);
        assertNotNull("Result is not null", testResult.mResult);
        assertEquals("Same access token", "sometokenhere", testResult.mResult.getToken());
    }

    @SuppressWarnings("unchecked")
    @SmallTest
    public void testRefreshTokenWebResponse_DeviceChallenge_Positive()
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchAlgorithmException, MalformedURLException {
        IWebRequestHandler mockWebRequest = mock(IWebRequestHandler.class);
        KeyPair keyPair = getKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();
        String nonce = UUID.randomUUID().toString();
        String context = "CookieConABcdeded";
        X509Certificate mockCert = mock(X509Certificate.class);
        String thumbPrint = "thumbPrinttest";
        AuthenticationSettings.INSTANCE.setDeviceCertificateProxyClass(MockDeviceCertProxy.class);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.sValidIssuer = true;
        MockDeviceCertProxy.sThumbPrint = thumbPrint;
        MockDeviceCertProxy.sPrivateKey = privateKey;
        MockDeviceCertProxy.sPublicKey = publicKey;
        IJWSBuilder mockJwsBuilder = mock(IJWSBuilder.class);
        when(
                mockJwsBuilder.generateSignedJWT(eq(nonce), any(String.class), eq(privateKey),
                        eq(publicKey), eq(mockCert))).thenReturn("signedJwtHere");
        String challangeHeaderValue = AuthenticationConstants.Broker.CHALLANGE_RESPONSE_TYPE
                + " Nonce=\"" + nonce + "\",  Version=\"1.0\", CertThumbprint=\"" + thumbPrint
                + "\",  Context=\"" + context + "\"";
        String tokenPositiveResponse = "{\"access_token\":\"accessTokenHere\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshWithDeviceChallange\",\"scope\":\"*\"}";
        HashMap<String, List<String>> headers = getHeader(
                AuthenticationConstants.Broker.CHALLANGE_REQUEST_HEADER, challangeHeaderValue);
        HttpWebResponse responeChallange = new HttpWebResponse(401, null, headers);
        HttpWebResponse responseValid = new HttpWebResponse(200,
                tokenPositiveResponse.getBytes(Charset.defaultCharset()), null);
        // first call returns 401 and second call returns token
        when(
                mockWebRequest.sendPost(eq(new URL(TEST_AUTHORITY + DEFAULT_TOKEN_ENDPOINT)),
                        any(headers.getClass()), any(byte[].class),
                        eq("application/x-www-form-urlencoded"))).thenReturn(responeChallange)
                .thenReturn(responseValid);

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                mockWebRequest, mockJwsBuilder, "testRefreshToken");

        // Verify that callback can receive this error
        assertNull("callback doesnot have error", testResult.mException);
        assertNotNull("Result is not null", testResult.mResult);
        assertEquals("Same access token", "accessTokenHere", testResult.mResult.getToken());
    }

    @SuppressWarnings("unchecked")
    @SmallTest
    public void testRefreshTokenWebResponse_DeviceChallenge_Header_Empty()
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchAlgorithmException, MalformedURLException {
        IWebRequestHandler mockWebRequest = mock(IWebRequestHandler.class);
        HashMap<String, List<String>> headers = getHeader(
                AuthenticationConstants.Broker.CHALLANGE_REQUEST_HEADER, " ");
        HttpWebResponse responeChallange = new HttpWebResponse(401, null, headers);
        when(
                mockWebRequest.sendPost(eq(new URL(TEST_AUTHORITY + DEFAULT_TOKEN_ENDPOINT)),
                        any(headers.getClass()), any(byte[].class),
                        eq("application/x-www-form-urlencoded"))).thenReturn(responeChallange);

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                mockWebRequest, "testRefreshToken");

        // Verify that callback can receive this error
        assertNotNull("Callback has error", testResult.mException);
        assertEquals("Check error message", "Challange header is empty",
                ((AuthenticationException)testResult.mException.getCause()).getMessage());
    }

    @SmallTest
    public void testprocessTokenResponse() throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, UnsupportedEncodingException {

        Object request = createAuthenticationRequest("authority", "resource", "client", "redirect",
                "loginhint", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "processTokenResponse",
                Class.forName("com.microsoft.aad.adal.HttpWebResponse"));
        ProfileInfo defaultIdToken = new ProfileInfo();
        String idToken = defaultIdToken.getIdToken();
        String json = "{\"profile_info\":\""
                + idToken
                + "\",\"access_token\":\"sometokenhere2343=\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshfasdfsdf435=\",\"scope\":\"*\"}";
        HttpWebResponse mockResponse = new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null);

        // send call with mocks
        AuthenticationResult result = (AuthenticationResult)m.invoke(oauth, mockResponse);

        // verify same token
        assertEquals("Same token in parsed result", "sometokenhere2343=", result.getToken());
        assertEquals("Same rawIdToken", idToken, result.getProfileInfo());
    }

    @SmallTest
    public void testprocessTokenResponse_Wrong_CorrelationId() throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {
        Object request = createAuthenticationRequest("authority", "resource", "client", "redirect",
                "loginhint", null, null, UUID.randomUUID());
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "processTokenResponse",
                Class.forName("com.microsoft.aad.adal.HttpWebResponse"));
        String json = "{\"access_token\":\"sometokenhere2343=\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshfasdfsdf435=\",\"scope\":\"*\"}";
        List<String> listOfHeaders = new ArrayList<String>();
        listOfHeaders.add(UUID.randomUUID().toString());
        HashMap<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, listOfHeaders);
        HttpWebResponse mockResponse = new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), headers);
        TestLogResponse logResponse = new TestLogResponse();
        logResponse.listenForLogMessage("CorrelationId is not matching", null);

        // send call with mocks
        AuthenticationResult result = (AuthenticationResult)m.invoke(oauth, mockResponse);

        // verify same token
        assertEquals("Same token in parsed result", "sometokenhere2343=", result.getToken());
        assertTrue("Log response has message",
                logResponse.errorCode
                        .equals(ADALError.CORRELATION_ID_NOT_MATCHING_REQUEST_RESPONSE));

        List<String> invalidHeaders = new ArrayList<String>();
        invalidHeaders.add("invalid-UUID");
        headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, invalidHeaders);
        mockResponse = new HttpWebResponse(200, json.getBytes(Charset.defaultCharset()), headers);
        TestLogResponse logResponse2 = new TestLogResponse();
        logResponse2.listenLogForMessageSegments(null, "Wrong format of the correlation ID:");

        // send call with mocks
        m.invoke(oauth, mockResponse);

        // verify same token
        assertTrue("Log response has message",
                logResponse2.errorCode.equals(ADALError.CORRELATION_ID_FORMAT));
    }

    private Object getValidAuthenticationRequest() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        return createAuthenticationRequest(TEST_AUTHORITY, "resource%20 ",
                "client 1234567890-+=;'", "redirect 1234567890-+=;'", "loginhint@ggg.com", null,
                null, null);
    }

    private MockAuthenticationCallback refreshToken(Object request, Object webrequest,
            final String refreshToken) throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        final CountDownLatch signal = new CountDownLatch(1);
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final Object oauth = createOAuthInstance(request, webrequest);
        final Method m = ReflectionUtils.getTestMethod(oauth, "refreshToken", String.class);
        try {
            callback.mResult = (AuthenticationResult)m.invoke(oauth, refreshToken);
        } catch (Exception e) {
            callback.mException = e;
        }

        // callback has set the result from the call
        return callback;
    }

    private MockAuthenticationCallback refreshToken(Object request, Object webrequest,
            IJWSBuilder jwsBuilder, final String refreshToken) throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        final CountDownLatch signal = new CountDownLatch(1);
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final Object oauth = createOAuthInstance(request, webrequest, jwsBuilder);
        final Method m = ReflectionUtils.getTestMethod(oauth, "refreshToken", String.class);
        try {
            callback.mResult = (AuthenticationResult)m.invoke(oauth, refreshToken);
        } catch (Exception e) {
            callback.mException = e;
        }

        // callback has set the result from the call
        return callback;
    }

    public static Object createAuthenticationRequest(String authority, String scope,
            String client, String redirect, String username, PromptBehavior prompt,
            String extraQueryParams, UUID correlationId) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");

        Constructor<?> constructor = c.getDeclaredConstructor(String.class, String[].class,
                String.class, String.class, UserIdentifier.class, PromptBehavior.class, String.class,
                UUID.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authority, new String[]{scope}, client, redirect, getUserId(username),
                prompt, extraQueryParams, correlationId);
        return o;
    }

    public static Object createOAuthInstance(Object authenticationRequest)
            throws ClassNotFoundException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.aad.adal.Oauth2");

        Constructor<?> constructor = c.getDeclaredConstructor(authenticationRequest.getClass());
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authenticationRequest);
        return o;
    }

    private static Object createOAuthInstance(Object authenticationRequest, Object mockWebRequest)
            throws ClassNotFoundException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        if (mockWebRequest == null) {
            return createOAuthInstance(authenticationRequest);
        }

        Class<?> c = Class.forName("com.microsoft.aad.adal.Oauth2");

        Constructor<?> constructor = c.getDeclaredConstructor(authenticationRequest.getClass(),
                IWebRequestHandler.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authenticationRequest, mockWebRequest);
        return o;
    }

    private static Object createOAuthInstance(Object authenticationRequest, Object mockWebRequest,
            IJWSBuilder jwsBuilder) throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        if (mockWebRequest == null) {
            return createOAuthInstance(authenticationRequest);
        }

        Class<?> c = Class.forName("com.microsoft.aad.adal.Oauth2");

        Constructor<?> constructor = c.getDeclaredConstructor(authenticationRequest.getClass(),
                IWebRequestHandler.class, IJWSBuilder.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authenticationRequest, mockWebRequest, jwsBuilder);
        return o;
    }

    private HashMap<String, List<String>> getHeader(String key, String value) {
        HashMap<String, List<String>> dummy = new HashMap<String, List<String>>();
        dummy.put(key, Arrays.asList(value));
        return dummy;
    }

    private KeyPair getKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();
        return keyPair;
    }

}
