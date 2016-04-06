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

package com.microsoft.aad.adal.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;
import junit.framework.Assert;

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
