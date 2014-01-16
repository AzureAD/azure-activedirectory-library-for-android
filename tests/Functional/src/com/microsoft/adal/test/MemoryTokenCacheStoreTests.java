
package com.microsoft.adal.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.CountDownLatch;

import android.content.Context;

import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.CacheKey;
import com.microsoft.adal.DefaultTokenCacheStore;
import com.microsoft.adal.ITokenCacheStore;
import com.microsoft.adal.MemoryTokenCacheStore;
import com.microsoft.adal.TokenCacheItem;
import com.microsoft.adal.UserInfo;
import com.microsoft.adal.test.AuthenticationContextTests.TestMockContext;

public class MemoryTokenCacheStoreTests extends BaseTokenStoreTests {

    private static final String TAG = "MemoryTokenCacheStoreTests";
    private static final String VALID_AUTHORITY = "https://Login.windows.net/Omercantest.Onmicrosoft.com";
    int activeTestThreads = 10;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ctx = this.getInstrumentation().getContext();
    }

    @Override
    protected void tearDown() throws Exception {
        MemoryTokenCacheStore store = new MemoryTokenCacheStore();
        store.removeAll();
        super.tearDown();
    }

    /**
     * test the usage of cache from different threads. It is expected to work
     * with multiThreads
     */
    public void testSharedCacheGetItem() {
        final ITokenCacheStore store = setupItems();

        final CountDownLatch signal = new CountDownLatch(activeTestThreads);

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {

                // Remove and then verify that
                // One thread will do the actual remove action.
                store.removeItem(CacheKey.createCacheKey(testItem));
                TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
                assertNull("Token cache item is expected to be null", item);

                item = store.getItem(CacheKey.createCacheKey("", "", "", false, ""));
                assertNull("Token cache item is expected to be null", item);

                store.removeItem(CacheKey.createCacheKey(testItem2));
                item = store.getItem(CacheKey.createCacheKey(testItem));
                assertNull("Token cache item is expected to be null", item);

                signal.countDown();
            }
        };

        testMultiThread(activeTestThreads, signal, runnable);

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

    public void testSerialization() throws IOException, ClassNotFoundException {

        ITokenCacheStore store = setupItems();

        ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(store);
        byte[] output = fileOut.toByteArray();
        fileOut.close();
        out.close();

        // read now
        ByteArrayInputStream fileIn = new ByteArrayInputStream(output);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        ITokenCacheStore deSerialized = (ITokenCacheStore)in.readObject();
        in.close();
        fileIn.close();

        // Verify the cache
        TokenCacheItem item = deSerialized.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);

        item = deSerialized.getItem(CacheKey.createCacheKey(testItem2));
        assertNotNull("Token cache item is expected to be NOT null", item);

        // do remove operation
        deSerialized.removeItem(CacheKey.createCacheKey(testItem));
        item = deSerialized.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

    /**
     * memory cache is shared between context
     */
    public void testMemoryCacheMultipleContext() {
        ITokenCacheStore tokenCacheA = setupItems();
        AuthenticationContext contextA = new AuthenticationContext(getInstrumentation()
                .getContext(), VALID_AUTHORITY, false, tokenCacheA);
        AuthenticationContext contextB = new AuthenticationContext(getInstrumentation()
                .getContext(), VALID_AUTHORITY, false, tokenCacheA);

        // Verify the cache
        TokenCacheItem item = contextA.getCache().getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);

        item = contextA.getCache().getItem(CacheKey.createCacheKey(testItem2));
        assertNotNull("Token cache item is expected to be NOT null", item);
        item = contextB.getCache().getItem(CacheKey.createCacheKey(testItem2));
        assertNotNull("Token cache item is expected to be NOT null", item);

        // do remove operation
        contextA.getCache().removeItem(CacheKey.createCacheKey(testItem));
        item = contextA.getCache().getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);

        item = contextB.getCache().getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);

    }

    @Override
    protected ITokenCacheStore getTokenCacheStore() {
        return new MemoryTokenCacheStore();
    }
}
