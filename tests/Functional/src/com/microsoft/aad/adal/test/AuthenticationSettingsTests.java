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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import junit.framework.Assert;

import android.app.Activity;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.CacheKey;
import com.microsoft.aad.adal.IWindowComponent;
import com.microsoft.aad.adal.TokenCacheItem;

/**
 * settings to use in ADAL
 */
public class AuthenticationSettingsTests extends AndroidTestCase {

    private static final String VALID_AUTHORITY = "https://Login.windows.net/Omercantest.Onmicrosoft.com";

    public void testActivityPackageName() throws NoSuchAlgorithmException, NoSuchPaddingException,
            SecurityException, IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {

        // verify setter/getter
        String packagename = "com.anotherapp";
        AuthenticationSettings.INSTANCE.setActivityPackageName(packagename);

        assertEquals("same packagename", packagename,
                AuthenticationSettings.INSTANCE.getActivityPackageName());

        // verify intent
        AuthenticationContext context = new AuthenticationContext(getContext(), VALID_AUTHORITY,
                false);
        Class clazzAuthRequest = Class.forName(ReflectionUtils.TEST_PACKAGE_NAME
                + ".AuthenticationRequest");
        Constructor<?> constructor = clazzAuthRequest.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object authRequest = constructor.newInstance();
        Method m = ReflectionUtils.getTestMethod(context, "getAuthenticationActivityIntent",
                IWindowComponent.class, clazzAuthRequest);

        Intent intent = (Intent)m.invoke(context, null, authRequest);

        assertEquals("same packagename", packagename, intent.getComponent().getPackageName());
    }

    public void testTimeOut() {
        // verify setter/getter for timeout
        assertEquals("default timeout", 30000, AuthenticationSettings.INSTANCE.getReadTimeOut());

        // Modify
        AuthenticationSettings.INSTANCE.setReadTimeOut(1000);

        assertEquals(1000, AuthenticationSettings.INSTANCE.getReadTimeOut());

        try {
            AuthenticationSettings.INSTANCE.setReadTimeOut(-1);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }

        AuthenticationSettings.INSTANCE.setReadTimeOut(30000);
    }
}
