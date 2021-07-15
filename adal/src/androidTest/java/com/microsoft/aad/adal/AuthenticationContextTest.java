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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.HttpUrlConnectionFactory;
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.internal.broker.PackageHelper;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public final class AuthenticationContextTest {

    /**
     * Check case-insensitive lookup.
     */
    private static final String VALID_AUTHORITY = "https://login.windows.net/test.onmicrosoft.com";

    /**
     * This value Controls the timeout for CloudDownLatches used in various tests
     * You may want to increase this value when debugging a test
     */
    protected static final int CONTEXT_REQUEST_TIME_OUT = 20000;

    protected static final int ACTIVITY_TIME_OUT = 1000;

    private static final String TEST_AUTHORITY = "https://login.windows.net/ComMon/";

    private static final String TEST_PACKAGE_NAME = "com.microsoft.aad.adal.test";

    private static final int EXPIRES_ON_ADJUST_MINS = 10;

    static final String TEST_IDTOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";

    static final String TEST_IDTOKEN_USERID = "4f859989-a2ff-411e-9048-c322247ac62c";

    static final String TEST_IDTOKEN_UPN = "admin@aaltests.onmicrosoft.com";


    private String mTestTag;

    private static final String TAG = "AuthenticationContextTest";

    @Before
    @SuppressLint("PackageManagerGetSignatures")
    public void setUp() throws Exception {
        Logger.d(TAG, "setup key at settings");

        System.setProperty(
                "dexmaker.dexcache",
                getInstrumentation()
                        .getTargetContext()
                        .getCacheDir()
                        .getPath()
        );

        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            // use same key for tests
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");

            final int iterationCount = 100;
            final int keyLength = 256;
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes(StandardCharsets.UTF_8), iterationCount, keyLength));

            SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }

        final InstanceDiscoveryMetadata metadata = new InstanceDiscoveryMetadata("login.microsoftonline.com", "login.windows.net");
        //final AzureActiveDirectoryCloud cloud = CoreAdapter.asAadCloud(metadata);

        AuthorityValidationMetadataCache.updateInstanceDiscoveryMap("login.windows.net", metadata);
        //AzureActiveDirectory.putCloud("login.windows.net", cloud);
        addAzureADCloudForValidAuthority();

        AuthenticationSettings.INSTANCE.setUseBroker(false);
        // ADAL is set to this signature for now
        PackageInfo info = getInstrumentation().getContext().getPackageManager()
                .getPackageInfo(getInstrumentation().getContext().getPackageName(), PackageHelper.getPackageManagerSignaturesFlag());

        // Broker App can be signed with multiple certificates. It will look
        // all of them
        // until it finds the correct one for ADAL broker.
        for (Signature signature : PackageHelper.getSignatures(info)) {
            final byte[] testSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(testSignature);
            mTestTag = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            break;
        }
    }

    @After
    public void tearDown() throws Exception {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        Logger.getInstance().setExternalLogger(null);
    }

    /**
     * Test constructor to make sure authority parameter is set.
     */
    @Test
    public void testConstructor() throws NoSuchAlgorithmException, NoSuchPaddingException {
        testAuthorityTrim("authorityFail");
        testAuthorityTrim("https://msft.com////");
        testAuthorityTrim("https:////");
        AuthenticationContext context2 = new AuthenticationContext(getInstrumentation().getContext(),
                "https://github.com/MSOpenTech/some/some", false);
        assertEquals("https://github.com/MSOpenTech", context2.getAuthority());
    }

    private void testAuthorityTrim(String authority) throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        try {
            new AuthenticationContext(getInstrumentation().getContext(), authority, false);
            Assert.fail("expected to fail");
        } catch (IllegalArgumentException e) {
            assertTrue("authority in the msg", e.getMessage().contains("authority"));
        }
    }

    @Test
    public void testConstructorNoCache() throws UsageAuthenticationException {
        final String authority = "https://github.com/MSOpenTech";
        final AuthenticationContext context = new AuthenticationContext(getInstrumentation().getContext(), authority, false,
                null);
        assertNull(context.getCache());
    }

    @Test
    public void testClearAdalCacheRemovesReplicaCache() {
        // Init a Context
        final Context context = getInstrumentation().getContext();

        // Create an instance of the MSAL cache, populate it with some data
        final IAccountCredentialCache accountCredentialCache = new SharedPreferencesAccountCredentialCache(
                new CacheKeyValueDelegate(),
                SharedPreferencesFileManager.getSharedPreferences(
                        context,
                        DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                        new AndroidAuthSdkStorageEncryptionManager(context, null)
                )
        );

        // Create a dummy account
        final AccountRecord dummyAccount = new AccountRecord();
        dummyAccount.setHomeAccountId("23bfff3f-2664-495d-8196-79e28eaf400b");
        dummyAccount.setEnvironment("login.windows.net");
        dummyAccount.setRealm("0b96cce5-7aef-4ecb-bb27-2e50e3ed69de");
        dummyAccount.setName("Garth Fort");

        // Save it
        accountCredentialCache.saveAccount(dummyAccount);

        // Assert that the dummy account exists
        assertEquals(dummyAccount, accountCredentialCache.getAccounts().get(0));

        // Create an instance of the Authentication Context
        final AuthenticationContext authContext =
                new AuthenticationContext(
                        getInstrumentation().getContext(),
                        "https://github.com/MSOpenTech/some/some",
                        false
                );

        // Clear the ADAL cache
        authContext.getCache().removeAll();

        // Assert that the replica is empty
        assertTrue(accountCredentialCache.getAccounts().isEmpty());
    }

    @Test
    public void testConstructorInternetPermission() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        String authority = "https://github.com/MSOpenTech";
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        mockContext.addPermission("android.permission.INTERNET");

        // no exception
        new AuthenticationContext(mockContext, authority, false);

        try {
            mockContext.removePermission("android.permission.INTERNET");
            new AuthenticationContext(mockContext, authority, false);
            Assert.fail("Supposed to fail");
        } catch (Exception e) {

            assertEquals("Permission related message",
                    ADALError.DEVELOPER_INTERNET_PERMISSION_MISSING,
                    ((AuthenticationException) e.getCause()).getCode());
        }
    }

    @Test
    public void testConstructorValidateAuthority() throws NoSuchAlgorithmException,
            NoSuchPaddingException {

        String authority = "https://github.com/MSOpenTech";
        AuthenticationContext context = getAuthenticationContext(getInstrumentation().getContext(), authority, true,
                null);
        assertTrue("Validate flag is expected to be same", context.getValidateAuthority());

        context = new AuthenticationContext(getInstrumentation().getContext(), authority, false);
        assertFalse("Validate flag is expected to be same", context.getValidateAuthority());
    }

    @Test
    public void testCorrelationIdSetAndGet() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        UUID requestCorrelationId = UUID.randomUUID();
        AuthenticationContext context = new AuthenticationContext(getInstrumentation().getContext(), TEST_AUTHORITY,
                true);
        context.setRequestCorrelationId(requestCorrelationId);
        assertEquals("Verifier getter and setter", requestCorrelationId,
                context.getRequestCorrelationId());
    }

    /**
     * External call to Service to get real error response. Add expired item in
     * cache to try refresh token request. Web Request should have mCorrelationId
     * in the header.
     *
     * @throws InterruptedException
     */
    @Test
    @UiThreadTest
    public void testCorrelationIdInWebRequest() throws InterruptedException, IOException {

        Logger.getInstance().setEnablePII(true);
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
        final int minSDKVersionForTest = 15;
        if (Build.VERSION.SDK_INT <= minSDKVersionForTest) {
            Logger.v(TAG,
                    "Server is returning 401 status code without challenge. "
                            + "HttpUrlConnection does not return error stream for that in SDK 15. "
                            + "Without error stream, this test is useless.");
            return;
        }

        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        String expectedAccessToken = "TokenFortestAcquireToken" + UUID.randomUUID().toString();
        String expectedClientId = "client" + UUID.randomUUID().toString();
        String expectedResource = "resource" + UUID.randomUUID().toString();
        String expectedUser = "userid" + UUID.randomUUID().toString();
        final int cacheTimeOffsetMins = -30;
        ITokenCacheStore mockCache = getMockCache(cacheTimeOffsetMins, expectedAccessToken, expectedResource,
                expectedClientId, expectedUser, false);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        UUID requestCorrelationId = UUID.randomUUID();
        Logger.d(TAG, "test correlationId:" + requestCorrelationId.toString());
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        final String errorResponse = "{\"error\":\"invalid_request\", \"error_description\":\"Authentication failed. "
                + "correlation_id:" + requestCorrelationId.toString() + "\", \"correlation_id\": \""
                + requestCorrelationId.toString() + "\"}";

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenThrow(IOException.class)
                .thenReturn(Util.createInputStream(errorResponse));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        final TestLogResponse response = new TestLogResponse();
        response.listenLogForMessageSegments("Authentication failed", "correlation_id:\"\""
                + requestCorrelationId.toString());

        // Call acquire token with prompt never to prevent activity launch
        context.setRequestCorrelationId(requestCorrelationId);
        context.acquireTokenSilentAsync(expectedResource, expectedClientId, expectedUser, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that web request send correct headers
        Logger.i(TAG, "Get response message. ", "Response msg:" + response.getMessage());
        assertNotNull("Server response isn't null ", response.getMessage());
        assertTrue("Server response has same correlationId",
                response.getMessage().contains(requestCorrelationId.toString()));
    }

    /**
     * If package does not have declaration for activity, it should return false.
     */
    @Test
    public void testResolveIntent() throws InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        mockContext.setResolveIntent(false);

        final AuthenticationContext context = new AuthenticationContext(mockContext, VALID_AUTHORITY,
                false);
        final AggregatedTelemetryTestClass telemetry = new AggregatedTelemetryTestClass();
        Telemetry.getInstance().registerDispatcher(telemetry, true);

        final TestAuthCallback callback = new TestAuthCallback();
        context.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", "redirect",
                PromptBehavior.Always, callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNotNull(callback.getCallbackException());
        assertTrue(callback.getCallbackException() instanceof AuthenticationException);

        final AuthenticationException authenticationException = (AuthenticationException) callback.getCallbackException();
        assertTrue(authenticationException.getCode() == ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED);

        assertTrue(telemetry.eventsReceived());
    }

    /**
     * Test throws for different missing arguments
     */
    @Test
    public void testAcquireTokenNegativeArguments() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        final MockActivity testActivity = new MockActivity();
        final MockAuthenticationCallback testEmptyCallback = new MockAuthenticationCallback();

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "callback",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(testActivity.getTestActivity(), "resource", "clientId", "redirectUri",
                                "userid", null);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "resource",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(testActivity.getTestActivity(), null, "clientId", "redirectUri",
                                "userid", testEmptyCallback);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "resource",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(testActivity.getTestActivity(), "", "clientId", "redirectUri", "userid",
                                testEmptyCallback);
                    }
                });

        AssertUtils.assertThrowsException(IllegalArgumentException.class, "clientid",
                new Runnable() {

                    @Override
                    public void run() {
                        context.acquireToken(testActivity.getTestActivity(), "resource", null, "redirectUri",
                                "userid", testEmptyCallback);
                    }
                });
    }

    @Test
    public void testAcquireTokenUserId() throws InterruptedException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        context.acquireToken(testActivity.getTestActivity(), "resource56", "clientId345", "redirect123", "userid123",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // verify request
        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertTrue(request.getClass().equals(AuthenticationRequest.class));

        final AuthenticationRequest authenticationRequest = (AuthenticationRequest) request;
        assertEquals("Redirect uri is same as package", "redirect123", authenticationRequest.getRedirectUri());
        assertEquals("login hint same as userid", "userid123", authenticationRequest.getLoginHint());
        assertEquals("client is same", "clientId345", authenticationRequest.getClientId());
        assertEquals("resource is same", "resource56", authenticationRequest.getResource());
    }

    @Test
    public void testEmptyRedirect() throws InterruptedException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        context.acquireToken(testActivity.getTestActivity(), "resource", "clientId", "", "userid", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertTrue(request.getClass().equals(AuthenticationRequest.class));

        final AuthenticationRequest authenticationRequest = (AuthenticationRequest) request;
        assertEquals("Redirect uri is same as package", TEST_PACKAGE_NAME, authenticationRequest.getRedirectUri());
    }

    @Test
    public void testPrompt() throws InterruptedException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        // 1 - Send prompt always
        context.acquireToken(testActivity.getTestActivity(), "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.Always, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Get intent from activity to verify extraparams are send
        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        final Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertTrue(request.getClass().equals(AuthenticationRequest.class));

        final AuthenticationRequest authenticationRequest = (AuthenticationRequest) request;
        assertEquals("Prompt param is same", PromptBehavior.Always, authenticationRequest.getPrompt());

        // 2 - Send refresh prompt
        final CountDownLatch signal2 = new CountDownLatch(1);
        MockAuthenticationCallback callback2 = new MockAuthenticationCallback(signal2);
        testActivity.mSignal = signal2;
        context.acquireToken(testActivity.getTestActivity(), "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.REFRESH_SESSION, callback2);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Get intent from activity to verify extraparams are send
        intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        final Serializable request2 = intent.getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertTrue(request2.getClass().equals(AuthenticationRequest.class));

        final AuthenticationRequest authenticationRequest2 = (AuthenticationRequest) request2;
        assertEquals("Prompt param is same", PromptBehavior.REFRESH_SESSION, authenticationRequest2.getPrompt());
    }

    @Test
    public void testExtraParams() throws InterruptedException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        String expected = "&extraParam=1";
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        // 1 - Send extra param
        context.acquireToken(testActivity.getTestActivity(), "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.Always, expected, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // get intent from activity to verify extraparams are send
        Intent intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);
        final Serializable request = intent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertTrue(request.getClass().equals(AuthenticationRequest.class));
        final AuthenticationRequest authenticationRequest = (AuthenticationRequest) request;
        assertEquals("Extra query param is same", expected, authenticationRequest.getExtraQueryParamsAuthentication());

        // 2- Don't send extraqueryparam
        CountDownLatch signal2 = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal2);
        testActivity.mSignal = signal2;
        context.acquireToken(testActivity.getTestActivity(), "testExtraParamsResource", "testExtraParamsClientId",
                "testExtraParamsredirectUri", PromptBehavior.Always, null, callback);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // verify from mocked activity intent
        intent = testActivity.mStartActivityIntent;
        assertNotNull(intent);

        final Serializable request2 = intent.getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertTrue(request2.getClass().equals(AuthenticationRequest.class));
        final AuthenticationRequest authenticationRequest2 = (AuthenticationRequest) request2;
        assertNull("Extra query param is null", authenticationRequest2.getExtraQueryParamsAuthentication());
    }

    @Test
    public void testInvalidClaimsAcquireToken() throws InterruptedException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                "https://login.windows.net/common", false, null);

        List<String> capabilities = new ArrayList<>();
        capabilities.add("CC1");
        capabilities.add("CC2");
        context.setClientCapabilites(capabilities);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        String expected = "&extraParam=1";
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        testActivity.mSignal = signal;

        // 1 - Send extra param
        context.acquireToken(testActivity.getTestActivity(), "testResource",
                "testClientId", "testredirectUri", "test_login_hint",
                PromptBehavior.Always, "&extraParam=1", "invalid_claims", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        // Check response in callback result
        assertNotNull("Error is not null", callback.getException());
        assertEquals("NOT_A_VALID_CLAIMS_JSON", ADALError.JSON_PARSE_ERROR,
                ((AuthenticationException) callback.getException()).getCode());

    }

    public static AuthenticationRequest createAuthenticationRequest(final String authority, final String resource,
                                                                    final String client, final String redirect, final String loginhint, final boolean isExtendedLifetimeEnabled) {

        return new AuthenticationRequest(authority, resource, client, redirect, loginhint, isExtendedLifetimeEnabled);
    }

    /**
     * Test throws for different missing arguments
     */
    @Test
    public void testAcquireTokenByRefreshTokenNegativeArguments() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());

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

    @Test
    public void testAcquireTokenByRefreshTokenConnectionNotAvailable() throws InterruptedException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        mockContext.setConnectionAvailable(false);

        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        CountDownLatch signal = new CountDownLatch(1);
        final MockAuthenticationCallback mockCallback = new MockAuthenticationCallback(signal);
        context.acquireTokenByRefreshToken("refresh", "clientId", "resource", mockCallback);

        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        assertTrue("Exception type", mockCallback.getException() instanceof AuthenticationException);
        assertEquals("Connection related error code", ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE,
                ((AuthenticationException) mockCallback.getException()).getCode());
    }


    /**
     * Test throws for different missing arguments
     *
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    public void testAcquireTokenByRefreshTokenPositive() throws IOException, InterruptedException, JSONException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        final CountDownLatch signal = new CountDownLatch(1);

        String expectedClientId = "client" + UUID.randomUUID().toString();
        String exptedResource = "resource" + UUID.randomUUID().toString();
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getSuccessTokenResponse(false, false)),
                Util.createInputStream(Util.getSuccessTokenResponse(true, true)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", "I am a new access token",
                callback.getAuthenticationResult().getAccessToken());
        assertEquals("Same refresh token", "I am a new refresh token",
                callback.getAuthenticationResult().getRefreshToken());

        final CountDownLatch signal2 = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal2);
        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, exptedResource,
                callback);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", "I am a new access token",
                callback.getAuthenticationResult().getAccessToken());
        assertEquals("Same refresh token", "I am a new refresh token",
                callback.getAuthenticationResult().getRefreshToken());

        assertNotNull("Result has user info from idtoken",
                callback.getAuthenticationResult().getUserInfo());
        assertEquals("Result has user info from idtoken", TEST_IDTOKEN_UPN,
                callback.getAuthenticationResult().getUserInfo().getDisplayableId());
    }

    @Test
    public void testAcquireTokenByRefreshTokenNotReturningRefreshToken() throws IOException, InterruptedException, JSONException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final CountDownLatch signal = new CountDownLatch(1);
        final String expectedClientId = "client" + UUID.randomUUID().toString();
        final String refreshToken = "refreshTokenSending";
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.
                getSuccessResponseWithoutRefreshToken()));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        context.acquireTokenByRefreshToken("refreshTokenSending", expectedClientId, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Verify that new refresh token is matching to mock response
        assertEquals("Same token", "I am a new access token",
                callback.getAuthenticationResult().getAccessToken());
        assertEquals("Same refresh token", refreshToken,
                callback.getAuthenticationResult().getRefreshToken());
    }

    /**
     * authority is malformed and error should come back in callback
     */
    @Test
    public void testAcquireTokenAuthorityMalformed() throws InterruptedException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        // Malformed url error will come back in callback
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                "abcd://vv../v", false);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        context.acquireToken(testActivity.getTestActivity(), "resource", "clientId", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
        assertNotNull("Error is not null", callback.getException());
        assertEquals("NOT_VALID_URL", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL,
                ((AuthenticationException) callback.getException()).getCode());
    }

    /**
     * authority is validated and intent start request is sent
     *
     * @throws InterruptedException
     * @throws IllegalArgumentException
     */
    @Test
    public void testAcquireTokenValidateAuthorityReturnsValid() throws InterruptedException {

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext, VALID_AUTHORITY,
                true);

        final CountDownLatch signal = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity();
        testActivity.mSignal = signal;
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        context.acquireToken(testActivity.getTestActivity(), "resource", "clientid", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
        assertNull("Error is null", callback.getException());
        assertEquals("Activity was attempted to start with request code",
                AuthenticationConstants.UIRequest.BROWSER_FLOW,
                testActivity.mStartActivityRequestCode);
    }

    @Test
    public void testCorrelationIdInDiscovery() throws InterruptedException {

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext authContext = getAuthenticationContext(mockContext, VALID_AUTHORITY, true, null);
        final CountDownLatch signal = new CountDownLatch(1);
        final UUID correlationId = UUID.randomUUID();
        final MockActivity testActivity = new MockActivity();
        testActivity.mSignal = signal;
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // API call
        authContext.setRequestCorrelationId(correlationId);
        authContext.acquireToken(testActivity.getTestActivity(), "resource", "clientid", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check correlationID that was set in the Discovery obj
        assertNull("Error is null", callback.getException());
        assertEquals("Activity was attempted to start with request code",
                AuthenticationConstants.UIRequest.BROWSER_FLOW,
                testActivity.mStartActivityRequestCode);
    }

    /**
     * Invalid authority returns
     *
     * @throws InterruptedException
     */
    @Test
    public void testAcquireTokenValidateAuthorityReturnsInValid() throws InterruptedException, IOException {
        final String successfulDRSProbe =
                "{\n"
                        +
                        "  \"DeviceRegistrationService\": {\n"
                        +
                        "    \"RegistrationEndpoint\": \"https://fs.lindft6.com/EnrollmentServer/DeviceEnrollmentWebService.svc\",\n"
                        +
                        "    \"RegistrationResourceId\": \"urn:ms-drs:24E21DCF-106E-4FCE-821E-7204CD49A809\",\n"
                        +
                        "    \"ServiceVersion\": \"1.0\"\n"
                        +
                        "  },\n"
                        +
                        "  \"AuthenticationService\": {\n"
                        +
                        "    \"OAuth2\": {\n"
                        +
                        "      \"AuthCodeEndpoint\": \"https://fs.lindft6.com/adfs/oauth2/authorize\",\n"
                        +
                        "      \"TokenEndpoint\": \"https://fs.lindft6.com/adfs/oauth2/token\"\n"
                        +
                        "    }\n"
                        +
                        "  },\n"
                        +
                        "  \"IdentityProviderService\": {\n"
                        +
                        "    \"PassiveAuthEndpoint\": \"https://fs.lindft6.com/adfs/ls\"\n"
                        +
                        "  }\n"
                        +
                        "}";

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        Util.prepareMockedUrlConnection(mockedConnection);

        Mockito.when(
                mockedConnection.getInputStream()
        ).thenReturn(
                Util.createInputStream(successfulDRSProbe)
        ).thenReturn(
                Util.createInputStream("")
        );

        Mockito.when(
                mockedConnection.getResponseCode()
        ).thenReturn(
                HttpURLConnection.HTTP_OK
        ).thenReturn(
                HttpURLConnection.HTTP_NOT_FOUND
        );

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final String invalidAuthority = "https://fs.lindft6.com/adfs";
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                invalidAuthority, true);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        context.acquireToken(testActivity.getTestActivity(), "resource", "clientid", "redirectUri", "userid@tenant.com",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
        assertNotNull("Error is not null", callback.getException());
        assertEquals("NOT_VALID_URL", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE,
                ((AuthenticationException) callback.getException()).getCode());
        assertTrue(
                "Activity was not attempted to start with request code",
                AuthenticationConstants.UIRequest.BROWSER_FLOW != testActivity.mStartActivityRequestCode);

        // Sync test
        try {
            context.acquireTokenSilentSync("resource", "clientid", "userid@tenant.com");
            Assert.fail("Validation should throw");
        } catch (AuthenticationException exc) {
            // AD FS cannot be validated in silent sync calls because no UPN is available
            assertEquals("NOT_VALID_URL", ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED,
                    exc.getCode());
        }

        clearCache(context);
    }

    /**
     * acquire token without validation
     *
     * @throws InterruptedException
     */
    @Test
    public void testAcquireTokenWithoutValidation() throws InterruptedException {

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, null);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        context.acquireToken(testActivity.getTestActivity(), "resource", "clientid", "redirectUri", "userid",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback result
        assertNull("Error is null", callback.getException());
        assertEquals("Activity was attempted to start with request code",
                AuthenticationConstants.UIRequest.BROWSER_FLOW,
                testActivity.mStartActivityRequestCode);
        clearCache(context);
    }


    /**
     * acquire token using refresh token. All web calls are mocked. Refresh
     * token response must match to result and cache.
     */
    @Test
    public void
    testRefreshTokenPositive() throws IOException, InterruptedException, AuthenticationException {

        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final String response = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\",\"expires_in\":\"-10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";

        final String response2 = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenReturnsWithIdToken\",\"token_type\":\"Bearer\",\"expires_in\":\"3600\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refreshABC\",\"scope\":\"*\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(response2));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // Call acquire token which will try refresh token based on cache
        context.acquireToken(testActivity.getTestActivity(), "resource", "clientid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        verifyRefreshTokenResponse(mockCache, callback.getException(),
                callback.getAuthenticationResult());

        // Do silent token request and return idtoken in the result

        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TEST_IDTOKEN_USERID);
        assertEquals("Access Token", "TokenReturnsWithIdToken", result.getAccessToken());
        assertEquals("Refresh Token", "refreshABC", result.getRefreshToken());
        assertEquals("IdToken", TEST_IDTOKEN, result.getIdToken());
        clearCache(context);
    }

    @Test
    public void testScenarioUserIdLoginHintUse() throws InterruptedException, AuthenticationException, IOException, JSONException {
        scenarioUserIdLoginHint("test@user.com", "test@user.com", "test@user.com");
    }

    private void scenarioUserIdLoginHint(final String idTokenUpn, final String responseIntentHint,
                                         final String acquireTokenHint)
            throws InterruptedException, AuthenticationException, IOException, JSONException {

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        context.getCache().removeAll();

        final CountDownLatch signal = new CountDownLatch(1);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);
        final MockedIdToken idtoken = new MockedIdToken();
        idtoken.setUpn(idTokenUpn);
        idtoken.setOid("userid123");
        final String response = "{\"id_token\":\""
                + idtoken.getIdToken()
                + "\",\"access_token\":\"TokenUserIdTest\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\","
                + "\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(Util.getSuccessTokenResponse(true, true)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        Intent intent = getResponseIntent(callback, "resource", "clientid", "redirectUri",
                responseIntentHint);

        // Get token from onActivityResult after Activity returns
        tokenWithAuthenticationActivity(context, testActivity, signal, signalCallback, intent,
                "resource", "clientid", "redirectUri", acquireTokenHint, callback);

        // Token will return to callback with idToken
        verifyTokenResult(idtoken, callback.getAuthenticationResult());

        // Same call should get token from cache
        final CountDownLatch signalCallback2 = new CountDownLatch(1);
        callback.setSignal(signalCallback2);
        context.acquireToken(testActivity.getTestActivity(), "resource", "clientid", "redirectUri", acquireTokenHint,
                callback);
        signalCallback2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        verifyTokenResult(idtoken, callback.getAuthenticationResult());

        // Call with userId should return from cache as well
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                idtoken.getOid());
        verifyTokenResult(idtoken, result);

        clearCache(context);
    }

    @Test
    @Ignore
    public void testScenarioNullUserIdToken() throws InterruptedException, AuthenticationException, IOException, JSONException {
        scenarioUserIdLoginHint("test@user.com", "", "");
    }

    @Test
    @Ignore
    public void testScenarioLoginHintIdTokenDifferent() throws IOException, AuthenticationException,
            InterruptedException {

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        context.getCache().removeAll();

        final CountDownLatch signal = new CountDownLatch(1);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);
        final MockedIdToken idtoken = new MockedIdToken();
        idtoken.setUpn("admin@user.com");
        idtoken.setOid("admin123");
        final String loginHint = "user1@user.com";
        final String response = "{\"id_token\":\""
                + idtoken.getIdToken()
                + "\",\"access_token\":\"TokenUserIdTest\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\","
                + "\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        final Intent intent = getResponseIntent(callback, "resource", "clientid", "redirectUri",
                loginHint);

        // Get token from onActivityResult after Activity returns
        tokenWithAuthenticationActivity(context, testActivity, signal, signalCallback, intent,
                "resource", "clientid", "redirectUri", loginHint, callback);

        // Token will return to callback with idToken
        verifyTokenResult(idtoken, callback.getAuthenticationResult());

        // Same call with correct upn will return from cache
        final CountDownLatch signalCallback2 = new CountDownLatch(1);
        callback.setSignal(signalCallback2);
        context.acquireToken(testActivity.getTestActivity(), "resource", "clientid", "redirectUri", loginHint, callback);
        signalCallback2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        verifyTokenResult(idtoken, callback.getAuthenticationResult());

        // Call with userId should return from cache as well
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                idtoken.getOid());
        verifyTokenResult(idtoken, result);

        clearCache(context);
    }


    @Ignore
    public void testScenarioEmptyIdToken() throws InterruptedException, AuthenticationException, IOException, JSONException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        context.getCache().removeAll();

        final CountDownLatch signal = new CountDownLatch(1);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);

        final String response = "{\"access_token\":\"TokenUserIdTest\",\"token_type\":\"Bearer\",\"expires_in\":"
                + "\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(Util.getSuccessTokenResponse(true, true)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        Intent intent = getResponseIntent(callback, "resource", "clientid", "redirectUri", null);

        // Get token from onActivityResult after Activity returns
        tokenWithAuthenticationActivity(context, testActivity, signal, signalCallback, intent,
                "resource", "clientid", "redirectUri", null, callback);

        // Token will return to callback with idToken
        verifyTokenResult(null, callback.getAuthenticationResult());

        // Call with userId should return from cache as well
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid", null);
        verifyTokenResult(null, result);

        clearCache(context);
    }

    /**
     * Make sure we cache the family id correctly when we get family id from server.
     *
     * @throws InterruptedException
     * @throws IOException
     * @throws AuthenticationException
     */
    @Test
    public void testFamilyClientIdCorrectlyStoredInCache() throws IOException, InterruptedException,
            AuthenticationException, JSONException {

        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);
        final String response = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\",\"expires_in\":\"-10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\",\"foci\":\"1\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";
        // response2 has FoCI as "familyClientId"
        final String response2 = Util.getSuccessTokenResponse(true, true);
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(response2));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // Call acquire token which will try refresh token based on cache
        context.acquireToken(testActivity.getTestActivity(), "resource", "clientid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        verifyRefreshTokenResponse(mockCache, callback.getException(),
                callback.getAuthenticationResult());
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

    private void addAzureADCloudForValidAuthority() {
        List<String> aliases = new ArrayList<String>();
        aliases.add("login.windows.net");
        aliases.add("login.microsoftonline.com");
        AzureActiveDirectoryCloud cloud = new AzureActiveDirectoryCloud("login.microsoftonline.com", "login.windows.net", aliases);
        AzureActiveDirectory.putCloud("login.windows.net", cloud);
    }

    /**
     * When an authority is invalid, but the validation is not required:
     * a. ADAL still behaves correctly using developer provided authority
     * b. Subsequent requests do not result in further authority validation network requests for the process lifetime.
     */
    @Test
    public void testAuthorityValidationTurnedOffAndInstanceDiscoveryFailed() throws IOException, InterruptedException, JSONException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore mockedCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockedCache);
        final Activity mockActivity = Mockito.mock(Activity.class);

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        // The first network call would be doing instance discovery, the second one is refresh token request.
        // mock another silent request will only do token refresh
        Mockito.when(mockedConnection.getInputStream()).thenThrow(new IOException())
                .thenReturn(Util.createInputStream(Util.getSuccessTokenResponse(true, true)),
                        Util.createInputStream(Util.getSuccessTokenResponse(true, false)));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_OK);

        // clear the authority validation map
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
        final CountDownLatch latch = new CountDownLatch(1);
        context.acquireToken(mockActivity, "resource", "clientid", "redirectUri", TEST_IDTOKEN_UPN, new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                try {
                    Assert.assertTrue(result.getAccessToken().equals("I am a new access token"));
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(Exception exc) {
                Assert.fail();
            }
        });

        latch.await();

        final CountDownLatch latch2 = new CountDownLatch(1);
        Iterator<TokenCacheItem> items = context.getCache().getAll();
        while (items.hasNext()) {
            final TokenCacheItem item = items.next();
            item.setExpiresOn(new Date());
        }

        context.acquireTokenSilentAsync("resource", "clientId", TEST_IDTOKEN_USERID, new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                try {
                    Assert.assertTrue(result.getAccessToken().equals("I am a new access token"));
                } finally {
                    latch2.countDown();
                }
            }

            @Override
            public void onError(Exception exc) {
                Assert.fail();
            }
        });

        latch2.await();

        final int invocationCount = 3;
        Mockito.verify(mockedConnection, times(invocationCount)).getInputStream();

        // Clean up Authority validation cache
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
    }

    /**
     * When an authority is valid, but no metadata is returned:
     * a.ADAL still behaves correctly using developer provided authority
     * b.Subsequent requests do not result in further authority validation network requests for the process lifetime
     */
    @Test
    public void testAuthorityValidationTurnedOnButNoMetadataReturned() throws IOException, InterruptedException, JSONException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore mockedCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, true, mockedCache);
        final Activity mockActivity = Mockito.mock(Activity.class);

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        // The first network call would be doing instance discovery, the second one is refresh token request.
        // mock another silent request will only do token refresh
        final String tenantDiscoveryResponse = "{\"tenant_discovery_endpoint\":\"valid endpoint\"}";
        Mockito.when(mockedConnection.getInputStream()).thenReturn(
                Util.createInputStream(tenantDiscoveryResponse),
                Util.createInputStream(Util.getSuccessTokenResponse(true, true)),
                Util.createInputStream(Util.getSuccessTokenResponse(true, false)));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // clear the authority validation map
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
        final CountDownLatch latch = new CountDownLatch(1);
        context.acquireToken(mockActivity, "resource", "clientid", "redirectUri", TEST_IDTOKEN_UPN, new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                try {
                    Assert.assertTrue(result.getAccessToken().equals("I am a new access token"));
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(Exception exc) {
                Assert.fail();
            }
        });

        latch.await();

        final CountDownLatch latch2 = new CountDownLatch(1);
        Iterator<TokenCacheItem> items = context.getCache().getAll();
        while (items.hasNext()) {
            final TokenCacheItem item = items.next();
            item.setExpiresOn(new Date());
        }

        context.acquireTokenSilentAsync("resource", "clientId", TEST_IDTOKEN_USERID, new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                try {
                    Assert.assertTrue(result.getAccessToken().equals("I am a new access token"));
                } finally {
                    latch2.countDown();
                }
            }

            @Override
            public void onError(Exception exc) {
                Assert.fail();
            }
        });

        latch2.await();

        final int invocationCount = 3;
        Mockito.verify(mockedConnection, times(invocationCount)).getInputStream();
    }

    /**
     * If multiple simultaneous acquireToken calls with the same authority are made without any authority cache,
     * only one authority validation network request is ever made.
     */
    @Test
    public void testMultipleATCallsInDifferentThreadsOnlyOneAuthorityValidation() throws IOException, InterruptedException, ExecutionException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final String accessToken = "some access token";
        final String resource = "resource";
        final String clientId = "clientId";
        final ITokenCacheStore tokenCacheStore = getMockCache(30, accessToken, resource, clientId, TEST_IDTOKEN_UPN, true);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, true, tokenCacheStore);
        final Activity mockActivity = Mockito.mock(Activity.class);

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        // there are valid access token in the cache.
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(DiscoveryTests.getDiscoveryResponse()));

        // clear the authority validation map
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        final Callable<Void> task = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final CountDownLatch latch = new CountDownLatch(1);
                context.acquireToken(mockActivity, "resource", "clientid", "redirectUri", TEST_IDTOKEN_UPN, new AuthenticationCallback<AuthenticationResult>() {
                    @Override
                    public void onSuccess(AuthenticationResult result) {
                        try {
                            Assert.assertTrue(result.getAccessToken().equals(accessToken));
                        } finally {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(Exception exc) {
                        Assert.fail();
                    }
                });

                latch.await();
                return null;
            }
        };

        final List<Callable<Void>> tasks = Collections.nCopies(2, task);
        final List<Future<Void>> results = executorService.invokeAll(tasks);
        for (final Future<Void> result : results) {
            result.get();
        }

        Mockito.verify(mockedConnection, times(1)).getInputStream();
    }

    /**
     * When an authority is valid and metadata is returned:
     * but differs from preferred_cache:
     * new tokens are written to cache using solely the preferred_cache authority
     */
    //TODO: Fix Test
    @Ignore
    @Test
    public void testNewTokenOnlyWrittenToPreferredCacheLocation() throws InterruptedException, IOException, JSONException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore tokenCacheStore = new DefaultTokenCacheStore(mockContext);

        final String resource = "resource";
        final String authority = "https://sts.microsoft.com/test.onmicrosoft.com";
        final AuthenticationContext context = getAuthenticationContext(mockContext, authority, true, tokenCacheStore);
        context.getCache().removeAll();

        final CountDownLatch latch = new CountDownLatch(1);
        final Activity mockedActivity = Mockito.mock(Activity.class);
        final AuthenticationCallback callback = new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                final String uniqueUserId = result.getUserInfo().getUserId();
                // validate the foci serialize can still find token
                try {
                    assertNotNull(context.serialize(uniqueUserId));
                } catch (AuthenticationException e) {
                    fail("unexpected exception");
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(Exception exc) {
                fail();
            }
        };

        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final Intent intent = new Intent();
                String state = String.format("a=%s&r=%s", authority, resource);
                final String encodedState = Base64.encodeToString(state.getBytes(StandardCharsets.UTF_8), Base64.NO_PADDING | Base64.URL_SAFE);
                intent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
                intent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, "https://login.windows.net/common?code=1234&state=" + encodedState);
                context.onActivityResult(AuthenticationConstants.UIRequest.BROWSER_FLOW, AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
                return null;
            }
        }).when(mockedActivity).startActivityForResult(Mockito.any(Intent.class), Mockito.anyInt());

        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(DiscoveryTests.getDiscoveryResponse()),
                Util.createInputStream(Util.getSuccessTokenResponseWithFamilyClientId()));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        context.acquireToken(mockedActivity, "resource", "clientid",
                "redirect", TEST_IDTOKEN_UPN, callback);

        latch.await();

        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        Mockito.verify(mockedActivity).startActivityForResult(intentArgumentCaptor.capture(), Mockito.anyInt());
        final AuthenticationRequest requestMessage = (AuthenticationRequest) intentArgumentCaptor.getValue().getSerializableExtra(
                AuthenticationConstants.Browser.REQUEST_MESSAGE);
        assertTrue(requestMessage.getAuthority().contains("login.microsoftonline.com"));

        final SharedPreferences sharedPreferences = mockContext.getSharedPreferences("com.microsoft.aad.adal.cache", Activity.MODE_PRIVATE);
        final Map<String, String> allTokens = (Map<String, String>) sharedPreferences.getAll();
        final int expectedMapSize = 8;
        Assert.assertTrue(allTokens.size() == expectedMapSize);
        // also verify all the key are using preferred_cache
        final Set<String> keys = allTokens.keySet();
        for (final String key : keys) {
            assertTrue(key.contains("login.windows.net"));
        }

        assertTrue(HttpUrlConnectionFactory.getMockedConnectionOpenUrl().getHost().equals("login.microsoftonline.com"));
    }

    @Test
    public void testCacheContainsPreferredCacheToken() throws InterruptedException, IOException, JSONException {
        final String resource = "resource";
        final String clientId = "clientId";
        final ITokenCacheStore tokenCacheStore = getMockCache(-20, "accessToken", resource, clientId, TEST_IDTOKEN_USERID, false);

        final String authority = "https://sts.microsoft.com/test.onmicrosoft.com";
        final String accessTokenForPassedInAuthority = "access token for passedInAuthority";
        final TokenCacheItem itemForPassedInAuthority = getTokenCacheItem(authority, resource, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        itemForPassedInAuthority.setAccessToken(accessTokenForPassedInAuthority);
        tokenCacheStore.setItem(CacheKey.createCacheKeyForRTEntry(authority, resource, clientId, TEST_IDTOKEN_USERID), itemForPassedInAuthority);

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext authenticationContext = new AuthenticationContext(mockContext, authority, true, tokenCacheStore);

        final HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(connection);
        when(connection.getInputStream()).thenReturn(Util.createInputStream(DiscoveryTests.getDiscoveryResponse()),
                Util.createInputStream(Util.getSuccessTokenResponse(false, false)));
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));

        final CountDownLatch latch = new CountDownLatch(1);
        authenticationContext.acquireTokenSilentAsync(resource, clientId, TEST_IDTOKEN_USERID, new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                assertNotNull(result.getAccessToken());
                latch.countDown();
            }

            @Override
            public void onError(Exception exc) {
                fail();
            }
        });

        latch.await();

        // verify cache
        assertNotNull(tokenCacheStore.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID)));
        final TokenCacheItem item = tokenCacheStore.getItem(CacheKey.createCacheKeyForRTEntry(authority, resource, clientId, TEST_IDTOKEN_USERID));
        assertNotNull(item);
        assertTrue(item.getAccessToken().equals(accessTokenForPassedInAuthority));

        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
    }

    /**
     * When an authority is valid and metadata is returned, but differs from the preferred_network,
     * preferred_network host is used for all subsequent network requests.
     */
    @Test
    public void testCacheContainsPassedInAuthority() throws IOException, InterruptedException, JSONException {
        final String resource = "resource";
        final String clientId = "clientId";
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore tokenCacheStore = new DefaultTokenCacheStore(mockContext);
        tokenCacheStore.removeAll();

        final String authority = "https://sts.microsoft.com/test.onmicrosoft.com";
        final String accessTokenForPassedInAuthority = "access token for passedInAuthority";
        final String refreshTokenForPassedInAuthority = "refresh token for passedInAuthority";
        final TokenCacheItem itemForPassedInAuthority = getTokenCacheItem(authority, resource, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        itemForPassedInAuthority.setAccessToken(accessTokenForPassedInAuthority);
        itemForPassedInAuthority.setRefreshToken(refreshTokenForPassedInAuthority);
        tokenCacheStore.setItem(CacheKey.createCacheKeyForRTEntry(authority, resource, clientId, TEST_IDTOKEN_USERID), itemForPassedInAuthority);

        final String refreshTokenForPreferredNetwork = "refresh token for preferredNetwork";
        final TokenCacheItem itemForPreferredNetwork = getTokenCacheItem("https://login.microsoftonline.com/test.onmicrosoft.com",
                resource, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        itemForPreferredNetwork.setRefreshToken(refreshTokenForPreferredNetwork);

        final AuthenticationContext authenticationContext = new AuthenticationContext(mockContext, authority, true, tokenCacheStore);

        final HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(connection);
        when(connection.getInputStream()).thenReturn(Util.createInputStream(DiscoveryTests.getDiscoveryResponse()),
                Util.createInputStream(Util.getSuccessTokenResponse(false, false)));
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        final OutputStream outputStream = Mockito.mock(OutputStream.class);
        when(connection.getOutputStream()).thenReturn(outputStream);

        final CountDownLatch latch = new CountDownLatch(1);
        authenticationContext.acquireTokenSilentAsync(resource, clientId, TEST_IDTOKEN_USERID, new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                assertNotNull(result.getAccessToken());
                latch.countDown();
            }

            @Override
            public void onError(Exception exc) {
                fail();
            }
        });

        latch.await();

        verify(outputStream).write(Util.getPostMessage(refreshTokenForPassedInAuthority, clientId, resource));
        assertTrue(HttpUrlConnectionFactory.getMockedConnectionOpenUrl().getHost().equals("login.microsoftonline.com"));

        // verify cache
        assertNotNull(tokenCacheStore.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID)));
        final TokenCacheItem item = tokenCacheStore.getItem(CacheKey.createCacheKeyForRTEntry(authority, resource, clientId, TEST_IDTOKEN_USERID));
        assertNotNull(item);
        assertTrue(item.getAccessToken().equals(accessTokenForPassedInAuthority));

        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
    }

    private void verifyFamilyIdStoredInTokenCacheItem(final ITokenCacheStore cacheStore, final String cacheKey,
                                                      final String expectedFamilyClientId) {

        final TokenCacheItem tokenCacheItem = cacheStore.getItem(cacheKey);
        assertNotNull(tokenCacheItem);
        assertEquals(expectedFamilyClientId, tokenCacheItem.getFamilyClientId());
    }

    private Intent getResponseIntent(final MockAuthenticationCallback callback, final String resource,
                                     final String clientid, final String redirect, final String loginHint) {

        // Provide mock result for activity that returns code and proper state
        final Intent intent = new Intent();
        intent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
        final AuthenticationRequest authRequest = new AuthenticationRequest(VALID_AUTHORITY,
                resource, clientid, redirect, loginHint, false);
        intent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO, authRequest);
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
        context.acquireToken(testActivity.getTestActivity(), resource, clientid, redirect, loginHint, callback);
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
            assertEquals("Result has userid", idtoken.getOid(), result.getUserInfo().getUserId());
            assertEquals("Result has username", idtoken.getUpn(), result.getUserInfo()
                    .getDisplayableId());
        }
    }

    /**
     * Test acquire token silent sync call.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws AuthenticationException
     */
    @Test
    public void testAcquireTokenSilentSyncPositive() throws IOException, AuthenticationException,
            InterruptedException {

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;

        final String response = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenFortestRefreshTokenPositive\",\"token_type\":\"Bearer\","
                + "\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\","
                + "\"scope\":\"*\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // Call refresh token in silent API method
        AuthenticationResult result = context.acquireTokenSilentSync("resource", "clientid",
                TEST_IDTOKEN_USERID);
        verifyRefreshTokenResponse(mockCache, null, result);

        clearCache(context);
    }

    @Test
    public void testAcquireTokenSilentSyncNegative() throws InterruptedException, AuthenticationException,
            IOException, JSONException {
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenThrow(IOException.class).thenReturn(Util.createInputStream(
                Util.getErrorResponseBody("invalid_grant")),
                Util.createInputStream(Util.getSuccessTokenResponse(true, true)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

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

    /**
     * Test acquire token silent sync using assertion call.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws AuthenticationException
     */
    @Test
    public void testAcquireTokenSilentSyncWithAssertionPositive() throws IOException, AuthenticationException,
            InterruptedException {

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(getInstrumentation().getContext());
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);

        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;

        final String response = "{\"id_token\":\"" + TEST_IDTOKEN
                + "\",\"access_token\":\"TokenForSamlAssertionTest\","
                + "\"token_type\":\"Bearer\","
                + "\"expires_in\":\"3600\","
                + "\"expires_on\":\"1368768616\","
                + "\"refresh_token\":\"refresh112\","
                + "\"scope\":\"*\", "
                + "\"client_info\":\"" + Util.TEST_CLIENT_INFO + "\""
                + "}";

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // Call acquire token silent using assertion which will try to fetch refresh token
        AuthenticationResult result = context.acquireTokenSilentSyncWithAssertion("sample assertion",
                com.microsoft.aad.adal.AuthenticationConstants.OAuth2.MSID_OAUTH2_SAML11_BEARER_VALUE, "resource",
                "clientId", TEST_IDTOKEN_USERID);

        // Check if the access token is present in the result and if its stored in the cache
        assertEquals("Token is same as passed in response", "TokenForSamlAssertionTest", result.getAccessToken());
        assertNotNull("Cache is NOT empty for passed userid for regular token",
                mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId",
                        TEST_IDTOKEN_USERID)));

        clearCache(context);
    }

    @Test
    public void testSilentRequestTokenItemNotContainRT() throws InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getInstrumentation().getContext());
        final String resource = "resource";
        final String clientId = "clientId";

        // Add MRRT in the cache
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(
                VALID_AUTHORITY,
                resource,
                clientId,
                TEST_IDTOKEN_USERID,
                TEST_IDTOKEN_UPN
        );

        mrrtTokenCacheItem.setRefreshToken(null);
        mrrtTokenCacheItem.setResource(null);
        mrrtTokenCacheItem.setFamilyClientId("familyClientId");
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);

        mockedCache.setItem(
                CacheKey.createCacheKeyForMRRT(
                        VALID_AUTHORITY,
                        clientId,
                        TEST_IDTOKEN_USERID
                ),
                mrrtTokenCacheItem
        );

        mockedCache.setItem(
                CacheKey.createCacheKeyForMRRT(
                        VALID_AUTHORITY,
                        clientId,
                        TEST_IDTOKEN_UPN
                ),
                mrrtTokenCacheItem
        );

        final AuthenticationContext context = getAuthenticationContext(
                mockContext,
                VALID_AUTHORITY,
                false,
                mockedCache
        );

        try {
            context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_USERID);
            fail("Expecting exception to be thrown");
        } catch (final AuthenticationException e) {
            assertSame(e.getCode(), ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED);
        } finally {
            clearCache(context);
        }
    }

    private void verifyRefreshTokenResponse(ITokenCacheStore mockCache, Exception resultException,
                                            AuthenticationResult result) {
        assertNull("Error is null", resultException);
        assertEquals("Token is same", "TokenFortestRefreshTokenPositive", result.getAccessToken());
        assertNotNull("Cache is NOT empty for this userid for regular token",
                mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId",
                        TEST_IDTOKEN_USERID)));
        assertTrue("Refresh token has userinfo",
                result.getUserInfo().getUserId().equalsIgnoreCase(TEST_IDTOKEN_USERID));
    }

    /**
     * authority and resource are case insensitive. Cache lookup will return
     * item from cache.
     *
     * @throws InterruptedException
     */
    @Test
    public void testAcquireTokenCacheLookup() throws InterruptedException {

        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        final String resource = "Resource" + UUID.randomUUID();
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken(tokenToTest);
        newItem.setRefreshToken("refreshToken");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId("clientId");
        newItem.setUserId("userId124");
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId("userA");
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // acquire token call will return from cache
        context.acquireToken(testActivity.getTestActivity(), resource, "ClienTid", "redirectUri", "userA",
                callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback.getException());
        assertEquals("Same access token in cache", tokenToTest,
                callback.getAuthenticationResult().getAccessToken());
        assertEquals("Same refresh token in cache", "refreshToken",
                callback.getAuthenticationResult().getRefreshToken());
        assertEquals("Same userid in cache", "userId124",
                callback.getAuthenticationResult().getUserInfo()
                        .getUserId());
        assertEquals("Same name in cache", "name",
                callback.getAuthenticationResult().getUserInfo().getGivenName());
        assertEquals("Same familyName in cache", "familyName",
                callback.getAuthenticationResult().getUserInfo()
                        .getFamilyName());
        assertEquals("Same displayid in cache", "userA",
                callback.getAuthenticationResult().getUserInfo()
                        .getDisplayableId());
        assertEquals("Same tenantid in cache", "tenantId",
                callback.getAuthenticationResult().getTenantId());
        clearCache(context);
    }

    /**
     * Test for verifying the userid in the request is different from what's in the cache.
     */
    @Test
    public void testAcquireTokenCacheLookupReturnWrongUserId() throws InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final String resource = "Resource" + UUID.randomUUID();
        final String clientId = "clientid" + UUID.randomUUID();
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();
        final Calendar timeAhead = new GregorianCalendar();
        timeAhead.add(Calendar.MINUTE, EXPIRES_ON_ADJUST_MINS);
        TokenCacheItem refreshItem = new TokenCacheItem();
        refreshItem.setAuthority(VALID_AUTHORITY);
        refreshItem.setResource(resource);
        refreshItem.setClientId(clientId);
        refreshItem.setAccessToken("token");
        refreshItem.setRefreshToken("refreshToken");
        refreshItem.setExpiresOn(timeAhead.getTime());
        refreshItem.setIsMultiResourceRefreshToken(false);
        final UserInfo userinfo = new UserInfo("user2", "test", "test", "idp", "user2");
        refreshItem.setUserInfo(userinfo);
        final String key = CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, "user1");
        mockCache.setItem(key, refreshItem);
        final TokenCacheItem item = mockCache.getItem(key);
        assertNotNull("item is in cache", item);

        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        final MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // Acquire token call will return from cache
        context.acquireTokenSilentAsync(resource, clientId, "user1", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNotNull("Error is not null", callback.getException());
        assertTrue(
                "Error is related to user mismatch",
                callback.getException().getMessage().contains(
                        "User returned by service does not match the one in the request"));
        clearCache(context);
    }

    /**
     * Test when there is no user id passed in request, if there is a token cache item existed in the cache, we'll
     * return it.
     */
    @Test
    public void testAcquireTokenNoUserPassedIn() throws InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);

        // set up cache
        mockCache.removeAll();
        final String resource = "resource";
        final String clientId = "clientId";
        final TokenCacheItem tokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final Calendar timeAhead = new GregorianCalendar();
        timeAhead.add(Calendar.MINUTE, EXPIRES_ON_ADJUST_MINS);
        tokenCacheItem.setExpiresOn(timeAhead.getTime());

        // Store the key without userid into cache
        mockCache.setItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, null), tokenCacheItem);

        final AuthenticationContext context = getAuthenticationContext(getInstrumentation().getContext(), VALID_AUTHORITY, false, mockCache);
        final MockActivity testActivity = new MockActivity();
        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // Acquire token call will return from cache
        context.acquireTokenSilentAsync(resource, clientId, null, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNull(callback.getException());
        assertNotNull(callback.getAuthenticationResult());
        assertNotNull(callback.getAuthenticationResult().getAccessToken());
    }

    @Test
    public void testAcquireTokenCacheLookupMultipleUserLoginHint() throws InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final String resource = "Resource" + UUID.randomUUID();
        final String clientId = "clientid" + UUID.randomUUID();
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken("token1");
        newItem.setRefreshToken("refresh1");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId("userId1");
        newItem.setName("userAname");
        newItem.setFamilyName("userAfamily");
        newItem.setDisplayId("userName1");
        newItem.setTenantId("tenant");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);

        newItem = new TestCacheItem();
        newItem.setToken("token2");
        newItem.setRefreshToken("refresh2");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId("userId2");
        newItem.setName("userBname");
        newItem.setFamilyName("userBfamily");
        newItem.setDisplayId("userName2");
        newItem.setTenantId("tenant");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        // User1
        final CountDownLatch signal = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // Acquire token call will return from cache
        context.acquireTokenSilentAsync(resource, clientId, "userid1", callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback.getException());
        assertEquals("token for user1", "token1",
                callback.getAuthenticationResult().getAccessToken());
        assertEquals("idtoken for user1", "userName1",
                callback.getAuthenticationResult().getUserInfo().getDisplayableId());
        assertEquals("idtoken for user1", "userAname",
                callback.getAuthenticationResult().getUserInfo().getGivenName());

        // User2 with userid call
        final CountDownLatch signal2 = new CountDownLatch(1);
        MockAuthenticationCallback callback2 = new MockAuthenticationCallback(signal2);

        // Acquire token call will return from cache
        context.acquireTokenSilentAsync(resource, clientId, "userid2", callback2);
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback2.getException());
        assertEquals("token for user1", "token2",
                callback2.getAuthenticationResult().getAccessToken());
        assertEquals("idtoken for user1", "userName2",
                callback2.getAuthenticationResult().getUserInfo().getDisplayableId());

        // User2 with loginHint call
        final CountDownLatch signal3 = new CountDownLatch(1);
        MockAuthenticationCallback callback3 = new MockAuthenticationCallback(signal3);

        final MockActivity testActivity = new MockActivity();
        testActivity.mSignal = signal3;
        context.acquireToken(testActivity.getTestActivity(), resource, clientId, "http://redirectUri", "userName1",
                callback3);
        signal3.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        // Check response in callback
        assertNull("Error is null", callback3.getException());
        assertEquals("token for user1", "token1",
                callback3.getAuthenticationResult().getAccessToken());
        assertEquals("idtoken for user1", "userName1",
                callback3.getAuthenticationResult().getUserInfo().getDisplayableId());

        clearCache(context);
    }

    @Test
    @Ignore
    public void testOnActivityResultMissingIntentData() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext authContext = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, null);
        int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        int resultCode = AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE;
        TestLogResponse logResponse = new TestLogResponse();
        String msgToCheck = "onActivityResult BROWSER_FLOW data is null";
        logResponse.listenLogForMessageSegments(msgToCheck);

        // act
        authContext.onActivityResult(requestCode, resultCode, null);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // assert
        assertTrue(logResponse.getMessage().contains(msgToCheck));
    }

    @Test
    public void testOnActivityResultResultCodeCancel() throws InterruptedException {
        final ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        final int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        final int resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL;
        final TestAuthCallback callback = new TestAuthCallback();

        final Intent data = new Intent();
        data.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());

        // act
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", "redirect",
                PromptBehavior.Always, callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertTrue("Returns cancel error",
                callback.getCallbackException() instanceof AuthenticationException);
        assertTrue("Cancel error has message",
                callback.getCallbackException().getMessage().contains("User cancelled the flow"));
    }

    @Test
    public void testOnActivityResultResultCodeError() throws InterruptedException {
        final ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        final int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        final int resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR;
        final TestAuthCallback callback = new TestAuthCallback();

        // act
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", "redirect",
                PromptBehavior.Always, callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        final Intent data = new Intent();
        data.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertTrue("Returns error", callback.getCallbackException() instanceof AuthenticationException);
    }

    @Test
    public void testOnActivityResultResultCodeException() throws InterruptedException {
        final ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        final int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        final int resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION;
        final TestAuthCallback callback = new TestAuthCallback();

        // act
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", "redirect",
                PromptBehavior.Always, callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        final Intent data = new Intent();
        data.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
        AuthenticationException exception = new AuthenticationException(ADALError.AUTH_FAILED);
        data.putExtra(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION,
                exception);
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertTrue("Returns authentication exception",
                callback.getCallbackException() instanceof AuthenticationException);
        assertTrue(
                "Returns authentication exception",
                ((AuthenticationException) callback.getCallbackException()).getCode() == ADALError.AUTH_FAILED);
    }

    @Test
    public void testOnActivityResultResultCodeExceptionMissing() throws InterruptedException {
        final ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        final int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        final int resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION;
        final TestAuthCallback callback = new TestAuthCallback();

        // act
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", "redirect",
                PromptBehavior.Always, callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        final Intent data = new Intent();
        data.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertTrue("Returns authentication exception",
                callback.getCallbackException() instanceof AuthenticationException);
        assertTrue(
                "Returns authentication exception",
                ((AuthenticationException) callback.getCallbackException()).getCode()
                        == ADALError.WEBVIEW_RETURNED_INVALID_AUTHENTICATION_EXCEPTION);
    }

    @Test
    public void testOnActivityResultBrokerResponse() throws InterruptedException {
        final ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cache);
        final int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        final int resultCode = AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE;
        final TestAuthCallback callback = new TestAuthCallback();

        // act
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", "redirect",
                PromptBehavior.Always, callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        final Intent data = new Intent();
        data.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
        data.putExtra(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN, "testAccessToken");
        authContext.onActivityResult(requestCode, resultCode, data);

        // assert
        assertEquals("Same token in response", "testAccessToken",
                callback.getCallbackResult().getAccessToken());
    }

    /**
     * setup cache with userid for normal token and multiresource refresh token
     * bound to one userid. test calls for different resources and users.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testAcquireTokenMultiResourceTokenUserId() throws IOException, InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        final String expectedAT = "accesstoken";
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken(tokenToTest);
        newItem.setRefreshToken("refreshTokenNormal");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId("ClienTid");
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);

        newItem = new TestCacheItem();
        newItem.setToken("");
        newItem.setRefreshToken("refreshTokenMultiResource");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId("ClienTid");
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(true);

        addItemToCache(mockCache, newItem);
        // only one MRRT for same user, client, authority
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final String response = "{\"access_token\":\"accesstoken"
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"29344\",\"expires_on\":\"1368768616\","
                + "\"resource\":1,"
                + "\"refresh_token\":\""
                + "refreshToken" + "\",\"scope\":\"*\",\"id_token\":\"" + TEST_IDTOKEN + "\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        final int requestWaitMs = 200000;
        CountDownLatch signal = new CountDownLatch(1);
        MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // 1st token request, read from cache.
        context.acquireToken(testActivity.getTestActivity(), resource, "ClienTid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(requestWaitMs, TimeUnit.MILLISECONDS);
        assertNull("Error is null", callback.getException());
        assertEquals("Same token in response as in cache", tokenToTest,
                callback.getAuthenticationResult().getAccessToken());

        // 2nd token request, use MRRT to refresh
        signal = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity.getTestActivity(), "dummyResource2", "ClienTid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(requestWaitMs, TimeUnit.MILLISECONDS);

        assertNull("Error is null", callback.getException());
        assertEquals("Same token as refresh token result", expectedAT,
                callback.getAuthenticationResult().getAccessToken());

        // 3rd request, different resource with same userid
        signal = new CountDownLatch(1);
        testActivity = new MockActivity(signal);
        callback = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity.getTestActivity(), "anotherResource123", "ClienTid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(requestWaitMs, TimeUnit.MILLISECONDS);

        assertEquals("Token is returned from refresh token request", expectedAT,
                callback.getAuthenticationResult().getAccessToken());

        // Same call again to use it from cache
        signal = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal);
        callback.setAuthenticationResult(null);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        context.acquireToken(testActivity.getTestActivity(), "anotherResource123", "ClienTid", "redirectUri",
                TEST_IDTOKEN_UPN, callback);
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertEquals("Same token in response as in cache for same call", expectedAT,
                callback.getAuthenticationResult().getAccessToken());

        clearCache(context);
    }

    /**
     * forceRefresh should result in the following behavior:
     * 1) When broker available (installed and user has refresh tokens in broker cache) for current user,
     * bypass local access and refresh token and perform refresh using the broker
     * <p>
     * 2) When the broker is not available (not installed or the user does not have refresh tokens in broker cache)
     * bypass the local access token and use the local refresh token to refresh
     * <p>
     * setup cache with userid for normal access token and AAD  refresh token (MRRT)
     * bound to one userid. verify that the forceRefresh parameter bypasses the local access token
     * and refreshes using the local refresh token
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testAcquireTokenSilentWithForceRefreshWithoutBroker() throws IOException, InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());

        final String redirectUri = "redirectUri";
        final String clientId = "clientId";

        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        final String expectedAT = "accesstoken";
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken(tokenToTest);
        newItem.setRefreshToken("refreshTokenNormal");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);

        newItem = new TestCacheItem();
        newItem.setToken("");
        newItem.setRefreshToken("refreshTokenMultiResource");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(true);

        addItemToCache(mockCache, newItem);
        // only one MRRT for same user, client, authority
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final String response = "{\"access_token\":\"accesstoken"
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"29344\",\"expires_on\":\"1368768616\","
                + "\"resource\":\"" + resource + "\","
                + "\"refresh_token\":\""
                + "refreshToken" + "\",\"scope\":\"*\",\"id_token\":\"" + TEST_IDTOKEN + "\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        final int requestWaitMs = 200000;
        CountDownLatch signal = new CountDownLatch(1);
        MockActivity testActivity = new MockActivity(signal);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signal);

        // 1st token request, read from cache.
        context.acquireTokenSilentAsync(resource, clientId, TEST_IDTOKEN_UPN, callback);
        signal.await(requestWaitMs, TimeUnit.MILLISECONDS);
        assertNull("Error is null", callback.getException());
        assertEquals("Same token in response as in cache", tokenToTest,
                callback.getAuthenticationResult().getAccessToken());

        // 2st token request, forceRefresh false.
        context.acquireTokenSilentAsync(resource, clientId, TEST_IDTOKEN_UPN, false, callback);
        signal.await(requestWaitMs, TimeUnit.MILLISECONDS);
        assertNull("Error is null", callback.getException());
        assertEquals("Same token in response as in cache", tokenToTest,
                callback.getAuthenticationResult().getAccessToken());

        // 3nd token request with force refresh true.
        signal = new CountDownLatch(1);
        callback = new MockAuthenticationCallback(signal);
        context.acquireTokenSilentAsync(resource, clientId, TEST_IDTOKEN_UPN, true, callback);
        signal.await(requestWaitMs, TimeUnit.MILLISECONDS);

        assertNull("Error is null", callback.getException());
        assertEquals("Same token as refresh token result", expectedAT,
                callback.getAuthenticationResult().getAccessToken());

        clearCache(context);
    }

    /**
     * forceRefresh should result in the following behavior:
     * 1) When broker available (installed and user has refresh tokens in broker cache) for current user,
     * bypass local access and refresh token and perform refresh using the broker
     * <p>
     * 2) When the broker is not available (not installed or the user does not have refresh tokens in broker cache)
     * bypass the local access token and use the local refresh token to refresh
     * <p>
     * setup cache with userid for normal access token and AAD  refresh token (MRRT)
     * bound to one userid. verify that the forceRefresh parameter bypasses the local access token
     * and refreshes using the local refresh token
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testAcquireTokenSilentSyncWithForceRefreshWithoutBroker() throws IOException, InterruptedException, AuthenticationException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());

        final String redirectUri = "redirectUri";
        final String clientId = "clientId";

        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        final String expectedAT = "accesstoken";
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken(tokenToTest);
        newItem.setRefreshToken("refreshTokenNormal");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);

        newItem = new TestCacheItem();
        newItem.setToken("");
        newItem.setRefreshToken("refreshTokenMultiResource");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(true);

        addItemToCache(mockCache, newItem);
        // only one MRRT for same user, client, authority
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final String response = "{\"access_token\":\"accesstoken"
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"29344\",\"expires_on\":\"1368768616\","
                + "\"resource\":\"" + resource + "\","
                + "\"refresh_token\":\""
                + "refreshToken" + "\",\"scope\":\"*\",\"id_token\":\"" + TEST_IDTOKEN + "\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // 1st token request, read from cache.
        AuthenticationResult result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN);
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token in response as in cache", tokenToTest,
                result.getAccessToken());

        // 2st token request, forceRefresh false.
        result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN, false);
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token in response as in cache", tokenToTest,
                result.getAccessToken());

        // 3nd token request with force refresh true.
        result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN, true);
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token as refresh token result", expectedAT,
                result.getAccessToken());

        clearCache(context);
    }

    @Test
    public void testAcquireTokenSilentSyncClaimsChallengeWithoutBroker() throws IOException, AuthenticationException, InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());

        final String clientId = "clientId";

        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        final String expectedAT = "accesstoken";
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken(tokenToTest);
        newItem.setRefreshToken("refreshTokenNormal");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);

        newItem = new TestCacheItem();
        newItem.setToken("");
        newItem.setRefreshToken("refreshTokenMultiResource");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(true);

        addItemToCache(mockCache, newItem);
        // only one MRRT for same user, client, authority
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final String response = "{\"access_token\":\"accesstoken"
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"29344\",\"expires_on\":\"1368768616\","
                + "\"resource\":\"" + resource + "\","
                + "\"refresh_token\":\""
                + "refreshToken" + "\",\"scope\":\"*\",\"id_token\":\"" + TEST_IDTOKEN + "\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // 1st token request, read from cache.
        AuthenticationResult result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN);
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token in response as in cache", tokenToTest,
                result.getAccessToken());

        JSONObject email = new JSONObject();
        JSONObject claims = new JSONObject();
        try {
            email.put("email", null);
            claims.put("id_token", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN, claims.toString());
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token as refresh token result", expectedAT,
                result.getAccessToken());

        clearCache(context);
    }

    @Test
    public void testAcquireTokenSilentSyncClientCapabilitiesWithoutBroker() throws IOException, AuthenticationException, InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());

        final String clientId = "clientId";

        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        final String expectedAT = "accesstoken";
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken(tokenToTest);
        newItem.setRefreshToken("refreshTokenNormal");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);

        newItem = new TestCacheItem();
        newItem.setToken("");
        newItem.setRefreshToken("refreshTokenMultiResource");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(true);

        addItemToCache(mockCache, newItem);
        // only one MRRT for same user, client, authority
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final String response = "{\"access_token\":\"accesstoken"
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"29344\",\"expires_on\":\"1368768616\","
                + "\"resource\":\"" + resource + "\","
                + "\"refresh_token\":\""
                + "refreshToken" + "\",\"scope\":\"*\",\"id_token\":\"" + TEST_IDTOKEN + "\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // 1st token request, read from cache.
        AuthenticationResult result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN);
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token in response as in cache", tokenToTest,
                result.getAccessToken());

        List<String> capabilities = new ArrayList<>();
        capabilities.add("CC1");
        capabilities.add("CC2");
        context.setClientCapabilites(capabilities);

        JSONObject email = new JSONObject();
        JSONObject claims = new JSONObject();
        try {
            email.put("email", null);
            claims.put("id_token", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 2nd token request with claims with correct claims
        result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN, claims.toString());
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token as refresh token result", expectedAT,
                result.getAccessToken());

        clearCache(context);
    }

    @Test
    public void testAcquireTokenSilentSyncClientCapabilitiesAndNoClaims() throws IOException, AuthenticationException, InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());

        final String clientId = "clientId";

        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken(tokenToTest);
        newItem.setRefreshToken("refreshTokenNormal");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);

        newItem = new TestCacheItem();
        newItem.setToken("");
        newItem.setRefreshToken("refreshTokenMultiResource");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(true);

        addItemToCache(mockCache, newItem);
        // only one MRRT for same user, client, authority
        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final String response = "{\"access_token\":\"accesstoken"
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"29344\",\"expires_on\":\"1368768616\","
                + "\"resource\":\"" + resource + "\","
                + "\"refresh_token\":\""
                + "refreshToken" + "\",\"scope\":\"*\",\"id_token\":\"" + TEST_IDTOKEN + "\", \"client_info\":\"" + Util.TEST_CLIENT_INFO + "\"}";
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response),
                Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        // 1st token request, read from cache.
        AuthenticationResult result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN);
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token in response as in cache", tokenToTest,
                result.getAccessToken());


        List<String> capabilities = new ArrayList<>();
        capabilities.add("CC1");
        capabilities.add("CC2");
        context.setClientCapabilites(capabilities);

        // 2nd token request with client capabilities only, should return token from cache
        result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN);
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token as refresh token result", tokenToTest,
                result.getAccessToken());

        clearCache(context);
    }


    @Test(expected = AuthenticationException.class)
    public void testInvalidClaimsAcquireTokenSilent() throws AuthenticationException, InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());

        final String clientId = "clientId";

        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        String resource = "Resource" + UUID.randomUUID();
        ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken(tokenToTest);
        newItem.setRefreshToken("refreshTokenNormal");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId(clientId);
        newItem.setUserId(TEST_IDTOKEN_USERID);
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId(TEST_IDTOKEN_UPN);
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);

        final AuthenticationContext context = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        // 1st token request, read from cache.
        AuthenticationResult result = context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN);
        assertNull("Error is null", result.getErrorCode());
        assertEquals("Same token in response as in cache", tokenToTest,
                result.getAccessToken());

        List<String> capabilities = new ArrayList<>();
        capabilities.add("CC1");
        capabilities.add("CC2");
        context.setClientCapabilites(capabilities);

        context.acquireTokenSilentSync(resource, clientId, TEST_IDTOKEN_UPN, "invalid_claims");
    }

    @Test
    public void testAcquireTokenMultiResourceADFSIssue() throws InterruptedException {
        // adfs does not return userid and multiresource token
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final String tokenToTest = "accessToken=" + UUID.randomUUID();
        final String resource = "Resource" + UUID.randomUUID();
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        // add item without userid and normal refresh token
        TestCacheItem newItem = new TestCacheItem();
        newItem.setToken(tokenToTest);
        newItem.setRefreshToken("refreshToken");
        newItem.setAuthority(VALID_AUTHORITY);
        newItem.setResource(resource);
        newItem.setClientId("ClienTid");
        newItem.setUserId("");
        newItem.setName("name");
        newItem.setFamilyName("familyName");
        newItem.setDisplayId("userA");
        newItem.setTenantId("tenantId");
        newItem.setMultiResource(false);

        addItemToCache(mockCache, newItem);

        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);
        MockActivity testActivity = new MockActivity();

        final CountDownLatch signal = new CountDownLatch(1);
        testActivity.mSignal = signal;

        // Acquire token call will return from cache
        context.acquireToken(testActivity.getTestActivity(), resource, "clientid", "redirectUri", "userA", new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult result) {
                assertEquals("Same token in response as in cache", tokenToTest, result.getAccessToken());
                signal.countDown();
            }

            @Override
            public void onError(Exception exc) {
                fail("Unexpected error message.");
            }
        });
        signal.await();

        // Request with different resource will result in prompt since Cache
        // does not have multi resource token
        final CountDownLatch signal2 = new CountDownLatch(1);
        testActivity = new MockActivity();
        testActivity.mSignal = signal2;

        final AuthenticationCallback callback = new TestAuthCallback();
        context.acquireToken(testActivity.getTestActivity(), "anotherResource123", "ClienTid", "redirectUri", "", callback);
        signal.await();

        assertTrue("Attemps to launch", testActivity.mStartActivityRequestCode != -1);

        // Asking with different userid will not return item from cache and try
        // to launch activity
        final CountDownLatch signal3 = new CountDownLatch(1);
        testActivity = new MockActivity();
        testActivity.mSignal = signal3;
        final AuthenticationCallback callback2 = new MockAuthenticationCallback(signal);
        context.acquireToken(testActivity.getTestActivity(), resource, "ClienTid", "redirectUri", "someuser",
                callback2);
        signal3.await();
        assertTrue("Attempts to launch", testActivity.mStartActivityRequestCode != -1);

        clearCache(context);
    }

    @Test
    public void testBrokerRedirectUri() throws UnsupportedEncodingException {
        final ITokenCacheStore cache = mock(ITokenCacheStore.class);
        final AuthenticationContext authContext = new AuthenticationContext(getInstrumentation().getContext(),
                VALID_AUTHORITY, false, cache);

        // act
        String actual = authContext.getRedirectUriForBroker();

        // assert
        assertTrue("should have packagename", actual.contains(TEST_PACKAGE_NAME));
        assertTrue("should have signature url encoded",
                actual.contains(URLEncoder.encode(mTestTag, AuthenticationConstants.ENCODING_UTF8)));
    }

    private AuthenticationContext getAuthenticationContext(Context mockContext, String authority,
                                                           boolean validate, ITokenCacheStore mockCache) {
        return new AuthenticationContext(mockContext, authority, validate, mockCache);
    }

    @Test
    public void testAutoFlowRefreshTokenRequestFailedWithOauthError() throws IOException, InterruptedException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final AuthenticationContext context = getAuthenticationContext(mockContext,
                VALID_AUTHORITY, false, mockCache);

        final AggregatedTelemetryTestClass telemetry = new AggregatedTelemetryTestClass();
        Telemetry.getInstance().registerDispatcher(telemetry, true);

        final CountDownLatch signal = new CountDownLatch(1);
        final MockActivity testActivity = new MockActivity(signal);
        final CountDownLatch signalCallback = new CountDownLatch(1);
        MockAuthenticationCallback callback = new MockAuthenticationCallback(signalCallback);

        final String responseBody = getErrorResponseBody("interaction_required");
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenThrow(new IOException());
        Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(responseBody));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        context.acquireToken(testActivity.getTestActivity(), "resource", "clientId", "redirect", TEST_IDTOKEN_UPN,
                callback);

        final int requestWaitMs = 200000;
        signal.await(requestWaitMs, TimeUnit.MILLISECONDS);

        // Activity will start
        assertEquals("Activity was attempted to start.",
                AuthenticationConstants.UIRequest.BROWSER_FLOW,
                testActivity.mStartActivityRequestCode);

        context.onActivityResult(testActivity.mStartActivityRequestCode,
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE,
                getResponseIntent(callback, "resource", "clientid", "redirect", TEST_IDTOKEN_UPN));
        signalCallback.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

        assertTrue(telemetry.checkOauthError());
        assertTrue(telemetry.checkNoPIIPresent(EventStrings.LOGIN_HINT, TEST_IDTOKEN_UPN));
        assertTrue(telemetry.checkAPIId("100"));
        assertTrue(telemetry.checkCacheEventCount("3"));
        assertFalse(telemetry.checkAPISucceeded());
    }

    /**
     * Add a FoCI token item into the mocked cache store
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
     */
    @Test
    public void testSerializeNullCacheItem() throws AuthenticationException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
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
    @Test
    public void testSerializeInvalidUserId() {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        addFRTCacheItem(mockCache);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        try {
            context.serialize("");
            Assert.fail("not expected");
        } catch (final Exception exception) {
            assertTrue(exception instanceof IllegalArgumentException);
        }

        try {
            context.serialize(null);
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
    @Test
    public void testSerializeValid() throws AuthenticationException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        addFRTCacheItem(mockCache);
        final AuthenticationContext context = getAuthenticationContext(mockContext, TEST_AUTHORITY, false, mockCache);
        final String result = context.serialize(TEST_IDTOKEN_USERID);
        assertNotNull(result);
    }

    /**
     * Test the deserialize() function with a valid deserialized string
     * containing the FoCI token cache item for the user. The function is
     * expected to store the deserialized FoCI token cache item back to the
     * cache store
     */
    @Test
    public void testDeserializeValid() throws AuthenticationException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
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
    @Test
    public void testDeserializeIncompatibleInput() {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
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
    @Test(expected = IllegalArgumentException.class)
    public void testDeserializeNullSerializedBlob() throws AuthenticationException {
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        addFRTCacheItem(mockCache);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        context.deserialize(null);
    }

    /**
     * Test the deserialize() function where the deserialize input is a json
     * token which has no tokencacheitems . The function is expected to
     * throw AuthenticationException
     *
     * @throws AuthenticationException
     */
    @Test(expected = AuthenticationException.class)
    public void testDeserializeNoTokenCacheItem() throws AuthenticationException {
        final String additionalAttributeString = "{\"version\":1}";
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        context.deserialize(additionalAttributeString);
    }

    /**
     * Test the deserialize() function where the deserialize input is a random
     * string. The function is expected to throw AuthenticationException
     */
    @Test(expected = DeserializationAuthenticationException.class)
    public void testDeserializeRandomString() throws AuthenticationException {
        final String ramdomString = "abc";
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        context.deserialize(ramdomString);
    }

    /**
     * Test the deserialize() function where the deserialize input is a json
     * token which missing one attribute which needed in the deserialization of
     * the tokenCacheItem. The function is expected to throw
     * AuthenticationException
     */
    @Test(expected = DeserializationAuthenticationException.class)
    public void testDeserializeMissingAttribute() throws AuthenticationException {
        final String missingAttributeString = "{\"tokenCacheItems\":[{\"authority\":\"https://login.windows.net/ComMon/\",\"refresh_token\":\"FRT\",\"foci\":\"1\"}],\"version\":1}";
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        context.deserialize(missingAttributeString);
    }

    /**
     * Test the deserialize() function where the deserialize input is a json
     * token which missing authority which is needed in the deserialization of
     * the tokenCacheItem. The function is expected to throw
     * DeserializationAuthenticationException
     */
    @Test(expected = DeserializationAuthenticationException.class)
    public void testDeserializeMissingAuthority() throws AuthenticationException {
        final String missingAttributeString = "{\"tokenCacheItems\":[{\"refresh_token\":\"FRT\",\"foci\":\"1\"}],\"version\":1}";
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        context.deserialize(missingAttributeString);
    }

    /**
     * Test the deserialize() function where the deserialize input is a json
     * token which missing Client Id which is needed in the deserialization of
     * the tokenCacheItem. The function is expected to throw
     * DeserializationAuthenticationException
     */
    @Test(expected = DeserializationAuthenticationException.class)
    public void testDeserializeMissingClientId() throws AuthenticationException {
        final String missingAttributeString = "{\"tokenCacheItems\":[{\"authority\":\"https://login.windows.net/ComMon/\",\"refresh_token\":\"FRT\"}],\"version\":1}";
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        context.deserialize(missingAttributeString);
    }

    /**
     * Test the deserialize() function where the deserialize input is a json
     * token which has one additional attribute. The function is expected to
     * store the deserialized FoCI token cache item back to the cache store.
     *
     * @throws AuthenticationException
     */
    @Test
    public void testDeserializeAdditionalAttribute() throws AuthenticationException {
        final String additionalAttributeString = "{\"tokenCacheItems\":[{\"authority\":\"https://login.windows.net/ComMon/\",\"refresh_token\":\"FRT\",\"id_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.\",\"foci\":\"1\"}],\"version\":1,\"comment\":\"no comment\"}";
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
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
    @Test(expected = DeserializationAuthenticationException.class)
    public void testDeserializeDifferentVersion() throws AuthenticationException {
        final String differentVersionString = "{\"tokenCacheItems\":[{\"authority\":\"https://login.windows.net/ComMon/\",\"refresh_token\":\"FRT\",\"id_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.\",\"foci\":\"1\"}],\"version\":2}";
        final FileMockContext mockContext = new FileMockContext(getInstrumentation().getContext());
        final DefaultTokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        final AuthenticationContext context = getAuthenticationContext(mockContext, VALID_AUTHORITY, false, mockCache);
        context.deserialize(differentVersionString);
    }

    @Test
    public void testMergeClaimsWithValidInput() throws JSONException {

        final List<String> capabilities = new ArrayList<>();
        capabilities.add("CP1");
        capabilities.add("CP2");

        String inputClaims = "{\"userinfo\":{\"given_name\":{\"essential\":true},\"email\":{\"essential\":true}},\"id_token\":{\"auth_time\":{\"essential\":true}}}";

        String expectedJson = "{\"userinfo\":{\"given_name\":{\"essential\":true},\"email\":{\"essential\":true}},\"id_token\":" +
                "{\"auth_time\":{\"essential\":true}},\"access_token\":{\"xms_cc\":{\"values\":[\"CP1\",\"CP2\"]}}}";

        String mergedClaims = AuthenticationContext.mergeClaimsWithClientCapabilities(inputClaims, capabilities);

        JSONObject mergedClaimsJson = new JSONObject(mergedClaims);

        assertNotNull(mergedClaimsJson);
        assertEquals("Valid merged claims Json", expectedJson, mergedClaimsJson.toString());
    }

    @Test
    public void testMergeClaimsWithNoClientCapabilities() throws JSONException {

        String inputClaims = "{\"userinfo\":{\"given_name\":{\"essential\":true},\"email\":{\"essential\":true}},\"id_token\":{\"auth_time\":{\"essential\":true}}}";
        String mergedClaims = AuthenticationContext.mergeClaimsWithClientCapabilities(inputClaims, null);

        JSONObject mergedClaimsJson = new JSONObject(mergedClaims);

        assertNotNull(mergedClaimsJson);
        assertEquals("Valid merged claims Json", inputClaims, mergedClaimsJson.toString());
    }

    @Test
    public void testMergeClaimsWithNoInputClaims() throws JSONException {

        final List<String> capabilities = new ArrayList<>();
        capabilities.add("CP1");
        capabilities.add("CP2");

        String expectedClaims = "{\"access_token\":{\"xms_cc\":{\"values\":[\"CP1\",\"CP2\"]}}}";
        String mergedClaims = AuthenticationContext.mergeClaimsWithClientCapabilities(null, capabilities);

        JSONObject mergedClaimsJson = new JSONObject(mergedClaims);

        assertNotNull(mergedClaimsJson);
        assertEquals("Valid merged claims Json", expectedClaims, mergedClaimsJson.toString());
    }

    @Test
    public void testMergeClaimsWithValidInputAccessTokenClaim() throws JSONException {

        final List<String> capabilities = new ArrayList<>();
        capabilities.add("CP1");
        capabilities.add("CP2");

        String inputClaims = "{\"userinfo\":{\"given_name\":{\"essential\":true},\"email\":{\"essential\":true}}," +
                "\"access_token\":{\"auth_time\":{\"essential\":true}}}";

        String expectedJson = "{\"userinfo\":{\"given_name\":{\"essential\":true},\"email\":{\"essential\":true}}," +
                "\"access_token\":{\"auth_time\":{\"essential\":true},\"xms_cc\":{\"values\":[\"CP1\",\"CP2\"]}}}";

        String mergedClaims = AuthenticationContext.mergeClaimsWithClientCapabilities(inputClaims, capabilities);

        JSONObject mergedClaimsJson = new JSONObject(mergedClaims);

        assertNotNull(mergedClaimsJson);
        assertEquals("Valid merged claims Json", expectedJson, mergedClaimsJson.toString());
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
        DefaultTokenCacheStore cache = new DefaultTokenCacheStore(getInstrumentation().getContext());
        cache.removeAll();
        Calendar expiredTime = new GregorianCalendar();
        Logger.d("Test", "Time now:" + expiredTime.toString());
        final int expiryAdjustMins = -60;
        expiredTime.add(Calendar.MINUTE, expiryAdjustMins);
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

    private TokenCacheItem getTokenCacheItem(final String authority, final String resource, final String clientId,
                                             final String userId, final String displayableId) {
        final TokenCacheItem item = new TokenCacheItem();
        item.setAuthority(authority);
        item.setResource(resource);
        item.setClientId(clientId);
        item.setUserInfo(new UserInfo(userId, "givenName", "familyName", "idp", displayableId));
        item.setAccessToken("accessToken");
        item.setRefreshToken("refreshTOken");
        item.setExpiresOn(new Date());

        return item;
    }

    private ITokenCacheStore getMockCache(int minutes, String token, String resource,
                                          String client, String user, boolean isMultiResource) {
        DefaultTokenCacheStore cache = new DefaultTokenCacheStore(getInstrumentation().getContext());
        // Code response
        Calendar timeAhead = new GregorianCalendar();
        Logger.d("Test", "Time now:" + timeAhead.toString());
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

    private class TestCacheItem {
        private String mToken;
        private String mRefreshToken;
        private String mAuthority;
        private String mResource;
        private String mClientId;
        private String mUserId;
        private String mName;
        private String mFamilyName;
        private String mDisplayId;
        private String mTenantId;
        private boolean mIsMultiResource;

        public String getToken() {
            return mToken;
        }

        public void setToken(String token) {
            mToken = token;
        }

        public String getRefreshToken() {
            return mRefreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            mRefreshToken = refreshToken;
        }

        public String getAuthority() {
            return mAuthority;
        }

        public void setAuthority(String authority) {
            mAuthority = authority;
        }

        public String getResource() {
            return mResource;
        }

        public void setResource(String resource) {
            mResource = resource;
        }

        public String getClientId() {
            return mClientId;
        }

        public void setClientId(String clientId) {
            mClientId = clientId;
        }

        public String getUserId() {
            return mUserId;
        }

        public void setUserId(String userId) {
            mUserId = userId;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getFamilyName() {
            return mFamilyName;
        }

        public void setFamilyName(String familyName) {
            mFamilyName = familyName;
        }

        public String getDisplayId() {
            return mDisplayId;
        }

        public void setDisplayId(String displayId) {
            mDisplayId = displayId;
        }

        public String getTenantId() {
            return mTenantId;
        }

        public void setTenantId(String tenantId) {
            mTenantId = tenantId;
        }

        public boolean isMultiResource() {
            return mIsMultiResource;
        }

        public void setMultiResource(boolean multiResource) {
            mIsMultiResource = multiResource;
        }
    }

    private ITokenCacheStore addItemToCache(ITokenCacheStore cache, TestCacheItem newItem) {
        // Code response
        Calendar timeAhead = new GregorianCalendar();
        Logger.d(TAG, "addItemToCache Time now:" + timeAhead.toString());
        timeAhead.add(Calendar.MINUTE, EXPIRES_ON_ADJUST_MINS);
        TokenCacheItem refreshItem = new TokenCacheItem();
        refreshItem.setAuthority(newItem.getAuthority());
        if (!newItem.isMultiResource()) {
            refreshItem.setResource(newItem.getResource());
        }
        refreshItem.setClientId(newItem.getClientId());
        refreshItem.setAccessToken(newItem.getToken());
        refreshItem.setRefreshToken(newItem.getRefreshToken());
        refreshItem.setExpiresOn(timeAhead.getTime());
        refreshItem.setIsMultiResourceRefreshToken(newItem.isMultiResource());
        refreshItem.setTenantId(newItem.getTenantId());
        refreshItem.setUserInfo(new UserInfo(
                newItem.getUserId(), newItem.getName(), newItem.getFamilyName(), "", newItem.getDisplayId()));
        final String keyUserId, keyUpn;
        if (newItem.isMultiResource()) {
            keyUserId = CacheKey.createCacheKeyForMRRT(
                    newItem.getAuthority(), newItem.getClientId(), newItem.getUserId());
            keyUpn = CacheKey.createCacheKeyForMRRT(
                    newItem.getAuthority(), newItem.getClientId(), newItem.getDisplayId());
        } else {
            keyUserId = CacheKey.createCacheKeyForRTEntry(
                    newItem.getAuthority(), newItem.getResource(), newItem.getClientId(), newItem.getUserId());
            keyUpn = CacheKey.createCacheKeyForRTEntry(
                    newItem.getAuthority(), newItem.getResource(), newItem.getClientId(), newItem.getDisplayId());
        }

        Logger.i(TAG, "Key with userId. ", "UserId: " + keyUserId);
        cache.setItem(keyUserId, refreshItem);
        TokenCacheItem item = cache.getItem(keyUserId);
        assertNotNull("item is in cache", item);


        Logger.i(TAG, "Key with upn. ", "UPN: " + keyUpn);
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

    /**
     * Mock activity
     */
    class MockActivity {

        // No idea why this is the starting value.  Will have to wait for test rewrites to fix.
        private static final int STARTING_ACTIVITY_REQUEST_CODE = -123;

        private static final String TAG = "MockActivity";

        private int mStartActivityRequestCode = STARTING_ACTIVITY_REQUEST_CODE;

        private Intent mStartActivityIntent;

        private CountDownLatch mSignal;

        private Bundle mStartActivityOptions;

        private Activity mActivity;

        MockActivity(CountDownLatch signal) {
            mSignal = signal;
            doMockActivity();
        }

        @SuppressLint("Registered")
        MockActivity() {
            doMockActivity();
        }

        private void doMockActivity() {
            mActivity = Mockito.mock(Activity.class);
            Mockito.when(mActivity.getPackageName()).thenReturn(ReflectionUtils.TEST_PACKAGE_NAME);

            Mockito.doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) {
                    mStartActivityIntent = invocationOnMock.getArgument(0);
                    mStartActivityRequestCode = invocationOnMock.getArgument(1);
                    countDownLatch();
                    return null;
                }
            }).when(mActivity).startActivityForResult(any(Intent.class), anyInt());

            Mockito.doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) {
                    mStartActivityIntent = invocationOnMock.getArgument(0);
                    mStartActivityRequestCode = invocationOnMock.getArgument(1);
                    mStartActivityOptions = invocationOnMock.getArgument(2);
                    countDownLatch();
                    return null;
                }
            }).when(mActivity).startActivityForResult(any(Intent.class), anyInt(), any(Bundle.class));
        }

        Activity getTestActivity() {
            return mActivity;
        }

        void countDownLatch() {
            if (mSignal != null) {
                mSignal.countDown();
            }
        }
    }
}