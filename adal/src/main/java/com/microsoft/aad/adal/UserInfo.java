// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import android.net.Uri;
import android.os.Bundle;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Contains information of a single user.
 */
public class UserInfo implements Serializable {

    /**
     * Universal version identifier for UserInfo class.
     */
    private static final long serialVersionUID = 8790127561636702672L;

    private String mUniqueId;

    private String mDisplayableId;

    private String mGivenName;

    private String mFamilyName;

    private String mIdentityProvider;

    private transient Uri mPasswordChangeUrl;

    private transient Date mPasswordExpiresOn;

    /**
     * Default constructor for {@link UserInfo}.
     */
    public UserInfo() {
        // Default constructor, intentionally empty.
    }

    /**
     * Constructor for {@link UserInfo}.
     * @param upn Upn that is used to construct the {@link UserInfo}.
     */
    public UserInfo(String upn) {
        mDisplayableId = upn;
    }

    /**
     * Constructor for {@lin UserInfo}.
     * @param userid Unique user id for the userInfo.
     * @param givenName Given name for the userInfo.
     * @param familyName Family name for the userInfo.
     * @param identityProvider IdentityProvider for the userInfo.
     * @param displayableId Displayable for the userInfo.
     */
    public UserInfo(String userid, String givenName, String familyName, String identityProvider,
            String displayableId) {
        mUniqueId = userid;
        mGivenName = givenName;
        mFamilyName = familyName;
        mIdentityProvider = identityProvider;
        mDisplayableId = displayableId;
    }

    /**
     * Constructor for creating {@link UserInfo} from {@link IdToken}.
     * @param idToken The {@link IdToken} to create {@link UserInfo}.
     */
    public UserInfo(IdToken idToken) {

        mUniqueId = null;
        mDisplayableId = null;

        if (!StringExtensions.isNullOrBlank(idToken.getObjectId())) {
            mUniqueId = idToken.getObjectId();
        } else if (!StringExtensions.isNullOrBlank(idToken.getSubject())) {
            mUniqueId = idToken.getSubject();
        }

        if (!StringExtensions.isNullOrBlank(idToken.getUpn())) {
            mDisplayableId = idToken.getUpn();
        } else if (!StringExtensions.isNullOrBlank(idToken.getEmail())) {
            mDisplayableId = idToken.getEmail();
        }

        mGivenName = idToken.getGivenName();
        mFamilyName = idToken.getFamilyName();
        mIdentityProvider = idToken.getIdentityProvider();
        if (idToken.getPasswordExpiration() > 0) {
            // pwd_exp returns seconds to expiration time
            // it returns in seconds. Date accepts milliseconds.
            Calendar expires = new GregorianCalendar();
            expires.add(Calendar.SECOND, (int) idToken.getPasswordExpiration());
            mPasswordExpiresOn = expires.getTime();
        }

        mPasswordChangeUrl = null;
        if (!StringExtensions.isNullOrBlank(idToken.getPasswordChangeUrl())) {
            mPasswordChangeUrl = Uri.parse(idToken.getPasswordChangeUrl());
        }
    }

    /**
     * Creates the {@link UserInfo} from the bundle returned from broker.
     * @param bundle The {@link Bundle} that broker returns.
     * @return {@link UserInfo} created from the bundle result.
     */
    static UserInfo getUserInfoFromBrokerResult(final Bundle bundle) {
        // Broker has one user and related to ADFS WPJ user. It does not return
        // idtoken
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
     * Gets unique user id.
     * 
     * @return the unique id representing an user
     */
    public String getUserId() {
        return mUniqueId;
    }

    /**
     * @param userid The unique user id to set.
     */
    void setUserId(String userid) {
        mUniqueId = userid;
    }

    /**
     * Gets given name.
     * 
     * @return the given name of the user
     */
    public String getGivenName() {
        return mGivenName;
    }

    /**
     * Gets family name.
     * 
     * @return the family name of the user
     */
    public String getFamilyName() {
        return mFamilyName;
    }

    /**
     * Gets Identity provider.
     * 
     * @return the identity provider
     */
    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    /**
     * Gets displayable user name.
     * 
     * @return the displayable user name
     */
    public String getDisplayableId() {
        return mDisplayableId;
    }

    /**
     * @param displayableId The displayable Id to set.
     */
    void setDisplayableId(String displayableId) {
        mDisplayableId = displayableId;
    }

    /**
     * Gets password change url.
     * 
     * @return the password change uri
     */
    public Uri getPasswordChangeUrl() {
        return mPasswordChangeUrl;
    }

    /**
     * Gets password expires on.
     * 
     * @return the time when the password will expire
     */
    public Date getPasswordExpiresOn() {
        return Utility.getImmutableDateObject(mPasswordExpiresOn);
    }
}
