
package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.test.AndroidTestCase;

public class AuthenticationRequestTests extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
    }

    public void testAuthenticationRequestParams() throws NoSuchMethodException,
            ClassNotFoundException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Constructor<?> constructor = Class.forName(
                ReflectionUtils.TEST_PACKAGE_NAME + ".AuthenticationRequest")
                .getDeclaredConstructor();
        constructor.setAccessible(true);
        Object o = constructor.newInstance();

        String actual = getValue(o, "getAuthority");
        assertNull("authority is null", actual);

        // call with params
        Constructor<?> constructorParams = Class.forName(
                ReflectionUtils.TEST_PACKAGE_NAME + ".AuthenticationRequest")
                .getDeclaredConstructor(String.class, String.class, String.class, String.class,
                        String.class);
        constructorParams.setAccessible(true);
        o = constructorParams.newInstance("authority", "resource", "client", "redirect",
                "loginhint");

        actual = getValue(o, "getAuthority");
        assertEquals("authority is same", "authority", actual);
        actual = getValue(o, "getResource");
        assertEquals("resource is same", "resource", actual);
        actual = getValue(o, "getClientId");
        assertEquals("client is same", "client", actual);
        actual = getValue(o, "getRedirectUri");
        assertEquals("redirect is same", "redirect", actual);
        actual = getValue(o, "getLoginHint");
        assertEquals("loginhint is same", "loginhint", actual);
    }

    private String getValue(Object authenticationRequest, String methodName)
            throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        Method m = authenticationRequest.getClass().getDeclaredMethod(methodName);

        String actual = (String)m.invoke(authenticationRequest, null);
        return actual;
    }
}
