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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the default dispatcher, this class if used will send events as they happen to the registered callback.
 * The expectation is that the data will be correlated at the query.
 */
class DefaultDispatcher {
    private final Map<String, List<IEvents>> mObjectsToBeDispatched = new HashMap<>();
    private final IDispatcher mDispatcher;

    private DefaultDispatcher() {
        mDispatcher = null;
   }

    DefaultDispatcher(final IDispatcher dispatcher) {
        mDispatcher = dispatcher;
    }

    /**
     * Flush is intentionally blank here, events are dispatched as they are received.
     * @param requestId
     */
    synchronized void flush(final String requestId) {
    }

    void receive(final String requestId, final IEvents events) {
        if (mDispatcher == null) {
            return;
        }

        final Map<String, String> dispatchMap = new HashMap<>();
        final List<Pair<String, String>> eventList = events.getEvents();

        for (final Pair<String, String> event : eventList) {
            dispatchMap.put(event.first, event.second);
        }

        mDispatcher.dispatchEvent(dispatchMap);
    }

    IDispatcher getDispatcher() {
        return mDispatcher;
    }

    Map<String, List<IEvents>> getObjectsToBeDispatched() {
        return mObjectsToBeDispatched;
    }
}