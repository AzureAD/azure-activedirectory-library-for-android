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

final class CacheEvent extends DefaultEvent {
    private final String mEventName;

    CacheEvent(final String eventName) {
        mEventName = eventName;
        setProperty(EventStrings.EVENT_NAME, eventName);
    }

    void setTokenType(final String tokenType) {
        getEventList().add(Pair.create(EventStrings.TOKEN_TYPE, tokenType));
    }

    void setTokenTypeRT(final boolean tokenTypeRT) {
        setProperty(EventStrings.TOKEN_TYPE_IS_RT, String.valueOf(tokenTypeRT));
    }

    void setTokenTypeMRRT(final boolean tokenTypeMRRT) {
        setProperty(EventStrings.TOKEN_TYPE_IS_MRRT, String.valueOf(tokenTypeMRRT));
    }

    void setTokenTypeFRT(final boolean tokenTypeFRT) {
        setProperty(EventStrings.TOKEN_TYPE_IS_FRT, String.valueOf(tokenTypeFRT));
    }

    /**
     * Each event chooses which of its members get picked on aggregation.
     * Cache event adds an event count field
     * @param dispatchMap the Map that is filled with the aggregated event properties
     */
    @Override
    public void processEvent(final Map<String, String> dispatchMap) {

        if (mEventName != EventStrings.TOKEN_CACHE_LOOKUP) {
            return;
        }

        final List<Pair<String, String>> eventList = getEventList();

        // We are keeping track of the number of Cache Events here, first time we insert the CACHE_EVENT_COUNT in the
        // map, next time onwards, we read the value of it and increment by one.
        final String count = dispatchMap.get(EventStrings.CACHE_EVENT_COUNT);
        if (count == null) {
            dispatchMap.put(EventStrings.CACHE_EVENT_COUNT, "1");
        } else {
            dispatchMap.put(EventStrings.CACHE_EVENT_COUNT,
                    Integer.toString(Integer.parseInt(count) + 1));
        }

        dispatchMap.put(EventStrings.TOKEN_TYPE_IS_FRT, "");
        dispatchMap.put(EventStrings.TOKEN_TYPE_IS_MRRT, "");
        dispatchMap.put(EventStrings.TOKEN_TYPE_IS_RT, "");

        for (Pair<String, String> eventPair : eventList) {
            final String name = eventPair.first;

            if (name.equals(EventStrings.TOKEN_TYPE_IS_FRT) || name.equals(EventStrings.TOKEN_TYPE_IS_RT)
                    || name.equals(EventStrings.TOKEN_TYPE_IS_MRRT)) {
                dispatchMap.put(name, eventPair.second);
            }
        }
    }
}
