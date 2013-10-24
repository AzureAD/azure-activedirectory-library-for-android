/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.TestCase;

/**
 * Discovery class is not public, so it needs reflection to make a call to
 * non-public class in different package
 * 
 * @author omercan
 */
public class DiscoveryTests extends TestCase {

    public DiscoveryTests(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsValidAuthority() throws NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, ClassNotFoundException,
            InstantiationException, InvocationTargetException
    {
        final String methodName = "IsValidAuthority";
        Object foo = getDiscoveryInstance();
        Class<?> c = foo.getClass();
        Method m = c.getDeclaredMethod(methodName, String.class);

        // Verify unimplemented exception
        try {
            m.invoke(foo, "invalid endpoint");
            assertFalse("add implementation", true);
        } catch (InvocationTargetException ex) {
            assertSame("come back later", ex.getCause().getMessage());
        }
    }

    private Object getDiscoveryInstance() throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException
    {
        // full package name
        Class<?> c;

        c = Class.forName("com.microsoft.adal.Discovery");

        // getConstructor() returns only public constructors,

        Constructor<?> constructor = c.getDeclaredConstructor();

        constructor.setAccessible(true);
        Object o = constructor.newInstance(null);

        return o;
    }
}
