/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Set;

import com.microsoft.adal.ADALError;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.IWebRequestHandler;
 

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

        @SuppressWarnings("unchecked")
        Set<String> validHosts = (Set<String>)ReflectionUtils.getFieldValue(discovery,
                "mValidHosts");
        assertTrue("host is in the list", validHosts.contains("login.somewhere.com"));
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
        assertFalse("Instance should not be valid without tenant info", response.result);
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

    private IWebRequestHandler getMockRequest(String json, int statusCode){
        MockWebRequestHandler mockWebRequest = new MockWebRequestHandler();
       
        mockWebRequest.setReturnResponse(new HttpWebResponse(statusCode, json.getBytes(Charset
                .defaultCharset()), null));
        return mockWebRequest;
    }
    
    public void testServerInvalidJsonResponse() throws ClassNotFoundException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, MalformedURLException{
        Object discovery = getDiscoveryInstance();
        ReflectionUtils.setFieldValue(discovery,
                "mWebrequestHandler", getMockRequest("{invalidJson}", 200)); 
        final TestResponse response = new TestResponse();
        final URL endpointFull = new URL("https://login.invalidlogin.net/common/oauth2/authorize");
        TestLogResponse logTrack = new TestLogResponse();
        logTrack.listenForLogMessage("Json parsing error", null);
        callIsValidAuthority(discovery, endpointFull, response, true);
        
        assertNull("Exception should not throw", response.exception);
        assertFalse("not valid instance", response.result);
        assertTrue("Exception msg is logged", logTrack.message.equals("Json parsing error"));
    }
    
    public void testIsValidAuthorityNegative_InvalidUrl() throws MalformedURLException,
            IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
            ClassNotFoundException, InstantiationException, InvocationTargetException {
        final TestResponse response = new TestResponse();
        Object discovery = getDiscoveryInstance();
        final URL endpointFull = new URL("http://login.windows.net/common");
        callIsValidAuthority(discovery, endpointFull, response, true);

        assertNotNull("response should not be null", response);
        assertFalse("Instance should be invalid since http", response.result);
        assertNull("It should not have exception", response.exception);

        final TestResponse responseQueryParams = new TestResponse();
        final URL endpointQueryParams = new URL(
                "https://login.windows.net/common?resource=2343&client_id=234");
        callIsValidAuthority(discovery, endpointQueryParams, responseQueryParams, true);

        assertNotNull("response should not be null", responseQueryParams);
        assertFalse("Instance should be invalid", responseQueryParams.result);
        assertNull("It should not have exception", responseQueryParams.exception);

        final TestResponse responseFragment = new TestResponse();
        final URL endpointFragment = new URL("https://login.windows.net/common#token=23434");
        callIsValidAuthority(discovery, endpointFragment, responseFragment, true);

        assertNotNull("response should not be null", responseFragment);
        assertFalse("Instance should be invalid", responseFragment.result);
        assertNull("It should not have exception", responseFragment.exception);

        final TestResponse responseAdfs = new TestResponse();
        final URL endpointAdfs = new URL("https://fs.ade2eadfs30.com/adfs");
        callIsValidAuthority(discovery, endpointAdfs, responseAdfs, true);

        assertNotNull("response should not be null", responseAdfs);
        assertNotNull("It should have exception", responseAdfs.exception.getCause().getMessage()
                .equals(ADALError.DISCOVERY_NOT_SUPPORTED.getDescription()));

        final TestResponse responseInvalidPath = new TestResponse();
        final URL endpointInvalidPath = new URL("https://login.windows.net/common/test/test");
        callIsValidAuthority(discovery, endpointInvalidPath, responseInvalidPath, true);

        assertNotNull("response should not be null", responseInvalidPath);
        assertTrue("Instance should be valid. Endpoints will be added.", responseInvalidPath.result);
        assertNull("It should not have exception", responseInvalidPath.exception);
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
    @SuppressWarnings("unchecked")
    public void testIsValidAuthorityPositiveRequeryInList() throws MalformedURLException,
            IllegalArgumentException, NoSuchMethodException, IllegalAccessException,
            ClassNotFoundException, InstantiationException, InvocationTargetException,
            NoSuchFieldException {
        final TestResponse response = new TestResponse();
        Object discovery = getDiscoveryInstance();
        final URL endpointFull = new URL("https://login.windows-ppe.net/common");
        callIsValidAuthority(discovery, endpointFull, response, true);

        assertNull("It should not have exception", response.exception);
        assertTrue("Instance should be valid", response.result);

        // case sensitivity check
        final URL endpointCaseDifferent = new URL("https://logiN.Windows-PPE.Net/Common");
        callIsValidAuthority(discovery, endpointCaseDifferent, response, true);

        assertNull("It should not have exception", response.exception);
        assertTrue("Instance should be valid", response.result);

        // it should be in the list
        Set<String> validHosts = (Set<String>)ReflectionUtils.getFieldValue(discovery,
                "mValidHosts");
        assertTrue("added new host in the list", validHosts.size() == 4);
        assertTrue("has new host in the list to skip query",
                validHosts.contains("login.windows-ppe.net"));

        // add different host directly to validated host list and it should
        // return true without actual instance query
        validHosts.add("login.test-direct-add.net");
        final URL endpointTest = new URL("https://login.test-direct-add.net/common");
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
        final Method m = c.getDeclaredMethod(methodName, URL.class);
        try {
            // callback needs to signal for completion to test to the
            // end
            response.result = (Boolean)m.invoke(discovery, endpoint);
        } catch (Exception e) {
            response.exception = e;
        }
    }

    private Object getDiscoveryInstance() throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException {

        // Full package name
        Class<?> c = Class.forName("com.microsoft.adal.Discovery");
        Constructor<?> constructor = c.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object o = constructor.newInstance((Object[])null);

        return o;
    }
}
