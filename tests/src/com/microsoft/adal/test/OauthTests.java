
package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.UUID;

import android.test.AndroidTestCase;

import com.microsoft.adal.AuthenticationConstants;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.AuthenticationResult.AuthenticationStatus;
import com.microsoft.adal.HttpWebResponse;

public class OauthTests extends AndroidTestCase {

    public void testGetCodeRequestUrl() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        // with login hint
        Object request = createAuthenticationRequest("http://www.something.com",
                "resource%20urn:!#$    &'( )*+,/:  ;=?@[]",
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890",
                "loginhint 1234567890-+=;'");
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
                "client 1234567890-+=;!#$   &'( )*+,/:  ;=?@[]", "redirect 1234567890", "");

        Object oauthWithoutLoginHint = createOAuthInstance(requestWithoutLogin);

        actual = (String)m.invoke(oauthWithoutLoginHint);
        assertTrue(
                "Matching message",
                actual.contains("http://www.something.com/oauth2/authorize?response_type=code&client_id=client+1234567890-%2B%3D%3B%21%23%24+++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&resource=resource%2520urn%3A%21%23%24++++%26%27%28+%29*%2B%2C%2F%3A++%3B%3D%3F%40%5B%5D&redirect_uri=redirect+1234567890&state="));
        assertFalse("Without loginhint", actual.contains("login_hint=loginhintForCode"));
    }

    public void testBuildTokenRequestMessage() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        // with login hint
        Object request = createAuthenticationRequest("http://www.something.com", "resource%20 ",
                "client 1234567890-+=;'", "redirect 1234567890-+=;'", "loginhint@ggg.com");
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "buildTokenRequestMessage", String.class);

        String actual = (String)m.invoke(oauth, "authorizationcodevalue=");
        assertEquals(
                "Token request",
                "grant_type=authorization_code&code=authorizationcodevalue%3D&client_id=client+1234567890-%2B%3D%3B%27&redirect_uri=redirect+1234567890-%2B%3D%3B%27&login_hint=loginhint%40ggg.com",
                actual);

        // without login hint
        Object requestWithoutLogin = createAuthenticationRequest("http://www.something.com",
                "resource%20 ", "client 1234567890-+=;'", "redirect 1234567890-+=;'", "");

        Object oauthWithoutLoginHint = createOAuthInstance(requestWithoutLogin);

        actual = (String)m.invoke(oauthWithoutLoginHint, "authorizationcodevalue=");
        assertEquals(
                "Token request",
                "grant_type=authorization_code&code=authorizationcodevalue%3D&client_id=client+1234567890-%2B%3D%3B%27&redirect_uri=redirect+1234567890-%2B%3D%3B%27",
                actual);
    }

    public void testprocessTokenResponse() throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException {
        HashMap<String, String> response = new HashMap<String, String>();
        Object request = createAuthenticationRequest("authority", "resource", "client", "redirect",
                "loginhint");
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "processTokenResponse",
                HttpWebResponse.class);
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

    public void testprocessUIResponseParams() throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {
        HashMap<String, String> response = new HashMap<String, String>();
        Object request = createAuthenticationRequest("authority", "resource", "client", "redirect",
                "loginhint");
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

    public void testCorrelationIdInErrorResponse() throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {
        HashMap<String, String> response = new HashMap<String, String>();
        Object request = createAuthenticationRequest("http://login.windows.net", "resource",
                "client", "redirect", "loginhint");
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "processUIResponseParams", HashMap.class);
        UUID correlationIdExpected = UUID.randomUUID();
        response.put(AuthenticationConstants.AAD.CORRELATION_ID, correlationIdExpected.toString());
        response.put(AuthenticationConstants.OAuth2.ERROR, "errorCodeHere");
        response.put(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION, "errorDescription");

        // call to process response
        AuthenticationResult result = (AuthenticationResult)m.invoke(null, response);
        assertEquals("Failed status", AuthenticationStatus.Failed, result.getStatus());
        assertEquals("Same error", "errorCodeHere", result.getErrorCode());
        assertEquals("Same error description", "errorDescription", result.getErrorDescription());
        assertEquals("Same correlationid", correlationIdExpected, result.getCorrelationId());

        // malformed correlationid
        response.put(AuthenticationConstants.AAD.CORRELATION_ID, "333-4");

        // call to get null correlationid
        result = (AuthenticationResult)m.invoke(null, response);
        assertNull("Null correlationid", result.getCorrelationId());
    }

    public static Object createAuthenticationRequest(String authority, String resource,
            String client, String redirect, String loginhint) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.adal.AuthenticationRequest");

        Constructor<?> constructor = c.getDeclaredConstructor(String.class, String.class,
                String.class, String.class, String.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authority, resource, client, redirect, loginhint);
        return o;
    }

    public static Object createOAuthInstance(Object authenticationRequest)
            throws ClassNotFoundException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.adal.Oauth");

        Constructor<?> constructor = c.getDeclaredConstructor(authenticationRequest.getClass());
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authenticationRequest);
        return o;
    }
}
