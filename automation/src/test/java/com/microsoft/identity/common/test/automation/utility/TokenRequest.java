//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

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
    @SerializedName("unique_id")
    private String mUniqueUserId = null;
    @SerializedName("tenant_id")
    private String mTenantId;
    @SerializedName("force_refresh")
    private Boolean mForceRefresh = false;

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

    public String getUniqueUserId() {
        return this.mUniqueUserId;
    }

    public void setUniqueUserId(String uniqueUserId) {
        this.mUniqueUserId = uniqueUserId;
    }

    public String getTenantId(){
        return mTenantId;
    }

    public void setTenantId(String tenantId) {
        mTenantId = tenantId;
    }

    public Boolean getForceRefresh() {
        return mForceRefresh;
    }

    public void setForceRefresh(Boolean forceRefresh) {
        this.mForceRefresh = forceRefresh;
    }
}
