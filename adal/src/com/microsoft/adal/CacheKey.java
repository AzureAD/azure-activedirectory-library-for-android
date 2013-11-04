
package com.microsoft.adal;

public class CacheKey {

    private String mAuthority;

    private String mResource;

    private String mClientId;

    private String mUserId;

    public static CacheKey createCacheKey(String authority, String resource, String clientId,
            String userId) {      
        
        CacheKey key = new CacheKey();
        key.mAuthority = authority;
        key.mResource = resource;
        key.mClientId = clientId;
        key.mUserId = userId;
        return key;
    }

    public static CacheKey createCacheKey(TokenCacheItem item) {
        String userId = null;

        if (item.getUserInfo() != null) {
            userId = item.getUserInfo().getUserId();
        }

        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId(), userId);
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

    public String getUserId() {
        return mUserId;
    }

    @Override
    public String toString() {
        return String.format("%s|$|%s|$|%s", getAuthority(), getResource(), getClientId());
    }
}
