
package com.microsoft.adal.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import android.content.Context;

import com.microsoft.adal.CacheKey;
import com.microsoft.adal.DefaultTokenCacheStore;
import com.microsoft.adal.ITokenCacheStore;
import com.microsoft.adal.TokenCacheItem;
import com.microsoft.adal.UserInfo;

public class DefaultTokenCacheStoreTests extends BaseTokenStoreTests {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ctx = this.getInstrumentation().getContext();
    }

    @Override
    protected void tearDown() throws Exception {
        DefaultTokenCacheStore store = new DefaultTokenCacheStore(ctx);
        store.removeAll();
        super.tearDown();
    }

    public void testGetAll() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        Iterator<TokenCacheItem> results = store.getAll();
        assertNotNull("Iterator is supposed to be not null", results);
        assertTrue("Has clientid", results.next().getAuthority().contains("client"));
    }

    public void testGetUniqueUsers() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();
        HashSet<String> users = store.getUniqueUsersWithTokenCache();
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    public void testGetTokensForResource() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        ArrayList<TokenCacheItem> tokens = store.getTokensForResource("resource");
        assertEquals("token size", 1, tokens.size());
        assertEquals("token content", "token", tokens.get(0).getAccessToken());

        tokens = store.getTokensForResource("resource2");
        assertEquals("token size", 2, tokens.size());
        assertEquals("token content", "token2", tokens.get(0).getAccessToken());

    }

    public void testGetTokensForUser() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        ArrayList<TokenCacheItem> tokens = store.getTokensForUser("userid");
        assertEquals("token size", 2, tokens.size());

        tokens = store.getTokensForUser("userid2");
        assertEquals("token size", 2, tokens.size());
    }

    public void testClearTokensForUser() {
        DefaultTokenCacheStore store = (DefaultTokenCacheStore)setupItems();

        store.clearTokensForUser("userid");

        ArrayList<TokenCacheItem> tokens = store.getTokensForUser("userid");
        assertEquals("token size", 0, tokens.size());

        store.clearTokensForUser("userid2");

        tokens = store.getTokensForUser("userid2");
        assertEquals("token size", 0, tokens.size());

        Iterator<TokenCacheItem> results = store.getAll();
        assertFalse("It does not have items", results.hasNext());
    }

    @Override
    protected ITokenCacheStore getTokenCacheStore() {
        return new DefaultTokenCacheStore(ctx);
    }
}
