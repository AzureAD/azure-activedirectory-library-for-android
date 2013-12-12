
package com.microsoft.adal.test;

import java.util.concurrent.CountDownLatch;

import android.content.Context;

import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.CacheKey;
import com.microsoft.adal.FileTokenCacheStore;
import com.microsoft.adal.ITokenCacheStore;
import com.microsoft.adal.TokenCacheItem;
import com.microsoft.adal.UserInfo;

/**
 * TODO: Cache related tests will be merged later.
 * 
 * @author omercan
 */
public class FileTokenCacheStoreTests extends AndroidTestHelper {

    int activeTestThreads = 10;

    Context ctx;

    private TokenCacheItem testItem;

    private TokenCacheItem testItem2;

    private final String FILE_DEFAULT_NAME = "testfile";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ctx = this.getInstrumentation().getContext();
    }

    @Override
    protected void tearDown() throws Exception {

        FileTokenCacheStore store = new FileTokenCacheStore(ctx, FILE_DEFAULT_NAME);
        store.removeAll();
        super.tearDown();
    }

    private ITokenCacheStore setupCache(String fileName) {
        // set item and then get
        ITokenCacheStore store = new FileTokenCacheStore(ctx, fileName);
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
        return store;
    }

    public void testGetItem() {

        ITokenCacheStore store = setupCache(FILE_DEFAULT_NAME);
        TokenCacheItem item = store.getItem(CacheKey.createCacheKey("", "", ""));
        assertNull("Token cache item is expected to be null", item);

        // get item
        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);
        assertEquals("Same tokencacheitem content", testItem.getAuthority(), item.getAuthority());
        assertEquals("Same tokencacheitem content", testItem.getClientId(), item.getClientId());
        assertEquals("Same tokencacheitem content", testItem.getResource(), item.getResource());
    }

    public void testRemoveItem() {
        ITokenCacheStore store = setupCache(FILE_DEFAULT_NAME);
        ITokenCacheStore store2 = setupCache(FILE_DEFAULT_NAME + "2");

        store.removeItem(CacheKey.createCacheKey(testItem));
        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
        TokenCacheItem itemInCache2 = store2.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", itemInCache2);

        // second call should be null as well
        store.removeItem(CacheKey.createCacheKey(testItem));
        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);

        store2.removeAll();
    }

    public void testRemoveAll() {
        ITokenCacheStore store = setupCache(FILE_DEFAULT_NAME);

        store.removeAll();

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

    /**
     * test the usage of cache from different threads. It is expected to work
     * with multiThreads
     */
    public void testSharedCacheGetItem() {
        final ITokenCacheStore store = setupCache(FILE_DEFAULT_NAME);

        final CountDownLatch signal = new CountDownLatch(activeTestThreads);

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {

                // Remove and then verify that
                // One thread will do the actual remove action.
                store.removeItem(testItem);
                TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
                assertNull("Token cache item is expected to be null", item);

                item = store.getItem(CacheKey.createCacheKey("", "", ""));
                assertNull("Token cache item is expected to be null", item);

                store.removeItem(testItem2);
                item = store.getItem(CacheKey.createCacheKey(testItem));
                assertNull("Token cache item is expected to be null", item);

                signal.countDown();
            }
        };

        testMultiThread(activeTestThreads, signal, runnable);

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

    /**
     * memory cache is shared between context
     */
    public void testMemoryCacheMultipleContext() {
        ITokenCacheStore tokenCacheA = setupCache(FILE_DEFAULT_NAME);
        AuthenticationContext contextA = new AuthenticationContext(getInstrumentation()
                .getContext(), "authority", false, tokenCacheA);
        AuthenticationContext contextB = new AuthenticationContext(getInstrumentation()
                .getContext(), "authority", false, tokenCacheA);

        // Verify the cache
        TokenCacheItem item = contextA.getCache().getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);

        item = contextA.getCache().getItem(CacheKey.createCacheKey(testItem2));
        assertNotNull("Token cache item is expected to be NOT null", item);
        item = contextB.getCache().getItem(CacheKey.createCacheKey(testItem2));
        assertNotNull("Token cache item is expected to be NOT null", item);

        // do remove operation
        contextA.getCache().removeItem(testItem);
        item = contextA.getCache().getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);

        item = contextB.getCache().getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }
}
