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

/**
 * {@link AuthenticationConstants} contains all the constant value the SDK is using.
 */
public final class AuthenticationConstants {
    /**
     * Private constructor to prevent an utility class from being initiated.
     */
    private AuthenticationConstants() { }

    /** ADAL package name. */
    public static final String ADAL_PACKAGE_NAME = "com.microsoft.aad.adal";

    /** Microsoft apps family of client Id. */
    public static final String MS_FAMILY_ID = "1";

    /** The Constant ENCODING_UTF8. */
    public static final String ENCODING_UTF8 = "UTF_8";

    /** Bundle message. */
    public static final String BUNDLE_MESSAGE = "Message";

    /** Default access token expiration time in seconds. */
    public static final int DEFAULT_EXPIRATION_TIME_SEC = 3600;

    /**
     * Holding all the constant value involved in the webview.
     */
    public static final class Browser {

        /** Represents the request object used to construst request sent to authorize endpoint. */
        public static final String REQUEST_MESSAGE = "com.microsoft.aad.adal:BrowserRequestMessage";

        /** Represents the request object returned from webview. */
        public static final String RESPONSE_REQUEST_INFO = "com.microsoft.aad.adal:BrowserRequestInfo";

        /** Represents the error code returned from webview. */
        public static final String RESPONSE_ERROR_CODE = "com.microsoft.aad.adal:BrowserErrorCode";

        /** Represents the error message returned from webview. */
        public static final String RESPONSE_ERROR_MESSAGE = "com.microsoft.aad.adal:BrowserErrorMessage";

        /** Represents the exception returned from webview. */
        public static final String RESPONSE_AUTHENTICATION_EXCEPTION = "com.microsoft.aad.adal:AuthenticationException";

        /** Represents the final url that webview receives. */
        public static final String RESPONSE_FINAL_URL = "com.microsoft.aad.adal:BrowserFinalUrl";

        /** Represents the response returned from broker. */
        public static final String RESPONSE = "com.microsoft.aad.adal:BrokerResponse";

        /** Represent the error code of invalid request returned from webview. */
        public static final String WEBVIEW_INVALID_REQUEST = "Invalid request";

        /** Used by LocalBroadcastReceivers to filter the intent string of request cancellation. */
        public static final String ACTION_CANCEL = "com.microsoft.aad.adal:BrowserCancel";

        /** Used as the key to send back request id. */
        public static final String REQUEST_ID = "com.microsoft.aad.adal:RequestId";
    }

    /**
     * Represents the response code.
     */
    public static final class UIResponse {

        /** Represents that user cancelled the flow. */
        public static final int BROWSER_CODE_CANCEL = 2001;

        /** Represents that browser error is returned. */
        public static final int BROWSER_CODE_ERROR = 2002;

        /** Represents that the authorization code is returned successfully. */
        public static final int BROWSER_CODE_COMPLETE = 2003;

        /** Represents that broker successfully returns the response. */
        public static final int TOKEN_BROKER_RESPONSE = 2004;

        /** Webview throws Authentication exception. It needs to be send to callback. */
        public static final int BROWSER_CODE_AUTHENTICATION_EXCEPTION = 2005;
        
        /**
         * CA flow, device doesn't have company portal or azure authenticator installed. 
         * Waiting for broker package to be installed, and resume request in broker. 
         */
        public static final int BROKER_REQUEST_RESUME = 2006;
    }

    /**
     * Represents the request code.
     */
    public static final class UIRequest {

        /** Represents the request of browser flow. */
        public static final int BROWSER_FLOW = 1001;
    }

    /**
     * Represents the constant value of oauth2 params.
     */
    public static final class OAuth2 {

        /** String of access token. */
        public static final String ACCESS_TOKEN = "access_token";

        /** String of authority. */
        public static final String AUTHORITY = "authority";

        /** String of authorization code. */
        public static final String AUTHORIZATION_CODE = "authorization_code";

        /** String of client id. */
        public static final String CLIENT_ID = "client_id";

        /** String of code. */
        public static final String CODE = "code";

        /** String of error. */
        public static final String ERROR = "error";

        /** String of error description. */
        public static final String ERROR_DESCRIPTION = "error_description";

        /** String of error codes. */
        public static final String ERROR_CODES = "error_codes";

        /** String of expires in. */
        public static final String EXPIRES_IN = "expires_in";

        /** String of grant type. */
        public static final String GRANT_TYPE = "grant_type";

        /** String redirect uri. */
        public static final String REDIRECT_URI = "redirect_uri";

        /** String of refresh token. */
        public static final String REFRESH_TOKEN = "refresh_token";

        /** String of response type. */
        public static final String RESPONSE_TYPE = "response_type";

        /** String of scope. */
        public static final String SCOPE = "scope";

        /** String of state. */
        public static final String STATE = "state";

        /** String of token type. */
        public static final String TOKEN_TYPE = "token_type";

        /** String of id token. */
        static final String ID_TOKEN = "id_token";

        /** String of sub in the id token. */
        static final String ID_TOKEN_SUBJECT = "sub";

        /** String of tenant id in the id token. */
        static final String ID_TOKEN_TENANTID = "tid";

        /** String of UPN in the id token claim. */
        static final String ID_TOKEN_UPN = "upn";

        /** String of given name in the id token claim. */
        static final String ID_TOKEN_GIVEN_NAME = "given_name";

        /** String of family name in the id token claim. */
        static final String ID_TOKEN_FAMILY_NAME = "family_name";

        /** String of unique name. */
        static final String ID_TOKEN_UNIQUE_NAME = "unique_name";

        /** String of email in the id token. */
        static final String ID_TOKEN_EMAIL = "email";

        /** String of identity provider in the id token claim. */
        static final String ID_TOKEN_IDENTITY_PROVIDER = "idp";

        /** String of oid in the id token claim. */
        static final String ID_TOKEN_OBJECT_ID = "oid";

        /** String of password expiration in the id token claim. */
        static final String ID_TOKEN_PASSWORD_EXPIRATION = "pwd_exp";

        /** String of password change url in the id token claim. */
        static final String ID_TOKEN_PASSWORD_CHANGE_URL = "pwd_url";

        /** String of FoCI field returnd in the JSON response from token endpoint. */
        static final String ADAL_CLIENT_FAMILY_ID = "foci";

        /** String of has_chrome sent as extra query param to hide back button in the webview. */
        static final String HAS_CHROME = "haschrome";

        static final String EXT_EXPIRES_IN = "ext_expires_in";
    }

    /**
     * Represents the constants value for Active Directory.
     */
    public static final class AAD {

        /** AAD OAuth2 extension strings. */
        public static final String RESOURCE = "resource";

        /** AAD OAuth2 Challenge strings. */
        public static final String BEARER = "Bearer";

        /** AAD Oauth2 authorization.*/
        public static final String AUTHORIZATION = "authorization";

        /** String of authorization uri. */
        public static final String AUTHORIZATION_URI = "authorization_uri";

        /** AAD Oauth2 string of realm. */
        public static final String REALM = "realm";

        /** String of login hint. */
        public static final String LOGIN_HINT = "login_hint";

        /** String of access denied. */
        public static final String WEB_UI_CANCEL = "access_denied";

        /** String of correlation id. */
        public static final String CORRELATION_ID = "correlation_id";

        /** String of client request id. */
        public static final String CLIENT_REQUEST_ID = "client-request-id";

        /** String of return client request id. */
        public static final String RETURN_CLIENT_REQUEST_ID = "return-client-request-id";

        /** String of prompt. */
        public static final String QUERY_PROMPT = "prompt";

        /** String of prompt behavior as always. */
        public static final String QUERY_PROMPT_VALUE = "login";

        /** String of prompt behavior as refresh session. */
        public static final String QUERY_PROMPT_REFRESH_SESSION_VALUE = "refresh_session";

        /** String of ADAL platform. */
        public static final String ADAL_ID_PLATFORM = "x-client-SKU";

        /** String of ADAL version. */
        public static final String ADAL_ID_VERSION = "x-client-Ver";

        /** String of ADAL id CPU. */
        public static final String ADAL_ID_CPU = "x-client-CPU";

        /** String of client app os version. */
        public static final String ADAL_ID_OS_VER = "x-client-OS";

        /** String of ADAL ID DM. */
        public static final String ADAL_ID_DM = "x-client-DM";

        /** String of platform value for the sdk. */
        public static final String ADAL_ID_PLATFORM_VALUE = "Android";

        /** String for request id returned from Evo. **/
        public static final String REQUEST_ID_HEADER = "x-ms-request-id";
    }

    /**
     * Represents the constants for broker.
     */
    public static final class Broker {

        /** Broker request id. */
        public static final int BROKER_REQUEST_ID = 1177;

        /** String for broker request. */
        public static final String BROKER_REQUEST = "com.microsoft.aadbroker.adal.broker.request";

        /** String for broker request resume. */
        public static final String BROKER_REQUEST_RESUME = "com.microsoft.aadbroker.adal.broker.request.resume";
        
        /** Account type string. */
        public static final String BROKER_ACCOUNT_TYPE = "com.microsoft.workaccount";

        /** String of account initial name. */
        public static final String ACCOUNT_INITIAL_NAME = "aad";

        /** String of background request message. */
        public static final String BACKGROUND_REQUEST_MESSAGE = "background.request";

        /** String of account default. */
        public static final String ACCOUNT_DEFAULT_NAME = "Default";

        /** String of broker version. */
        public static final String BROKER_VERSION = "broker.version";

        /** String of broker protocl version with PRT support. */
        public static final String BROKER_PROTOCOL_VERSION = "v2";

        /** String of broker result returned. */
        public static final String BROKER_RESULT_RETURNED = "broker.result.returned";

        /** Authtoken type string. */
        public static final String AUTHTOKEN_TYPE = "adal.authtoken.type";

        /** String of broker final url. */
        public static final String BROKER_FINAL_URL = "adal.final.url";

        /** String of account initial request. */
        public static final String ACCOUNT_INITIAL_REQUEST = "account.initial.request";

        /** String of account client id key.*/
        public static final String ACCOUNT_CLIENTID_KEY = "account.clientid.key";

        /** String of account correlation id. */
        public static final String ACCOUNT_CORRELATIONID = "account.correlationid";

        /** String of account prompt. */
        public static final String ACCOUNT_PROMPT = "account.prompt";

        /** String of account extra query param. */
        public static final String ACCOUNT_EXTRA_QUERY_PARAM = "account.extra.query.param";

        /** String of account login hint. */
        public static final String ACCOUNT_LOGIN_HINT = "account.login.hint";

        /** String of account resource. */
        public static final String ACCOUNT_RESOURCE = "account.resource";

        /** String of account redirect. */
        public static final String ACCOUNT_REDIRECT = "account.redirect";

        /** String of account authority. */
        public static final String ACCOUNT_AUTHORITY = "account.authority";

        /** String of account refresh token. */
        public static final String ACCOUNT_REFRESH_TOKEN = "account.refresh.token";

        /** String of account access token. */
        public static final String ACCOUNT_ACCESS_TOKEN = "account.access.token";

        /** String of token expiration data for the broker account. */
        public static final String ACCOUNT_EXPIREDATE = "account.expiredate";

        /** String of token result for the broker account. */
        public static final String ACCOUNT_RESULT = "account.result";

        /** String of account remove tokens. */
        public static final String ACCOUNT_REMOVE_TOKENS = "account.remove.tokens";

        /** String of account remove token value. */
        public static final String ACCOUNT_REMOVE_TOKENS_VALUE = "account.remove.tokens.value";

        /** String of multi resource refresh token. */
        public static final String MULTI_RESOURCE_TOKEN = "account.multi.resource.token";

        /** String of key for account name. */
        public static final String ACCOUNT_NAME = "account.name";

        /** String of key for account id token. */
        public static final String ACCOUNT_IDTOKEN = "account.idtoken";

        /** String of key for user id. */
        public static final String ACCOUNT_USERINFO_USERID = "account.userinfo.userid";

        /** String of key for given name. */
        public static final String ACCOUNT_USERINFO_GIVEN_NAME = "account.userinfo.given.name";

        /** String of key for family name. */
        public static final String ACCOUNT_USERINFO_FAMILY_NAME = "account.userinfo.family.name";

        /** String of key for identity provider. */
        public static final String ACCOUNT_USERINFO_IDENTITY_PROVIDER = "account.userinfo.identity.provider";

        /** String of key for displayable id. */
        public static final String ACCOUNT_USERINFO_USERID_DISPLAYABLE = "account.userinfo.userid.displayable";

        /** String of key for tenant id. */
        public static final String ACCOUNT_USERINFO_TENANTID = "account.userinfo.tenantid";

        /** String of key for adal version. */
        public static final String ADAL_VERSION_KEY = "adal.version.key";

        /** String of key for UIDs in the cache. */
        public static final String ACCOUNT_UID_CACHES = "account.uid.caches";

        /** String of key for user data prefix. */
        public static final String USERDATA_PREFIX = "userdata.prefix";

        /** String of key for UID key. */
        public static final String USERDATA_UID_KEY = "calling.uid.key";

        /** String of key for caller cache keys. */
        public static final String USERDATA_CALLER_CACHEKEYS = "userdata.caller.cachekeys";

        /** String of caller cache key delimiter. */
        public static final String CALLER_CACHEKEY_PREFIX = "|";

        /** String for pkeyauth sent in user agent string. */
        public static final String CLIENT_TLS_NOT_SUPPORTED = " PKeyAuth/1.0";

        /** String of challenge request header. */
        public static final String CHALLENGE_REQUEST_HEADER = "WWW-Authenticate";

        /** String of challenge response header. */
        public static final String CHALLENGE_RESPONSE_HEADER = "Authorization";

        /** String of challenge response type. */
        public static final String CHALLENGE_RESPONSE_TYPE = "PKeyAuth";

        /** String of challenge response token. */
        public static final String CHALLENGE_RESPONSE_TOKEN = "AuthToken";

        /** String of challenge response context. */
        public static final String CHALLENGE_RESPONSE_CONTEXT = "Context";

        /** Certificate authorities are passed with delimiter. */
        public static final String CHALLENGE_REQUEST_CERT_AUTH_DELIMETER = ";";

        /**
         * Apk packagename that will install AD-Authenticator. It is used to
         * query if this app installed or not from package manager.
         */
        public static final String COMPANY_PORTAL_APP_PACKAGE_NAME = "com.microsoft.windowsintune.companyportal";

        /**
         * Signature info for Intune Company portal app that installs authenticator
         * component.
         */
        public static final String COMPANY_PORTAL_APP_SIGNATURE = "1L4Z9FJCgn5c0VLhyAxC5O9LdlE=";
        
        /**
         * Signature info for Azure authenticator app that installs authenticator
         * component.
         */
        public static final String AZURE_AUTHENTICATOR_APP_SIGNATURE = "ho040S3ffZkmxqtQrSwpTVOn9r0=";

        /** Azure Authenticator app signature hash. */
        public static final String AZURE_AUTHENTICATOR_APP_PACKAGE_NAME = "com.azure.authenticator";

        /** The value for pkeyauth redirect. */
        public static final String PKEYAUTH_REDIRECT = "urn:http-auth:PKeyAuth";

        /** Value of pkeyauth sent in the header. */
        public static final String CHALLENGE_TLS_INCAPABLE = "x-ms-PKeyAuth";

        /** Value of supported pkeyauth version. */
        public static final String CHALLENGE_TLS_INCAPABLE_VERSION = "1.0";

        /** Broker redirect prefix. */
        public static final String REDIRECT_PREFIX = "msauth";

        /** Encoded delimiter for redirect. */
        public static final Object REDIRECT_DELIMETER_ENCODED = "%2C";

        /** Prefix of redirect to open external browser. */
        public static final String BROWSER_EXT_PREFIX = "browser://";

        /** Prefix in the redirect for installing broker apps. */
        public static final String BROWSER_EXT_INSTALL_PREFIX = "msauth://";

        /** String for caller package. */
        public static final String CALLER_INFO_PACKAGE = "caller.info.package";

        /** String for ssl prefix. */
        public static final String REDIRECT_SSL_PREFIX = "https://";
    }

    /**
     * Represents the oauth2 error code.
     */
    protected static final class OAuth2ErrorCode {
        /**       
         * Oauth2 error code invalid_grant.
         */       
        static final String INVALID_GRANT = "invalid_grant";
    }

    /**
     * HTTP header fields.
     */
    static final class HeaderField {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc1945#appendix-D.2.1">RFC-1945</a>
         */
        static final String ACCEPT = "Accept";
    }

    /**
     * Identifiers for file formats and format contents.
     */
    static final class MediaType {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc7159">RFC-7159</a>
         */
        static final String APPLICATION_JSON = "application/json";
    }
}