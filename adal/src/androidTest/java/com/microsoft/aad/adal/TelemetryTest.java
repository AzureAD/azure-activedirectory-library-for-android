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

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class TelemetryTest {

    private static final String TAG = TelemetryTest.class.getSimpleName();

    @Before
    public void setUp() throws Exception {
        Logger.d(TAG, "setup key at settings");
        System.setProperty("dexmaker.dexcache", androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getCacheDir().getPath());
    }

    @Test
    public void testAggregatedDispatcher() throws PackageManager.NameNotFoundException {
        final TestDispatcher dispatch = new TestDispatcher();
        final AggregatedDispatcher dispatcher = new AggregatedDispatcher(dispatch);
        final FileMockContext mockContext = createMockContext();

        final DefaultEvent default1 = new DefaultEvent();
        default1.setDefaults(mockContext, "client-id");
        default1.setProperty("a", "a");

        final DefaultEvent default2 = new DefaultEvent();

        dispatcher.receive("1", default1);
        dispatcher.receive("1", default2);

        dispatcher.flush("1");

        // We should not have any extra event over the default event
        assertTrue(default1.getDefaultEventCount() >= dispatch.getEventCount());
    }

    @Test
    public void testDefaultDispatcher() throws PackageManager.NameNotFoundException {
        final TestDispatcher dispatch = new TestDispatcher();
        final DefaultDispatcher dispatcher = new DefaultDispatcher(dispatch);
        final FileMockContext mockContext = createMockContext();

        final DefaultEvent default1 = new DefaultEvent();
        default1.setDefaults(mockContext, "client-id");
        default1.setProperty("a", "a");

        final DefaultEvent default2 = new DefaultEvent();

        dispatcher.receive("2", default1);
        assertEquals(default1.getEventList().size(), dispatch.getEventCount());

        dispatcher.receive("2", default2);
        assertEquals(default2.getEventList().size(), dispatch.getEventCount());
    }

    private PackageManager getMockedPackageManager() throws PackageManager.NameNotFoundException {
        final Signature mockedSignature = Mockito.mock(Signature.class);
        when(mockedSignature.toByteArray()).thenReturn(Base64.decode(
                Util.ENCODED_SIGNATURE, Base64.NO_WRAP));

        final PackageInfo mockedPackageInfo = new MockedPackageInfo(new Signature[]{mockedSignature});
        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);
        when(mockedPackageManager.getPackageInfo(Mockito.anyString(), anyInt())).thenReturn(mockedPackageInfo);

        // Mock intent query
        final List<ResolveInfo> activities = new ArrayList<>(1);
        activities.add(Mockito.mock(ResolveInfo.class));
        when(mockedPackageManager.queryIntentActivities(Mockito.any(Intent.class), anyInt()))
                .thenReturn(activities);

        return mockedPackageManager;
    }

    private FileMockContext createMockContext()
            throws PackageManager.NameNotFoundException {
        final FileMockContext mockContext = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());

        final PackageManager mockedPackageManager = getMockedPackageManager();
        mockContext.setMockedPackageManager(mockedPackageManager);

        return mockContext;
    }

    class TestDispatcher implements IDispatcher {
        private int mEventCount = 0;

        public void dispatchEvent(Map events) {
            mEventCount = events.size();
        }

        public int getEventCount() {
            return mEventCount;
        }
    }
}

class AggregatedTelemetryTestClass implements IDispatcher {

    private Map<String, String> mEventData;

    AggregatedTelemetryTestClass() {
        mEventData = new HashMap<>();
    }

    @Override
    public void dispatchEvent(Map<String, String> events) {
        mEventData.putAll(events);
    }

    boolean checkOauthError() {
        return mEventData.containsKey(EventStrings.OAUTH_ERROR_CODE);
    }

    boolean checkNoPIIPresent(final String piiKey, final String piiValue) {
        try {
            final String value = mEventData.get(piiKey);
            return null == value || value.equals(StringExtensions.createHash(piiValue));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return false;
        }
    }

    boolean eventsReceived() {
        return (mEventData.size() > 0);
    }

    boolean checkAPIId(String id) {
        return mEventData.get(EventStrings.API_ID).equals(id);
    }

    boolean checkCacheEventCount(String count) {
        return mEventData.get(EventStrings.CACHE_EVENT_COUNT).equals(count);
    }

    boolean checkAPISucceeded() {
        return mEventData.get(EventStrings.WAS_SUCCESSFUL).equals("true");
    }
}

class DefaultTelemetryTestClass implements IDispatcher {

    private List<EventBlocks> mEventData;

    DefaultTelemetryTestClass() {
        mEventData = new ArrayList<>();
    }

    @Override
    public void dispatchEvent(Map<String, String> events) {
        mEventData.add(new EventBlocks(events));
    }

    class EventBlocks {

        private Map<String, String> mEventData;

        EventBlocks(Map<String, String> eventProperties) {
            mEventData = eventProperties;
        }
    }

}