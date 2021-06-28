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

import android.content.Context;
import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static androidx.test.InstrumentationRegistry.getContext;
import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class TokenCacheAccessorTests {

    private static final String WORLDWIDE_AUTHORITY = "https://login.microsoftonline.com/common";
    private static final String MOONCAKE_AUTHORITY = "https://login.partner.microsoftonline.cn/common";
    private static final String MOCK_AUTHORITY = "https://login.partner.microsoftonline.cn/0287f963-2d72-4363-9e3a-5705c5b0f031/";
    private static final String MOCK_OID = "1c1db626-0fcb-42bb-b39e-8e983dd92932";
    private static final String MOCK_PREFERRED_USERNAME = "jdoe";
    private static final String MOCK_GIVEN_NAME = "John";
    private static final String MOCK_FAMILY_NAME = "Doe";
    private static final String MOCK_NAME = "John Doe";
    private static final String MOCK_MIDDLE_NAME = "Q";
    private static final String MOCK_ID_TOKEN_WITH_CLAIMS;
    private static final String MOCK_UID = "mock_uid";
    private static final String MOCK_UTID = "mock_utid";
    private static final String MOCK_CLIENT_INFO = createRawClientInfo(MOCK_UID, MOCK_UTID);

    static String createRawClientInfo(final String uid, final String utid) {
        final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

        return new String(Base64.encode(claims.getBytes(
                Charset.forName("UTF-8")), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
    }

    static {
        String idTokenWithClaims;
        final SecureRandom random = new SecureRandom();
        final byte[] sharedSecret = new byte[32];
        random.nextBytes(sharedSecret);

        try {
            // Create HMAC signer
            final JWSSigner signer = new MACSigner(sharedSecret);

            // Create/populate claims for the JWT
            final JWTClaimsSet claimsSet =
                    new JWTClaimsSet.Builder()
                            .issuer(MOCK_AUTHORITY)
                            .claim("iat", 1521498950)
                            .claim("exp", 1553035656)
                            .audience("www.contoso.com")
                            .subject("fake.email@contoso.com")
                            .claim("oid", MOCK_OID)
                            .claim("preferred_username", MOCK_PREFERRED_USERNAME)
                            .claim("given_name", MOCK_GIVEN_NAME)
                            .claim("family_name", MOCK_FAMILY_NAME)
                            .claim("name", MOCK_NAME)
                            .claim("middle_name", MOCK_MIDDLE_NAME)
                            .claim("upn", "jdoe@contoso.com")
                            .build();

            // Create the JWT
            final SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

            // Sign it
            signedJWT.sign(signer);

            // Stringify it for testing
            idTokenWithClaims = signedJWT.serialize();

        } catch (JOSEException e) {
            e.printStackTrace();
            idTokenWithClaims = null;
        }

        MOCK_ID_TOKEN_WITH_CLAIMS = idTokenWithClaims;
    }

    Context mContext;
    TokenCacheAccessor mTokenCacheAccessor;

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
            final SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
            final int iterations = 100;
            final int keySize = 256;
            final SecretKey tempkey =
                    keyFactory.generateSecret(
                            new PBEKeySpec(
                                    "test".toCharArray(),
                                    "abcdedfdfd".getBytes(StandardCharsets.UTF_8),
                                    iterations,
                                    keySize
                            )
                    );
            final SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }

        // initialize the class under test
        mContext = new FileMockContext(getContext());
        final ITokenCacheStore tokenCacheStore = new DelegatingCache(mContext, new DefaultTokenCacheStore(mContext));
        mTokenCacheAccessor = new TokenCacheAccessor(
                mContext,
                tokenCacheStore,
                WORLDWIDE_AUTHORITY,
                UUID.randomUUID().toString()
        );
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testUpdateTokenCacheUsesResultAuthority() throws MalformedURLException, ServiceException {
        // First assert the cache initialization is using the default authority
        assertEquals(WORLDWIDE_AUTHORITY, mTokenCacheAccessor.getAuthorityUrlWithPreferredCache());

        // Create a Request and Result that use differing authorities
        final AuthenticationRequest request =
                new AuthenticationRequest(
                        WORLDWIDE_AUTHORITY,
                        "a_resource",
                        "12345",
                        "https://localhost:2000",
                        "",
                        PromptBehavior.Auto,
                        "",
                        UUID.randomUUID(),
                        false,
                        null
                );
        final AuthenticationResult result = new AuthenticationResult(
                "mock_at",
                "mock_rt",
                new Date(System.currentTimeMillis() + (3600 * 1000)),
                false,
                new UserInfo(
                        "userid1",
                        "givenName",
                        "familyName",
                        "identity",
                        "userid1"
                ),
                "tid",
                MOCK_ID_TOKEN_WITH_CLAIMS,
                null,
                "12345"
        );

        result.setAuthority(MOONCAKE_AUTHORITY);
        result.setClientInfo(new ClientInfo(MOCK_CLIENT_INFO));
        result.setResponseReceived(System.currentTimeMillis());
        result.setExpiresIn(System.currentTimeMillis());

        // Populate a mock Instance Discovery
        AzureActiveDirectory.putCloud(
                new URL(WORLDWIDE_AUTHORITY).getHost(),
                new AzureActiveDirectoryCloud(
                        "login.microsoftonline.com",
                        "login.windows.net",
                        Arrays.asList(
                                "login.microsoftonline.com",
                                "login.windows.net",
                                "login.microsoft.com",
                                "sts.windows.net"
                        )
                )
        );

        AzureActiveDirectory.putCloud(
                new URL(MOONCAKE_AUTHORITY).getHost(),
                new AzureActiveDirectoryCloud(
                        "login.partner.microsoftonline.cn",
                        "login.partner.microsoftonline.cn",
                        Arrays.asList(
                                "login.partner.microsoftonline.cn",
                                "login.chinacloudapi.cn"
                        )
                )
        );

        // Save this to the cache
        mTokenCacheAccessor.updateTokenCache(request, result);

        assertEquals(MOONCAKE_AUTHORITY, mTokenCacheAccessor.getAuthorityUrlWithPreferredCache());
    }

    @Test
    public void testMsalCacheIsUpdated() throws ServiceException, MalformedURLException {
        // First assert the cache initialization is using the default authority
        assertEquals(WORLDWIDE_AUTHORITY, mTokenCacheAccessor.getAuthorityUrlWithPreferredCache());

        // Create a Request and Result that use differing authorities
        final AuthenticationRequest request =
                new AuthenticationRequest(
                        WORLDWIDE_AUTHORITY,
                        "a_resource",
                        "12345",
                        "https://localhost:2000",
                        "",
                        PromptBehavior.Auto,
                        "",
                        UUID.randomUUID(),
                        false,
                        null
                );
        final AuthenticationResult result = new AuthenticationResult(
                "mock_at",
                "mock_rt",
                new Date(System.currentTimeMillis() + (3600 * 1000)),
                false,
                new UserInfo(
                        "userid1",
                        "givenName",
                        "familyName",
                        "identity",
                        "userid1"
                ),
                "tid",
                MOCK_ID_TOKEN_WITH_CLAIMS,
                null,
                "12345"
        );

        result.setAuthority(WORLDWIDE_AUTHORITY);
        result.setClientInfo(new ClientInfo(MOCK_CLIENT_INFO));
        result.setResponseReceived(System.currentTimeMillis());
        result.setExpiresIn(System.currentTimeMillis());

        // Populate a mock Instance Discovery
        AzureActiveDirectory.putCloud(
                new URL(WORLDWIDE_AUTHORITY).getHost(),
                new AzureActiveDirectoryCloud(
                        "login.microsoftonline.com",
                        "login.windows.net",
                        Arrays.asList(
                                "login.microsoftonline.com",
                                "login.windows.net",
                                "login.microsoft.com",
                                "sts.windows.net"
                        )
                )
        );

        // Save this to the cache
        mTokenCacheAccessor.updateTokenCache(request, result);

        assertEquals(WORLDWIDE_AUTHORITY, mTokenCacheAccessor.getAuthorityUrlWithPreferredCache());

        // Assert the MSAL replicated cache now contains the account & RT
        final IAccountCredentialCache accountCredentialCache = new SharedPreferencesAccountCredentialCache(
                new CacheKeyValueDelegate(),
                new SharedPreferencesFileManager(
                        mContext,
                        DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                        new StorageHelper(mContext)
                )
        );

        final MsalOAuth2TokenCache msalCache =  new MsalOAuth2TokenCache(
                mContext,
                accountCredentialCache,
                new MicrosoftStsAccountCredentialAdapter()
        );

        // Assert the presence of the account
        final AccountRecord accountRecord = msalCache.getAccount(
                "login.windows.net",
                "12345",
                "mock_uid.mock_utid",
                "mock_utid"
        );

        Assert.assertNotNull(accountRecord);

        // The RT
        final ICacheRecord cacheRecord = msalCache.load(
                "12345",
                null,
                accountRecord,
                new BearerAuthenticationSchemeInternal()
        );

        final IdTokenRecord idToken = cacheRecord.getIdToken();
        final RefreshTokenRecord refreshToken = cacheRecord.getRefreshToken();

        Assert.assertEquals("mock_utid", idToken.getRealm());
        Assert.assertEquals("12345", idToken.getClientId());
        Assert.assertEquals(accountRecord.getHomeAccountId(), idToken.getHomeAccountId());

        Assert.assertEquals("login.windows.net", refreshToken.getEnvironment());
        Assert.assertEquals("12345", refreshToken.getClientId());
        Assert.assertEquals(accountRecord.getHomeAccountId(), refreshToken.getHomeAccountId());
    }
}
