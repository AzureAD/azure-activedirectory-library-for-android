// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import java.io.Serializable;

import android.content.Intent;
import android.os.Bundle;

public class UserInfo implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8790127561636702672L;

    private String mUserId;

    private String mGivenName;

    private String mFamilyName;

    private String mIdentityProvider;

    private boolean mIsUserIdDisplayable;

    private String mTenantId;

    public UserInfo() {

    }

    public UserInfo(String userid, String givenName, String familyName, String identityProvider,
            boolean isDisplayable) {
        mUserId = userid;
        mGivenName = givenName;
        mFamilyName = familyName;
        mIdentityProvider = identityProvider;
        mIsUserIdDisplayable = isDisplayable;
    }

    public UserInfo(IdToken token) {
        mIsUserIdDisplayable = false;
        if (token != null) {
            mTenantId = token.mTenantId;
            if (!StringExtensions.IsNullOrBlank(token.mUpn)) {
                mUserId = token.mUpn;
                mIsUserIdDisplayable = true;
            } else if (!StringExtensions.IsNullOrBlank(token.mEmail)) {
                mUserId = token.mEmail;
                mIsUserIdDisplayable = true;
            } else if (!StringExtensions.IsNullOrBlank(token.mSubject)) {
                mUserId = token.mSubject;
            }

            mGivenName = token.mGivenName;
            mFamilyName = token.mFamilyName;
            mIdentityProvider = token.mIdentityProvider;
        }
    }

    static UserInfo getUserInfoFromBrokerResult(final Bundle bundle) {
        String userid = bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID);
        String givenName = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME);
        String familyName = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME);
        String identityProvider = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER);
        Boolean displayable = bundle.getBoolean(
                AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE, false);
        return new UserInfo(userid, givenName, familyName, identityProvider, displayable);
    }

    public String getUserId() {
        return mUserId;
    }

    public boolean getIsUserIdDisplayable() {
        return mIsUserIdDisplayable;
    }

    public String getGivenName() {
        return mGivenName;
    }

    public String getFamilyName() {
        return mFamilyName;
    }

    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    public String getTenantId() {
        return mTenantId;
    }
}
