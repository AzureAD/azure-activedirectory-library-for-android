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
import android.test.suitebuilder.annotation.SmallTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class AuthenticationRequestTests extends AndroidTestCase {
    static final int REQUEST_ID = 1234;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
    }

    @SmallTest
    public void testAuthenticationRequestParams() throws NoSuchMethodException,
            ClassNotFoundException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Constructor<?> constructor = Class.forName(
                ReflectionUtils.TEST_PACKAGE_NAME + ".AuthenticationRequest")
                .getDeclaredConstructor();
        constructor.setAccessible(true);
        Object o = constructor.newInstance();

        String actual = ReflectionUtils.getterValue(String.class, o, "getAuthority");
        assertNull("authority is null", actual);

        // call with params
        o = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".AuthenticationRequest", "authority1", "resource2", "client3", false);
        actual = ReflectionUtils.getterValue(String.class, o, "getAuthority");
        assertEquals("authority is same", "authority1", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getResource");
        assertEquals("resource is same", "resource2", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getClientId");
        assertEquals("client is same", "client3", actual);

        o = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                        + ".AuthenticationRequest", "authority31", "resource32", "client33", "redirect34",
                "loginhint35", false);

        actual = ReflectionUtils.getterValue(String.class, o, "getAuthority");
        assertEquals("authority is same", "authority31", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getResource");
        assertEquals("resource is same", "resource32", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getClientId");
        assertEquals("client is same", "client33", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getRedirectUri");
        assertEquals("redirect is same", "redirect34", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getLoginHint");
        assertEquals("loginhint is same", "loginhint35", actual);

        UUID correlationId = UUID.randomUUID();
        o = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                        + ".AuthenticationRequest", "authority41", "resource42", "client43", "redirect44",
                "loginhint45", correlationId, false);

        actual = ReflectionUtils.getterValue(String.class, o, "getAuthority");
        assertEquals("authority is same", "authority41", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getResource");
        assertEquals("resource is same", "resource42", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getClientId");
        assertEquals("client is same", "client43", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getRedirectUri");
        assertEquals("redirect is same", "redirect44", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getLoginHint");
        assertEquals("loginhint is same", "loginhint45", actual);
        UUID actualId = ReflectionUtils.getterValue(UUID.class, o, "getCorrelationId");
        assertEquals("correlationId is same", correlationId, actualId);

        o = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                        + ".AuthenticationRequest", "authority51", "resource52", "client53", "redirect54",
                "loginhint55", PromptBehavior.Always, "extraQueryPAram56", correlationId, false);

        actual = ReflectionUtils.getterValue(String.class, o, "getAuthority");
        assertEquals("authority is same", "authority51", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getResource");
        assertEquals("resource is same", "resource52", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getClientId");
        assertEquals("client is same", "client53", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getRedirectUri");
        assertEquals("redirect is same", "redirect54", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getLoginHint");
        assertEquals("loginhint is same", "loginhint55", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getExtraQueryParamsAuthentication");
        assertEquals("ExtraQueryParams is same", "extraQueryPAram56", actual);
        PromptBehavior actualPrompt = ReflectionUtils.getterValue(PromptBehavior.class, o, "getPrompt");
        assertEquals("PromptBehavior is same", PromptBehavior.Always, actualPrompt);
        actualId = ReflectionUtils.getterValue(UUID.class, o, "getCorrelationId");
        assertEquals("correlationId is same", correlationId, actualId);
    }

    @SmallTest
    public void testRequestId() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Object o = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".AuthenticationRequest", "authority1", "resource2", "client3", false);
        ReflectionUtils.setterValue(o, "setRequestId", Integer.valueOf(REQUEST_ID));
        int actual = ReflectionUtils.getterValue(Integer.class, o, "getRequestId");
        assertEquals("Same RequestId", REQUEST_ID, actual);
    }

    @SmallTest
    public void testGetUpnSuffix() {
        AuthenticationRequest authenticationRequest =
                new AuthenticationRequest(
                        "NA", // authority
                        "NA", // resource
                        "NA", // client
                        "NA", // redirect
                        "user@foo.internet", // loginhint,
                        false
                );
        assertEquals("foo.internet", authenticationRequest.getUpnSuffix());
    }

    @SmallTest
    public void testGetUpnSuffixNull() {
        AuthenticationRequest authenticationRequest =
                new AuthenticationRequest(
                        "NA", // authority
                        "NA", // resource
                        "NA", // client
                        "NA", // redirect
                        "user", // loginhint,
                        false
                );
        assertEquals(null, authenticationRequest.getUpnSuffix());
    }
}
