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

final class UIEvent extends DefaultEvent {
    UIEvent(final String eventName) {
        getEventList().add(Pair.create(EventStrings.EVENT_NAME, eventName));
    }

    void setRedirectCount(final Integer redirectCount) {
        setProperty(EventStrings.REDIRECT_COUNT, redirectCount.toString());
    }

    void setNTLM(final boolean ntlm) {
        setProperty(EventStrings.NTLM, String.valueOf(ntlm));
    }

    void setUserCancel() {
        setProperty(EventStrings.USER_CANCEL, "true");
    }

    /**
     * Each event chooses which of its members get picked on aggregation.
     * UI event adds an event count field
     * @param dispatchMap the Map that is filled with the aggregated event properties
     */
    @Override
    public void processEvent(final Map<String, String> dispatchMap) {
        final List<Pair<String, String>> eventList = getEventList();

        // We are keeping track of the number of UI Events here, first time we insert the UI_EVENT_COUNT into the map
        // next time onwards, we read the value of it and increment by one.
        final String count = dispatchMap.get(EventStrings.UI_EVENT_COUNT);
        if (count == null) {
            dispatchMap.put(EventStrings.UI_EVENT_COUNT, "1");
        } else {
            dispatchMap.put(EventStrings.UI_EVENT_COUNT, Integer.toString(Integer.parseInt(count) + 1));
        }

        if (dispatchMap.containsKey(EventStrings.USER_CANCEL)) {
            dispatchMap.put(EventStrings.USER_CANCEL, "");
        }

        if (dispatchMap.containsKey(EventStrings.NTLM)) {
            dispatchMap.put(EventStrings.NTLM, "");
        }

        for (Pair<String, String> eventPair : eventList) {
            final String name = eventPair.first;

            if (name.equals(EventStrings.USER_CANCEL) || name.equals(EventStrings.NTLM)) {
                dispatchMap.put(name, eventPair.second);
            }
        }
    }
}
