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

import java.util.List;
import java.util.Map;

final class BrokerEvent extends DefaultEvent {
    BrokerEvent(final String eventName) {
        setProperty(EventStrings.EVENT_NAME, eventName);
    }

    void setBrokerAppName(final String brokerAppName) {
        setProperty(EventStrings.BROKER_APP, brokerAppName);
    }

    void setBrokerAppVersion(final String brokerAppVersion) {
        setProperty(EventStrings.BROKER_VERSION, brokerAppVersion);
    }

    /**
     * Each event chooses which of its members get picked on aggregation.
     * @param dispatchMap the Map that is filled with the aggregated event properties
     */
    @Override
    public void processEvent(final Map<String, String> dispatchMap) {
        final List<Pair<String, String>> eventList = getEventList();

        dispatchMap.put(EventStrings.BROKER_APP_USED, "true");
        for (Pair<String, String> eventPair : eventList) {
            final String name = eventPair.first;

            if (name.equals(EventStrings.BROKER_APP) || name.equals(EventStrings.BROKER_VERSION)) {
                dispatchMap.put(name, eventPair.second);
            }
        }
    }
}