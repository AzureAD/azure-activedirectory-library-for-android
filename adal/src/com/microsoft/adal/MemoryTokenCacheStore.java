
package com.microsoft.adal;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import android.util.Log;
import android.util.SparseArray;

/**
 * tokenCacheItem is not persisted. Memory cache does not keep static items.
 * 
 * @author omercan
 */
public class MemoryTokenCacheStore implements ITokenCacheStore {

    /**
     * 
     */
    private static final long serialVersionUID = 3465700945655867086L;

    private static final String TAG = "MemoryTokenCacheStore";

    private final SparseArray<TokenCacheItem> mCache = new SparseArray<TokenCacheItem>();

    private transient Object mCacheLock = new Object();

    public MemoryTokenCacheStore() {
    }

    @Override
    public TokenCacheItem getItem(CacheKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        Log.v(TAG, "Get Item from cache. Key:" + key.hashCode());
        synchronized (mCacheLock) {
            return mCache.get(key.hashCode());
        }
    }

    @Override
    public void setItem(CacheKey key, TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item");
        }

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        Log.v(TAG, "Set Item to cache. Key:" + key.hashCode());
        synchronized (mCacheLock) {
            mCache.append(key.hashCode(), item);
        }
    }

    @Override
    public void removeItem(CacheKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        Log.v(TAG, "Remove Item from cache. Key:" + key.hashCode());
        synchronized (mCacheLock) {
            mCache.delete(key.hashCode());
        }
    }

    @Override
    public void removeAll() {
        Log.v(TAG, "Remove all items from cache. Key:");
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
    public boolean contains(CacheKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        Log.v(TAG, "contains Item from cache. Key:" + key.toString());
        synchronized (mCacheLock) {
            return mCache.get(key.hashCode()) != null;
        }
    }
}
