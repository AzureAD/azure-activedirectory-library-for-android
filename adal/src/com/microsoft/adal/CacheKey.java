
package com.microsoft.adal;

public class CacheKey {

    public static CacheKey createCacheKey(String authority, String resource,
            String clientId, String userId) {
        throw new UnsupportedOperationException("come back later");
    }

    public static CacheKey createCacheKey(TokenCacheItem item) {
        throw new UnsupportedOperationException("come back later");
    }

    public String getAuthority() {
        throw new UnsupportedOperationException("come back later");
    }

    public String getResource() {
        throw new UnsupportedOperationException("come back later");
    }

    public String getClientId() {
        throw new UnsupportedOperationException("come back later");
    }

    public String getUserId() {
        throw new UnsupportedOperationException("come back later");
    }
}
