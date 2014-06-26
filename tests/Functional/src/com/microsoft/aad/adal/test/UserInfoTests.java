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

package com.microsoft.aad.adal.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.microsoft.aad.adal.UserInfo;

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
    public void testIdTokenParam_upn() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {
        Object obj = setIdTokenFields("objectid", "upnid", "email", "subj");
        UserInfo info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".UserInfo", obj);
        assertEquals("same userid", "objectid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());

        obj = setIdTokenFields("", "upnid", "email", "subj");
        info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".UserInfo", obj);
        assertEquals("same userid", "subj", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());

        obj = setIdTokenFields("", "upnid", "email", "");
        info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".UserInfo", obj);
        assertNull("null userid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());

        obj = setIdTokenFields("", "", "email", "");
        info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".UserInfo", obj);
        assertNull("null userid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "email", info.getDisplayableId());

        obj = setIdTokenFields("", "", "", "");
        info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".UserInfo", obj);

        assertNull("null userid", info.getUserId());
        assertNull("check displayable", info.getDisplayableId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
    }

    @SmallTest
    public void testIdTokenParam_password() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Object obj = setIdTokenFields("objectid", "upnid", "email", "subj");
        Calendar calendar = new GregorianCalendar();
        long seconds = calendar.getTimeInMillis() / 1000;
        Date timenow = new Date(seconds * 1000);
        ReflectionUtils.setFieldValue(obj, "mPasswordExpiration", seconds);
        ReflectionUtils.setFieldValue(obj, "mPasswordChangeUrl",
                "https://github.com/MSOpenTech/azure-activedirectory-library");
        UserInfo info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".UserInfo", obj);
        assertEquals("same userid", "objectid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());
        assertEquals("check expireson", timenow, info.getPasswordExpiresOn());
        assertEquals("check uri", "https://github.com/MSOpenTech/azure-activedirectory-library",
                info.getPasswordChangeUrl().toString());
    }

    private Object setIdTokenFields(String objId, String upn, String email, String subject)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Object obj = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME + ".IdToken");
        ReflectionUtils.setFieldValue(obj, "mObjectId", objId);
        ReflectionUtils.setFieldValue(obj, "mSubject", subject);
        ReflectionUtils.setFieldValue(obj, "mTenantId", "tenantid");
        ReflectionUtils.setFieldValue(obj, "mUpn", upn);
        ReflectionUtils.setFieldValue(obj, "mGivenName", "givenName");
        ReflectionUtils.setFieldValue(obj, "mFamilyName", "familyName");
        ReflectionUtils.setFieldValue(obj, "mEmail", email);
        ReflectionUtils.setFieldValue(obj, "mIdentityProvider", "provider");
        return obj;
    }
}
