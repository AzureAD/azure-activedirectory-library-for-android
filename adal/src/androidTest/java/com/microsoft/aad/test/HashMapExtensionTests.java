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

package com.microsoft.aad.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

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
        Object foo = ReflectionUtils.getNonPublicInstance("com.microsoft.aad.adal.HashMapExtensions");
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
        Object foo = ReflectionUtils.getNonPublicInstance("com.microsoft.aad.adal.HashMapExtensions");
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
