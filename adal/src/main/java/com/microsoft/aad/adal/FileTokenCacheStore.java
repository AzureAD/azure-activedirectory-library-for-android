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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;

import android.content.Context;

/**
 * Persisted cache that keeps cache in-memory until write operation. Filename
 * should not be used on another instance of FiletokenCacheStore since read
 * operations are not synced to file.
 */
public class FileTokenCacheStore implements ITokenCacheStore {

    /**
     * Default serial version.
     */
    private static final long serialVersionUID = -8252291336171327870L;

    private static final String TAG = null;

    private final File mFile;

    private final MemoryTokenCacheStore mInMemoryCache;

    private final Object mCacheLock = new Object();

    /**
     * It tracks data in memory until it writes that to a file with write
     * operation.
     * 
     * @param context {@link Context}
     * @param fileName filename should be unique to this instance since read
     *            operations don't read from file directly. write operations
     *            write to a file.
     */
    public FileTokenCacheStore(Context context, String fileName) {
        if (context == null) {
            throw new IllegalArgumentException("context");
        }

        if (StringExtensions.isNullOrBlank(fileName)) {
            throw new IllegalArgumentException("fileName");
        }

        // It is using package directory not the external storage, so
        // external write permissions are not needed
        final File directory = context.getDir(context.getPackageName(), Context.MODE_PRIVATE);

        if (directory == null) {
            throw new IllegalStateException("It could not access the Authorization cache directory");
        }

        // Initialize cache from file if it exists
        try {
            mFile = new File(directory, fileName);

            if (mFile.exists()) {
                Logger.v(TAG, "There is previous cache file to load cache.");
                FileInputStream inputStream = new FileInputStream(mFile);
                ObjectInputStream objectStream = new ObjectInputStream(inputStream);
                Object cacheObj = objectStream.readObject();
                inputStream.close();
                objectStream.close();

                if (cacheObj instanceof MemoryTokenCacheStore) {
                    mInMemoryCache = (MemoryTokenCacheStore) cacheObj;
                } else {
                    Logger.w(TAG, "Existing cache format is wrong", "",
                            ADALError.DEVICE_FILE_CACHE_FORMAT_IS_WRONG);

                    // Write operation will replace with correct file
                    mInMemoryCache = new MemoryTokenCacheStore();
                }
            } else {
                Logger.v(TAG, "There is not any previous cache file to load cache.");
                mInMemoryCache = new MemoryTokenCacheStore();
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.e(TAG, "Exception during cache load",
                    ExceptionExtensions.getExceptionMessage(ex),
                    ADALError.DEVICE_FILE_CACHE_IS_NOT_LOADED_FROM_FILE);
            // if it is not possible to load the cache because of permissions or
            // similar, it will not work again. File cache is not working and
            // not make sense to use it.
            throw new IllegalStateException(ex);

        }
    }

    @Override
    public TokenCacheItem getItem(String key) {
        return mInMemoryCache.getItem(key);
    }

    @Override
    public boolean contains(String key) {
        return mInMemoryCache.contains(key);
    }

    @Override
    public void setItem(String key, TokenCacheItem item) {
        mInMemoryCache.setItem(key, item);
        writeToFile();
    }


    @Override
    public void removeItem(String key) {
        mInMemoryCache.removeItem(key);
        writeToFile();
    }

    @Override
    public void removeAll() {
        mInMemoryCache.removeAll();
        writeToFile();
    }

    private void writeToFile() {
        synchronized (mCacheLock) {
            if (mFile != null && mInMemoryCache != null) {
                try {

                    // FileOutputStream will create the file.
                    FileOutputStream outputStream = new FileOutputStream(mFile);
                    ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
                    objectStream.writeObject(mInMemoryCache);
                    objectStream.flush();
                    objectStream.close();
                    outputStream.close();

                } catch (IOException ex) {
                    Logger.e(TAG, "Exception during cache flush",
                            ExceptionExtensions.getExceptionMessage(ex),
                            ADALError.DEVICE_FILE_CACHE_IS_NOT_WRITING_TO_FILE);
                }
            }
        }
    }

    @Override
    public Iterator<TokenCacheItem> getAll() {
        return mInMemoryCache.getAll();
    }
}
