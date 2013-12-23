
package com.microsoft.adal.test;

import java.lang.reflect.InvocationTargetException;

import android.test.suitebuilder.annotation.SmallTest;

import com.microsoft.adal.UserInfo;

import junit.framework.TestCase;

public class UserInfoTests extends TestCase {

    @SmallTest
    public void testUserInfo() {
        UserInfo user = new UserInfo("userid", "givenName", "familyName", "identity", true);
        assertEquals("same userid", "userid", user.getUserId());
        assertEquals("same name", "givenName", user.getGivenName());
        assertEquals("same family name", "familyName", user.getFamilyName());
        assertEquals("same idenity name", "identity", user.getIdentityProvider());
        assertEquals("same flag", true, user.getIsUserIdDisplayable());
    }
    
    @SmallTest
    public void testIdTokenParam_upn() throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchFieldException{
      Object obj = setIdTokenFields("upnid", "email", "subj");     
      UserInfo info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME+".UserInfo", obj);
      
      assertEquals("same userid", "upnid", info.getUserId());
      assertEquals("same name", "givenName", info.getGivenName());
      assertEquals("same family name", "familyName", info.getFamilyName());
      assertEquals("same idenity name", "provider", info.getIdentityProvider());
      assertEquals("same flag", true, info.getIsUserIdDisplayable());
      
      obj = setIdTokenFields("", "email", "subj");     
      info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME+".UserInfo", obj);
      
      assertEquals("same userid", "email", info.getUserId());
      assertEquals("same name", "givenName", info.getGivenName());
      assertEquals("same family name", "familyName", info.getFamilyName());
      assertEquals("same idenity name", "provider", info.getIdentityProvider());
      assertEquals("same flag", true, info.getIsUserIdDisplayable());
      
      obj = setIdTokenFields("", "", "subj");     
      info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME+".UserInfo", obj);
      
      assertEquals("same userid", "subj", info.getUserId());
      assertEquals("same name", "givenName", info.getGivenName());
      assertEquals("same family name", "familyName", info.getFamilyName());
      assertEquals("same idenity name", "provider", info.getIdentityProvider());
      assertEquals("same flag", false, info.getIsUserIdDisplayable());
      
      obj = setIdTokenFields("", "", "");     
      info = (UserInfo)ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME+".UserInfo", obj);
      
      assertNull("same userid", info.getUserId());
      assertEquals("same name", "givenName", info.getGivenName());
      assertEquals("same family name", "familyName", info.getFamilyName());
      assertEquals("same idenity name", "provider", info.getIdentityProvider());
      assertEquals("same flag", false, info.getIsUserIdDisplayable());
    }

    private Object setIdTokenFields(String upn, String email, String subject) throws ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchFieldException {
        Object obj = ReflectionUtils.getInstance(ReflectionUtils.TEST_PACKAGE_NAME+".IdToken");
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
