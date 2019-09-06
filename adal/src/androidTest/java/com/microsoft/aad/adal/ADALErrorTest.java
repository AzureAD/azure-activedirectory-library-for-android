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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ADALErrorTest {

    private static final String TAG = "ADALErrorTests";

    @Before
    @SuppressLint("PackageManagerGetSignatures")
    public void setUp() throws Exception {
        System.setProperty(
                "dexmaker.dexcache",
                androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext()
                        .getCacheDir()
                        .getPath()
        );

        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Logger.d(TAG, "mTestSignature is set");
    }

    @Test
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void testResourceOverwrite() {
        ADALError err = ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED;
        String msg = err.getDescription();
        Logger.i(TAG, "", "Test context packagename:" + getInstrumentation().getTargetContext().getPackageName());
        Locale locale2 = new Locale("de");
        Locale.setDefault(locale2);
        Configuration config = new Configuration();
        config.setLocale(locale2);
        getInstrumentation().getContext().getResources().updateConfiguration(config,
                getInstrumentation().getContext().getResources().getDisplayMetrics());
        String localizedMsg = err.getLocalizedDescription(getInstrumentation().getContext());

        assertFalse("Error description is different in resource", msg.equalsIgnoreCase(localizedMsg));

        Locale localefr = new Locale("fr");
        Locale.setDefault(localefr);
        config.setLocale(localefr);
        getInstrumentation().getContext().getResources().updateConfiguration(config,
                getInstrumentation().getContext().getResources().getDisplayMetrics());
        localizedMsg = err.getLocalizedDescription(getInstrumentation().getContext());

        assertFalse("Same as english", msg.equalsIgnoreCase(localizedMsg));
        assertTrue("in default", localizedMsg.contains("Authority validation returned an error"));
    }
}
