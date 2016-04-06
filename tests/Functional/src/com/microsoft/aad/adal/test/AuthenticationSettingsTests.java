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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.IWindowComponent;

import android.content.Intent;
import android.test.AndroidTestCase;
import junit.framework.Assert;

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

    public void testHardwareAcceleration() {
        // verify setter/getter for WebView hardwareAcceleration
        //By default it should be enable
        assertEquals("isWebViewHardwareAccelerated", true, AuthenticationSettings.INSTANCE.getDisableWebViewHardwareAcceleration());

        // Modify
        AuthenticationSettings.INSTANCE.setDisableWebViewHardwareAcceleration(false);

        // Now it should be disable
        assertEquals("isWebViewHardwareAccelerated", false, AuthenticationSettings.INSTANCE.getDisableWebViewHardwareAcceleration());
    }
}
