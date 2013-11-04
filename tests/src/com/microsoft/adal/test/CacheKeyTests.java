
package com.microsoft.adal.test;

import com.microsoft.adal.CacheKey;

import android.test.AndroidTestCase;

public class CacheKeyTests extends AndroidTestCase {

    /**
     * Verify constructor and getters
     */
    public void testcreateCacheKey() {
        CacheKey testKey = CacheKey.createCacheKey("authority", "resource", "clientId", "userId");
        assertEquals("Same authority is expected", "authority", testKey.getAuthority());
        assertEquals("Same resource is expected", "resource", testKey.getResource());
        assertEquals("Same clientid is expected", "clientId", testKey.getClientId());
        assertEquals("Same userId is expected", testKey.getUserId());

        // key itself contains at least authority
        assertTrue(testKey.toString().contains("authority"));
    }

    /**
     * null values does not fail
     */
    public void testcreateCacheKeyNullValues() {
        CacheKey testKey = CacheKey.createCacheKey(null, null, null, null);
        assertEquals(null, testKey.getAuthority());
        assertEquals(null, testKey.getResource());
        assertEquals(null, testKey.getClientId());
        assertEquals(null, testKey.getUserId());

        // key itself contains at least authority
        assertFalse(testKey.toString().contains("authority"));
    }
}
