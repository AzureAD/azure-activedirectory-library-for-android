/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.Serializable;

/**
 * Minimal interface needed by ADAL for cache
 */
public interface ITokenCacheStore extends Serializable {
    public TokenCacheItem getItem(CacheKey key);// Needed by the library

    public void setItem(TokenCacheItem item);// Needed by the library

    public void removeItem(CacheKey key);// Needed by the library

    public void removeItem(TokenCacheItem item);

    public void removeAll();
}
