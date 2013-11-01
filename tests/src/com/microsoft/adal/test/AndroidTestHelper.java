
package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.WebRequestHandler;

import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;
import android.util.Log;
import junit.framework.Assert;

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
     * get non public method from class
     * 
     * @param foo
     * @param methodName
     * @return
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected Method getTestMethod(Object foo, final String methodName, Class<?>... paramtypes)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?> c = foo.getClass();
        Method m = c.getDeclaredMethod(methodName, paramtypes);
        m.setAccessible(true);
        return m;
    }

    /**
     * get non public instance for testing
     * 
     * @param name
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected Object getNonPublicInstance(String name) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        // full package name
        Class<?> c;

        c = Class.forName(name);

        // getConstructor() returns only public constructors,

        Constructor<?> constructor = c.getDeclaredConstructor();

        constructor.setAccessible(true);
        Object o = constructor.newInstance(null);

        return o;
    }

    /**
     * run the code at ui thread and wait until it is finished
     * 
     * @param signal Use this signal inside your callback.
     * @param testCode code that actually calls the method that needs ui thread
     *            and async handling
     */
    public void testAsyncNoException(final CountDownLatch signal, final Runnable testCode) {
        testAsyncNoExceptionUIOption(signal, testCode, true);
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
        } catch (Exception ex) {
            Log.e(getName(), ex.getMessage());
            assertFalse("not expected:" + ex.getMessage(), true);
            signal.countDown();
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

    public Object getFieldValue(Object object, String fieldName) throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = object.getClass().getDeclaredField("mValidHosts");
        f.setAccessible(true);
        return f.get(object);
    }
}
