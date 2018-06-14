package com.microsoft.identity.common.test.automation.model;

import com.google.gson.annotations.SerializedName;

public class AuthenticationResult {

    @SerializedName(Constants.ACCESS_TOKEN)
    public String accessToken;

    @SerializedName(Constants.TENANT_ID)
    public String tenantId;

}
