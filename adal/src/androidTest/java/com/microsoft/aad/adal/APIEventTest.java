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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public final class APIEventTest extends AndroidTestCase {

    @SmallTest
    public void testProcessEvent() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        final APIEvent event = new APIEvent(EventStrings.API_EVENT);

        event.setAPIId("123");
        event.setAuthority("AAD");
        event.setAuthorityType("AAD");
        event.setExtendedExpiresOnSetting(true);
        event.setIdToken(AuthenticationContextTest.TEST_IDTOKEN);
        event.setLoginHint("pii@pii.com");

        final Map<String, String> dispatchMap = new HashMap();
        event.processEvent(dispatchMap);

        assertFalse(dispatchMap.isEmpty());
        assertTrue(dispatchMap.containsKey(EventStrings.AUTHORITY_TYPE));
        assertTrue(dispatchMap.containsKey(EventStrings.EXTENDED_EXPIRES_ON_SETTING));
        assertFalse(dispatchMap.containsKey(EventStrings.API_ERROR_CODE));
        assertTrue(dispatchMap.containsKey(EventStrings.TENANT_ID));
        assertTrue(dispatchMap.containsKey(EventStrings.DEVICE_ID));
        assertTrue(dispatchMap.containsKey(EventStrings.CLIENT_ID));
        assertTrue(dispatchMap.containsKey(EventStrings.APPLICATION_VERSION));
        assertTrue(dispatchMap.containsKey(EventStrings.USER_ID));

        final String email = dispatchMap.get(EventStrings.LOGIN_HINT);
        assertTrue(email.equals(StringExtensions.createHash("pii@pii.com")));
    }
}
