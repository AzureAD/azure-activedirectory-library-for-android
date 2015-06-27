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

package com.microsoft.aad.adal.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.DigestException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.CacheKey;
import com.microsoft.aad.adal.TokenCache;
import com.microsoft.aad.adal.ITokenCacheStore;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.StorageHelper;
import com.microsoft.aad.adal.TokenCacheItem;

public class DefaultTokenCacheStoreTests extends BaseTokenStoreTests {

    private static final String TAG = "DefaultTokenCacheStoreTests";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ctx = this.getInstrumentation().getContext();
    }

    @Override
    protected void tearDown() throws Exception {
        AuthenticationSettings.INSTANCE.setSharedPrefPackageName(null);
        TokenCache store = new TokenCache(ctx);
        store.removeAll();
        super.tearDown();
    }

    public void testSharedCache() throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidKeySpecException, KeyStoreException, CertificateException,
            NoSuchProviderException, InvalidAlgorithmParameterException,
            UnrecoverableEntryException, DigestException, IllegalBlockSizeException,
            BadPaddingException, IOException, NameNotFoundException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        AuthenticationSettings.INSTANCE.setSharedPrefPackageName("mockpackage");
        StorageHelper mockSecure = mock(StorageHelper.class);
        Context mockContext = mock(Context.class);
        Context packageContext = mock(Context.class);
        SharedPreferences prefs = mock(SharedPreferences.class);
        when(prefs.contains("testkey")).thenReturn(true);
        when(prefs.getString("testkey", "")).thenReturn("test_encrypted");
        when(mockSecure.decrypt("test_encrypted")).thenReturn("{\"mClientId\":\"clientId23\"}");
        when(mockContext.createPackageContext("mockpackage", Context.MODE_PRIVATE)).thenReturn(
                packageContext);
        when(
                packageContext.getSharedPreferences("com.microsoft.aad.adal.cache",
                        Activity.MODE_PRIVATE)).thenReturn(prefs);
        Class<?> c = TokenCache.class;
        Field encryptHelper = c.getDeclaredField("sHelper");
        encryptHelper.setAccessible(true);
        encryptHelper.set(null, mockSecure);
        TokenCache cache = new TokenCache(mockContext);
        TokenCacheItem item = cache.getItem("testkey");

        // Verify returned item
        assertEquals("Same item as mock", "clientId23", item.getClientId());
        encryptHelper.set(null, null);
    }

    public void testGetAll() throws NoSuchAlgorithmException, NoSuchPaddingException {
        TokenCache store = (TokenCache)setupItems();

        Iterator<TokenCacheItem> results = store.getAll();
        assertNotNull("Iterator is supposed to be not null", results);
        TokenCacheItem item = results.next();
        assertNotNull("Has item", item);
    }

    public void testGetUniqueUsers() throws NoSuchAlgorithmException, NoSuchPaddingException {
        TokenCache store = (TokenCache)setupItems();
        HashSet<String> users = store.getUniqueUsersWithTokenCache();
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    public void testDateTimeFormatterOldFormat() throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException,
            KeyStoreException, CertificateException, NoSuchProviderException,
            InvalidAlgorithmParameterException, UnrecoverableEntryException, DigestException,
            IllegalBlockSizeException, BadPaddingException, IOException, NameNotFoundException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        StorageHelper mockSecure = mock(StorageHelper.class);
        Context mockContext = mock(Context.class);
        SharedPreferences prefs = mock(SharedPreferences.class);
        when(prefs.contains("testkey")).thenReturn(true);
        when(prefs.getString("testkey", "")).thenReturn("test_encrypted");
        when(mockSecure.decrypt("test_encrypted")).thenReturn(
                "{\"mClientId\":\"clientId23\",\"mExpiresOn\":\"Apr 28, 2015 1:09:57 PM\"}");
        when(
                mockContext.getSharedPreferences("com.microsoft.aad.adal.cache",
                        Activity.MODE_PRIVATE)).thenReturn(prefs);
        Class<?> c = TokenCache.class;
        Field encryptHelper = c.getDeclaredField("sHelper");
        encryptHelper.setAccessible(true);
        encryptHelper.set(null, mockSecure);
        TokenCache cache = new TokenCache(mockContext);
        TokenCacheItem item = cache.getItem("testkey");

        // Verify returned item
        assertNotNull(item.getExpiresOn());
        assertNotNull(item.getExpiresOn().after(new Date()));
        encryptHelper.set(null, null);
    }

    public void testDateTimeFormatterLocaleChange() throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException,
            KeyStoreException, CertificateException, NoSuchProviderException,
            InvalidAlgorithmParameterException, UnrecoverableEntryException, DigestException,
            IllegalBlockSizeException, BadPaddingException, IOException, NameNotFoundException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        TokenCache store = (TokenCache)setupItems();
        ArrayList<TokenCacheItem> tokens = store.getTokensForResource("resource");
        // Serializing without miliseconds
        long precision = 1000;
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

    public void testGetTokensForResource() throws NoSuchAlgorithmException, NoSuchPaddingException {
        TokenCache store = (TokenCache)setupItems();

        ArrayList<TokenCacheItem> tokens = store.getTokensForResource("resource");
        assertEquals("token size", 1, tokens.size());
        assertEquals("token content", "token", tokens.get(0).getAccessToken());

        tokens = store.getTokensForResource("resource2");
        assertEquals("token size", 3, tokens.size());
    }

    public void testGetTokensForUser() throws NoSuchAlgorithmException, NoSuchPaddingException {
        TokenCache store = (TokenCache)setupItems();

        ArrayList<TokenCacheItem> tokens = store.getTokensForUser("userid1");
        assertEquals("token size", 2, tokens.size());

        tokens = store.getTokensForUser("userid2");
        assertEquals("token size", 2, tokens.size());
    }

    public void testExpiringTokens() throws NoSuchAlgorithmException, NoSuchPaddingException {
        TokenCache store = (TokenCache)setupItems();

        ArrayList<TokenCacheItem> tokens = store.getTokensForUser("userid1");
        ArrayList<TokenCacheItem> expireTokenList = store.getTokensAboutToExpire();
        assertEquals("token size", 0, expireTokenList.size());
        assertEquals("token size", 2, tokens.size());

        TokenCacheItem expire = tokens.get(0);

        Calendar timeAhead = Calendar.getInstance();
        timeAhead.add(Calendar.MINUTE, -10);
        expire.setExpiresOn(timeAhead.getTime());

        store.setItem(CacheKey.createCacheKey(expire), expire);

        expireTokenList = store.getTokensAboutToExpire();
        assertEquals("token size", 1, expireTokenList.size());
    }

    public void testClearTokensForUser() throws NoSuchAlgorithmException, NoSuchPaddingException {
        TokenCache store = (TokenCache)setupItems();

        store.clearTokensForUser("userid");

        ArrayList<TokenCacheItem> tokens = store.getTokensForUser("userid");
        assertEquals("token size", 0, tokens.size());

        store.clearTokensForUser("userid2");

        tokens = store.getTokensForUser("userid2");
        assertEquals("token size", 0, tokens.size());
    }

    public void testExpireBuffer() throws NoSuchAlgorithmException, NoSuchPaddingException {
        TokenCache store = (TokenCache)setupItems();

        ArrayList<TokenCacheItem> tokens = store.getTokensForUser("userid1");
        Calendar expireTime = Calendar.getInstance();
        Logger.d(TAG, "Time now: " + expireTime.getTime());
        expireTime.add(Calendar.SECOND, 240);
        Logger.d(TAG, "Time modified: " + expireTime.getTime());

        // Sets token to expire if less than this buffer
        AuthenticationSettings.INSTANCE.setExpirationBuffer(300);
        for (TokenCacheItem item : tokens) {
            item.setExpiresOn(expireTime.getTime());
            assertTrue("Should say expired", TokenCacheItem.isTokenExpired(item.getExpiresOn()));
        }

        // Set expire time ahead of buffer 240 +100 secs more than 300secs
        // buffer
        expireTime.add(Calendar.SECOND, 100);
        for (TokenCacheItem item : tokens) {
            item.setExpiresOn(expireTime.getTime());
            assertFalse("Should not say expired since time is more than buffer",
                    TokenCacheItem.isTokenExpired(item.getExpiresOn()));
        }
    }

    @Override
    protected ITokenCacheStore getTokenCacheStore() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        return new TokenCache(this.getInstrumentation().getTargetContext());
    }
}
