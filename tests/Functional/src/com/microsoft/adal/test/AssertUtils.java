package com.microsoft.adal.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import junit.framework.Assert;

public class AssertUtils extends Assert {

    private static final String TAG = "AssertUtils";
    protected final static int REQUEST_TIME_OUT = 20000; // miliseconds
    
    public static void assertThrowsException(final Class<? extends Exception> expected, String hasMessage,
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
                        (result.getMessage().toLowerCase().contains(hasMessage)));
            }
        }
    }
    
    public static void assertAsync(final CountDownLatch signal, final Runnable testCode) {

        Log.d(TAG, "Thread:" + android.os.Process.myTid());

        try {             
                testCode.run();
        } catch (Throwable ex) {
            Log.e(TAG, ex.getMessage());
            Assert.fail("not expected:" + ex.getMessage());
            signal.countDown();
        }

        try {
            signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail("not expected:" + e.getMessage());
        }
    }
}
