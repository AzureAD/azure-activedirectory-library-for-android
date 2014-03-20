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
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.CountDownLatch;

import android.content.Context;

import com.microsoft.adal.ADALError;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationSettings;
import com.microsoft.adal.CacheKey;
import com.microsoft.adal.FileTokenCacheStore;
import com.microsoft.adal.ITokenCacheStore;
import com.microsoft.adal.Logger;
import com.microsoft.adal.Logger.ILogger;
import com.microsoft.adal.Logger.LogLevel;
import com.microsoft.adal.TokenCacheItem;
import com.microsoft.adal.UserInfo;

public class FileTokenCacheStoreTests extends AndroidTestHelper {

    int activeTestThreads = 10;

    Context targetContex;

    private TokenCacheItem testItem;

    private TokenCacheItem testItem2;

    private static final String VALID_AUTHORITY = "https://Login.windows.net/Omercantest.Onmicrosoft.com";

    private final String FILE_DEFAULT_NAME = "testfile";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        targetContex = this.getInstrumentation().getTargetContext();
        AuthenticationSettings.INSTANCE.setBrokerPackageName("invalid");
    }

    @Override
    protected void tearDown() throws Exception {

        FileTokenCacheStore store = new FileTokenCacheStore(targetContex, FILE_DEFAULT_NAME);
        store.removeAll();
        super.tearDown();
    }

    private void setupCache(String fileName) {
        // set item and then get
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, fileName);
        store.removeAll();
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
        store.setItem(CacheKey.createCacheKey(testItem), testItem);
        store.setItem(CacheKey.createCacheKey(testItem2), testItem2);
    }

    public void testFileCacheWriteError() {
        final FileMockContext mockContext = new FileMockContext(targetContex);
        assertThrowsException(IllegalStateException.class,
                "it could not access the authorization cache directory", new Runnable() {
                    @Override
                    public void run() {
                        ITokenCacheStore store = new FileTokenCacheStore(mockContext,
                                FILE_DEFAULT_NAME);
                    }
                });

        assertEquals("Check requested directory name", FileMockContext.PREFIX, mockContext.dirName);
        assertEquals("Check requested mode", Context.MODE_PRIVATE, mockContext.fileWriteMode);
    }

    public void testLoadingFromInvalidCacheFile() {

        File directory = targetContex.getDir(targetContex.getPackageName(), Context.MODE_PRIVATE);
        File mock = new File(directory, FILE_DEFAULT_NAME);
        try {
            mock.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(mock);
            ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
            // write invalid item to a file
            objectStream.writeObject(new TokenCacheItem());
            objectStream.flush();
            objectStream.close();
            outputStream.close();

        } catch (Exception e) {
            fail("Error in mocking file");
        }

        CustomLogger logger = new CustomLogger();
        Logger.getInstance().setExternalLogger(logger);
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, FILE_DEFAULT_NAME);

        assertEquals("Verify message", "Existing cache format is wrong", logger.logMessage);
    }

    public void testGetItem() {
        String file = FILE_DEFAULT_NAME + "testGetItem";
        setupCache(file);
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, file);
        TokenCacheItem item = store.getItem(CacheKey.createCacheKey("", "", "", false, ""));
        assertNull("Token cache item is expected to be null", item);

        // get item
        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);
        assertEquals("Same tokencacheitem content", testItem.getAuthority(), item.getAuthority());
        assertEquals("Same tokencacheitem content", testItem.getClientId(), item.getClientId());
        assertEquals("Same tokencacheitem content", testItem.getResource(), item.getResource());
    }

    public void testWriteFileException() {
        String file = FILE_DEFAULT_NAME + "fileWriteFileException";
        setupCache(file);
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, file);
        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);

        // Change file permissions to cause an error
        CustomLogger logger = new CustomLogger();
        Logger.getInstance().setExternalLogger(logger);
        File directory = targetContex.getDir(targetContex.getPackageName(), Context.MODE_PRIVATE);
        File mock = new File(directory, file);
        mock.setWritable(false);
        store.removeItem(CacheKey.createCacheKey(testItem));

        assertEquals("Permission issue", ADALError.DEVICE_FILE_CACHE_IS_NOT_WRITING_TO_FILE,
                logger.logErrorCode);

        mock.setWritable(true);
    }

    public void testRemoveItem() {
        String file = FILE_DEFAULT_NAME + "testRemoveItem";
        String file2 = FILE_DEFAULT_NAME + "testRemoveItem2";
        setupCache(file);
        setupCache(file2);
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, file);
        ITokenCacheStore store2 = new FileTokenCacheStore(targetContex, file2);

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
        String file = FILE_DEFAULT_NAME + "testGetItem";
        setupCache(file);
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, file);

        store.removeAll();

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

    /**
     * test the usage of cache from different threads. It is expected to work
     * with multiThreads
     */
    public void testSharedCacheGetItem() {
        String file = FILE_DEFAULT_NAME + "testGetItem";
        setupCache(file);
        final ITokenCacheStore store = new FileTokenCacheStore(targetContex, file);
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

    /**
     * memory cache is shared between context
     */
    public void testMemoryCacheMultipleContext() {
        String file = FILE_DEFAULT_NAME + "testGetItem";
        setupCache(file);
        ITokenCacheStore tokenCacheA = new FileTokenCacheStore(targetContex, file);
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

    class CustomLogger implements ILogger {

        String logMessage;

        ADALError logErrorCode;

        @Override
        public void Log(String tag, String message, String additionalMessage, LogLevel level,
                ADALError errorCode) {
            logMessage = message;
            logErrorCode = errorCode;
        }
    }
}
