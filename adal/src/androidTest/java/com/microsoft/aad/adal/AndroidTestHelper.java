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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.test.InstrumentationTestCase;
import android.util.Base64;
import android.util.Log;

import junit.framework.Assert;

import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AndroidTestHelper extends InstrumentationTestCase {

    protected static final int REQUEST_TIME_OUT = 40000; // milliseconds

    private static final String TAG = "AndroidTestHelper";

    private byte[] mTestSignature;

    private String mTestTag;

    @Override
    @SuppressLint("PackageManagerGetSignatures")
    protected void setUp() throws Exception {
        super.setUp();
        getInstrumentation().getTargetContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        // ADAL is set to this signature for now
        PackageInfo info = getInstrumentation().getContext().getPackageManager()
                .getPackageInfo("com.microsoft.aad.adal.testapp", PackageManager.GET_SIGNATURES);
        for (Signature signature : info.signatures) {
            mTestSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(mTestSignature);
            mTestTag = Base64.encodeToString(md.digest(), Base64.DEFAULT);
            break;
        }
        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);
        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Log.d(TAG, "mTestSignature is set");
    }

    @Override
    protected void tearDown() throws Exception {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        super.tearDown();
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
                assertTrue("Message has the text " + result.getMessage(),
                        (result.getMessage().toLowerCase(Locale.US).contains(hasMessage.toLowerCase())));
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
                assertTrue("Message has the text", (result.getMessage().toLowerCase(Locale.US).contains(hasMessage)));
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
    public void testAsyncNoExceptionUIOption(final CountDownLatch signal, final Runnable testCode, boolean runOnUI) {

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

    public void testMultiThread(int activeThreads, final CountDownLatch signal, final Runnable runnable) {

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

    interface ThrowableRunnable {
        void run() throws Exception;
    }
}
