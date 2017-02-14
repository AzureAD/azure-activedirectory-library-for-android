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

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class HttpEventTest extends AndroidTestCase {

    @SmallTest
    public void testProcessEvent() throws MalformedURLException {
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setHttpPath(new URL("https://login.microsoftonline.com/contoso/oauth2/token"));
        event.setOauthErrorCode("interaction_required");
        event.setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);

        final Map<String, String> dispatchMap = new HashMap<>();
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

        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);

        // The path should not be there, so the map should have only the count element
        assertNull(dispatchMap.get(EventStrings.HTTP_PATH));
        assertTrue(dispatchMap.get(EventStrings.HTTP_EVENT_COUNT).equals("1"));
    }

    @SmallTest
    public void testAADAuthorityPath() throws MalformedURLException {
        final HttpEvent event = new HttpEvent((EventStrings.HTTP_EVENT));
        event.setHttpPath(new URL("https://login.microsoftonline.com/myTenant.com/oauth2/"));

        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);

        // The path should be here
        assertNotNull(dispatchMap.get(EventStrings.HTTP_PATH));
        assertTrue(dispatchMap.get(EventStrings.HTTP_PATH).equals("https://login.microsoftonline.com/oauth2/"));
        assertTrue(dispatchMap.get(EventStrings.HTTP_EVENT_COUNT).equals("1"));
    }

    @SmallTest
    public void testOverlappingHttpEvent() {
        final HttpEvent event = new HttpEvent((EventStrings.HTTP_EVENT));
        event.setOauthErrorCode("some error");
        event.setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);

        final HttpEvent event2 = new HttpEvent(EventStrings.HTTP_EVENT);
        event2.setResponseCode(HttpURLConnection.HTTP_OK);

        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        event2.processEvent(dispatchMap);

        assertTrue(dispatchMap.get(EventStrings.OAUTH_ERROR_CODE).isEmpty());
        assertTrue(dispatchMap.get(EventStrings.HTTP_RESPONSE_CODE).equals(String.valueOf(HttpURLConnection.HTTP_OK)));
    }
}
