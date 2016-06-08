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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.mockito.Mockito;

import com.google.gson.Gson;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

    protected final static int CONTEXT_REQUEST_TIME_OUT = 20000;

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

    @Override
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

    @Override
    protected void tearDown() throws Exception {
        HttpUrlConnectionFactory.mockedConnection = null;
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

    public void testConstructorNoCache() throws UsageAuthenticationException {
        String authority = "https://github.com/MSOpenTech";
        AuthenticationContext context = new AuthenticationContext(getContext(), authority, false,
                null);
        assertNull(context.getCache());
    }

    public void testConstructorWithCache() throws NoSuchAlgorithmException, NoSuchPaddingException, UsageAuthenticationException {
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
        mockContext.setPermission("android.permission.INTERNET",PackageManager.PERMISSION_GRANTED);

        // no exception
        new AuthenticationContext(mockContext, authority, false);

        try {
            mockContext.setPermission("android.permission.INTERNET",PackageManager.PERMISSION_DENIED);
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
    public void testCorrelationId_InWebRequest() throws InterruptedException {

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

        UUID requestCorrelationId = UUID.randomUUID();
        Log.d(TAG, "test correlationId:" + requestCorrelationId.toString());
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final TestLogResponse response = new TestLogResponse();
        response.listenLogForMessageSegments("Authentication failed", "correlation_id:\"\""
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
        assertEquals("Redirect uri is same as package", TEST_PACKAGE_NAME, redirect);
    }

    @SmallTest
    public void testPrompt() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, ClassNotFoundException, NoSuchAlgorithmException,
            NoSuchPaddingException, InterruptedException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);

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
        mockContext.isConnectionAvaliable = false;
        
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        CountDownLatch signal = new CountDownLatch(1);
        final MockAuthenticationCallback mockCallback = new MockAuthenticationCallback(signal);
        context.acquireTokenByRefreshToken("refresh", "clientId", "resource", mockCallback);

        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        assertTrue("Exception type", mockCallback.mException instanceof AuthenticationException);
        assertEquals("Connection related error code", ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE,
                ((AuthenticationException)mockCallback.mException).getCode());
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
     * @throws AuthenticationException 
     * @throws IOException 
     */
    @SmallTest
    public void testAcquireTokenByRefreshTokenPositive() throws IOException, InterruptedException  {
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        final CountDownLatch signal = new CountDownLatch(1);

        String expectedClientId = "client" + UUID.randomUUID().toString();
        String exptedResource = "resource" + UUID.randomUUID().toString();
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getSuccessTokenResponse(false, false)), 
                Util.createInputStream(Util.getSuccessTokenResponse(true, true)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);

        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", "I am a new access token", callback.mResult.getAccessToken());
        assertEquals("Same refresh token", "I am a new refresh token", callback.mResult.getRefreshToken());

        final CountDownLatch signal2 = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal2);
        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, exptedResource,
                callback);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", "I am a new access token", callback.mResult.getAccessToken());
        assertEquals("Same refresh token", "I am a new refresh token", callback.mResult.getRefreshToken());

        assertNotNull("Result has user info from idtoken", callback.mResult.getUserInfo());
        assertEquals("Result has user info from idtoken", TEST_IDTOKEN_UPN,
                callback.mResult.getUserInfo().getDisplayableId());
    }

    public void testAcquireTokenByRefreshToken_NotReturningRefreshToken() throws IOException, InterruptedException {
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final CountDownLatch signal = new CountDownLatch(1);
        String expectedClientId = "client" + UUID.randomUUID().toString();
        String refreshToken = "refreshTokenSending";
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getSuccessResponseWithoutRefreshToken()));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);
        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", "I am a new access token", callback.mResult.getAccessToken());
        assertEquals("Same refresh token", refreshToken, callback.mResult.getRefreshToken());
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
                ((AuthenticationException)callback.mException).getCode());
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
     * acquire token using refresh token. All web calls are mocked. Refresh
     * token response must match to result and cache.
     */
    @SmallTest
    public void testRefreshTokenPositive() throws IOException, InterruptedException, AuthenticationException {

        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final String response = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\",\"expires_in\":\"-10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";

        final String response2 = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenReturnsWithIdToken\",\"token_type\":\"Bearer\",\"expires_in\":\"10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshABC\",\"scope\":\"*\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response), 
                Util.createInputStream(response2));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);

        // Call acquire token which will try refresh token based on cache
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        verifyRefreshTokenResponse(mockCache, callback.mException, callback.mResult);

        // Do silent token request and return idtoken in the result
        
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TEST_IDTOKEN_USERID);
        assertEquals("Access Token", "TokenReturnsWithIdToken", result.getAccessToken());
        assertEquals("Refresh Token", "refreshABC", result.getRefreshToken());
        assertEquals("IdToken", TEST_IDTOKEN, result.getIdToken());
        clearCache(context);
    }

    @SmallTest
    public void testScenario_UserId_LoginHint_Use() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InvocationTargetException, NoSuchAlgorithmException, NoSuchPaddingException,
            AuthenticationException, IOException {
        scenario_UserId_LoginHint("test@user.com", "test@user.com", "test@user.com");
    }

    private void scenario_UserId_LoginHint(String idTokenUpn, String responseIntentHint,
            String acquireTokenHint) throws InterruptedException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException,
            NoSuchAlgorithmException, NoSuchPaddingException, AuthenticationException, IOException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        context.getCache().removeAll();

        final CountDownLatch signal = new CountDownLatch(1);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);
        MockedIdToken idtoken = new MockedIdToken();
        idtoken.upn = idTokenUpn;
        idtoken.oid = "userid123";
        final String response = "{\"id_token\":\""
                + idtoken.getIdToken()
                + "\",\"access_token\":\"TokenUserIdTest\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response), 
                Util.createInputStream(Util.getSuccessTokenResponse(true, true)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);
        
        Intent intent = getResponseIntent(callback, "resource", "clientid", "redirectUri",
                responseIntentHint);

        // Get token from onActivityResult after Activity returns
        tokenWithAuthenticationActivity(context, testActivity, signal, signalCallback, intent,
                "resource", "clientid", "redirectUri", acquireTokenHint, callback);

        // Token will return to callback with idToken
        verifyTokenResult(idtoken, callback.mResult);

        // Same call should get token from cache
        final CountDownLatch signalCallback2 = new CountDownLatch(1);
        callback.mSignal = signalCallback2;
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", acquireTokenHint,
                callback);
        signalCallback2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        verifyTokenResult(idtoken, callback.mResult);

        // Call with userId should return from cache as well
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                idtoken.oid);
        verifyTokenResult(idtoken, result);

        clearCache(context);
    }

    @SmallTest
    public void testScenario_NullUser_IdToken() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InvocationTargetException, NoSuchAlgorithmException, NoSuchPaddingException,
            AuthenticationException, IOException {
        scenario_UserId_LoginHint("test@user.com", "", "");
    }

    @SmallTest
    public void testScenario_LoginHint_IdToken_Different() throws IOException, IllegalArgumentException, ClassNotFoundException, 
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, InterruptedException, AuthenticationException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        context.getCache().removeAll();

        final CountDownLatch signal = new CountDownLatch(1);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);
        MockedIdToken idtoken = new MockedIdToken();
        idtoken.upn = "admin@user.com";
        idtoken.oid = "admin123";
        String loginHint = "user1@user.com";
        final String response = "{\"id_token\":\""
                + idtoken.getIdToken()
                + "\",\"access_token\":\"TokenUserIdTest\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response), 
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);
        
        Intent intent = getResponseIntent(callback, "resource", "clientid", "redirectUri",
                loginHint);

        // Get token from onActivityResult after Activity returns
        tokenWithAuthenticationActivity(context, testActivity, signal, signalCallback, intent,
                "resource", "clientid", "redirectUri", loginHint, callback);

        // Token will return to callback with idToken
        verifyTokenResult(idtoken, callback.mResult);

        // Same call with correct upn will return from cache
        final CountDownLatch signalCallback2 = new CountDownLatch(1);
        callback.mSignal = signalCallback2;
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri", loginHint, callback);
        signalCallback2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        verifyTokenResult(idtoken, callback.mResult);

        // Call with userId should return from cache as well
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                idtoken.oid);
        verifyTokenResult(idtoken, result);

        clearCache(context);
    }

    @SmallTest
    public void testScenario_Empty_IdToken() throws InterruptedException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException,
            NoSuchAlgorithmException, NoSuchPaddingException, AuthenticationException, IOException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        context.getCache().removeAll();

        final CountDownLatch signal = new CountDownLatch(1);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);

        final String response = "{\"access_token\":\"TokenUserIdTest\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response), 
                Util.createInputStream(Util.getSuccessTokenResponse(true, true)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);
        
        Intent intent = getResponseIntent(callback, "resource", "clientid", "redirectUri", null);

        // Get token from onActivityResult after Activity returns
        tokenWithAuthenticationActivity(context, testActivity, signal, signalCallback, intent,
                "resource", "clientid", "redirectUri", null, callback);

        // Token will return to callback with idToken
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
     * @throws IOException 
     * @throws AuthenticationException 
     */
    @SmallTest
    public void testFamilyClientIdCorrectlyStoredInCache() throws IOException, InterruptedException, AuthenticationException {

        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final String response = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\",\"expires_in\":\"-10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\",\"foci\":\"1\"}";
        // response2 has FoCI as "familyClientId"
        final String response2 = Util.getSuccessTokenResponse(true, true);
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response), 
                Util.createInputStream(response2));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);

        // Call acquire token which will try refresh token based on cache
        context.acquireToken(testActivity, "resource", "clientid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        verifyRefreshTokenResponse(mockCache, callback.mException, callback.mResult);
        verifyFamilyIdStoredInTokenCacheItem(mockCache, 
                CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId", TEST_IDTOKEN_UPN), "1");

        // Do silent token request and return idtoken in the result
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TEST_IDTOKEN_USERID);
        assertEquals("Returned assess token is not as expected.", "I am a new access token", result.getAccessToken());
        assertEquals("Returned refresh token is not as expected.", "I am a new refresh token", result.getRefreshToken());
        assertEquals("Returned id token is not as expected.", TEST_IDTOKEN, result.getIdToken());
        verifyFamilyIdStoredInTokenCacheItem(mockCache, CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId",
                        TEST_IDTOKEN_UPN), "familyClientId");
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
                (Serializable)authRequest);
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
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

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

    private void verifyTokenResult(MockedIdToken idtoken, AuthenticationResult result) {
        assertEquals("Check access token", "TokenUserIdTest", result.getAccessToken());
        assertEquals("Check refresh token", "refresh112", result.getRefreshToken());
        if (idtoken != null) {
            assertEquals("Result has userid", idtoken.oid, result.getUserInfo().getUserId());
            assertEquals("Result has username", idtoken.upn, result.getUserInfo()
                    .getDisplayableId());
        }
    }

    /**
     * Test acquire token silent sync call. 
     * @throws IOException 
     * @throws InterruptedException 
     * @throws AuthenticationException 
     */
    @SmallTest
    public void testAcquireTokenSilentSync_Positive() throws IOException, AuthenticationException, InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        final String response = "{\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);

        // Call refresh token in silent API method
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TEST_IDTOKEN_USERID);
        verifyRefreshTokenResponse(mockCache, null, result);

        clearCache(context);
    }

    public void testAcquireTokenSilentSync_Negative() throws NoSuchAlgorithmException,
            NoSuchPaddingException, NoSuchFieldException, IllegalAccessException,
            InterruptedException, ExecutionException, AuthenticationException, IOException {
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        String responseBody = "{\"error\":\"invalid_grant\",\"error_description\":\"AADSTS70000: Authentication failed. Refresh Token is not valid.\r\nTrace ID: bb27293d-74e4-4390-882b-037a63429026\r\nCorrelation ID: b73106d5-419b-4163-8bc6-d2c18f1b1a13\r\nTimestamp: 2014-11-06 18:39:47Z\",\"error_codes\":[70000],\"timestamp\":\"2014-11-06 18:39:47Z\",\"trace_id\":\"bb27293d-74e4-4390-882b-037a63429026\",\"correlation_id\":\"b73106d5-419b-4163-8bc6-d2c18f1b1a13\",\"submit_url\":null,\"context\":null}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(responseBody), 
                Util.createInputStream(Util.getSuccessTokenResponse(true, true)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(400);

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
                mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId", TEST_IDTOKEN_USERID)));
        assertNull("Cache is empty for multiresource token", mockCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, 
                "clientId", TEST_IDTOKEN_USERID)));
        assertNotNull("Cache is NOT empty for this userid for regular token",
                mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId", TEST_IDTOKEN_USERID)));
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

    /**
     * Test for verifying the userid in the request is different from what's in the cache. 
     */
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
        String key = CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, "user1");
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

        assertNotNull("Error is not null", callback.mException);
        assertTrue(
                "Error is related to user mismatch",
                callback.mException.getMessage().contains(
                        "User returned by service does not match the one in the request"));
        clearCache(context);
    }
    
    /**
     * Test when there is no user id passed in request, if there is a token cache item existed in the cache, we'll return it.
     */
    @SmallTest
    public void testAcquireTokenNoUserPassedIn() throws InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        
        // set up cache
        mockCache.removeAll();
        final String resource = "resource";
        final String clientId = "clientId";
        final TokenCacheItem tokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final Calendar timeAhead = new GregorianCalendar();
        timeAhead.add(Calendar.MINUTE, 10);
        tokenCacheItem.setExpiresOn(timeAhead.getTime());
        
        // Store the key without userid into cache
        mockCache.setItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, null), tokenCacheItem);
        
        final AuthenticationContext context = getAuthenticationContext(mContext, VALID_AUTHORITY, false, mockCache);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        
        // Acquire token call will return from cache
        context.acquireTokenSilentAsync(resource, clientId, null, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        
        assertNull(callback.mException);
        assertNotNull(callback.mResult);
        assertNotNull(callback.mResult.getAccessToken());
    }

    @SmallTest
    public void testAcquireTokenCacheLookup_MultipleUser_LoginHint() throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final String resource = "Resource" + UUID.randomUUID();
        final String clientId = "clientid" + UUID.randomUUID();
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
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
        assertEquals("idtoken for user1", "userName1" , callback.mResult.getUserInfo().getDisplayableId());
        assertEquals("idtoken for user1", "userAname" , callback.mResult.getUserInfo().getGivenName());

        // User2 with userid call
        final CountDownLatch signal2 = new CountDownLatch(1);
        MockAuthenticationCallback callback2 = new MockAuthenticationCallback(signal2);

        // Acquire token call will return from cache
        context.acquireTokenSilentAsync(resource, clientId, "userid2", callback2);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback2.mException);
        assertEquals("token for user1", "token2" , callback2.mResult.getAccessToken());
        assertEquals("idtoken for user1", "userName2" , callback2.mResult.getUserInfo().getDisplayableId());

        // User2 with loginHint call
        final CountDownLatch signal3 = new CountDownLatch(1);
        MockAuthenticationCallback callback3 = new MockAuthenticationCallback(signal3);

        final MockActivity testActivity = new MockActivity();
        testActivity.mSignal = signal3;
        context.acquireToken(testActivity, resource, clientId, "http://redirectUri", "userName1", callback3);
        signal3.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback3.mException);
        assertEquals("token for user1", "token1" , callback3.mResult.getAccessToken());
        assertEquals("idtoken for user1", "userName1" , callback3.mResult.getUserInfo().getDisplayableId());

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
        logResponse.listenLogForMessageSegments(msgToCheck);

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
        logResponse.listenLogForMessageSegments(msgToCheck);

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
                exception);

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
                ((AuthenticationException)callback.callbackException).getCode() == ADALError.WEBVIEW_RETURNED_INVALID_AUTHENTICATION_EXCEPTION);
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
     * @throws IOException 
     * @throws InterruptedException 
     */
    @SmallTest
    public void testAcquireTokenMultiResourceToken_UserId() throws IOException, InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        final String expectedAT = "accesstoken";
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

        final String response = "{\"access_token\":\"accesstoken"
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"29344\",\"expires_on\":\"1368768616\",\"refresh_token\":\""
                + "refreshToken" + "\",\"scope\":\"*\",\"id_token\":\"" + TEST_IDTOKEN + "\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response), 
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);

        CountDownLatch signal = new CountDownLatch(1);
        MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // 1st token request, read from cache. 
        context.acquireToken(testActivity, resource, "ClienTid", "redirectUri", TEST_IDTOKEN_UPN, callback);
        signal.await(200000, TimeUnit.MILLISECONDS);
        assertNull("Error is null", callback.mException);
        assertEquals("Same token in response as in cache", tokenToTest,
                callback.mResult.getAccessToken());

        // 2nd token request, use MRRT to refresh
        signal = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, "dummyResource2", "ClienTid", "redirectUri", TEST_IDTOKEN_UPN,
                callback);
        signal.await(200000, TimeUnit.MILLISECONDS);

        assertNull("Error is null", callback.mException);
        assertEquals("Same token as refresh token result", expectedAT,
                callback.mResult.getAccessToken());

        // 3rd request, different resource with same userid
        signal = new CountDownLatch(1);
        testActivity = new MockActivity(signal);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity, "anotherResource123", "ClienTid", "redirectUri", TEST_IDTOKEN_UPN,
                callback);
        signal.await(200000, TimeUnit.MILLISECONDS);

        assertEquals("Token is returned from refresh token request", expectedAT,
                callback.mResult.getAccessToken());
        assertFalse("Multiresource is not set in the mocked response",
                callback.mResult.getIsMultiResourceRefreshToken());

        // Same call again to use it from cache
        signal = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal);
        callback.mResult = null;
        HttpUrlConnectionFactory.mockedConnection = null;
        context.acquireToken(testActivity, "anotherResource123", "ClienTid", "redirectUri", TEST_IDTOKEN_UPN,
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertEquals("Same token in response as in cache for same call", expectedAT,
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

     @SmallTest
     public void testAutoFlow_RefreshTokenRequestFailed_WithOauthError() throws IOException, InterruptedException, 
             IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, 
             IllegalAccessException, InvocationTargetException {        
         FileMockContext mockContext = new FileMockContext(getContext());
         ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
         final AuthenticationContext context = getAuthenticationContext(mockContext,
                 VALID_AUTHORITY, false, mockCache);
         final CountDownLatch signal = new CountDownLatch(1);
         final MockActivity testActivity = new MockActivity(signal);
         final CountDownLatch signalCallback = new CountDownLatch(1);
         MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);

         final String responseBody = getErrorResponseBody("interaction_required");
         final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
         HttpUrlConnectionFactory.mockedConnection = mockedConnection;
         Util.prepareMockedUrlConnection(mockedConnection);
         Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
         Mockito.when(mockedConnection.getInputStream()).thenThrow(new IOException());
         Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(responseBody));
         Mockito.when(mockedConnection.getResponseCode()).thenReturn(400);

         context.acquireToken(testActivity, "resource", "clientId", "redirect", TEST_IDTOKEN_UPN,
                 callback);

         signal.await(200000, TimeUnit.MILLISECONDS);

         // Activity will start
         assertEquals("Activity was attempted to start.",
                 AuthenticationConstants.UIRequest.BROWSER_FLOW,
                 testActivity.mStartActivityRequestCode);

         context.onActivityResult(testActivity.mStartActivityRequestCode,
                 AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, getResponseIntent(callback, "resource", "clientid", "redirect", TEST_IDTOKEN_UPN));
         signalCallback.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
     }

     /**
      * Add a FoCI token item into the mocked cache store
      * @param mockCache
      */
     private void addFRTCacheItem(DefaultTokenCacheStore mockCache) {
        final UserInfo user2 = new UserInfo(TEST_IDTOKEN_USERID, "givenName", "familyName", "identity",
                TEST_IDTOKEN_USERID);
        final TokenCacheItem testFamilyRefreshTokenItemUser2 = new TokenCacheItem();
        testFamilyRefreshTokenItemUser2.setAccessToken("");
        testFamilyRefreshTokenItemUser2.setIsMultiResourceRefreshToken(true);
        testFamilyRefreshTokenItemUser2.setAuthority(TEST_AUTHORITY);
        testFamilyRefreshTokenItemUser2.setUserInfo(user2);
        testFamilyRefreshTokenItemUser2.setFamilyClientId("1");
        testFamilyRefreshTokenItemUser2.setRefreshToken("FRT");
        testFamilyRefreshTokenItemUser2.setClientId("1");
        testFamilyRefreshTokenItemUser2.setRawIdToken(TEST_IDTOKEN);
        mockCache.setItem(CacheKey.createCacheKeyForFRT(testFamilyRefreshTokenItemUser2.getAuthority(),
                AuthenticationConstants.MS_FAMILY_ID, TEST_IDTOKEN_USERID), testFamilyRefreshTokenItemUser2);
        }

     /**
      * Test the serialize() function
      * where the cache store does not have the FoCI token for the user
      * the function is expected to return null
      *
      * @throws AuthenticationException
      */
    @SmallTest
    public void testSerialize_nullCacheItem() throws AuthenticationException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        this.clearCache(context);
        try {
            context.serialize(TEST_IDTOKEN_USERID);
            Assert.fail("not expected");
        } catch (final Exception exception) {
            assertTrue(exception instanceof UsageAuthenticationException);
        }
    }

    /**
     * Test the serialize() function where the input userID is blank or null the
     * function is expected to throw the IllegalArgumentException
     */
    @SmallTest
    public void testSerialize_invalidUserId() {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        addFRTCacheItem(mockCache);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        try {
            final String jsonString = context.serialize("");
            Assert.fail("not expected");
        } catch (final Exception exception) {
            assertTrue(exception instanceof IllegalArgumentException);
        }

        try {
            final String jsonString = context.serialize(null);
            Assert.fail("not expected");
        } catch (final Exception exception) {
            assertTrue(exception instanceof IllegalArgumentException);
        }
    }

    /**
     * Test the serialize() function where the cache store has the FoCI token
     * for the user the function is expected to return the serialized string of
     * the BlobContainer object which contain the FoCI token cache item for the
     * user
     */
    @SmallTest
    public void testSerialize_valid() throws AuthenticationException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        addFRTCacheItem(mockCache);
        final AuthenticationContext context = getAuthenticationContext(mockContext, TEST_AUTHORITY, false, mockCache);
        final String result = context.serialize(TEST_IDTOKEN_USERID);
        assertTrue(null != context.serialize(TEST_IDTOKEN_USERID));
    }

    /**
     * Test the deserialize() function with a valid deserialized string
     * containing the FoCI token cache item for the user. The function is
     * expected to store the deserialized FoCI token cache item back to the
     * cache store
     */
    @SmallTest
    public void testDeserialize_valid() throws AuthenticationException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        addFRTCacheItem(mockCache);
        final AuthenticationContext context = getAuthenticationContext(mockContext, TEST_AUTHORITY, false, mockCache);
        final String serializedBlob = context.serialize(TEST_IDTOKEN_USERID);
        context.deserialize(serializedBlob);
        assertTrue(mockCache.getTokensForUser(TEST_IDTOKEN_USERID) != null);
     }

    /**
     * Test the deserialize() function where the serial version UID is not
     * compatible the function is expected to throw the
     * DeserializationAuthenticationException
     */
    @SmallTest
    public void testDeserialize_incompatibleInput() {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        addFRTCacheItem(mockCache);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        final Date date = new Date(1000);
        final Gson gson = new Gson();
        final String mockFalseSerializedBlob = gson.toJson(date);
        try {
            context.deserialize(mockFalseSerializedBlob);
            Assert.fail("Not expected.");
        } catch (final Exception exception) {
            assertTrue(((AuthenticationException) exception).getCode()
                    .equals(ADALError.INCOMPATIBLE_BLOB_VERSION));
        }
    }

    /**
     * Test the deserialize() function where the deserialize input is null. The
     * function is expected to throw IllegalArgumentException
     */
    @SmallTest
    public void testDeserialize_nullSerializedBlob() {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        addFRTCacheItem(mockCache);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        try {
            context.deserialize(null);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof IllegalArgumentException);
        }
    }

    /**
     * Test the deserialize() function where the deserialize input is a random
     * string. The function is expected to throw AuthenticationException
     */
    @SmallTest
    public void testDeserialize_randomString() {
        final String ramdomString = "abc";
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        try {
            context.deserialize(ramdomString);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof DeserializationAuthenticationException);
        }
    }

    /**
     * Test the deserialize() function where the deserialize input is a json
     * token which missing one attribute which needed in the deserialization of
     * the tokenCacheItem. The function is expected to throw
     * AuthenticationException
     */
    @SmallTest
    public void testDeserialize_missingAttribute() {
        final String missingAttributeString = "{\"tokenCacheItems\":[{\"authority\":\"https://login.windows.net/ComMon/\",\"refresh_token\":\"FRT\",\"foci\":\"1\"}],\"version\":1}";
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        try {
            context.deserialize(missingAttributeString);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof DeserializationAuthenticationException);
        }
    }

    /**
     * Test the deserialize() function where the deserialize input is a json
     * token which has one additional attribute. The function is expected to
     * store the deserialized FoCI token cache item back to the cache store.
     * 
     * @throws AuthenticationException
     */
    @SmallTest
    public void testDeserialize_additionalAttribute() throws AuthenticationException {
        final String additionalAttributeString = "{\"tokenCacheItems\":[{\"authority\":\"https://login.windows.net/ComMon/\",\"refresh_token\":\"FRT\",\"id_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.\",\"foci\":\"1\"}],\"version\":1,\"comment\":\"no comment\"}";
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        context.deserialize(additionalAttributeString);
        assertTrue(mockCache.getTokensForUser(TEST_IDTOKEN_USERID) != null);
    }

    /**
     * Test the deserialize() function where the serial version UID is different
     * with expected one. The calling should throw the
     * DeserializationAuthenticationException
     */
    @SmallTest
    public void testDeserialize_differentVersion() {
        final String differentVersionString = "{\"tokenCacheItems\":[{\"authority\":\"https://login.windows.net/ComMon/\",\"refresh_token\":\"FRT\",\"id_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.\",\"foci\":\"1\"}],\"version\":2}";
        final FileMockContext mockContext = new FileMockContext(getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        try {
            context.deserialize(differentVersionString);
        } catch (final Exception exception) {
            assertTrue("argument exception", exception instanceof DeserializationAuthenticationException);
        }
    }

    public void testVerifyManifestPermission_valid() throws InterruptedException, PackageManager.NameNotFoundException {
        final FileMockContext mockContext = createMockContext();
        mockContext.setPermission("android.permission.INTERNET",PackageManager.PERMISSION_GRANTED);
        mockContext.setPermission("android.permission.GET_ACCOUNTS",PackageManager.PERMISSION_GRANTED);
        mockContext.setPermission("android.permission.USE_CREDENTIALS",PackageManager.PERMISSION_GRANTED);
        mockContext.setPermission("android.permission.MANAGE_ACCOUNTS",PackageManager.PERMISSION_GRANTED);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", authContext.getRedirectUriForBroker(),
                "loginHint", callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNull(callback.callbackException);
    }

    public void testVerifyManifestPermission_missingPermission_MANAGE_ACCOUNTS() throws InterruptedException, PackageManager.NameNotFoundException {
        final FileMockContext mockContext = createMockContext();
        mockContext.setPermission("android.permission.INTERNET",PackageManager.PERMISSION_GRANTED);
        mockContext.setPermission("android.permission.GET_ACCOUNTS",PackageManager.PERMISSION_GRANTED);
        mockContext.setPermission("android.permission.USE_CREDENTIALS",PackageManager.PERMISSION_GRANTED);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", authContext.getRedirectUriForBroker(),
                "loginHint", callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNotNull(callback.callbackException);
        assertTrue(callback.callbackException instanceof UsageAuthenticationException);
        final UsageAuthenticationException usageAuthenticationException
                = (UsageAuthenticationException) callback.callbackException;

        assertTrue(usageAuthenticationException.getMessage().contains("MANAGE_ACCOUNTS"));
        assertEquals(ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING, usageAuthenticationException.getCode());
    }

    public void testVerifyManifestPermission_missingPermission_USE_CREDENTIALS() throws InterruptedException, PackageManager.NameNotFoundException {
        final FileMockContext mockContext = createMockContext();
        mockContext.setPermission("android.permission.INTERNET",PackageManager.PERMISSION_GRANTED);
        mockContext.setPermission("android.permission.GET_ACCOUNTS",PackageManager.PERMISSION_GRANTED);
        mockContext.setPermission("android.permission.MANAGE_ACCOUNTS",PackageManager.PERMISSION_GRANTED);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", authContext.getRedirectUriForBroker(),
                "loginHint", callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNotNull(callback.callbackException);
        assertTrue(callback.callbackException instanceof UsageAuthenticationException);
        final UsageAuthenticationException usageAuthenticationException
                = (UsageAuthenticationException) callback.callbackException;

        assertTrue(usageAuthenticationException.getMessage().contains("USE_CREDENTIALS"));
        assertEquals(ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING, usageAuthenticationException.getCode());
    }

     public void testVerifyManifestPermission_missingPermission_MANAGE_ACCOUNTS_USE_CREDENTIALS() throws InterruptedException, PackageManager.NameNotFoundException {
         final FileMockContext mockContext = createMockContext();
         mockContext.setPermission("android.permission.INTERNET",PackageManager.PERMISSION_GRANTED);
         mockContext.setPermission("android.permission.GET_ACCOUNTS",PackageManager.PERMISSION_GRANTED);

         AuthenticationSettings.INSTANCE.setUseBroker(true);
         final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                 VALID_AUTHORITY, false);
         final TestAuthCallback callback = new TestAuthCallback();
         authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", authContext.getRedirectUriForBroker(),
                 "loginHint", callback);
         final CountDownLatch signal = new CountDownLatch(1);
         signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

         assertNotNull(callback.callbackException);
         assertTrue(callback.callbackException instanceof UsageAuthenticationException);
         final UsageAuthenticationException usageAuthenticationException
                 = (UsageAuthenticationException) callback.callbackException;

         assertTrue(usageAuthenticationException.getMessage().contains("MANAGE_ACCOUNTS"));
         assertTrue(usageAuthenticationException.getMessage().contains("USE_CREDENTIALS"));
         assertEquals(ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING, usageAuthenticationException.getCode());
     }

    public void testVerifyManifestPermission_missingPermission_GET_ACCOUNT() throws InterruptedException, PackageManager.NameNotFoundException {
        final FileMockContext mockContext = createMockContext();
        mockContext.setPermission("android.permission.INTERNET",PackageManager.PERMISSION_GRANTED);
        mockContext.setPermission("android.permission.MANAGE_ACCOUNTS",PackageManager.PERMISSION_GRANTED);
        mockContext.setPermission("android.permission.USE_CREDENTIALS",PackageManager.PERMISSION_GRANTED);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", authContext.getRedirectUriForBroker(),
                "loginHint", callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNotNull(callback.callbackException);
        assertTrue(callback.callbackException instanceof UsageAuthenticationException);
        final UsageAuthenticationException usageAuthenticationException
                = (UsageAuthenticationException) callback.callbackException;
        assertTrue(usageAuthenticationException.getMessage().contains("GET_ACCOUNT"));
        assertEquals(ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING, usageAuthenticationException.getCode());
    }

    private FileMockContext createMockContext()
            throws PackageManager.NameNotFoundException {

        final FileMockContext mockContext = new FileMockContext(getContext());

        final AccountManager mockedAccountManager = getMockedAccountManager();
        mockContext.setMockedAccountManager(mockedAccountManager);

        final PackageManager mockedPackageManager = getMockedPackageManager();
        mockContext.setMockedPackageManager(mockedPackageManager);

        return mockContext;
    }


    private AccountManager getMockedAccountManager() {
        final AccountManager mockedAccountManager = Mockito.mock(AccountManager.class);
        final AuthenticatorDescription authenticatorDescription
                = new AuthenticatorDescription(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, 0, 0, 0, 0);
        final AuthenticatorDescription mockedAuthenticator = Mockito.spy(authenticatorDescription);
        final AuthenticatorDescription[] mockedAuthenticatorTypes
                = new AuthenticatorDescription[] {mockedAuthenticator};
        Mockito.when(mockedAccountManager.getAuthenticatorTypes()).thenReturn(mockedAuthenticatorTypes);

        return mockedAccountManager;
    }

    private PackageManager getMockedPackageManager() throws PackageManager.NameNotFoundException {
        final Signature mockedSignature = Mockito.mock(Signature.class);
        when(mockedSignature.toByteArray()).thenReturn(Base64.decode(
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_SIGNATURE, Base64.NO_WRAP));

        final PackageInfo mockedPackageInfo = Mockito.mock(PackageInfo.class);
        mockedPackageInfo.signatures = new Signature[] {mockedSignature};

        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);
        when(mockedPackageManager.getPackageInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(mockedPackageInfo);
        //mock permission
        when(mockedPackageManager.checkPermission(Mockito.refEq("android.permission.GET_ACCOUNTS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mockedPackageManager.checkPermission(Mockito.refEq("android.permission.MANAGE_ACCOUNTS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mockedPackageManager.checkPermission(Mockito.refEq("android.permission.USE_CREDENTIALS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);

        // Mock intent query
        final List<ResolveInfo> activities = new ArrayList<>(1);
        activities.add(Mockito.mock(ResolveInfo.class));
        when(mockedPackageManager.queryIntentActivities(Mockito.any(Intent.class), Mockito.anyInt()))
                .thenReturn(activities);

        return mockedPackageManager;
    }

     private String getErrorResponseBody(final String errorCode) {
         final String errorDescription = "\"error_description\":\"AADSTS70000: Authentication failed. Refresh Token is not valid.\r\nTrace ID: bb27293d-74e4-4390-882b-037a63429026\r\nCorrelation ID: b73106d5-419b-4163-8bc6-d2c18f1b1a13\r\nTimestamp: 2014-11-06 18:39:47Z\",\"error_codes\":[70000],\"timestamp\":\"2014-11-06 18:39:47Z\",\"trace_id\":\"bb27293d-74e4-4390-882b-037a63429026\",\"correlation_id\":\"b73106d5-419b-4163-8bc6-d2c18f1b1a13\",\"submit_url\":null,\"context\":null";

         if (errorCode != null) {
             return "{\"error\":\"" + errorCode + "\"," + errorDescription + "}";
         }

         return "{" + errorDescription + "}";
     }

     // No Family client id set in the cache. Only regular RT token cache entry
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
                 CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId", false, userId, null),
                 refreshItem);
         cache.setItem(
                 CacheKey.createCacheKey(VALID_AUTHORITY, "resource", "clientId", false, displayableId, null),
                 refreshItem);
         return cache;
     }


    private ITokenCacheStore getMockCache(int minutes, String token, String resource,
            String client, String user, boolean isMultiResource) {
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
                CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, client, user),
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
        if (!isMultiResource) {
            refreshItem.setResource(resource);
        }
        refreshItem.setClientId(clientId);
        refreshItem.setAccessToken(token);
        refreshItem.setRefreshToken(refreshToken);
        refreshItem.setExpiresOn(timeAhead.getTime());
        refreshItem.setIsMultiResourceRefreshToken(isMultiResource);
        refreshItem.setTenantId(tenantId);
        refreshItem.setUserInfo(new UserInfo(userId, name, familyName, "", displayId));
        final String keyUserId, keyUpn;
        if (isMultiResource) {
            keyUserId = CacheKey.createCacheKeyForMRRT(authority, clientId, userId);
            keyUpn = CacheKey.createCacheKeyForMRRT(authority, clientId, displayId);
        } else {
            keyUserId = CacheKey.createCacheKeyForRTEntry(authority, resource, clientId, userId);
            keyUpn = CacheKey.createCacheKeyForRTEntry(authority, resource, clientId, displayId);
        }

        Log.d(TAG, "Key with userId: " + keyUserId);
        cache.setItem(keyUserId, refreshItem);
        TokenCacheItem item = cache.getItem(keyUserId);
        assertNotNull("item is in cache", item);


        Log.d(TAG, "Key with upn: " + keyUpn);
        cache.setItem(keyUpn, refreshItem);
        item = cache.getItem(keyUpn);
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
            try {
                mCache.append(CacheKey.createCacheKey(item).hashCode(), item);
            } catch (final AuthenticationException e) {
                Logger.e(TAG, "Unable to create cache key from token cache item", "", ADALError.INVALID_TOKEN_CACHE_ITEM);
            }
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
