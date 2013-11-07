
package com.microsoft.adal.test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;
import android.test.mock.MockPackageManager;

import com.microsoft.adal.AuthenticationActivity;
import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationConstants;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationException;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.ErrorCodes.ADALError;
import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.IDiscovery;
import com.microsoft.adal.IWebRequestHandler;

public class AuthenticationContextTests extends AndroidTestCase {

    protected final static int CONTEXT_REQUEST_TIME_OUT = 20000;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * test constructor to make sure authority parameter is set
     */
    public void testConstructor() {
        String authority = "authority";
        AuthenticationContext context = new AuthenticationContext(getContext(), authority, false);
        assertSame(authority, context.getAuthority());
    }

    /**
     * if package does not have declaration for activity, it should return false
     * 
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public void testResolveIntent() throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException {
        TestMockContext mockContext = new TestMockContext(getContext());
        AuthenticationContext context = new AuthenticationContext(mockContext, "authority", false);
        Method m = ReflectionUtils.getTestMethod(context, "resolveIntent", Intent.class);
        Intent intent = new Intent();
        intent.setClass(mockContext, AuthenticationActivity.class);

        boolean actual = (Boolean)m.invoke(context, intent);
        assertTrue("Intent is expected to resolve", actual);

        mockContext.resolveIntent = false;
        actual = (Boolean)m.invoke(context, intent);
        assertFalse("Intent is not expected to resolve", actual);
    }

    /**
     * Test throws for different missing arguments
     */
    public void testAcquireTokenNegativeArguments() {
        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext, "authority",
                false);
        final MockActivity testActivity = new MockActivity();
        final MockAuthenticationCallback testEmptyCallback = new MockAuthenticationCallback();

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "callback",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(testActivity, "resource", "clientId", "redirectUri",
                                "userid", null);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "activity",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(null, "resource", "clientId", "redirectUri", "userid",
                                testEmptyCallback);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "resource",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(testActivity, null, "clientId", "redirectUri",
                                "userid", testEmptyCallback);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "clientId",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(testActivity, "resource", null, "redirectUri",
                                "userid", testEmptyCallback);
                    }
                });

    }

    /**
     * authority is malformed and error should come back in callback
     * 
     * @throws InterruptedException
     */
    public void testAcquireTokenAuthorityMalformed() throws InterruptedException {
        // Malformed url error will come back in callback
        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext, "authority",
                false);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        context.acquireToken(testActivity, "resource", null, "redirectUri", "userid", callback);

        callback.wait(CONTEXT_REQUEST_TIME_OUT);

        // check response in callback
        assertNotNull("Error is not null", callback.mException);
        assertEquals("NOT_VALID_URL", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL,
                ((AuthenticationException)callback.mException).getCode());
    }

    /**
     * authority is validated and intent start request is sent
     * 
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void testAcquireTokenValidateAuthorityReturnsValid() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                "https://login.windows.net/omercantest.onmicrosoft.com", true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockDiscovery discovery = new MockDiscovery(true);
        ReflectionUtils.setFieldValue(context, "mDiscovery", discovery);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                callback);
        callback.wait(CONTEXT_REQUEST_TIME_OUT);

        // check response in callback
        assertNull("Error is null", callback.mException);
        assertEquals("Activity was attempted to start with request code",
                AuthenticationConstants.UIRequest.BROWSER_FLOW,
                testActivity.mStartActivityRequestCode);
    }

    /**
     * Invalid authority returns
     * 
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void testAcquireTokenValidateAuthorityReturnsInValid() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                "https://login.windows.net/omercantest.onmicrosoft.com", true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockDiscovery discovery = new MockDiscovery(false);
        ReflectionUtils.setFieldValue(context, "mDiscovery", discovery);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                callback);
        callback.wait(CONTEXT_REQUEST_TIME_OUT);

        // check response in callback
        assertNotNull("Error is not null", callback.mException);
        assertEquals("NOT_VALID_URL", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE,
                ((AuthenticationException)callback.mException).getCode());
        assertTrue(
                "Activity was not attempted to start with request code",
                AuthenticationConstants.UIRequest.BROWSER_FLOW != testActivity.mStartActivityRequestCode);

    }

    /**
     * acquire token without validation
     * 
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void testAcquireTokenWithoutValidation() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                "https://login.windows.net/omercantest.onmicrosoft.com", false);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                callback);
        callback.wait(CONTEXT_REQUEST_TIME_OUT);

        // check response in callback
        assertNotNull("Error is not null", callback.mException);
        assertEquals("NOT_VALID_URL", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL,
                ((AuthenticationException)callback.mException).getCode());
        assertEquals("Activity was attempted to start with request code",
                AuthenticationConstants.UIRequest.BROWSER_FLOW,
                testActivity.mStartActivityRequestCode);
    }

    class MockDiscovery implements IDiscovery {

        private boolean isValid = false;

        private URL authorizationUrl;

        MockDiscovery(boolean validFlag) {
            isValid = validFlag;
        }

        @Override
        public void isValidAuthority(URL authorizationEndpoint,
                AuthenticationCallback<Boolean> callback) {
            authorizationUrl = authorizationEndpoint;
            callback.onSuccess(isValid);
        }
    }

    /**
     * handler to return mock responses
     * 
     * @author omercan
     */
    class MockWebRequestHandler implements IWebRequestHandler {

        private URL mRequestUrl;

        private HashMap<String, String> mRequestHeaders;

        private HttpWebResponse mReturnResponse;

        private Exception mReturnException;

        @Override
        public AsyncTask<?, ?, ?> sendAsyncGet(URL url, HashMap<String, String> headers,
                HttpWebRequestCallback callback) throws IllegalArgumentException, IOException {
            mRequestUrl = url;
            mRequestHeaders = headers;
            callback.onComplete(mReturnResponse, mReturnException);
            return null;
        }

        @Override
        public AsyncTask<?, ?, ?> sendAsyncDelete(URL url, HashMap<String, String> headers,
                HttpWebRequestCallback callback) throws IllegalArgumentException, IOException {
            mRequestUrl = url;
            mRequestHeaders = headers;
            callback.onComplete(mReturnResponse, mReturnException);
            return null;
        }

        @Override
        public AsyncTask<?, ?, ?> sendAsyncPut(URL url, HashMap<String, String> headers,
                byte[] content, String contentType, HttpWebRequestCallback callback)
                throws IllegalArgumentException, IOException {
            mRequestUrl = url;
            mRequestHeaders = headers;
            callback.onComplete(mReturnResponse, mReturnException);
            return null;
        }

        @Override
        public AsyncTask<?, ?, ?> sendAsyncPost(URL url, HashMap<String, String> headers,
                byte[] content, String contentType, HttpWebRequestCallback callback)
                throws IllegalArgumentException, IOException {
            mRequestUrl = url;
            mRequestHeaders = headers;
            callback.onComplete(mReturnResponse, mReturnException);
            return null;
        }
    }

    /**
     * callback to store responses
     * 
     * @author omercan
     */
    class MockAuthenticationCallback implements AuthenticationCallback<AuthenticationResult> {

        Exception mException = null;

        AuthenticationResult mResult = null;

        CountDownLatch mSignal;

        public MockAuthenticationCallback() {
            mSignal = null;
        }

        public MockAuthenticationCallback(final CountDownLatch signal) {
            mSignal = signal;
        }

        @Override
        public void onSuccess(AuthenticationResult result) {
            mResult = result;
            if (mSignal != null)
                mSignal.countDown();
        }

        @Override
        public void onError(Exception exc) {
            mException = exc;
            if (mSignal != null)
                mSignal.countDown();
        }
    };

    /**
     * Mock activity
     * 
     * @author omercan
     */
    class MockActivity extends Activity {

        int mStartActivityRequestCode = -123;

        Intent mStartActivityIntent;

        Bundle mStartActivityOptions;

        @Override
        public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
            mStartActivityIntent = intent;
            mStartActivityRequestCode = requestCode;
            mStartActivityOptions = options;
        }
    }

    class TestMockContext extends MockContext {

        private Context mContext;

        private static final String PREFIX = "test.";

        boolean resolveIntent = true;

        public TestMockContext(Context context) {
            mContext = context;
        }

        @Override
        public String getPackageName() {
            return PREFIX;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return mContext.getSharedPreferences(name, mode);
        }

        @Override
        public PackageManager getPackageManager() {
            return new TestPackageManager();
        }

        class TestPackageManager extends MockPackageManager {
            @Override
            public ResolveInfo resolveActivity(Intent intent, int flags) {
                if (resolveIntent)
                    return new ResolveInfo();

                return null;
            }
        }
    }

}
