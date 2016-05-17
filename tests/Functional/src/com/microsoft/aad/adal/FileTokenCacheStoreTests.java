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

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import com.microsoft.aad.adal.Logger.ILogger;
import com.microsoft.aad.adal.Logger.LogLevel;

import android.content.Context;

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
        AuthenticationSettings.INSTANCE.setBrokerSignature("signature");
    }

    @Override
    protected void tearDown() throws Exception {

        FileTokenCacheStore store = new FileTokenCacheStore(targetContex, FILE_DEFAULT_NAME);
        store.removeAll();
        super.tearDown();
    }

    private void setupCache(String fileName) throws AuthenticationException {
        // set item and then get
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, fileName);
        store.removeAll();
        testItem = new TokenCacheItem();
        testItem.setAccessToken("token");
        testItem.setAuthority("authority");
        testItem.setClientId("clientid");
        testItem.setResource("resource");
        UserInfo user = new UserInfo("userid", "givenName", "familyName", "identity", "userid");
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
        String msgToCheck = "Existing cache format is wrong ver:" + AuthenticationContext.getVersionName();
        assertTrue("Verify message ", logger.logMessage.contains(msgToCheck));
    }

    public void testGetItem() throws AuthenticationException {
        String file = FILE_DEFAULT_NAME + "testGetItem";
        setupCache(file);
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, file);
        TokenCacheItem item = store.getItem(CacheKey.createCacheKey("", "", "", false, "", null));
        assertNull("Token cache item is expected to be null", item);

        // get item
        item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNotNull("Token cache item is expected to be NOT null", item);
        assertEquals("Same tokencacheitem content", testItem.getAuthority(), item.getAuthority());
        assertEquals("Same tokencacheitem content", testItem.getClientId(), item.getClientId());
        assertEquals("Same tokencacheitem content", testItem.getResource(), item.getResource());
    }

    public void testWriteFileException() throws AuthenticationException {
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

    public void testRemoveItem() throws AuthenticationException {
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

    public void testRemoveAll() throws AuthenticationException {
        String file = FILE_DEFAULT_NAME + "testGetItem";
        setupCache(file);
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, file);

        store.removeAll();

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }
    
    public void testGetAll() throws AuthenticationException {
        String file = FILE_DEFAULT_NAME + "testGetItem";
        setupCache(file);
        ITokenCacheStore store = new FileTokenCacheStore(targetContex, file);

        final Iterator<TokenCacheItem> allItems = store.getAll();
        
        assertTrue(allItems.hasNext());
        final TokenCacheItem tokenCacheItem1 = allItems.next();
        assertNotNull(tokenCacheItem1);
        
        assertTrue(allItems.hasNext());
        final TokenCacheItem tokenCacheItem2 = allItems.next();
        assertNotNull(tokenCacheItem2);
        
        assertFalse(allItems.hasNext());
    }

    /**
     * test the usage of cache from different threads. It is expected to work
     * with multiThreads
     * @throws AuthenticationException 
     */
    public void testSharedCacheGetItem() throws AuthenticationException {
        String file = FILE_DEFAULT_NAME + "testGetItem";
        setupCache(file);
        final ITokenCacheStore store = new FileTokenCacheStore(targetContex, file);
        final CountDownLatch signal = new CountDownLatch(activeTestThreads);
        final Runnable runnable = new Runnable() {

            @Override
            public void run() {

                // Remove and then verify that
                // One thread will do the actual remove action.
                try {
                    store.removeItem(CacheKey.createCacheKey(testItem));
                    TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
                    assertNull("Token cache item is expected to be null", item);

                    item = store.getItem(CacheKey.createCacheKey("", "", "", false, "", null));
                    assertNull("Token cache item is expected to be null", item);

                    store.removeItem(CacheKey.createCacheKey(testItem2));
                    item = store.getItem(CacheKey.createCacheKey(testItem));
                    assertNull("Token cache item is expected to be null", item);
                } catch (AuthenticationException e) {
                    e.printStackTrace();
                } finally {
                    signal.countDown();
                }
            }
        };

        testMultiThread(activeTestThreads, signal, runnable);

        TokenCacheItem item = store.getItem(CacheKey.createCacheKey(testItem));
        assertNull("Token cache item is expected to be null", item);
    }

    /**
     * memory cache is shared between context
     * @throws AuthenticationException 
     */
    public void testMemoryCacheMultipleContext() throws AuthenticationException {
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
