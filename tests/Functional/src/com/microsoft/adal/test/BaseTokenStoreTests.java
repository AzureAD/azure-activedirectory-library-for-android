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

package com.microsoft.adal.test;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.crypto.NoSuchPaddingException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.test.mock.MockContext;
import android.test.mock.MockPackageManager;

import com.microsoft.adal.CacheKey;
import com.microsoft.adal.ITokenCacheStore;
import com.microsoft.adal.TokenCacheItem;
import com.microsoft.adal.UserInfo;

public abstract class BaseTokenStoreTests extends AndroidTestHelper {

    protected Context ctx;

    protected TokenCacheItem testItem;

    protected TokenCacheItem testItem2;

    protected TokenCacheItem testItemUser2;

    protected TokenCacheItem testItemMultiResourceUser2;

    protected final static String TEST_AUTHORITY2 = "https://Developer.AndroiD.com/reference/android";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ctx = this.getInstrumentation().getContext();
    }

    @Override
    protected void tearDown() throws Exception {

        ITokenCacheStore store = getTokenCacheStore();
        store.removeAll();
        super.tearDown();
    }

    protected ITokenCacheStore setupItems() throws NoSuchAlgorithmException, NoSuchPaddingException {
        ITokenCacheStore store = getTokenCacheStore();
        store.removeAll();
        // set items for user1
        UserInfo user = new UserInfo("userid1", "givenName", "familyName", "identity", true);
        testItem = new TokenCacheItem();
        testItem.setAccessToken("token");
        testItem.setAuthority("authority");
        testItem.setClientId("clientid");
        testItem.setResource("resource");
        testItem.setTenantId("tenantId");
        testItem.setUserInfo(user);

        testItem2 = new TokenCacheItem();
        testItem2.setAccessToken("token2");
        testItem2.setAuthority(TEST_AUTHORITY2);
        testItem2.setClientId("clientid2");
        testItem2.setResource("resource2");
        testItem2.setUserInfo(user);
        testItem2.setTenantId("tenantId2");
        store.setItem(CacheKey.createCacheKey(testItem), testItem);
        store.setItem(CacheKey.createCacheKey(testItem2), testItem2);

        UserInfo user2 = new UserInfo("userid2", "givenName", "familyName", "identity", true);
        testItemUser2 = new TokenCacheItem();
        // same authority, client, resource but different token for this user
        testItem2.setAccessToken("user2token2");
        testItem2.setAuthority(TEST_AUTHORITY2);
        testItem2.setClientId("clientid2");
        testItem2.setResource("resource2");
        testItem2.setUserInfo(user2);

        testItemMultiResourceUser2 = new TokenCacheItem();
        // same authority, client, resource but different token for this user
        testItemMultiResourceUser2.setAccessToken("user2token2Broad");
        testItemMultiResourceUser2.setIsMultiResourceRefreshToken(true);
        testItemMultiResourceUser2.setAuthority(TEST_AUTHORITY2);
        testItemMultiResourceUser2.setClientId("clientid2");
        testItemMultiResourceUser2.setResource("resource2");
        testItemMultiResourceUser2.setUserInfo(user2);
        store.setItem(CacheKey.createCacheKey(testItem2), testItem2);
        store.setItem(CacheKey.createCacheKey(testItemMultiResourceUser2),
                testItemMultiResourceUser2);

        return store;
    }

    protected abstract ITokenCacheStore getTokenCacheStore() throws NoSuchAlgorithmException, NoSuchPaddingException;

    public void testGetRemoveItem() throws NoSuchAlgorithmException, NoSuchPaddingException {
        // each test method will get new tokencachestore instance
        ITokenCacheStore store = setupItems();

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey("", "", "", false, ""));
        assertNull("Token cache item is expected to be null", item);

        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);
        assertEquals("same item", testItem.getTenantId(), item.getTenantId());
        assertEquals("same item", testItem.getAccessToken(), item.getAccessToken());
        
        item = store.getItem(CacheKey.createCacheKey("", "", "", true, ""));
        assertNull("Token cache item is expected to be null", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                true, ""));
        assertNull("Token cache item is expected to be null since userid is expected", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                true, "userid1"));
        assertNull(
                "Token cache item is NOT expected since there isn't any multiResourceItem for this user",
                item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                false, "userid1"));
        assertNotNull("Token cache item is expected", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                true, "userid2"));
        assertNotNull("Token cache item is expected", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                false, "userid2"));
        assertNotNull("Token cache item is expected", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2.toUpperCase(Locale.US),
                "resource2", "clientid2", false, "userid2"));
        assertNotNull("Expected to be case insensitive", item);

        item = store.getItem(CacheKey.createCacheKey("AuthoritY", "resource", "clientid", false,
                "userid1"));
        assertNotNull("Expected to be case insensitive", item);

        item = store.getItem(CacheKey.createCacheKey("AuthoritY", "resource", "clientid", false,
                null));
        assertNull("Expected to be null for null userid", item);

        item = store.getItem(CacheKey.createCacheKey("AuthoritY", "resource", "clientid", true,
                null));
        assertNull("Expected to be null for null userid", item);

        store.removeItem(CacheKey.createCacheKey(testItem));
        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);

        // second call should return false
        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);

        //
        store.removeItem(CacheKey.createCacheKey(testItem2));
        item = store.getItem(CacheKey.createCacheKey(testItem2));
        assertNull("Token cache item is expected to be null", item);
    }

    public void testContains() throws NoSuchAlgorithmException, NoSuchPaddingException {
        // each test method will get new tokencachestore instance
        ITokenCacheStore store = setupItems();

        // item is present
        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);

        boolean actual = store.contains(CacheKey.createCacheKey(testItem));
        assertTrue("Item is expected to be there", actual);
    }

    public void testRemoveAll() throws NoSuchAlgorithmException, NoSuchPaddingException {
        // each test method will get new tokencachestore instance
        ITokenCacheStore store = setupItems();

        store.removeAll();

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }
}
