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

class BlobContainer {

    /**
     * universal version identifier for BlobContainer class
     */
    private static final long serialVersionUID = 5722517776407773544L;

    /**
     * the BlobContainer stores the FRT tokenCacheItem 
     * of the given user
     */
    private TokenCacheItem tokenItem;
    
    /**
     * Construct default blob container.
     */
    BlobContainer() {
        tokenItem = new TokenCacheItem();
    }
    
    /**
     * Check if the cache item contains family refresh token
     * if true, value the tokenItem with this cache item
     * @param item
     * @throws AuthenticationException 
     */
    BlobContainer(TokenCacheItem tokenItem) throws AuthenticationException {
        //only FRT is stored here 
        if(tokenItem == null) {
            throw new IllegalArgumentException("invalid input TokenCacheItem");
        }
        
        if (StringExtensions.IsNullOrBlank(tokenItem.getFamilyClientId())) {
            throw new AuthenticationException(
                    ADALError.FAMILY_REFRESH_TOKEN_NOT_FOUND, 
                    "Invalid constructor input, the input TokenCacheItem should contains family refresh token."
                    );
        } else {
            this.tokenItem = tokenItem;
        }
    }
    
    public TokenCacheItem getTokenItem() {
        return tokenItem;
    }

    public void setTokenItem(TokenCacheItem tokenItem) throws AuthenticationException {
    	if(tokenItem != null
    			&& !StringExtensions.IsNullOrBlank(tokenItem.getFamilyClientId())) {
    		this.tokenItem = tokenItem;
    	} else {
    		throw new AuthenticationException(
                    ADALError.FAMILY_REFRESH_TOKEN_NOT_FOUND, 
                    "Invalid constructor input, the input TokenCacheItem should contains family refresh token."
                    );
    	}
    }    
    
    /**
     * Get the family refresh token from the tokenItem
     * @return family refresh token
     * @throws AuthenticationException 
     */
    public String getFamilyRefreshToken() throws AuthenticationException {
        if(tokenItem!=null){
        	return tokenItem.getRefreshToken();
        } else {
        	throw new AuthenticationException(
                    ADALError.FAMILY_REFRESH_TOKEN_NOT_FOUND, 
                    "No family refresh token is found from the cache item"
                    );
        }
    }
    
    /**
     * return the values of variables in the tokenItem
     */
    @Override
    public String toString() {
        return "BlobContainer { "
                + "tokenItem { UserInfo = " + (tokenItem.getUserInfo()).toString() + ","
                + "Resource = " + tokenItem.getResource() + ","
                + "Authority = " + tokenItem.getAuthority() + ","
                + "ClientId = " + tokenItem.getClientId() + ","
                + "AccessToken = " + tokenItem.getAccessToken() + ","
                + "RefreshToken = " + tokenItem.getRefreshToken() + ","
                + "RawIdToken = " + tokenItem.getRawIdToken() + ","
                + "ExpiresOn = " + (tokenItem.getExpiresOn() != null? tokenItem.getExpiresOn().toString(): "null") + ","
                + "IsMultiResourceRefreshToken = " + (tokenItem.getIsMultiResourceRefreshToken() ? "y" : "n") + ","
                + "TenantId = " + tokenItem.getTenantId() + ","
                + "FamilyClientId = " + tokenItem.getFamilyClientId()
                + " } }";
    }
}
