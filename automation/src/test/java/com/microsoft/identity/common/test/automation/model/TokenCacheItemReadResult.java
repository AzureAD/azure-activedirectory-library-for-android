package com.microsoft.identity.common.test.automation.model;

import com.google.gson.annotations.SerializedName;

public class TokenCacheItemReadResult {

 /*
 static final String ACCESS_TOKEN = Constants.ACCESS_TOKEN;
        static final String REFRESH_TOKEN = Constants.REFRESH_TOKEN;
        static final String RESOURCE = "resource";
        static final String AUTHORITY = "authority";
        static final String CLIENT_ID = "client_id";
        static final String RAW_ID_TOKEN = "id_token";
        static final String EXPIRES_ON = "expires_on";
        static final String IS_MRRT = "is_mrrt";
        static final String TENANT_ID = Constants.TENANT_ID;
        static final String FAMILY_CLIENT_ID = "foci";
        static final String EXTENDED_EXPIRES_ON= "extended_expires_on";
        static final String UNIQUE_USER_ID = Constants.UNIQUE_ID;
        static final String DISPLAYABLE_ID = Constants.DISPLAYABLE_ID;
        static final String FAMILY_NAME = Constants.FAMILY_NAME;
        static final String GIVEN_NAME = Constants.GIVEN_NAME;
        static final String IDENTITY_PROVIDER = Constants.IDENTITY_PROVIDER;
 */

 @SerializedName(Constants.CACHE_DATA.ACCESS_TOKEN)
 public String accessToken;

 @SerializedName(Constants.CACHE_DATA.REFRESH_TOKEN)
 public String refreshToken;

 @SerializedName(Constants.CACHE_DATA.RESOURCE)
 public String resource;

 @SerializedName(Constants.CACHE_DATA.AUTHORITY)
 public String authority;

 @SerializedName(Constants.CACHE_DATA.CLIENT_ID)
 public String clientId;

 @SerializedName(Constants.CACHE_DATA.RAW_ID_TOKEN)
 public String idToken;

 @SerializedName(Constants.CACHE_DATA.IS_MRRT)
 public Boolean isMRRT;

 @SerializedName(Constants.CACHE_DATA.TENANT_ID)
 public String tenantId;

 @SerializedName(Constants.CACHE_DATA.FAMILY_CLIENT_ID)
 public String familyClientId;

 @SerializedName(Constants.CACHE_DATA.UNIQUE_USER_ID)
 public String uniqueUserId;

 @SerializedName(Constants.CACHE_DATA.DISPLAYABLE_ID)
 public String displayableId;

 @SerializedName(Constants.CACHE_DATA.FAMILY_NAME)
 public String familyName;

 @SerializedName(Constants.CACHE_DATA.GIVEN_NAME)
 public String givenName;

 @SerializedName(Constants.CACHE_DATA.IDENTITY_PROVIDER)
 public String identityProvider;




}
