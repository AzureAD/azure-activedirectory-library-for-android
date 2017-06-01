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
import java.util.concurrent.ConcurrentHashMap;

public class Telemetry {
    private DefaultDispatcher mDispatcher;
    private final Map<Pair<String, String>, String> mEventTracking = new ConcurrentHashMap<Pair<String, String>, String>();
    private final static Telemetry sInstance = new Telemetry();

    public static synchronized Telemetry getInstance() {
        return sInstance;
    }

    public void registerDispatcher(IDispatcher dispatcher, boolean aggregationRequired) {
        if (aggregationRequired) {
            mDispatcher = new AggregatedDispatcher(dispatcher);
        } else {
            mDispatcher = new DefaultDispatcher(dispatcher);
        }
    }

    String registerNewRequest() {
        //TODO:
        return "";
    }

    void startEvent(final String requestId, final String eventName) {

    }

    void stopEvent(final String requestId, final IEvents events) {

    }
}
