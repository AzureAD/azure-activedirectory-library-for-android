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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

import javax.crypto.NoSuchPaddingException;

public class MemoryTokenCacheStoreTests extends BaseTokenStoreTests {

    private static final String VALID_AUTHORITY = "https://Login.windows.net/Omercantest.Onmicrosoft.com";

    private static final int ACTIVE_TEST_THREADS = 10;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
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
     * 
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws AuthenticationException 
     */
    public void testSharedCacheGetItem() throws NoSuchAlgorithmException, NoSuchPaddingException, AuthenticationException {
        final ITokenCacheStore store = setupItems();

        final CountDownLatch signal = new CountDownLatch(ACTIVE_TEST_THREADS);

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {

                // Remove and then verify that
                // One thread will do the actual remove action.
                try {
                    store.removeItem(CacheKey.createCacheKey(getTestItem()));
                    TokenCacheItem item = store.getItem(CacheKey.createCacheKey(getTestItem()));
                    assertNull("Token cache item is expected to be null", item);

                    item = store.getItem(CacheKey.createCacheKey("", "", "", false, "", null));
                    assertNull("Token cache item is expected to be null", item);

                    store.removeItem(CacheKey.createCacheKey(getTestItem2()));
                    item = store.getItem(CacheKey.createCacheKey(getTestItem()));
                    assertNull("Token cache item is expected to be null", item);
                } catch (AuthenticationException e) {
                    e.printStackTrace();
                } finally {
                    signal.countDown();
                }
            }
        };

        testMultiThread(ACTIVE_TEST_THREADS, signal, runnable);

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(getTestItem()));
        assertNull("Token cache item is expected to be null", item);
    }

    public void testSerialization() throws IOException, ClassNotFoundException,
            NoSuchAlgorithmException, NoSuchPaddingException, AuthenticationException {

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
        ITokenCacheStore deSerialized = (ITokenCacheStore) in.readObject();
        in.close();
        fileIn.close();

        // Verify the cache
        TokenCacheItem item = deSerialized.getItem(CacheKey.createCacheKey(getTestItem()));
        assertNotNull("Token cache item is expected to be NOT null", item);

        item = deSerialized.getItem(CacheKey.createCacheKey(getTestItem2()));
        assertNotNull("Token cache item is expected to be NOT null", item);

        // do remove operation
        deSerialized.removeItem(CacheKey.createCacheKey(getTestItem()));
        item = deSerialized.getItem(CacheKey.createCacheKey(getTestItem()));
        assertNull("Token cache item is expected to be null", item);
    }

    /**
     * memory cache is shared between context
     * 
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws AuthenticationException 
     */
    public void testMemoryCacheMultipleContext() throws NoSuchAlgorithmException,
            NoSuchPaddingException, AuthenticationException {
        ITokenCacheStore tokenCacheA = setupItems();
        AuthenticationContext contextA = new AuthenticationContext(getInstrumentation()
                .getContext(), VALID_AUTHORITY, false, tokenCacheA);
        AuthenticationContext contextB = new AuthenticationContext(getInstrumentation()
                .getContext(), VALID_AUTHORITY, false, tokenCacheA);

        // Verify the cache
        TokenCacheItem item = contextA.getCache().getItem(CacheKey.createCacheKey(getTestItem()));
        assertNotNull("Token cache item is expected to be NOT null", item);

        item = contextA.getCache().getItem(CacheKey.createCacheKey(getTestItem2()));
        assertNotNull("Token cache item is expected to be NOT null", item);
        item = contextB.getCache().getItem(CacheKey.createCacheKey(getTestItem2()));
        assertNotNull("Token cache item is expected to be NOT null", item);

        // do remove operation
        contextA.getCache().removeItem(CacheKey.createCacheKey(getTestItem()));
        item = contextA.getCache().getItem(CacheKey.createCacheKey(getTestItem()));
        assertNull("Token cache item is expected to be null", item);

        item = contextB.getCache().getItem(CacheKey.createCacheKey(getTestItem()));
        assertNull("Token cache item is expected to be null", item);
    }

    @Override
    protected ITokenCacheStore getTokenCacheStore() {
        return new MemoryTokenCacheStore();
    }
}
