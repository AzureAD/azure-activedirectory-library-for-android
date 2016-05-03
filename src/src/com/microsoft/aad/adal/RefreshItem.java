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

import java.util.ArrayList;
import java.util.List;

/**
 * Internal class holding the state for the item used to send refresh token request. 
 */
class RefreshItem {
    final String mRefreshToken;
    final String mKey;
    final boolean mMultiResource;
    final String mFamilyClientId;
    final UserInfo mUserInfo;
    final String mRawIdToken;
    final String mTenantId;
    final KeyEntryType mKeyEntryType;
    final List<String> mKeysWithUser = new ArrayList<String>();
    
    RefreshItem(final AuthenticationRequest authenticationRequest, final String keyInCache, final KeyEntryType refreshTokenEntryType, final TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item");
        }
        mKey = keyInCache;
        mMultiResource = item.getIsMultiResourceRefreshToken();
        mRefreshToken = item.getRefreshToken();
        mFamilyClientId = item.getFamilyClientId();
        mUserInfo = item.getUserInfo();
        mRawIdToken = item.getRawIdToken();
        mTenantId = item.getTenantId();
        mKeyEntryType = refreshTokenEntryType;
        
        if (mUserInfo != null) {
            final String mKeyWithUniqueUserId = CacheKey.createCacheKey(authenticationRequest, refreshTokenEntryType, mUserInfo.getUserId());
            final String mKeyWithDisplayableId = CacheKey.createCacheKey(authenticationRequest, refreshTokenEntryType, mUserInfo.getDisplayableId());
            mKeysWithUser.add(mKeyWithUniqueUserId);
            mKeysWithUser.add(mKeyWithDisplayableId);
            
            if (KeyEntryType.MULTI_RESOURCE_REFRESH_TOKEN_ENTRY == refreshTokenEntryType) {
                mKeysWithUser.add(CacheKey.createCacheKey(authenticationRequest, mUserInfo.getUserId()));
                mKeysWithUser.add(CacheKey.createCacheKey(authenticationRequest, mUserInfo.getDisplayableId()));
            }
        }
    }

    enum KeyEntryType {
        REGULAR_REFRESH_TOKEN_ENTRY, 
        MULTI_RESOURCE_REFRESH_TOKEN_ENTRY, 
        FAMILY_REFRESH_TOKEN_ENTRY
    }
}
