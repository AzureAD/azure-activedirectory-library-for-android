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
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.crypto.NoSuchPaddingException;

import org.mockito.Mockito;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class DefaultTokenCacheStoreTests extends BaseTokenStoreTests {

    private static final String TAG = "DefaultTokenCacheStoreTests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        AuthenticationSettings.INSTANCE.setSharedPrefPackageName(null);
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(getContext());
        store.removeAll();
        super.tearDown();
    }

    public void testCacheItemRetrieval() throws GeneralSecurityException, IOException {
        TokenCacheItem item = mockDefaultCacheStore("Apr 28, 2015 1:09:57 PM").getItem("testkey");

        // Verify returned item
        assertEquals("Same item as mock", "clientId23", item.getClientId());
    }

    public void testGetAll() throws AuthenticationException {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore) setupItems();

        Iterator<TokenCacheItem> results = store.getAll();
        assertNotNull("Iterator is supposed to be not null", results);
        TokenCacheItem item = results.next();
        assertNotNull("Has item", item);
    }

    public void testGetUniqueUsers() throws AuthenticationException {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore) setupItems();
        Set<String> users = store.getUniqueUsersWithTokenCache();
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    public void testDateTimeFormatterOldFormatWithAMOrPM() throws GeneralSecurityException, IOException {
        TokenCacheItem item = mockDefaultCacheStore("Apr 28, 2015 1:09:57 PM").getItem("testkey");

        // Verify returned item
        assertNotNull(item.getExpiresOn());
        assertNotNull(item.getExpiresOn().after(new Date()));
    }

    public void testDateTimeFormatterOldFormat24hourFormat() throws GeneralSecurityException, IOException {
        TokenCacheItem item = mockDefaultCacheStore("Apr 28, 2015 13:09:57").getItem("testkey");

        // Verify returned item
        assertNotNull(item.getExpiresOn());
        assertNotNull(item.getExpiresOn().after(new Date()));
    }

    private DefaultTokenCacheStore mockDefaultCacheStore(final String dateTimeString) throws GeneralSecurityException, IOException {
        final StorageHelper mockSecure = Mockito.mock(StorageHelper.class);
        Context mockContext = mock(Context.class);
        SharedPreferences prefs = mock(SharedPreferences.class);
        when(prefs.contains("testkey")).thenReturn(true);
        when(prefs.getString("testkey", "")).thenReturn("test_encrypted");
        when(mockSecure.decrypt("test_encrypted"))
                .thenReturn("{\"mClientId\":\"clientId23\",\"mExpiresOn\":\"" + dateTimeString + "\"}");
        when(
                mockContext.getSharedPreferences("com.microsoft.aad.adal.cache",
                        Activity.MODE_PRIVATE)).thenReturn(prefs);
        DefaultTokenCacheStore cache = new DefaultTokenCacheStore(mockContext) {
            @Override
            protected StorageHelper getStorageHelper() {
                return mockSecure;
            }
        };
        return cache;
    }


    public void testDateTimeFormatterLocaleChange() throws AuthenticationException {
        final DefaultTokenCacheStore store = (DefaultTokenCacheStore) setupItems();
        List<TokenCacheItem> tokens = store.getTokensForResource("resource");
        // Serializing without miliseconds
        final long precision = 1000;
        TokenCacheItem item = tokens.get(0);
        String cacheKey = CacheKey.createCacheKey(item);
        Calendar time = Calendar.getInstance();
        Date dateTimeNow = time.getTime();
        long timeNowMiliSeconds = dateTimeNow.getTime();
        item.setExpiresOn(dateTimeNow);
        store.setItem(cacheKey, item);
        TokenCacheItem fromCache = store.getItem(cacheKey);
        assertTrue(Math.abs(timeNowMiliSeconds - fromCache.getExpiresOn().getTime()) < precision);

        // Parse for different settings
        Locale.setDefault(Locale.FRANCE);
        fromCache = store.getItem(cacheKey);
        assertTrue(Math.abs(timeNowMiliSeconds - fromCache.getExpiresOn().getTime()) < precision);

        Locale.setDefault(Locale.US);
        fromCache = store.getItem(cacheKey);
        assertTrue(Math.abs(timeNowMiliSeconds - fromCache.getExpiresOn().getTime()) < precision);

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+03:00"));
        fromCache = store.getItem(cacheKey);
        assertTrue(Math.abs(timeNowMiliSeconds - fromCache.getExpiresOn().getTime()) < precision);

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:00"));
        fromCache = store.getItem(cacheKey);
        assertTrue(Math.abs(timeNowMiliSeconds - fromCache.getExpiresOn().getTime()) < precision);
    }

    public void testGetTokensForResource() throws AuthenticationException {
        final DefaultTokenCacheStore store = (DefaultTokenCacheStore) setupItems();

        List<TokenCacheItem> tokens = store.getTokensForResource("resource");
        assertEquals("token size", 1, tokens.size());
        assertEquals("token content", "token", tokens.get(0).getAccessToken());

        tokens = store.getTokensForResource("resource2");
        assertEquals("token size", 2, tokens.size());
    }

    public void testGetTokensForUser() throws AuthenticationException {
        final DefaultTokenCacheStore store = (DefaultTokenCacheStore) setupItems();

        List<TokenCacheItem> tokens = store.getTokensForUser("userid1");
        assertEquals("token size", 2, tokens.size());

        tokens = store.getTokensForUser("userid2");
        assertEquals("token size", 2, tokens.size());
    }

    public void testExpiringTokens() throws NoSuchAlgorithmException, NoSuchPaddingException, AuthenticationException {
        final DefaultTokenCacheStore store = (DefaultTokenCacheStore) setupItems();

        List<TokenCacheItem> tokens = store.getTokensForUser("userid1");
        List<TokenCacheItem> expireTokenList = store.getTokensAboutToExpire();
        assertEquals("token size", 0, expireTokenList.size());
        assertEquals("token size", 2, tokens.size());

        TokenCacheItem expire = tokens.get(0);

        final int timeAdjustmentMins = -10;
        Calendar timeAhead = Calendar.getInstance();
        timeAhead.add(Calendar.MINUTE, timeAdjustmentMins);
        expire.setExpiresOn(timeAhead.getTime());

        store.setItem(CacheKey.createCacheKey(expire), expire);

        expireTokenList = store.getTokensAboutToExpire();
        assertEquals("token size", 1, expireTokenList.size());
    }

    public void testClearTokensForUser() throws AuthenticationException {
        final DefaultTokenCacheStore store = (DefaultTokenCacheStore) setupItems();

        store.clearTokensForUser("userid");

        List<TokenCacheItem> tokens = store.getTokensForUser("userid");
        assertEquals("token size", 0, tokens.size());

        store.clearTokensForUser("userid2");

        tokens = store.getTokensForUser("userid2");
        assertEquals("token size", 0, tokens.size());
    }

    public void testExpireBuffer() throws AuthenticationException {
        final DefaultTokenCacheStore store = (DefaultTokenCacheStore) setupItems();

        List<TokenCacheItem> tokens = store.getTokensForUser("userid1");
        Calendar expireTime = Calendar.getInstance();
        Logger.d(TAG, "Time now: " + expireTime.getTime());
        final int timeAdjustmentSecs = 240;
        expireTime.add(Calendar.SECOND, timeAdjustmentSecs);
        Logger.d(TAG, "Time modified: " + expireTime.getTime());

        // Sets token to expire if less than this buffer
        final int expirationBuffer = 300;
        AuthenticationSettings.INSTANCE.setExpirationBuffer(expirationBuffer);
        for (TokenCacheItem item : tokens) {
            item.setExpiresOn(expireTime.getTime());
            assertTrue("Should say expired", TokenCacheItem.isTokenExpired(item.getExpiresOn()));
        }

        // Set expire time ahead of buffer 240 +100 secs more than 300secs
        // buffer
        expireTime.add(Calendar.SECOND, timeAdjustmentSecs);
        for (TokenCacheItem item : tokens) {
            item.setExpiresOn(expireTime.getTime());
            assertFalse("Should not say expired since time is more than buffer",
                    TokenCacheItem.isTokenExpired(item.getExpiresOn()));
        }
    }

    @Override
    protected ITokenCacheStore getTokenCacheStore() {
        return new DefaultTokenCacheStore(this.getInstrumentation().getTargetContext());
    }
}
