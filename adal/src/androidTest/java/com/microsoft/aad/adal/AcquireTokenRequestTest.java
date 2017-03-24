//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.adal;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;
import android.util.Log;

import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AcquireTokenRequest}.
 */
public final class AcquireTokenRequestTest extends AndroidTestCase {

    private static final String TAG = AcquireTokenRequestTest.class.getSimpleName();
    /**
     * Check case-insensitive lookup
     */
    private static final String VALID_AUTHORITY = "https://login.windows.net/test.onmicrosoft.com";
    private static final int ACTIVITY_TIME_OUT = 1000;
    private static final int MINUS_MINUITE = 10;
    private static final int EXTEND_MINUS_MINUTE = 60;
    private static final String TEST_UPN = "testupn";
    private static final String TEST_USERID = "testuserid";
    private static final int ACCOUNT_MANAGER_ERROR_CODE_BAD_AUTHENTICATION = 9;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Log.d(TAG, "setup key at settings");
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        Log.d(TAG, "setup key at settings");
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            // use same key for tests
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
            final int iterationCount = 100;
            final int keyLength = 256;
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes("UTF-8"), iterationCount, keyLength));
            SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        Logger.getInstance().setExternalLogger(null);
        AuthenticationSettings.INSTANCE.setUseBroker(false);
        super.tearDown();
    }

    /**
     * Test if there is a valid AT in local cache, will use it even we can switch to broker for auth.
     */
    @SmallTest
    public void testFavorLocalCacheValidATInLocalCache()
            throws PackageManager.NameNotFoundException, OperationCanceledException, IOException,
            AuthenticatorException, InterruptedException {

        // Make sure AT is not expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(MINUS_MINUITE), false, false, null);

        final AccountManager mockedAccountManager = getMockedAccountManager();
        mockAccountManagerGetAccountBehavior(mockedAccountManager);
        final FileMockContext mockContext = createMockContext();
        mockContext.setMockedAccountManager(mockedAccountManager);

        prepareAuthForBrokerCall();

        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireTokenSilentAsync("resource", "clientid", TEST_USERID, callback);

        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNull(callback.getCallbackException());
        assertNotNull(callback.getCallbackResult());
        assertTrue(callback.getCallbackResult().getAccessToken().equals("I am an AT"));

        cacheStore.removeAll();
    }

    /**
     * Test if there is a RT in cache, we'll use the RT first. If it fails and if we can switch to broker for auth,
     * will switch to broker.
     */
    @SmallTest
    public void testFavorLocalCacheUseLocalRTFailsSwitchToBroker()
            throws PackageManager.NameNotFoundException, OperationCanceledException, IOException,
            AuthenticatorException, InterruptedException {
        // Make sure AT is expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), true, true, null);

        final AccountManager mockedAccountManager = getMockedAccountManager();
        mockAccountManagerGetAccountBehavior(mockedAccountManager);
        mockGetAuthTokenCall(mockedAccountManager, true);

        final FileMockContext mockContext = createMockContext();
        mockContext.setMockedAccountManager(mockedAccountManager);

        final String errorCode = "invalid_request";
        prepareFailedHttpUrlConnection(errorCode, errorCode);

        prepareAuthForBrokerCall();

        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireTokenSilentAsync("resource", "clientid", TEST_USERID, callback);

        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        // verify getAuthToken called once
        verify(mockedAccountManager, times(1)).getAuthToken(Mockito.any(Account.class), Matchers.anyString(),
                Matchers.any(Bundle.class), Matchers.eq(false), (AccountManagerCallback<Bundle>) Matchers.eq(null),
                Matchers.any(Handler.class));

        // verify returned AT is as expected
        assertNull(callback.getCallbackException());
        assertNotNull(callback.getCallbackResult());
        assertTrue(callback.getCallbackResult().getAccessToken().equals("I am an access token from broker"));

        //verify local cache is not cleared
        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientid",
                TEST_USERID)));
        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientid",
                TEST_UPN)));

        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, "clientid", TEST_UPN)));
        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, "clientid", TEST_USERID)));

        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY,
                AuthenticationConstants.MS_FAMILY_ID, TEST_UPN)));
        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY,
                AuthenticationConstants.MS_FAMILY_ID, TEST_USERID)));

        assertFalse(authContext.getCache().getAll().hasNext());
        cacheStore.removeAll();
    }

    /**
     * Test if there is a RT in cache, we'll use the RT first. If it fails with invalid_grant, local cache will be
     * cleared. If we can switch to broker, will switch to broker for auth.
     */
    @SmallTest
    public void testFavorLocalCacheUseLocalRTFailsWithInvalidGrantSwitchToBroker()
            throws PackageManager.NameNotFoundException, OperationCanceledException,
            IOException, AuthenticatorException, InterruptedException {

        // Make sure AT is expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), true, false, null);

        final AccountManager mockedAccountManager = getMockedAccountManager();
        mockAccountManagerGetAccountBehavior(mockedAccountManager);
        mockGetAuthTokenCall(mockedAccountManager, true);

        final FileMockContext mockContext = createMockContext();
        mockContext.setMockedAccountManager(mockedAccountManager);

        prepareFailedHttpUrlConnection("invalid_grant");
        prepareAuthForBrokerCall();

        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireTokenSilentAsync("resource", "clientid", TEST_USERID, callback);

        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        // verify getAuthToken called once
        verify(mockedAccountManager, times(1)).getAuthToken(Mockito.any(Account.class), Matchers.anyString(),
                Matchers.any(Bundle.class), Matchers.eq(false), (AccountManagerCallback<Bundle>) Matchers.eq(null),
                Matchers.any(Handler.class));

        // verify returned AT is as expected
        assertNull(callback.getCallbackException());
        assertNotNull(callback.getCallbackResult());
        assertTrue(callback.getCallbackResult().getAccessToken().equals("I am an access token from broker"));

        //verify local cache is cleared
        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientid",
                TEST_USERID)));
        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientid",
                TEST_UPN)));

        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, "clientid", TEST_UPN)));
        assertNull(cacheStore.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, "clientid", TEST_USERID)));

        assertFalse(authContext.getCache().getAll().hasNext());
        cacheStore.removeAll();
    }

    /**
     * Test if there is a RT in cache, we'll use the RT first. If it fails and if we can switch to broker for auth,
     * will switch to broker.
     */
    @SmallTest
    public void testFavorLocalCacheUseLocalRTSucceeds()
            throws PackageManager.NameNotFoundException, OperationCanceledException,
            IOException, AuthenticatorException, InterruptedException {
        // Make sure AT is expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), false, false, null);

        final AccountManager mockedAccountManager = getMockedAccountManager();
        mockAccountManagerGetAccountBehavior(mockedAccountManager);
        mockGetAuthTokenCall(mockedAccountManager, true);

        final FileMockContext mockContext = createMockContext();
        mockContext.setMockedAccountManager(mockedAccountManager);

        //mock HttpUrlConnection for refresh token request
        prepareSuccessHttpUrlConnection();

        prepareAuthForBrokerCall();

        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireTokenSilentAsync("resource", "clientid", TEST_USERID, callback);

        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        // verify getAuthToken not called
        verify(mockedAccountManager, times(0)).getAuthToken(Mockito.any(Account.class), Matchers.anyString(),
                Matchers.any(Bundle.class), Matchers.eq(false), (AccountManagerCallback<Bundle>) Matchers.eq(null),
                Matchers.any(Handler.class));

        // verify returned AT is as expected
        assertNull(callback.getCallbackException());
        assertNotNull(callback.getCallbackResult());
        assertTrue(callback.getCallbackResult().getAccessToken().equals("I am a new access token"));

        assertTrue(cacheStore.getAll().hasNext());
        cacheStore.removeAll();
    }

    @SmallTest
    public void testBothLocalAndBrokerSilentAuthFailedSwitchedToBrokerForInteractive()
            throws OperationCanceledException, IOException, AuthenticatorException,
            PackageManager.NameNotFoundException, InterruptedException {

        // Make sure AT is expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), false, false, null);

        final AccountManager mockedAccountManager = getMockedAccountManager();
        mockAccountManagerGetAccountBehavior(mockedAccountManager);
        mockGetAuthTokenCallWithThrow(mockedAccountManager);
        mockAddAccountCall(mockedAccountManager);

        final FileMockContext mockContext = createMockContext();
        mockContext.setMockedAccountManager(mockedAccountManager);

        prepareFailedHttpUrlConnection("invalid_request");
        prepareAuthForBrokerCall();

        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        final TestAuthCallback callback = new TestAuthCallback();

        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid",
                authContext.getRedirectUriForBroker(), TEST_UPN, callback);

        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        // verify getAuthToken called once
        verify(mockedAccountManager, times(1)).getAuthToken(Mockito.any(Account.class), Matchers.anyString(),
                Matchers.any(Bundle.class), Matchers.eq(false), (AccountManagerCallback<Bundle>) Matchers.eq(null),
                Matchers.any(Handler.class));

        verify(mockedAccountManager, times(1)).addAccount(
                Matchers.refEq(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE),
                anyString(), (String[]) Matchers.eq(null),
                Matchers.any(Bundle.class), (Activity) Matchers.eq(null), (AccountManagerCallback<Bundle>) Matchers.
                        eq(null), Matchers.any(Handler.class));

        assertFalse(cacheStore.getAll().hasNext());
        cacheStore.removeAll();
    }

    @SmallTest
    public void testLocalSilentFailedBrokerSilentReturnErrorCannotTryWithInteractive()
            throws OperationCanceledException, IOException, AuthenticatorException,
            PackageManager.NameNotFoundException, InterruptedException {
        // Make sure AT is expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), false, false, null);

        final AccountManager mockedAccountManager = getMockedAccountManager();
        mockAccountManagerGetAccountBehavior(mockedAccountManager);
        mockGetAuthTokenCall(mockedAccountManager, false);
        mockAddAccountCall(mockedAccountManager);

        final FileMockContext mockContext = createMockContext();
        mockContext.setMockedAccountManager(mockedAccountManager);

        prepareFailedHttpUrlConnection("invalid_request");
        prepareAuthForBrokerCall();

        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        final TestAuthCallback callback = new TestAuthCallback();

        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid",
                authContext.getRedirectUriForBroker(), TEST_UPN, callback);

        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        // verify getAuthToken called once
        verify(mockedAccountManager, times(1)).getAuthToken(Mockito.any(Account.class), Matchers.anyString(),
                Matchers.any(Bundle.class), Matchers.eq(false), (AccountManagerCallback<Bundle>) Matchers.eq(null),
                Matchers.any(Handler.class));

        verify(mockedAccountManager, times(0)).addAccount(
                Matchers.refEq(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE),
                anyString(), (String[]) Matchers.eq(null),
                Matchers.any(Bundle.class), (Activity) Matchers.eq(null), (AccountManagerCallback<Bundle>) Matchers.
                        eq(null), Matchers.any(Handler.class));

        assertFalse(cacheStore.getAll().hasNext());
        cacheStore.removeAll();
    }

    private ITokenCacheStore getTokenCache(final Date expiresOn, final boolean storeMRRT, final boolean storeFRT, final Date extendedExpiresOn) {
        // prepare valid item in cache
        final UserInfo userInfo = new UserInfo();
        userInfo.setDisplayableId(TEST_UPN);
        userInfo.setUserId(TEST_USERID);
        final String accessToken = "I am an AT";
        final String refreshToken = "I am a RT";
        final String idToken = "I am an id token";
        final String resource = "resource";
        final String clientId = "clientid";
        
        final AuthenticationResult result;
        if (extendedExpiresOn == null) {
            result = new AuthenticationResult(accessToken, refreshToken, expiresOn, storeMRRT, 
                    userInfo, "", idToken, null);
        } else {
            result = new AuthenticationResult(accessToken, refreshToken, expiresOn, storeMRRT, 
                    userInfo, "", idToken, extendedExpiresOn);
        }
        
        final ITokenCacheStore cacheStore = new DefaultTokenCacheStore(getContext());
        cacheStore.removeAll();
        final TokenCacheItem regularRTItem = TokenCacheItem.createRegularTokenCacheItem(VALID_AUTHORITY,
                resource, clientId, result);
        cacheStore.setItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_USERID),
                regularRTItem);
        cacheStore.setItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_UPN),
                regularRTItem);

        if (storeMRRT) {
            final TokenCacheItem mrrtItem = TokenCacheItem.createMRRTTokenCacheItem(VALID_AUTHORITY, clientId, result);
            cacheStore.setItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_UPN), mrrtItem);
            cacheStore.setItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_USERID), mrrtItem);
        }

        if (storeFRT) {
            result.setFamilyClientId(AuthenticationConstants.MS_FAMILY_ID);
            final TokenCacheItem frtItem = TokenCacheItem.createFRRTTokenCacheItem(VALID_AUTHORITY, result);
            cacheStore.setItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY,
                    AuthenticationConstants.MS_FAMILY_ID, TEST_USERID), frtItem);
            cacheStore.setItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY,
                    AuthenticationConstants.MS_FAMILY_ID, TEST_UPN), frtItem);
        }

        return cacheStore;
    }

    @SmallTest
    public void testVerifyBrokerRedirectUriValid() throws PackageManager.NameNotFoundException,
            InterruptedException, OperationCanceledException, IOException, AuthenticatorException {
        final AccountManager mockedAccountManager = getMockedAccountManager();
        mockAddAccountCall(mockedAccountManager);

        final FileMockContext mockContext = new FileMockContext(getContext());
        mockContext.setMockedAccountManager(mockedAccountManager);
        mockContext.setMockedPackageManager(getMockedPackageManager());

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);

        //test@case valid redirect uri
        final String testRedirectUri = "msauth://" + mockContext.getPackageName() + "/"
                + URLEncoder.encode(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_SIGNATURE,
                AuthenticationConstants.ENCODING_UTF8);
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid",
                testRedirectUri, "loginHint", callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        final int requestCode = AuthenticationConstants.UIRequest.BROWSER_FLOW;
        final int resultCode = AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE;

        final Intent data = new Intent();
        data.putExtra(AuthenticationConstants.Browser.REQUEST_ID, callback.hashCode());
        data.putExtra(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN, "testAccessToken");
        authContext.onActivityResult(requestCode, resultCode, data);

        assertNull(callback.getCallbackException());
    }

    @SmallTest
    public void testVerifyBrokerRedirectUriInvalidPrefix() throws PackageManager.NameNotFoundException,
            InterruptedException {
        final FileMockContext mockContext = createMockContext();

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        final String testRedirectUri = "http://helloApp";

        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", testRedirectUri,
                "loginHint", callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNotNull(callback.getCallbackException());
        assertTrue(callback.getCallbackException() instanceof UsageAuthenticationException);
        final UsageAuthenticationException usageAuthenticationException
                = (UsageAuthenticationException) callback.getCallbackException();
        assertTrue(usageAuthenticationException.getMessage().contains("prefix"));
    }

    @SmallTest
    public void testVerifyBrokerRedirectUriInvalidPackageName() throws NoSuchAlgorithmException,
            PackageManager.NameNotFoundException, InterruptedException {
        final FileMockContext mockContext = createMockContext();

        prepareAuthForBrokerCall();
        final TestAuthCallback callback = new TestAuthCallback();
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        final String testRedirectUri = "msauth://testapp/" + getEncodedTestingSignature();

        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", testRedirectUri,
                "loginHint", callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNotNull(callback.getCallbackException());
        assertTrue(callback.getCallbackException() instanceof UsageAuthenticationException);
        final UsageAuthenticationException usageAuthenticationException
                = (UsageAuthenticationException) callback.getCallbackException();
        assertTrue(usageAuthenticationException.getMessage().contains("package name"));
    }

    @SmallTest
    public void testVerifyBrokerRedirectUriInvalidSignature()
            throws PackageManager.NameNotFoundException, InterruptedException {

        final FileMockContext mockContext = createMockContext();

        prepareAuthForBrokerCall();

        final TestAuthCallback callback = new TestAuthCallback();
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        final String testRedirectUri = "msauth://" + mockContext.getPackageName() + "/falsesignH070%3D";

        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", testRedirectUri,
                "loginHint", callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNotNull(callback.getCallbackException());
        assertTrue(callback.getCallbackException() instanceof UsageAuthenticationException);
        final UsageAuthenticationException usageAuthenticationException
                = (UsageAuthenticationException) callback.getCallbackException();
        assertTrue(usageAuthenticationException.getMessage().contains("signature"));
    }


    /**
     * Test for returning a valid stale AT when ExtendedLifetime is on and the server is down.
     */
    @SmallTest
    public void testResiliencyTokenReturnExtendedLifetimeOnPositive() throws PackageManager.NameNotFoundException,
            NoSuchAlgorithmException, OperationCanceledException, IOException, AuthenticatorException,
            InterruptedException {
        // make sure AT's expires_in is expired and ext_expires_in is not expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), false, false, getExpireDate(EXTEND_MINUS_MINUTE));

        final FileMockContext mockContext = createMockContext();
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        authContext.setExtendedLifetimeEnabled(true);

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getErrorResponseBody("HTTP_GATEWAY_TIMEOUT")),
                Util.createInputStream(Util.getErrorResponseBody("HTTP_GATEWAY_TIMEOUT")));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, HttpURLConnection.HTTP_GATEWAY_TIMEOUT);

        try {
            final AuthenticationResult result = authContext.acquireTokenSilentSync("resource", "clientid", TEST_USERID);
            verify(mockedConnection, times(2)).getInputStream();
            assertNotNull(result);
            assertTrue(result.getAccessToken().equals("I am an AT"));
            assertTrue(result.isExtendedLifeTimeToken());
            assertNotNull(result.getExtendedExpiresOn());
            assertTrue(!TokenCacheItem.isTokenExpired(result.getExtendedExpiresOn()));
        } catch (final AuthenticationException exception) {
            fail("Did not expect an exception");
        } finally {
            cacheStore.removeAll();
        }
    }

    /**
     * Test for throwing exception when the request is rejected by server through there is a valid 
     * stale AT in the cache and the ExtendedLifetime is on.
     */
    @SmallTest
    public void testResiliencyTokenReturnExtendedLifetimeOnwithRetryFail() throws PackageManager.NameNotFoundException,
            NoSuchAlgorithmException, OperationCanceledException, IOException, AuthenticatorException,
            InterruptedException {
        // make sure AT's expires_in is expired and ext_expires_in is not expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), false, false, getExpireDate(EXTEND_MINUS_MINUTE));

        final FileMockContext mockContext = createMockContext();
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        authContext.setExtendedLifetimeEnabled(true);
        final TestAuthCallback callback = new TestAuthCallback();

        //mock http response
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getErrorResponseBody("HTTP_GATEWAY_TIMEOUT")),
                Util.createInputStream(Util.getErrorResponseBody("HTTP_BAD_GATEWAY")));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, HttpURLConnection.HTTP_BAD_GATEWAY);

        try {
            authContext.acquireTokenSilentSync("resource", "clientid", TEST_USERID);
            fail("Expect an exception");
        } catch (final AuthenticationException exception) {
            verify(mockedConnection, times(2)).getInputStream();
            assertNotNull(exception);
            assertTrue(exception.getCode() == ADALError.AUTH_FAILED_NO_TOKEN);
            assertTrue(exception.getCause() instanceof AuthenticationException);
            assertTrue(((AuthenticationException) exception.getCause()).getCode() == ADALError.SERVER_ERROR);
        } finally {
            cacheStore.removeAll();
        }
    }

    /**
     * Test for throwing exception when the server is down and ExtendedLifetime is on
     * but AT is expired either in terms of expires_on or ext_expires_on
     */
    @SmallTest
    public void testResiliencyTokenReturnExtendedLifetimeOnwithExpiredStaleAT() throws PackageManager.NameNotFoundException,
            NoSuchAlgorithmException, OperationCanceledException, IOException, AuthenticatorException,
            InterruptedException {
        // make sure AT's expires_in is expired and ext_expires_in is expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), false, false, getExpireDate(-1));

        final FileMockContext mockContext = createMockContext();
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        authContext.setExtendedLifetimeEnabled(true);
        
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getErrorResponseBody("HTTP_GATEWAY_TIMEOUT")));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);

        try {
            authContext.acquireTokenSilentSync("resource", "clientid", TEST_USERID);
            fail("Expect an exception");
        } catch (final AuthenticationException exception) {
            verify(mockedConnection, times(2)).getInputStream();
            assertNotNull(exception);
            assertTrue(exception.getCode() == ADALError.AUTH_FAILED_NO_TOKEN);
            assertTrue(exception.getCause() instanceof AuthenticationException);
            assertTrue(((AuthenticationException) exception.getCause()).getCode() == ADALError.SERVER_ERROR);
        } finally {
            cacheStore.removeAll();
        }
    }

    /**
     * Test for returning new AT from the server when the
     * ExtendedLifetime is on and the retry succeeds
     */
    @SmallTest
    public void testResiliencyTokenReturnExtendedLifetimeOnwithValidRetry() throws PackageManager.NameNotFoundException,
            NoSuchAlgorithmException, OperationCanceledException, IOException, AuthenticatorException,
            InterruptedException {
        // make sure AT's expires_in is expired and ext_expires_in is not expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), false, false, getExpireDate(EXTEND_MINUS_MINUTE));

        final FileMockContext mockContext = createMockContext();
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        authContext.setExtendedLifetimeEnabled(true);

        //mock http response
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getErrorResponseBody("HTTP_GATEWAY_TIMEOUT")),
                Util.createInputStream(Util.getSuccessTokenResponse(false, false)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, HttpURLConnection.HTTP_OK);

        try {
            final AuthenticationResult result = authContext.acquireTokenSilentSync("resource", "clientid", TEST_USERID);
            verify(mockedConnection, times(2)).getInputStream();
            assertNotNull(result);
            assertTrue(result.getAccessToken().equals("I am a new access token"));
            assertTrue(!result.isExtendedLifeTimeToken());
        } catch (final AuthenticationException exception) {
            fail("Did not expect an exception");
        } finally {
            cacheStore.removeAll();
        }
    }

    /**
     * Test for throwing an exception when ExtendedLifetime is off, the server is down
     * and AT is expired in the term of expires_in but still valid in ext_expires_in
     */
    @SmallTest
    public void testResiliencyTokenReturnExtendedLifetimeOff() throws PackageManager.NameNotFoundException,
            NoSuchAlgorithmException, OperationCanceledException, IOException, AuthenticatorException,
            InterruptedException {
        // make sure AT's expires_in is expired and ext_expires_in is not expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), false, false, getExpireDate(EXTEND_MINUS_MINUTE));

        final FileMockContext mockContext = createMockContext();
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        authContext.setExtendedLifetimeEnabled(false);

        //mock http response
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getErrorResponseBody("HTTP_GATEWAY_TIMEOUT")),
                Util.createInputStream(Util.getErrorResponseBody("HTTP_GATEWAY_TIMEOUT")));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, HttpURLConnection.HTTP_GATEWAY_TIMEOUT);

        try {
            authContext.acquireTokenSilentSync("resource", "clientid", TEST_USERID);
            fail("Expect an exception");
        } catch (final AuthenticationException exception) {
            verify(mockedConnection, times(2)).getInputStream();
            assertNotNull(exception);
            assertTrue(exception.getCode() == ADALError.AUTH_FAILED_NO_TOKEN);
            assertTrue(exception.getCause() instanceof AuthenticationException);
            assertTrue(((AuthenticationException) exception.getCause()).getCode() == ADALError.SERVER_ERROR);
        } finally {
            cacheStore.removeAll();
        }
    }

    /**
     * Test for throwing exception when the request is rejected by server through there is a valid 
     * stale AT in the cache and the ExtendedLifetime is on.
     */
    @SmallTest
    public void testResiliencyTokenReturnExtendedLifetimeOnwithoutRetry() throws PackageManager.NameNotFoundException,
            NoSuchAlgorithmException, OperationCanceledException, IOException, AuthenticatorException,
            InterruptedException {
        // make sure AT's expires_in is expired and ext_expires_in is not expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), false, false, getExpireDate(EXTEND_MINUS_MINUTE));

        final FileMockContext mockContext = createMockContext();
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        authContext.setExtendedLifetimeEnabled(true);
        final TestAuthCallback callback = new TestAuthCallback();

        //mock http response
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getErrorResponseBody("HTTP_BAD_GATEWAY")));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_GATEWAY);

        try {
            authContext.acquireTokenSilentSync("resource", "clientid", TEST_USERID);
            fail("Expect an exception");
        } catch (final AuthenticationException exception) {
            verify(mockedConnection, times(1)).getInputStream();
            assertNotNull(exception);
            assertTrue(exception.getCode() == ADALError.AUTH_FAILED_NO_TOKEN);
            assertTrue(exception.getCause() instanceof AuthenticationException);
            assertTrue(((AuthenticationException) exception.getCause()).getCode() == ADALError.SERVER_ERROR);
        } finally {
            cacheStore.removeAll();
        }
    }

    public void testResiliencyTokenReturnExtendedLifetimeOnwithNullAccessTokenCacheItem() throws PackageManager.NameNotFoundException,
            NoSuchAlgorithmException, OperationCanceledException, IOException, AuthenticatorException,
            InterruptedException {
        // make sure AT's expires_in is expired and ext_expires_in is expired
        final ITokenCacheStore cacheStore = getTokenCache(getExpireDate(-MINUS_MINUITE), true, true, getExpireDate(EXTEND_MINUS_MINUTE));
        cacheStore.removeItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId", TEST_USERID));
        cacheStore.removeItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId", TEST_UPN));
        cacheStore.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, "clientId", TEST_USERID)).setFamilyClientId(AuthenticationConstants.MS_FAMILY_ID);
        cacheStore.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, "clientId", TEST_UPN)).setFamilyClientId(AuthenticationConstants.MS_FAMILY_ID);
        
        final FileMockContext mockContext = createMockContext();
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false, cacheStore);
        authContext.setExtendedLifetimeEnabled(true);
        
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(Util.getErrorResponseBody("HTTP_GATEWAY_TIMEOUT")),
                Util.createInputStream(Util.getErrorResponseBody("HTTP_GATEWAY_TIMEOUT")));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, HttpURLConnection.HTTP_GATEWAY_TIMEOUT);

        try {
            authContext.acquireTokenSilentSync("resource", "clientid", TEST_USERID);
            fail("Expect an exception");
        } catch (final AuthenticationException exception) {
            verify(mockedConnection, times(2)).getInputStream();
            assertNotNull(exception);
            assertTrue(exception.getCode() == ADALError.AUTH_FAILED_NO_TOKEN);
            assertTrue(exception.getCause() instanceof AuthenticationException);
            assertTrue(((AuthenticationException) exception.getCause()).getCode() == ADALError.SERVER_ERROR);
        } finally {
            cacheStore.removeAll();
        }
    }
    
    @SmallTest
    public void testVerifyManifestPermissionMissingGetAccountsPermission() throws InterruptedException, PackageManager.NameNotFoundException {
        final FileMockContext mockContext = createMockContext();
        when(mockContext.getPackageManager().checkPermission(Mockito.refEq("android.permission.GET_ACCOUNTS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_DENIED);
        AuthenticationSettings.INSTANCE.setUseBroker(true);

        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        
        final TestAuthCallback callback = new TestAuthCallback();
        authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", authContext.getRedirectUriForBroker(),
                "loginHint", callback);
        final CountDownLatch signal = new CountDownLatch(1);
        signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

        assertNotNull(callback.getCallbackException());
        assertTrue(callback.getCallbackException() instanceof UsageAuthenticationException);
        final UsageAuthenticationException usageAuthenticationException
                = (UsageAuthenticationException) callback.getCallbackException();
        assertTrue(usageAuthenticationException.getMessage().contains("GET_ACCOUNTS"));
        assertEquals(ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING, usageAuthenticationException.getCode());
    }

    @SmallTest
    public void testVerifyManifestPermissionMissingMultiPermissions() throws InterruptedException, PackageManager.NameNotFoundException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            final FileMockContext mockContext = createMockContext();
            when(mockContext.getPackageManager().checkPermission(Mockito.refEq("android.permission.GET_ACCOUNTS"),
                    Mockito.anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
            when(mockContext.getPackageManager().checkPermission(Mockito.refEq("android.permission.MANAGE_ACCOUNTS"),
                    Mockito.anyString())).thenReturn(PackageManager.PERMISSION_DENIED);
            when(mockContext.getPackageManager().checkPermission(Mockito.refEq("android.permission.USE_CREDENTIALS"),
                    Mockito.anyString())).thenReturn(PackageManager.PERMISSION_DENIED);

            AuthenticationSettings.INSTANCE.setUseBroker(true);
            final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                    VALID_AUTHORITY, false);
            final TestAuthCallback callback = new TestAuthCallback();
            authContext.acquireToken(Mockito.mock(Activity.class), "resource", "clientid", authContext.getRedirectUriForBroker(),
                    "loginHint", callback);
            final CountDownLatch signal = new CountDownLatch(1);
            signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

            assertNotNull(callback.getCallbackException());
            assertTrue(callback.getCallbackException() instanceof UsageAuthenticationException);
            final UsageAuthenticationException usageAuthenticationException
                    = (UsageAuthenticationException) callback.getCallbackException();
            assertTrue(usageAuthenticationException.getMessage().contains("MANAGE_ACCOUNTS"));
            assertTrue(usageAuthenticationException.getMessage().contains("USE_CREDENTIALS"));
            assertEquals(ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING, usageAuthenticationException.getCode());
        }
    }

    public void testSilentRequestMissingPermissionHandlingForAndroid22andBelow() throws InterruptedException, PackageManager.NameNotFoundException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            final FileMockContext mockContext = createMockContext();
            when(mockContext.getPackageManager().checkPermission(Mockito.refEq("android.permission.GET_ACCOUNTS"),
                    Mockito.anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
            when(mockContext.getPackageManager().checkPermission(Mockito.refEq("android.permission.MANAGE_ACCOUNTS"),
                    Mockito.anyString())).thenReturn(PackageManager.PERMISSION_DENIED);
            when(mockContext.getPackageManager().checkPermission(Mockito.refEq("android.permission.USE_CREDENTIALS"),
                    Mockito.anyString())).thenReturn(PackageManager.PERMISSION_DENIED);

            AuthenticationSettings.INSTANCE.setUseBroker(true);
            final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                    VALID_AUTHORITY, false);
            final TestAuthCallback callback = new TestAuthCallback();
            authContext.acquireTokenSilentAsync("resource", "clientid", "userid", callback);
            final CountDownLatch signal = new CountDownLatch(1);
            signal.await(ACTIVITY_TIME_OUT, TimeUnit.MILLISECONDS);

            assertNotNull(callback.getCallbackException());
            assertTrue(callback.getCallbackException() instanceof UsageAuthenticationException);
            final UsageAuthenticationException usageAuthenticationException
                    = (UsageAuthenticationException) callback.getCallbackException();
            assertTrue(usageAuthenticationException.getMessage().contains("MANAGE_ACCOUNTS"));
            assertTrue(usageAuthenticationException.getMessage().contains("USE_CREDENTIALS"));
            assertEquals(ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING, usageAuthenticationException.getCode());
        }
    }

    public void testSilentRequestMissingContactsPermissionHandling() throws InterruptedException, PackageManager.NameNotFoundException {
        final FileMockContext mockContext = createMockContext();
        when(mockContext.getPackageManager().checkPermission(Mockito.refEq("android.permission.GET_ACCOUNTS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_DENIED);
        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final AuthenticationContext authContext = new AuthenticationContext(mockContext,
                VALID_AUTHORITY, false);
        try {
            authContext.acquireTokenSilentSync("resource", "clientid", "userid");
            fail("Expect an exception");
        } catch (final AuthenticationException exception) {
            assertTrue(exception.getMessage().contains("GET_ACCOUNTS"));
            assertEquals(ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING, exception.getCode());
        } catch (final Exception exception) {
            fail("Expect an AuthenticationException exception");
        }
    }

    private FileMockContext createMockContext()
            throws PackageManager.NameNotFoundException {

        final FileMockContext mockContext = new FileMockContext(getContext());

        mockContext.addPermission("android.permission.GET_ACCOUNTS");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mockContext.addPermission("android.permission.MANAGE_ACCOUNTS");
            mockContext.addPermission("android.permission.USE_CREDENTIALS");
        }

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

    private void mockAccountManagerGetAccountBehavior(final AccountManager mockedAccountManger)
            throws OperationCanceledException, IOException, AuthenticatorException {
        final Account account = new Account(TEST_UPN, AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        when(mockedAccountManger.getAccountsByType(Matchers.refEq(
                AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE))).thenReturn(new Account[] {account});

        final Bundle bundle = new Bundle();
        bundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, TEST_USERID);
        bundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE, TEST_UPN);

        final AccountManagerFuture<Bundle> mockedResult = Mockito.mock(AccountManagerFuture.class);
        when(mockedResult.getResult()).thenReturn(bundle);

        when(mockedAccountManger.updateCredentials(Matchers.any(Account.class), Matchers.anyString(),
                Matchers.any(Bundle.class), (Activity) Matchers.eq(null),
                (AccountManagerCallback<Bundle>) Matchers.eq(null), (Handler) Matchers.eq(null)))
                .thenReturn(mockedResult);
    }

    private void mockGetAuthTokenCallWithThrow(final AccountManager mockedAccountManager)
            throws OperationCanceledException, IOException, AuthenticatorException {
        final AccountManagerFuture<Bundle> mockedResult = Mockito.mock(AccountManagerFuture.class);
        when(mockedResult.getResult()).thenThrow(mock(AuthenticatorException.class));

        when(mockedAccountManager.getAuthToken(Mockito.any(Account.class), Matchers.anyString(),
                Matchers.any(Bundle.class), Matchers.eq(false), (AccountManagerCallback<Bundle>) Matchers.eq(null),
                Matchers.any(Handler.class))).thenReturn(mockedResult);
    }

    private void mockGetAuthTokenCall(final AccountManager mockedAccountManager, final boolean returnToken)
            throws OperationCanceledException, IOException, AuthenticatorException {

        final Bundle resultBundle = new Bundle();
        if (returnToken) {
            resultBundle.putString(AccountManager.KEY_AUTHTOKEN, "I am an access token from broker");
        } else {
            resultBundle.putInt(AccountManager.KEY_ERROR_CODE, ACCOUNT_MANAGER_ERROR_CODE_BAD_AUTHENTICATION);
            resultBundle.putString(AccountManager.KEY_ERROR_MESSAGE, "Error from broker");
        }

        final AccountManagerFuture<Bundle> mockedResult = Mockito.mock(AccountManagerFuture.class);
        when(mockedResult.getResult()).thenReturn(resultBundle);

        when(mockedAccountManager.getAuthToken(Mockito.any(Account.class), Matchers.anyString(),
                Matchers.any(Bundle.class), Matchers.eq(false), (AccountManagerCallback<Bundle>) Matchers.eq(null),
                Matchers.any(Handler.class))).thenReturn(mockedResult);
    }

    private void mockAddAccountCall(final AccountManager mockedAccountManager) throws OperationCanceledException,
            IOException, AuthenticatorException {

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, mock(Intent.class));
        final AccountManagerFuture<Bundle> mockedResult = Mockito.mock(AccountManagerFuture.class);
        when(mockedResult.getResult()).thenReturn(bundle);

        when(mockedAccountManager.addAccount(Matchers.refEq(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE),
                anyString(), (String[]) Matchers.eq(null), Matchers.any(Bundle.class), (Activity) Matchers.eq(null),
                (AccountManagerCallback<Bundle>) Matchers.eq(null),
                Matchers.any(Handler.class))).thenReturn(mockedResult);
    }

    private PackageManager getMockedPackageManager() throws PackageManager.NameNotFoundException {
        final Signature mockedSignature = Mockito.mock(Signature.class);
        when(mockedSignature.toByteArray()).thenReturn(Base64.decode(
                Util.ENCODED_SIGNATURE, Base64.NO_WRAP));

        final PackageInfo mockedPackageInfo = Mockito.mock(PackageInfo.class);
        mockedPackageInfo.signatures = new Signature[] {mockedSignature};

        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);
        when(mockedPackageManager.getPackageInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(mockedPackageInfo);

        // Mock intent query
        final List<ResolveInfo> activities = new ArrayList<>(1);
        activities.add(Mockito.mock(ResolveInfo.class));
        when(mockedPackageManager.queryIntentActivities(Mockito.any(Intent.class), Mockito.anyInt()))
                .thenReturn(activities);

        // Mock permissions
        when(mockedPackageManager.checkPermission(Mockito.refEq("android.permission.GET_ACCOUNTS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mockedPackageManager.checkPermission(Mockito.refEq("android.permission.MANAGE_ACCOUNTS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mockedPackageManager.checkPermission(Mockito.refEq("android.permission.USE_CREDENTIALS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);

        return mockedPackageManager;
    }

    private void prepareAuthForBrokerCall() {
        AuthenticationSettings.INSTANCE.setUseBroker(true);
    }

    private void prepareSuccessHttpUrlConnection() throws IOException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        Mockito.when(mockedConnection.getInputStream()).thenReturn(
                Util.createInputStream(Util.getSuccessTokenResponse(false, false)));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    }

    private void prepareFailedHttpUrlConnection(final String errorCode, final String ...errorCodes) throws IOException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));

        Mockito.when(mockedConnection.getInputStream()).thenReturn(
                Util.createInputStream(Util.getErrorResponseBody(errorCode)), errorCodes.length == 1
                        ? Util.createInputStream(Util.getErrorResponseBody(errorCodes[0])) : null);
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    private String getEncodedTestingSignature() throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(Base64.decode(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_SIGNATURE, Base64.NO_WRAP));
        final byte[] testingSignature = md.digest();
        return Base64.encodeToString(testingSignature, Base64.NO_WRAP);
    }
    
    private Date getExpireDate(int expireTime) {
        final Calendar expiredTime = new GregorianCalendar();
        expiredTime.add(Calendar.MINUTE, expireTime);
        return expiredTime.getTime();
    }
}
