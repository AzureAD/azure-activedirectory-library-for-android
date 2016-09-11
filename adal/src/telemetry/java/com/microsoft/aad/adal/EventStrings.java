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

    static final String EVENT_NAME = "event_name";

    // Event names
    static final String API_EVENT = "api_event";

    static final String AUTHORITY_VALIDATION_EVENT = "authority_validation";

    static final String HTTP_EVENT = "http_event";

    static final String BROKER_EVENT = "broker_event";

    static final String UI_EVENT = "ui_event";

    static final String ENCRYPTION_EVENT = "encryption_event";

    static final String DECRYPTION_EVENT = "decryption_event";

    static final String TOKEN_CACHE_LOOKUP = "token_cache_lookup";

    static final String TOKEN_CACHE_WRITE = "token_cache_write";

    static final String TOKEN_CACHE_DELETE = "token_cache_delete";

    // API ID
    static final String API_ID = "api_id";

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

    // Event Parameter strings
    static final String APPLICATION_NAME = "application_name";

    static final String APPLICATION_VERSION = "application_version";

    static final String CLIENT_ID = "client_id";

    static final String CLIENT_IP = "client_ip";

    static final String AUTHORITY_NAME = "authority";

    static final String AUTHORITY_TYPE = "authority_type";

    static final String AUTHORITY_TYPE_ADFS = "adfs";

    static final String AUTHORITY_TYPE_AAD = "aad";

    static final String API_DEPRECATED = "is_deprecated";

    static final String AUTHORITY_VALIDATION = "authority_validation_status";

    static final String PROMPT_BEHAVIOR = "prompt_behavior";

    static final String EXTENDED_EXPIRES_ON_SETTING = "extended_expires_on_setting";

    static final String WAS_SUCCESSFUL = "is_successful";

    static final String IDP_NAME = "idp";

    static final String TENANT_ID = "tenant";

    static final String LOGIN_HINT = "login_hint";

    static final String USER_ID = "user_id";

    static final String AUTHORITY_VALIDATION_SUCCESS = "authority_validation_status_success";

    static final String AUTHORITY_VALIDATION_FAILURE = "authority_validation_status_failure";

    static final String AUTHORITY_VALIDATION_NOT_DONE = "authority_validation_status_not_done";

    static final String CORRELATION_ID = "correlation_id";

    static final String DEVICE_ID = "device_id";

    static final String REQUEST_ID = "request_id";

    static final String START_TIME = "start_time";

    static final String STOP_TIME = "stop_time";

    static final String RESPONSE_TIME = "response_time";

    static final String REDIRECT_COUNT = "redirect_count";

    static final String USER_CANCEL = "user_cancel";

    static final String BROKER_APP = "broker_app";

    static final String BROKER_VERSION = "broker_version";

    static final String BROKER_APP_USED = "broker_app_used";

    static final String TOKEN_TYPE = "token_type";

    static final String TOKEN_TYPE_IS_RT = "is_rt";

    static final String TOKEN_TYPE_IS_MRRT = "is_mrrt";

    static final String TOKEN_TYPE_IS_FRT = "is_frt";

    static final String TOKEN_TYPE_RT = "rt";

    static final String TOKEN_TYPE_MRRT = "mrrt";

    static final String TOKEN_TYPE_FRT = "frt";

    static final String CACHE_EVENT_COUNT = "cache_event_count";

    static final String UI_EVENT_COUNT = "ui_event_count";

    static final String HTTP_EVENT_COUNT = "http_event_count";

    static final String CRYPTOGRAPHY_STATUS = "status";

    static final String CRYPTOGRAPHY_EXCEPTION = "exception";

    static final String HTTP_USER_AGENT = "user_agent";

    static final String HTTP_METHOD = "method";

    static final String HTTP_METHOD_POST = "post";

    static final String HTTP_QUERY_PARAMETERS = "query_parameters";

    static final String HTTP_RESPONSE_CODE = "response_code";

    static final String HTTP_API_VERSION = "api_version";

    // Private constructor to prevent initialization
    private EventStrings() {
        // Intentionally left blank
    }
}
