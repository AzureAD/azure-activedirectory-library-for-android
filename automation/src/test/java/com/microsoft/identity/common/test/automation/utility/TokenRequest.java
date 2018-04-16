package com.microsoft.identity.common.test.automation.utility;

import com.google.gson.annotations.SerializedName;

/**
 * Created by shoatman on 3/9/2018.
 */

public class TokenRequest {

    @SerializedName("client_id")
    private String mClientId = null;
    @SerializedName("resource")
    private String mResourceId = null;
    @SerializedName("redirect_uri")
    private String mRedirectUri = null;
    @SerializedName("authority")
    private String mAuthority = null;
    @SerializedName("validate_authority")
    private Boolean mValidateAuthority = true;
    @SerializedName("user_broker")
    private Boolean mUseBroker = false;
    @SerializedName("prompt_behavior")
    private String mPromptBehavior = null;
    @SerializedName("extra_qp")
    private String mExtraQueryStringParameters = null;
    @SerializedName("user_identifier")
    private String mUserIdentifier = null;
    @SerializedName("user_identifier_type")
    private String mUserIdentifierType = null;


    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String mClientId) {
        this.mClientId = mClientId;
    }

    public String getResourceId() {
        return mResourceId;
    }

    public void setResourceId(String mResourceId) {
        this.mResourceId = mResourceId;
    }

    public String getRedirectUri() {
        return mRedirectUri;
    }

    public void setRedirectUri(String mRedirectUri) {
        this.mRedirectUri = mRedirectUri;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(String mAuthority) {
        this.mAuthority = mAuthority;
    }

    public Boolean getValidateAuthority() {
        return mValidateAuthority;
    }

    public void setValidateAuthority(Boolean mValidateAuthority) {
        this.mValidateAuthority = mValidateAuthority;
    }

    public Boolean getUseBroker() {
        return mUseBroker;
    }

    public void setUseBroker(Boolean mUseBroker) {
        this.mUseBroker = mUseBroker;
    }

    public String getPromptBehavior() {
        return mPromptBehavior;
    }

    public void setPromptBehavior(String mPromptBehavior) {
        this.mPromptBehavior = mPromptBehavior;
    }

    public String getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
    }

    public void setExtraQueryStringParameters(String mExtraQueryStringParameters) {
        this.mExtraQueryStringParameters = mExtraQueryStringParameters;
    }

    public String getUserIdentitfier() {
        return mUserIdentifier;
    }

    public void setUserIdentitfier(String mUserIdentitfier) {
        this.mUserIdentifier = mUserIdentitfier;
    }

    public String getUserIdentifierType() {
        return mUserIdentifierType;
    }

    public void setUserIdentifierType(String mUserIdentifierType) {
        this.mUserIdentifierType = mUserIdentifierType;
    }
}
