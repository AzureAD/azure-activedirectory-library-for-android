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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

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
        assertFalse("not empty", StringExtensions.isNullOrBlank("non-Empty"));
    }

    public void testIsNullOrBlankEmpty() throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException {
        assertTrue("empty", StringExtensions.isNullOrBlank(""));

        assertTrue("empty", StringExtensions.isNullOrBlank("          "));
    }

    public void testURLFormEncodeDecode() {

        try {
            assertEquals("https%3A%2F%2Flogin.windows.net%2Faaltests.onmicrosoft.com%2F",
                    StringExtensions.urlFormEncode("https://login.windows.net/aaltests.onmicrosoft.com/"));

            assertEquals("https://login.windows.net/aaltests.onmicrosoft.com/",
                    StringExtensions.urlFormDecode("https%3A%2F%2Flogin.windows.net%2Faaltests.onmicrosoft.com%2F"));

            assertEquals("abc+d1234567890-",
                    StringExtensions.urlFormEncode("abc d1234567890-"));

            assertEquals("abc d1234567890-",
                    StringExtensions.urlFormDecode("abc+d1234567890-"));

            String longString = "asdfk+j0a-=skjwe43;1l234 1#$!$#%345903485qrq@#$!@#$!(rekr341!#$%Ekfaآزمايشsdsdfsddfdgsfgjsglk==CVADS";
            String result = StringExtensions.urlFormEncode(longString);

            String decodeResult = StringExtensions.urlFormDecode(result);
            assertEquals(longString, decodeResult);

        } catch (UnsupportedEncodingException ueex) {
            Log.e(getName(), ueex.getMessage());
            Assert.fail("Did not expect exception");
        }
    }
}
