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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.net.Uri;
import android.os.Bundle;

/**
 * Contains information of a single user.
 */
public class UserInfo implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8790127561636702672L;

    private String mUniqueId;

    private String mDisplayableId;

    private String mGivenName;

    private String mFamilyName;

    private String mIdentityProvider;

    private transient Uri mPasswordChangeUrl;

    private transient Date mPasswordExpiresOn;

    public UserInfo() {

    }

    public UserInfo(String userid, String givenName, String familyName, String identityProvider,
            String displayableId) {
        mUniqueId = userid;
        mGivenName = givenName;
        mFamilyName = familyName;
        mIdentityProvider = identityProvider;
        mDisplayableId = displayableId;
    }

    public UserInfo(IdToken token) {

        mUniqueId = null;
        mDisplayableId = null;

        if (!StringExtensions.IsNullOrBlank(token.mObjectId)) {
            mUniqueId = token.mObjectId;
        } else if (!StringExtensions.IsNullOrBlank(token.mSubject)) {
            mUniqueId = token.mSubject;
        }

        if (!StringExtensions.IsNullOrBlank(token.mUpn)) {
            mDisplayableId = token.mUpn;
        } else if (!StringExtensions.IsNullOrBlank(token.mEmail)) {
            mDisplayableId = token.mEmail;
        }

        mGivenName = token.mGivenName;
        mFamilyName = token.mFamilyName;
        mIdentityProvider = token.mIdentityProvider;
        if (token.mPasswordExpiration > 0) {
            // pwd_exp returns seconds to expiration time
            // it returns in seconds. Date accepts milliseconds.
            Calendar expires = new GregorianCalendar();
            expires.add(Calendar.SECOND, (int)token.mPasswordExpiration);
            mPasswordExpiresOn = expires.getTime();
        }

        mPasswordChangeUrl = null;
        if (!StringExtensions.IsNullOrBlank(token.mPasswordChangeUrl)) {
            mPasswordChangeUrl = Uri.parse(token.mPasswordChangeUrl);
        }
    }

    static UserInfo getUserInfoFromBrokerResult(final Bundle bundle) {
        // Broker has one user and related to ADFS WPJ user. It does not return idtoken
        String userid = bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID);
        String givenName = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME);
        String familyName = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME);
        String identityProvider = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER);
        String displayableId = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE);
        return new UserInfo(userid, givenName, familyName, identityProvider, displayableId);
    }

    /**
     * Gets unique userid.
     * 
     * @return
     */
    public String getUserId() {
        return mUniqueId;
    }

    void setUserId(String userid) {
        mUniqueId = userid;
    }

    /**
     * Gets given name.
     * 
     * @return
     */
    public String getGivenName() {
        return mGivenName;
    }

    void setGivenName(String name) {
        mGivenName = name;
    }

    /**
     * Gets family name.
     * 
     * @return
     */
    public String getFamilyName() {
        return mFamilyName;
    }

    void setFamilyName(String familyName) {
        mFamilyName = familyName;
    }

    /**
     * Gets Identity provider.
     * 
     * @return
     */
    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    void setIdentityProvider(String provider) {
        mIdentityProvider = provider;
    }

    /**
     * Gets displayable user name.
     * 
     * @return
     */
    public String getDisplayableId() {
        return mDisplayableId;
    }

    void setDisplayableId(String displayId) {
        mDisplayableId = displayId;
    }

    /**
     * Gets password change url.
     * 
     * @return
     */
    public Uri getPasswordChangeUrl() {
        return mPasswordChangeUrl;
    }

    void setPasswordChangeUrl(Uri passwordChangeUrl) {
        this.mPasswordChangeUrl = passwordChangeUrl;
    }

    /**
     * Gets password expires on.
     * 
     * @return
     */
    public Date getPasswordExpiresOn() {
        return mPasswordExpiresOn;
    }

    void setPasswordExpiresOn(Date passwordExpiresOn) {
        this.mPasswordExpiresOn = passwordExpiresOn;
    }
}
