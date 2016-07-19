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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface ITokenStoreQuery {

    /**
     * @return {@link Iterator} of all the {@link TokenCacheItem}s.
     */
    Iterator<TokenCacheItem> getAll();

    /**
     * @return {@link Set} of unique users in the token cache.
     */
    Set<String> getUniqueUsersWithTokenCache();

    /**
     * @param resource The resource to retrieve the token for.
     * @return {@link List} of {@link TokenCacheItem}s for the given resource.
     */
    List<TokenCacheItem> getTokensForResource(String resource);

    /**
     * @param userid The user id to retrieve the token for.
     * @return {@link List} of tokens for the given user id.
     */
    List<TokenCacheItem> getTokensForUser(String userid);

    /**
     * Clear tokens for given user.
     * @param userId The unique user id to clear the token for.
     */
    void clearTokensForUser(String userId);

    /**
     * @return A {@link List} of {@link TokenCacheItem}s that are going to expire.
     */
    List<TokenCacheItem> getTokensAboutToExpire();
}