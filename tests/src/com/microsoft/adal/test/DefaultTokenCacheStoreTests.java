
package com.microsoft.adal.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import android.content.Context;

import com.microsoft.adal.CacheKey;
import com.microsoft.adal.DefaultTokenCacheStore;
import com.microsoft.adal.TokenCacheItem;
import com.microsoft.adal.UserInfo;

public class DefaultTokenCacheStoreTests extends AndroidTestHelper {

    Context ctx;

    private TokenCacheItem testItem;

    private TokenCacheItem testItem2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ctx = this.getInstrumentation().getContext();
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);

        // set item and then get
        testItem = new TokenCacheItem();
        testItem.setAccessToken("token");
        testItem.setAuthority("authority");
        testItem.setClientId("clientid");
        testItem.setResource("resource");
        UserInfo user = new UserInfo("userid", "givenName", "familyName", "identity", true);
        testItem.setUserInfo(user);
        testItem2 = new TokenCacheItem();
        testItem2.setAccessToken("token2");
        testItem2.setAuthority("authority2");
        testItem2.setClientId("clientid2");
        testItem2.setResource("resource2");
        testItem2.setUserInfo(user);
        store.setItem(testItem);
        store.setItem(testItem2);
    }

    @Override
    protected void tearDown() throws Exception {

        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);
        assertTrue("Clear all", store.removeAll());
        super.tearDown();
    }

    public void testGetItem() {

        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);
        TokenCacheItem item = store.getItem(new CacheKey());
        assertNull("Token cache item is expected to be null", item);

        // get item
        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);
        assertEquals("Same tokencacheitem content", testItem.getAuthority(), item.getAuthority());
        assertEquals("Same tokencacheitem content", testItem.getClientId(), item.getClientId());
        assertEquals("Same tokencacheitem content", testItem.getResource(), item.getResource());
    }

    public void testRemoveItem() {
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);

        boolean actual = store.removeItem(CacheKey.createCacheKey(testItem));
        assertEquals("Expected to remove item", true, actual);

        // second call should return false
        actual = store.removeItem(CacheKey.createCacheKey(testItem));
        assertFalse("second call should return false", actual);
    }

    public void testRemoveAll() {
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);

        boolean actual = store.removeAll();
        assertEquals("Expected to remove all items", true, actual);
    }

    public void testGetAll() {
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);

        Iterator<TokenCacheItem> results = store.getAll();
        assertNotNull("Iterator is supposed to be not null", results);
        assertTrue("Has authority", results.next().getAuthority().contains("authority"));
    }

    public void testGetUniqueUsers() {
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);
        HashSet<String> users = store.getUniqueUsersWithTokenCache();
        assertNotNull(users);
        assertEquals(1, users.size());
    }

    public void testGetTokensForResource() {
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);

        ArrayList<TokenCacheItem> tokens = store.getTokensForResource("resource");
        assertEquals("token size", 1, tokens.size());
        assertEquals("token content", "token", tokens.get(0).getAccessToken());

        tokens = store.getTokensForResource("resource2");
        assertEquals("token size", 1, tokens.size());
        assertEquals("token content", "token2", tokens.get(0).getAccessToken());

    }

    public void testGetTokensForUser() {
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);

        ArrayList<TokenCacheItem> tokens = store.getTokensForUser("userid");
        assertEquals("token size", 2, tokens.size());
    }

    public void testClearTokensForUser() {
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);

        store.clearTokensForUser("userid");

        ArrayList<TokenCacheItem> tokens = store.getTokensForUser("userid");
        assertEquals("token size", 0, tokens.size());
    }
}
