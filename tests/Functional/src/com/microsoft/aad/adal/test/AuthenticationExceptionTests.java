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

package com.microsoft.aad.adal.test;

import android.test.AndroidTestCase;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationException;

public class AuthenticationExceptionTests extends AndroidTestCase{

    public void testException(){
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
