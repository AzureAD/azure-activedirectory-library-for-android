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

package com.microsoft.aad.adal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Assert;

import android.test.AndroidTestCase;
import android.util.Log;

/**
 * StringExtensions class has helper methods and it is not public
 */
public class StringExtensionTests extends AndroidTestHelper {
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsNullOrBlankNotEmpty() throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {
        final String methodName = "IsNullOrBlank";
        Object foo = ReflectionUtils.getNonPublicInstance("com.microsoft.aad.adal.StringExtensions");
        Method m = ReflectionUtils.getTestMethod(foo, methodName, String.class);
        boolean result = (Boolean)m.invoke(foo, "non-empty");
        assertFalse("not empty", result);
    }

    public void testIsNullOrBlankEmpty() throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException {
        final String methodName = "IsNullOrBlank";
        Object foo = ReflectionUtils.getNonPublicInstance("com.microsoft.aad.adal.StringExtensions");

        Method m = ReflectionUtils.getTestMethod(foo, methodName, String.class);
        boolean result = (Boolean)m.invoke(foo, "");
        assertTrue("empty", result);

        result = (Boolean)m.invoke(foo, "  ");
        assertTrue("empty", result);

        result = (Boolean)m.invoke(foo, "          ");
        assertTrue("empty", result);

    }

    public void testURLFormEncodeDecode() {
        final String methodName = "URLFormEncode";
        try {
            Object foo = ReflectionUtils
                    .getNonPublicInstance("com.microsoft.aad.adal.StringExtensions");
            Method m = ReflectionUtils.getTestMethod(foo, methodName, String.class);
            Method decodeMethod = ReflectionUtils.getTestMethod(foo, "URLFormDecode", String.class);

            String result = (String)m.invoke(foo,
                    "https://login.windows.net/aaltests.onmicrosoft.com/");
            assertEquals("https%3A%2F%2Flogin.windows.net%2Faaltests.onmicrosoft.com%2F", result);

            result = (String)decodeMethod.invoke(foo,
                    "https%3A%2F%2Flogin.windows.net%2Faaltests.onmicrosoft.com%2F");
            assertEquals("https://login.windows.net/aaltests.onmicrosoft.com/", result);

            result = (String)m.invoke(foo, "abc d1234567890-");
            assertEquals("abc+d1234567890-", result);

            result = (String)decodeMethod.invoke(foo, "abc+d1234567890-");
            assertEquals("abc d1234567890-", result);

            String longString = "asdfk+j0a-=skjwe43;1l234 1#$!$#%345903485qrq@#$!@#$!(rekr341!#$%Ekfaآزمايشsdsdfsddfdgsfgjsglk==CVADS";
            result = (String)m.invoke(foo, longString);

            String decodeResult = (String)decodeMethod.invoke(foo, result);
            assertEquals(longString, decodeResult);

        } catch (Exception ex) {
            Log.e(getName(), ex.getMessage());
            Assert.fail("Dont expect exception");
        }
    }
}
