package com.microsoft.adal;
/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Does cache related actions such as putResult, getResult, removeResult for AuthenticationResult
 * 
 * @author omercan
 */
public class TokenCache implements ITokenCache {

    private static TokenCache sSharedInstance = new TokenCache();

    public static TokenCache getInstance()
    {
        return sSharedInstance;
    }

    private HashMap<String, AuthenticationResult> mCache;
    private Object mCacheLock;

    private TokenCache()
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
    public void putResult(String key, AuthenticationResult result)
    {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key");

        synchronized (mCacheLock)
        {
            mCache.put(key, result);
        }
    }

    public void removeResult(String key)
    {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key");

        synchronized (mCacheLock)
        {
            mCache.remove(key);
        }
    }

    public void removeAll()
    {
        synchronized (mCacheLock)
        {
            mCache.clear();
        }
    }
}
