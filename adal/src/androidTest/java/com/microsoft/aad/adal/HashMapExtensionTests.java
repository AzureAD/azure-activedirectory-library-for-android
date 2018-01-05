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

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class HashMapExtensionTests extends AndroidTestHelper {
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testURLFormDecodeNegative() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        final String methodName = "urlFormDecode";
        Object object = ReflectionUtils.getNonPublicInstance("com.microsoft.aad.adal.HashMapExtensions");
        Method m = ReflectionUtils.getTestMethod(object, methodName, String.class);
        HashMap<String, String> result = (HashMap<String, String>) m.invoke(object, "");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>) m.invoke(object, "&&&");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>) m.invoke(object, "=&=");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>) m.invoke(object, "=&");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>) m.invoke(object, "&=");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = (HashMap<String, String>) m.invoke(object, "&=b");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testURLFormDecodePositive() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        final String methodName = "urlFormDecode";
        Object object = ReflectionUtils.getNonPublicInstance("com.microsoft.aad.adal.HashMapExtensions");
        Method m = ReflectionUtils.getTestMethod(object, methodName, String.class);
        HashMap<String, String> result = (HashMap<String, String>) m.invoke(object, "a=b&c=2");
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

        result = (HashMap<String, String>) m.invoke(object, "a=v");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsValue("v"));

        result = (HashMap<String, String>) m.invoke(object, "d=f&");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("d"));
        assertTrue(result.containsValue("f"));
        assertTrue(result.size() == 1);

        result = (HashMap<String, String>) m.invoke(object, "=b&c=");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("c"));
        assertFalse(result.containsValue("b"));
        assertTrue(result.size() == 1);
    }
}
