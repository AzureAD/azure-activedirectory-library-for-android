// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.microsoft.aad.adal.UsageAuthenticationException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import junit.framework.Assert;

public class AuthenticationContextTest extends AndroidTestCase {

    /**
     * Check case-insensitive lookup
     */
    private static final String VALID_AUTHORITY = "https://Login.windows.net/Omercantest.Onmicrosoft.com";

    protected final static int CONTEXT_REQUEST_TIME_OUT = 3000;

    protected final static int ACTIVITY_TIME_OUT = 1000;

    private final static String TEST_AUTHORITY = "https://login.windows.net/ComMon/";

    private static final String TEST_PACKAGE_NAME = "com.microsoft.aad.adal.test";

    static final String testClientId = "650a6609-5463-4bc4-b7c6-19df7990a8bc";

    static final String testResource = "https://omercantest.onmicrosoft.com/spacemonkey";

    static final String TEST_IDTOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";

    static final String TEST_IDTOKEN_USERID = "4f859989-a2ff-411e-9048-c322247ac62c";

    static final String TEST_IDTOKEN_UPN = "admin@aaltests.onmicrosoft.com";

    private byte[] testSignature;

    private String testTag;

    private static final String TAG = "AuthenticationContextTest";

    protected void setUp() throws Exception {
        super.setUp();
        Log.d(TAG, "setup key at settings");
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            // use same key for tests
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes("UTF-8"), 100, 256));
            SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }
        AuthenticationSettings.INSTANCE.setUseBroker(false);
        // ADAL is set to this signature for now
        PackageInfo info = mContext.getPackageManager().getPackageInfo(TEST_PACKAGE_NAME,
                PackageManager.GET_SIGNATURES);

        // Broker App can be signed with multiple certificates. It will look
        // all of them
        // until it finds the correct one for ADAL broker.
        for (Signature signature : info.signatures) {
            testSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(testSignature);
            testTag = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            break;
        }
    }

    protected void tearDown() throws Exception {
        Logger.getInstance().setExternalLogger(null);
        super.tearDown();
    }

    /**
     * test constructor to make sure authority parameter is set
     *
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    public void testConstructor() throws NoSuchAlgorithmException, NoSuchPaddingException {
        testAuthorityTrim("authorityFail");
        testAuthorityTrim("https://msft.com////");
        testAuthorityTrim("https:////");
        AuthenticationContext context2 = new AuthenticationContext(getContext(),
                "https://github.com/MSOpenTech/some/some", false);
        assertEquals("https://github.com/MSOpenTech", context2.getAuthority());
    }

    private void testAuthorityTrim(String authority) throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        try {
            new AuthenticationContext(getContext(), authority, false);
            Assert.fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue("authority in the msg", e.getMessage().contains("authority"));
        }
    }

    public void testConstructorNoCache() {
        String authority = "https://github.com/MSOpenTech";
        AuthenticationContext context = new AuthenticationContext(getContext(), authority, false,
                null);
        assertNull(context.getCache());
    }

    public void testConstructorWithCache() throws NoSuchAlgorithmException, NoSuchPaddingException {
        String authority = "https://github.com/MSOpenTech";
        DefaultTokenCacheStore expected = new DefaultTokenCacheStore(getContext());
        AuthenticationContext context = new AuthenticationContext(getContext(), authority, false,
                expected);
        assertEquals("Cache object is expected to be same", expected, context.getCache());

        AuthenticationContext contextDefaultCache = new AuthenticationContext(getContext(),
                authority, false);
        assertNotNull(contextDefaultCache.getCache());
    }

    public void testConstructor_InternetPermission() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        String authority = "https://github.com/MSOpenTech";
        FileMockContext mockContext = new FileMockContext(getContext());
        mockContext.requestedPermissionName = "android.permission.INTERNET";
        mockContext.responsePermissionFlag = PackageManager.PERMISSION_GRANTED;

        // no exception
        new AuthenticationContext(mockContext, authority, false);

        try {
            mockContext.responsePermissionFlag = PackageManager.PERMISSION_DENIED;
            new AuthenticationContext(mockContext, authority, false);
            Assert.fail("Supposed to fail");
        } catch (Exception e) {

            assertEquals("Permission related message",
                    ADALError.DEVELOPER_INTERNET_PERMISSION_MISSING,
                    ((AuthenticationException)e.getCause()).getCode());
        }
    }

    public void testConstructorValidateAuthority() throws NoSuchAlgorithmException,
            NoSuchPaddingException {

        String authority = "https://github.com/MSOpenTech";
        AuthenticationContext context = getAuthenticationContext(getContext(), authority, true,
                null);
        assertTrue("Validate flag is expected to be same", context.getValidateAuthority());

        context = new AuthenticationContext(getContext(), authority, false);
        assertFalse("Validate flag is expected to be same", context.getValidateAuthority());
    }

    public void testCorrelationId_setAndGet() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        UUID requestCorrelationId = UUID.randomUUID();
        AuthenticationContext context = new AuthenticationContext(getContext(), TEST_AUTHORITY,
                true);
        context.setRequestCorrelationId(requestCorrelationId);
        assertEquals("Verifier getter and setter", requestCorrelationId,
                context.getRequestCorrelationId());
    }

    /**
     * External call to Service to get real error response. Add expired item in
     * cache to try refresh token request. Web Request should have correlationId
     * in the header.
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws InterruptedException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @MediumTest
    @UiThreadTest
    public void testCorrelationId_InWebRequest() throws NoSuchFieldException,
            IllegalAccessException, InterruptedException, NoSuchAlgorithmException,
            NoSuchPaddingException {

        if (Build.VERSION.SDK_INT <= 15) {
            Log.v(TAG,
                    "Server is returning 401 status code without challenge. HttpUrlConnection does not return error stream for that in SDK 15. Without error stream, this test is useless.");
            return;
        }

        FileMockContext mockContext = new FileMockContext(getContext());
        String expectedAccessToken = "TokenFortestAcquireToken" + UUID.randomUUID().toString();
        String expectedClientId = "client" + UUID.randomUUID().toString();
        String expectedResource = "resource" + UUID.randomUUID().toString();
        String expectedUser = "userid" + UUID.randomUUID().toString();
        ITokenCacheStore mockCache = getMockCache(-30, expectedAccessToken, expectedResource,
                expectedClientId, expectedUser, false);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        setConnectionAvailable(context, true);
        UUID requestCorrelationId = UUID.randomUUID();
        Log.d(TAG, "test correlationId:" + requestCorrelationId.toString());
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final TestLogResponse response = new TestLogResponse();
        response.listenLogForMessageSegments(signal, "Authentication failed", "correlation_id:\"\""
                + requestCorrelationId.toString());

        // Call acquire token with prompt never to prevent activity launch
        context.setRequestCorrelationId(requestCorrelationId);
        context.acquireTokenSilentAsync(expectedResource, expectedClientId, expectedUser, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that web request send correct headers
        Log.v(TAG, "Response msg:" + response.message);
        assertNotNull("Server response isn't null ", response.message);
        assertTrue("Server response has same correlationId",
                response.message.contains(requestCorrelationId.toString()));
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
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testResolveIntent() throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, NoSuchAlgorithmException, NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getContext());
        AuthenticationContext context = new AuthenticationContext(mockContext, VALID_AUTHORITY,
                false);
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
     *
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testAcquireTokenNegativeArguments() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
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
    }

    @SmallTest
    public void testAcquireToken_userId() throws ClassNotFoundException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, NoSuchAlgorithmException,
            NoSuchPaddingException, InterruptedException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);
        setConnectionAvailable(context, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        context.acquireToken(testActivity, "resource56", "clientId345", "redirect123", "userid123",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // verify request
        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertEquals("AuthenticationRequest inside the intent", request.getClass(),
                Class.forName("com.microsoft.aad.adal.AuthenticationRequest"));
        String redirect = (String)ReflectionUtils.getFieldValue(request, "mRedirectUri");
        assertEquals("Redirect uri is same as package", "redirect123", redirect);
        String loginHint = (String)ReflectionUtils.getFieldValue(request, "mLoginHint");
        assertEquals("login hint same as userid", "userid123", loginHint);
        String client = (String)ReflectionUtils.getFieldValue(request, "mClientId");
        assertEquals("client is same", "clientId345", client);
        String authority = (String)ReflectionUtils.getFieldValue(request, "mAuthority");
        assertEquals("authority is same", "https://login.windows.net/common", authority);
        String resource = (String)ReflectionUtils.getFieldValue(request, "mResource");
        assertEquals("resource is same", "resource56", resource);
    }

    @SmallTest
    public void testEmptyRedirect() throws ClassNotFoundException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, NoSuchAlgorithmException,
            NoSuchPaddingException, InterruptedException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);
        setConnectionAvailable(context, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        context.acquireToken(testActivity, "resource", "clientId", "", "userid", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertEquals("AuthenticationRequest inside the intent", request.getClass(),
                Class.forName("com.microsoft.aad.adal.AuthenticationRequest"));
        String redirect = (String)ReflectionUtils.getFieldValue(request, "mRedirectUri");
        assertEquals("Redirect uri is same as package", "com.microsoft.aad.adal.test", redirect);
    }

    @SmallTest
    public void testPrompt() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException, NoSuchAlgorithmException,
            NoSuchPaddingException, InterruptedException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);
        setConnectionAvailable(context, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        // 1 - Send prompt always
        context.acquireToken(testActivity, "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.Always, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Get intent from activity to verify extraparams are send
        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

        PromptBehavior prompt = (PromptBehavior)ReflectionUtils.getFieldValue(request, "mPrompt");
        assertEquals("Prompt param is same", PromptBehavior.Always, prompt);

        // 2 - Send refresh prompt
        final CountDownLatch signal2 = new CountDownLatch(1);
        MockAuthenticationCallback callback2 = new MockAuthenticationCallback(signal2);
        testActivity.mSignal = signal2;
        context.acquireToken(testActivity, "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.REFRESH_SESSION, callback2);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Get intent from activity to verify extraparams are send
        intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        request = intent.getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

        prompt = (PromptBehavior)ReflectionUtils.getFieldValue(request, "mPrompt");
        assertEquals("Prompt param is same", PromptBehavior.REFRESH_SESSION, prompt);
    }

    @SmallTest
    public void testExtraParams() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException, NoSuchAlgorithmException,
            NoSuchPaddingException, InterruptedException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);
        setConnectionAvailable(context, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        String expected = "&extraParam=1";
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        // 1 - Send extra param
        context.acquireToken(testActivity, "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.Always, expected, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // get intent from activity to verify extraparams are send
        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

        assertEquals("AuthenticationRequest inside the intent", request.getClass(),
                Class.forName("com.microsoft.aad.adal.AuthenticationRequest"));
        String extraparm = (String)ReflectionUtils.getFieldValue(request,
                "mExtraQueryParamsAuthentication");
        assertEquals("Extra query param is same", expected, extraparm);

        // 2- Don't send extraqueryparam
        ReflectionUtils.setFieldValue(context, "mAuthorizationCallback", null);
        CountDownLatch signal2 = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal2);
        testActivity.mSignal = signal2;
        context.acquireToken(testActivity, "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.Always, null, callback);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // verify from mocked activity intent
        intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        request = intent.getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

        assertEquals("AuthenticationRequest inside the intent", request.getClass(),
                Class.forName("com.microsoft.aad.adal.AuthenticationRequest"));
        extraparm = (String)ReflectionUtils.getFieldValue(request,
                "mExtraQueryParamsAuthentication");
        assertNull("Extra query param is null", extraparm);
    }

    public static Object createAuthenticationRequest(String authority, String resource,
            String client, String redirect, String loginhint) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");

        Constructor<?> constructor = c.getDeclaredConstructor(String.class, String.class,
                String.class, String.class, String.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authority, resource, client, redirect, loginhint);
        return o;
    }

    /**
     * Test throws for different missing arguments
     *
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testAcquireTokenByRefreshTokenNegativeArguments() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getContext());

        // AuthenticationContext will throw at constructor if authority is null.

        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
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

    @SmallTest
    public void testAcquireTokenByRefreshToken_ConnectionNotAvailable()
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException, InterruptedException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        setConnectionAvailable(context, false);

        CountDownLatch signal = new CountDownLatch(1);
        final MockAuthenticationCallback mockCallback = new MockAuthenticationCallback(signal);
        context.acquireTokenByRefreshToken("refresh", "clientId", "resource", mockCallback);

        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        assertTrue("Exception type", mockCallback.mException instanceof AuthenticationException);
        assertEquals("Connection related error code", ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE,
                ((AuthenticationException) mockCallback.mException).getCode());
    }

    private void setConnectionAvailable(final AuthenticationContext context, final boolean status)
            throws NoSuchFieldException, IllegalAccessException {
        ReflectionUtils.setFieldValue(context, "mConnectionService", new IConnectionService() {

            @Override
            public boolean isConnectionAvailable() {
                return status;
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
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     */
    @SmallTest
    public void testAcquireTokenByRefreshTokenPositive() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException,
            NoSuchAlgorithmException, NoSuchPaddingException, InterruptedException {
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        setConnectionAvailable(context, true);
        final CountDownLatch signal = new CountDownLatch(1);
        String id = UUID.randomUUID().toString();
        String expectedAccessToken = "accessToken" + id;
        String expectedClientId = "client" + UUID.randomUUID().toString();
        String exptedResource = "resource" + UUID.randomUUID().toString();
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        MockWebRequestHandler mockWebRequest = setMockWebRequest(context, id, "refreshToken" + id);

        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", expectedAccessToken, callback.mResult.getAccessToken());
        assertEquals("Same refresh token", "refreshToken" + id, callback.mResult.getRefreshToken());
        assertTrue("Content has client in the message", mockWebRequest.getRequestContent()
                .contains(expectedClientId));
        assertFalse("Content does not have resource in the message", mockWebRequest
                .getRequestContent().contains(exptedResource));

        final CountDownLatch signal2 = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal2);
        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, exptedResource,
                callback);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", expectedAccessToken, callback.mResult.getAccessToken());
        assertEquals("Same refresh token", "refreshToken" + id, callback.mResult.getRefreshToken());
        assertTrue("Content has client in the message", mockWebRequest.getRequestContent()
                .contains(expectedClientId));
        assertTrue("Content has resource in the message", mockWebRequest.getRequestContent()
                .contains(exptedResource));
        assertNotNull("Result has user info from TestIdToken", callback.mResult.getUserInfo());
        assertEquals("Result has user info from TestIdToken", "admin@aaltests.onmicrosoft.com",
                callback.mResult.getUserInfo().getDisplayableId());
    }

    public void testAcquireTokenByRefreshToken_NotReturningRefreshToken()
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InvocationTargetException, NoSuchAlgorithmException, NoSuchPaddingException,
            InterruptedException {
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        setConnectionAvailable(context, true);
        final CountDownLatch signal = new CountDownLatch(1);
        String id = UUID.randomUUID().toString();
        String expectedAccessToken = "accessToken" + id;
        String expectedClientId = "client" + UUID.randomUUID().toString();
        String exptedResource = "resource" + UUID.randomUUID().toString();
        String refreshToken = "refreshTokenSending";
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockWebRequestHandler mockWebRequest = setMockWebRequest(context, id, "");
        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", expectedAccessToken, callback.mResult.getAccessToken());
        assertEquals("Same refresh token", refreshToken, callback.mResult.getRefreshToken());
        assertTrue("Content has client in the message", mockWebRequest.getRequestContent()
                .contains(expectedClientId));
        assertFalse("Content does not have resource in the message", mockWebRequest
                .getRequestContent().contains(exptedResource));
    }

    private MockWebRequestHandler setMockWebRequest(final AuthenticationContext context, String id,
            String refreshToken) throws NoSuchFieldException, IllegalAccessException {
        MockWebRequestHandler mockWebRequest = new MockWebRequestHandler();
        String json = "{\"access_token\":\"accessToken"
                + id
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"29344\",\"expires_on\":\"1368768616\",\"refresh_token\":\""
                + refreshToken + "\",\"scope\":\"*\",\"id_token\":\"" + TEST_IDTOKEN + "\"}";
        mockWebRequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", mockWebRequest);
        return mockWebRequest;
    }

    private void removeMockWebRequest(final AuthenticationContext context)
            throws NoSuchFieldException, IllegalAccessException {
        ReflectionUtils.setFieldValue(context, "mWebRequest", null);
    }

    /**
     * authority is malformed and error should come back in callback
     *
     * @throws InterruptedException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testAcquireTokenAuthorityMalformed() throws InterruptedException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        // Malformed url error will come back in callback
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                "abcd://vv../v", false);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        context.acquireToken(testActivity, "resource", "clientId", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
        assertNotNull("Error is not null", callback.mException);
        assertEquals("NOT_VALID_URL", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL,
                ((AuthenticationException) callback.mException).getCode());
    }

    /**
     * authority is validated and intent start request is sent
     *
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testAcquireTokenValidateAuthorityReturnsValid() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {

        FileMockContext mockContext = new FileMockContext(getContext());
        AuthenticationContext context = new AuthenticationContext(mockContext, VALID_AUTHORITY,
                true);
        setConnectionAvailable(context, true);
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

    @SmallTest
    public void testCorrelationId_InDiscovery() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {

        FileMockContext mockContext = new FileMockContext(getContext());
        AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY,
                true, null);
        setConnectionAvailable(context, true);
        final CountDownLatch signal = new CountDownLatch(1);
        UUID correlationId = UUID.randomUUID();
        MockActivity testActivity = new MockActivity();
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockDiscovery discovery = new MockDiscovery(true);
        ReflectionUtils.setFieldValue(context, "mDiscovery", discovery);

        // API call
        context.setRequestCorrelationId(correlationId);
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check correlationID that was set in the Discovery obj
        assertEquals("CorrelationId in discovery needs to be same as in request", correlationId,
                discovery.correlationId);
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
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testAcquireTokenValidateAuthorityReturnsInValid() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {

        FileMockContext mockContext = new FileMockContext(getContext());
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

        // Sync test
        try {
            context.acquireTokenSilentSync("resource", "clientid", "userid");
            Assert.fail("Validation should throw");
        } catch (AuthenticationException exc) {
            assertEquals("NOT_VALID_URL", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE,
                    exc.getCode());
        }

        clearCache(context);
    }

    /**
     * acquire token without validation
     *
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testAcquireTokenWithoutValidation() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {

        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, null);
        setConnectionAvailable(context, true);
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
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testRefreshTokenWebRequestHasError() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InvocationTargetException, NoSuchAlgorithmException, NoSuchPaddingException {

        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        setConnectionAvailable(context, true);
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        webrequest.setReturnResponse(new HttpWebResponse(500, null, null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);
        final TestLogResponse response = new TestLogResponse();
        response.listenLogForMessageSegments(signal, "Refresh token did not return accesstoken");

        context.acquireTokenSilentAsync("resource", "clientid", TEST_IDTOKEN_USERID, callback);

        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
        assertTrue("Log message has same webstatus code", response.errorCode.equals(ADALError.AUTH_FAILED_NO_TOKEN));
        assertNotNull("Cache item is not removed for this item", mockCache.getItem(CacheKey.createCacheKey(
                VALID_AUTHORITY, "resource", "clientId", false, TEST_IDTOKEN_USERID)));
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
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testRefreshTokenPositive() throws InterruptedException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException,
            NoSuchAlgorithmException, NoSuchPaddingException, AuthenticationException {

        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        setConnectionAvailable(context, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String json = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\",\"expires_in\":\"-10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);

        // Call acquire token which will try refresh token based on cache
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        verifyRefreshTokenResponse(mockCache, callback.mException, callback.mResult);

        // Do silent token request and return TestIdToken in the result
        json = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenReturnsWithTestIdToken\",\"token_type\":\"Bearer\",\"expires_in\":\"10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshABC\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TEST_IDTOKEN_USERID);
        assertEquals("Access Token", "TokenReturnsWithTestIdToken", result.getAccessToken());
        assertEquals("Refresh Token", "refreshABC", result.getRefreshToken());
        assertEquals("TestIdToken", TEST_IDTOKEN, result.getIdToken());
        clearCache(context);
    }

    @SmallTest
    public void testScenario_UserId_LoginHint_Use() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InvocationTargetException, NoSuchAlgorithmException, NoSuchPaddingException,
            UnsupportedEncodingException, AuthenticationException {
        scenario_UserId_LoginHint("test@user.com", "test@user.com", "test@user.com");
    }

    private void scenario_UserId_LoginHint(String TestIdTokenUpn, String responseIntentHint,
            String acquireTokenHint) throws InterruptedException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException,
            NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException,
            AuthenticationException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        context.getCache().removeAll();
        setConnectionAvailable(context, true);
        final CountDownLatch signal = new CountDownLatch(1);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        TestIdToken TestIdToken = new TestIdToken();
        TestIdToken.upn = TestIdTokenUpn;
        TestIdToken.oid = "userid123";
        String json = "{\"id_token\":\""
                + TestIdToken.getTestIdToken()
                + "\",\"access_token\":\"TokenUserIdTest\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);
        Intent intent = getResponseIntent(callback, "resource", "clientid", "redirectUri",
                responseIntentHint);

        // Get token from onActivityResult after Activity returns
        tokenWithAuthenticationActivity(context, testActivity, signal, signalCallback, intent,
                "resource", "clientid", "redirectUri", acquireTokenHint, callback);

        // Token will return to callback with TestIdToken
        verifyTokenResult(TestIdToken, callback.mResult);

        // Same call should get token from cache
        final CountDownLatch signalCallback2 = new CountDownLatch(1);
        callback.mSignal = signalCallback2;
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", acquireTokenHint,
                callback);
        signalCallback2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        verifyTokenResult(TestIdToken, callback.mResult);

        // Call with userId should return from cache as well
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TestIdToken.oid);
        verifyTokenResult(TestIdToken, result);

        clearCache(context);
    }

    @SmallTest
    public void testScenario_NullUser_TestIdToken() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InvocationTargetException, NoSuchAlgorithmException, NoSuchPaddingException,
            UnsupportedEncodingException, AuthenticationException {
        scenario_UserId_LoginHint("test@user.com", "", "");
    }

    @SmallTest
    public void testScenario_LoginHint_TestIdToken_Different() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InvocationTargetException, NoSuchAlgorithmException, NoSuchPaddingException,
            UnsupportedEncodingException, AuthenticationException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        context.getCache().removeAll();
        setConnectionAvailable(context, true);
        final CountDownLatch signal = new CountDownLatch(1);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        TestIdToken TestIdToken = new TestIdToken();
        TestIdToken.upn = "admin@user.com";
        TestIdToken.oid = "admin123";
        String loginHint = "user1@user.com";
        String json = "{\"id_token\":\""
                + TestIdToken.getTestIdToken()
                + "\",\"access_token\":\"TokenUserIdTest\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);
        Intent intent = getResponseIntent(callback, "resource", "clientid", "redirectUri",
                loginHint);

        // Get token from onActivityResult after Activity returns
        tokenWithAuthenticationActivity(context, testActivity, signal, signalCallback, intent,
                "resource", "clientid", "redirectUri", loginHint, callback);

        // Token will return to callback with TestIdToken
        verifyTokenResult(TestIdToken, callback.mResult);

        // Same call with correct upn will return from cache
        final CountDownLatch signalCallback2 = new CountDownLatch(1);
        callback.mSignal = signalCallback2;
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", TestIdToken.upn, callback);
        signalCallback2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        verifyTokenResult(TestIdToken, callback.mResult);

        // Call with userId should return from cache as well
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TestIdToken.oid);
        verifyTokenResult(TestIdToken, result);

        clearCache(context);
    }

    @SmallTest
    public void testScenario_Empty_TestIdToken() throws InterruptedException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException,
            NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException,
            AuthenticationException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        context.getCache().removeAll();
        setConnectionAvailable(context, true);
        final CountDownLatch signal = new CountDownLatch(1);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String json = "{\"access_token\":\"TokenUserIdTest\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);
        Intent intent = getResponseIntent(callback, "resource", "clientid", "redirectUri", null);

        // Get token from onActivityResult after Activity returns
        tokenWithAuthenticationActivity(context, testActivity, signal, signalCallback, intent,
                "resource", "clientid", "redirectUri", null, callback);

        // Token will return to callback with TestIdToken
        verifyTokenResult(null, callback.mResult);

        // Call with userId should return from cache as well
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid", null);
        verifyTokenResult(null, result);

        clearCache(context);
    }
    
    /**
     * Make sure we cache the family id correctly when we get family id from server. 
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    @SmallTest
    public void testFamilyClientIdCorrectlyStoredInCache() throws InterruptedException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException,
            NoSuchAlgorithmException, NoSuchPaddingException, AuthenticationException {

        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        setConnectionAvailable(context, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String json = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\",\"expires_in\":\"-10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\",\"foci\":\"familyClientId\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);

        // Call acquire token which will try refresh token based on cache
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        verifyRefreshTokenResponse(mockCache, callback.mException, callback.mResult);
        verifyFamilyIdStoredInTokenCacheItem(mockCache, CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId",
                        false, TEST_IDTOKEN_UPN), "familyClientId");

        // Do silent token request and return idtoken in the result
        json = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"I am a new access token\",\"token_type\":\"Bearer\",\"expires_in\":\"10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"I am a new refresh token\",\"scope\":\"*\",\"foci\":\"familyClientId\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TEST_IDTOKEN_USERID);
        assertEquals("Returned assess token is not as expected.", "I am a new access token", result.getAccessToken());
        assertEquals("Returned refresh token is not as expected.", "I am a new refresh token", result.getRefreshToken());
        assertEquals("Returned id token is not as expected.", TEST_IDTOKEN, result.getIdToken());
        verifyFamilyIdStoredInTokenCacheItem(mockCache, CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId",
                        false, TEST_IDTOKEN_UPN), "familyClientId");
        clearCache(context);
    }
    
    /**
     * Make sure if we acquire token for a client id, and if we already have a family item in cache, we use that refresh token.
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    @SmallTest
    public void testRefreshTokenRequestWithFamilyIdSuccess() throws InterruptedException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException,
            NoSuchAlgorithmException, NoSuchPaddingException, AuthenticationException {
        
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheWithFamilyIdForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        
        setConnectionAvailable(context, true);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        
        // Do silent token request and return idtoken in the result
        final String json = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"I am a new access token\",\"token_type\":\"Bearer\",\"expires_in\":\"10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"I am a new refresh token\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);
        
        final String anotherClientId = "clientId2";
        AuthenticationResult result = context.acquireTokenSilentSync("resource", anotherClientId,
                TEST_IDTOKEN_USERID);
        assertEquals("Returned assess token is not as expected.", "I am a new access token", result.getAccessToken());
        assertEquals("Returned refresh token is not as expected", "I am a new refresh token", result.getRefreshToken());
        assertEquals("Returned id token is not as expected.", TEST_IDTOKEN, result.getIdToken());
        clearCache(context);
    }
    
    /**
     * Make sure if we have a family token in the cache and we fail to get a token using it, we correctly fail out. 
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    @SmallTest
    public void testRefreshTokenRequestWithFamilyIdFailed() throws InterruptedException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheWithFamilyIdForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        
        setConnectionAvailable(context, true);
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        
        // Do silent token request and return idtoken in the result
        String responseBody = "{\"error\":\"invalid_grant\",\"error_description\":\"AADSTS70000: Authentication failed. Refresh Token is not valid.\r\nTrace ID: bb27293d-74e4-4390-882b-037a63429026\r\nCorrelation ID: b73106d5-419b-4163-8bc6-d2c18f1b1a13\r\nTimestamp: 2014-11-06 18:39:47Z\",\"error_codes\":[70000],\"timestamp\":\"2014-11-06 18:39:47Z\",\"trace_id\":\"bb27293d-74e4-4390-882b-037a63429026\",\"correlation_id\":\"b73106d5-419b-4163-8bc6-d2c18f1b1a13\",\"submit_url\":null,\"context\":null}";
        webrequest.setReturnResponse(new HttpWebResponse(400, responseBody.getBytes(), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);
        
        final String anotherClientId = "clientId2";
        try {
            context.acquireTokenSilentSync("resource", anotherClientId,
                    TEST_IDTOKEN_USERID);
        } catch (AuthenticationException authException) {
            assertEquals("Error code is not as expected", ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED, authException.getCode());
        }
        
        clearCache(context);
    }
    
    private void verifyFamilyIdStoredInTokenCacheItem(final ITokenCacheStore cacheStore, final String cacheKey, 
            final String expectedFamilyClientId) {
        
        final TokenCacheItem tokenCacheItem = cacheStore.getItem(cacheKey);
        assertNotNull(tokenCacheItem);
        assertEquals(expectedFamilyClientId, tokenCacheItem.getFamilyClientId());
    }

    private Intent getResponseIntent(MockAuthenticationCallback callback, String resource,
            String clientid, String redirect, String loginHint) throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        // Provide mock result for activity that returns code and proper state
        Intent intent = new Intent();
        intent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
        Object authRequest = createAuthenticationRequest(VALID_AUTHORITY, resource, clientid,
                redirect, loginHint);
        intent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                (Serializable) authRequest);
        intent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, VALID_AUTHORITY
                + "/oauth2/authorize?code=123&state=" + getState(VALID_AUTHORITY, resource));
        return intent;
    }

    private void tokenWithAuthenticationActivity(final AuthenticationContext context,
            final MockActivity testActivity, CountDownLatch signal,
            CountDownLatch signalOnActivityResult, Intent responseIntent, String resource,
            String clientid, String redirect, String loginHint, MockAuthenticationCallback callback)
            throws InterruptedException {

        // Call acquire token
        context.acquireToken(testActivity, resource, clientid, redirect, loginHint, callback);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        // Activity will start
        assertEquals("Activity was attempted to start.",
                AuthenticationConstants.UIRequest.BROWSER_FLOW,
                testActivity.mStartActivityRequestCode);

        context.onActivityResult(testActivity.mStartActivityRequestCode,
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, responseIntent);
        signalOnActivityResult.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
    }

    private String getState(String authority, String resource) {
        String state = String.format("a=%s&r=%s", authority, resource);
        return Base64.encodeToString(state.getBytes(), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private void verifyTokenResult(TestIdToken TestIdToken, AuthenticationResult result) {
        assertEquals("Check access token", "TokenUserIdTest", result.getAccessToken());
        assertEquals("Check refresh token", "refresh112", result.getRefreshToken());
        if (TestIdToken != null) {
            assertEquals("Result has userid", TestIdToken.oid, result.getUserInfo().getUserId());
            assertEquals("Result has username", TestIdToken.upn, result.getUserInfo()
                    .getDisplayableId());
        }
    }

    public void testAcquireTokenSilentSync_Positive() throws NoSuchAlgorithmException,
            NoSuchPaddingException, NoSuchFieldException, IllegalAccessException,
            InterruptedException, ExecutionException, AuthenticationException {
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        setConnectionAvailable(context, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String json = "{\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);

        // Call refresh token in silent API method
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TEST_IDTOKEN_USERID);
        verifyRefreshTokenResponse(mockCache, null, result);

        clearCache(context);
    }

    public void testAcquireTokenSilentSync_Negative() throws NoSuchAlgorithmException,
            NoSuchPaddingException, NoSuchFieldException, IllegalAccessException,
            InterruptedException, ExecutionException, AuthenticationException {
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        setConnectionAvailable(context, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String responseBody = "{\"error\":\"invalid_grant\",\"error_description\":\"AADSTS70000: Authentication failed. Refresh Token is not valid.\r\nTrace ID: bb27293d-74e4-4390-882b-037a63429026\r\nCorrelation ID: b73106d5-419b-4163-8bc6-d2c18f1b1a13\r\nTimestamp: 2014-11-06 18:39:47Z\",\"error_codes\":[70000],\"timestamp\":\"2014-11-06 18:39:47Z\",\"trace_id\":\"bb27293d-74e4-4390-882b-037a63429026\",\"correlation_id\":\"b73106d5-419b-4163-8bc6-d2c18f1b1a13\",\"submit_url\":null,\"context\":null}";
        webrequest.setReturnResponse(new HttpWebResponse(400, responseBody.getBytes(), null));
        ReflectionUtils.setFieldValue(context, "mWebRequest", webrequest);

        // Call refresh token in silent API method

        try {
            context.acquireTokenSilentSync(null, "clientid", TEST_IDTOKEN_USERID);
            Assert.fail("Expected argument exception");
        } catch (IllegalArgumentException e) {
            assertTrue("Resource is missin", e.getMessage().contains("resource"));
        }

        try {
            context.acquireTokenSilentSync("resource", null, TEST_IDTOKEN_USERID);
            Assert.fail("Expected argument exception");
        } catch (IllegalArgumentException e) {
            assertTrue("Resource is missin", e.getMessage().contains("clientId"));
        }

        try {
            context.acquireTokenSilentSync("resource", "clientid", TEST_IDTOKEN_USERID);
        } catch (AuthenticationException e) {
            assertEquals("Token is not exchanged",
                    ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED, e.getCode());
        }

        // Verify call with displayableid also fails
        try {
            context.acquireTokenSilentSync("resource", "clientid", TEST_IDTOKEN_UPN);
        } catch (AuthenticationException e) {
            assertEquals("Token is not exchanged",
                    ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED, e.getCode());
        }

        clearCache(context);
    }

    private void verifyRefreshTokenResponse(ITokenCacheStore mockCache, Exception resultException,
            AuthenticationResult result) {
        assertNull("Error is null", resultException);
        assertEquals("Token is same", "TokenFortestRefreshTokenPositive", result.getAccessToken());
        assertNotNull("Cache is NOT empty for this userid for regular token",
                mockCache.getItem(CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId",
                        false, TEST_IDTOKEN_USERID)));
        assertNull("Cache is empty for multiresource token", mockCache.getItem(CacheKey
                .createCacheKey(VALID_AUTHORITY, "resource", "clientId", true, TEST_IDTOKEN_USERID)));
        assertNotNull("Cache is NOT empty for this userid for regular token",
                mockCache.getItem(CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId",
                        false, TEST_IDTOKEN_USERID)));
        assertTrue("Refresh token has userinfo",
                result.getUserInfo().getUserId().equalsIgnoreCase(TEST_IDTOKEN_USERID));
    }

    /**
     * authority and resource are case insensitive. Cache lookup will return
     * item from cache.
     *
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testAcquireTokenCacheLookup() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {

        FileMockContext mockContext = new FileMockContext(getContext());
        String tokenToTest = "accessToken=" + UUID.randomUUID();
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();
        addItemToCache(mockCache, tokenToTest, "refreshToken", VALID_AUTHORITY, resource,
                "clientId", "userId124", "name", "familyName", "userA", "tenantId", false);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // acquire token call will return from cache
        context.acquireToken(testActivity, resource, "ClienTid", "redirectUri", "userA",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback.mException);
        assertEquals("Same access token in cache", tokenToTest, callback.mResult.getAccessToken());
        assertEquals("Same refresh token in cache", "refreshToken",
                callback.mResult.getRefreshToken());
        assertEquals("Same userid in cache", "userId124", callback.mResult.getUserInfo()
                .getUserId());
        assertEquals("Same name in cache", "name", callback.mResult.getUserInfo().getGivenName());
        assertEquals("Same familyName in cache", "familyName", callback.mResult.getUserInfo()
                .getFamilyName());
        assertEquals("Same displayid in cache", "userA", callback.mResult.getUserInfo()
                .getDisplayableId());
        assertEquals("Same tenantid in cache", "tenantId", callback.mResult.getTenantId());
        clearCache(context);
    }

    @SmallTest
    public void testAcquireTokenCacheLookup_ReturnWrongUserId() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getContext());
        String resource = "Resource" + UUID.randomUUID();
        String clientId = "clientid" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();
        Calendar timeAhead = new GregorianCalendar();
        timeAhead.add(Calendar.MINUTE, 10);
        TokenCacheItem refreshItem = new TokenCacheItem();
        refreshItem.setAuthority(VALID_AUTHORITY);
        refreshItem.setResource(resource);
        refreshItem.setClientId(clientId);
        refreshItem.setAccessToken("token");
        refreshItem.setRefreshToken("refreshToken");
        refreshItem.setExpiresOn(timeAhead.getTime());
        refreshItem.setIsMultiResourceRefreshToken(false);
        UserInfo userinfo = new UserInfo("user2", "test", "test", "idp", "user2");
        refreshItem.setUserInfo(userinfo);
        String key = CacheKey.createCacheKey(VALID_AUTHORITY, resource, clientId, false, "user1");
        mockCache.setItem(key, refreshItem);
        TokenCacheItem item = mockCache.getItem(key);
        assertNotNull("item is in cache", item);

        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // Acquire token call will return from cache
        context.acquireTokenSilentAsync(resource, clientId, "user1", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNotNull("Error is not null", callback.mException);
        assertTrue(
                "Error is related to user mismatch",
                callback.mException.getMessage().contains(
                        "User returned by service does not match the one in the request"));
        clearCache(context);
    }

    @SmallTest
    public void testAcquireTokenCacheLookup_MultipleUser_LoginHint() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getContext());

        String resource = "Resource" + UUID.randomUUID();
        String clientId = "clientid" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();
        addItemToCache(mockCache, "token1", "refresh1", VALID_AUTHORITY, resource, clientId, "userid1", "userAname", "userAfamily" , "userName1", "tenant", false);
        addItemToCache(mockCache, "token2", "refresh2", VALID_AUTHORITY, resource, clientId, "userid2", "userBname", "userBfamily" , "userName2", "tenant", false);

        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        // User1
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // Acquire token call will return from cache
        context.acquireTokenSilentAsync(resource, clientId, "userid1", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback.mException);
        assertEquals("token for user1", "token1" , callback.mResult.getAccessToken());
        assertEquals("TestIdToken for user1", "userName1" , callback.mResult.getUserInfo().getDisplayableId());
        assertEquals("TestIdToken for user1", "userAname" , callback.mResult.getUserInfo().getGivenName());

        // User2 with userid call
        final CountDownLatch signal2 = new CountDownLatch(1);
        MockAuthenticationCallback callback2 = new MockAuthenticationCallback(signal2);

        // Acquire token call will return from cache
        context.acquireTokenSilentAsync(resource, clientId, "userid2", callback2);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback2.mException);
        assertEquals("token for user1", "token2" , callback2.mResult.getAccessToken());
        assertEquals("TestIdToken for user1", "userName2" , callback2.mResult.getUserInfo().getDisplayableId());

        // User2 with loginHint call
        final CountDownLatch signal3 = new CountDownLatch(1);
        MockAuthenticationCallback callback3 = new MockAuthenticationCallback(signal3);

        context.acquireToken(new MockActivity(null), resource, clientId, "http://redirectUri", "userName1", callback3);
        signal3.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback3.mException);
        assertEquals("token for user1", "token1" , callback3.mResult.getAccessToken());
        assertEquals("TestIdToken for user1", "userName1" , callback3.mResult.getUserInfo().getDisplayableId());

        clearCache(context);
    }


    @SmallTest
    public void testOnActivityResult_MissingIntentData() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext authContext = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, null);
        int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        int resultCode = AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE;
        TestLogResponse logResponse = new TestLogResponse();
        String msgToCheck = "onActivityResult BROWSER_FLOW data is null";
        logResponse.listenLogForMessageSegments(null, msgToCheck);

        // act
        authContext.onActivityResult(requestCode, resultCode, null);

        // assert
        assertTrue(logResponse.message.contains(msgToCheck));
    }

    @SmallTest
    public void testOnActivityResult_MissingCallbackRequestId() {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext authContext = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        int resultCode = AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE;
        Intent data = new Intent();
        data.putExtra("Test", "value");
        TestLogResponse logResponse = new TestLogResponse();
        String msgToCheck = "onActivityResult did not find waiting request for RequestId";
        logResponse.listenLogForMessageSegments(null, msgToCheck);

        // act
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertTrue(logResponse.message.contains(msgToCheck));
    }

    @SmallTest
    public void testOnActivityResult_ResultCode_Cancel() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        int resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL;
        TestAuthCallBack callback = new TestAuthCallBack();
        Intent data = setWaitingRequestToContext(authContext, callback);

        // act
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertTrue("Returns cancel error",
                callback.callbackException instanceof AuthenticationException);
        assertTrue("Cancel error has message",
                callback.callbackException.getMessage().contains("User cancelled the flow"));
    }

    private Intent setWaitingRequestToContext(final AuthenticationContext authContext,
            TestAuthCallBack callback) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object authRequestState = getRequestState(callback);
        Intent data = new Intent();
        data.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
        Method m = ReflectionUtils.getTestMethod(authContext, "putWaitingRequest", int.class,
                Class.forName("com.microsoft.aad.adal.AuthenticationRequestState"));
        m.invoke(authContext, callback.hashCode(), authRequestState);
        return data;
    }

    @SmallTest
    public void testOnActivityResult_ResultCode_Error() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        int resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR;
        TestAuthCallBack callback = new TestAuthCallBack();
        Intent data = setWaitingRequestToContext(authContext, callback);

        // act
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertTrue("Returns error", callback.callbackException instanceof AuthenticationException);
    }

    @SmallTest
    public void testOnActivityResult_ResultCode_Exception() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        int resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION;
        TestAuthCallBack callback = new TestAuthCallBack();
        Intent data = setWaitingRequestToContext(authContext, callback);
        AuthenticationException exception = new AuthenticationException(ADALError.AUTH_FAILED);
        data.putExtra(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION,
                (Serializable)exception);

        // act
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertTrue("Returns authentication exception",
                callback.callbackException instanceof AuthenticationException);
        assertTrue(
                "Returns authentication exception",
                ((AuthenticationException)callback.callbackException).getCode() == ADALError.AUTH_FAILED);
    }

    @SmallTest
    public void testOnActivityResult_ResultCode_ExceptionMissing() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        int resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION;
        TestAuthCallBack callback = new TestAuthCallBack();
        Intent data = setWaitingRequestToContext(authContext, callback);

        // act
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertTrue("Returns authentication exception",
                callback.callbackException instanceof AuthenticationException);
        assertTrue(
                "Returns authentication exception",
                ((AuthenticationException) callback.callbackException).getCode() == ADALError.WEBVIEW_RETURNED_INVALID_AUTHENTICATION_EXCEPTION);
    }

    @SmallTest
    public void testOnActivityResult_BrokerResponse() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        int resultCode = AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE;
        TestAuthCallBack callback = new TestAuthCallBack();
        Object authRequestState = getRequestState(callback);
        Intent data = new Intent();
        data.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
        data.putExtra(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN, "testAccessToken");
        Method m = ReflectionUtils.getTestMethod(authContext, "putWaitingRequest", int.class,
                Class.forName("com.microsoft.aad.adal.AuthenticationRequestState"));
        m.invoke(authContext, callback.hashCode(), authRequestState);

        // act
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertEquals("Same token in response", "testAccessToken",
                callback.callbackResult.getAccessToken());
    }

    private Object getRequestState(TestAuthCallBack callback) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException {
        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequestState");
        Class<?> c2 = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");
        Constructor<?> constructorParams = c.getDeclaredConstructor(int.class, c2,
                AuthenticationCallback.class);
        constructorParams.setAccessible(true);
        Object o = constructorParams.newInstance(callback.hashCode(), null, callback);
        return o;
    }

    class TestAuthCallBack implements AuthenticationCallback<AuthenticationResult> {

        public AuthenticationResult callbackResult;

        public Exception callbackException;

        @Override
        public void onSuccess(AuthenticationResult result) {
            callbackResult = result;
        }

        @Override
        public void onError(Exception exc) {
            callbackException = exc;
        }

    }

    /**
     * setup cache with userid for normal token and multiresource refresh token
     * bound to one userid. test calls for different resources and users.
     *
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    @SmallTest
    public void testAcquireTokenMultiResourceToken_UserId() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getContext());
        String tokenToTest = "accessToken=" + UUID.randomUUID();
        String tokenId = "id" + UUID.randomUUID().toString().replace("-", "");
        String tokenInfo = "accessToken" + tokenId;
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();
        addItemToCache(mockCache, tokenToTest, "refreshTokenNormal", VALID_AUTHORITY, resource,
                "ClienTid", TEST_IDTOKEN_USERID, "name", "familyName", TEST_IDTOKEN_UPN, "tenantId", false);
        addItemToCache(mockCache, "", "refreshTokenMultiResource", VALID_AUTHORITY, resource,
                "ClienTid", TEST_IDTOKEN_USERID, "name", "familyName", TEST_IDTOKEN_UPN, "tenantId", true);
        // only one MRRT for same user, client, authority
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        setConnectionAvailable(context, true);
        MockWebRequestHandler mockWebRequest = setMockWebRequest(context, tokenId, "refreshToken"
                + tokenId);

        CountDownLatch signal = new CountDownLatch(1);
        MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // Acquire token call will return from cache
        context.acquireToken(testActivity, resource, "ClienTid", "redirectUri", TEST_IDTOKEN_UPN, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNull("Error is null", callback.mException);
        assertEquals("Same token in response as in cache", tokenToTest,
                callback.mResult.getAccessToken());

        // Acquire token call will not return from cache for broad
        // Token-cached item does not have access token since it was broad
        // refresh token
        signal = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, "dummyResource2", "ClienTid", "redirectUri", TEST_IDTOKEN_UPN,
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNull("Error is null", callback.mException);
        assertEquals("Same token as refresh token result", tokenInfo,
                callback.mResult.getAccessToken());

        // Different resource with same userid
        signal = new CountDownLatch(1);
        testActivity = new MockActivity(signal);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, "anotherResource123", "ClienTid", "redirectUri", TEST_IDTOKEN_UPN,
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertEquals("Token is returned from refresh token request", tokenInfo,
                callback.mResult.getAccessToken());
        assertFalse("Multiresource is not set in the mocked response",
                callback.mResult.getIsMultiResourceRefreshToken());
        assertTrue("Request to get token uses broad refresh token", mockWebRequest
                .getRequestContent().contains(tokenId));

        // Same call again to use it from cache
        signal = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal);
        callback.mResult = null;
        removeMockWebRequest(context);
        context.acquireToken(testActivity, "anotherResource123", "ClienTid", "redirectUri", TEST_IDTOKEN_UPN,
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertEquals("Same token in response as in cache for same call", tokenInfo,
                callback.mResult.getAccessToken());

        // Empty userid will prompt.
        // Items are linked to userid. If it is not there, it can't use for
        // refresh or access token.
        signal = new CountDownLatch(1);
        testActivity = new MockActivity(signal);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, resource, "ClienTid", "redirectUri", "", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNull("Result is null since it tries to start activity", callback.mResult);
        assertEquals("Activity was attempted to start.",
                AuthenticationConstants.UIRequest.BROWSER_FLOW,
                testActivity.mStartActivityRequestCode);

        clearCache(context);
    }

    @SmallTest
    public void testAcquireTokenMultiResource_ADFSIssue() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        // adfs does not return userid and multiresource token
        FileMockContext mockContext = new FileMockContext(getContext());
        String tokenToTest = "accessToken=" + UUID.randomUUID();
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();
        // add item without userid and normal refresh token
        addItemToCache(mockCache, tokenToTest, "refreshToken", VALID_AUTHORITY, resource,
                "ClienTid", "", "name", "familyName", "userA", "tenantId", false);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        MockActivity testActivity = new MockActivity();
        CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // Acquire token call will return from cache
        context.acquireToken(testActivity, resource, "clientid", "redirectUri", "userA", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback.mException);
        assertEquals("Same token in response as in cache", tokenToTest,
                callback.mResult.getAccessToken());

        // Request with different resource will result in prompt since Cache
        // does not have multi resource token
        signal = new CountDownLatch(1);
        testActivity = new MockActivity();
        testActivity.mSignal = signal;
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, "anotherResource123", "ClienTid", "redirectUri", "",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertTrue("Attemps to launch", testActivity.mStartActivityRequestCode != -1);

        // Asking with different userid will not return item from cache and try
        // to launch activity
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

    @SmallTest
    public void testBrokerRedirectUri() throws UnsupportedEncodingException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final AuthenticationContext authContext = new AuthenticationContext(getContext(),
                VALID_AUTHORITY, false, cache);

        // act
        String actual = authContext.getRedirectUriForBroker();

        // assert
        assertTrue("should have packagename", actual.contains(TEST_PACKAGE_NAME));
        assertTrue("should have signature url encoded",
                actual.contains(URLEncoder.encode(testTag, AuthenticationConstants.ENCODING_UTF8)));
    }

    private AuthenticationContext getAuthenticationContext(Context mockContext, String authority,
            boolean validate, ITokenCacheStore mockCache) {
        AuthenticationContext context = new AuthenticationContext(mockContext, authority, validate,
                mockCache);
        Class<?> c;
        try {
            c = Class.forName("com.microsoft.aad.adal.BrokerProxy");
            Constructor<?> constructorParams = c.getDeclaredConstructor(Context.class);
            constructorParams.setAccessible(true);
            Object brokerProxy = constructorParams.newInstance(mockContext);
            ReflectionUtils.setFieldValue(brokerProxy, "mBrokerTag", "invalid");
            ReflectionUtils.setFieldValue(context, "mBrokerProxy", brokerProxy);
        } catch (ClassNotFoundException e) {
            Assert.fail("getAuthenticationContext:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            Assert.fail("getAuthenticationContext:" + e.getMessage());
        } catch (IllegalArgumentException e) {
            Assert.fail("getAuthenticationContext:" + e.getMessage());
        } catch (InstantiationException e) {
            Assert.fail("getAuthenticationContext:" + e.getMessage());
        } catch (IllegalAccessException e) {
            Assert.fail("getAuthenticationContext:" + e.getMessage());
        } catch (InvocationTargetException e) {
            Assert.fail("getAuthenticationContext:" + e.getMessage());
        } catch (NoSuchFieldException e) {
            Assert.fail("getAuthenticationContext:" + e.getMessage());
        }

        return context;
    }
    
    @SmallTest
    public void testVerifyBrokerRedirectUri_valid() throws NoSuchAlgorithmException, NoSuchPaddingException, 
           IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, 
           IllegalAccessException, InvocationTargetException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final AuthenticationContext authContext = new AuthenticationContext(getContext(),
                VALID_AUTHORITY, false, cache);
        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");
        Method m = ReflectionUtils.getTestMethod(authContext, "verifyBrokerRedirectUri", c);

        //test@case valid redirect uri
        String testRedirectUri = authContext.getRedirectUriForBroker();
        Object authRequest = AuthenticationContextTest.createAuthenticationRequest(VALID_AUTHORITY, 
            "resource", "clientid", testRedirectUri, "loginHint");
        Boolean testResult = (Boolean)m.invoke(authContext, authRequest);
        assertTrue(testResult);
    }

    @SmallTest
    public void testVerifyBrokerRedirectUri_invalidPrefix() throws NoSuchAlgorithmException, NoSuchPaddingException, 
           IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, 
           IllegalAccessException, InvocationTargetException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final AuthenticationContext authContext = new AuthenticationContext(getContext(),
                VALID_AUTHORITY, false, cache);
        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");
        Method m = ReflectionUtils.getTestMethod(authContext, "verifyBrokerRedirectUri", c);

        //test@case broker redirect uri with invalid prefix
        try {
            String testRedirectUri = "http://helloApp";
            Object authRequest = AuthenticationContextTest.createAuthenticationRequest(VALID_AUTHORITY, 
                    "resource", "clientid", testRedirectUri, "loginHint");
            m.invoke(authContext, authRequest);
            Assert.fail("It is expected to return an exception here.");
        } catch(InvocationTargetException e) {
            assertTrue(e.getCause() instanceof UsageAuthenticationException);
            assertEquals(ADALError.DEVELOPER_REDIRECTURI_INVALID,((UsageAuthenticationException)e.getCause()).getCode());
            assertTrue((e.getCause()).getMessage().toString().contains("prefix"));
        }
    }

    @SmallTest
    public void testVerifyBrokerRedirectUri_invalidPackageName() throws NoSuchAlgorithmException, NoSuchPaddingException, 
           IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, 
           IllegalAccessException, InvocationTargetException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final AuthenticationContext authContext = new AuthenticationContext(getContext(),
                VALID_AUTHORITY, false, cache);
        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");
        Method m = ReflectionUtils.getTestMethod(authContext, "verifyBrokerRedirectUri", c);

        //test@case broker redirect uri with invalid packageName
        try {
             String testRedirectUri = "msauth://testapp/gwdiktUBDmQq%2BfbWiJoa%2B%2FYH070%3D";
             Object authRequest = AuthenticationContextTest.createAuthenticationRequest(VALID_AUTHORITY, 
                     "resource", "clientid", testRedirectUri, "loginHint");
             m.invoke(authContext, authRequest);
             Assert.fail("It is expected to return an exception here.");
        } catch(InvocationTargetException e) {
            assertTrue(e.getCause() instanceof UsageAuthenticationException);
            assertEquals(ADALError.DEVELOPER_REDIRECTURI_INVALID,((UsageAuthenticationException)e.getCause()).getCode());
            assertTrue((e.getCause()).getMessage().toString().contains("package name"));
        }
    }

    @SmallTest
    public void testVerifyBrokerRedirectUri_invalidSignature() throws NoSuchAlgorithmException, NoSuchPaddingException, 
           IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, 
        IllegalAccessException, InvocationTargetException {
        ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final AuthenticationContext authContext = new AuthenticationContext(getContext(),
                VALID_AUTHORITY, false, cache);
        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");
        Method m = ReflectionUtils.getTestMethod(authContext, "verifyBrokerRedirectUri", c);

        //test@case broker redirect uri with invalid signature
        try {
        String testRedirectUri = "msauth://" + getContext().getPackageName() + "/falsesignH070%3D";
        Object authRequest = AuthenticationContextTest.createAuthenticationRequest(VALID_AUTHORITY, 
                "resource", "clientid", testRedirectUri, "loginHint");
        m.invoke(authContext, authRequest);
        Assert.fail("It is expected to return an exception here.");
        } catch(InvocationTargetException e) {
            assertTrue(e.getCause() instanceof UsageAuthenticationException);
            assertEquals(ADALError.DEVELOPER_REDIRECTURI_INVALID,((UsageAuthenticationException)e.getCause()).getCode());
            assertTrue((e.getCause()).getMessage().toString().contains("signature"));
        }
    }

    private ITokenCacheStore getCacheForRefreshToken(String userId, String displayableId) {
        DefaultTokenCacheStore cache = new DefaultTokenCacheStore(getContext());
        cache.removeAll();
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
        refreshItem.setUserInfo(new UserInfo(userId, "givenName", "familyName",
                "identityProvider", displayableId));
        cache.setItem(
                CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId", false, userId),
                refreshItem);
        cache.setItem(
                CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId", false, displayableId),
                refreshItem);
        return cache;
    }
    
    private ITokenCacheStore getCacheWithFamilyIdForRefreshToken(String userId, String displayableId) 
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        DefaultTokenCacheStore cache = new DefaultTokenCacheStore(getContext());
        cache.removeAll();
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
        refreshItem.setUserInfo(new UserInfo(userId, "givenName", "familyName",
            "identityProvider", displayableId));
        refreshItem.setFamilyClientId("familyClientId");
        cache.setItem(
            CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId", false, userId),
            refreshItem);
        cache.setItem(
            CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId", false, displayableId),
            refreshItem);
        return cache;
    }

    private ITokenCacheStore getMockCache(int minutes, String token, String resource,
            String client, String user, boolean isMultiResource) throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        DefaultTokenCacheStore cache = new DefaultTokenCacheStore(getContext());
        // Code response
        Calendar timeAhead = new GregorianCalendar();
        Log.d("Test", "Time now:" + timeAhead.toString());
        timeAhead.add(Calendar.MINUTE, minutes);
        TokenCacheItem refreshItem = new TokenCacheItem();
        refreshItem.setAuthority(VALID_AUTHORITY);
        refreshItem.setResource(resource);
        refreshItem.setClientId(client);
        refreshItem.setAccessToken(token);
        refreshItem.setRefreshToken("refreshToken=");
        refreshItem.setExpiresOn(timeAhead.getTime());
        refreshItem.setUserInfo(new UserInfo(user, "", "", "", user));
        cache.setItem(
                CacheKey.createCacheKey(VALID_AUTHORITY, resource, client, isMultiResource, user),
                refreshItem);
        return cache;
    }

    private ITokenCacheStore addItemToCache(ITokenCacheStore cache, String token,
            String refreshToken, String authority, String resource, String clientId, String userId,
            String name, String familyName, String displayId, String tenantId,
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
        refreshItem.setTenantId(tenantId);
        refreshItem.setUserInfo(new UserInfo(userId, name, familyName, "", displayId));
        String key = CacheKey.createCacheKey(VALID_AUTHORITY, resource, clientId, isMultiResource,
                userId);
        Log.d(TAG, "Key: " + key);
        cache.setItem(key, refreshItem);
        TokenCacheItem item = cache.getItem(key);
        assertNotNull("item is in cache", item);

        key = CacheKey.createCacheKey(VALID_AUTHORITY, resource, clientId, isMultiResource,
                displayId);
        Log.d(TAG, "Key: " + key);
        cache.setItem(key, refreshItem);
        item = cache.getItem(key);
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

        @Override
        public Iterator<TokenCacheItem> getAll() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    class MockDiscovery implements IDiscovery {

        private boolean isValid = false;

        private URL authorizationUrl;

        private UUID correlationId;

        MockDiscovery(boolean validFlag) {
            isValid = validFlag;
        }

        @Override
        public boolean isValidAuthority(URL authorizationEndpoint) {
            authorizationUrl = authorizationEndpoint;
            return isValid;
        }

        public URL getAuthorizationUrl() {
            return authorizationUrl;
        }

        @Override
        public void setCorrelationId(UUID requestCorrelationId) {
            correlationId = requestCorrelationId;
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

        public MockActivity(CountDownLatch signal) {
            mSignal = signal;
        }

        @SuppressLint("Registered")
        public MockActivity() {
            // TODO Auto-generated constructor stub
        }

        @Override
        public String getPackageName() {
            return ReflectionUtils.TEST_PACKAGE_NAME;
        }

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            Log.d(TAG, "startActivityForResult:" + requestCode);
            mStartActivityIntent = intent;
            mStartActivityRequestCode = requestCode;
            // test call needs to stop the tests at this point. If it reaches
            // here, it means authenticationActivity was attempted to launch.
            // Since it is mock activity, it will not launch something.
            if (mSignal != null)
                mSignal.countDown();
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
}
