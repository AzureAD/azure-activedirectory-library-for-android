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

import java.util.Locale;
import android.content.Context;

public abstract class BaseTokenStoreTests extends AndroidTestHelper {

    protected static final String TEST_AUTHORITY2 = "https://Developer.AndroiD.com/reference/android";

    private Context mCtx;

    private TokenCacheItem mTestItem;

    private TokenCacheItem mTestItem2;

    private TokenCacheItem mTestItemUser2;

    private TokenCacheItem mTestItemMultiResourceUser2;

    Context getContext() {
        return mCtx;
    }

    TokenCacheItem getTestItem() {
        return mTestItem;
    }

    TokenCacheItem getTestItem2() {
        return mTestItem2;
    }

    TokenCacheItem getTestItemUser2() {
        return mTestItemUser2;
    }

    TokenCacheItem getTestItemMultiResourceUser2() {
        return mTestItemMultiResourceUser2;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCtx = this.getInstrumentation().getContext();
        AuthenticationSettings.INSTANCE.setSharedPrefPackageName(null);
    }

    @Override
    protected void tearDown() throws Exception {
        AuthenticationSettings.INSTANCE.setSharedPrefPackageName(null);
        ITokenCacheStore store = getTokenCacheStore();
        store.removeAll();
        super.tearDown();
    }

    protected ITokenCacheStore setupItems() throws AuthenticationException {
        ITokenCacheStore store = getTokenCacheStore();
        store.removeAll();
        // set items for user1
        UserInfo user = new UserInfo("userid1", "givenName", "familyName", "identity", "userid1");
        mTestItem = new TokenCacheItem();
        mTestItem.setAccessToken("token");
        mTestItem.setAuthority("authority");
        mTestItem.setClientId("clientid");
        mTestItem.setResource("resource");
        mTestItem.setTenantId("tenantId");
        mTestItem.setUserInfo(user);

        mTestItem2 = new TokenCacheItem();
        mTestItem2.setAccessToken("token2");
        mTestItem2.setAuthority(TEST_AUTHORITY2);
        mTestItem2.setClientId("clientid2");
        mTestItem2.setResource("resource2");
        mTestItem2.setUserInfo(user);
        mTestItem2.setTenantId("tenantId2");
        store.setItem(CacheKey.createCacheKey(mTestItem), mTestItem);
        store.setItem(CacheKey.createCacheKey(mTestItem2), mTestItem2);

        UserInfo user2 = new UserInfo("userid2", "givenName", "familyName", "identity", "userid2");
        mTestItemUser2 = new TokenCacheItem();
        // same authority, client, resource but different token for this user
        mTestItem2.setAccessToken("user2token2");
        mTestItem2.setAuthority(TEST_AUTHORITY2);
        mTestItem2.setClientId("clientid2");
        mTestItem2.setResource("resource2");
        mTestItem2.setUserInfo(user2);

        mTestItemMultiResourceUser2 = new TokenCacheItem();
        // same authority, client, resource but different token for this user
        mTestItemMultiResourceUser2.setAccessToken("user2token2Broad");
        mTestItemMultiResourceUser2.setIsMultiResourceRefreshToken(true);
        mTestItemMultiResourceUser2.setAuthority(TEST_AUTHORITY2);
        mTestItemMultiResourceUser2.setClientId("clientid2");
        mTestItemMultiResourceUser2.setUserInfo(user2);
        store.setItem(CacheKey.createCacheKey(mTestItem2), mTestItem2);
        store.setItem(CacheKey.createCacheKey(mTestItemMultiResourceUser2),
                mTestItemMultiResourceUser2);

        return store;
    }

    protected abstract ITokenCacheStore getTokenCacheStore();

    public void testGetRemoveItem() throws AuthenticationException {
        // each test method will get new tokencachestore instance
        ITokenCacheStore store = setupItems();

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey("", "", "", false, "", ""));
        assertNull("Token cache item is expected to be null", item);

        item = store.getItem(CacheKey.createCacheKey(mTestItem));
        assertNotNull("Token cache item is expected to be NOT null", item);
        assertEquals("same item", mTestItem.getTenantId(), item.getTenantId());
        assertEquals("same item", mTestItem.getAccessToken(), item.getAccessToken());
        
        item = store.getItem(CacheKey.createCacheKey("", "", "", true, "", null));
        assertNull("Token cache item is expected to be null", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                true, "", null));
        assertNull("Token cache item is expected to be null since userid is expected", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                true, "userid1", null));
        assertNull(
                "Token cache item is NOT expected since there isn't any multiResourceItem for this user",
                item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                false, "userid1", null));
        assertNotNull("Token cache item is expected", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                true, "userid2", null));
        assertNotNull("Token cache item is expected", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2, "resource2", "clientid2",
                false, "userid2", null));
        assertNotNull("Token cache item is expected", item);

        item = store.getItem(CacheKey.createCacheKey(TEST_AUTHORITY2.toUpperCase(Locale.US),
                "resource2", "clientid2", false, "userid2", null));
        assertNotNull("Expected to be case insensitive", item);

        item = store.getItem(CacheKey.createCacheKey("AuthoritY", "resource", "clientid", false,
                "userid1", null));
        assertNotNull("Expected to be case insensitive", item);

        item = store.getItem(CacheKey.createCacheKey("AuthoritY", "resource", "clientid", false,
                null, null));
        assertNull("Expected to be null for null userid", item);

        item = store.getItem(CacheKey.createCacheKey("AuthoritY", "resource", "clientid", true,
                null, null));
        assertNull("Expected to be null for null userid", item);

        store.removeItem(CacheKey.createCacheKey(mTestItem));
        item = store.getItem(CacheKey.createCacheKey(mTestItem));
        assertNull("Token cache item is expected to be null", item);

        // second call should return false
        item = store.getItem(CacheKey.createCacheKey(mTestItem));
        assertNull("Token cache item is expected to be null", item);

        //
        store.removeItem(CacheKey.createCacheKey(mTestItem2));
        item = store.getItem(CacheKey.createCacheKey(mTestItem2));
        assertNull("Token cache item is expected to be null", item);
    }

    public void testContains() throws AuthenticationException {
        // each test method will get new tokencachestore instance
        ITokenCacheStore store = setupItems();

        // item is present
        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(mTestItem));
        assertNotNull("Token cache item is expected to be NOT null", item);

        boolean actual = store.contains(CacheKey.createCacheKey(mTestItem));
        assertTrue("Item is expected to be there", actual);
    }

    public void testRemoveAll() throws AuthenticationException {
        // each test method will get new tokencachestore instance
        ITokenCacheStore store = setupItems();

        store.removeAll();

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(mTestItem));
        assertNull("Token cache item is expected to be null", item);
    }
}
