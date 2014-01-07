
package com.microsoft.adal.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import android.test.InstrumentationTestCase;
import android.util.Log;

public class AndroidTestHelper extends InstrumentationTestCase {

    protected final static int REQUEST_TIME_OUT = 20000; // miliseconds

    /** The Constant ENCODING_UTF8. */
    public static final String ENCODING_UTF8 = "UTF_8";

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
                        (result.getMessage().toLowerCase().contains(hasMessage)));
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
    
}
