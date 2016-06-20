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

import android.test.AndroidTestCase;

public class AuthenticationExceptionTests extends AndroidTestCase {

    public void testException() {
        AuthenticationException exception = new AuthenticationException();
        assertNull("Code is null", exception.getCode());
        assertNull("Details is null", exception.getMessage());
        assertNull("Cause is null", exception.getCause());

        exception = new AuthenticationException(ADALError.AUTH_FAILED);
        assertEquals("Code is same", ADALError.AUTH_FAILED, exception.getCode());
        assertNotNull("Details is not null", exception.getMessage());
        assertNull("Cause is null", exception.getCause());

        exception = new AuthenticationException(ADALError.AUTH_FAILED, "Some details");
        assertEquals("Code is same", ADALError.AUTH_FAILED, exception.getCode());
        assertEquals("Details is not null", "Some details", exception.getMessage());
        assertNull("Cause is null", exception.getCause());

        exception = new AuthenticationException(ADALError.AUTH_FAILED, "Some details", new Throwable("test"));
        assertEquals("Code is same", ADALError.AUTH_FAILED, exception.getCode());
        assertEquals("Details is not null", "Some details", exception.getMessage());
        assertNotNull("Cause is not null", exception.getCause());
        assertEquals("Message from Cause is not null", "test", exception.getCause().getMessage());
    }
}
