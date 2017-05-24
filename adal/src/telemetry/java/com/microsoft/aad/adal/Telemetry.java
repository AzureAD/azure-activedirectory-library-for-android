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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Telemetry {
    private static final String TAG = Telemetry.class.getSimpleName();
    private DefaultDispatcher mDispatcher = null;
    private final Map<Pair<String, String>, String> mEventTracking = new ConcurrentHashMap<Pair<String, String>, String>();
    private static final Telemetry INSTANCE = new Telemetry();

    /**
     * Method to get the singleton instance of the Telemetry object.
     * @return Telemetry object
     */
    public static synchronized Telemetry getInstance() {
        return INSTANCE;
    }

    /**
     * registerDispatcher is called by the app to register their own implementation of IDispatcher.
     * If aggregation is required, a single call to IDispatcher.dispatch is made per call to acquireToken
     * If aggregation is not required, every event as it is fired is sent to the dispatcher.
     * The choice between aggregation required or not should be made based on what Telemetry system is being used and
     * whether its optimized to aggregate or handle large telemetry payloads.
     *
     * @param dispatcher the IDispatcher interface to be registered
     * @param aggregationRequired true if client wants a single event per call to AcquireToken, false otherwise
     */
    public synchronized void registerDispatcher(final IDispatcher dispatcher, final boolean aggregationRequired) {
        if (aggregationRequired) {
            mDispatcher = new AggregatedDispatcher(dispatcher);
        } else {
            mDispatcher = new DefaultDispatcher(dispatcher);
        }
    }

    static String registerNewRequest() {
        return UUID.randomUUID().toString();
    }

    void startEvent(final String requestId, final String eventName) {
        // We do not need to log if we do not have a dispatcher.
        if (mDispatcher == null) {
            return;
        }

        mEventTracking.put(new Pair<>(requestId, eventName), Long.toString(System.currentTimeMillis()));
    }

    void stopEvent(final String requestId, final IEvents events, final String eventName) {
        // We do not need to log if we do not have a dispatcher.
        if (mDispatcher == null) {
            return;
        }

        final String startTime = mEventTracking.get(new Pair<>(requestId, eventName));

        // If we did not get anything back from the dictionary, most likely its a bug that stopEvent was called without
        // a corresponding startEvent
        if (StringExtensions.isNullOrBlank(startTime)) {
            Logger.w(TAG, "Stop Event called without a corresponding start_event", "", null);
            return;
        }

        long startTimeLong = Long.parseLong(startTime);
        long stopTimeLong  = System.currentTimeMillis();
        long diffTime = stopTimeLong - startTimeLong;

        final String stopTime = Long.toString(stopTimeLong);

        events.setProperty(EventStrings.START_TIME, startTime);
        events.setProperty(EventStrings.STOP_TIME, stopTime);
        events.setProperty(EventStrings.RESPONSE_TIME, Long.toString(diffTime));

        mDispatcher.receive(requestId, events);
    }

    void flush(final String requestId) {
        if (mDispatcher != null) {
            mDispatcher.flush(requestId);
        }
    }
}
