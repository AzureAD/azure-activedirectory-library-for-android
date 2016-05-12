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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

class SSOStateContainer {
    /**
     * the version number of {@link SSOStateContainer}
     */
    @SerializedName("version")
    private final int version = 1;

    /**
     * the {@link SSOStateContainer} stores the FRT tokenCacheItem of the given
     * user
     */
    @SerializedName("tokenCacheItems")
    private final List<SerializedTokenCacheItem> tokenCacheItems = new ArrayList<SerializedTokenCacheItem>();

    /**
     * Check if the cache item contains family refresh token if true, value the
     * tokenItem with this cache item
     * 
     * @param item
     */
    SSOStateContainer(final TokenCacheItem tokenItem) {
        // only FRT is stored here
        if (tokenItem == null) {
            throw new IllegalArgumentException("tokenItem is null");
        }

        if (StringExtensions.IsNullOrBlank(tokenItem.getFamilyClientId())) {
            throw new IllegalArgumentException("tokenItem does not contain family refresh token");
        }

        final SerializedTokenCacheItem serializedTokenCacheItem = new SerializedTokenCacheItem(tokenItem);
        this.tokenCacheItems.add(serializedTokenCacheItem);
    }

    /**
     * return the token cache item in this blob container object
     * 
     * @return tokenCacheItem
     * @throws AuthenticationException
     */
    private TokenCacheItem getTokenItem() throws AuthenticationException {
        if (tokenCacheItems == null || tokenCacheItems.isEmpty()) {
            throw new AuthenticationException(ADALError.FAMILY_REFRESH_TOKEN_NOT_FOUND,
                    "There is no family refresh token cache item in the SSOStateContainer.");
        }
        return tokenCacheItems.get(0).toTokenCacheItem();
    }

    String serialize() {
        final Gson gson = new Gson();
        return gson.toJson(this);
    }

    static TokenCacheItem deserialize(String serializedBlob) throws AuthenticationException {
        final Gson gson = new Gson();
        try {
            return gson.fromJson(serializedBlob, SSOStateContainer.class).getTokenItem();
        } catch (final JsonParseException exception) {
            throw new DeserializationAuthenticationException(exception.getMessage());
        }
    }
}
