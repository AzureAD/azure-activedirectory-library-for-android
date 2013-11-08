
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
        key.mAuthority = authority;
        key.mResource = resource;
        key.mClientId = clientId;
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
     * @param requestItem
     * @return
     */
    public static CacheKey createCacheKey(AuthenticationRequest requestItem) {
        //implementation is another code review...
        return new CacheKey();
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
