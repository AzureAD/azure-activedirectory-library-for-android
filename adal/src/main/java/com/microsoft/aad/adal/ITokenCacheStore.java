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

import java.io.Serializable;
import java.util.Iterator;

/**
 * Minimal interface needed by ADAL for cache.
 */
public interface ITokenCacheStore extends Serializable {

    /**
     * Get cache item.
     * 
     * @param key {@link CacheKey}
     * @return Token cache item
     */
    TokenCacheItem getItem(String key);
    
    /**
     * Get all cached token items. 
     * @return {@link Iterator} of {@link TokenCacheItem}s in the cache. 
     */
    Iterator<TokenCacheItem> getAll();

    /**
     * Checks if cache key exists.
     * @param key {@link CacheKey}
     * @return true if it exists
     */
    boolean contains(String key);

    /**
     * Sets item.
     * @param key {@link CacheKey}
     * @param item Cache item
     */
    void setItem(String key, TokenCacheItem item);

    /**
     * Removes item with key.
     * @param key {@link CacheKey}
     */
    void removeItem(String key);

    /**
     * Removes all items from cache.
     */
    void removeAll();
}
