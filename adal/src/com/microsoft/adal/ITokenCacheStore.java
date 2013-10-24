/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.Serializable;

/**
 * Minimal interface needed by ADAL for cache
 */
public interface ITokenCacheStore extends Serializable
{
    public TokenCacheItem GetItem(CacheKey key);// Needed by the library

    public void SetItem(TokenCacheItem item);// Needed by the library

    public void RemoveItem(CacheKey key);// Needed by the library

    public void RemoveItem(TokenCacheItem item);

    public void RemoveAll();
}
