
package com.microsoft.adal.test;

import junit.framework.Assert;

import com.microsoft.adal.CacheKey;
import com.microsoft.adal.TokenCacheItem;

import android.test.AndroidTestCase;

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
        
        String testKeySlash = CacheKey.createCacheKey("Authority123EndsSlash/",
                "Resource123", "ClientId123", true, "user123");
        assertEquals("expected key", "authority123endsslash$null$clientid123$y$user123",
                testKeySlash);
        
        testKeySlash = CacheKey.createCacheKey("Authority123EndsSlash/",
                "Resource123", "ClientId123", false, "user123");
        assertEquals("expected key", "authority123endsslash$Resource123$clientid123$n$user123",
                testKeySlash);
    }

    /**
     * empty values does not fail
     */
    public void testcreateCacheKeyEmptyValues() {
        String testKey = CacheKey.createCacheKey("", "", "", false, "");
        assertEquals("expected key", "$$$n$null", testKey);
        
        String testKeyNullUser = CacheKey.createCacheKey("", "", "", false, null);
        assertEquals("expected key", "$$$n$null", testKeyNullUser);

        String testKeyWithUser = CacheKey.createCacheKey("", "", "", false, "userid");
        assertEquals("expected key", "$$$n$userid", testKeyWithUser);
    }

    public void testcreateCacheKeyNullItem() {

        try {
            CacheKey.createCacheKey((TokenCacheItem)null);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }
    }

    public void testcreateCacheKeyNullArgument() {

        try {
            CacheKey.createCacheKey(null, null, null, false, null);
            Assert.fail("not expected");
        } catch (Exception exc) {
            assertTrue("argument exception", exc instanceof IllegalArgumentException);
        }
    }
}
