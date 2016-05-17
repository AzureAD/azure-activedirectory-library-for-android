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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

/**
 * SSOStateSerializer class is used to hide the details of the serialization and
 * deserialization details. It provides a serializer to serialize the
 * TokenCacheItem and a deserializer to return the TokenCacheItem.
 */
class SSOStateSerializer {
    /**
     * The version number of {@link SSOStateSerializer }
     */
    @SerializedName("version")
    private final static int version = 1;

    /**
     * The {@link SSOStateSerializer } stores the FRT tokenCacheItem of the
     * given user.
     */
    @SerializedName("tokenCacheItems")
    private final List<TokenCacheItem> mTokenCacheItems = new ArrayList<TokenCacheItem>();

    /**
     * To customize the serialize/deserialize process and provide a more
     * lightweight TokenCacheItem, FamilyTokenCacheItemAdapter is used here to
     * register custom serializer.
     */
    private static Gson mGson = new GsonBuilder()
            .registerTypeAdapter(TokenCacheItem.class, new FamilyTokenCacheItemAdapter())
            .create();

    static int getVersion() {
        return version;
    }
    /**
     * A private constructor to with an input item in type TokenCacheItem.
     * 
     * @param item
     */
    private SSOStateSerializer(final TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("tokenItem is null");
        }
        this.mTokenCacheItems.add(item);
    }

    /**
     * Return the token cache item in this blob container object.
     * 
     * @return tokenCacheItem
     * @throws AuthenticationException
     */
    private TokenCacheItem getTokenItem() throws AuthenticationException {
        if (mTokenCacheItems == null || mTokenCacheItems.isEmpty()) {
            throw new AuthenticationException(ADALError.TOKEN_CACHE_ITEM_NOT_FOUND,
                    "There is no token cache item in the SSOStateContainer.");
        }
        return mTokenCacheItems.get(0);
    }

    /**
     * Serialize the TokenCacheItem
     * 
     * To hide the details of the serialization, we take TokenCacheItem as input
     * and call the constructor to initialize a SSOStateSerializer object. The
     * type of the TokenCacheItem will be checked in the caller method. This is
     * a static method because we want to hide the details in the
     * SSOStateSerializer on the serialization.
     * 
     * @return
     */
    static String serialize(final TokenCacheItem item) {
        SSOStateSerializer serializerObject = new SSOStateSerializer(item);
        return mGson.toJson(serializerObject);
    }

    /**
     * Deserialize the serializedBlob
     * 
     * In order to provide symmetry, we have serialize function take a
     * TokenCacheItem as input and a deserialize return it. This is a static
     * method because we want to hide the details in the SSOStateSerializer on
     * the deserialization.
     * 
     * @param String
     * @return TokenCacheItem
     * @throws AuthenticationException
     */
    static TokenCacheItem deserialize(String serializedBlob) throws AuthenticationException {
        try {
            final JsonObject srcJsonObj = new JsonPrimitive(serializedBlob).getAsJsonObject();
            final JsonElement blobVersion = srcJsonObj.get("version");
            if (blobVersion != null && blobVersion.getAsInt() == SSOStateSerializer.getVersion()) {
                return mGson.fromJson(serializedBlob, SSOStateSerializer.class).getTokenItem();
            } else {
                throw new DeserializationAuthenticationException(
                        "Fail to deserialize because the blob version is incompatible. The version of the serializedBlob is "
                                + blobVersion.getAsInt() + "And the target class version is "
                                + SSOStateSerializer.getVersion());
            }
        } catch (final JsonParseException exception) {
            throw new DeserializationAuthenticationException(exception.getMessage());
        }
    }
}
