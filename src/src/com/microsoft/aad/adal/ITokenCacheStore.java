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
