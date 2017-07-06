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
import android.test.InstrumentationTestCase;
import android.util.Log;

import java.util.Locale;

public class ADALErrorTest extends InstrumentationTestCase {

    private static final String TAG = "ADALErrorTests";

    @Override
    @SuppressLint("PackageManagerGetSignatures")
    protected void setUp() throws Exception {
        super.setUp();
        getInstrumentation().getTargetContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Log.d(TAG, "mTestSignature is set");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void testResourceOverwrite() {
        ADALError err = ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED;
        String msg = err.getDescription();
        Log.v(TAG, "Test context packagename:" + getInstrumentation().getTargetContext().getPackageName());
        Locale locale2 = new Locale("de");
        Locale.setDefault(locale2);
        Configuration config = new Configuration();
        config.setLocale(locale2);
        getInstrumentation().getContext().getResources().updateConfiguration(config,
                getInstrumentation().getContext().getResources().getDisplayMetrics());
        String localizedMsg = err.getLocalizedDescription(getInstrumentation().getContext());

        assertFalse("Error decription is different in resource", msg.equalsIgnoreCase(localizedMsg));

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
