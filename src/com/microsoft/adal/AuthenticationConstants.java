/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 */

package com.microsoft.adal;

/**
 * Provides constants
 */
final public class AuthenticationConstants
{
    public static final class UIResponse
    {
    static final int BROWSER_CODE_CANCEL = 2001;
    static final int BROWSER_CODE_ERROR = 2002;
    static final int BROWSER_CODE_COMPLETE = 2003;
    
    }
    
    public static final class UIRequest
    {
        static final int BROWSER_FLOW = 1001;
    }
    
    
    public static final class OAuth2
    {
        /** Core OAuth2 strings */
        public static final String ACCESS_TOKEN      = "access_token";
        public static final String AUTHORIZATION_CODE = "authorization_code";
        public static final String CLIENT_ID         = "client_id";
        public static final String CLIENT_SECRET     = "client_secret";
        public static final String CODE              = "code";
        public static final String ERROR             = "error";
        public static final String ERROR_DESCRIPTION = "error_description";
        public static final String EXPIRES_IN        = "expires_in";
        public static final String GRANT_TYPE        = "grant_type";
        public static final String REDIRECT_URI      = "redirect_uri";
        public static final String REFRESH_TOKEN     = "refresh_token";
        public static final String RESPONSE_TYPE     = "response_type";
        public static final String SCOPE             = "scope";
        public static final String STATE             = "state";
        public static final String TOKEN_TYPE        = "token_type";
    }
    
    public static final class AAD
    {
        /** AAD OAuth2 extension strings */
        public static final String RESOURCE          = "resource";

        /** AAD OAuth2 Challenge strings */
        public static final String BEARER            = "Bearer";
        public static final String AUTHORIZATION     = "authorization";
        public static final String AUTHORIZATION_URI = "authorization_uri";
        public static final String REALM             = "realm";

        public static final String LOGIN_HINT = "login_hint";
    }


    /** The Constant ENCODING_UTF8. */
    static final String ENCODING_UTF8               = "UTF_8";
    
    public static final String BUNDLE_MESSAGE				= "Message";
    /** Error string constants, matched to other implementations */
    
    static final String AUTH_FAILED                 = "Authorization Failed";
    static final String AUTH_FAILED_ERROR_CODE      = "Authorization Failed: %d";
    static final String AUTH_FAILED_SERVER_ERROR    = "The Authorization Server returned an unrecognized response";
    static final String AUTH_FAILED_NO_CONTROLLER   = "The Application does not have a current ViewController";
    static final String AUTH_FAILED_NO_RESOURCES    = "The required resource bundle could not be loaded";
    static final String AUTH_FAILED_NO_STATE        = "The authorization server response has incorrectly encoded state";
    static final String AUTH_FAILED_BAD_STATE       = "The authorization server response has no encoded state";
    static final String AUTH_FAILED_NO_TOKEN        = "The requested access token could not be found";
    static final String AUTH_FAILED_CANCELLED       = "The user cancelled the authorization request";
    static final String AUTH_FAILED_INTERNAL_ERROR  = "Invalid parameters for authorization operation";
}
