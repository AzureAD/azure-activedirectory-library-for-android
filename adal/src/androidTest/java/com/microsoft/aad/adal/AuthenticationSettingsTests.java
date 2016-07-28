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

import junit.framework.Assert;

/**
 * settings to use in ADAL
 */
public class AuthenticationSettingsTests extends AndroidTestCase {

    private static final int READ_TIMEOUT_1 = 1000;

    private static final int READ_TIMEOUT_2 = 30000;

    public void testActivityPackageName() {

        // verify setter/getter
        final String packagename = "com.anotherapp";
        AuthenticationSettings.INSTANCE.setActivityPackageName(packagename);

        assertEquals("same packagename", packagename,
                AuthenticationSettings.INSTANCE.getActivityPackageName());
    }

    public void testTimeOut() {
        // verify setter/getter for timeout
        assertEquals("default timeout", READ_TIMEOUT_2, AuthenticationSettings.INSTANCE.getReadTimeOut());

        // Modify
        AuthenticationSettings.INSTANCE.setReadTimeOut(READ_TIMEOUT_1);

        assertEquals(READ_TIMEOUT_1, AuthenticationSettings.INSTANCE.getReadTimeOut());

        try {
            AuthenticationSettings.INSTANCE.setReadTimeOut(-1);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }

        AuthenticationSettings.INSTANCE.setReadTimeOut(READ_TIMEOUT_2);
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
