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

import android.test.AndroidTestCase;
import junit.framework.Assert;

public class CacheKeyTests extends AndroidTestCase {

    /**
     * Verify constructor and getters
     */
    public void testcreateCacheKey() {
        String testKey = CacheKey.createCacheKey("Authority", "Resource", "ClientId", false, null, null);
        assertEquals("expected key", "authority$Resource$clientid$n$null", testKey);

        String testKeyMultiResource = CacheKey.createCacheKey("Authority123", "Resource123",
                "ClientId123", true, null, null);
        assertEquals("expected key", "authority123$null$clientid123$y$null", testKeyMultiResource);

        String testKeyMultiResourceWithUser = CacheKey.createCacheKey("Authority123",
                "Resource123", "ClientId123", true, "user123", null);
        assertEquals("expected key", "authority123$null$clientid123$y$user123",
                testKeyMultiResourceWithUser);

        String testKeyWithUser = CacheKey.createCacheKey("Authority123", "Resource123",
                "ClientId123", false, "user123", null);
        assertEquals("expected key", "authority123$Resource123$clientid123$n$user123",
                testKeyWithUser);

        String testKeySlash = CacheKey.createCacheKey("Authority123EndsSlash/", "Resource123",
                "ClientId123", true, "user123", null);
        assertEquals("expected key", "authority123endsslash$null$clientid123$y$user123",
                testKeySlash);

        testKeySlash = CacheKey.createCacheKey("Authority123EndsSlash/", "Resource123",
                "ClientId123", false, "user123", null);
        assertEquals("expected key", "authority123endsslash$Resource123$clientid123$n$user123",
                testKeySlash);
        
        final String testKeyWithFamilyCientId = CacheKey.createCacheKey("authority", null, null, true, "user123", "family123");
        assertEquals("authority$null$null$y$user123$foci-family123", testKeyWithFamilyCientId);
    }

    /**
     * empty values does not fail
     */
    public void testcreateCacheKeyEmptyValues() {
        String testKey = CacheKey.createCacheKey("", "", "", false, "", "");
        assertEquals("expected key", "$$$n$null$foci-", testKey);

        String testKeyNullUser = CacheKey.createCacheKey("", "", "", false, null, "");
        assertEquals("expected key", "$$$n$null$foci-", testKeyNullUser);

        String testKeyWithUser = CacheKey.createCacheKey("", "", "", false, "userid", "");
        assertEquals("expected key", "$$$n$userid$foci-", testKeyWithUser);
    }

    public void testcreateCacheKeyNullItem() {

        try {
            CacheKey.createCacheKey((TokenCacheItem) null);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }
    }

    public void testcreateCacheKeyNullArgument() {

        try {
            CacheKey.createCacheKey(null, null, null, false, null, null);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }
    }

    public void testcreateCacheKeyNullResource() {

        // Test resource is null and the cache key is not for MRRT
        try {
            CacheKey.createCacheKey("https://authority", null, "clientid", false, null, null);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
            assertEquals("contains resource", "resource",
                    ((IllegalArgumentException) exc).getMessage());
        }
        
        // Test resource is null but the cache key is for MRRT. 
        try {
            final String cacheKey = CacheKey.createCacheKey("authority", null, "clientid", true, "user123", null);
            assertEquals("authority$null$clientid$y$user123", cacheKey);
        } catch (final Exception e) {
            fail("Non expected exceptions");
        }
    }
    
    public void testcreateCacheKeyNullClientid() {

        // Test both client id and family client id is null
        try {
            CacheKey.createCacheKey("https://authority", "resource", null, false, null, null);
            fail("Expect exceptions");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
            assertEquals("contains clientId", "both clientId and familyClientId are null",
                    ((IllegalArgumentException) exc).getMessage());
        }
        
        // Test client id is null but family client id is not null
        try {
            final String key = CacheKey.createCacheKey("authority", null, null, true, "user123", "family123");
            assertEquals("authority$null$null$y$user123$foci-family123", key);
        } catch (final Exception exception) {
            fail("Non-expected exception");
        }
    }
}
