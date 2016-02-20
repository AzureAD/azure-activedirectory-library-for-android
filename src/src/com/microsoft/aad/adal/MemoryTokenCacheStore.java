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

package com.microsoft.aad.adal;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * tokenCacheItem is not persisted. Memory cache does not keep static items.
 */
public class MemoryTokenCacheStore implements ITokenCacheStore {

    /**
     * 
     */
    private static final long serialVersionUID = 3465700945655867086L;

    private static final String TAG = "MemoryTokenCacheStore";

    private final HashMap<String, TokenCacheItem> mCache = new HashMap<String, TokenCacheItem>();

    private transient Object mCacheLock = new Object();

    /**
     * Creates MemoryTokenCacheStore.
     */
    public MemoryTokenCacheStore() {
    }

    @Override
    public TokenCacheItem getItem(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
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

    private synchronized void readObject(ObjectInputStream inputStream) throws NotActiveException,
            IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        mCacheLock = new Object();
    }

    @Override
    public boolean contains(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        Logger.v(TAG, "contains Item from cache. Key:" + key.toString());
        synchronized (mCacheLock) {
            return mCache.get(key) != null;
        }
    }
}
