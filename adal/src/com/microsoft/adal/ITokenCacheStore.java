/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.Serializable;

/**
 * Minimal interface needed by ADAL for cache
 */
public interface ITokenCacheStore extends Serializable {
    public TokenCacheItem getItem(String key);// Needed by the library
    
    public boolean contains(String key);// Needed by the library

    public void setItem(String key, TokenCacheItem item);// Needed by the library

    public void removeItem(String key);// Needed by the library

    public void removeAll();
}
