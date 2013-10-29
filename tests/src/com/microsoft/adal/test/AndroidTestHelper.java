
package com.microsoft.adal.test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.WebRequestHandler;

import android.test.AndroidTestCase;
import junit.framework.Assert;

public class AndroidTestHelper extends AndroidTestCase {

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

            if (hasMessage != null && !hasMessage.isEmpty())
            {
                assertTrue("Message has the text",
                        (result.getMessage().toLowerCase().contains(hasMessage)));
            }
        }
    }

    public void testAsyncNoException(final CountDownLatch signal, final Runnable testCode) {
        try {
            testCode.run();

        } catch (Exception ex) {
            assertFalse("not expected", true);
            signal.countDown();
        }

        try {
            signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            assertFalse("InterruptedException is not expected", true);
        }
    }

}
