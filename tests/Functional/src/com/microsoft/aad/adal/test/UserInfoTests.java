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

package com.microsoft.aad.adal.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.microsoft.aad.adal.UserInfo;

import android.test.suitebuilder.annotation.SmallTest;
import junit.framework.TestCase;

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
        int seconds = 1000;
        ReflectionUtils.setFieldValue(obj, "mPasswordExpiration", seconds);
        ReflectionUtils.setFieldValue(obj, "mPasswordChangeUrl",
                "https://github.com/MSOpenTech/azure-activedirectory-library");
        UserInfo info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME
                + ".UserInfo", obj);
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
