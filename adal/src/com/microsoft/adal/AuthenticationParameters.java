
package com.microsoft.adal;

import java.net.URL;

import com.microsoft.adal.AuthenticationParameters;
import com.microsoft.adal.AuthenticationParameters.AuthenticationParamCallback;

/**
 * Matching to ADAL.NET It provides helper methods to get the
 * authorization_endpoint from resource address
 */
public class AuthenticationParameters {

    /**
     * get authority
     */
    public String getAuthority() {
        throw new UnsupportedOperationException();
    }

    /**
     * get resource
     */
    public String getResource() {
        throw new UnsupportedOperationException();
    }

    public interface AuthenticationParamCallback {
        public void onCompleted(Exception exception,
                AuthenticationParameters param);
    }

    /**
     * ADAL will make the call to get authority and resource info
     */
    public static void createFromResourceUrl(
            URL resourceUrl, AuthenticationParamCallback callback) {
        throw new UnsupportedOperationException();
    }

    /**
     * ADAL will parse the header response to get authority and resource info
     */
    public static AuthenticationParameters createFromResponseAuthenticateHeader(
            String authenticateHeader) {
        throw new UnsupportedOperationException();
    }
}
