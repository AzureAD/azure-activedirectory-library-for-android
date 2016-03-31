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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationSettings;

import junit.framework.Assert;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.test.InstrumentationTestCase;
import android.util.Base64;
import android.util.Log;

public class AndroidTestHelper extends InstrumentationTestCase {

    protected final static int REQUEST_TIME_OUT = 40000; // miliseconds

    /** The Constant ENCODING_UTF8. */
    public static final String ENCODING_UTF8 = "UTF_8";

    private static final String TAG = "AndroidTestHelper";

    protected byte[] testSignature = null;

    protected String testTag = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getInstrumentation().getTargetContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext()
                .getCacheDir().getPath());

        // ADAL is set to this signature for now
        PackageInfo info = getInstrumentation().getContext().getPackageManager()
                .getPackageInfo("com.microsoft.aad.adal.testapp", PackageManager.GET_SIGNATURES);
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
        // AuthenticationSettings.INSTANCE.setDeviceCertificateProxy();
        Log.d(TAG, "testSignature is set");
    }

    public void assertThrowsException(final Class<? extends Exception> expected, String hasMessage,
                                      final ThrowableRunnable testCode) {
        try {
            testCode.run();
            Assert.fail("This is expecting an exception, but it was not thrown.");
        } catch (final Throwable result) {
            if (!expected.isInstance(result)) {
                Assert.fail("Exception was not correct");
            }

            if (hasMessage != null && !hasMessage.isEmpty()) {
                assertTrue("Message has the text",
                        (result.getMessage().toLowerCase(Locale.US).contains(hasMessage)));
            }
        }
    }

    public void assertThrowsException(final Class<? extends Exception> expected, String hasMessage,
            final Runnable testCode) {
        try {
            testCode.run();
            Assert.fail("This is expecting an exception, but it was not thrown.");
        } catch (final Throwable result) {
            if (!expected.isInstance(result)) {
                Assert.fail("Exception was not correct");
            }

            if (hasMessage != null && !hasMessage.isEmpty()) {
                assertTrue("Message has the text",
                        (result.getMessage().toLowerCase(Locale.US).contains(hasMessage)));
            }
        }
    }

    /**
     * just run tests and wait until finished
     * 
     * @param signal
     * @param testCode
     * @param runOnUI
     */
    public void testAsyncNoExceptionUIOption(final CountDownLatch signal, final Runnable testCode,
            boolean runOnUI) {

        Log.d(getName(), "thread:" + android.os.Process.myTid());

        try {
            if (runOnUI) {
                // run on UI thread to create async object at UI thread.
                // Background
                // work will happen in another thread.
                runTestOnUiThread(testCode);
            } else {
                testCode.run();
            }
        } catch (Throwable ex) {
            Log.e(getName(), ex.getMessage());
            assertFalse("not expected:" + ex.getMessage(), true);
            signal.countDown();
        }

        try {
            signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            assertFalse("Timeout " + getName(), true);
        }
    }

    public void testMultiThread(int activeThreads, final CountDownLatch signal,
            final Runnable runnable) {

        Log.d(getName(), "thread:" + android.os.Process.myTid());

        Thread[] threads = new Thread[activeThreads];

        for (int i = 0; i < activeThreads; i++) {
            Log.d(getName(), "Run shared cache test for thread:" + i);
            threads[i] = new Thread(runnable);
            threads[i].start();
        }

        try {
            signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            assertFalse("Timeout " + getName(), true);
        }
    }

    interface ThrowableRunnable
    {
        void run( ) throws Exception;
    }
}
