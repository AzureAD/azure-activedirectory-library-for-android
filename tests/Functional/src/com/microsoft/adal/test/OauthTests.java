
package com.microsoft.adal.test;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;

import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.AuthenticationResult.AuthenticationStatus;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.IWebRequestHandler;
import com.microsoft.adal.PromptBehavior;
import com.microsoft.adal.UserInfo;
import com.microsoft.adal.test.AuthenticationConstants.AAD;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OauthTests extends AndroidTestCase {

    private static final String TEST_RETURNED_EXCEPTION = "test-returned-exception";

    @SmallTest
    public void testParseIdTokenPositive() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        String idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";
        UserInfo actual = parseIdToken(idToken);
        assertEquals("IdToken tenantid", "30baa666-8df8-48e7-97e6-77cfd0995963",
                actual.getTenantId());
        assertEquals("IdToken userid", "admin@aaltests.onmicrosoft.com", actual.getUserId());
        assertEquals("IdToken userid", "admin@aaltests.onmicrosoft.com", actual.getUserId());
        assertEquals("IdToken familyname", "Sepehri", actual.getFamilyName());
        assertEquals("IdToken name", "Afshin", actual.getGivenName());
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
    public void testGetToken_() throws UnsupportedEncodingException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {
        String resource = "resource:" + UUID.randomUUID().toString();
        Object request = createAuthenticationRequest("http://www.something.com", resource,
                "client", "redirect", "loginhint@ggg.com", null, null, null);
        Object oauth = createOAuthInstance(request);

    }

    @SmallTest
    public void testParseIdTokenNegativeIncorrectMessage() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        String idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2.";
        UserInfo actual = parseIdToken(idToken);
        assertNull("IdToken is null", actual);
    }

    @SmallTest
    public void testParseIdTokenNegativeInvalidEncodedTokens() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        String idToken = "..";
        UserInfo actual = parseIdToken(idToken);
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

    @SmallTest
    private UserInfo parseIdToken(String idToken) throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Object request = createAuthenticationRequest("http://www.something.com", "resource",
                "client", "redirect", "loginhint@ggg.com", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "parseIdToken", String.class);
        return (UserInfo)m.invoke(oauth, idToken);
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
                "grant_type=authorization_code&code=authorizationcodevalue%3D&client_id=client+1234567890-%2B%3D%3B%27&redirect_uri=redirect+1234567890-%2B%3D%3B%27&login_hint=loginhint%40ggg.com",
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

        // send request
        MockAuthenticationCallback testResult = refreshToken(getValidAuthenticationRequest(),
                webrequest, "test");

        // Verify that callback can receive this error
        assertNotNull("callback receives error code in the result after processing",
                testResult.mResult);
        assertTrue("callback has status info", testResult.mResult.getErrorCode().contains("503"));
        assertNull("Exception is null", testResult.mException);

        // Invalid status that cause some exception at webrequest
        webrequest.setReturnException(TEST_RETURNED_EXCEPTION);

        // send request
        TestLogResponse response = new TestLogResponse();
        response.listenForLogMessage(TEST_RETURNED_EXCEPTION, null);
        testResult = refreshToken(getValidAuthenticationRequest(), webrequest, "test");

        // Verify that result returns null from this error
        assertNull("Result is null", testResult.mResult);
        assertEquals("Exception has same error message", TEST_RETURNED_EXCEPTION, response.message);
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
        assertEquals("Same access token", "sometokenhere", testResult.mResult.getAccessToken());
        assertEquals("Same refresh token", "refreshfasdfsdf435",
                testResult.mResult.getRefreshToken());
    }

    @SmallTest
    public void testprocessTokenResponse() throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException {

        Object request = createAuthenticationRequest("authority", "resource", "client", "redirect",
                "loginhint", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "processTokenResponse",
                Class.forName("com.microsoft.adal.HttpWebResponse"));
        String json = "{\"access_token\":\"sometokenhere2343=\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshfasdfsdf435=\",\"scope\":\"*\"}";
        HttpWebResponse mockResponse = new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null);

        // send call with mocks
        AuthenticationResult result = (AuthenticationResult)m.invoke(oauth, mockResponse);

        // verify same token
        assertEquals("Same token in parsed result", "sometokenhere2343=", result.getAccessToken());
        assertEquals("Same refresh token in parsed result", "refreshfasdfsdf435=",
                result.getRefreshToken());
    }

    @SmallTest
    public void testprocessTokenResponseNegative() throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {

        Object request = createAuthenticationRequest("authority", "resource", "client", "redirect",
                "loginhint", null, null, null);
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "processTokenResponse",
                Class.forName("com.microsoft.adal.HttpWebResponse"));
        String json = "{invalid";
        HttpWebResponse mockResponse = new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null);

        // send call with mocks
        AuthenticationResult result = (AuthenticationResult)m.invoke(oauth, mockResponse);

        // verify same token
        assertEquals("Same token in parsed result", "It failed to parse response as json",
                result.getErrorCode());
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
        assertEquals("Failed status", AuthenticationStatus.Failed, result.getStatus());
        assertNull("Token is null", result.getAccessToken());

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
        return createAuthenticationRequest("http://www.something.com", "resource%20 ",
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

    public static Object createAuthenticationRequest(String authority, String resource,
            String client, String redirect, String loginhint, PromptBehavior prompt,
            String extraQueryParams, UUID correlationId) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.adal.AuthenticationRequest");

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

        Class<?> c = Class.forName("com.microsoft.adal.Oauth2");

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

        Class<?> c = Class.forName("com.microsoft.adal.Oauth2");

        Constructor<?> constructor = c.getDeclaredConstructor(authenticationRequest.getClass(),
                IWebRequestHandler.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authenticationRequest, mockWebRequest);
        return o;
    }
}
