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

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


class DefaultEvent implements IEvents {
    private final List<Pair<String, String>> mEventList;

    private static String sApplicationName = null;

    private static String sApplicationVersion = null;

    private static String sClientId = null;

    private static String sClientIp = null;

    private static String sDeviceId = null;

    private static final int DEFAULT_EVENT_COUNT = 5;

    DefaultEvent() {
        mEventList = new ArrayList<>();

        mEventList.add(new Pair<>(EventStrings.CORRELATION_ID, sApplicationName));
        mEventList.add(new Pair<>(EventStrings.CORRELATION_ID, sApplicationVersion));
        mEventList.add(new Pair<>(EventStrings.CORRELATION_ID, sClientId));
        mEventList.add(new Pair<>(EventStrings.CORRELATION_ID, sClientIp));
        mEventList.add(new Pair<>(EventStrings.DEVICE_ID, sDeviceId));
    }

    static int getDefaultEventCount() {
        return DEFAULT_EVENT_COUNT;
    }

    @Override
    public void setEvent(final String name, final String value) {
        if (!TextUtils.isEmpty(name)) {
            mEventList.add(Pair.create(name, value));
        }
    }

    @Override
    public List<Pair<String, String>> getEvents() {
        return Collections.unmodifiableList(mEventList);
    }

    void setDefaults(final Context context, final String clientId) {
        sClientId = clientId;
        sApplicationName = context.getPackageName();
        try {
            sApplicationVersion = context.getPackageManager().getPackageInfo(sApplicationName, 0).versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            sApplicationVersion = "0.0.0.0";
        }
        //TODO: Getting IP will require network permissions do we want to do it?
        sClientIp = "0.0.0.0";

        //sDeviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }

    void setCorrelationId(final String correlationId) {
        mEventList.add(new Pair<>(EventStrings.CORRELATION_ID, correlationId));
    }

    void setRequestId(final String requestId) {
        mEventList.add(new Pair<>(EventStrings.REQUEST_ID, requestId));
    }

    List<Pair<String, String>> getEventList() {
        return mEventList;
    }
}