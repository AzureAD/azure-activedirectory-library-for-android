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
    private static final String EVENT_PREFIX = "Microsoft.ADAL.";

    static final String EVENT_NAME = EVENT_PREFIX + "event_name";

    // Event names
    static final String API_EVENT = EVENT_PREFIX + "api_event";

    static final String AUTHORITY_VALIDATION_EVENT = EVENT_PREFIX + "authority_validation";

    static final String HTTP_EVENT = EVENT_PREFIX + "http_event";

    static final String BROKER_EVENT = EVENT_PREFIX + "broker_event";

    static final String UI_EVENT = EVENT_PREFIX + "ui_event";

    static final String TOKEN_CACHE_LOOKUP = EVENT_PREFIX + "token_cache_lookup";

    static final String TOKEN_CACHE_WRITE = EVENT_PREFIX + "token_cache_write";

    static final String TOKEN_CACHE_DELETE = EVENT_PREFIX + "token_cache_delete";

    static final String BROKER_REQUEST_SILENT = EVENT_PREFIX + "broker_request_silent";

    static final String BROKER_REQUEST_INTERACTIVE = EVENT_PREFIX + "broker_request_interactive";

    // Event Parameter names
    static final String APPLICATION_NAME = EVENT_PREFIX + "application_name";

    static final String APPLICATION_VERSION = EVENT_PREFIX + "application_version";

    static final String CLIENT_ID = EVENT_PREFIX + "client_id";

    static final String AUTHORITY_NAME = EVENT_PREFIX + "authority";

    static final String AUTHORITY_TYPE = EVENT_PREFIX + "authority_type";

    static final String API_DEPRECATED = EVENT_PREFIX + "is_deprecated"; // Android only

    static final String AUTHORITY_VALIDATION = EVENT_PREFIX + "authority_validation_status";

    static final String PROMPT_BEHAVIOR = EVENT_PREFIX + "prompt_behavior";

    static final String EXTENDED_EXPIRES_ON_SETTING = EVENT_PREFIX + "extended_expires_on_setting";

    static final String WAS_SUCCESSFUL = EVENT_PREFIX + "is_successful";

    static final String API_ERROR_CODE = EVENT_PREFIX + "api_error_code";

    static final String OAUTH_ERROR_CODE = EVENT_PREFIX + "oauth_error_code";

    static final String IDP_NAME = EVENT_PREFIX + "idp";

    static final String TENANT_ID = EVENT_PREFIX + "tenant_id";

    static final String LOGIN_HINT = EVENT_PREFIX + "login_hint";

    static final String USER_ID = EVENT_PREFIX + "user_id";

    static final String CORRELATION_ID = EVENT_PREFIX + "correlation_id";

    static final String DEVICE_ID = EVENT_PREFIX + "device_id";

    static final String REQUEST_ID = EVENT_PREFIX + "request_id";

    static final String START_TIME = EVENT_PREFIX + "start_time";

    static final String STOP_TIME = EVENT_PREFIX + "stop_time";

    static final String RESPONSE_TIME = EVENT_PREFIX + "response_time";

    static final String REDIRECT_COUNT = EVENT_PREFIX + "redirect_count"; // Android only

    static final String NTLM = EVENT_PREFIX + "ntlm";

    static final String USER_CANCEL = EVENT_PREFIX + "user_cancel";

    static final String BROKER_APP = EVENT_PREFIX + "broker_app";

    static final String BROKER_VERSION = EVENT_PREFIX + "broker_version";

    static final String BROKER_APP_USED = EVENT_PREFIX + "broker_app_used";

    static final String TOKEN_TYPE = EVENT_PREFIX + "token_type";

    static final String TOKEN_TYPE_IS_RT = EVENT_PREFIX + "is_rt";

    static final String TOKEN_TYPE_IS_MRRT = EVENT_PREFIX + "is_mrrt";

    static final String TOKEN_TYPE_IS_FRT = EVENT_PREFIX + "is_frt";

    static final String TOKEN_TYPE_RT = EVENT_PREFIX + "rt"; // Android only

    static final String TOKEN_TYPE_MRRT = EVENT_PREFIX + "mrrt"; // Android only

    static final String TOKEN_TYPE_FRT = EVENT_PREFIX + "frt"; // Android only

    static final String CACHE_EVENT_COUNT = EVENT_PREFIX + "cache_event_count";

    static final String UI_EVENT_COUNT = EVENT_PREFIX + "ui_event_count";

    static final String HTTP_EVENT_COUNT = EVENT_PREFIX + "http_event_count";

    static final String HTTP_PATH = EVENT_PREFIX + "http_path";

    static final String HTTP_USER_AGENT = EVENT_PREFIX + "user_agent";

    static final String HTTP_METHOD = EVENT_PREFIX + "method";

    static final String HTTP_METHOD_POST = EVENT_PREFIX + "post";

    static final String HTTP_QUERY_PARAMETERS = EVENT_PREFIX + "query_params";

    static final String HTTP_RESPONSE_CODE = EVENT_PREFIX + "response_code";

    static final String HTTP_API_VERSION = EVENT_PREFIX + "api_version";

    static final String REQUEST_ID_HEADER = EVENT_PREFIX + "x_ms_request_id";

    static final String SERVER_ERROR_CODE = EVENT_PREFIX + "server_error_code";

    static final String SERVER_SUBERROR_CODE = EVENT_PREFIX + "server_sub_error_code";

    static final String TOKEN_AGE = EVENT_PREFIX + "rt_age";

    static final String SPE_INFO = EVENT_PREFIX + "spe_info";

    // Parameter values
    static final String AUTHORITY_TYPE_ADFS = "adfs";
    static final String AUTHORITY_TYPE_AAD = "aad";

    static final String AUTHORITY_VALIDATION_SUCCESS = "yes";
    static final String AUTHORITY_VALIDATION_FAILURE = "no";
    static final String AUTHORITY_VALIDATION_NOT_DONE = "not_done";

    // Broker account service related events
    static final String BROKER_ACCOUNT_SERVICE_STARTS_BINDING = EVENT_PREFIX + "broker_account_service_starts_binding";

    static final String BROKER_ACCOUNT_SERVICE_BINDING_SUCCEED = EVENT_PREFIX + "broker_account_service_binding_succeed";

    static final String BROKER_ACCOUNT_SERVICE_CONNECTED = EVENT_PREFIX + "broker_account_service_connected";

    // API ID
    static final String API_ID = EVENT_PREFIX + "api_id";

    static final String ACQUIRE_TOKEN_SILENT_SYNC = "1";

    static final String ACQUIRE_TOKEN_SILENT_SYNC_FORCE_REFRESH = "13";

    static final String ACQUIRE_TOKEN_SILENT_SYNC_CLAIMS_CHALLENGE = "15";

    static final String ACQUIRE_TOKEN_SILENT = "2";

    static final String ACQUIRE_TOKEN_SILENT_ASYNC = "3";

    static final String ACQUIRE_TOKEN_SILENT_ASYNC_FORCE_REFRESH = "14";

    static final String ACQUIRE_TOKEN_SILENT_ASYNC_CLAIMS_CHALLENGE = "16";

    static final String ACQUIRE_TOKEN_WITH_REFRESH_TOKEN = "4";

    static final String ACQUIRE_TOKEN_WITH_REFRESH_TOKEN_2 = "5";

    static final String ACQUIRE_TOKEN_1 = "100";

    static final String ACQUIRE_TOKEN_2 = "104";

    static final String ACQUIRE_TOKEN_3 = "108";

    static final String ACQUIRE_TOKEN_4 = "111";

    static final String ACQUIRE_TOKEN_5 = "115";

    static final String ACQUIRE_TOKEN_6 = "116";

    static final String ACQUIRE_TOKEN_7 = "117";

    static final String ACQUIRE_TOKEN_8 = "118";

    static final String ACQUIRE_TOKEN_9 = "119";

    static final String ACQUIRE_TOKEN_10 = "120";

    // Private constructor to prevent initialization
    private EventStrings() {
        // Intentionally left blank
    }
}
