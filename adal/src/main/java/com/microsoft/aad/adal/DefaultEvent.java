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

import android.text.TextUtils;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

interface EventsInterface {
    void setEvent(String name, String value);
    List<Pair<String,String>> getEvents();
    int getDefaultEventCount();
}

class DefaultEvent implements EventsInterface {
    List<Pair<String,String>> mEventList = null;
    int mDefaultEventCount = 0;

    DefaultEvent() {
        mEventList = new ArrayList<>();
    }

    @Override
    public void setEvent(String name, String value) {
        if (!TextUtils.isEmpty(name)) {
            mEventList.add(Pair.create(name, value));
        }
    }

    @Override
    public List<Pair<String, String>> getEvents() {
        return mEventList;
    }

    @Override
    public int getDefaultEventCount() {
        return mDefaultEventCount;
    }
}

class HttpEvent extends DefaultEvent{
    HttpEvent(final String eventName) {
        mEventList.add(Pair.create("Event", eventName));
    }
}

class UIEvent extends DefaultEvent{
    UIEvent(final String eventName) {
        mEventList.add(Pair.create("Event", eventName));
    }
}

class GrantEvent extends DefaultEvent{
    GrantEvent(final String eventName) {
        mEventList.add(Pair.create("Event", eventName));
    }
}

class CryptographyEvent extends DefaultEvent{
    CryptographyEvent(final String eventName) {
        mEventList.add(Pair.create("Event", eventName));
    }
}

class CacheEvent extends DefaultEvent{
    CacheEvent(final String eventName) {
        mEventList.add(Pair.create("Event", eventName));
    }
}

class BrokerEvent extends DefaultEvent{
    BrokerEvent(final String eventName) {
        mEventList.add(Pair.create("Event", eventName));
    }
}