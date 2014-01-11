
package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import junit.framework.Assert;

public class HashMapExtensionTests extends AndroidTestHelper {
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @SuppressWarnings("unchecked")
    public void testURLFormDecodeNegative() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        final String methodName = "URLFormDecode";
        Object foo = ReflectionUtils.getNonPublicInstance("com.microsoft.adal.HashMapExtensions");
        Method m = ReflectionUtils.getTestMethod(foo, methodName, String.class);
        HashMap<String, String> result = (HashMap<String, String>)m.invoke(foo, "nokeyvalue");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>)m.invoke(foo, "&&&");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>)m.invoke(foo, "=&=");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>)m.invoke(foo, "=&");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>)m.invoke(foo, "&=");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>)m.invoke(foo, "&a=");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>)m.invoke(foo, "&=b");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @SuppressWarnings("unchecked")
    public void testURLFormDecodePositive() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        final String methodName = "URLFormDecode";
        Object foo = ReflectionUtils.getNonPublicInstance("com.microsoft.adal.HashMapExtensions");
        Method m = ReflectionUtils.getTestMethod(foo, methodName, String.class);
        HashMap<String, String> result = (HashMap<String, String>)m.invoke(foo, "a=b&c=2");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("c"));
        assertTrue(result.containsValue("b"));
        assertTrue(result.containsValue("2"));

        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("c"));
        assertTrue(result.containsValue("b"));
        assertTrue(result.containsValue("2"));

        result = (HashMap<String, String>)m.invoke(foo, "a=v");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsValue("v"));

        result = (HashMap<String, String>)m.invoke(foo, "d=f&");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("d"));
        assertTrue(result.containsValue("f"));
        assertTrue(result.size() == 1);
    }

    private HashMap<String, String> getTestKeyValue(String key, String value) {
        HashMap<String, String> result = new HashMap<String, String>();
        result.put(key, value);
        return result;
    }
}
