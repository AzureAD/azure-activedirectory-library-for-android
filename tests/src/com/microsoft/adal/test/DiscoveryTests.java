/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.test.UiThreadTest;
import android.util.Log;

import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationParameters;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Discovery class is not public, so it needs reflection to make a call to
 * non-public class in different package Valid call:
 * https://login.windows.net/common
 * /discovery/instance?api-version=1.0&authorization_endpoint
 * =https%3A%2F%2Flogin
 * .windows.net%2Faaltest.onmicrosoft.com%2Foauth2%2Fauthorize
 * 
 * @author omercan
 */
public class DiscoveryTests extends AndroidTestHelper {

    private static final String TAG = "DiscoveryTests";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // sts.login.windows-int.net
    public void testaddValidHostToList() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, MalformedURLException, NoSuchFieldException {
        final String methodName = "addValidHostToList";
        final Object discovery = getDiscoveryInstance();
        final Method m = discovery.getClass().getDeclaredMethod(methodName, URL.class);
        m.setAccessible(true);
        m.invoke(discovery, new URL("https://login.somewhere.com"));

        Set<String> validHosts = (Set<String>)getFieldValue(discovery, "mValidHosts");
        assertTrue("host is in the list", validHosts.contains("login.somewhere.com"));
    }

    public void testInstanceList() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, MalformedURLException, NoSuchFieldException {

        final Object discovery = getDiscoveryInstance();

        Set<String> validInstances = (Set<String>)getFieldValue(discovery, "mCloudInstances");
        assertTrue("host is in the list", validInstances.contains("login.windows.net"));
        assertEquals(3, validInstances.size());
    }

    /**
     * instance that is in the list with different path
     * 
     * @throws MalformedURLException
     * @throws IllegalArgumentException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws InvocationTargetException
     */
    public void testIsValidAuthorityPositiveInList() throws MalformedURLException,
            IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
            ClassNotFoundException, InstantiationException, InvocationTargetException {
        final TestResponse response = new TestResponse();
        Object discovery = getDiscoveryInstance();

        final URL endpointFull = new URL("https://login.windows.net/common/oauth2/authorize");
        callIsValidAuthority(discovery, endpointFull, response, true);

        assertNull("It should not have exception", response.exception);
        assertTrue("Instance should be valid", response.result);

        final URL endpointInstanceRight = new URL("https://login.windows.net/something/something");
        callIsValidAuthority(discovery, endpointInstanceRight, response, true);

        assertNull("It should not have exception", response.exception);
        assertTrue("Instance should be valid", response.result);

        final URL endpointInstanceOnly = new URL("https://login.windows.net");
        callIsValidAuthority(discovery, endpointInstanceOnly, response, true);

        assertNull("It should not have exception", response.exception);
        assertTrue("Instance should be valid", response.result);
    }

    public void testIsValidAuthorityNegative() throws MalformedURLException,
            IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
            ClassNotFoundException, InstantiationException, InvocationTargetException {
        final TestResponse response = new TestResponse();
        Object discovery = getDiscoveryInstance();
        final URL endpointFull = new URL("https://login.invalidlogin.net/common/oauth2/authorize");
        callIsValidAuthority(discovery, endpointFull, response, true);

        assertNotNull("response should not be null", response);
        assertNull("It should not have exception", response.exception);
        assertFalse("Instance should be invalid", response.result);

    }

    /**
     * call instance that is not in the hard coded list.
     * 
     * @throws MalformedURLException
     * @throws IllegalArgumentException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException
     */
    public void testIsValidAuthorityPositiveRequeryInList() throws MalformedURLException,
            IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
            ClassNotFoundException, InstantiationException, InvocationTargetException,
            NoSuchFieldException {
        final TestResponse response = new TestResponse();
        Object discovery = getDiscoveryInstance();
        final URL endpointFull = new URL("https://login.windows-ppe.net/common/oauth2/authorize");
        callIsValidAuthority(discovery, endpointFull, response, true);

        assertNull("It should not have exception", response.exception);
        assertTrue("Instance should be valid", response.result);

        // it should be in the list
        Set<String> validHosts = (Set<String>)getFieldValue(discovery, "mValidHosts");
        assertTrue("added new host in the list", validHosts.size() == 4);
        assertTrue("has new host in the list to skip query",
                validHosts.contains("login.windows-ppe.net"));

        // add different host directly to validated host list and it should
        // return true without actual instance query
        validHosts.add("login.test-direct-add.net");
        final URL endpointTest = new URL(
                "https://login.test-direct-add.net/common/oauth2/authorize");
        callIsValidAuthority(discovery, endpointTest, response, true);

        assertNull("It should not have exception", response.exception);
        assertTrue("Instance should be valid", response.result);
    }

    class TestResponse {
        Boolean result;

        Exception exception;
    }

    /**
     * setup call to test and send results for testing
     * 
     * @param endpoint
     * @param response
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws InvocationTargetException
     */
    private void callIsValidAuthority(final Object discovery, final URL endpoint,
            final TestResponse response, boolean runAtUIThread) throws NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException, ClassNotFoundException,
            InstantiationException, InvocationTargetException {
        final String methodName = "isValidAuthority";
        Class<?> c = discovery.getClass();
        final Method m = c.getDeclaredMethod(methodName, URL.class, AuthenticationCallback.class);
        final CountDownLatch signal = new CountDownLatch(1);

        testAsyncNoExceptionUIOption(signal, new Runnable() {

            @Override
            public void run() {
                try {
                    // callback needs to signal for completion to test to the
                    // end
                    m.invoke(discovery, endpoint, new AuthenticationCallback<Boolean>() {

                        @Override
                        public void onSuccess(Boolean result) {
                            response.result = result;
                            signal.countDown();
                        }

                        @Override
                        public void onError(Exception exc) {
                            response.exception = exc;
                            signal.countDown();
                        }
                    });
                } catch (IllegalArgumentException e) {
                    Assert.fail("IllegalArgumentException");
                    signal.countDown();
                } catch (IllegalAccessException e) {
                    Assert.fail("IllegalAccessException");
                    signal.countDown();
                } catch (InvocationTargetException e) {
                    Assert.fail("InvocationTargetException");
                    signal.countDown();
                }
            }
        }, runAtUIThread);
    }

    private Object getDiscoveryInstance() throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException {

        // Full package name
        Class<?> c = Class.forName("com.microsoft.adal.Discovery");
        Constructor<?> constructor = c.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object o = constructor.newInstance(null);

        return o;
    }

    private Object getDiscoveryInstance(Handler handler) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        // Full package name
        Class<?> c = Class.forName("com.microsoft.adal.Discovery");
        Constructor<?> constructor = c.getDeclaredConstructor(Handler.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(handler);

        return o;
    }
}
