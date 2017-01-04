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

import java.net.MalformedURLException;
import java.net.URL;

public final class AggregatedDispatcherTest extends AndroidTestCase {

    private static final String CONSTANT_REQUEST_ID = "random";

    private static final String CONSTANT_REQUEST_ID_1 = "random1";

    private static final String CONSTANT_REQUEST_ID_2 = "random2";

    @SmallTest
    public void testEmptyEvents() {
        final AggregatedDispatcher dispatcher = new AggregatedDispatcher(new AggregatedTelemetryTestClass());
        // Empty events should not throw
        dispatcher.flush(CONSTANT_REQUEST_ID);

        // even on being flushed multiple times
        dispatcher.flush(CONSTANT_REQUEST_ID_1);
        dispatcher.flush(CONSTANT_REQUEST_ID_2);
        dispatcher.flush(CONSTANT_REQUEST_ID);
    }

    @SmallTest
    public void testNoDispatcherProvided() {
        final AggregatedDispatcher dispatcher = new AggregatedDispatcher(null);
        // No dispatcher provided is ok
        dispatcher.flush(CONSTANT_REQUEST_ID);

        // and multiple flushes should do nothing
        dispatcher.flush(CONSTANT_REQUEST_ID_1);
        dispatcher.flush(CONSTANT_REQUEST_ID_2);
        dispatcher.flush(CONSTANT_REQUEST_ID);
    }

    @SmallTest
    public void testOneEventOneRequestId() throws MalformedURLException {
        final AggregatedTelemetryTestClass aggregated = new AggregatedTelemetryTestClass();
        final AggregatedDispatcher dispatcher = new AggregatedDispatcher(aggregated);
        final HttpEvent httpEvent = new HttpEvent(EventStrings.HTTP_EVENT);
        httpEvent.setHttpPath(new URL("https://contoso.com"));
        httpEvent.setOauthErrorCode("interaction_required");

        dispatcher.receive(CONSTANT_REQUEST_ID, httpEvent);

        dispatcher.flush(CONSTANT_REQUEST_ID);

        assertTrue(aggregated.eventsReceived());
        assertTrue(aggregated.checkOauthError());

        // after the flush there should be no more objects to be dispatched
        assertTrue(dispatcher.getObjectsToBeDispatched().isEmpty());
    }

    @SmallTest
    public void testOneEventMultipleRequestId() throws MalformedURLException {
        final AggregatedTelemetryTestClass aggregated = new AggregatedTelemetryTestClass();
        final AggregatedDispatcher dispatcher = new AggregatedDispatcher(aggregated);
        final HttpEvent httpEvent = new HttpEvent(EventStrings.HTTP_EVENT);
        httpEvent.setHttpPath(new URL("https://contoso.com"));
        httpEvent.setOauthErrorCode("interaction_required");

        dispatcher.receive(CONSTANT_REQUEST_ID, httpEvent);
        dispatcher.receive(CONSTANT_REQUEST_ID_1, httpEvent);

        dispatcher.flush(CONSTANT_REQUEST_ID);

        assertTrue(aggregated.eventsReceived());
        assertTrue(aggregated.checkOauthError());

        // after the flush there should be no more objects to be dispatched
        assertFalse(dispatcher.getObjectsToBeDispatched().isEmpty());
    }
}
