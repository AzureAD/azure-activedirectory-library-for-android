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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.SmallTest;

// TODO: Likely Unit Test

public class UserInfoTests extends TestCase {

    @SmallTest
    public void testUserInfo() {
        UserInfo user = new UserInfo("userid", "givenName", "familyName", "identity", "userid");
        assertEquals("same userid", "userid", user.getUserId());
        assertEquals("same name", "givenName", user.getGivenName());
        assertEquals("same family name", "familyName", user.getFamilyName());
        assertEquals("same idenity name", "identity", user.getIdentityProvider());
        assertEquals("same flag", "userid", user.getDisplayableId());
    }

    @SmallTest
    public void testTestIdTokenParam_upn() {
        IdToken idToken = setTestIdTokenFields("objectid", "upnid", "email", "subj");
        UserInfo info = new UserInfo(idToken);
        assertEquals("same userid", "objectid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());

        idToken = setTestIdTokenFields("", "upnid", "email", "subj");
        info = new UserInfo(idToken);
        assertEquals("same userid", "subj", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());

        idToken = setTestIdTokenFields("", "upnid", "email", "");
        info = new UserInfo(idToken);
        assertNull("null userid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());

        idToken = setTestIdTokenFields("", "", "email", "");
        info = new UserInfo(idToken);
        assertNull("null userid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "email", info.getDisplayableId());

        idToken = setTestIdTokenFields("", "", "", "");
        info = new UserInfo(idToken);
        assertNull("null userid", info.getUserId());
        assertNull("check displayable", info.getDisplayableId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
    }

    @SmallTest
    public void testTestIdTokenParam_password() {
        IdToken idToken = setTestIdTokenFields("objectid", "upnid", "email", "subj");
        Calendar calendar = new GregorianCalendar();
        int seconds = 1000;
        idToken.mPasswordExpiration = seconds;
        idToken.mPasswordChangeUrl = "https://github.com/MSOpenTech/azure-activedirectory-library";
        UserInfo info = new UserInfo(idToken);
        calendar.add(Calendar.SECOND, seconds);
        Date passwordExpiresOn = calendar.getTime();

        assertEquals("same userid", "objectid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());
        assertEquals("check expireson", passwordExpiresOn.getTime() / 1000, info
                .getPasswordExpiresOn().getTime() / 1000);
        assertEquals("check uri", "https://github.com/MSOpenTech/azure-activedirectory-library",
                info.getPasswordChangeUrl().toString());
    }

    private IdToken setTestIdTokenFields(String objId, String upn, String email, String subject) {
        IdToken idToken = new IdToken();
        idToken.mObjectId = objId;
        idToken.mSubject = subject;
        idToken.mTenantId = "tenantid";
        idToken.mUpn = upn;
        idToken.mGivenName = "givenName";
        idToken.mFamilyName = "familyName";
        idToken.mEmail = email;
        idToken.mIdentityProvider = "provider";
        return idToken;
    }
}
