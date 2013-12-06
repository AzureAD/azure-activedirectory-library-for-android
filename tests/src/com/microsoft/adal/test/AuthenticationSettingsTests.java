
package com.microsoft.adal.test;

import android.test.AndroidTestCase;

import com.microsoft.adal.AuthenticationSettings;

/**
 * 
 * settings to use in ADAL
 *
 */
public class AuthenticationSettingsTests extends AndroidTestCase {

    /**
     * Verify constructor and getters
     */
    public void testgetDefault() {
        AuthenticationSettings setting = AuthenticationSettings.INSTANCE;
        assertEquals("Gives default", "/oauth2/token", setting.getTokenEndpointKeyword());
        assertEquals("Gives default", "/oauth2/authorize", setting.getAuthorizeEndpointKeyword());

        String authorize = "/authorize";
        String token = "/token";
        setting.setAuthorizeEndpointKeyword("somethingelse");
        setting.setTokenEndpointKeyword(token);
        AuthenticationSettings.INSTANCE.setAuthorizeEndpointKeyword(authorize);
        assertEquals("Gives default", token, setting.getTokenEndpointKeyword());
        assertEquals("Gives default", authorize, setting.getAuthorizeEndpointKeyword());
        
        // set back to default
        setting.setAuthorizeEndpointKeyword("/oauth2/authorize");
        setting.setTokenEndpointKeyword("/oauth2/token");
    }
}
