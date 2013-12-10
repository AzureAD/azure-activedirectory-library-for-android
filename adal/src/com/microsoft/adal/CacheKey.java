
package com.microsoft.adal;

import java.io.Serializable;
import java.util.Locale;

/**
 * CacheKey will be the object for key
 * 
 * @author omercan
 */
public class CacheKey implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8067972995583126404L;

    private String mAuthority;

    private String mResource;

    private String mClientId;

    private String mUserId;

    private boolean mIsMultipleResourceRefreshToken;

    private CacheKey() {
        mAuthority = null;
        mResource = null;
        mClientId = null;
    }

    public static CacheKey createCacheKey(String authority, String resource, String clientId,
            boolean isMultiResourceRefreshToken, String userId) {

        CacheKey key = new CacheKey();
        if (authority == null) {
            throw new IllegalArgumentException("authority");
        }

        if (clientId == null) {
            throw new IllegalArgumentException("clientId");
        }

        if (!isMultiResourceRefreshToken) {

            if (resource == null) {
                throw new IllegalArgumentException("resource");
            }

            // MultiResource token items will be stored without resource
            key.mResource = resource.toLowerCase(Locale.US);
        }

        key.mAuthority = authority.toLowerCase(Locale.US);
        key.mClientId = clientId.toLowerCase(Locale.US);
        key.mIsMultipleResourceRefreshToken = isMultiResourceRefreshToken;

        // optional
        if (!StringExtensions.IsNullOrBlank(userId)) {
            key.mUserId = userId.toLowerCase(Locale.US);
        }

        return key;
    }

    public static CacheKey createCacheKey(TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("TokenCacheItem");
        }

        String userid = null;

        if (item.getUserInfo() != null) {
            userid = item.getUserInfo().getUserId();
        }

        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId(),
                item.getIsMultiResourceRefreshToken(), userid);
    }

    /**
     * get cache key for query. ADAL checks cache first without multiresource
     * and then multiresource items.
     * 
     * @param requestItem
     * @return
     */
    static CacheKey createCacheKey(AuthenticationRequest item, boolean isMultiResource) {
        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId(),
                isMultiResource, item.getLoginHint());
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

    public void setUserId(String mUserId) {
        this.mUserId = mUserId;
    }

    public boolean getIsMultipleResourceRefreshToken() {
        return mIsMultipleResourceRefreshToken;
    }

    public void setIsMultipleResourceRefreshToken(boolean mIsMultipleResourceRefreshToken) {
        this.mIsMultipleResourceRefreshToken = mIsMultipleResourceRefreshToken;
    }
}
