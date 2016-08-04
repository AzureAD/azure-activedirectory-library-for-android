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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * tokenCacheItem is not persisted. Memory cache does not keep static items.
 */
public class MemoryTokenCacheStore implements ITokenCacheStore {

    /**
     * 
     */
    private static final long serialVersionUID = 3465700945655867086L;

    private static final String TAG = "MemoryTokenCacheStore";

    private final Map<String, TokenCacheItem> mCache = new HashMap<>();

    private transient Object mCacheLock = new Object();

    /**
     * Creates MemoryTokenCacheStore.
     */
    public MemoryTokenCacheStore() {
    }

    @Override
    public TokenCacheItem getItem(String key) {
        if (key == null) {
            throw new IllegalArgumentException("The input key is null.");
        }

        Logger.v(TAG, "Get Item from cache. Key:" + key);
        synchronized (mCacheLock) {
            return mCache.get(key);
        }
    }

    @Override
    public void setItem(String key, TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item");
        }

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        Logger.v(TAG, "Set Item to cache. Key:" + key);
        synchronized (mCacheLock) {
            mCache.put(key, item);
        }
    }

    @Override
    public void removeItem(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        Logger.v(TAG, "Remove Item from cache. Key:" + key.hashCode());
        synchronized (mCacheLock) {
            mCache.remove(key);
        }
    }

    @Override
    public void removeAll() {
        Logger.v(TAG, "Remove all items from cache. Key:");
        synchronized (mCacheLock) {
            mCache.clear();
        }
    }

    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream inputStream) throws IOException,
            ClassNotFoundException {
        inputStream.defaultReadObject();

        mCacheLock = new Object();
    }

    @Override
    public boolean contains(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        Logger.v(TAG, "contains Item from cache. Key:" + key);
        synchronized (mCacheLock) {
            return mCache.get(key) != null;
        }
    }

    @Override
    public Iterator<TokenCacheItem> getAll() {
        Logger.v(TAG, "Retrieving all items from cache. ");
        synchronized (mCacheLock) {
            return mCache.values().iterator();
        }
    }
}
