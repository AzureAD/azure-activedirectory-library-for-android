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

package com.microsoft.aad.adal.test;

import java.util.Date;
import android.net.Uri;
import com.microsoft.aad.adal.TokenCacheItem;
import com.microsoft.aad.adal.UserInfo;
import android.test.AndroidTestCase;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.BlobContainer;
import junit.framework.Assert;

public class BlobContainerTest extends AndroidTestCase {
    
    protected TokenCacheItem testItemFRT;
    protected TokenCacheItem testItemNoFRT;
    
    protected void setupItems() {
        UserInfo user = new UserInfo("userid1", "givenName", "familyName", "identity", "userid1");
        
        testItemFRT = new TokenCacheItem();
        testItemFRT.setUserInfo(user);
        testItemFRT.setResource("resource");
        testItemFRT.setAuthority("authority");
        testItemFRT.setClientId("clientid");
        testItemFRT.setAccessToken("accessToken");
        testItemFRT.setRefreshToken("refreshToken");
        testItemFRT.setRawIdToken("rawIdToken");
        testItemFRT.setExpiresOn(new Date(10000));
        testItemFRT.setIsMultiResourceRefreshToken(true);
        testItemFRT.setTenantId("tenantId");
        testItemFRT.setFamilyClientId("1");
        
        testItemNoFRT = new TokenCacheItem();
        testItemNoFRT = new TokenCacheItem();
        testItemNoFRT.setUserInfo(user);
        testItemNoFRT.setResource("resource");
        testItemNoFRT.setAuthority("authority");
        testItemNoFRT.setClientId("clientid");
        testItemNoFRT.setAccessToken("accessToken");
        testItemNoFRT.setRefreshToken("refreshToken");
        testItemNoFRT.setRawIdToken("rawIdToken");
        testItemNoFRT.setExpiresOn(new Date(10000));
        testItemNoFRT.setIsMultiResourceRefreshToken(false);
        testItemNoFRT.setTenantId("tenantId");
    }
    
     /**
     * Verify constructor and getters
     * @throws AuthenticationException 
     */    
    public void testcreateCacheKey() throws AuthenticationException {
        setupItems();
        BlobContainer testBlobContainer = new BlobContainer(testItemFRT);
        
        assertEquals(testItemFRT.getRefreshToken(), testBlobContainer.getFamilyRefreshToken());
        
        assertEquals(testItemFRT.getUserInfo().getDisplayableId(),
        		testBlobContainer.getTokenItem().getUserInfo().getDisplayableId());
        
        assertEquals(testItemFRT.getUserInfo().getGivenName(),
        		testBlobContainer.getTokenItem().getUserInfo().getGivenName());
        
        assertEquals(testItemFRT.getUserInfo().getFamilyName(),
        		testBlobContainer.getTokenItem().getUserInfo().getFamilyName());
        
         
        assertEquals(testItemFRT.getUserInfo().getIdentityProvider(),
        		testBlobContainer.getTokenItem().getUserInfo().getIdentityProvider());
        
        Uri uriExp = testItemFRT.getUserInfo().getPasswordChangeUrl();
        Uri uriAct = testBlobContainer.getTokenItem().getUserInfo().getPasswordChangeUrl();
        assertEquals(uriExp != null? uriExp.toString():"null",
        		uriAct != null? uriAct.toString():"null");
        
        Date dateExp = testItemFRT.getUserInfo().getPasswordExpiresOn();
        Date dateAct = testBlobContainer.getTokenItem().getUserInfo().getPasswordExpiresOn();
        assertEquals((dateExp != null? dateExp.toString():"null"),
        		(dateAct != null? dateAct.toString():"null"));        
        
        assertEquals(testItemFRT.getResource(),testBlobContainer.getTokenItem().getResource());
        assertEquals(testItemFRT.getAuthority(),testBlobContainer.getTokenItem().getAuthority());
        assertEquals(testItemFRT.getClientId(),testBlobContainer.getTokenItem().getClientId());
        assertEquals(testItemFRT.getAccessToken(),testBlobContainer.getTokenItem().getAccessToken());
        
        assertEquals(testItemFRT.getRefreshToken(),testBlobContainer.getTokenItem().getRefreshToken());
        assertEquals(testItemFRT.getRawIdToken(),testBlobContainer.getTokenItem().getRawIdToken());
        assertEquals(testItemFRT.getIsMultiResourceRefreshToken(),testBlobContainer.getTokenItem().getIsMultiResourceRefreshToken());
        assertEquals(testItemFRT.getTenantId(),testBlobContainer.getTokenItem().getTenantId());
        assertEquals(testItemFRT.getFamilyClientId(),testBlobContainer.getTokenItem().getFamilyClientId());

        Date tokenDateExp = testItemFRT.getExpiresOn();
        Date tokenDateAct = testBlobContainer.getTokenItem().getExpiresOn();
        assertEquals((tokenDateExp != null? tokenDateExp.toString():"null"),
        		(tokenDateAct != null? tokenDateAct.toString():"null"));
    }
    
    public void testcreateCacheKey_InvalidTokenCacheItem() {
    	TokenCacheItem tokenCacheItem = null;
        try {
            BlobContainer testBlobContainer = new BlobContainer(tokenCacheItem);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }
    }
    
    public void testcreateCacheKey_NullItem() {        
        try {
            BlobContainer testBlobContainer = new BlobContainer((TokenCacheItem)null);    
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }
    }
    
    public void testGetFamilyRefreshToken_validFRT() throws AuthenticationException {
        setupItems();
        BlobContainer testBlobContainer = new BlobContainer(testItemFRT);
        try {
            assertTrue(testBlobContainer.getTokenItem().getFamilyClientId().equalsIgnoreCase("1"));
            assertTrue(testBlobContainer.getFamilyRefreshToken()!=null);
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof AuthenticationException);
            Assert.fail("not expected");
        }        
    }
    
    public void testGetFamilyRefreshToken_noFRT() throws AuthenticationException {
        setupItems();        
        testItemFRT.setFamilyClientId("");
        try {
        	BlobContainer testBlobContainer = new BlobContainer(testItemFRT);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof AuthenticationException);
        }    
    }
}
