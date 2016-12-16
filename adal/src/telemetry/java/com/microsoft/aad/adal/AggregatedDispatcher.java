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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class AggregatedDispatcher extends DefaultDispatcher {

    AggregatedDispatcher(final IDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * This class is invoked when the developer wants only one callback for telemetry per call to AcquireToken.
     * This class condenses multiple telemetry events to a single one.
     * A part of the work done in this class is to remove the duplicate event properties from the events which all
     * have the common fields like application_name et al from DefaultEvents class
     * @param requestId to be aggregated
     */
    @SuppressWarnings("unchecked")
    synchronized void flush(final String requestId) {
        final Map<String, String> dispatchMap = new HashMap<>();
        if (getDispatcher() == null) {
            return;
        }

        final List<IEvents> events = getObjectsToBeDispatched().remove(requestId);
        if (events == null || events.isEmpty()) {
            return;
        }

        for (int i = 0; i < events.size(); i++) {
            IEvents event = events.get(i);

            // The child class of IEvent that is received here will call its processEvent
            event.processEvent(dispatchMap);
        }

        getDispatcher().dispatchEvent(dispatchMap);
    }

    @SuppressWarnings("unchecked")
    void receive(final String requestId, final IEvents events) {
        List<IEvents> eventsList = getObjectsToBeDispatched().get(requestId);
        if (eventsList == null) {
            eventsList = new ArrayList<>();
        }

        eventsList.add(events);
        getObjectsToBeDispatched().put(requestId, eventsList);
    }
}
