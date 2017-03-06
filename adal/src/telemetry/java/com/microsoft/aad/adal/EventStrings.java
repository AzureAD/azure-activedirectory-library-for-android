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

final class EventStrings {
    static final String EVENT_NAME = "Microsoft.ADAL.event_name";

    // Event names
    static final String API_EVENT = "Microsoft.ADAL.api_event";

    static final String AUTHORITY_VALIDATION_EVENT = "Microsoft.ADAL.authority_validation";

    static final String HTTP_EVENT = "Microsoft.ADAL.http_event";

    static final String BROKER_EVENT = "Microsoft.ADAL.broker_event";

    static final String UI_EVENT = "Microsoft.ADAL.ui_event";

    static final String TOKEN_CACHE_LOOKUP = "Microsoft.ADAL.token_cache_lookup";

    static final String TOKEN_CACHE_WRITE = "Microsoft.ADAL.token_cache_write";

    static final String TOKEN_CACHE_DELETE = "Microsoft.ADAL.token_cache_delete";

    // Event Parameter names
    static final String APPLICATION_NAME = "Microsoft.ADAL.application_name";

    static final String APPLICATION_VERSION = "Microsoft.ADAL.application_version";

    static final String CLIENT_ID = "Microsoft.ADAL.client_id";

    static final String AUTHORITY_NAME = "Microsoft.ADAL.authority";

    static final String AUTHORITY_TYPE = "Microsoft.ADAL.authority_type";

    static final String API_DEPRECATED = "Microsoft.ADAL.is_deprecated"; // Android only

    static final String AUTHORITY_VALIDATION = "Microsoft.ADAL.authority_validation_status";

    static final String PROMPT_BEHAVIOR = "Microsoft.ADAL.prompt_behavior";

    static final String EXTENDED_EXPIRES_ON_SETTING = "Microsoft.ADAL.extended_expires_on_setting";

    static final String WAS_SUCCESSFUL = "Microsoft.ADAL.is_successful";

    static final String API_ERROR_CODE = "Microsoft.ADAL.api_error_code";

    static final String OAUTH_ERROR_CODE = "Microsoft.ADAL.oauth_error_code";

    static final String IDP_NAME = "Microsoft.ADAL.idp";

    static final String TENANT_ID = "Microsoft.ADAL.tenant_id";

    static final String LOGIN_HINT = "Microsoft.ADAL.login_hint";

    static final String USER_ID = "Microsoft.ADAL.user_id";

    static final String CORRELATION_ID = "Microsoft.ADAL.correlation_id";

    static final String DEVICE_ID = "Microsoft.ADAL.device_id";

    static final String REQUEST_ID = "Microsoft.ADAL.request_id";

    static final String START_TIME = "Microsoft.ADAL.start_time";

    static final String STOP_TIME = "Microsoft.ADAL.stop_time";

    static final String RESPONSE_TIME = "Microsoft.ADAL.response_time";

    static final String REDIRECT_COUNT = "Microsoft.ADAL.redirect_count"; // Android only

    static final String NTLM = "Microsoft.ADAL.ntlm";

    static final String USER_CANCEL = "Microsoft.ADAL.user_cancel";

    static final String BROKER_APP = "Microsoft.ADAL.broker_app";

    static final String BROKER_VERSION = "Microsoft.ADAL.broker_version";

    static final String BROKER_APP_USED = "Microsoft.ADAL.broker_app_used";

    static final String TOKEN_TYPE = "Microsoft.ADAL.token_type";

    static final String TOKEN_TYPE_IS_RT = "Microsoft.ADAL.is_rt";

    static final String TOKEN_TYPE_IS_MRRT = "Microsoft.ADAL.is_mrrt";

    static final String TOKEN_TYPE_IS_FRT = "Microsoft.ADAL.is_frt";

    static final String TOKEN_TYPE_RT = "Microsoft.ADAL.rt"; // Android only

    static final String TOKEN_TYPE_MRRT = "Microsoft.ADAL.mrrt"; // Android only

    static final String TOKEN_TYPE_FRT = "Microsoft.ADAL.frt"; // Android only

    static final String CACHE_EVENT_COUNT = "Microsoft.ADAL.cache_event_count";

    static final String UI_EVENT_COUNT = "Microsoft.ADAL.ui_event_count";

    static final String HTTP_EVENT_COUNT = "Microsoft.ADAL.http_event_count";

    static final String HTTP_PATH = "Microsoft.ADAL.http_path";

    static final String HTTP_USER_AGENT = "Microsoft.ADAL.user_agent";

    static final String HTTP_METHOD = "Microsoft.ADAL.method";

    static final String HTTP_METHOD_POST = "Microsoft.ADAL.post";

    static final String HTTP_QUERY_PARAMETERS = "Microsoft.ADAL.query_params";

    static final String HTTP_RESPONSE_CODE = "Microsoft.ADAL.response_code";

    static final String HTTP_API_VERSION = "Microsoft.ADAL.api_version";

    static final String REQUEST_ID_HEADER = "Microsoft.ADAL.x_ms_request_id";

    // Parameter values
    static final String AUTHORITY_TYPE_ADFS = "adfs";
    static final String AUTHORITY_TYPE_AAD = "Microsoft.ADAL.aad";

    static final String AUTHORITY_VALIDATION_SUCCESS = "Microsoft.ADAL.authority_validation_status_success";
    static final String AUTHORITY_VALIDATION_FAILURE = "Microsoft.ADAL.authority_validation_status_failure";
    static final String AUTHORITY_VALIDATION_NOT_DONE = "Microsoft.ADAL.authority_validation_status_not_done";

    // API ID
    static final String API_ID = "Microsoft.ADAL.api_id";

    static final String ACQUIRE_TOKEN_SILENT_SYNC = "1";

    static final String ACQUIRE_TOKEN_SILENT = "2";

    static final String ACQUIRE_TOKEN_SILENT_ASYNC = "3";

    static final String ACQUIRE_TOKEN_WITH_REFRESH_TOKEN = "4";

    static final String ACQUIRE_TOKEN_WITH_REFRESH_TOKEN_2 = "5";

    static final String ACQUIRE_TOKEN_1 = "100";

    static final String ACQUIRE_TOKEN_2 = "104";

    static final String ACQUIRE_TOKEN_3 = "108";

    static final String ACQUIRE_TOKEN_4 = "111";

    static final String ACQUIRE_TOKEN_5 = "115";

    static final String ACQUIRE_TOKEN_6 = "116";

    static final String ACQUIRE_TOKEN_7 = "117";

    // Private constructor to prevent initialization
    private EventStrings() {
        // Intentionally left blank
    }
}
