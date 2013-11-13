
package com.microsoft.adal;

public class CacheKey {

    private String mAuthority;

    private String mResource;

    private String mClientId;

    private CacheKey() {
        mAuthority = null;
        mResource = null;
        mClientId = null;
    }

    public static CacheKey createCacheKey(String authority, String resource, String clientId) {

        CacheKey key = new CacheKey();
        if (authority == null) {
            throw new IllegalArgumentException("authority");
        }
        
        if (resource == null) {
            throw new IllegalArgumentException("resource");
        }
        
        if (clientId == null) {
            throw new IllegalArgumentException("clientId");
        }
        
        key.mAuthority = authority.toLowerCase();
        key.mResource = resource.toLowerCase();
        key.mClientId = clientId.toLowerCase();
        return key;
    }

    public static CacheKey createCacheKey(TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("TokenCacheItem");
        }

        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId());
    }

    /**
     * get cache key
     * 
     * @param requestItem
     * @return
     */
    static CacheKey createCacheKey(AuthenticationRequest item) {
        // implementation is another code review...
        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId());
    }

    public String getAuthority() {
        return mAuthority;
    }

    public String getResource() {
        return mResource;
    }

    public String getClientId() {
        return mClientId;
    }

    @Override
    public String toString() {
        return String.format("%s|$|%s|$|%s", getAuthority(), getResource(), getClientId());
    }
}
