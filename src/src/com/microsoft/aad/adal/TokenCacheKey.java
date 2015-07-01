package com.microsoft.aad.adal;

import java.io.Serializable;
import java.util.Locale;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

/**
 * internal cache key implementation.
 */
final class TokenCacheKey implements Serializable {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 8067972995583126404L;

	private static final String TAG = "TokenCacheKey";

	private String mAuthority = "";

	private String[] mScope;

	private String mClientId = "";

	private String mUniqueId = "";

	private String mDisplayableId = "";

	private boolean mIsMultipleResourceRefreshToken;

	private TokenCacheKey() {
	}

	public String toJsonString() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("a", mAuthority);
		obj.put("s", StringExtensions.createStringFromArray(mScope, " "));
		obj.put("c", mClientId);
		obj.put("u", mUniqueId);
		obj.put("d", mDisplayableId);
		obj.put("mr", mIsMultipleResourceRefreshToken);
		return obj.toString();
	}

	public String getLog() {
		StringBuilder sb = new StringBuilder();
		sb.append("Authority:" + mAuthority);
		sb.append(" scope:" + StringExtensions.createStringFromArray(mScope, " "));
		sb.append(" clientid:" + mClientId);
		sb.append(" uniqueid:" + mUniqueId);
		sb.append(" mrrt:" + mIsMultipleResourceRefreshToken);
		return sb.toString();
	}

	/**
	 * @param authority
	 *            URL of the authenticating authority
	 * @param scope
	 *            scope identifier
	 * @param clientId
	 *            client identifier
	 * @param isMultiResourceRefreshToken
	 *            true/false for refresh token type
	 * @param userId
	 *            userid provided from {@link UserInfo}
	 * @return CacheKey to use in saving token
	 */
	public static TokenCacheKey createCacheKey(String authority,
			String[] scope, String clientId,
			boolean isMultiResourceRefreshToken, String uniqueId,
			String displayableId) {

		if (authority == null) {
			throw new IllegalArgumentException("authority");
		}

		if (clientId == null) {
			throw new IllegalArgumentException("clientId");
		}

		TokenCacheKey key = new TokenCacheKey();

		// MultiResource token items will be stored without resource
		key.mScope = scope;

		key.mAuthority = authority.toLowerCase(Locale.US);
		if (key.mAuthority.endsWith("/")) {
			key.mAuthority = (String) key.mAuthority.subSequence(0,
					key.mAuthority.length() - 1);
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
	 * @param item
	 *            Token item in the cache
	 * @return CacheKey to save token
	 */
	public static TokenCacheKey createCacheKey(TokenCacheItem item) {
		if (item == null) {
			throw new IllegalArgumentException("TokenCacheItem");
		}

		String uniqueId = "";
		String displayableId = "";

		if (item.getUserInfo() != null) {
			uniqueId = item.getUserInfo().getUniqueId();
			displayableId = item.getUserInfo().getDisplayableId();
		}

		return createCacheKey(item.getAuthority(), item.getScope(),
				item.getClientId(), item.getIsMultiResourceRefreshToken(),
				uniqueId, displayableId);
	}

	/**
	 * @param item
	 *            AuthenticationRequest item
	 * @return CacheKey to save token
	 */
	public static TokenCacheKey createCacheKey(AuthenticationRequest item) {
		if (item == null) {
			throw new IllegalArgumentException("AuthenticationRequest");
		}

		return createCacheKey(item.getAuthority(), item.getScope(),
				item.getClientId(), false, item.getUserIdentifier()
						.getUniqueId(), item.getUserIdentifier()
						.getDisplayableId());
	}

	/**
	 * Store multi resource refresh tokens with different key. Key will not
	 * include resource and set flag to y.
	 * 
	 * @param item
	 *            AuthenticationRequest item
	 * @param cacheUserId
	 *            UserId in the cache
	 * @return CacheKey to save token
	 */
	public static TokenCacheKey createMultiResourceRefreshTokenKey(
			AuthenticationRequest item, String cacheUniqueID, String cacheDispId) {
		if (item == null) {
			throw new IllegalArgumentException("AuthenticationRequest");
		}

		return createCacheKey(item.getAuthority(), item.getScope(),
				item.getClientId(), true, cacheUniqueID, cacheDispId);
	}

	public static TokenCacheKey createCacheKey(AuthenticationRequest request,
			AuthenticationResult result) {
		String uniqueId = "";
		String displayableId = "";

		if (result.getUserInfo() != null) {
			uniqueId = result.getUserInfo().getUniqueId();
			Logger.v(TAG, "Create key with uniqueid:" + uniqueId);
			displayableId = result.getUserInfo().getDisplayableId();
		}

		return createCacheKey(request.getAuthority(), request.getScope(),
				request.getClientId(), result.getIsMultiResourceRefreshToken(),
				uniqueId, displayableId);
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
	 * Gets Scope.
	 * 
	 * @return Scope
	 */
	public String[] getScope() {
		return mScope;
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

	public void setIsMultipleResourceRefreshToken(boolean mrrt) {
		this.mIsMultipleResourceRefreshToken = mrrt;
	}

    public boolean matches(TokenCacheItem item) {
        // MMRT items can be used without checking resource
        // Match user if specified
        // if key is specified for mrrt, it does not need to check scope intersection
        return mAuthority.equalsIgnoreCase(item.getAuthority())
                && mClientId.equalsIgnoreCase(item.getClientId())
                && (mIsMultipleResourceRefreshToken || isScopeIntersect(item.getScope()))
                && (TextUtils.isEmpty(mUniqueId) || item.getUserInfo() == null || mUniqueId
                        .equalsIgnoreCase(item.getUserInfo().getUniqueId()))
                && (TextUtils.isEmpty(mDisplayableId) || item.getUserInfo() == null || mDisplayableId
                        .equalsIgnoreCase(item.getUserInfo().getDisplayableId()));
    }

    public boolean isUserEmpty() {
		return TextUtils.isEmpty(mDisplayableId)
				&& TextUtils.isEmpty(mUniqueId);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof TokenCacheKey)) {
			return false;
		}

		TokenCacheKey other = (TokenCacheKey) o;
		return mAuthority.equalsIgnoreCase(other.getAuthority())
				&& mClientId.equalsIgnoreCase(other.getClientId())
				&& isScopeEquals(other.getScope())
				&& mIsMultipleResourceRefreshToken == other.mIsMultipleResourceRefreshToken
				&& (mUniqueId.equalsIgnoreCase(other.getUniqueId()))
				&& (mDisplayableId.equalsIgnoreCase(other.getDisplayableId()));

	}
	
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 17 * hash + mAuthority.hashCode();
		hash = 17 * hash + mClientId.hashCode();
		hash = 17 * hash + mScope.hashCode();
		hash = 17 * hash + (mIsMultipleResourceRefreshToken ? 7 : 0);
		hash = 17 * hash + mUniqueId.hashCode();
		hash = 17 * hash + mDisplayableId.hashCode();
		return hash;
	}
	
	private boolean isScopeIntersect(String[] scope) {
	    Set<String> self = StringExtensions.createSet(mScope);
        Set<String> other = StringExtensions.createSet(mScope);
        
        return self.containsAll(other);
    }
	
	private boolean isScopeEquals(String[] otherScope){
	    Set<String> self = StringExtensions.createSet(mScope);
        Set<String> other = StringExtensions.createSet(mScope);

        if (self.size() == other.size())
        {
            return self.containsAll(other);
        }

        return false;
	}
}
