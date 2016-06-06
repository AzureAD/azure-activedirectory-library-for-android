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
import com.microsoft.aad.adal.AuthenticationConstants.OAuth2;

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
        jsonObj.add(OAuth2.AUTHORITY, new JsonPrimitive(tokenCacheItem.getAuthority()));
        jsonObj.add(OAuth2.REFRESH_TOKEN, new JsonPrimitive(tokenCacheItem.getRefreshToken()));
        jsonObj.add(OAuth2.ID_TOKEN, new JsonPrimitive(tokenCacheItem.getRawIdToken()));
        jsonObj.add(OAuth2.ADAL_CLIENT_FAMILY_ID, new JsonPrimitive(tokenCacheItem.getFamilyClientId()));
        return jsonObj;
    }

    @Override
    public TokenCacheItem deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject srcJsonObj = json.getAsJsonObject();
        throwIfParameterMissing(srcJsonObj, OAuth2.AUTHORITY);
        throwIfParameterMissing(srcJsonObj, OAuth2.ID_TOKEN);
        throwIfParameterMissing(srcJsonObj, OAuth2.ADAL_CLIENT_FAMILY_ID);
        throwIfParameterMissing(srcJsonObj, OAuth2.REFRESH_TOKEN);

        final String rawIdToken = srcJsonObj.get(OAuth2.ID_TOKEN).getAsString();
        final TokenCacheItem tokenCacheItem = new TokenCacheItem();
        final IdToken idToken;
        try {
            idToken = new IdToken(rawIdToken);
        } catch (AuthenticationException e) {
            throw new JsonParseException(TAG + ": Could not deserialize into a tokenCacheItem object", e);
        }
        final UserInfo userInfo = new UserInfo(idToken);
        tokenCacheItem.setUserInfo(userInfo);
        tokenCacheItem.setTenantId(idToken.getTenantId());
        tokenCacheItem.setAuthority(srcJsonObj.get(OAuth2.AUTHORITY).getAsString());
        tokenCacheItem.setIsMultiResourceRefreshToken(true);
        tokenCacheItem.setRawIdToken(rawIdToken);
        tokenCacheItem.setFamilyClientId(srcJsonObj.get(OAuth2.ADAL_CLIENT_FAMILY_ID).getAsString());
        tokenCacheItem.setRefreshToken(srcJsonObj.get(OAuth2.REFRESH_TOKEN).getAsString());
        return tokenCacheItem;
    }

    private void throwIfParameterMissing(JsonObject json, String name) {
        if (!json.has(name)) {
            throw new JsonParseException(TAG + "Attribute " + name + " is missing for deserialization.");
        }
    }
}
