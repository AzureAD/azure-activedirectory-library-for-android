// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal.hello;

public class Constants {
    public static final String SDK_VERSION = "1.0";

    /**
     * UTF-8 encoding
     */
    public static final String UTF8_ENCODING = "UTF-8";

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String HEADER_AUTHORIZATION_VALUE_PREFIX = "Bearer ";

    // AAD PARAMETERS
    // https://login.windows.net/tenantInfo
    static final String AUTHORITY_URL = "https://your.authority.url.here";

    // Clientid is given from AAD page when you register your Android app
    static final String CLIENT_ID = "the_client_ID";

    // RedirectUri
    static final String REDIRECT_URL = "http://your.redirect.url.here";

    // URI for the resource. You need to setup this resource at AAD
    static final String RESOURCE_ID = "your_resource_ID";

    // Endpoint we are targeting for the deployed WebAPI service
    static final String SERVICE_URL = "https://your.serive.url.here";

    // ------------------------------------------------------------------------------------------

    public static final String SHARED_PREFERENCE_NAME = "com.example.com.test.settings";

    public static final String KEY_NAME_ASK_BROKER_INSTALL = "test.settings.ask.broker";

    public static final String KEY_NAME_CHECK_BROKER = "test.settings.check.broker";

    public static final String LOGIN_HINT = "faruk@omercantest.onmicrosoft.com";
}
