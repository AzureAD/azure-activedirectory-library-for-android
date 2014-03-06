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

package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.microsoft.adal.PromptBehavior;

public class AuthenticationRequestTests extends AndroidTestCase {

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
                + ".AuthenticationRequest", "authority1", "resource2", "client3");
        actual = ReflectionUtils.getterValue(String.class, o, "getAuthority");
        assertEquals("authority is same", "authority1", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getResource");
        assertEquals("resource is same", "resource2", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getClientId");
        assertEquals("client is same", "client3", actual);

        o = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".AuthenticationRequest", "authority21", "resource22", "client23", "redirect24");
        actual = ReflectionUtils.getterValue(String.class, o, "getAuthority");
        assertEquals("authority is same", "authority21", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getResource");
        assertEquals("resource is same", "resource22", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getClientId");
        assertEquals("client is same", "client23", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getRedirectUri");
        assertEquals("client is same", "redirect24", actual);

        o = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".AuthenticationRequest", "authority31", "resource32", "client33", "redirect34",
                "loginhint35");

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
                "loginhint45", correlationId);

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
                "loginhint55", PromptBehavior.Never, "extraQueryPAram56", correlationId);

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
        assertEquals("PromptBehavior is same", PromptBehavior.Never, actualPrompt);
        actualId = ReflectionUtils.getterValue(UUID.class, o, "getCorrelationId");
        assertEquals("correlationId is same", correlationId, actualId);
    }

    @SmallTest
    public void testRequestId() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Object o = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".AuthenticationRequest", "authority1", "resource2", "client3");
        ReflectionUtils.setterValue(o, "setRequestId", Integer.valueOf(1234));
        int actual = ReflectionUtils.getterValue(Integer.class, o, "getRequestId");
        assertEquals("Same RequestId", 1234, actual);
    }

}
