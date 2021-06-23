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

import static com.microsoft.aad.adal.TokenCacheAccessor.getMsalOAuth2TokenCache;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Iterator;

class DelegatingCache implements ITokenCacheStore {

    private final Context mContext;
    private final ITokenCacheStore mDelegate;

    DelegatingCache(@NonNull final Context context,
                    @NonNull final ITokenCacheStore delegate) {
        mContext = context;
        mDelegate = delegate;
    }

    public ITokenCacheStore getDelegateCache() {
        return mDelegate;
    }

    @Override
    public TokenCacheItem getItem(final String key) {
        return mDelegate.getItem(key);
    }

    @Override
    public Iterator<TokenCacheItem> getAll() {
        return mDelegate.getAll();
    }

    @Override
    public boolean contains(final String key) {
        return mDelegate.contains(key);
    }

    @Override
    public void setItem(final String key, final TokenCacheItem item) {
        mDelegate.setItem(key, item);
    }

    @Override
    public void removeItem(final String key) {
        mDelegate.removeItem(key);
    }

    @Override
    public void removeAll() {
        // Clear our original cache
        mDelegate.removeAll();

        // clear our replica cache
        getMsalOAuth2TokenCache(mContext).clearAll();
    }
}
