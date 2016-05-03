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

package com.microsoft.aad.adal.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationActivity;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.CacheKey;
import com.microsoft.aad.adal.DefaultTokenCacheStore;
import com.microsoft.aad.adal.ITokenCacheStore;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.StorageHelper;
import com.microsoft.aad.adal.TokenCacheItem;
import com.microsoft.aad.adal.UserInfo;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.Signature;
import junit.framework.Assert;

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
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);
        store.removeAll();
        super.tearDown();
    }

    public void testCacheItemRetrieval() throws GeneralSecurityException, IOException {
        TokenCacheItem item = mockDefaultCacheStore("Apr 28, 2015 1:09:57 PM").getItem("testkey");

        // Verify returned item
        assertEquals("Same item as mock", "clientId23", item.getClientId());
    }

    public void testGetAll() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        Iterator<TokenCacheItem> results = store.getAll();
        assertNotNull("Iterator is supposed to be not null", results);
        TokenCacheItem item = results.next();
        assertNotNull("Has item", item);
    }

    public void testGetUniqueUsers() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
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
        when(mockSecure.loadSecretKeyForAPI()).thenReturn(null);
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

    
    public void testDateTimeFormatterLocaleChange() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        List<TokenCacheItem> tokens = store.getTokensForResource("resource");
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

    public void testGetTokensForResource() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        List<TokenCacheItem> tokens = store.getTokensForResource("resource");
        assertEquals("token size", 1, tokens.size());
        assertEquals("token content", "token", tokens.get(0).getAccessToken());

        tokens = store.getTokensForResource("resource2");
        assertEquals("token size", 3, tokens.size());
    }

    public void testGetTokensForUser() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        List<TokenCacheItem> tokens = store.getTokensForUser("userid1");
        assertEquals("token size", 2, tokens.size());

        tokens = store.getTokensForUser("userid2");
        assertEquals("token size", 2, tokens.size());
    }

    public void testExpiringTokens() throws NoSuchAlgorithmException, NoSuchPaddingException {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        List<TokenCacheItem> tokens = store.getTokensForUser("userid1");
        List<TokenCacheItem> expireTokenList = store.getTokensAboutToExpire();
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

    public void testClearTokensForUser() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        store.clearTokensForUser("userid");

        List<TokenCacheItem> tokens = store.getTokensForUser("userid");
        assertEquals("token size", 0, tokens.size());

        store.clearTokensForUser("userid2");

        tokens = store.getTokensForUser("userid2");
        assertEquals("token size", 0, tokens.size());
    }

    public void testExpireBuffer() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        List<TokenCacheItem> tokens = store.getTokensForUser("userid1");
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
    protected ITokenCacheStore getTokenCacheStore() {
        return new DefaultTokenCacheStore(this.getInstrumentation().getTargetContext());
    }
    
    private void addFRTCacheItem(DefaultTokenCacheStore defaultTokenCacheStore) {
        final String TEST_AUTHORITY2 = "https://Developer.AndroiD.com/reference/android";
        UserInfo user2 = new UserInfo("userid2", "givenName", "familyName", "identity", "userid2");
        TokenCacheItem testFamilyRefreshTokenItemUser2 = new TokenCacheItem();
        testFamilyRefreshTokenItemUser2.setAccessToken("user2token2Family");
        testFamilyRefreshTokenItemUser2.setIsMultiResourceRefreshToken(true);
        testFamilyRefreshTokenItemUser2.setAuthority(TEST_AUTHORITY2);
        testFamilyRefreshTokenItemUser2.setUserInfo(user2);
        testFamilyRefreshTokenItemUser2.setFamilyClientId("1");
        testFamilyRefreshTokenItemUser2.setRefreshToken("user2FRT");
        testFamilyRefreshTokenItemUser2.setClientId("");
        testFamilyRefreshTokenItemUser2.setResource("");
        defaultTokenCacheStore.setItem(CacheKey.createCacheKey(testFamilyRefreshTokenItemUser2),
                testFamilyRefreshTokenItemUser2);
    }
    
    public void testGetFamilyRefreshTokenItemForUser_withFRTCacheItem() throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException{
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        String userId = "userid2";
        addFRTCacheItem(store);         
        Method m = ReflectionUtils.getTestMethod(store, "getFamilyRefreshTokenItemForUser", String.class);
        TokenCacheItem frtTokenCacheItem =(TokenCacheItem)m.invoke(store, userId);
        assertTrue(frtTokenCacheItem != null);
    }
    
    public void testGetFamilyRefreshTokenItemForUser_noFRTCacheItem() throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException{
        String userId = "userid2";
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        Method m = ReflectionUtils.getTestMethod(store, "getFamilyRefreshTokenItemForUser", String.class);
        TokenCacheItem frtTokenCacheItem =(TokenCacheItem)m.invoke(store, userId);
        assertTrue(frtTokenCacheItem == null);
    }    
    
    public void testSerialize_invalidUserId() throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException{
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        Method m = ReflectionUtils.getTestMethod(store, "serialize", String.class);
        
        try {
            String jsonString = (String)m.invoke(store, "");
            Assert.fail("not expected");
        } catch (InvocationTargetException exp){
             assertTrue(exp.getCause() instanceof IllegalArgumentException);
        }
        
        String testStr = null;
        try {
            String jsonString = (String)m.invoke(store, testStr);
            Assert.fail("not expected");
        } catch (InvocationTargetException exp){
            assertTrue(exp.getCause() instanceof IllegalArgumentException);
        }        
    }
    
    public void testSerialize_nullCacheItem() throws AuthenticationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, InstantiationException{
        String userId = "userid2";
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        Method m = ReflectionUtils.getTestMethod(store, "serialize", String.class);        
        assertTrue((String)m.invoke(store, userId) == null);
    }
     
    public void testSerialize_valid() throws AuthenticationException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        String userId = "userid2";
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        addFRTCacheItem(store);
        Method m = ReflectionUtils.getTestMethod(store, "serialize", String.class);
        String jsonStr = (String)m.invoke(store, userId);
        assertTrue( jsonStr != null);
    }

    public void testDeserialize_valid() throws AuthenticationException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        String userId = "userid2";
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        addFRTCacheItem(store);
        Method m = ReflectionUtils.getTestMethod(store, "serialize", String.class);        
        String serializedBlob = (String)m.invoke(store, userId); 
        
        Method deserializeReflect = ReflectionUtils.getTestMethod(store, "deserialize", String.class);
        deserializeReflect.invoke(store, serializedBlob);
        
        Method getfrtReflect = ReflectionUtils.getTestMethod(store, "getFamilyRefreshTokenItemForUser", String.class);
        TokenCacheItem item = (TokenCacheItem)getfrtReflect.invoke(store, userId);
        assertTrue(getfrtReflect.invoke(store, userId)!=null);
    }    
    
    public void testDeserialize_invalidSerialVersionUID() throws AuthenticationException{
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        addFRTCacheItem(store);
        Date date = new Date(1000);
        Gson gson = new Gson();
        String mockFalseSerializedBlob = gson.toJson(date); 
        
        try {
            Method deserializeReflect = ReflectionUtils.getTestMethod(store, "deserialize", String.class);
            deserializeReflect.invoke(store, mockFalseSerializedBlob);
            Assert.fail("Not expected.");
        } catch (Exception exp) {
            assertTrue(((AuthenticationException)exp.getCause()).getCode().equals(ADALError.FAIL_TO_DESERIALIZE));
        }
    }
    
    public void testDeserialize_nullSerializedBlob() {
        String nullSerializedBlob = null;
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        
        try {
            Method deserializeReflect = ReflectionUtils.getTestMethod(store, "deserialize", String.class);
            deserializeReflect.invoke(store, nullSerializedBlob);
        } catch (Exception exp) {
            assertTrue("argument exception", exp.getCause() instanceof IllegalArgumentException);            
        }
    }   
}
