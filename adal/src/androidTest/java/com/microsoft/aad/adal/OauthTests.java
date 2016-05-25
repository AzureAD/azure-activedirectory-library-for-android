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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
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

import com.microsoft.aad.adal.AuthenticationConstants.AAD;
import com.microsoft.aad.adal.AuthenticationResult.AuthenticationStatus;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;

@SuppressLint("TrulyRandom")
public class OauthTests extends AndroidTestCase {

    private static final String TEST_RETURNED_EXCEPTION = "test-returned-exception";

    private static final String TEST_AUTHORITY = "https://login.windows.net/common";

    @SmallTest
    public void testParseIdTokenPositive() throws UnsupportedEncodingException, AuthenticationException {
        IdToken actual = new IdToken(Util.getIdToken());
        assertEquals("0DxnAlLi12IvGL", actual.getSubject());
        assertEquals("6fd1f5cd-a94c-4335-889b-6c598e6d8048", actual.getTenantId());
        assertEquals("test@test.onmicrosoft.com", actual.getUpn());
        assertEquals("givenName", actual.getGivenName());
        assertEquals("familyName", actual.getFamilyName());
        assertEquals("emailField", actual.getEmail());
        assertEquals("idpProvider", actual.getIdentityProvider());
        assertEquals("53c6acf2-2742-4538-918d-e78257ec8516", actual.getObjectId());
        assertTrue(1387227772 == actual.getPasswordExpiration());
        assertEquals("pwdUrl", actual.getPasswordChangeUrl());
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
    public void testEncodeDecodeProtocolState() throws UnsupportedEncodingException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException {
        String resource = "resource:" + UUID.randomUUID().toString();
        Object request = createAuthenticationRequest("http://www.something.com", resource,
                "client", "redirect", "loginhint@ggg.com", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method decodeMethod = ReflectionUtils.getTestMethod(oauth, "decodeProtocolState",
                String.class);
        Method encodeMethod = ReflectionUtils.getTestMethod(oauth, "encodeProtocolState");

        String encoded = (String)encodeMethod.invoke(oauth);
        String decoded = (String)decodeMethod.invoke(oauth, encoded);
        assertTrue("State contains authority", decoded.contains("http://www.something.com"));
        assertTrue("State contains resource", decoded.contains(resource));
    }

    @SmallTest
    public void testParseIdTokenNegativeIncorrectMessage() {
        String idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2.";
        try {
            final IdToken actual = new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }
    }

    @SmallTest
    public void testParseIdTokenNegativeInvalidEncodedTokens() {
        String idToken = "..";
        try {
            final IdToken actual = new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }

        idToken = "sdf.sdf.";
        try {
            final IdToken actual = new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }

        idToken = "sdf.sdf.34";
        try {
            final IdToken actual = new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }

        idToken = "dfdf";
        try {
            final IdToken actual = new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }

        idToken = ".....";
        try {
            final IdToken actual = new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }
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
        assertEquals(actual.getErrorCode(), "testerr");
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
                actual.contains("http://www.something.com/oauth2/authorize?response_type=code&client_id=client+1234567890-%2B%3D%3B%21%23%24+++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&resource=resource%2520urn%3A%21%23%24++++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&redirect_uri=redirect+1234567890&state="));
        assertTrue("Matching loginhint",
                actual.contains("login_hint=loginhint+1234567890-%2B%3D%3B%27"));

        // without login hint
        Object requestWithoutLogin = createAuthenticationRequest("http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890", "", null,
                null, null);

        Object oauthWithoutLoginHint = createOAuthInstance(requestWithoutLogin);

        actual = (String)m.invoke(oauthWithoutLoginHint);
        assertTrue(
                "Matching message",
                actual.contains("http://www.something.com/oauth2/authorize?response_type=code&client_id=client+1234567890-%2B%3D%3B%21%23%24+++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&resource=resource%2520urn%3A%21%23%24++++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&redirect_uri=redirect+1234567890&state="));
        assertFalse("Without loginhint", actual.contains("login_hint=loginhintForCode"));

        Object requestAlways = createAuthenticationRequest("http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890", "",
                PromptBehavior.Always, null, null);

        Object oauthAlways = createOAuthInstance(requestAlways);

        actual = (String)m.invoke(oauthAlways);
        assertTrue(
                "Matching message",
                actual.contains("http://www.something.com/oauth2/authorize?response_type=code&client_id=client+1234567890-%2B%3D%3B%21%23%24+++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&resource=resource%2520urn%3A%21%23%24++++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&redirect_uri=redirect+1234567890&state="));
        assertTrue("Prompt", actual.contains("&prompt=login"));

        Object requestExtraParam = createAuthenticationRequest("http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890", "",
                PromptBehavior.Always, "extra=1", null);

        Object oauthExtraParam = createOAuthInstance(requestExtraParam);

        actual = (String)m.invoke(oauthExtraParam);
        assertTrue("Prompt", actual.contains("&prompt=login&extra=1"));

        requestExtraParam = createAuthenticationRequest("http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890", "",
                PromptBehavior.Always, "&extra=1", null);

        oauthExtraParam = createOAuthInstance(requestExtraParam);

        actual = (String)m.invoke(oauthExtraParam);
        assertTrue("Prompt", actual.contains("&prompt=login&extra=1"));
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
                "grant_type=refresh_token&refresh_token=refreshToken23434%3D&client_id=client+1234567890-%2B%3D%3B%27&resource=resource%2520+",
                actual);

        // without resource
        Object requestWithoutResource = createAuthenticationRequest("http://www.something.com", "",
                "client 1234567890-+=;'", "redirect 1234567890-+=;'", "loginhint@ggg.com", null,
                null, null);

        Object oauthWithoutResource = createOAuthInstance(requestWithoutResource);

        actual = (String)m.invoke(oauthWithoutResource, "refreshToken234343455=");
        assertEquals(
                "Token request",
                "grant_type=refresh_token&refresh_token=refreshToken234343455%3D&client_id=client+1234567890-%2B%3D%3B%27",
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
        webrequest.setReturnResponse(new HttpWebResponse(200, json, null));

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                webrequest, "test");

        // Verify that callback can receive this error
        assertNull("callback doesnot have error", testResult.mException);
        assertNotNull("Result is not null", testResult.mResult);
        assertEquals("Same access token", "sometokenhere", testResult.mResult.getAccessToken());
        assertEquals("Same refresh token", "refreshfasdfsdf435",
                testResult.mResult.getRefreshToken());
    }

    @SuppressWarnings("unchecked")
    @SmallTest
    public void testRefreshTokenWebResponse_DeviceChallenge_Positive()
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchAlgorithmException, IOException, AuthenticationException {
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        
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
        String challengeHeaderValue = AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE
                + " Nonce=\"" + nonce + "\",  Version=\"1.0\", CertThumbprint=\"" + thumbPrint
                + "\",  Context=\"" + context + "\"";
        String tokenPositiveResponse = "{\"access_token\":\"accessTokenHere\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshWithDeviceChallenge\",\"scope\":\"*\"}";
        HashMap<String, List<String>> headers = getHeader(
                AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER, challengeHeaderValue);
        HttpWebResponse responeChallenge = new HttpWebResponse(401, null, headers);
        HttpWebResponse responseValid = new HttpWebResponse(200, tokenPositiveResponse, null);
        // first call returns 401 and second call returns token
        when(
                mockWebRequest.sendPost(eq(new URL(TEST_AUTHORITY + "/oauth2/token")),
                        any(headers.getClass()), any(byte[].class),
                        eq("application/x-www-form-urlencoded"))).thenReturn(responeChallenge)
                .thenReturn(responseValid);

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                mockWebRequest, mockJwsBuilder, "testRefreshToken");

        // Verify that callback can receive this error
        assertNull("callback doesnot have error", testResult.mException);
        assertNotNull("Result is not null", testResult.mResult);
        assertEquals("Same access token", "accessTokenHere", testResult.mResult.getAccessToken());
        assertEquals("Same refresh token", "refreshWithDeviceChallenge",
                testResult.mResult.getRefreshToken());
    }

    @SuppressWarnings("unchecked")
    @SmallTest
    public void testRefreshTokenWebResponse_DeviceChallenge_Header_Empty()
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchAlgorithmException, IOException {
        IWebRequestHandler mockWebRequest = mock(IWebRequestHandler.class);
        HashMap<String, List<String>> headers = getHeader(
                AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER, " ");
        HttpWebResponse responeChallenge = new HttpWebResponse(401, null, headers);
        when(
                mockWebRequest.sendPost(eq(new URL(TEST_AUTHORITY + "/oauth2/token")),
                        any(headers.getClass()), any(byte[].class),
                        eq("application/x-www-form-urlencoded"))).thenReturn(responeChallenge);

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                mockWebRequest, "testRefreshToken");

        // Verify that callback can receive this error
        assertNotNull("Callback has error", testResult.mException);
        assertEquals("Check error message", "Challenge header is empty",
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
        String idToken = Util.getIdToken();
        String json = "{\"id_token\":\""
                + idToken
                + "\",\"access_token\":\"sometokenhere2343=\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshfasdfsdf435=\",\"scope\":\"*\"}";
        HttpWebResponse mockResponse = new HttpWebResponse(200, json, null);

        // send call with mocks
        AuthenticationResult result = (AuthenticationResult)m.invoke(oauth, mockResponse);

        // verify same token
        assertEquals("Same token in parsed result", "sometokenhere2343=", result.getAccessToken());
        assertEquals("Same refresh token in parsed result", "refreshfasdfsdf435=",
                result.getRefreshToken());
        assertEquals("Same rawIdToken", idToken, result.getIdToken());
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
        HttpWebResponse mockResponse = new HttpWebResponse(200, json, headers);
        TestLogResponse logResponse = new TestLogResponse();
        logResponse.listenForLogMessage("CorrelationId is not matching", null);

        // send call with mocks
        AuthenticationResult result = (AuthenticationResult)m.invoke(oauth, mockResponse);

        // verify same token
        assertEquals("Same token in parsed result", "sometokenhere2343=", result.getAccessToken());
        assertTrue("Log response has message",
                logResponse.errorCode
                        .equals(ADALError.CORRELATION_ID_NOT_MATCHING_REQUEST_RESPONSE));

        List<String> invalidHeaders = new ArrayList<String>();
        invalidHeaders.add("invalid-UUID");
        headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, invalidHeaders);
        mockResponse = new HttpWebResponse(200, json, headers);
        TestLogResponse logResponse2 = new TestLogResponse();
        logResponse2.listenLogForMessageSegments("Wrong format of the correlation ID:");

        // send call with mocks
        m.invoke(oauth, mockResponse);
        // verify same token
        assertTrue("Log response has message",
                logResponse2.errorCode.equals(ADALError.CORRELATION_ID_FORMAT));
    }

    @SmallTest
    public void testprocessTokenResponseNegative() throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {

        Object request = createAuthenticationRequest("authority", "resource", "client", "redirect",
                "loginhint", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "processTokenResponse",
                Class.forName("com.microsoft.aad.adal.HttpWebResponse"));
        String json = "{invalid";
        HttpWebResponse mockResponse = new HttpWebResponse(200, json, null);

        // send call with mocks
        try {
            AuthenticationResult result = (AuthenticationResult)m.invoke(oauth, mockResponse);
            fail("must throw exception");
        } catch (InvocationTargetException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof AuthenticationException);
            AuthenticationException cause = (AuthenticationException)e.getCause();
            assertEquals(cause.getCode(), ADALError.SERVER_INVALID_JSON_RESPONSE);
        }
    }

    @SmallTest
    public void testprocessUIResponseParams() throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {
        HashMap<String, String> response = new HashMap<String, String>();
        Object request = createAuthenticationRequest("authority", "resource", "client", "redirect",
                "loginhint", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "processUIResponseParams", HashMap.class);

        // call for empty response
        AuthenticationResult result = (AuthenticationResult)m.invoke(null, response);
        assertNull("Result is null", result);

        // call when response has error
        response.put(AuthenticationConstants.OAuth2.ERROR, "error");
        response.put(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION, "error description");
        result = (AuthenticationResult)m.invoke(null, response);
        assertEquals("Failed status", AuthenticationStatus.Failed, result.getStatus());
        assertEquals("Error is same", "error", result.getErrorCode());
        assertEquals("Error description is same", "error description", result.getErrorDescription());

        // add token
        response.clear();
        response.put(AuthenticationConstants.OAuth2.ACCESS_TOKEN, "token");
        result = (AuthenticationResult)m.invoke(null, response);
        assertEquals("Success status", AuthenticationStatus.Succeeded, result.getStatus());
        assertEquals("Token is same", "token", result.getAccessToken());
        assertFalse("MultiResource token", result.getIsMultiResourceRefreshToken());

        // multi resource token
        response.put(AuthenticationConstants.AAD.RESOURCE, "resource");
        result = (AuthenticationResult)m.invoke(null, response);
        assertEquals("Success status", AuthenticationStatus.Succeeded, result.getStatus());
        assertEquals("Token is same", "token", result.getAccessToken());
        assertTrue("MultiResource token", result.getIsMultiResourceRefreshToken());
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

    public static Object createAuthenticationRequest(String authority, String resource,
            String client, String redirect, String loginhint, PromptBehavior prompt,
            String extraQueryParams, UUID correlationId) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");

        Constructor<?> constructor = c.getDeclaredConstructor(String.class, String.class,
                String.class, String.class, String.class, PromptBehavior.class, String.class,
                UUID.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authority, resource, client, redirect, loginhint,
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
