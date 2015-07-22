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
import java.util.UUID;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.UserIdentifier;
import com.microsoft.aad.adal.UserIdentifier.UserIdentifierType;

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
        o = createAuthenticationRequest("authority1", "resource2", "client3", "" , "userid");
        actual = ReflectionUtils.getterValue(String.class, o, "getAuthority");
        assertEquals("authority is same", "authority1", actual);
        String[] scope = ReflectionUtils.getterValue(String[].class, o, "getScope");
        assertEquals("scope is same", "resource2", scope[0]);
        actual = ReflectionUtils.getterValue(String.class, o, "getClientId");
        assertEquals("client is same", "client3", actual);

        o = createAuthenticationRequest("authority31", "resource32", "client33", "redirect34",
                "loginhint35");

        actual = ReflectionUtils.getterValue(String.class, o, "getAuthority");
        assertEquals("authority is same", "authority31", actual);
        scope = ReflectionUtils.getterValue(String[].class, o, "getScope");
        assertEquals("resource is same", "resource32", scope[0]);
        actual = ReflectionUtils.getterValue(String.class, o, "getClientId");
        assertEquals("client is same", "client33", actual);
        actual = ReflectionUtils.getterValue(String.class, o, "getRedirectUri");
        assertEquals("redirect is same", "redirect34", actual);
    }

    @SmallTest
    public void testRequestId() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Object o = createAuthenticationRequest("authority1", "scope", "client3", "", "");
        ReflectionUtils.setterValue(o, "setRequestId", Integer.valueOf(1234));
        int actual = ReflectionUtils.getterValue(Integer.class, o, "getRequestId");
        assertEquals("Same RequestId", 1234, actual);
    }
    
	private Object createAuthenticationRequest(String authority,
			String singleScope, String client, String redirect, String userid)
			throws IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {
		Class<?> c = Class
				.forName("com.microsoft.aad.adal.AuthenticationRequest");
		Constructor<?> constructor = c.getDeclaredConstructor(String.class,
				String[].class, String.class, String.class,
				UserIdentifier.class, PromptBehavior.class, String.class,
				UUID.class);
		constructor.setAccessible(true);
		Object obj = constructor.newInstance(authority,
				new String[] { singleScope }, client, redirect,
				new UserIdentifier(userid, UserIdentifierType.UniqueId),
				PromptBehavior.Auto, "", null);
		return obj;
	}
}
