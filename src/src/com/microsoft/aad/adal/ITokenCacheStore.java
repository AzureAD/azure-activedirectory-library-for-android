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

import java.io.Serializable;
import java.util.List;

/**
 * Minimal interface needed by ADAL for cache.
 */
public interface ITokenCacheStore extends Serializable {

    /**
     * Get cache items.
     * 
     * @return Token cache item
     */
    List<TokenCacheItem> readItems();

    /**
     * Removes item with key.
     * 
     * @param item {@link TokenCacheItem}
     */
    void deleteItem(TokenCacheItem item);

    /**
     * Removes all items from cache.
     */
    void clear();

    /**
     * Called before using the cache.
     * 
     * @param args
     */
    void beforeAccess(TokenCacheNotificationArgs args);

    /**
     * Notification method called before any library method writes to the cache.
     * 
     * @param args
     */
    void beforeWrite(TokenCacheNotificationArgs args);

    /**
     * Notification method called after any library method accesses the cache.
     * 
     * @param args
     */
    void afterAccess(TokenCacheNotificationArgs args);
    
    void stateChanged();
    
    String serialize();
    
    void deserialize(String blob);
}
