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
