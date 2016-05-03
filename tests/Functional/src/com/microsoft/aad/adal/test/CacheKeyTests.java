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

import com.microsoft.aad.adal.CacheKey;
import com.microsoft.aad.adal.TokenCacheItem;

import android.test.AndroidTestCase;
import junit.framework.Assert;

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
            fail("Expect exceptions");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
            assertEquals("contains clientId", "clientId", ((IllegalArgumentException)exc).getMessage());
        }
    }
}
