
package com.microsoft.adal;

import java.util.HashMap;

/*
 * Stores AuthenticationResult object inside memory cache.
 * It keeps single instance and does synchronization.
 * This is used if context is not available to use SharedPreference based cache.
 */
public class TokenMemoryCache implements ITokenCache {

    private static TokenCache sSharedInstance = new TokenCache();

    public static TokenCache getInstance()
    {
        return sSharedInstance;
    }

    private HashMap<String, AuthenticationResult> mCache;
    private Object mCacheLock;

    private TokenMemoryCache()
    {
        mCache = new HashMap<String, AuthenticationResult>();
        mCacheLock = new Object();
    }

    /**
     * Gets AuthenticationResult for a given key
     */
    public AuthenticationResult getResult(String key)
    {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key");

        synchronized (mCacheLock)
        {
            return mCache.get(key);
        }
    }

    /*
     * @key Lookup key that may be composed of authorization, resourceId, and
     * clientId
     * @result Result which has min of access token, refresh token and expire
     * time
     */
    public boolean putResult(String key, AuthenticationResult result)
    {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key");

        synchronized (mCacheLock)
        {
            mCache.put(key, result);
        }
        return true;
    }

    /**
     * @key     Remove result object for this key
     * 
     */
    public boolean removeResult(String key)
    {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key");

        synchronized (mCacheLock)
        {
            mCache.remove(key);
        }
        return true;
    }

    public boolean removeAll()
    {
        synchronized (mCacheLock)
        {
            mCache.clear();
        }
        return true;
    }

    @Override
    public HashMap<String, AuthenticationResult> getAllResults() {
        synchronized (mCacheLock)
        {
            return mCache;
        }
    }
}
