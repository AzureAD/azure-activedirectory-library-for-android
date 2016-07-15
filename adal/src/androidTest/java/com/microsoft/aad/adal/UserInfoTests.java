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

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.net.Uri;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;
import junit.framework.TestCase;

public class UserInfoTests extends TestCase {
    static final int MILLISECONDS_TO_SECONDS = 1000;

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
    public void testIdTokenParamUpn() throws UnsupportedEncodingException, AuthenticationException {
        IdToken idToken = new IdToken(getIdToken("objectid", "upnid", "email", "subj"));
        UserInfo info = new UserInfo(idToken);
        assertEquals("same userid", "objectid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());

        idToken = new IdToken(getIdToken("", "upnid", "email", "subj"));
        info = new UserInfo(idToken);
        assertEquals("same userid", "subj", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());

        idToken = new IdToken(getIdToken("", "upnid", "email", ""));
        info = new UserInfo(idToken);
        assertNull("null userid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());

        idToken = new IdToken(getIdToken("", "", "email", ""));
        info = new UserInfo(idToken);
        assertNull("null userid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "email", info.getDisplayableId());

        idToken = new IdToken(getIdToken("", "", "", ""));
        info = new UserInfo(idToken);
        assertNull("null userid", info.getUserId());
        assertNull("check displayable", info.getDisplayableId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
    }

    @SmallTest
    public void testIdTokenParamPassword() throws AuthenticationException, UnsupportedEncodingException {
        final String rawIdToken = getIdToken("objectid", "upnid", "email", "subj");
        final IdToken idToken = new IdToken(rawIdToken);
        final UserInfo info = new UserInfo(idToken);

        Calendar expires = new GregorianCalendar();
        expires.add(Calendar.SECOND, (int) idToken.getPasswordExpiration());

        assertEquals("same userid", "objectid", info.getUserId());
        assertEquals("same name", "givenName", info.getGivenName());
        assertEquals("same family name", "familyName", info.getFamilyName());
        assertEquals("same idenity name", "provider", info.getIdentityProvider());
        assertEquals("check displayable", "upnid", info.getDisplayableId());
        assertEquals("check expireson", expires.getTime().getTime() / MILLISECONDS_TO_SECONDS,
                info.getPasswordExpiresOn().getTime() / MILLISECONDS_TO_SECONDS);
        assertEquals("check uri", Uri.parse(idToken.getPasswordChangeUrl()).toString(),
                info.getPasswordChangeUrl().toString());
    }

    private String getIdToken(String objId, String upnStr, String emailStr, String subjectStr)
            throws UnsupportedEncodingException {
        final String sIdTokenClaims = "{\"aud\":\"c3c7f5e5-7153-44d4-90e6-329686d48d76\",\"iss\":\"https://sts.windows.net/6fd1f5cd-a94c-4335-889b-6c598e6d8048/\",\"iat\":1387224169,\"nbf\":1387224170,\"exp\":1387227769,\"pwd_exp\":1387227772,\"pwd_url\":\"pwdUrl\",\"ver\":\"1.0\",\"tid\":\"%s\",\"oid\":\"%s\",\"upn\":\"%s\",\"uniqueName\":\"%s\",\"sub\":\"%s\",\"family_name\":\"%s\",\"given_name\":\"%s\",\"altsecid\":\"%s\",\"idp\":\"%s\",\"email\":\"%s\"}";
        final String sIdTokenHeader = "{\"typ\":\"JWT\",\"alg\":\"none\"}";
        final String tid = "tenantid";
        final String oid = objId;
        final String upn = upnStr;
        final String uniqueName = "testUnique@test.onmicrosoft.com";
        final String sub = subjectStr;
        final String familyName = "familyName";
        final String givenName = "givenName";
        final String altsecid = "altsecid";
        final String idp = "provider";
        final String email = emailStr;
        final String claims = String.format(sIdTokenClaims, tid, oid, upn, uniqueName, sub, familyName, givenName,
                altsecid, idp, email);
        return String.format("%s.%s.",
                new String(
                        Base64.encode(sIdTokenHeader.getBytes(AuthenticationConstants.ENCODING_UTF8),
                                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        AuthenticationConstants.ENCODING_UTF8),
                new String(
                        Base64.encode(claims.getBytes(AuthenticationConstants.ENCODING_UTF8),
                                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        AuthenticationConstants.ENCODING_UTF8));
    }
}
