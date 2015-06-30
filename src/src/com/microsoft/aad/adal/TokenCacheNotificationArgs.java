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

/**
 * Contains parameters used by the ADAL call accessing the cache.
 */
public class TokenCacheNotificationArgs {

    private String mClientId;

    private String[] mScope;

    private String mUniqueId;

    private String mDisplayableId;

    public String getClientId() {
        return mClientId;
    }

    public String[] getScope() {
        return mScope;
    }

    public String getUniqueId() {
        return mUniqueId;
    }

    public String getDisplayableId() {
        return mDisplayableId;
    }
    
    /**
     * internal usage for notification
     * @param key
     * @return
     */
    static TokenCacheNotificationArgs create(TokenCacheKey key){
        TokenCacheNotificationArgs cacheNotication = new TokenCacheNotificationArgs();
        cacheNotication.mClientId = key.getClientId();
        cacheNotication.mScope = key.getScope();
        cacheNotication.mDisplayableId = key.getDisplayableId();
        cacheNotication.mUniqueId = key.getUniqueId();
        return cacheNotication;
    }
}
