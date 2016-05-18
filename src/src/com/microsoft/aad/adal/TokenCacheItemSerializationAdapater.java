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

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * This TokenCacheItemAdapter class is a customized serializer for the family
 * token cache item into GSON where we are trying to keep a lightweight form of
 * tokenCacheItem by parsing the raw idToken, we can get all the claims in it,
 * including userInfo, and tenantId.
 */
public final class TokenCacheItemSerializationAdapater
        implements JsonDeserializer<TokenCacheItem>, JsonSerializer<TokenCacheItem> {

    private static final String TAG = TokenCacheItemSerializationAdapater.class.getSimpleName();

    @Override
    public JsonElement serialize(TokenCacheItem tokenCacheItem, Type type, JsonSerializationContext context) {
        JsonObject jsonObj = new JsonObject();
        jsonObj.add("authority", new JsonPrimitive(tokenCacheItem.getAuthority()));
        jsonObj.add("refreshToken", new JsonPrimitive(tokenCacheItem.getRefreshToken()));
        jsonObj.add("idToken", new JsonPrimitive(tokenCacheItem.getRawIdToken()));
        jsonObj.add("familyClientId", new JsonPrimitive(tokenCacheItem.getFamilyClientId()));
        return jsonObj;
    }

    @Override
    public TokenCacheItem deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject srcJsonObj = json.getAsJsonObject();
        String rawIdToken = srcJsonObj.get("idToken").getAsString();
        TokenCacheItem tokenCacheItem = new TokenCacheItem();
        try {
            IdToken idToken = new IdToken(rawIdToken);
            UserInfo userInfo = new UserInfo(idToken);
            tokenCacheItem.setUserInfo(userInfo);
            tokenCacheItem.setTenantId(idToken.getTenantId());
        } catch (AuthenticationException e) {
            throw new JsonParseException(TAG + ": Could not deserialize into a tokenCacheItem object", e);
        }
        tokenCacheItem.setAuthority(srcJsonObj.get("authority").getAsString());
        tokenCacheItem.setIsMultiResourceRefreshToken(true);
        tokenCacheItem.setRawIdToken(rawIdToken);
        tokenCacheItem.setFamilyClientId(srcJsonObj.get("familyClientId").getAsString());
        tokenCacheItem.setRefreshToken(srcJsonObj.get("refreshToken").getAsString());
        return tokenCacheItem;
    }
}
