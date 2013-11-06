
package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import android.test.AndroidTestCase;

import com.microsoft.adal.AuthenticationConstants;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.AuthenticationResult.AuthenticationStatus;

public class OauthTests extends AndroidTestCase {

    public void testGetCodeRequestUrl() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        // with login hint
        Object request = createAuthenticationRequest("authorityForCode", "resourceForCode",
                "clientForCode", "redirectForCode", "loginhintForCode");
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "getCodeRequestUrl");

        String actual = (String)m.invoke(oauth);
        assertTrue(
                "Matching message",
                actual.contains("authorityForCode/oauth2/authorize?response_type=code&client_id=clientForCode&resource=resourceForCode&redirect_uri=redirectForCode&state="));
        assertTrue("Matching loginhint", actual.contains("login_hint=loginhintForCode"));
        // without login hint
        Object requestWithoutLogin = createAuthenticationRequest("authorityForCode",
                "resourceForCode", "clientForCode", "redirectForCode", "");

        Object oauthWithoutLoginHint = createOAuthInstance(requestWithoutLogin);

        actual = (String)m.invoke(oauthWithoutLoginHint);
        assertTrue(
                "Matching message",
                actual.contains("authorityForCode/oauth2/authorize?response_type=code&client_id=clientForCode&resource=resourceForCode&redirect_uri=redirectForCode&state="));
        assertFalse("Without loginhint", actual.contains("login_hint=loginhintForCode"));
    }

    public void testBuildTokenRequestMessage() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        // with login hint
        Object request = createAuthenticationRequest("authority", "resource", "client", "redirect",
                "loginhint");
        Object oauth = createOAuthInstance(request);
        Method m = ReflectionUtils.getTestMethod(oauth, "buildTokenRequestMessage", String.class);

        String actual = (String)m.invoke(oauth, "authorizationcodevalue");
        assertEquals(
                "Token request",
                "grant_type=authorization_code&code=authorizationcodevalue&client_id=client&redirect_uri=redirect&login_hint=loginhint",
                actual);

        // without login hint
        Object requestWithoutLogin = createAuthenticationRequest("authority", "resource", "client",
                "redirect", "");

        Object oauthWithoutLoginHint = createOAuthInstance(requestWithoutLogin);

        actual = (String)m.invoke(oauthWithoutLoginHint, "authorizationcodevalue");
        assertEquals(
                "Token request",
                "grant_type=authorization_code&code=authorizationcodevalue&client_id=client&redirect_uri=redirect",
                actual);
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

    private static Object createAuthenticationRequest(String authority, String resource,
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

    private static Object createOAuthInstance(Object authenticationRequest)
            throws ClassNotFoundException, NoSuchMethodException, IllegalArgumentException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.adal.Oauth");

        Constructor<?> constructor = c.getDeclaredConstructor(authenticationRequest.getClass());
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authenticationRequest);
        return o;
    }
}
