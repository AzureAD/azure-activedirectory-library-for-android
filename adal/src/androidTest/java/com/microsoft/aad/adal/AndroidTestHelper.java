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
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import androidx.test.InstrumentationRegistry;
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.HttpUrlConnectionFactory;

import junit.framework.Assert;

import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AndroidTestHelper {

    protected static final int REQUEST_TIME_OUT = 40000; // milliseconds

    private static final String TAG = "AndroidTestHelper";

    private byte[] mTestSignature;

    private String mTestTag;

    @SuppressLint("PackageManagerGetSignatures")
    public void setUp() throws Exception {
        getInstrumentation().getTargetContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        // ADAL is set to this signature for now
        final Context context = getInstrumentation().getContext();
        PackageInfo info = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

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
        Logger.d(TAG, "mTestSignature is set");
    }

    public void tearDown() throws Exception {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
    }

    void assertThrowsException(final Class<? extends Exception> expected,
                               final String hasMessage,
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

    void assertThrowsException(final Class<? extends Exception> expected,
                               final String hasMessage,
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
    void testAsyncNoExceptionUIOption(final CountDownLatch signal,
                                      final Runnable testCode,
                                      boolean runOnUI) {
        Logger.d(TAG, "thread:" + android.os.Process.myTid());

        try {
            if (runOnUI) {
                // run on UI thread to create async object at UI thread.
                // Background
                // work will happen in another thread.
                InstrumentationRegistry.getInstrumentation().runOnMainSync(testCode);
            } else {
                testCode.run();
            }
        } catch (Throwable ex) {
            Logger.e(TAG, ex.getMessage(), ex);
            assertFalse("not expected:" + ex.getMessage(), true);
            signal.countDown();
        }

        try {
            signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            assertFalse("Timeout " + getClass().getName(), true);
        }
    }

    public void testMultiThread(int activeThreads,
                                final CountDownLatch signal,
                                final Runnable runnable) {
        Logger.d(TAG, "thread:" + android.os.Process.myTid());

        Thread[] threads = new Thread[activeThreads];

        for (int i = 0; i < activeThreads; i++) {
            Logger.d(TAG, "Run shared cache test for thread:" + i);
            threads[i] = new Thread(runnable);
            threads[i].start();
        }

        try {
            signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("Timeout " + getClass().getName());
        }
    }

    interface ThrowableRunnable {
        void run() throws Exception;
    }
}
