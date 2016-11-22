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
import android.util.Pair;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HttpEventTest extends AndroidTestCase {

    @SmallTest
    public void testProcessEvent() throws MalformedURLException {
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setHttpPath(new URL("https://login.microsoftonline.com/contoso/oauth2/token"));
        event.setOauthErrorCode("interaction_required");
        event.setResponseCode(400);

        final Map<String, String> dispatchMap = new HashMap();
        event.processEvent(dispatchMap);

        assertFalse(dispatchMap.isEmpty());
        assertTrue(dispatchMap.containsKey(EventStrings.OAUTH_ERROR_CODE));
        assertTrue(dispatchMap.containsKey(EventStrings.HTTP_PATH));

        final String httpPath = dispatchMap.get(EventStrings.HTTP_PATH);
        assertTrue(httpPath.equals("https://login.microsoftonline.com/oauth2/token/"));
    }

    @SmallTest
    public void testADFSAuthorityPath() throws MalformedURLException {
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setHttpPath(new URL("https://contoso.com/adfs/ls"));

        final Map<String, String> dispatchMap = new HashMap();
        event.processEvent(dispatchMap);

        // The path should not be there, so the map should have only the count element
        assertTrue(dispatchMap.get(EventStrings.HTTP_PATH) == null);
        assertTrue(dispatchMap.get(EventStrings.HTTP_EVENT_COUNT).equals("1"));
    }
}
