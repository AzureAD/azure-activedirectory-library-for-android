// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal.test;

import junit.framework.Assert;
import android.test.AndroidTestCase;

import com.microsoft.aad.adal.CacheKey;
import com.microsoft.aad.adal.TokenCacheItem;

public class CacheKeyTests extends AndroidTestCase {

    /**
     * Verify constructor and getters
     */
    public void testcreateCacheKey() {
        String testKey = CacheKey.createCacheKey("Authority", "Resource", "ClientId", false, null);
        assertEquals("expected key", "authority$Resource$clientid$n$null", testKey);

        String testKeyMultiResource = CacheKey.createCacheKey("Authority123", "Resource123",
                "ClientId123", true, null);
        assertEquals("expected key", "authority123$null$clientid123$y$null", testKeyMultiResource);

        String testKeyMultiResourceWithUser = CacheKey.createCacheKey("Authority123",
                "Resource123", "ClientId123", true, "user123");
        assertEquals("expected key", "authority123$null$clientid123$y$user123",
                testKeyMultiResourceWithUser);

        String testKeyWithUser = CacheKey.createCacheKey("Authority123", "Resource123",
                "ClientId123", false, "user123");
        assertEquals("expected key", "authority123$Resource123$clientid123$n$user123",
                testKeyWithUser);

        String testKeySlash = CacheKey.createCacheKey("Authority123EndsSlash/", "Resource123",
                "ClientId123", true, "user123");
        assertEquals("expected key", "authority123endsslash$null$clientid123$y$user123",
                testKeySlash);

        testKeySlash = CacheKey.createCacheKey("Authority123EndsSlash/", "Resource123",
                "ClientId123", false, "user123");
        assertEquals("expected key", "authority123endsslash$Resource123$clientid123$n$user123",
                testKeySlash);
    }

    /**
     * empty values does not fail
     */
    public void testcreateCacheKey_EmptyValues() {
        String testKey = CacheKey.createCacheKey("", "", "", false, "");
        assertEquals("expected key", "$$$n$null", testKey);

        String testKeyNullUser = CacheKey.createCacheKey("", "", "", false, null);
        assertEquals("expected key", "$$$n$null", testKeyNullUser);

        String testKeyWithUser = CacheKey.createCacheKey("", "", "", false, "userid");
        assertEquals("expected key", "$$$n$userid", testKeyWithUser);
    }

    public void testcreateCacheKey_NullItem() {

        try {
            CacheKey.createCacheKey((TokenCacheItem)null);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }
    }

    public void testcreateCacheKey_NullArgument() {

        try {
            CacheKey.createCacheKey(null, null, null, false, null);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }
    }

    public void testcreateCacheKey_NullResource() {

        try {
            CacheKey.createCacheKey("https://authority", null, "clientid", false, null);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
            assertEquals("contains resource", "resource", ((IllegalArgumentException)exc).getMessage());
        }
    }
    
    public void testcreateCacheKey_NullClientid() {

        try {
            CacheKey.createCacheKey("https://authority", "resource", null, false, null);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
            assertEquals("contains resource", "clientId", ((IllegalArgumentException)exc).getMessage());
        }
    }
}
