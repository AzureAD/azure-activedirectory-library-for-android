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

public class AuthenticationConstants {

    public static final class Browser {
        public static final String REQUEST_MESSAGE = "com.microsoft.aad.adal:BrowserRequestMessage";

        public static final String RESPONSE_REQUEST_INFO = "com.microsoft.aad.adal:BrowserRequestInfo";

        public static final String RESPONSE_ERROR_CODE = "com.microsoft.aad.adal:BrowserErrorCode";

        public static final String RESPONSE_ERROR_MESSAGE = "com.microsoft.aad.adal:BrowserErrorMessage";

        public static final String RESPONSE_AUTHENTICATION_EXCEPTION = "com.microsoft.aad.adal:AuthenticationException";

        public static final String RESPONSE_FINAL_URL = "com.microsoft.aad.adal:BrowserFinalUrl";

        public static final String RESPONSE = "com.microsoft.aad.adal:BrokerResponse";

        public static final String WEBVIEW_INVALID_REQUEST = "Invalid request";

        public static final String ACTION_CANCEL = "com.microsoft.aad.adal:BrowserCancel";

        public static final String REQUEST_ID = "com.microsoft.aad.adal:RequestId";
    }

    public static final class UIResponse {
        /**
         * User cancelled.
         */
        public static final int BROWSER_CODE_CANCEL = 2001;

        /**
         * Browser error.
         */
        public static final int BROWSER_CODE_ERROR = 2002;

        /**
         * Flow complete.
         */
        public static final int BROWSER_CODE_COMPLETE = 2003;

        /**
         * Broker returns full response.
         */
        public static final int TOKEN_BROKER_RESPONSE = 2004;

        /**
         * Webview throws Authentication exception. It needs to be send to
         * callback.
         */
        public static final int BROWSER_CODE_AUTHENTICATION_EXCEPTION = 2005;
        
        /**
         * CA flow, device doesn't have company portal or azure authenticator installed. 
         * Waiting for broker package to be installed, and resume request in broker. 
         */
        public static final int BROKER_REQUEST_RESUME = 2006;
    }

    public static final class UIRequest {
        public static final int BROWSER_FLOW = 1001;

        public static final int TOKEN_FLOW = 1002;

        public static final int BROKER_FLOW = 1003;
    }

    public static final class OAuth2 {
        /** Core OAuth2 strings. */
        public static final String ACCESS_TOKEN = "access_token";

        public static final String AUTHORITY = "authority";

        public static final String AUTHORIZATION_CODE = "authorization_code";

        public static final String CLIENT_ID = "client_id";

        public static final String CLIENT_SECRET = "client_secret";

        public static final String CODE = "code";

        public static final String ERROR = "error";

        public static final String ERROR_DESCRIPTION = "error_description";

        public static final String ERROR_CODES = "error_codes";

        public static final String EXPIRES_IN = "expires_in";

        public static final String GRANT_TYPE = "grant_type";

        public static final String REDIRECT_URI = "redirect_uri";

        public static final String REFRESH_TOKEN = "refresh_token";

        public static final String RESPONSE_TYPE = "response_type";

        public static final String SCOPE = "scope";

        public static final String STATE = "state";

        public static final String TOKEN_TYPE = "token_type";

        static final String ID_TOKEN = "id_token";

        static final String ID_TOKEN_SUBJECT = "sub";

        static final String ID_TOKEN_TENANTID = "tid";

        static final String ID_TOKEN_UPN = "upn";

        static final String ID_TOKEN_GIVEN_NAME = "given_name";

        static final String ID_TOKEN_FAMILY_NAME = "family_name";

        static final String ID_TOKEN_UNIQUE_NAME = "unique_name";

        static final String ID_TOKEN_EMAIL = "email";

        static final String ID_TOKEN_IDENTITY_PROVIDER = "idp";

        static final String ID_TOKEN_OBJECT_ID = "oid";

        static final String ID_TOKEN_PASSWORD_EXPIRATION = "pwd_exp";

        static final String ID_TOKEN_PASSWORD_CHANGE_URL = "pwd_url";
        
        static final String ADAL_CLIENT_FAMILY_ID = "foci";
    }

    public static final class AAD {

        /** AAD OAuth2 extension strings. */
        public static final String RESOURCE = "resource";

        /** AAD OAuth2 Challenge strings. */
        public static final String BEARER = "Bearer";

        public static final String AUTHORIZATION = "authorization";

        public static final String AUTHORIZATION_URI = "authorization_uri";

        public static final String REALM = "realm";

        public static final String LOGIN_HINT = "login_hint";
        
        public static final String WEB_UI_CANCEL = "access_denied";

        public static final String CORRELATION_ID = "correlation_id";

        public static final String CLIENT_REQUEST_ID = "client-request-id";

        public static final String RETURN_CLIENT_REQUEST_ID = "return-client-request-id";

        public static final String QUERY_PROMPT = "prompt";

        public static final String QUERY_PROMPT_VALUE = "login";

        public static final String QUERY_PROMPT_REFRESH_SESSION_VALUE = "refresh_session";

        public final static String ADAL_ID_PLATFORM = "x-client-SKU";

        public final static String ADAL_ID_VERSION = "x-client-Ver";

        public final static String ADAL_ID_CPU = "x-client-CPU";

        public final static String ADAL_ID_OS_VER = "x-client-OS";

        public final static String ADAL_ID_DM = "x-client-DM";
    }

    public static final class Broker {

        public static final int BROKER_REQUEST_ID = 1177;

        public static final String BROKER_REQUEST = "com.microsoft.aadbroker.adal.broker.request";

        public static final String BROKER_REQUEST_RESUME = "com.microsoft.aadbroker.adal.broker.request.resume";
        
        /**
         * Account type string.
         */
        public static final String BROKER_ACCOUNT_TYPE = "com.microsoft.workaccount";

        public static final String ACCOUNT_INITIAL_NAME = "aad";

        public static final String BACKGROUND_REQUEST_MESSAGE = "background.request";

        public static final String ACCOUNT_DEFAULT_NAME = "Default";
        
        public static final String BROKER_VERSION = "broker.version";
        
        public static final String BROKER_PROTOCOL_VERSION = "v2";
        
        public static final String BROKER_RESULT_RETURNED = "broker.result.returned";

        /**
         * Authtoken type string.
         */
        public static final String AUTHTOKEN_TYPE = "adal.authtoken.type";

        public static final String BROKER_FINAL_URL = "adal.final.url";

        public static final String ACCOUNT_INITIAL_REQUEST = "account.initial.request";

        public static final String ACCOUNT_CLIENTID_KEY = "account.clientid.key";

        public static final String ACCOUNT_CLIENT_SECRET_KEY = "account.client.secret.key";

        public static final String ACCOUNT_CORRELATIONID = "account.correlationid";

        public static final String ACCOUNT_PROMPT = "account.prompt";

        public static final String ACCOUNT_EXTRA_QUERY_PARAM = "account.extra.query.param";

        public static final String ACCOUNT_LOGIN_HINT = "account.login.hint";

        public static final String ACCOUNT_RESOURCE = "account.resource";

        public static final String ACCOUNT_REDIRECT = "account.redirect";

        public static final String ACCOUNT_AUTHORITY = "account.authority";

        public static final String ACCOUNT_REFRESH_TOKEN = "account.refresh.token";

        public static final String ACCOUNT_ACCESS_TOKEN = "account.access.token";

        public static final String ACCOUNT_EXPIREDATE = "account.expiredate";

        public static final String ACCOUNT_RESULT = "account.result";

        public static final String ACCOUNT_REMOVE_TOKENS = "account.remove.tokens";

        public static final String ACCOUNT_REMOVE_TOKENS_VALUE = "account.remove.tokens.value";

        public static final String MULTI_RESOURCE_TOKEN = "account.multi.resource.token";

        public static final String ACCOUNT_NAME = "account.name";
        
        public static final String ACCOUNT_IDTOKEN = "account.idtoken";

        public static final String ACCOUNT_USERINFO_USERID = "account.userinfo.userid";

        public static final String ACCOUNT_USERINFO_GIVEN_NAME = "account.userinfo.given.name";

        public static final String ACCOUNT_USERINFO_FAMILY_NAME = "account.userinfo.family.name";

        public static final String ACCOUNT_USERINFO_IDENTITY_PROVIDER = "account.userinfo.identity.provider";

        public static final String ACCOUNT_USERINFO_USERID_DISPLAYABLE = "account.userinfo.userid.displayable";

        public static final String ACCOUNT_USERINFO_TENANTID = "account.userinfo.tenantid";

        public static final String ADAL_VERSION_KEY = "adal.version.key";
        
        public static final String ACCOUNT_UID_CACHES = "account.uid.caches";

        public static final String USERDATA_PREFIX = "userdata.prefix";

        public static final String USERDATA_UID_KEY = "calling.uid.key";

        public static final String USERDATA_CALLER_CACHEKEYS = "userdata.caller.cachekeys";

        public static final String CALLER_CACHEKEY_PREFIX = "|";

        public static final String CLIENT_TLS_NOT_SUPPORTED = " PKeyAuth/1.0";

        public static final String CHALLENGE_REQUEST_HEADER = "WWW-Authenticate";

        public static final String CHALLENGE_RESPONSE_HEADER = "Authorization";

        public static final String CHALLENGE_RESPONSE_TYPE = "PKeyAuth";

        public static final String CHALLENGE_RESPONSE_TOKEN = "AuthToken";

        public static final String CHALLENGE_RESPONSE_CONTEXT = "Context";

        /**
         * Certificate authorities are passed with delimiter.
         */
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
        
        public static final String AZURE_AUTHENTICATOR_APP_PACKAGE_NAME = "com.azure.authenticator";

        public static final String PKEYAUTH_REDIRECT = "urn:http-auth:PKeyAuth";

        public static final String CHALLENGE_TLS_INCAPABLE = "x-ms-PKeyAuth";

        public static final String CHALLENGE_TLS_INCAPABLE_VERSION = "1.0";

        public static final String REDIRECT_PREFIX = "msauth";

        public static final Object REDIRECT_DELIMETER_ENCODED = "%2C";
        
        public static final String BROWSER_EXT_PREFIX = "browser://";
        
        public static final String BROWSER_EXT_INSTALL_PREFIX = "msauth://";

        public static final String CALLER_INFO_PACKAGE = "caller.info.package";
        
        public static final String REDIRECT_SSL_PREFIX = "https://";
    }

    public static final String ADAL_PACKAGE_NAME = "com.microsoft.aad.adal";
    
    public static final String MS_FAMILY_ID = "1";

    /** The Constant ENCODING_UTF8. */
    public static final String ENCODING_UTF8 = "UTF_8";

    public static final String BUNDLE_MESSAGE = "Message";

    public static final int DEFAULT_EXPIRATION_TIME_SEC = 3600;

    public static final String AUTHENTICATION_FILE_DIRECTORY = "com.microsoft.aad.adal.authentication";
    
    protected static final class OAuth2ErrorCode {
        /**       
         * Oauth2 error code invalid_grant.
         */       
        static final String INVALID_GRANT = "invalid_grant";
    }     

}
