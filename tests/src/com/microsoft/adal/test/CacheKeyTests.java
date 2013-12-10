
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
        CacheKey testKey = CacheKey.createCacheKey("Authority", "Resource", "ClientId", false, null);
        assertEquals("lowercase authority is expected", "authority", testKey.getAuthority());
        assertEquals("lowercase resource is expected", "resource", testKey.getResource());
        assertEquals("lowercase clientid is expected", "clientid", testKey.getClientId());
        assertNull("userid is null", testKey.getUserId());
        assertFalse("Not multi resource", testKey.getIsMultipleResourceRefreshToken());
        
        // key itself contains at least authority
        assertTrue(testKey.toString().contains("authority"));
        
        CacheKey testKeyMultiResource = CacheKey.createCacheKey("Authority123", "Resource123", "ClientId123", true, null);
        assertEquals("lowercase authority is expected", "authority123", testKeyMultiResource.getAuthority());
        assertNull("MultiResource key does not keep resource", testKeyMultiResource.getResource());
        assertEquals("lowercase clientid is expected", "clientid123", testKeyMultiResource.getClientId());
        assertNull("userid is null", testKeyMultiResource.getUserId());
        assertTrue("multi resource", testKeyMultiResource.getIsMultipleResourceRefreshToken());
        
    }

    /**
     * empty values does not fail
     */
    public void testcreateCacheKeyEmptyValues() {
        CacheKey testKey = CacheKey.createCacheKey("", "", "", false, "");
        assertEquals("", testKey.getAuthority());
        assertEquals("", testKey.getResource());
        assertEquals("", testKey.getClientId());

        // key itself contains at least authority
        assertFalse(testKey.toString().contains("authority"));
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
