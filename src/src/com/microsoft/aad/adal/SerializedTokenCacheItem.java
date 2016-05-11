package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

final class SerializedTokenCacheItem {

    private static final String TAG = SerializedTokenCacheItem.class.getSimpleName();

    @SerializedName("authority")
    private final String mAuthority;

    @SerializedName("refreshToken")
    private final String mRefreshToken;

    @SerializedName("idToken")
    private final String mIdToken;
    
    @SerializedName("familyClientId")
    private final String mFamilyClientId;
    
    SerializedTokenCacheItem(final TokenCacheItem tokenCacheItem) {
        this.mAuthority = tokenCacheItem.getAuthority();
        this.mRefreshToken = tokenCacheItem.getRefreshToken();
        this.mIdToken = tokenCacheItem.getRawIdToken();
        this.mFamilyClientId = tokenCacheItem.getFamilyClientId();        
    }
    
    TokenCacheItem parseSerializedTokenCacheItem() throws AuthenticationException {
        TokenCacheItem tokenCacheItem = new TokenCacheItem();
        IdToken idToken;
        idToken = new IdToken(this.mIdToken);
        UserInfo userInfo = new UserInfo(idToken);
        tokenCacheItem.setUserInfo(userInfo);
        tokenCacheItem.setTenantId(idToken.getTenantId());
        
        //For family refresh token, clientId and resource will all be null
        tokenCacheItem.setAuthority(this.mAuthority);
        tokenCacheItem.setIsMultiResourceRefreshToken(true);
        tokenCacheItem.setRawIdToken(this.mIdToken);
        tokenCacheItem.setFamilyClientId(this.mFamilyClientId);        
        return tokenCacheItem;
    }
}
