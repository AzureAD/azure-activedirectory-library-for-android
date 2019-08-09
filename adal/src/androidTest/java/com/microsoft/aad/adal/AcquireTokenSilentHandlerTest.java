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

import android.content.Context;
import android.os.Build;
import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.aad.adal.AuthenticationRequest.UserIdentifierType;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;
import com.microsoft.identity.common.adal.internal.net.IWebRequestHandler;
import com.microsoft.identity.common.adal.internal.net.WebRequestHandler;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static androidx.test.InstrumentationRegistry.getContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for verifying acquire token silent flow.
 */
@RunWith(AndroidJUnit4.class)
public final class AcquireTokenSilentHandlerTest {
    /**
     * Check case-insensitive lookup
     */
    private static final String VALID_AUTHORITY = "https://Login.windows.net/Omercantest.Onmicrosoft.com";

    static final String TEST_IDTOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";

    static final String TEST_IDTOKEN_USERID = "4f859989-a2ff-411e-9048-c322247ac62c";

    static final String TEST_IDTOKEN_UPN = "admin@aaltests.onmicrosoft.com";

    @Before
    public void setUp() throws Exception {
        System.setProperty(
                "dexmaker.dexcache",
                androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation()
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
            final int iterations = 100;
            final int keySize = 256;
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes(StandardCharsets.UTF_8), iterations, keySize));
            SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }
    }

    @After
    public void tearDown() {
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
    }

    /**
     * Acquire token users refresh token, but the client app is inactive.
     */
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void testRefreshTokenFailedNoNetworkAppIsInactive() {
        FileMockContext mockContext = new FileMockContext(getContext());
        mockContext.setAppInactive();
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final String resource = "resource";
        final String clientId = "clientId";
        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        try {
            acquireTokenSilentHandler.acquireTokenWithRefreshToken("refreshToken");
            fail("Expect exception");
        } catch (final Exception exception) {
            assertTrue(exception instanceof AuthenticationException);
            assertTrue(((AuthenticationException) exception).getCode() == ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION);
        }
    }

    /**
     * Acquire token users refresh token, but the device is in doze mode.
     */
    @SmallTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void testRefreshTokenFailedNoNetworkDeviceIsIdle() {
        FileMockContext mockContext = new FileMockContext(getContext());
        mockContext.setDeviceInIdleMode();
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final String resource = "resource";
        final String clientId = "clientId";
        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        try {
            acquireTokenSilentHandler.acquireTokenWithRefreshToken("refreshToken");
            fail("Expect exception");
        } catch (final Exception exception) {
            assertTrue(exception instanceof AuthenticationException);
            assertTrue(((AuthenticationException) exception).getCode() == ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION);
        }
    }

    /**
     * Acquire token uses refresh token, but web request returns error with an empty body.
     */
    @Test
    public void testRefreshTokenWebRequestHasError() throws IOException {

        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final String resource = "resource";
        final String clientId = "clientId";
        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        Mockito.when(
                mockedWebRequestHandler.sendPost(
                        Mockito.any(URL.class),
                        Mockito.<String, String>anyMap(),
                        Mockito.any(byte[].class),
                        Mockito.anyString()
                )
        ).thenReturn(
                new HttpWebResponse(
                        HttpURLConnection.HTTP_INTERNAL_ERROR,
                        "{\"error\":\"interaction_required\" ,\"error_description\":\"Windows device is not in required device state\"}",
                        new HashMap<String, List<String>>()
                )
        );
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            acquireTokenSilentHandler.getAccessToken();
            fail("Expect exception");
        } catch (final AuthenticationException authenticationException) {
            assertTrue(authenticationException.getCode() == ADALError.AUTH_FAILED_NO_TOKEN);
            assertTrue(authenticationException.getCause() instanceof AuthenticationException);
            final AuthenticationException throwable = (AuthenticationException) authenticationException.getCause();
            assertTrue(throwable.getCode() == ADALError.SERVER_ERROR);
            assertNotNull(authenticationException.getHttpResponseBody());
            assertEquals(authenticationException.getServiceStatusCode(), HttpURLConnection.HTTP_INTERNAL_ERROR);
        }

        clearCache(mockCache);
    }

    // Verify if regular RT exists, if the RT is not MRRT, we only redeem token with the regular RT. 
    @Test
    public void testRegularRT() throws IOException, JSONException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        final String resource = "resource";
        final String clientId = "clientId";

        // Add regular RT in the cache, RT is not MRRT
        final TokenCacheItem regularTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final String regularRT = "Regular RT";
        regularTokenCacheItem.setRefreshToken(regularRT);
        saveTokenIntoCache(mockedCache, regularTokenCacheItem);

        // Add MRRT in the cache for different clientid
        final String mrrtClientId = "clientId2";
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource, mrrtClientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final String mrrt = "MRRT Refresh Token";
        mrrtTokenCacheItem.setRefreshToken(mrrt);
        mrrtTokenCacheItem.setResource(null);
        mrrtTokenCacheItem.setFamilyClientId("familyClientId");
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockedCache, mrrtTokenCacheItem);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        // Token redeem with RT fail with invalid_grant.
        final byte[] postMessage = Util.getPostMessage(regularRT, clientId, resource);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(postMessage), Mockito.anyString()))
                .thenReturn(new HttpWebResponse(HttpURLConnection.HTTP_BAD_REQUEST, Util.getErrorResponseBody("invalid_grant"), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult authenticationResult = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(authenticationResult);
            assertTrue(authenticationResult.getErrorCode().equalsIgnoreCase("invalid_grant"));
        } catch (AuthenticationException authException) {
            fail("Unexpected Exception");
        }

        ArgumentCaptor<byte[]> webRequestHandlerArgument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(mockedWebRequestHandler).sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(), webRequestHandlerArgument.capture(), Mockito.anyString());
        assertTrue(Arrays.equals(postMessage, webRequestHandlerArgument.getValue()));

        // verify regular token entry not existed
        assertNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID)));
        assertNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_UPN)));

        // verify MRRT entry exist
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, mrrtClientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, mrrtClientId, TEST_IDTOKEN_UPN)));

        clearCache(mockedCache);
    }

    // Test the current cache that does not mark RT as MRRT even it's MRRT.
    @Test
    public void testRegularRTExistsMRRTForSameClientIdExist() throws IOException, JSONException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        final String resource = "resource";
        final String clientId = "clientId";

        // Add regular RT in the cache, RT is not MRRT
        final TokenCacheItem regularTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final String regularRT = "Regular RT";
        regularTokenCacheItem.setRefreshToken(regularRT);
        saveTokenIntoCache(mockedCache, regularTokenCacheItem);

        // Add MRRT in the cache for same clientid
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final String mrrt = "MRRT Refresh Token";
        mrrtTokenCacheItem.setRefreshToken(mrrt);
        mrrtTokenCacheItem.setResource(null);
        mrrtTokenCacheItem.setFamilyClientId("familyClientId");
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockedCache, mrrtTokenCacheItem);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        // Token redeem with RT fail with invalid_grant.
        final byte[] postMessage = Util.getPostMessage(mrrt, clientId, resource);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(postMessage), Mockito.anyString()))
                .thenReturn(new HttpWebResponse(HttpURLConnection.HTTP_BAD_REQUEST,
                        Util.getErrorResponseBody("invalid_grant"), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult authenticationResult = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(authenticationResult);
            assertTrue(authenticationResult.getErrorCode().equalsIgnoreCase("invalid_grant"));
        } catch (AuthenticationException authException) {
            fail("Unexpected Exception");
        }

        ArgumentCaptor<byte[]> webRequestHandlerArgument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(mockedWebRequestHandler).sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(), webRequestHandlerArgument.capture(), Mockito.anyString());
        assertTrue(Arrays.equals(postMessage, webRequestHandlerArgument.getValue()));

        // verify regular token entry not existed
        assertNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID)));
        assertNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_UPN)));

        // verify MRRT entry exist
        assertNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_USERID)));
        assertNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_UPN)));

        clearCache(mockedCache);
    }

    /**
     * Test only when MRRT without FoCI in the cache.
     */
    @Test
    public void testMRRTSuccessNoFoCI() throws IOException, JSONException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        final String resource = "resource";
        final String clientId = "clientId";

        // Add MRRT in the cache for same clientid
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final String mrrt = "MRRT Refresh Token";
        mrrtTokenCacheItem.setRefreshToken(mrrt);
        mrrtTokenCacheItem.setResource(null);
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockedCache, mrrtTokenCacheItem);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        // Token redeem with RT fail with invalid_grant.
        final byte[] postMessage = Util.getPostMessage(mrrt, clientId, resource);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(postMessage), Mockito.anyString()))
                .thenReturn(new HttpWebResponse(HttpURLConnection.HTTP_OK,
                        Util.getSuccessTokenResponse(true, false), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult authenticationResult = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(authenticationResult);
            assertNull(authenticationResult.getErrorCode());
            assertNotNull(authenticationResult.getAccessToken());
            assertNotNull(authenticationResult.getRefreshToken());
        } catch (AuthenticationException authException) {
            fail("Unexpected Exception");
        }

        // MRRT token entry
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_UPN)));

        // RT entry
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_UPN)));

        clearCache(mockedCache);
    }

    /**
     * Make sure if we acquire token for a client id, and if we already have a family token item in cache, we use that
     * refresh token.
     */
    @Test
    public void testFRTSuccess() throws IOException, JSONException {

        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);

        // note: if only FRT exists, cache key will be hard-coded to 1
        final TokenCacheItem frTokenCacheItem = getTokenCacheItemWithFoCI(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN, AuthenticationConstants.MS_FAMILY_ID);
        saveTokenIntoCache(mockCache, frTokenCacheItem);

        addAzureADCloudForValidAuthority();

        final String resource = "resource";
        final String clientId = "clientId";
        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                Mockito.any(byte[].class), Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_OK,
                        Util.getSuccessTokenResponse(true, true), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult authResult = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(authResult);
            assertEquals("Returned assess token is not as expected.", "I am a new access token", authResult.getAccessToken());
            assertEquals("Returned refresh token is not as expected", "I am a new refresh token", authResult.getRefreshToken());
            assertEquals("Returned id token is not as expected.", TEST_IDTOKEN, authResult.getIdToken());
        } catch (AuthenticationException e) {
            fail("Unexpected exception");
        }

        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, "familyClientId", TEST_IDTOKEN_USERID)));
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, "familyClientId", TEST_IDTOKEN_UPN)));

        // MRRT token entry
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_UPN)));

        // RT entry
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_UPN)));

        clearCache(mockCache);
    }

    private void addAzureADCloudForValidAuthority() {
        List<String> aliases = new ArrayList<String>();
        aliases.add("login.windows.net");
        aliases.add("login.microsoftonline.com");
        AzureActiveDirectoryCloud cloud = new AzureActiveDirectoryCloud("login.microsoftonline.com", "login.windows.net", aliases);

        AzureActiveDirectory.putCloud("login.windows.net", cloud);
    }

    /**
     * Make sure if we have a family token in the cache and we fail to redeem access token with FRT, we correctly fail.
     * Also make sure only FRT token entry is deleted.
     */
    @Test
    public void testFRTFailedWithInvalidGrant() throws IOException, JSONException {

        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(mockContext);
        mockCache.removeAll();

        // note: if only FRT exists, cache key will be hard-coded to 1
        final TokenCacheItem frTokenCacheItem = getTokenCacheItemWithFoCI(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN, AuthenticationConstants.MS_FAMILY_ID);
        saveTokenIntoCache(mockCache, frTokenCacheItem);

        final String resource = "resource";
        final String clientId = "clientId";
        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                Mockito.any(byte[].class), Mockito.anyString()))
                .thenReturn(new HttpWebResponse(HttpURLConnection.HTTP_BAD_REQUEST,
                        Util.getErrorResponseBody("invalid_grant"), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult authenticationResult = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(authenticationResult);
            assertTrue(authenticationResult.getErrorCode().equalsIgnoreCase("invalid_grant"));
        } catch (AuthenticationException e) {
            fail("Unexpected exception");
        }

        // Verify Cache entry
        // Token request with FRT should delete token cache entry with FRT
        assertNull(mockCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, AuthenticationConstants.MS_FAMILY_ID, TEST_IDTOKEN_UPN)));
        assertNull(mockCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, AuthenticationConstants.MS_FAMILY_ID, TEST_IDTOKEN_UPN)));

        clearCache(mockCache);
    }

    /**
     * Test if FRT request failed, retry with MRRT if exists.
     */
    @Test
    public void testFRTRequestFailedFallBackMRRTRequest() throws IOException, JSONException {

        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(getContext());
        final String clientId = "clientId";
        final String familyClientId = "familyClientId";

        //MRRT token Cache Item with FoCI flag
        final String mrrtToken = "MRRT Refresh Token";
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, null, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        mrrtTokenCacheItem.setRefreshToken(mrrtToken);
        mrrtTokenCacheItem.setFamilyClientId(familyClientId);
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockCache, mrrtTokenCacheItem);

        // FRT token cache item
        final TokenCacheItem frtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, null, null, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final String frtToken = "FRT Refresh Token";
        frtTokenCacheItem.setRefreshToken(frtToken);
        frtTokenCacheItem.setFamilyClientId(familyClientId);
        frtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockCache, frtTokenCacheItem);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, "resource", clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        // FRT token request fails with invalid_grant
        final String anotherResource = "anotherResource";
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                Mockito.refEq(Util.getPostMessage(frtToken, clientId, anotherResource)),
                Mockito.anyString())).thenReturn(new HttpWebResponse(HttpURLConnection.HTTP_BAD_REQUEST, Util.getErrorResponseBody("invalid_grant"), null));

        // retry request with MRRT succeeds
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                Mockito.refEq(Util.getPostMessage(mrrtToken, clientId, anotherResource)),
                Mockito.anyString())).thenReturn(new HttpWebResponse(HttpURLConnection.HTTP_OK, Util.getSuccessTokenResponse(true, false), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            AuthenticationResult result = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(result);
            assertEquals("Returned assess token is not as expected.", "I am a new access token", result.getAccessToken());
            assertEquals("Returned refresh token is not as expected", "I am a new refresh token", result.getRefreshToken());
            assertEquals("Returned id token is not as expected.", TEST_IDTOKEN, result.getIdToken());
        } catch (AuthenticationException e) {
            fail("Unexpected exception");
        }

        // Verify post request with FRT token is executed first, followed by post request with MRRT.. 
        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                Mockito.refEq(Util.getPostMessage(frtToken, clientId, anotherResource)), Mockito.anyString());
        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                Mockito.refEq(Util.getPostMessage(mrrtToken, clientId, anotherResource)), Mockito.anyString());


        clearCache(mockCache);
    }

    /**
     * Verify if FRT request fails with invalid_grant, and retry request with MRRT failed with invalid request,
     * only FRT token cache entry is removed.
     */
    @Test
    public void testFRTRequestFailFallBackToMRTMRTRequestFail() throws IOException, JSONException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(getContext());
        mockCache.removeAll();
        final String clientId = "clientId";
        final String familyClientId = "familyClientId";
        final String resource = "resource";

        //MRRT token Cache Item with FoCI flag
        final String mrrtToken = "MRRT Refresh Token";
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, null, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        mrrtTokenCacheItem.setRefreshToken(mrrtToken);
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        mrrtTokenCacheItem.setFamilyClientId(familyClientId);
        saveTokenIntoCache(mockCache, mrrtTokenCacheItem);

        // FRT token cache item
        final TokenCacheItem frtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, null, null, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final String frtToken = "FRT Refresh Token";
        frtTokenCacheItem.setRefreshToken(frtToken);
        frtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        frtTokenCacheItem.setFamilyClientId(familyClientId);
        saveTokenIntoCache(mockCache, frtTokenCacheItem);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        //FRT request fails with invalid_grant
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(frtToken, clientId, resource)),
                Mockito.anyString())).thenReturn(new HttpWebResponse(
                HttpURLConnection.HTTP_BAD_REQUEST, Util.getErrorResponseBody("invalid_grant"),
                null));

        // MRT request also fails
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(mrrtToken, clientId, resource)),
                Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_BAD_REQUEST,
                        Util.getErrorResponseBody("invalid_request"), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult authResult = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(authResult);
            assertTrue(authResult.getErrorCode().equalsIgnoreCase("invalid_request"));
        } catch (final AuthenticationException e) {
            fail("Unexpected exception");
        }

        // Verify post request with MRRT token is executed first, followed by post request with FRT. 
        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(
                Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(
                        frtToken, clientId, resource)), Mockito.anyString());
        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(
                Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(mrrtToken, clientId, resource)),
                Mockito.anyString());

        // Verify cache entry
        // the First FRT request should delete the FRT entries
        assertNull(mockCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, familyClientId, TEST_IDTOKEN_USERID)));
        assertNull(mockCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, familyClientId, TEST_IDTOKEN_UPN)));

        // MRT request gets back invalid_request, cache entry should still exist
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_UPN)));

        clearCache(mockCache);
    }

    /**
     * Test if MRRT is not marked as FRT, if the MRRT request fails, we will try with FRT.
     */
    @Test
    public void testMRRTRequestFailsTryFRT() throws JSONException, IOException {
        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockCache = new DefaultTokenCacheStore(getContext());
        mockCache.removeAll();
        final String clientId = "clientId";
        final String resource = "resource";

        //MRRT token Cache Item without FoCI flag
        final String mrrtToken = "MRRT Refresh Token";
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, null, clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        mrrtTokenCacheItem.setRefreshToken(mrrtToken);
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockCache, mrrtTokenCacheItem);

        // FRT token cache item
        final TokenCacheItem frtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, null, null, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        final String frtToken = "FRT Refresh Token";
        frtTokenCacheItem.setRefreshToken(frtToken);
        frtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        frtTokenCacheItem.setFamilyClientId(AuthenticationConstants.MS_FAMILY_ID);
        saveTokenIntoCache(mockCache, frtTokenCacheItem);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        // MRRT request fails with invalid_grant
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(mrrtToken, clientId, resource)),
                Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_BAD_REQUEST,
                        Util.getErrorResponseBody("invalid_grant"), null));

        // FRT request succeed
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(frtToken, clientId, resource)),
                Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_OK,
                        Util.getSuccessTokenResponse(true, true), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult authResult = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(authResult);
            assertNull(authResult.getErrorCode());
            assertNotNull(authResult.getAccessToken());
            assertNotNull(authResult.getRefreshToken());
        } catch (final AuthenticationException e) {
            fail("Unexpected exception");
        }

        // Verify post request with MRRT token is executed first, followed by post request with FRT. 
        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(
                Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(
                        Util.getPostMessage(mrrtToken, clientId, resource)), Mockito.anyString());
        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(
                Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(frtToken, clientId, resource)),
                Mockito.anyString());

        // Verify cache entry, FRT return token back, should store entries with user in cache.
        // FRT token entry
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, "familyClientId", TEST_IDTOKEN_USERID)));
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, "familyClientId", TEST_IDTOKEN_UPN)));

        // MRRT token entry
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_UPN)));

        // RT entry
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId, TEST_IDTOKEN_UPN)));

        clearCache(mockCache);
    }

    /**
     * Test RT request returns errors, but error response doesn't contain error_code.
     */
    @Test
    public void testRefreshTokenRequestNotReturnErrorCode() throws IOException, JSONException {
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, "resource", "clientid", false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                Mockito.any(byte[].class), Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_BAD_REQUEST,
                        Util.getErrorResponseBody(null), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            acquireTokenSilentHandler.getAccessToken();
            fail();
        } catch (final AuthenticationException e) {
            assertEquals("Token is not exchanged",
                    ADALError.AUTH_FAILED_NO_TOKEN, e.getCode());
            // If AUTH_FAILE_NO_TOKEN is thrown, cause will be return in AuthenticationException
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof AuthenticationException);
            final AuthenticationException authException = (AuthenticationException) e.getCause();
            assertTrue(authException.getCode() == ADALError.SERVER_ERROR);
        }

        // verify that the cache is not cleared
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId", TEST_IDTOKEN_USERID)));
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId", TEST_IDTOKEN_UPN)));

        clearCache(mockCache);
    }

    /**
     * Test RT request failed with interaction_required, cache will not be cleared.
     */
    @Test
    public void testRefreshTokenWithInteractionRequiredCacheNotCleared() throws IOException, JSONException {
        FileMockContext mockContext = new FileMockContext(getContext());
        ITokenCacheStore mockCache = getCacheForRefreshToken(TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(VALID_AUTHORITY, "resource", "clientid", false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                Mockito.any(byte[].class), Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_BAD_REQUEST,
                        Util.getErrorResponseBody("interaction_required"), null));
        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult authenticationResult = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(authenticationResult);
            assertTrue(authenticationResult.getErrorCode().equalsIgnoreCase("interaction_required"));
        } catch (final AuthenticationException e) {
            fail("unexpected exception");
        }

        // verify that the cache is cleared
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId", TEST_IDTOKEN_USERID)));
        assertNotNull(mockCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, "resource", "clientId", TEST_IDTOKEN_UPN)));

        clearCache(mockCache);
    }

    @Test
    public void testMRRTItemNotContainRT() {
        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        final String resource = "resource";
        final String clientId = "clientId";

        // Add MRRT in the cache
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource,
                clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        mrrtTokenCacheItem.setRefreshToken(null);
        mrrtTokenCacheItem.setResource(null);
        mrrtTokenCacheItem.setFamilyClientId("familyClientId");
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockedCache, mrrtTokenCacheItem);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(
                VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        try {
            final AuthenticationResult authenticationResult = acquireTokenSilentHandler.getAccessToken();
            assertNull(authenticationResult);
        } catch (AuthenticationException authException) {
            fail("Unexpected Exception");
        }

        // verify MRRT entry exist
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_UPN)));

        clearCache(mockedCache);
    }

    @Test
    public void testAllTokenItemNotContainRT() {
        FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        final String resource = "resource";
        final String clientId = "clientId";

        // Add regular RT item without RT in the cache
        final TokenCacheItem rtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        rtTokenCacheItem.setRefreshToken(null);
        rtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockedCache, rtTokenCacheItem);

        // Add MRRT in the cache
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource,
                clientId, TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        mrrtTokenCacheItem.setRefreshToken(null);
        mrrtTokenCacheItem.setResource(null);
        mrrtTokenCacheItem.setFamilyClientId("familyId");
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockedCache, mrrtTokenCacheItem);

        // Add FRT item into cache without rt
        final TokenCacheItem frtTokenCacheItem = Util.getTokenCacheItem(VALID_AUTHORITY, resource, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        frtTokenCacheItem.setClientId(null);
        frtTokenCacheItem.setRefreshToken(null);
        frtTokenCacheItem.setResource(null);
        frtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        frtTokenCacheItem.setFamilyClientId("familyId");
        saveTokenIntoCache(mockedCache, frtTokenCacheItem);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(
                VALID_AUTHORITY, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        try {
            final AuthenticationResult authenticationResult = acquireTokenSilentHandler.getAccessToken();
            assertNull(authenticationResult);
        } catch (AuthenticationException authException) {
            fail("Unexpected Exception");
        }

        // verify RT entry exist
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId,
                TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(VALID_AUTHORITY, resource, clientId,
                TEST_IDTOKEN_UPN)));

        // verify MRRT entry exist
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(VALID_AUTHORITY, clientId, TEST_IDTOKEN_UPN)));

        // verify FRT entry exist
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, "familyId", TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForFRT(VALID_AUTHORITY, "familyId", TEST_IDTOKEN_UPN)));
        clearCache(mockedCache);
    }

    @Test
    public void testRTExistedInPreferredCache() throws IOException, JSONException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        clearCache(mockedCache);

        updateAuthorityMetadataCache();
        // insert token with authority as preferred cache
        final String resource = "resource";
        final String clientId = "clientId";

        // Add regular RT item without RT in the cache
        final String preferredCacheAuthority = "https://preferred.cache/test.onmicrosoft.com";
        final String rtForPreferredCache = "rt with preferred cache";
        final TokenCacheItem rtTokenCacheItem = Util.getTokenCacheItem(preferredCacheAuthority, resource, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        rtTokenCacheItem.setRefreshToken(rtForPreferredCache);
        rtTokenCacheItem.setIsMultiResourceRefreshToken(false);
        saveTokenIntoCache(mockedCache, rtTokenCacheItem);

        // insert token with authority as aliased host
        final String testHostAuthority = "https://test.host/test.onmicrosoft.com";
        final TokenCacheItem itemWithTestHost = Util.getTokenCacheItem(testHostAuthority, resource, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        itemWithTestHost.setRefreshToken("rt with test host");
        saveTokenIntoCache(mockedCache, itemWithTestHost);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(testHostAuthority, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(rtForPreferredCache, clientId, resource)),
                Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_OK,
                        Util.getSuccessTokenResponse(false, false), null));

        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult result = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
        } catch (final AuthenticationException e) {
            fail();
        }

        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(
                Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(
                        Util.getPostMessage(rtForPreferredCache, clientId, resource)), Mockito.anyString());

        // verify token items
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(preferredCacheAuthority, resource, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(preferredCacheAuthority, resource, clientId, TEST_IDTOKEN_UPN)));
        clearCache(mockedCache);
    }

    @Test
    public void testMRRTExistInPreferredLocation() throws IOException, JSONException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        clearCache(mockedCache);

        updateAuthorityMetadataCache();
        // insert token with authority as preferred cache
        final String resource = "resource";
        final String clientId = "clientId";

        final String preferredCacheAuthority = "https://preferred.cache/test.onmicrosoft.com";
        final String mrrtForPreferredCache = "rt with preferred cache";
        final TokenCacheItem mrrtTokenCacheItem = Util.getTokenCacheItem(preferredCacheAuthority, null, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        mrrtTokenCacheItem.setRefreshToken(mrrtForPreferredCache);
        mrrtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        saveTokenIntoCache(mockedCache, mrrtTokenCacheItem);

        final String testHostAuthority = "https://test.host/test.onmicrosoft.com";
        final TokenCacheItem mrrtWithTestHost = Util.getTokenCacheItem(testHostAuthority, null, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        mrrtWithTestHost.setRefreshToken("rt with test host");
        saveTokenIntoCache(mockedCache, mrrtWithTestHost);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(testHostAuthority, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(mrrtForPreferredCache, clientId, resource)),
                Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_OK,
                        Util.getSuccessTokenResponse(false, false), null));

        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult result = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
        } catch (final AuthenticationException e) {
            fail();
        }

        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(
                Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(
                        Util.getPostMessage(mrrtForPreferredCache, clientId, resource)), Mockito.anyString());

        // verify token items
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(preferredCacheAuthority, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForMRRT(preferredCacheAuthority, clientId, TEST_IDTOKEN_UPN)));
        clearCache(mockedCache);
    }

    @Test
    public void testFRTExistedInPreferredLocation() throws IOException, JSONException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        clearCache(mockedCache);

        updateAuthorityMetadataCache();
        // insert token with authority as preferred cache
        final String resource = "resource";
        final String clientId = "clientId";
        final String familyClientId = "1";

        final String preferredCacheAuthority = "https://preferred.cache/test.onmicrosoft.com";
        final String frtForPreferredCache = "frt with preferred cache";
        final TokenCacheItem frtTokenCacheItem = Util.getTokenCacheItem(preferredCacheAuthority, null, null,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        frtTokenCacheItem.setRefreshToken(frtForPreferredCache);
        frtTokenCacheItem.setIsMultiResourceRefreshToken(true);
        frtTokenCacheItem.setFamilyClientId(familyClientId);
        saveTokenIntoCache(mockedCache, frtTokenCacheItem);

        final String testHostAuthority = "https://test.host/test.onmicrosoft.com";
        final TokenCacheItem frtWithTestHost = Util.getTokenCacheItem(testHostAuthority, null, null,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        frtWithTestHost.setRefreshToken("frt with test host");
        frtWithTestHost.setFamilyClientId(familyClientId);
        saveTokenIntoCache(mockedCache, frtWithTestHost);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(testHostAuthority, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(frtForPreferredCache, clientId, resource)),
                Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_OK,
                        Util.getSuccessTokenResponse(true, true), null));

        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult result = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
        } catch (final AuthenticationException e) {
            fail();
        }

        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(
                Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(
                        Util.getPostMessage(frtForPreferredCache, clientId, resource)), Mockito.anyString());

        // verify token items
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForFRT(preferredCacheAuthority, familyClientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForFRT(preferredCacheAuthority, familyClientId, TEST_IDTOKEN_UPN)));
        clearCache(mockedCache);
    }

    /**
     * If a token is not present for preferred_cache, but available for the developer specified authority, as well as other alias,
     * the developer specified authority token is used.
     */
    @Test
    public void testTokenPresentForPassedInAuthorityAndOtherAliasedHost() throws IOException, JSONException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        clearCache(mockedCache);

        updateAuthorityMetadataCache();
        // insert token with authority as other aliased host
        final String resource = "resource";
        final String clientId = "clientId";

        // Add regular RT item without RT in the cache
        final String aliasedAuthority = "https://test.alias/test.onmicrosoft.com";
        final String rtForAliashedHost = "rt with aliased authority";
        final TokenCacheItem rtTokenCacheItem = Util.getTokenCacheItem(aliasedAuthority, resource, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        rtTokenCacheItem.setRefreshToken(rtForAliashedHost);
        rtTokenCacheItem.setIsMultiResourceRefreshToken(false);
        saveTokenIntoCache(mockedCache, rtTokenCacheItem);

        // insert token with authority as aliased host
        final String testHostAuthority = "https://test.host/test.onmicrosoft.com";
        final String rtForTestHost = "rt for test host";
        final TokenCacheItem itemWithTestHost = Util.getTokenCacheItem(testHostAuthority, resource, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        itemWithTestHost.setRefreshToken(rtForTestHost);
        saveTokenIntoCache(mockedCache, itemWithTestHost);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest(testHostAuthority, resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        // MRRT request fails with invalid_grant
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(Util.getPostMessage(rtForTestHost, clientId, resource)),
                Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_OK,
                        Util.getSuccessTokenResponse(false, false), null));

        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult result = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
        } catch (final AuthenticationException e) {
            fail();
        }

        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(
                Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                AdditionalMatchers.aryEq(
                        Util.getPostMessage(rtForTestHost, clientId, resource)), Mockito.anyString());

        // verify token items
        final String preferredCacheLocation = "https://preferred.cache/test.onmicrosoft.com";
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(preferredCacheLocation, resource, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(preferredCacheLocation, resource, clientId, TEST_IDTOKEN_UPN)));
        clearCache(mockedCache);
    }

    @Test
    public void testTokenForAliasedAuthorityPresent() throws IOException, JSONException {
        final FileMockContext mockContext = new FileMockContext(getContext());
        final ITokenCacheStore mockedCache = new DefaultTokenCacheStore(getContext());
        clearCache(mockedCache);

        updateAuthorityMetadataCache();
        // insert token with authority as other aliased host
        final String resource = "resource";
        final String clientId = "clientId";

        // Add regular RT item without RT in the cache
        final String aliasedAuthority = "https://test.alias/test.onmicrosoft.com";
        final String rtForAliashedHost = "rt with aliased authority";
        final TokenCacheItem rtTokenCacheItem = Util.getTokenCacheItem(aliasedAuthority, resource, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        rtTokenCacheItem.setRefreshToken(rtForAliashedHost);
        rtTokenCacheItem.setIsMultiResourceRefreshToken(false);
        saveTokenIntoCache(mockedCache, rtTokenCacheItem);

        // insert token with authority as aliased host
        final String preferredNetworkAuthority = "https://preferred.network/test.onmicrosoft.com";
        final String rtForPreferredNetwork = "rt for preferred network";
        final TokenCacheItem itemWithPreferredNetwork = Util.getTokenCacheItem(preferredNetworkAuthority, resource, clientId,
                TEST_IDTOKEN_USERID, TEST_IDTOKEN_UPN);
        itemWithPreferredNetwork.setRefreshToken(rtForPreferredNetwork);
        saveTokenIntoCache(mockedCache, itemWithPreferredNetwork);

        final AuthenticationRequest authenticationRequest = getAuthenticationRequest("https://test.host/test.onmicrosoft.com", resource, clientId, false);
        authenticationRequest.setUserIdentifierType(UserIdentifierType.UniqueId);
        authenticationRequest.setUserId(TEST_IDTOKEN_USERID);
        final AcquireTokenSilentHandler acquireTokenSilentHandler = getAcquireTokenHandler(mockContext,
                authenticationRequest, mockedCache);

        // inject mocked web request handler
        final IWebRequestHandler mockedWebRequestHandler = Mockito.mock(WebRequestHandler.class);
        // MRRT request fails with invalid_grant
        Mockito.when(mockedWebRequestHandler.sendPost(Mockito.any(URL.class), Mockito.<String, String>anyMap(),
                (byte[]) Mockito.any(), Mockito.anyString())).thenReturn(
                new HttpWebResponse(HttpURLConnection.HTTP_OK,
                        Util.getSuccessTokenResponse(false, false), null));

        acquireTokenSilentHandler.setWebRequestHandler(mockedWebRequestHandler);

        try {
            final AuthenticationResult result = acquireTokenSilentHandler.getAccessToken();
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
        } catch (final AuthenticationException e) {
            fail();
        }

        Mockito.verify(mockedWebRequestHandler, Mockito.times(1)).sendPost(
                Mockito.any(URL.class), Mockito.<String, String>anyMap(), (byte[]) Mockito.any(), Mockito.anyString());

        // verify token items
        final String preferredCacheLocation = "https://preferred.cache/test.onmicrosoft.com";
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(preferredCacheLocation, resource, clientId, TEST_IDTOKEN_USERID)));
        assertNotNull(mockedCache.getItem(CacheKey.createCacheKeyForRTEntry(preferredCacheLocation, resource, clientId, TEST_IDTOKEN_UPN)));
        clearCache(mockedCache);
    }

    private void updateAuthorityMetadataCache() {
        final InstanceDiscoveryMetadata metadata = getInstanceDiscoveryMetadata();
        final AzureActiveDirectoryCloud cloud = CoreAdapter.asAadCloud(metadata);
        for (final String alias : metadata.getAliases()) {
            AuthorityValidationMetadataCache.updateInstanceDiscoveryMap(alias, metadata);
            AzureActiveDirectory.putCloud(alias, cloud);
        }
    }

    private InstanceDiscoveryMetadata getInstanceDiscoveryMetadata() {
        final String preferredNetwork = "preferred.network";
        final String preferredCacheLocation = "preferred.cache";

        final List<String> aliases = new ArrayList<>();
        aliases.add("preferred.network");
        aliases.add("preferred.cache");
        aliases.add("test.host");
        aliases.add("test.alias");

        return new InstanceDiscoveryMetadata(preferredNetwork, preferredCacheLocation, aliases);
    }

    private void saveTokenIntoCache(final ITokenCacheStore mockedCache, final TokenCacheItem token) {
        if (!StringExtensions.isNullOrBlank(token.getResource())) {
            mockedCache.setItem(CacheKey.createCacheKeyForRTEntry(token.getAuthority(), token.getResource(), token.getClientId(),
                    token.getUserInfo().getUserId()), token);
            mockedCache.setItem(CacheKey.createCacheKeyForRTEntry(token.getAuthority(), token.getResource(), token.getClientId(),
                    token.getUserInfo().getDisplayableId()), token);
        } else if (StringExtensions.isNullOrBlank(token.getClientId())) {
            mockedCache.setItem(CacheKey.createCacheKeyForFRT(token.getAuthority(), token.getFamilyClientId(),
                    token.getUserInfo().getUserId()), token);
            mockedCache.setItem(CacheKey.createCacheKeyForFRT(token.getAuthority(), token.getFamilyClientId(),
                    token.getUserInfo().getDisplayableId()), token);
        } else {
            mockedCache.setItem(CacheKey.createCacheKeyForMRRT(token.getAuthority(), token.getClientId(),
                    token.getUserInfo().getUserId()), token);
            mockedCache.setItem(CacheKey.createCacheKeyForMRRT(token.getAuthority(), token.getClientId(),
                    token.getUserInfo().getDisplayableId()), token);
        }
    }

    // No Family client id set in the cache. Only regular RT token cache entry
    private ITokenCacheStore getCacheForRefreshToken(String userId, String displayableId) {
        DefaultTokenCacheStore cache = new DefaultTokenCacheStore(getContext());
        cache.removeAll();
        Calendar expiredTime = new GregorianCalendar();
        Logger.d("Test", "Time now:" + expiredTime.toString());
        final int expireTimeAdjust = -60;
        expiredTime.add(Calendar.MINUTE, expireTimeAdjust);
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

    private TokenCacheItem getTokenCacheItemWithFoCI(final String userId, final String displayableId, final String familyClientId) {
        final TokenCacheItem tokenCacheItemWithFoCI = TokenCacheItem.createFRRTTokenCacheItem(VALID_AUTHORITY,
                Util.getAuthenticationResult(true, displayableId, userId, familyClientId));

        return tokenCacheItemWithFoCI;
    }


    private void clearCache(final ITokenCacheStore cacheStore) {
        cacheStore.removeAll();
    }

    private AuthenticationRequest getAuthenticationRequest(final String authority, final String resource,
                                                           final String clientId, final boolean isExtendedLifetimeEnabled) {
        AuthenticationRequest request = new AuthenticationRequest(authority, resource, clientId, UUID.randomUUID(),
                isExtendedLifetimeEnabled);
        request.setAppVersion("test");
        request.setAppName("test.mock.");

        request.setTelemetryRequestId(UUID.randomUUID().toString());
        return request;
    }

    private AcquireTokenSilentHandler getAcquireTokenHandler(final Context context, final AuthenticationRequest authRequest,
                                                             final ITokenCacheStore mockCache) {
        return new AcquireTokenSilentHandler(context, authRequest,
                new TokenCacheAccessor(context.getApplicationContext(), mockCache, authRequest.getAuthority(), authRequest.getTelemetryRequestId()));
    }
}
