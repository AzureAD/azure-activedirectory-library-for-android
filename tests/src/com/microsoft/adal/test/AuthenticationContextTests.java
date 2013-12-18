
package com.microsoft.adal.test;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.util.SparseArray;

import com.microsoft.adal.ADALError;
import com.microsoft.adal.AuthenticationActivity;
import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationException;
import com.microsoft.adal.CacheKey;
import com.microsoft.adal.DefaultTokenCacheStore;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.IDiscovery;
import com.microsoft.adal.ITokenCacheStore;
import com.microsoft.adal.PromptBehavior;
import com.microsoft.adal.TokenCacheItem;
import com.microsoft.adal.test.AuthenticationConstants.UIRequest;

public class AuthenticationContextTests extends AndroidTestCase {

    /**
     * check case-insensitive lookup
     */
    private static final String VALID_AUTHORITY = "https://Login.windows.net/Omercantest.Onmicrosoft.com";

    protected final static int CONTEXT_REQUEST_TIME_OUT = 20000;
 
    private final static String TEST_AUTHORITY = "http://login.windows.net/common";

    private static final String TAG = "AuthenticationContextTests";

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

    public void testConstructorNoCache() {
        String authority = "authority";
        AuthenticationContext context = new AuthenticationContext(getContext(), authority, false,
                null);
        assertNull(context.getCache());
    }

    public void testConstructorWithCache() {
        String authority = "authority";
        DefaultTokenCacheStore expected = new DefaultTokenCacheStore(getContext());
        AuthenticationContext context = new AuthenticationContext(getContext(), authority, false,
                expected);
        assertEquals("Cache object is expected to be same", expected, context.getCache());

        AuthenticationContext contextDefaultCache = new AuthenticationContext(getContext(),
                authority, false);
        assertNotNull(contextDefaultCache.getCache());
    }

    public void testConstructorValidateAuthority() {
        String authority = "authority";
        AuthenticationContext context = new AuthenticationContext(getContext(), authority, true);
        assertTrue("Validate flag is expected to be same", context.getValidateAuthority());

        context = new AuthenticationContext(getContext(), authority, false);
        assertFalse("Validate flag is expected to be same", context.getValidateAuthority());
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

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "resource",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(testActivity, "", "clientId", "redirectUri", "userid",
                                testEmptyCallback);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "clientid",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(testActivity, "resource", null, "redirectUri",
                                "userid", testEmptyCallback);
                    }
                });

        // userId is optional, it will throw error for invalid authority not for
        // missing userid
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                "extraquearyparam", testEmptyCallback);
        assertTrue("Authority parameter error",
                testEmptyCallback.mException instanceof AuthenticationException);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "", "",
                testEmptyCallback);
        assertTrue("Authority parameter error",
                testEmptyCallback.mException instanceof AuthenticationException);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri",
                PromptBehavior.Always, testEmptyCallback);
        assertTrue("Authority parameter error",
                testEmptyCallback.mException instanceof AuthenticationException);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri",
                PromptBehavior.Auto, testEmptyCallback);
        assertTrue("Authority parameter error",
                testEmptyCallback.mException instanceof AuthenticationException);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri",
                PromptBehavior.Never, testEmptyCallback);
        assertTrue("Authority parameter error",
                testEmptyCallback.mException instanceof AuthenticationException);
    }

    public void testAcquireToken_userId() throws ClassNotFoundException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException{
        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                "https://login.windows.net/common", false);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        context.acquireToken(testActivity, "resource56", "clientId345", "redirect123", "userid123", callback);
        
        // verify request
        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertEquals("AuthenticationRequest inside the intent", request.getClass(),
                Class.forName("com.microsoft.adal.AuthenticationRequest"));
        String redirect = (String)ReflectionUtils.getFieldValue(request, "mRedirectUri");
        assertEquals("Redirect uri is same as package", "redirect123", redirect);
        String loginHint = (String)ReflectionUtils.getFieldValue(request, "mLoginHint");
        assertEquals("login hint same as userid", "userid123", loginHint);
        String client = (String)ReflectionUtils.getFieldValue(request, "mClientId");
        assertEquals("client hint same as userid", "clientId345", client);
        String authority = (String)ReflectionUtils.getFieldValue(request, "mAuthority");
        assertEquals("client hint same as userid", "https://login.windows.net/common", authority);
        String resource = (String)ReflectionUtils.getFieldValue(request, "mResource");
        assertEquals("client hint same as userid", "resource56", resource);        
    }
    
    public void testEmptyRedirect() throws ClassNotFoundException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException {
        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                "https://login.windows.net/common", false);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        context.acquireToken(testActivity, "resource", "clientId", "", "userid", callback);

        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertEquals("AuthenticationRequest inside the intent", request.getClass(),
                Class.forName("com.microsoft.adal.AuthenticationRequest"));
        String redirect = (String)ReflectionUtils.getFieldValue(request, "mRedirectUri");
        assertEquals("Redirect uri is same as package", "com.microsoft.adal.testapp", redirect);
    }

    public void testPrompt() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException {
        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                "https://login.windows.net/common", false);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        // 1 - Send prompt always
        context.acquireToken(testActivity, "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.Always, callback);

        // get intent from activity to verify extraparams are send
        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

        PromptBehavior prompt = (PromptBehavior)ReflectionUtils.getFieldValue(request, "mPrompt");
        assertEquals("Prompt param is same", PromptBehavior.Always, prompt);
    }

    public void testExtraParams() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException {
        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                "https://login.windows.net/common", false);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        String expected = "&extraParam=1";
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        // 1 - Send extra param
        context.acquireToken(testActivity, "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.Always, expected, callback);

        // get intent from activity to verify extraparams are send
        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

        assertEquals("AuthenticationRequest inside the intent", request.getClass(),
                Class.forName("com.microsoft.adal.AuthenticationRequest"));
        String extraparm = (String)ReflectionUtils.getFieldValue(request,
                "mExtraQueryParamsAuthentication");
        assertEquals("Extra query param is same", expected, extraparm);

        // 2- Don't send extraqueryparam
        ReflectionUtils.setFieldValue(context, "mAuthorizationCallback", null);
        context.acquireToken(testActivity, "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.Always, null, callback);

        // verify from mocked activity intent
        intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        request = intent.getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

        assertEquals("AuthenticationRequest inside the intent", request.getClass(),
                Class.forName("com.microsoft.adal.AuthenticationRequest"));
        extraparm = (String)ReflectionUtils.getFieldValue(request,
                "mExtraQueryParamsAuthentication");
        assertNull("Extra query param is null", extraparm);
    }

    public static Object createAuthenticationRequest(String authority, String resource,
            String client, String redirect, String loginhint) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.adal.AuthenticationRequest");

        Constructor<?> constructor = c.getDeclaredConstructor(String.class, String.class,
                String.class, String.class, String.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authority, resource, client, redirect, loginhint);
        return o;
    }

    /**
     * Test throws for different missing arguments
     */
    public void testAcquireTokenByRefreshTokenNegativeArguments() {
        TestMockContext mockContext = new TestMockContext(getContext());
        final AuthenticationContext contextAuthorityNull = new AuthenticationContext(mockContext,
                null, false);
        AssertUtils.assertThrowsException(IllegalArgumentException.class, "authority",
                new Runnable() {

                    @Override
                    public void run() {
                        contextAuthorityNull.acquireTokenByRefreshToken("refresh", "clientId",
                                "resource", null);
                    }
                });

        // other method
        AssertUtils.assertThrowsException(IllegalArgumentException.class, "authority",
                new Runnable() {

                    @Override
                    public void run() {
                        contextAuthorityNull
                                .acquireTokenByRefreshToken("refresh", "clientId", null);
                    }
                });

        final AuthenticationContext context = new AuthenticationContext(mockContext, "authority",
                false);
        final MockAuthenticationCallback mockCallback = new MockAuthenticationCallback();

        // null callback
        AssertUtils.assertThrowsException(IllegalArgumentException.class, "callback",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireTokenByRefreshToken("refresh", "clientId", "resource", null);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "callback",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireTokenByRefreshToken("refresh", "clientId", null);
                    }
                });

        // null refresh token
        AssertUtils.assertThrowsException(IllegalArgumentException.class, "refresh",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireTokenByRefreshToken(null, "clientId", "resource",
                                mockCallback);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "refresh",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireTokenByRefreshToken(null, "clientId", mockCallback);
                    }
                });

        // null clientiD
        AssertUtils.assertThrowsException(IllegalArgumentException.class, "clientid",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireTokenByRefreshToken("refresh", null, "resource",
                                mockCallback);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "clientid",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireTokenByRefreshToken("refresh", null, mockCallback);
                    }
                });
    }

    /**
     * Test throws for different missing arguments
     * 
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public void testAcquireTokenByRefreshTokenPositive() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException {
        TestMockContext mockContext = new TestMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken();

        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        String expectedAccessToken = "TokenFortestAcquireToken"+ UUID.randomUUID().toString();
        String expectedClientId = "client" + UUID.randomUUID().toString();
        String exptedResource = "resource" + UUID.randomUUID().toString();
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        
        MockWebRequestHandler mockWebRequest = setMockWebRequest(context, expectedAccessToken);
        
        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, callback);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", expectedAccessToken,
                callback.mResult.getAccessToken());
        assertEquals("Same refresh token", "refreshToken=", callback.mResult.getRefreshToken());
        assertTrue("Content has client in the message", mockWebRequest.getRequestContent()
                .contains(expectedClientId));
        assertFalse("Content does not have resource in the message", mockWebRequest
                .getRequestContent().contains(exptedResource));

        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, exptedResource,
                callback);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", expectedAccessToken,
                callback.mResult.getAccessToken());
        assertEquals("Same refresh token", "refreshToken=", callback.mResult.getRefreshToken());
        assertTrue("Content has client in the message", mockWebRequest.getRequestContent()
                .contains(expectedClientId));
        assertTrue("Content has resource in the message", mockWebRequest.getRequestContent()
                .contains(exptedResource));
    }

    private MockWebRequestHandler setMockWebRequest(final AuthenticationContext context,
            String expectedAccessToken) throws NoSuchFieldException, IllegalAccessException {
        MockWebRequestHandler mockWebRequest = new MockWebRequestHandler();
        String json = "{\"access_token\":\""+expectedAccessToken+"\",\"token_type\":\"Bearer\",\"expires_in\":\"29344\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshToken=\",\"scope\":\"*\"}";
        mockWebRequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", mockWebRequest);
        return mockWebRequest;
    }
    
    private void removeMockWebRequest(final AuthenticationContext context)throws NoSuchFieldException, IllegalAccessException{
        ReflectionUtils.setFieldValue(context, "mWebRequest", null);
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

        context.acquireToken(testActivity, "resource", "clientId", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
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
        AuthenticationContext context = new AuthenticationContext(mockContext, VALID_AUTHORITY,
                true);
        final CountDownLatch signal = new CountDownLatch(1);
        MockActivity testActivity = new MockActivity();
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockDiscovery discovery = new MockDiscovery(true);
        ReflectionUtils.setFieldValue(context, "mDiscovery", discovery);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
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
                VALID_AUTHORITY, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockDiscovery discovery = new MockDiscovery(false);
        ReflectionUtils.setFieldValue(context, "mDiscovery", discovery);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
        assertNotNull("Error is not null", callback.mException);
        assertEquals("NOT_VALID_URL", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE,
                ((AuthenticationException)callback.mException).getCode());
        assertTrue(
                "Activity was not attempted to start with request code",
                AuthenticationConstants.UIRequest.BROWSER_FLOW != testActivity.mStartActivityRequestCode);
        clearCache(context);
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
                VALID_AUTHORITY, false);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
        assertNull("Error is null", callback.mException);
        assertEquals("Activity was attempted to start with request code",
                AuthenticationConstants.UIRequest.BROWSER_FLOW,
                testActivity.mStartActivityRequestCode);
        clearCache(context);
    }

    /**
     * acquire token uses refresh token, but web request returns error
     * 
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public void testRefreshTokenWebRequestHasError() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InvocationTargetException {

        TestMockContext mockContext = new TestMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken();
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        webrequest.setReturnResponse(new HttpWebResponse(503, null, null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);

        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
        assertNotNull("Error is not null", callback.mException);
        assertNull("Cache is empty for this item", mockCache.getItem(CacheKey.createCacheKey(
                VALID_AUTHORITY, "resource", "clientId", false, "userid")));
        assertNull("Cache is empty for this item", mockCache.getItem(CacheKey.createCacheKey(
                VALID_AUTHORITY, "resource", "clientId", true, "userid")));
        clearCache(context);
    }

    /**
     * acquire token using refresh token. All web calls are mocked. Refresh
     * token response must match to result and cache.
     * 
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public void testRefreshTokenPositive() throws InterruptedException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException {

        TestMockContext mockContext = new TestMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken();

        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String json = "{\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);

        // call acquire token which will try refresh token based on cache
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // check response in callback
        assertNull("Error is null", callback.mException);
        assertEquals("Token is same", "TokenFortestRefreshTokenPositive", callback.mResult.getAccessToken());
        assertNotNull("Cache is NOT empty for this userid for regular token", mockCache.getItem(CacheKey.createCacheKey(
                VALID_AUTHORITY, "resource", "clientId", false, "userid")));
        assertNull("Cache is empty for multiresource token", mockCache.getItem(CacheKey.createCacheKey(
                VALID_AUTHORITY, "resource", "clientId", true, "userid")));
        clearCache(context);
    }

    /**
     * authority and resource are case insensitive. Cache lookup will return
     * item from cache.
     * 
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void testAcquireTokenCacheLookup() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

        TestMockContext mockContext = new TestMockContext(getContext());
        String tokenToTest = "accessToken=" + UUID.randomUUID();
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        addItemToCache(mockCache, tokenToTest, "refreshToken", VALID_AUTHORITY, resource,
                "clientId", "userId", false);

        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // acquire token call will return from cache
        context.acquireToken(testActivity, resource, "ClienTid", "redirectUri", "userid", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback.mException);
        assertEquals("Same token in response as in cache", tokenToTest,
                callback.mResult.getAccessToken());
        clearCache(context);
    }

    /**
     * setup cache with userid for normal token and multiresource refresh token bound to one userid. test calls for different resources and users.
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void testAcquireTokenMultiResourceToken_UserId() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

        TestMockContext mockContext = new TestMockContext(getContext());
        String tokenToTest = "accessToken=" + UUID.randomUUID();
        String tokenWithRefreshToken = "accessToken=" + UUID.randomUUID();
        String resource = "Resource" + UUID.randomUUID();

        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();
        addItemToCache(mockCache, tokenToTest, "refreshTokenNormal", VALID_AUTHORITY, resource,
                "ClienTid", "userid", false);
        addItemToCache(mockCache, "", "refreshTokenMultiResource", VALID_AUTHORITY, resource,
                "ClienTid", "userid", true);
        addItemToCache(mockCache, "", "refreshTokenMultiResource2", VALID_AUTHORITY, "dummyResource2",
                "ClienTid", "userid", true);
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        MockWebRequestHandler mockWebRequest = setMockWebRequest(context, tokenWithRefreshToken);
                
        CountDownLatch signal = new CountDownLatch(1);
        MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        //-----------Acquire token call will return from cache--------------------
        context.acquireToken(testActivity, resource, "ClienTid", "redirectUri", "userid", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        
        assertNull("Error is null", callback.mException);
        assertEquals("Same token in response as in cache", tokenToTest,
                callback.mResult.getAccessToken());  
        
      //-----------Acquire token call will not return from cache for broad Token-
        // cached item does not have access token since it was broad refresh token
        signal = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, "dummyResource2", "ClienTid", "redirectUri", "userid", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        
        assertNull("Error is null", callback.mException);
        assertEquals("Same token as refresh token result", tokenWithRefreshToken,
                callback.mResult.getAccessToken());

        //-----------Different resource with same userid--------------------------
        signal = new CountDownLatch(1);
        testActivity = new MockActivity(signal);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, "anotherResource123", "ClienTid", "redirectUri",
                "userid", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        
        assertEquals("Token is returned from refresh token request", tokenWithRefreshToken,
                callback.mResult.getAccessToken());
        assertFalse("Multiresource is not set in the mocked response", callback.mResult.getIsMultiResourceRefreshToken());
        assertTrue("Request to get token uses broad refresh token", mockWebRequest.getRequestContent().contains("refreshTokenMultiResource"));

        //----------Same call again to use it from cache----------------------------
        callback.mResult = null;
        removeMockWebRequest(context);
        context.acquireToken(testActivity, "anotherResource123", "ClienTid", "redirectUri",
                "userid", callback);
        assertEquals("Same token in response as in cache for same call", tokenWithRefreshToken,
                callback.mResult.getAccessToken());
                
        //-----------Empty userid will prompt---------------------------------------
        // Items are linked to userid. If it is not there, it can't use for refresh or access token.
        signal = new CountDownLatch(1);
        testActivity = new MockActivity(signal);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, resource, "ClienTid", "redirectUri", "", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNull("Result is null since it tries to start activity", callback.mResult);
        assertEquals("ACtivity was attempted to start.",  UIRequest.BROWSER_FLOW, testActivity.mStartActivityRequestCode);
       
        clearCache(context);
    }

    public void testAcquireTokenMultiResource_ADFSIssue() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        // adfs does not return userid and multiresource token
        TestMockContext mockContext = new TestMockContext(getContext());
        String tokenToTest = "accessToken=" + UUID.randomUUID();
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        // add item without userid and normal refresh token
        addItemToCache(mockCache, tokenToTest, "refreshToken", VALID_AUTHORITY, resource,
                "ClienTid", "", false);
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        MockActivity testActivity = new MockActivity();
        CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // acquire token call will return from cache
        context.acquireToken(testActivity, resource, "clientid", "redirectUri", "", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback.mException);
        assertEquals("Same token in response as in cache", tokenToTest,
                callback.mResult.getAccessToken());
               
        // Request with different resource will result in prompt
        signal = new CountDownLatch(1);
        testActivity = new MockActivity();
        testActivity.mSignal = signal;
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, "anotherResource123", "ClienTid", "redirectUri", "",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        
        assertTrue("Attemps to launch", testActivity.mStartActivityRequestCode != -1);

        // asking with different userid will not return item from cache and try to launch activity
        signal = new CountDownLatch(1);
        testActivity = new MockActivity();
        testActivity.mSignal = signal;
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, resource, "ClienTid", "redirectUri", "someuser",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        assertTrue("Attemps to launch", testActivity.mStartActivityRequestCode != -1);
        
        clearCache(context);
    }

    private ITokenCacheStore getCacheForRefreshToken() {
        DefaultTokenCacheStore cache = new DefaultTokenCacheStore(getContext());
        Calendar expiredTime = new GregorianCalendar();
        Log.d("Test", "Time now:" + expiredTime.toString());
        expiredTime.add(Calendar.MINUTE, -60);
        TokenCacheItem refreshItem = new TokenCacheItem();
        refreshItem.setAuthority(VALID_AUTHORITY);
        refreshItem.setResource("resource");
        refreshItem.setClientId("clientId");
        refreshItem.setAccessToken("accessToken");
        refreshItem.setRefreshToken("refreshToken=");
        refreshItem.setExpiresOn(expiredTime.getTime());
        cache.setItem(
                CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId", false, "userId"),
                refreshItem);
        return cache;
    }

    private ITokenCacheStore getValidCache(String token, String resource, boolean isMultiResource) {
        DefaultTokenCacheStore cache = new DefaultTokenCacheStore(getContext());
        // Code response
        Calendar timeAhead = new GregorianCalendar();
        Log.d("Test", "Time now:" + timeAhead.toString());
        timeAhead.add(Calendar.MINUTE, 10);
        TokenCacheItem refreshItem = new TokenCacheItem();
        refreshItem.setAuthority(VALID_AUTHORITY);
        refreshItem.setResource(resource);
        refreshItem.setClientId("clientId");
        refreshItem.setAccessToken(token);
        refreshItem.setRefreshToken("refreshToken=");
        refreshItem.setExpiresOn(timeAhead.getTime());
        cache.setItem(CacheKey.createCacheKey(VALID_AUTHORITY, resource, "clientId",
                isMultiResource, "userId"), refreshItem);
        return cache;
    }

    private ITokenCacheStore addItemToCache(ITokenCacheStore cache, String token,
            String refreshToken, String authority, String resource, String clientId, String userId,
            boolean isMultiResource) {
        // Code response
        Calendar timeAhead = new GregorianCalendar();
        Log.d(TAG, "addItemToCache Time now:" + timeAhead.toString());
        timeAhead.add(Calendar.MINUTE, 10);
        TokenCacheItem refreshItem = new TokenCacheItem();
        refreshItem.setAuthority(authority);
        refreshItem.setResource(resource);
        refreshItem.setClientId(clientId);
        refreshItem.setAccessToken(token);
        refreshItem.setRefreshToken(refreshToken);
        refreshItem.setExpiresOn(timeAhead.getTime());
        refreshItem.setIsMultiResourceRefreshToken(isMultiResource);
        String key = CacheKey.createCacheKey(VALID_AUTHORITY, resource, clientId,
                isMultiResource, userId);
        Log.d(TAG, "Key: " + key);
        cache.setItem(key, refreshItem);

        TokenCacheItem item = cache.getItem(CacheKey.createCacheKey(VALID_AUTHORITY, resource,
                clientId, isMultiResource, userId));
        assertNotNull("item is in cache", item);

        return cache;
    }

    private void clearCache(AuthenticationContext context) {
        if (context.getCache() != null) {
            context.getCache().removeAll();
        }
    }

    class MockCache implements ITokenCacheStore {
        /**
         * serial version related to serializable interface
         */
        private static final long serialVersionUID = -3292746098551178627L;

        private static final String TAG = "MockCache";

        SparseArray<TokenCacheItem> mCache = new SparseArray<TokenCacheItem>();

        @Override
        public TokenCacheItem getItem(String key) {
            Log.d(TAG, "Mock cache get item:" + key.toString());
            return mCache.get(key.hashCode());
        }

        @Override
        public void setItem(String key, TokenCacheItem item) {
            Log.d(TAG, "Mock cache set item:" + item.toString());
            mCache.append(CacheKey.createCacheKey(item).hashCode(), item);
        }

        @Override
        public void removeItem(String key) {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean contains(String key) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void removeAll() {
            // TODO Auto-generated method stub
        }
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

        public URL getAuthorizationUrl() {
            return authorizationUrl;
        }
    }

    /**
     * Mock activity
     */
    class MockActivity extends Activity {

        private static final String TAG = "MockActivity";

        int mStartActivityRequestCode = -123;

        Intent mStartActivityIntent;

        CountDownLatch mSignal;

        Bundle mStartActivityOptions;

        public MockActivity(CountDownLatch signal){
            mSignal = signal;
        }
        
        public MockActivity() {
            // TODO Auto-generated constructor stub
        }

        @Override
        public String getPackageName() {
            return ReflectionUtils.TEST_PACKAGE_NAME;
        }

        @Override
        public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
            Log.d(TAG, "startActivityForResult:" + requestCode);
            mStartActivityIntent = intent;
            mStartActivityRequestCode = requestCode;
            mStartActivityOptions = options;
            // test call needs to stop the tests at this point. If it reaches
            // here, it means authenticationActivity was attempted to launch.
            // Since it is mock activity, it will not launch something.
            if (mSignal != null)
                mSignal.countDown();
        }

    }

    class TestMockContext extends MockContext {

        private Context mContext;

        private static final String PREFIX = "test.mock.package";

        boolean resolveIntent = true;

        public TestMockContext(Context context) {
            mContext = context;
        }

        @Override
        public String getPackageName() {
            return PREFIX;
        }

        @Override
        public Context getApplicationContext() {
            return mContext;
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
