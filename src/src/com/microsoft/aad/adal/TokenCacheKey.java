
package com.microsoft.aad.adal;

import java.io.Serializable;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * internal cache key implementation.
 */
final class TokenCacheKey implements Serializable {

    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 8067972995583126404L;

    private String mAuthority;

    private String mResource;

    private String mClientId;

    private String mUniqueId;

    private String mDisplayableId;

    private boolean mIsMultipleResourceRefreshToken;

    private TokenCacheKey() {
        mAuthority = null;
        mResource = null;
        mClientId = null;
    }

    public String toJsonString() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("a", mAuthority);
        obj.put("r", mResource);
        obj.put("c", mClientId);
        obj.put("u", mUniqueId);
        obj.put("d", mDisplayableId);
        obj.put("mr", mIsMultipleResourceRefreshToken);
        return obj.toString();
    }
    
    public static TokenCacheKey fromJsonString(String json) throws JSONException {
        TokenCacheKey key = new TokenCacheKey();
        JSONObject obj = new JSONObject(json);
        key.mAuthority = obj.optString("a", "");
        key.mResource = obj.optString("r", "");
        key.mClientId = obj.optString("c", "");
        key.mUniqueId = obj.optString("u", "");
        key.mDisplayableId = obj.optString("d", "");
        key.mIsMultipleResourceRefreshToken = obj.optBoolean("mr", false);
        return key;
    }

    /**
     * @param authority URL of the authenticating authority
     * @param resource resource identifier
     * @param clientId client identifier
     * @param isMultiResourceRefreshToken true/false for refresh token type
     * @param userId userid provided from {@link UserInfo}
     * @return CacheKey to use in saving token
     */
    public static TokenCacheKey createCacheKey(String authority, String resource, String clientId,
            boolean isMultiResourceRefreshToken, String uniqueId, String displayableId) {

        if (authority == null) {
            throw new IllegalArgumentException("authority");
        }

        if (clientId == null) {
            throw new IllegalArgumentException("clientId");
        }

        TokenCacheKey key = new TokenCacheKey();

        if (!isMultiResourceRefreshToken) {

            if (resource == null) {
                throw new IllegalArgumentException("resource");
            }

            // MultiResource token items will be stored without resource
            key.mResource = resource;
        }

        key.mAuthority = authority.toLowerCase(Locale.US);
        if (key.mAuthority.endsWith("/")) {
            key.mAuthority = (String)key.mAuthority.subSequence(0, key.mAuthority.length() - 1);
        }

        key.mClientId = clientId.toLowerCase(Locale.US);
        key.mIsMultipleResourceRefreshToken = isMultiResourceRefreshToken;

        // optional
        if (!StringExtensions.IsNullOrBlank(uniqueId)) {
            key.mUniqueId = uniqueId.toLowerCase(Locale.US);
        }

        if (!StringExtensions.IsNullOrBlank(displayableId)) {
            key.mDisplayableId = displayableId.toLowerCase(Locale.US);
        }

        return key;
    }

    /**
     * @param item Token item in the cache
     * @return CacheKey to save token
     */
    public static TokenCacheKey createCacheKey(TokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("TokenCacheItem");
        }

        String uniqueId = null;
        String displayableId = null;

        if (item.getUserInfo() != null) {
            uniqueId = item.getUserInfo().getUniqueId();
            displayableId = item.getUserInfo().getDisplayableId();
        }

        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId(),
                item.getIsMultiResourceRefreshToken(), uniqueId, displayableId);
    }

    /**
     * @param item AuthenticationRequest item
     * @return CacheKey to save token
     */
    public static TokenCacheKey createCacheKey(AuthenticationRequest item) {
        if (item == null) {
            throw new IllegalArgumentException("AuthenticationRequest");
        }

        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId(), false,
                item.getUserIdentifier().getUniqueId(), item.getUserIdentifier().getDisplayableId());
    }

    /**
     * Store multi resource refresh tokens with different key. Key will not
     * include resource and set flag to y.
     * 
     * @param item AuthenticationRequest item
     * @param cacheUserId UserId in the cache
     * @return CacheKey to save token
     */
    public static TokenCacheKey createMultiResourceRefreshTokenKey(AuthenticationRequest item,
            String cacheUniqueID, String cacheDispId) {
        if (item == null) {
            throw new IllegalArgumentException("AuthenticationRequest");
        }

        return createCacheKey(item.getAuthority(), item.getResource(), item.getClientId(), true,
                cacheUniqueID, cacheDispId);
    }

    /**
     * Gets Authority.
     * 
     * @return Authority
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * Gets Resource.
     * 
     * @return Resource
     */
    public String getResource() {
        return mResource;
    }

    /**
     * Gets ClientId.
     * 
     * @return ClientId
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Gets UniqueId.
     * 
     * @return UniqueId
     */
    public String getUniqueId() {
        return mUniqueId;
    }

    /**
     * Gets DisplayableId.
     * 
     * @return DisplayableId
     */
    public String getDisplayableId() {
        return mDisplayableId;
    }

    /**
     * Gets status for multi resource refresh token.
     * 
     * @return status for multi resource refresh token
     */
    public boolean getIsMultipleResourceRefreshToken() {
        return mIsMultipleResourceRefreshToken;
    }
}
