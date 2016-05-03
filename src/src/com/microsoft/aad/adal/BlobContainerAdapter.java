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
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class BlobContainerAdapter implements JsonDeserializer<BlobContainer>, JsonSerializer<BlobContainer> {
    
    private static final String TAG = "BlobContainerAdapter";    
    
    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateTimeAdapter())
            .create();
    
    public BlobContainerAdapter(){        
    }
    
    @Override
    public synchronized JsonElement serialize(BlobContainer blobContainer, Type typeOfSrc, JsonSerializationContext arg2) {
        
        long versionUID = BlobContainer.getSerialversionuid();        
        String serialCacheToke = mGson.toJson(blobContainer.getTokenItem(), TokenCacheItem.class);
        JsonObject jsonObj = new JsonObject();
        JsonPrimitive versionUIDJson = new JsonPrimitive(versionUID);
        JsonPrimitive cacheItemJson = new JsonPrimitive(serialCacheToke);        
        jsonObj.add("SerialVersionUID",versionUIDJson);
        jsonObj.add("TokenCacheItem",cacheItemJson);        
        return jsonObj;
    }
    
    @Override
    public synchronized BlobContainer deserialize(JsonElement json, Type typeOfTarget, JsonDeserializationContext context) {
        JsonObject srcJsonObj = json.getAsJsonObject();
        
        if(BlobContainer.getSerialversionuid() == srcJsonObj.get("SerialVersionUID").getAsLong()) {
            String cacheItemJson = srcJsonObj.get("TokenCacheItem").getAsString();
            
            try {
                TokenCacheItem tokenCacheItem = mGson.fromJson(cacheItemJson, TokenCacheItem.class);                
                return new BlobContainer(tokenCacheItem);
            } catch (final Exception e) {
                Logger.e(TAG, "The input json string cannot be deserialized to an BlobContainer object." + e.getMessage()
                        , "", ADALError.FAIL_TO_DESERIALIZE);
                throw new JsonParseException("Cannot parse the blob: " + json.getAsString()); 
            }        
        }
        
        Logger.e(TAG, "cannot deserialze because the serial version UID does not match. "
                + "Deserialzing target UID is:" + BlobContainer.getSerialversionuid(), 
                "", ADALError.FAIL_TO_DESERIALIZE);
        throw new JsonParseException("Cannot parse the blob, because the serial version UID does not match.");
    }
}
