
package com.microsoft.adal.test;

import com.microsoft.adal.UserInfo;

import junit.framework.TestCase;

public class UserInfoTests extends TestCase {

    public void testUserInfo() {
        UserInfo user = new UserInfo("userid", "givenName", "familyName", "identity", true);
        assertEquals("same userid", "userid", user.getUserId());
        assertEquals("same name", "givenName", user.getGivenName());
        assertEquals("same family name", "familyName", user.getFamilyName());
        assertEquals("same idenity name", "identity", user.getIdentityProvider());
        assertEquals("same flag", true, user.getIsUserIdDisplayable());
    }
}
