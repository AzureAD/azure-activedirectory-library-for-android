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
import java.util.Date;

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

    private String mVersion;

    private String mUniqueId;

    private String mDisplayableId;

    private String mName;

    private String mIdentityProvider;

    private transient Uri mPasswordChangeUrl;

    private transient Date mPasswordExpiresOn;

    public UserInfo() {

    }

    public UserInfo(String upn) {
        mDisplayableId = upn;
    }

    public UserInfo(String userid, String givenName, String identityProvider,
            String displayableId) {
        mUniqueId = userid;
        mName = givenName;
        mIdentityProvider = identityProvider;
        mDisplayableId = displayableId;
    }

    public UserInfo(ProfileInfo profileinfo) {
        if (!StringExtensions.IsNullOrBlank(profileinfo.mVersion)) {
            mVersion = profileinfo.mVersion;
        }

        if (!StringExtensions.IsNullOrBlank(profileinfo.mSubject)) {
            mUniqueId = profileinfo.mSubject;
        }

        if (!StringExtensions.IsNullOrBlank(profileinfo.mPreferredName)) {
            mDisplayableId = profileinfo.mPreferredName;
        }

        if (!StringExtensions.IsNullOrBlank(profileinfo.mName)) {
            mName = profileinfo.mName;
        }
    }

    static UserInfo getUserInfoFromBrokerResult(final Bundle bundle) {
        // Broker has one user and related to ADFS WPJ user. It does not return
        // idtoken
        String userid = bundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID);
        String givenName = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME);
        String identityProvider = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER);
        String displayableId = bundle
                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE);
        return new UserInfo(userid, givenName, identityProvider, displayableId);
    }

    /**
     * Gets unique userid.
     * 
     * @return
     */
    public String getUniqueId() {
        return mUniqueId;
    }

    void setUniqueId(String userid) {
        mUniqueId = userid;
    }

    /**
     * Gets given name.
     * 
     * @return
     */
    public String getName() {
        return mName;
    }

    void setName(String name) {
        mName = name;
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
