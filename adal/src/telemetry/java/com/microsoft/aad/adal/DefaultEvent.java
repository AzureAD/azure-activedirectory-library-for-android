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

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


class DefaultEvent implements IEvents {
    private final List<Pair<String, String>> mEventList;

    private static String sApplicationName = null;

    private static String sApplicationVersion = "NA";

    private static String sClientId = "NA";

    private static String sDeviceId = "NA";

    private static final int EVENT_LIST_SIZE = 30;

    private String mRequestId;

    private int mDefaultEventCount;

    DefaultEvent() {
        mEventList = new ArrayList<>(EVENT_LIST_SIZE);

        // Keying off Application name not being null to decide if the defaults have been set
        if (sApplicationName != null) {
            setProperty(EventStrings.APPLICATION_NAME, sApplicationName);
            setProperty(EventStrings.APPLICATION_VERSION, sApplicationVersion);
            setProperty(EventStrings.CLIENT_ID, sClientId);
            setProperty(EventStrings.DEVICE_ID, sDeviceId);
            mDefaultEventCount = mEventList.size();
        }
    }

    @Override
    public int getDefaultEventCount() {
        return mDefaultEventCount;
    }

    @Override
    public void setProperty(final String name, final String value) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Telemetry setProperty on null name");
        }

        if (value == null || !isPrivacyCompliant(name)) {
            return;
        }

        mEventList.add(Pair.create(name, value));
    }

    @Override
    public List<Pair<String, String>> getEvents() {
        return Collections.unmodifiableList(mEventList);
    }

    /**
     * Each event chooses which of its members get picked on aggregation.
     * @param dispatchMap the Map that is filled with the aggregated event properties
     */
    @Override
    public void processEvent(final Map<String, String> dispatchMap) {
        if (sApplicationName != null && isPrivacyCompliant(EventStrings.APPLICATION_NAME)) {
            dispatchMap.put(EventStrings.APPLICATION_NAME, sApplicationName);
        }

        if (sApplicationVersion != null && isPrivacyCompliant(EventStrings.APPLICATION_VERSION)) {
            dispatchMap.put(EventStrings.APPLICATION_VERSION, sApplicationVersion);
        }

        if (sClientId != null && isPrivacyCompliant(EventStrings.CLIENT_ID)) {
            dispatchMap.put(EventStrings.CLIENT_ID, sClientId);
        }

        if (sDeviceId != null && isPrivacyCompliant(EventStrings.DEVICE_ID)) {
            dispatchMap.put(EventStrings.DEVICE_ID, sDeviceId);
        }
    }

    @SuppressLint("HardwareIds")
    void setDefaults(final Context context, final String clientId) {
        sClientId = clientId;
        sApplicationName = context.getPackageName();
        try {
            sApplicationVersion = context.getPackageManager().getPackageInfo(sApplicationName, 0).versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            sApplicationVersion = "NA";
        }

        try {
            sDeviceId = StringExtensions.createHash(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            sDeviceId = "";
        }

        if (mDefaultEventCount == 0) {
            setProperty(EventStrings.APPLICATION_NAME, sApplicationName);
            setProperty(EventStrings.APPLICATION_VERSION, sApplicationVersion);
            setProperty(EventStrings.CLIENT_ID, sClientId);
            setProperty(EventStrings.DEVICE_ID, sDeviceId);
            mDefaultEventCount = mEventList.size();
        }
    }

    // Sets the correlation id to the top of the list
    void setCorrelationId(final String correlationId) {
        mEventList.add(0, new Pair<>(EventStrings.CORRELATION_ID, correlationId));
        mDefaultEventCount++;
    }

    void setRequestId(final String requestId) {
        mRequestId = requestId;
        mEventList.add(0, new Pair<>(EventStrings.REQUEST_ID, requestId));
        mDefaultEventCount++;
    }

    List<Pair<String, String>> getEventList() {
        return mEventList;
    }

    String getTelemetryRequestId() {
        return mRequestId;
    }

    /**
     * Tests supplied EventStrings for privacy compliance.
     * @param fieldName The EventString to evaluate.
     * @return True, if the field can be reported. False otherwise.
     */
    static boolean isPrivacyCompliant(final String fieldName) {
        return Telemetry.getAllowPii() || !TelemetryUtils.GDPR_FILTERED_FIELDS.contains(fieldName);
    }
}