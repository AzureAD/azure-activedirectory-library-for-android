// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import java.security.MessageDigest;
import java.util.Locale;

import android.annotation.TargetApi;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.os.Build;
import android.test.InstrumentationTestCase;
import android.util.Base64;
import android.util.Log;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationSettings;

public class ADALErrorTest extends InstrumentationTestCase {

    private static final String TAG = "ADALErrorTests";

    private byte[] testSignature = null;

    private String testTag = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getInstrumentation().getTargetContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext()
                .getCacheDir().getPath());

        // ADAL is set to this signature for now
        PackageInfo info = getInstrumentation().getContext().getPackageManager()
                .getPackageInfo("com.microsoft.aad.testapp", PackageManager.GET_SIGNATURES);
        for (Signature signature : info.signatures) {
            testSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(testSignature);
            testTag = Base64.encodeToString(md.digest(), Base64.DEFAULT);
            break;
        }
        AuthenticationSettings.INSTANCE.setBrokerSignature(testTag);
        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.PACKAGE_NAME);
        Log.d(TAG, "testSignature is set");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void testResourceOverwrite() {
        ADALError err = ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED;
        String msg = err.getDescription();
        Log.v(TAG, "Test context packagename:"
                + getInstrumentation().getTargetContext().getPackageName());
        Locale locale2 = new Locale("de");
        Locale.setDefault(locale2);
        Configuration config = new Configuration();
        config.setLocale(locale2);
        getInstrumentation()
                .getContext()
                .getResources()
                .updateConfiguration(config,
                        getInstrumentation().getContext().getResources().getDisplayMetrics());
        String localizedMsg = err.getLocalizedDescription(getInstrumentation().getContext());

        assertFalse("Error decription is different in resource", msg.equalsIgnoreCase(localizedMsg));

        Locale localefr = new Locale("fr");
        Locale.setDefault(localefr);
        config.setLocale(localefr);
        getInstrumentation()
                .getContext()
                .getResources()
                .updateConfiguration(config,
                        getInstrumentation().getContext().getResources().getDisplayMetrics());
        localizedMsg = err.getLocalizedDescription(getInstrumentation().getContext());

        assertFalse("Same as english", msg.equalsIgnoreCase(localizedMsg));
        assertTrue("in default",
                localizedMsg.contains("Authority validation returned an error"));
    }
}
