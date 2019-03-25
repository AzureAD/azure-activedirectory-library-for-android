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
    private AuthenticationConstants() {
    }

    /**
     * ADAL package name.
     */
    public static final String ADAL_PACKAGE_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.ADAL_PACKAGE_NAME;

    /**
     * Microsoft apps family of client Id.
     */
    public static final String MS_FAMILY_ID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.MS_FAMILY_ID;

    /**
     * The Constant ENCODING_UTF8.
     */
    public static final String ENCODING_UTF8 = com.microsoft.identity.common.adal.internal.AuthenticationConstants.ENCODING_UTF8;

    /**
     * Bundle message.
     */
    public static final String BUNDLE_MESSAGE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.BUNDLE_MESSAGE;

    /**
     * Default access token expiration time in seconds.
     */
    public static final int DEFAULT_EXPIRATION_TIME_SEC = com.microsoft.identity.common.adal.internal.AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC;

    /**
     * Holding all the constant value involved in the webview.
     */
    public static final class Browser {

        /**
         * Represents the request object used to construct request sent to authorize endpoint.
         */
        public static final String REQUEST_MESSAGE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.REQUEST_MESSAGE;

        /**
         * Represents the request object returned from webview.
         */
        public static final String RESPONSE_REQUEST_INFO = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO;

        /**
         * Represents the error code returned from webview.
         */
        public static final String RESPONSE_ERROR_CODE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.RESPONSE_ERROR_CODE;

        /**
         * Represents the error message returned from webview.
         */
        public static final String RESPONSE_ERROR_MESSAGE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE;

        /**
         * Represents the exception returned from webview.
         */
        public static final String RESPONSE_AUTHENTICATION_EXCEPTION = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION;

        /**
         * Represents the final url that webview receives.
         */
        public static final String RESPONSE_FINAL_URL = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.RESPONSE_FINAL_URL;

        /**
         * Represents the response returned from broker.
         */
        public static final String RESPONSE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.RESPONSE;

        /**
         * Represent the error code of invalid request returned from webview.
         */
        public static final String WEBVIEW_INVALID_REQUEST = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.WEBVIEW_INVALID_REQUEST;

        /**
         * Used by LocalBroadcastReceivers to filter the intent string of request cancellation.
         */
        public static final String ACTION_CANCEL = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.ACTION_CANCEL;

        /**
         * Used as the key to send back request id.
         */
        public static final String REQUEST_ID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.REQUEST_ID;
    }

    /**
     * Represents the response code.
     */
    public static final class UIResponse {

        /**
         * Represents that user cancelled the flow.
         */
        public static final int BROWSER_CODE_CANCEL = com.microsoft.identity.common.adal.internal.AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL;

        /**
         * Represents that browser error is returned.
         */
        public static final int BROWSER_CODE_ERROR = com.microsoft.identity.common.adal.internal.AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR;

        /**
         * Represents that the authorization code is returned successfully.
         */
        public static final int BROWSER_CODE_COMPLETE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE;

        /**
         * Represents that broker successfully returns the response.
         */
        public static final int TOKEN_BROKER_RESPONSE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE;

        /**
         * Webview throws Authentication exception. It needs to be send to callback.
         */
        public static final int BROWSER_CODE_AUTHENTICATION_EXCEPTION = com.microsoft.identity.common.adal.internal.AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION;

        /**
         * CA flow, device doesn't have company portal or azure authenticator installed.
         * Waiting for broker package to be installed, and resume request in broker.
         */
        public static final int BROKER_REQUEST_RESUME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME;
    }

    /**
     * Represents the request code.
     */
    public static final class UIRequest {

        /**
         * Represents the request of browser flow.
         */
        public static final int BROWSER_FLOW = com.microsoft.identity.common.adal.internal.AuthenticationConstants.UIRequest.BROWSER_FLOW;
    }

    /**
     * Represents the constant value of oauth2 params.
     */
    public static final class OAuth2 {

        /**
         * String of access token.
         */
        public static final String ACCESS_TOKEN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ACCESS_TOKEN;

        /**
         * String of authority.
         */
        public static final String AUTHORITY = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.AUTHORITY;

        /**
         * String of authorization code.
         */
        public static final String AUTHORIZATION_CODE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.AUTHORIZATION_CODE;

        /**
         * String of client id.
         */
        public static final String CLIENT_ID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.CLIENT_ID;

        /**
         * String of code.
         */
        public static final String CODE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.CODE;

        /**
         * String of error.
         */
        public static final String ERROR = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ERROR;

        /**
         * String of error description.
         */
        public static final String ERROR_DESCRIPTION = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ERROR_DESCRIPTION;

        /**
         * String of error codes.
         */
        public static final String ERROR_CODES = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ERROR_CODES;

        /**
         * String of expires in.
         */
        public static final String EXPIRES_IN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.EXPIRES_IN;

        /**
         * String of grant type.
         */
        public static final String GRANT_TYPE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.GRANT_TYPE;

        /**
         * String redirect uri.
         */
        public static final String REDIRECT_URI = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.REDIRECT_URI;

        /**
         * String of refresh token.
         */
        public static final String REFRESH_TOKEN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.REFRESH_TOKEN;

        /**
         * String of response type.
         */
        public static final String RESPONSE_TYPE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.RESPONSE_TYPE;

        /**
         * String of scope.
         */
        public static final String SCOPE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.SCOPE;

        /**
         * String of state.
         */
        public static final String STATE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.STATE;

        /**
         * String of token type.
         */
        public static final String TOKEN_TYPE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.TOKEN_TYPE;

        /**
         * String of http web response body.
         */
        public static final String HTTP_RESPONSE_BODY = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.HTTP_RESPONSE_BODY;

        /**
         * String of http web response headers.
         */
        public static final String HTTP_RESPONSE_HEADER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.HTTP_RESPONSE_HEADER;

        /**
         * String of http web response status code.
         */
        public static final String HTTP_STATUS_CODE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.HTTP_STATUS_CODE;

        /**
         * String of id token.
         */
        static final String ID_TOKEN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN;

        /**
         * String of sub in the id token.
         */
        static final String ID_TOKEN_SUBJECT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_SUBJECT;

        /**
         * String of tenant id in the id token.
         */
        static final String ID_TOKEN_TENANTID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_TENANTID;

        /**
         * String of UPN in the id token claim.
         */
        static final String ID_TOKEN_UPN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_UPN;

        /**
         * String of given name in the id token claim.
         */
        static final String ID_TOKEN_GIVEN_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_GIVEN_NAME;

        /**
         * String of family name in the id token claim.
         */
        static final String ID_TOKEN_FAMILY_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_FAMILY_NAME;

        /**
         * String of unique name.
         */
        static final String ID_TOKEN_UNIQUE_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_UNIQUE_NAME;

        /**
         * String of email in the id token.
         */
        static final String ID_TOKEN_EMAIL = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_EMAIL;

        /**
         * String of identity provider in the id token claim.
         */
        static final String ID_TOKEN_IDENTITY_PROVIDER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_IDENTITY_PROVIDER;

        /**
         * String of oid in the id token claim.
         */
        static final String ID_TOKEN_OBJECT_ID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_OBJECT_ID;

        /**
         * String of password expiration in the id token claim.
         */
        static final String ID_TOKEN_PASSWORD_EXPIRATION = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_PASSWORD_EXPIRATION;

        /**
         * String of password change url in the id token claim.
         */
        static final String ID_TOKEN_PASSWORD_CHANGE_URL = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_PASSWORD_CHANGE_URL;

        /**
         * String of FoCI field returned in the JSON response from token endpoint.
         */
        static final String ADAL_CLIENT_FAMILY_ID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ADAL_CLIENT_FAMILY_ID;

        /**
         * String of has_chrome sent as extra query param to hide back button in the webview.
         */
        static final String HAS_CHROME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.HAS_CHROME;

        static final String EXT_EXPIRES_IN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.EXT_EXPIRES_IN;

        static final String CLAIMS = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.CLAIMS;

        static final String CLOUD_INSTANCE_HOST_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.CLOUD_INSTANCE_HOST_NAME;
    }

    /**
     * Represents the constants value for Active Directory.
     */
    public static final class AAD {

        /**
         * AAD OAuth2 extension strings.
         */
        public static final String RESOURCE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.RESOURCE;

        /**
         * AAD OAuth2 Challenge strings.
         */
        public static final String BEARER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.BEARER;

        /**
         * AAD Oauth2 authorization.
         */
        public static final String AUTHORIZATION = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.AUTHORIZATION;

        /**
         * String of authorization uri.
         */
        public static final String AUTHORIZATION_URI = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.AUTHORIZATION_URI;

        /**
         * AAD Oauth2 string of realm.
         */
        public static final String REALM = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.REALM;

        /**
         * String of login hint.
         */
        public static final String LOGIN_HINT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.LOGIN_HINT;

        /**
         * String of access denied.
         */
        public static final String WEB_UI_CANCEL = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.WEB_UI_CANCEL;

        /**
         * String of correlation id.
         */
        public static final String CORRELATION_ID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.CORRELATION_ID;

        /**
         * String of client request id.
         */
        public static final String CLIENT_REQUEST_ID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.CLIENT_REQUEST_ID;

        /**
         * String of return client request id.
         */
        public static final String RETURN_CLIENT_REQUEST_ID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.RETURN_CLIENT_REQUEST_ID;

        /**
         * String of prompt.
         */
        public static final String QUERY_PROMPT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.QUERY_PROMPT;

        /**
         * String of prompt behavior as always.
         */
        public static final String QUERY_PROMPT_VALUE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.QUERY_PROMPT_VALUE;

        /**
         * String of prompt behavior as refresh session.
         */
        public static final String QUERY_PROMPT_REFRESH_SESSION_VALUE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.QUERY_PROMPT_REFRESH_SESSION_VALUE;

        /**
         * String of ADAL platform.
         */
        public static final String ADAL_ID_PLATFORM = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.ADAL_ID_PLATFORM;

        /**
         * String of ADAL version.
         */
        public static final String ADAL_ID_VERSION = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.ADAL_ID_VERSION;

        /**
         * String of ADAL id CPU.
         */
        public static final String ADAL_ID_CPU = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.ADAL_ID_CPU;

        /**
         * String of client app os version.
         */
        public static final String ADAL_ID_OS_VER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.ADAL_ID_OS_VER;

        /**
         * String of ADAL ID DM.
         */
        public static final String ADAL_ID_DM = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.ADAL_ID_DM;

        /**
         * String of platform value for the sdk.
         */
        public static final String ADAL_ID_PLATFORM_VALUE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.ADAL_ID_PLATFORM_VALUE;

        /**
         * String for request id returned from Evo.
         **/
        public static final String REQUEST_ID_HEADER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.REQUEST_ID_HEADER;
    }

    /**
     * Represents the constants for broker.
     */
    public static final class Broker {

        /**
         * Broker request id.
         */
        public static final int BROKER_REQUEST_ID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_REQUEST_ID;

        /**
         * String for broker request.
         */
        public static final String BROKER_REQUEST = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_REQUEST;

        /**
         * String for broker request resume.
         */
        public static final String BROKER_REQUEST_RESUME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_REQUEST_RESUME;

        /**
         * Account type string.
         */
        public static final String BROKER_ACCOUNT_TYPE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;

        /**
         * String of account initial name.
         */
        public static final String ACCOUNT_INITIAL_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_INITIAL_NAME;

        /**
         * String of background request message.
         */
        public static final String BACKGROUND_REQUEST_MESSAGE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BACKGROUND_REQUEST_MESSAGE;

        /**
         * String of account default.
         */
        public static final String ACCOUNT_DEFAULT_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_DEFAULT_NAME;

        /**
         * String of broker version.
         */
        public static final String BROKER_VERSION = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_VERSION;

        /**
         * String of broker protocol version with PRT support.
         */
        public static final String BROKER_PROTOCOL_VERSION = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION;

        /**
         * String of broker skip cache.
         */
        public static final String BROKER_SKIP_CACHE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_SKIP_CACHE;

        /**
         * String of broker result returned.
         */
        public static final String BROKER_RESULT_RETURNED = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_RESULT_RETURNED;

        /**
         * Authtoken type string.
         */
        public static final String AUTHTOKEN_TYPE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AUTHTOKEN_TYPE;

        /**
         * String of broker final url.
         */
        public static final String BROKER_FINAL_URL = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_FINAL_URL;

        /**
         * String of account initial request.
         */
        public static final String ACCOUNT_INITIAL_REQUEST = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_INITIAL_REQUEST;

        /**
         * String of account client id key.
         */
        public static final String ACCOUNT_CLIENTID_KEY = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;

        /**
         * String of account correlation id.
         */
        public static final String ACCOUNT_CORRELATIONID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID;

        /**
         * String of account prompt.
         */
        public static final String ACCOUNT_PROMPT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_PROMPT;

        /**
         * String of account extra query param.
         */
        public static final String ACCOUNT_EXTRA_QUERY_PARAM = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_EXTRA_QUERY_PARAM;

        /**
         * String of account claims.
         */
        public static final String ACCOUNT_CLAIMS = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLAIMS;

        /**
         * String of account login hint.
         */
        public static final String ACCOUNT_LOGIN_HINT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT;

        /**
         * String of account resource.
         */
        public static final String ACCOUNT_RESOURCE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_RESOURCE;

        /**
         * String of account redirect.
         */
        public static final String ACCOUNT_REDIRECT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;

        /**
         * String of account authority.
         */
        public static final String ACCOUNT_AUTHORITY = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_AUTHORITY;

        /**
         * String of account refresh token.
         */
        public static final String ACCOUNT_REFRESH_TOKEN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REFRESH_TOKEN;

        /**
         * String of account access token.
         */
        public static final String ACCOUNT_ACCESS_TOKEN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN;

        /**
         * String of token expiration data for the broker account.
         */
        public static final String ACCOUNT_EXPIREDATE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE;

        /**
         * String of token result for the broker account.
         */
        public static final String ACCOUNT_RESULT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_RESULT;

        /**
         * String of account remove tokens.
         */
        public static final String ACCOUNT_REMOVE_TOKENS = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS;

        /**
         * String of account remove token value.
         */
        public static final String ACCOUNT_REMOVE_TOKENS_VALUE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS_VALUE;

        /**
         * String of multi resource refresh token.
         */
        public static final String MULTI_RESOURCE_TOKEN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.MULTI_RESOURCE_TOKEN;

        /**
         * String of key for account name.
         */
        public static final String ACCOUNT_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_NAME;

        /**
         * String of key for account id token.
         */
        public static final String ACCOUNT_IDTOKEN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_IDTOKEN;

        /**
         * String of key for user id.
         */
        public static final String ACCOUNT_USERINFO_USERID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID;

        /**
         * String of key for given name.
         */
        public static final String ACCOUNT_USERINFO_GIVEN_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME;

        /**
         * String of key for family name.
         */
        public static final String ACCOUNT_USERINFO_FAMILY_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME;

        /**
         * String of key for identity provider.
         */
        public static final String ACCOUNT_USERINFO_IDENTITY_PROVIDER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER;

        /**
         * String of key for displayable id.
         */
        public static final String ACCOUNT_USERINFO_USERID_DISPLAYABLE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE;

        /**
         * String of key for tenant id.
         */
        public static final String ACCOUNT_USERINFO_TENANTID = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID;

        /**
         * String of key for adal version.
         */
        public static final String ADAL_VERSION_KEY = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ADAL_VERSION_KEY;

        /**
         * String of key for UIDs in the cache.
         */
        public static final String ACCOUNT_UID_CACHES = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_UID_CACHES;

        /**
         * String of key for user data prefix.
         */
        public static final String USERDATA_PREFIX = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.USERDATA_PREFIX;

        /**
         * String of key for UID key.
         */
        public static final String USERDATA_UID_KEY = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.USERDATA_UID_KEY;

        /**
         * String of key for caller cache keys.
         */
        public static final String USERDATA_CALLER_CACHEKEYS = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.USERDATA_CALLER_CACHEKEYS;

        /**
         * String of caller cache key delimiter.
         */
        public static final String CALLER_CACHEKEY_PREFIX = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CALLER_CACHEKEY_PREFIX;

        /**
         * String for pkeyauth sent in user agent string.
         */
        public static final String CLIENT_TLS_NOT_SUPPORTED = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CLIENT_TLS_NOT_SUPPORTED;

        /**
         * String of challenge request header.
         */
        public static final String CHALLENGE_REQUEST_HEADER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER;

        /**
         * String of challenge response header.
         */
        public static final String CHALLENGE_RESPONSE_HEADER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER;

        /**
         * String of challenge response type.
         */
        public static final String CHALLENGE_RESPONSE_TYPE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE;

        /**
         * String of challenge response token.
         */
        public static final String CHALLENGE_RESPONSE_TOKEN = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TOKEN;

        /**
         * String of challenge response context.
         */
        public static final String CHALLENGE_RESPONSE_CONTEXT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_CONTEXT;

        /**
         * Certificate authorities are passed with delimiter.
         */
        public static final String CHALLENGE_REQUEST_CERT_AUTH_DELIMETER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_REQUEST_CERT_AUTH_DELIMETER;

        /**
         * Apk packagename that will install AD-Authenticator. It is used to
         * query if this app installed or not from package manager.
         */
        public static final String COMPANY_PORTAL_APP_PACKAGE_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;

        /**
         * Signature info for Intune Company portal app that installs authenticator
         * component.
         */
        public static final String COMPANY_PORTAL_APP_SIGNATURE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_SIGNATURE;

        /**
         * Signature info for Azure authenticator app that installs authenticator
         * component.
         */
        public static final String AZURE_AUTHENTICATOR_APP_SIGNATURE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_SIGNATURE;

        /**
         * Azure Authenticator app signature hash.
         */
        public static final String AZURE_AUTHENTICATOR_APP_PACKAGE_NAME = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;

        /**
         * The value for pkeyauth redirect.
         */
        public static final String PKEYAUTH_REDIRECT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.PKEYAUTH_REDIRECT;

        /**
         * Value of pkeyauth sent in the header.
         */
        public static final String CHALLENGE_TLS_INCAPABLE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_TLS_INCAPABLE;

        /**
         * Value of supported pkeyauth version.
         */
        public static final String CHALLENGE_TLS_INCAPABLE_VERSION = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_TLS_INCAPABLE_VERSION;

        /**
         * Broker redirect prefix.
         */
        public static final String REDIRECT_PREFIX = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.REDIRECT_PREFIX;

        /**
         * Encoded delimiter for redirect.
         */
        public static final Object REDIRECT_DELIMETER_ENCODED = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.REDIRECT_DELIMETER_ENCODED;

        /**
         * Prefix of redirect to open external browser.
         */
        public static final String BROWSER_EXT_PREFIX = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROWSER_EXT_PREFIX;

        /**
         * Prefix in the redirect for installing broker apps.
         */
        public static final String BROWSER_EXT_INSTALL_PREFIX = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROWSER_EXT_INSTALL_PREFIX;

        /**
         * String for caller package.
         */
        public static final String CALLER_INFO_PACKAGE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CALLER_INFO_PACKAGE;

        /**
         * String for ssl prefix.
         */
        public static final String REDIRECT_SSL_PREFIX = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.REDIRECT_SSL_PREFIX;

        /**
         * Integer for token expiration buffer.
         */
        public static final String EXPIRATION_BUFFER = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.EXPIRATION_BUFFER;

        /**
         * Bundle identifiers for x-ms-clitelem info.
         */
        public static final class CliTelemInfo {

            /**
             * Bundle id for server errors.
             */
            public static final String SERVER_ERROR = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_ERROR;

            /**
             * Bundle id for server suberrors.
             */
            public static final String SERVER_SUBERROR = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_SUBERROR;

            /**
             * Bundle id for refresh token age.
             */
            public static final String RT_AGE = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.RT_AGE;

            /**
             * Bundle id for spe_ring info.
             */
            public static final String SPE_RING = com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SPE_RING;
        }
    }

    /**
     * Represents the oauth2 error code.
     */
    protected static final class OAuth2ErrorCode {
        /**
         * Oauth2 error code invalid_grant.
         */
        static final String INVALID_GRANT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2ErrorCode.INVALID_GRANT;
    }

    /**
     * HTTP header fields.
     */
    static final class HeaderField {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc1945#appendix-D.2.1">RFC-1945</a>
         */
        static final String ACCEPT = com.microsoft.identity.common.adal.internal.AuthenticationConstants.HeaderField.ACCEPT;

        /**
         * Header used to track SPE Ring for telemetry.
         */
        static final String X_MS_CLITELEM = com.microsoft.identity.common.adal.internal.AuthenticationConstants.HeaderField.X_MS_CLITELEM;
    }

    /**
     * Identifiers for file formats and format contents.
     */
    static final class MediaType {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc7159">RFC-7159</a>
         */
        static final String APPLICATION_JSON = com.microsoft.identity.common.adal.internal.AuthenticationConstants.MediaType.APPLICATION_JSON;
    }
}