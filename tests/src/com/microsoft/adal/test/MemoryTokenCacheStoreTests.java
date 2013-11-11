
package com.microsoft.adal.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.CountDownLatch;

import android.content.Context;

import com.microsoft.adal.CacheKey;
import com.microsoft.adal.ITokenCacheStore;
import com.microsoft.adal.MemoryTokenCacheStore;
import com.microsoft.adal.TokenCacheItem;
import com.microsoft.adal.UserInfo;

public class MemoryTokenCacheStoreTests extends AndroidTestHelper {

    private static final String TAG = "MemoryTokenCacheStoreTests";

    int activeTestThreads = 10;

    Context ctx;

    private TokenCacheItem testItem;

    private TokenCacheItem testItem2;

    private ITokenCacheStore setupCache() {

        MemoryTokenCacheStore store = new MemoryTokenCacheStore();
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
        return store;
    }

    public void testGetItem() {

        ITokenCacheStore store = setupCache();
        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(null, null, null));
        assertNull("Token cache item is expected to be null", item);

        // get item
        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);
        assertEquals("Same tokencacheitem content", testItem.getAuthority(), item.getAuthority());
        assertEquals("Same tokencacheitem content", testItem.getClientId(), item.getClientId());
        assertEquals("Same tokencacheitem content", testItem.getResource(), item.getResource());
    }

    public void testRemoveItem() {
        ITokenCacheStore store = setupCache();

        store.removeItem(CacheKey.createCacheKey(testItem));
        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);

        // second call should be null as well
        store.removeItem(CacheKey.createCacheKey(testItem));
        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

    public void testRemoveAll() {
        ITokenCacheStore store = setupCache();

        boolean actual = store.removeAll();
        assertEquals("Expected to remove all items", true, actual);

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

    /**
     * test the usage of cache from different threads
     */
    public void testSharedCacheGetItem() {
        final ITokenCacheStore store = setupCache();

        final CountDownLatch signal = new CountDownLatch(activeTestThreads);

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {

                TokenCacheItem item = store.getItem(CacheKey.createCacheKey(null, null, null));
                assertNull("Token cache item is expected to be null", item);

                // get item
                item = store.getItem(CacheKey.createCacheKey(testItem));
                assertNotNull("Token cache item is expected to be NOT null", item);
                assertEquals("Same tokencacheitem content", testItem.getAuthority(),
                        item.getAuthority());
                assertEquals("Same tokencacheitem content", testItem.getClientId(),
                        item.getClientId());
                assertEquals("Same tokencacheitem content", testItem.getResource(),
                        item.getResource());

                store.removeItem(testItem);
                item = store.getItem(CacheKey.createCacheKey(testItem));
                assertNull("Token cache item is expected to be null", item);

                item = store.getItem(CacheKey.createCacheKey(testItem2));
                assertNotNull("Token cache item is expected to be NOT null", item);
                signal.countDown();

                store.removeItem(testItem2);
                item = store.getItem(CacheKey.createCacheKey(testItem));
                assertNull("Token cache item is expected to be null", item);
            }
        };

        testMultiThread(activeTestThreads, signal, runnable);

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

    public void testSerialization() throws IOException, ClassNotFoundException {

        ITokenCacheStore store = setupCache();

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
        deSerialized.removeItem(testItem);
        item = deSerialized.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

}
