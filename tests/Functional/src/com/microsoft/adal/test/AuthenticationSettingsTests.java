
package com.microsoft.adal.test;

import android.test.AndroidTestCase;

import com.microsoft.adal.AuthenticationSettings;

/**
 * settings to use in ADAL
 */
public class AuthenticationSettingsTests extends AndroidTestCase {

    /**
     * Verify constructor and getters
     */
    public void testgetDefault() {
        AuthenticationSettings setting = AuthenticationSettings.INSTANCE;
        assertEquals("Gives default", "/oauth2/token", setting.getTokenEndpoint());
        assertEquals("Gives default", "/oauth2/authorize", setting.getAuthorizeEndpoint());

        String authorize = "/authorize";
        String token = "token";
        setting.setAuthorizeEndpoint("somethingelse");
        setting.setTokenEndpoint(token);
        AuthenticationSettings.INSTANCE.setAuthorizeEndpoint(authorize);
        assertEquals("Adds prefix to given value", "/" + token, setting.getTokenEndpoint());
        assertEquals("Compare to the expected", authorize, setting.getAuthorizeEndpoint());

        // set back to default
        setting.setAuthorizeEndpoint("/oauth2/authorize");
        setting.setTokenEndpoint("/oauth2/token");
    }
}
