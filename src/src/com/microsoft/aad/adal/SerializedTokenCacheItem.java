package com.microsoft.aad.adal;

import java.io.UnsupportedEncodingException;
import org.json.JSONException;
import com.google.gson.annotations.SerializedName;

final class SerializedTokenCacheItem {

    private static final String TAG = SerializedTokenCacheItem.class.getSimpleName();

    @SerializedName("authority")
    private String mAuthority;

    @SerializedName("refreshToken")
    private String mRefreshToken;

    @SerializedName("idToken")
    private String mIdToken;
    
    @SerializedName("familyClientId")
    private String mFamilyClientId;
    
    SerializedTokenCacheItem(final TokenCacheItem tokenCacheItem) {
        this.mAuthority = tokenCacheItem.getAuthority();
        this.mRefreshToken = tokenCacheItem.getRefreshToken();
        this.mIdToken = tokenCacheItem.getRawIdToken();
        this.mFamilyClientId = tokenCacheItem.getFamilyClientId();        
    }
    
    public TokenCacheItem parseSerializedTokenCacheItem() {
        TokenCacheItem tokenCacheItem = new TokenCacheItem();
        IdToken idToken;
        
        try {
            idToken = IdToken.create(this.mIdToken);
            UserInfo userInfo = new UserInfo(idToken);
            tokenCacheItem.setUserInfo(userInfo);
            tokenCacheItem.setTenantId(idToken.getTenantId());
        } catch (UnsupportedEncodingException | JSONException e) {
            Logger.e(TAG, "Error in parsing user id token", null,
                    ADALError.IDTOKEN_PARSING_FAILURE, e);
            return null;
        }
        
        //For family refresh token, clientId and resource will all be null
        tokenCacheItem.setAuthority(this.mAuthority);
        tokenCacheItem.setIsMultiResourceRefreshToken(true);
        tokenCacheItem.setRawIdToken(this.mIdToken);
        tokenCacheItem.setFamilyClientId(this.mFamilyClientId);        
        return tokenCacheItem;
    }
}
