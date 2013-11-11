
package com.microsoft.adal;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import android.util.Log;

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

    private final HashMap<String, TokenCacheItem> mCache = new HashMap<String, TokenCacheItem>();

    private final Object mCacheLock = new Object();

    public MemoryTokenCacheStore() {
    }

    @Override
    public TokenCacheItem getItem(CacheKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        Log.v(TAG, "Get Item from cache. Key:" + key.toString());
        synchronized (mCacheLock) {
            return mCache.get(key.toString());
        }
    }

    @Override
    public boolean setItem(TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("key");
        }
        CacheKey key = CacheKey.createCacheKey(item);
        Log.v(TAG, "Set Item to cache. Key:" + key.toString());
        synchronized (mCacheLock) {
            mCache.put(key.toString(), item);
        }

        return true;
    }

    @Override
    public boolean removeItem(CacheKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        Log.v(TAG, "Remove Item from cache. Key:" + key.toString());
        synchronized (mCacheLock) {
            mCache.remove(key.toString());
        }

        return true;
    }

    @Override
    public boolean removeItem(TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item");
        }

        CacheKey key = CacheKey.createCacheKey(item);
        Log.v(TAG, "Remove Item from cache. Key:" + key.toString());
        synchronized (mCacheLock) {
            mCache.remove(key.toString());
        }

        return true;
    }

    @Override
    public boolean removeAll() {
        Log.v(TAG, "Remove all items from cache. Key:");
        synchronized (mCacheLock) {
            mCache.clear();
        }
        return true;
    }

    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private synchronized void readObject(ObjectInputStream inputStream) throws NotActiveException,
            IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
    }
}
