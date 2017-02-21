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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;

import com.microsoft.aad.adal.AuthenticationConstants.AAD;
import com.microsoft.aad.adal.AuthenticationResult.AuthenticationStatus;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OauthTests extends AndroidTestCase {

    private static final String TEST_RETURNED_EXCEPTION = "test-returned-exception";

    private static final String TEST_AUTHORITY = "https://login.windows.net/common";
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
    }

    @Override
    protected void tearDown() throws Exception {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        super.tearDown();
    }

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
        assertTrue(actual.getPasswordExpiration() == Util.TEST_PASSWORD_EXPIRATION);
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
    public void testEncodeDecodeProtocolState() throws UnsupportedEncodingException {
        final String resource = "resource:" + UUID.randomUUID().toString();
        final String authority = "http://www.something.com";
        final AuthenticationRequest request = createAuthenticationRequest(
                authority, resource,
                "client", "redirect", "loginhint@ggg.com", null, null, null, false);
        final Oauth2 oauth = createOAuthInstance(request);

        final String encoded = oauth.encodeProtocolState();
        final String decoded = Oauth2.decodeProtocolState(encoded);
        assertNotNull(decoded);
        assertTrue("State contains authority", decoded.contains(authority));
        assertTrue("State contains resource", decoded.contains(resource));
    }

    @SmallTest
    public void testParseIdTokenNegativeIncorrectMessage() {
        String idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2.";
        try {
            new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }
    }

    @SmallTest
    public void testParseIdTokenNegativeInvalidEncodedTokens() {
        String idToken = "..";
        try {
            new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }

        idToken = "sdf.sdf.";
        try {
            new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }

        idToken = "sdf.sdf.34";
        try {
            new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }

        idToken = "dfdf";
        try {
            new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }

        idToken = ".....";
        try {
            new IdToken(idToken);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof AuthenticationException);
        }
    }

    @SmallTest
    public void testGetTokenNullCode() throws IOException {
        // with login hint
        final AuthenticationRequest request = createAuthenticationRequest(TEST_AUTHORITY,
                "https://officeapps.live.com", "clientID123456789", "redirect123",
                "loginhint", null, null, null, false);
        final Oauth2 oauth2 = createOAuthInstance(request);
        final String uriWithNoCode = "http://www.nocodeurl.com?state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L2NvbW1vbiZyPWh0dHBzOi8vb2ZmaWNlYXBwcy5saXZlLmNvbQ";
        try {
            final AuthenticationResult result = oauth2.getToken(uriWithNoCode);
            assertNull(result);
        } catch (final AuthenticationException e) {
            fail();
        }

        final String authorizationUriWithError = "http://www.nocodeurl.com?error=testerr&error_description=errtestdecription&state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L2NvbW1vbiZyPWh0dHBzOi8vb2ZmaWNlYXBwcy5saXZlLmNvbQ";
        try {
            final AuthenticationResult result = oauth2.getToken(authorizationUriWithError);
            assertTrue(result.getStatus() == AuthenticationStatus.Failed);
            assertEquals(result.getErrorCode(), "testerr");
        } catch (AuthenticationException e) {
            fail();
        }
    }

    @SmallTest
    public void testGetCodeRequestUrl() throws UnsupportedEncodingException {
        // with login hint
        final AuthenticationRequest request = createAuthenticationRequest(
                "http://www.something.com", "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890",
                "loginhint 1234567890-+=;'", null, null, null, false);
        final Oauth2 oauth2 = createOAuthInstance(request);
        final String actualCodeRequestUrl = oauth2.getCodeRequestUrl();
        assertTrue("Matching message",
                actualCodeRequestUrl.contains("http://www.something.com/oauth2/authorize?response_type=code&client_id=client+1234567890-%2B%3D%3B%21%23%24+++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&resource=resource%2520urn%3A%21%23%24++++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&redirect_uri=redirect+1234567890&state="));
        assertTrue("Matching loginhint",
                actualCodeRequestUrl.contains("login_hint=loginhint+1234567890-%2B%3D%3B%27"));

        // without login hint
        final AuthenticationRequest requestWithoutLogin = createAuthenticationRequest(
                "http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890", "", null,
                null, null, false);

        final Oauth2 oauthWithoutLoginHint = createOAuthInstance(requestWithoutLogin);

        final String actualCodeRequestNoLoginhint = oauthWithoutLoginHint.getCodeRequestUrl();
        assertTrue("Matching message", actualCodeRequestNoLoginhint.contains(
                "http://www.something.com/oauth2/authorize?response_type=code&client_id=client+1234567890-%2B%3D%3B%21%23%24+++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&resource=resource%2520urn%3A%21%23%24++++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&redirect_uri=redirect+1234567890&state="));
        assertFalse("Without loginhint", actualCodeRequestNoLoginhint.contains(
                "login_hint=loginhintForCode"));

        final AuthenticationRequest requestAlways = createAuthenticationRequest(
                "http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890", "",
                PromptBehavior.Always, null, null, false);

        final Oauth2 oauthAlways = createOAuthInstance(requestAlways);
        final String actualCodeRequestWithPrompt = oauthAlways.getCodeRequestUrl();

        assertTrue(
                "Matching message",
                actualCodeRequestWithPrompt.contains(
                        "http://www.something.com/oauth2/authorize?response_type=code&client_id=client+1234567890-%2B%3D%3B%21%23%24+++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&resource=resource%2520urn%3A%21%23%24++++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&redirect_uri=redirect+1234567890&state="));
        assertTrue("Prompt", actualCodeRequestWithPrompt.contains("&prompt=login"));

        final AuthenticationRequest requestExtraParam = createAuthenticationRequest(
                "http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890", "",
                PromptBehavior.Always, "extra=1", null, false);
        final Oauth2 oauth2WithExtraPara = createOAuthInstance(requestExtraParam);
        final String actualCodeRequestWithExtraParam = oauth2WithExtraPara.getCodeRequestUrl();
        assertTrue("Prompt", actualCodeRequestWithExtraParam.contains("&prompt=login&haschrome=1&extra=1"));

        final AuthenticationRequest extraQPContainHasChrome = createAuthenticationRequest(
                "http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890", "",
                PromptBehavior.Always, "extra=1&haschrome=1", null, false);
        final Oauth2 oAuthxtraQPContainHasChrome = createOAuthInstance(extraQPContainHasChrome);
        final String actualCodeRequestQPHasChrome = oAuthxtraQPContainHasChrome.getCodeRequestUrl();
        assertTrue("Prompt", actualCodeRequestQPHasChrome.contains("&prompt=login&extra=1&haschrome=1"));
    }

    @SmallTest
    public void testGetCodeRequestUrlClientTrace() throws UnsupportedEncodingException {
        // with login hint
        final AuthenticationRequest request = createAuthenticationRequest("http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890",
                "loginhint 1234567890-+=;'", null, null, null, false);

        final Oauth2 oauth2 = createOAuthInstance(request);

        final String actualCodeRequestUrl = oauth2.getCodeRequestUrl();
        assertTrue("Matching message", actualCodeRequestUrl.contains(AAD.ADAL_ID_PLATFORM + "=Android"));
        assertTrue("Matching message",
                actualCodeRequestUrl.contains(AAD.ADAL_ID_VERSION + "=" + AuthenticationContext.getVersionName()));
    }

    @SmallTest
    public void testBuildTokenRequestMessage() throws UnsupportedEncodingException {
        // with login hint
        final AuthenticationRequest request = createAuthenticationRequest(
                "http://www.something.com", "resource%20 ", "client 1234567890-+=;'",
                "redirect 1234567890-+=;'", "loginhint@ggg.com", null, null, null, false);
        final Oauth2 oauth2 = createOAuthInstance(request);
        assertEquals(
                "Token request",
                "grant_type=authorization_code&code=authorizationcodevalue%3D&client_id=client+1234567890-%2B%3D%3B%27&redirect_uri=redirect+1234567890-%2B%3D%3B%27",
                oauth2.buildTokenRequestMessage("authorizationcodevalue="));

        // without login hint
        final AuthenticationRequest requestWithoutLogin = createAuthenticationRequest("http://www.something.com",
                "resource%20 ", "client 1234567890-+=;'", "redirect 1234567890-+=;'", "", null,
                null, null, false);

        final Oauth2 oauthWithoutLoginHint = createOAuthInstance(requestWithoutLogin);
        assertEquals(
                "Token request",
                "grant_type=authorization_code&code=authorizationcodevalue%3D&client_id=client+1234567890-%2B%3D%3B%27&redirect_uri=redirect+1234567890-%2B%3D%3B%27",
                oauthWithoutLoginHint.buildTokenRequestMessage("authorizationcodevalue="));
    }

    /**
     * check message encoding issues
     */
    @SmallTest
    public void testRefreshTokenRequestMessage() throws UnsupportedEncodingException {

        final AuthenticationRequest request = createAuthenticationRequest(
                "http://www.something.com", "resource%20 ",
                "client 1234567890-+=;'", "redirect 1234567890-+=;'", "loginhint@ggg.com", null,
                null, null, false);
        final Oauth2 oauth2 = createOAuthInstance(request);
        assertEquals(
                "Token request",
                "grant_type=refresh_token&refresh_token=refreshToken23434%3D&client_id=client+1234567890-%2B%3D%3B%27&resource=resource%2520+",
                oauth2.buildRefreshTokenRequestMessage("refreshToken23434="));

        // without resource
        final AuthenticationRequest requestWithoutResource = createAuthenticationRequest(
                "http://www.something.com", "", "client 1234567890-+=;'",
                "redirect 1234567890-+=;'", "loginhint@ggg.com", null, null, null, false);

        final Oauth2 oauthWithoutResource = createOAuthInstance(requestWithoutResource);
        assertEquals(
                "Token request",
                "grant_type=refresh_token&refresh_token=refreshToken234343455%3D&client_id=client+1234567890-%2B%3D%3B%27",
                oauthWithoutResource.buildRefreshTokenRequestMessage("refreshToken234343455="));
    }

    /**
     * web request handler is empty.
     */
    @SmallTest
    public void testRefreshTokenEmptyWebRequest() {
        final String refreshToken = "refreshToken234343455=";

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(), null,
                refreshToken);

        // Verify that we have error for request handler
        assertTrue("web request argument error", testResult.getException().getMessage()
                .contains("webRequestHandler"));
        assertNull("Result is null", testResult.getAuthenticationResult());
    }

    @SmallTest
    public void testRefreshTokenMalformedUrl() {
        final AuthenticationRequest request = createAuthenticationRequest(
                "malformedurl", "resource%20 ", "client 1234567890-+=;'",
                "redirect 1234567890-+=;'", "loginhint@ggg.com", null, null, null, false);
        final MockWebRequestHandler webrequest = new MockWebRequestHandler();

        // send request
        MockAuthenticationCallback testResult = refreshToken(request, webrequest, "test");

        // Verify that we have error for request handler
        assertTrue("web request argument error", testResult.getException().getMessage()
                .contains("url"));
        assertNull("Result is null", testResult.getAuthenticationResult());
    }

    @SmallTest
    public void testRefreshTokenWebResponseHasException() {
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        webrequest.setReturnException("request should return error");

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                webrequest, "test");

        // Verify that callback can receive this error
        assertTrue("callback receives error", testResult.getException().getMessage()
                .contains("request should return error"));
        assertNull("Result is null", testResult.getAuthenticationResult());
    }

    @SmallTest
    public void testRefreshTokenWebResponseInvalidStatus() {
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        webrequest.setReturnResponse(new HttpWebResponse(HttpURLConnection.HTTP_UNAVAILABLE,
                null, null));
        // Invalid status that cause some exception at webrequest
        webrequest.setReturnException(TEST_RETURNED_EXCEPTION);

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                webrequest, "test");

        // Verify that callback can receive this error
        assertNull("AuthenticationResult is null", testResult.getAuthenticationResult());
        assertNotNull("Exception is not null", testResult.getException());
        assertEquals("Exception has same error message", TEST_RETURNED_EXCEPTION,
                testResult.getException().getMessage());
    }

    @SmallTest
    public void testRefreshTokenWebResponsePositive() {
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String json = "{\"access_token\":\"sometokenhere\",\"token_type\":\"Bearer\","
                + "\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":"
                + "\"refreshfasdfsdf435\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(HttpURLConnection.HTTP_OK, json, null));

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                webrequest, "test");

        // Verify that callback can receive this error
        assertNull("callback does not have error", testResult.getException());
        assertNotNull("Result is not null", testResult.getAuthenticationResult());
        assertEquals("Same access token", "sometokenhere",
                testResult.getAuthenticationResult().getAccessToken());
        assertEquals("Same refresh token", "refreshfasdfsdf435",
                testResult.getAuthenticationResult().getRefreshToken());
    }

    @SuppressWarnings("unchecked")
    @SmallTest
    public void testRefreshTokenWebResponseDeviceChallengePositive()
            throws IOException, AuthenticationException, NoSuchAlgorithmException {
        final IWebRequestHandler mockWebRequest = mock(IWebRequestHandler.class);
        final KeyPair keyPair = getKeyPair();
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        final String nonce = UUID.randomUUID().toString();
        final String context = "CookieConABcdeded";
        final X509Certificate mockCert = mock(X509Certificate.class);
        final String thumbPrint = "thumbPrinttest";
        AuthenticationSettings.INSTANCE.setDeviceCertificateProxyClass(MockDeviceCertProxy.class);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        MockDeviceCertProxy.setThumbPrint(thumbPrint);
        MockDeviceCertProxy.setPrivateKey(privateKey);
        MockDeviceCertProxy.setPublicKey(publicKey);
        final IJWSBuilder mockJwsBuilder = mock(IJWSBuilder.class);
        when(
                mockJwsBuilder.generateSignedJWT(eq(nonce), any(String.class), eq(privateKey),
                        eq(publicKey), eq(mockCert))).thenReturn("signedJwtHere");
        final String challengeHeaderValue = AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE
                + " Nonce=\"" + nonce + "\",  Version=\"1.0\", CertThumbprint=\"" + thumbPrint
                + "\",  Context=\"" + context + "\"";
        final String tokenPositiveResponse = "{\"access_token\":\"accessTokenHere\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshWithDeviceChallenge\",\"scope\":\"*\"}";
        final Map<String, List<String>> headers = getHeader(
                AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER, challengeHeaderValue);
        final HttpWebResponse responeChallenge = new HttpWebResponse(
                HttpURLConnection.HTTP_UNAUTHORIZED, null, headers);
        final HttpWebResponse responseValid = new HttpWebResponse(
                HttpURLConnection.HTTP_OK, tokenPositiveResponse, null);
        // first call returns 401 and second call returns token
        when(
                mockWebRequest.sendPost(eq(new URL(TEST_AUTHORITY + "/oauth2/token")),
                        any(headers.getClass()), any(byte[].class),
                        eq("application/x-www-form-urlencoded"))).thenReturn(responeChallenge)
                .thenReturn(responseValid);

        // send request
        final MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                mockWebRequest, mockJwsBuilder, "testRefreshToken");

        // Verify that callback can receive this error
        assertNull("callback does not have error", testResult.getException());
        assertNotNull("Result is not null", testResult.getAuthenticationResult());
        assertEquals("Same access token", "accessTokenHere",
                testResult.getAuthenticationResult().getAccessToken());
        assertEquals("Same refresh token", "refreshWithDeviceChallenge",
                testResult.getAuthenticationResult().getRefreshToken());
    }

    @SuppressWarnings("unchecked")
    @SmallTest
    public void testRefreshTokenWebResponseDeviceChallengeHeaderEmpty()
            throws IOException {
        IWebRequestHandler mockWebRequest = mock(IWebRequestHandler.class);
        Map<String, List<String>> headers = getHeader(
                AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER, " ");
        HttpWebResponse responeChallenge = new HttpWebResponse(HttpURLConnection.HTTP_UNAUTHORIZED, null, headers);
        when(
                mockWebRequest.sendPost(eq(new URL(TEST_AUTHORITY + "/oauth2/token")),
                        any(headers.getClass()), any(byte[].class),
                        eq("application/x-www-form-urlencoded"))).thenReturn(responeChallenge);

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                mockWebRequest, "testRefreshToken");

        // Verify that callback can receive this error
        assertNotNull("Callback has error", testResult.getException());
        assertNotNull(testResult.getException());
        assertTrue(testResult.getException() instanceof AuthenticationException);
        assertEquals("Check error message", "Challenge header is empty",
                testResult.getException().getMessage());
    }

    @SmallTest
    public void testprocessTokenResponse() throws  IOException {

        final AuthenticationRequest request = createAuthenticationRequest(TEST_AUTHORITY,
                "resource", "client", "redirect", "loginhint", null, null, UUID.randomUUID(), false);
        final Oauth2 oauth2 = createOAuthInstance(request, new WebRequestHandler());
        final String idToken = Util.getIdToken();
        final String jsonResponse = "{\"id_token\":\"" + idToken
                + "\",\"access_token\":\"sometokenhere2343=\",\"token_type\":\"Bearer\","
                + "\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":"
                + "\"refreshfasdfsdf435=\",\"scope\":\"*\"}";

        // mock token request response
        final HttpURLConnection mockedConnection = mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        when(mockedConnection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockedConnection.getInputStream()).thenReturn(
                Util.createInputStream(jsonResponse));
        when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // send call with mocks
        try {
            final AuthenticationResult result = oauth2.refreshToken("fakeRT");
            // verify same token
            assertEquals("Same token in parsed result", "sometokenhere2343=", result.getAccessToken());
            assertEquals("Same refresh token in parsed result", "refreshfasdfsdf435=",
                    result.getRefreshToken());
            assertEquals("Same rawIdToken", idToken, result.getIdToken());
        } catch (final AuthenticationException e) {
            fail("Unexpected Exception");
        }
    }

    @SmallTest
    public void testprocessTokenResponseWrongCorrelationId() throws IOException {
        final AuthenticationRequest request = createAuthenticationRequest(TEST_AUTHORITY, "resource",
                "client", "redirect", "loginhint", null, null, UUID.randomUUID(), false);
        final Oauth2 oauth2 = createOAuthInstance(request, new WebRequestHandler());

        final String jsonResponse = "{\"access_token\":\"sometokenhere2343=\",\"token_type\":"
                + "\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\","
                + "\"refresh_token\":\"refreshfasdfsdf435=\",\"scope\":\"*\"}";

        // mock token request response
        final HttpURLConnection mockedConnection = mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        when(mockedConnection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockedConnection.getInputStream()).thenReturn(
                Util.createInputStream(jsonResponse), Util.createInputStream(jsonResponse));
        when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // mock conncetion header
        final List<String> listOfHeaders = new ArrayList<>();
        listOfHeaders.add(UUID.randomUUID().toString());
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, listOfHeaders);
        when(mockedConnection.getHeaderFields()).thenReturn(headers);

        final TestLogResponse logResponse = new TestLogResponse();
        logResponse.listenForLogMessage("CorrelationId is not matching", null);

        try {
            final AuthenticationResult result = oauth2.refreshToken("fakeRefreshToken");
            // verify same token
            assertEquals("Same token in parsed result", "sometokenhere2343=", result.getAccessToken());
            assertTrue("Log response has message",
                    logResponse.getErrorCode()
                            .equals(ADALError.CORRELATION_ID_NOT_MATCHING_REQUEST_RESPONSE));
        } catch (final AuthenticationException e) {
            fail("unexpected exception");
        }

        final List<String> invalidHeaders = new ArrayList<>();
        invalidHeaders.add("invalid-UUID");
        headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, invalidHeaders);
        when(mockedConnection.getHeaderFields()).thenReturn(headers);
        TestLogResponse logResponse2 = new TestLogResponse();
        logResponse2.listenLogForMessageSegments("Wrong format of the correlation ID:");
        try {
            oauth2.refreshToken("fakeRefreshToken");
            assertTrue("Log response has message",
                    logResponse2.getErrorCode().equals(ADALError.CORRELATION_ID_FORMAT));
        } catch (final AuthenticationException e) {
            fail("Unexpected exception");
        }
    }

    @SmallTest
    public void testprocessTokenResponseNegative() throws IOException {

        final AuthenticationRequest request = createAuthenticationRequest(TEST_AUTHORITY, "resource",
                "client", "redirect", "loginhint", null, null, UUID.randomUUID(), false);
        final Oauth2 oauth2 = createOAuthInstance(request, new WebRequestHandler());
        final String jsonResponse = "{invalid";

        final HttpURLConnection mockedConnection = mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        when(mockedConnection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockedConnection.getInputStream()).thenReturn(
                Util.createInputStream(jsonResponse));
        when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // send call with mocks
        try {
            oauth2.refreshToken("fakeRefreshToken");
            fail("must throw exception");
        } catch (final AuthenticationException e) {
            assertNotNull(e);
            assertEquals(e.getCode(), ADALError.SERVER_INVALID_JSON_RESPONSE);
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof JSONException);
        }
    }

    @SmallTest
    public void testprocessUIResponseParams() throws AuthenticationException {
        final Map<String, String> response = new HashMap<>();

        AuthenticationResult result = Oauth2.processUIResponseParams(response);
        assertNull("Result is null", result);

        // call when response has error
        response.put(AuthenticationConstants.OAuth2.ERROR, "error");
        response.put(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION, "error description");
        result = Oauth2.processUIResponseParams(response);
        assertEquals("Failed status", AuthenticationStatus.Failed, result.getStatus());
        assertEquals("Error is same", "error", result.getErrorCode());
        assertEquals("Error description is same", "error description", result.getErrorDescription());

        // add token
        response.clear();
        response.put(AuthenticationConstants.OAuth2.ACCESS_TOKEN, "token");
        result = Oauth2.processUIResponseParams(response);
        assertEquals("Success status", AuthenticationStatus.Succeeded, result.getStatus());
        assertEquals("Token is same", "token", result.getAccessToken());
        assertFalse("MultiResource token", result.getIsMultiResourceRefreshToken());

        // resource returned in JSON response, but RT is not returned.
        response.put(AuthenticationConstants.AAD.RESOURCE, "resource");
        result = Oauth2.processUIResponseParams(response);
        assertEquals("Success status", AuthenticationStatus.Succeeded, result.getStatus());
        assertEquals("Token is same", "token", result.getAccessToken());
        assertFalse("MultiResource token", result.getIsMultiResourceRefreshToken());

        // resource returned in JSON response and RT is also returned.
        response.put(AuthenticationConstants.OAuth2.REFRESH_TOKEN, "refresh_token");
        result = Oauth2.processUIResponseParams(response);
        assertEquals("Success status", AuthenticationStatus.Succeeded, result.getStatus());
        assertEquals("Token is same", "token", result.getAccessToken());
        assertEquals("RT is the same", "refresh_token", result.getRefreshToken());
        assertTrue("MultiResource token", result.getIsMultiResourceRefreshToken());
    }

    private AuthenticationRequest getValidAuthenticationRequest() {
        return createAuthenticationRequest(TEST_AUTHORITY, "resource%20 ",
                "client 1234567890-+=;'", "redirect 1234567890-+=;'", "loginhint@ggg.com", null,
                null, UUID.randomUUID(), false);
    }

    private MockAuthenticationCallback refreshToken(final AuthenticationRequest request,
                                                    final IWebRequestHandler webRequest,
                                                    final String refreshToken) {
        final CountDownLatch signal = new CountDownLatch(1);
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final Oauth2 oauth2 = createOAuthInstance(request, webRequest);

        try {
            callback.setAuthenticationResult(oauth2.refreshToken(refreshToken));
        } catch (Exception e) {
            callback.setException(e);
        }

        // callback has set the result from the call
        return callback;
    }

    private MockAuthenticationCallback refreshToken(final AuthenticationRequest request,
                                                    final IWebRequestHandler webRequest,
                                                    final IJWSBuilder jwsBuilder,
                                                    final String refreshToken) {
        final CountDownLatch signal = new CountDownLatch(1);
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final Oauth2 oauth2 = createOAuthInstance(request, webRequest, jwsBuilder);
        try {
            callback.setAuthenticationResult(oauth2.refreshToken(refreshToken));
        } catch (Exception e) {
            callback.setException(e);
        }

        // callback has set the result from the call
        return callback;
    }

    public static AuthenticationRequest createAuthenticationRequest(final String authority,
                                                                    final String resource,
                                                                    final String client,
                                                                    final String redirect,
                                                                    final String loginhint,
                                                                    final PromptBehavior prompt,
                                                                    final String extraQueryParams,
                                                                    final UUID correlationId,
                                                                    final boolean isExtendedLifetimeEnabled) {

        return new AuthenticationRequest(authority, resource, client, redirect, loginhint, prompt,
                extraQueryParams, correlationId, isExtendedLifetimeEnabled);

    }

    public static Oauth2 createOAuthInstance(final AuthenticationRequest authenticationRequest) {
        return new Oauth2(authenticationRequest);
    }

    private static Oauth2 createOAuthInstance(final AuthenticationRequest authenticationRequest,
                                              final IWebRequestHandler mockWebRequest) {
        if (mockWebRequest == null) {
            return createOAuthInstance(authenticationRequest);
        }

        return new Oauth2(authenticationRequest, mockWebRequest);
    }

    private static Oauth2 createOAuthInstance(final AuthenticationRequest authenticationRequest,
                                              final IWebRequestHandler mockWebRequest,
                                              final IJWSBuilder jwsBuilder) {
        if (mockWebRequest == null) {
            return createOAuthInstance(authenticationRequest);
        }

        return new Oauth2(authenticationRequest, mockWebRequest, jwsBuilder);
    }

    private Map<String, List<String>> getHeader(String key, String value) {
        return Collections.singletonMap(key, Collections.singletonList(value));
    }

    private KeyPair getKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        final int keySize = 1024;
        keyGen.initialize(keySize);
        KeyPair keyPair = keyGen.genKeyPair();
        return keyPair;
    }
}