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

import java.util.Date;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class BlobContainer {

    /**
     * universal version identifier for BlobContainer class
     * @SerializedName("SerialVersionUID")
     */
    private static final long serialVersionUID = 20160501L;
    
    private static final String TAG = "BlobContainer";

    /**
     * the BlobContainer stores the FRT tokenCacheItem 
     * of the given user
     * @SerializedName("TokenCacheItem")
     */ 
    private TokenCacheItem tokenItem;
    
    /**
     * Check if the cache item contains family refresh token
     * if true, value the tokenItem with this cache item
     * @param item
     */
    BlobContainer(TokenCacheItem tokenItem) {
        //only FRT is stored here 
        if (tokenItem == null) {
            Logger.e(TAG, 
                    "Invalid constructor input, the input should be a TokenCacheItem object.", 
                    "", 
                    ADALError.FAMILY_REFRESH_TOKEN_NOT_FOUND);
            throw new IllegalArgumentException("invalid input TokenCacheItem");
        }
        
        
        if (StringExtensions.IsNullOrBlank(tokenItem.getFamilyClientId())) {
            Logger.e(TAG, 
                    "Invalid constructor input, the input TokenCacheItem should contains family refresh token.", 
                    "", 
                    ADALError.FAMILY_REFRESH_TOKEN_NOT_FOUND);
            throw new IllegalArgumentException("invalid input TokenCacheItem");
        }
        
        this.tokenItem = tokenItem;        
    }
    
    /**
     * return the token cache item in this blob container object
     * @return tokenCacheItem
     */
    public TokenCacheItem getTokenItem() {
        return tokenItem;
    }
    
    /**
     * return the values of variables in the tokenItem
     */
    @Override
    public String toString() {        
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateTimeAdapter())
                .create();
         return gson.toJson(this).toString();
    }

    /**
     * method to get the serialVersionUID of BlobContainer
     * @return serialVersionUID
     */
    protected static long getSerialversionuid() {
        return serialVersionUID;
    }
}
