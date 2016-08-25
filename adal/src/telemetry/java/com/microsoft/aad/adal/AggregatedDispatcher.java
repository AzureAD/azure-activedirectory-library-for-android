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

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AggregatedDispatcher extends DefaultDispatcher {

    AggregatedDispatcher(final IDispatcher dispatcher) {
        super(dispatcher);
    }

    @SuppressWarnings("unchecked")
    synchronized void flush(final String requestId) {
        final int defaultEventCount = DefaultEvent.getDefaultEventCount();
        final List<Pair<String, String>> dispatchList = new ArrayList<>();
        if (getDispatcher() == null) {
            return;
        }

        final List<IEvents> events = getObjectsToBeDispatched().get(requestId);
        boolean first = true;
        for (final IEvents event : events) {
            if (first) {
                dispatchList.addAll(event.getEvents());
                first = false;
                continue;
            }

            dispatchList.addAll(event.getEvents().subList(defaultEventCount, event.getEvents().size()));
        }

        getDispatcher().dispatch(dispatchList);
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
