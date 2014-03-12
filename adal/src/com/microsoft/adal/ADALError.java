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

package com.microsoft.adal;

public enum ADALError {

    DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED("Authority validation returned an error"),

    DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE("Authority is not a valid instance"),

    DEVELOPER_AUTHORITY_IS_NOT_VALID_URL("Authority url is not valid"),

    DEVELOPER_AUTHORITY_IS_EMPTY("Authority is empty"),

    DEVELOPER_ASYNC_TASK_REUSED(
            "Async tasks can only be executed one time. They are not supposed to be reused."),

    DEVELOPER_RESOURCE_IS_EMPTY("Resource is empty"),

    DEVELOPER_CONTEXT_IS_NOT_PROVIDED("Context is not provided"),

    DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS(
            "Key/value pair list contains redundant items in the header"),

    CALLBACK_IS_NOT_FOUND("Active callback is not found"),

    DEVELOPER_ACTIVITY_IS_NOT_RESOLVED("Activity is not resolved"),

    DEVELOPER_INTERNET_PERMISSION_MISSING(
            "android.permission.INTERNET is not added to AndroidManifest file"),

    DEVELOPER_CALLING_ON_MAIN_THREAD("Calling from main thread for background operation"), SERVER_INVALID_REQUEST(
            "Invalid request to server"),

    SERVER_ERROR("Server returned an error"),

    IO_EXCEPTION("I/O exception"),

    ARGUMENT_EXCEPTION("Invalid argument"),

    /**
     * webview has ssl related error and returned error code for that.
     */
    ERROR_FAILED_SSL_HANDSHAKE("Webview returned error for SSL"),

    /**
     * webview has some generic error
     */
    ERROR_WEBVIEW("Webview returned an error"),

    ACTIVITY_REQUEST_INTENT_DATA_IS_NULL("Request object is needed to start AuthenticationActivity"),

    BROADCAST_RECEIVER_ERROR("Broadcast receiver has an error"),

    AUTH_FAILED("Authorization failed"),

    AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED("Refresh token is failed and prompt is not allowed"),

    AUTH_FAILED_SERVER_ERROR("The Authorization Server returned an unrecognized response"),

    AUTH_FAILED_NO_RESOURCES("The required resource bundle could not be loaded"),

    AUTH_FAILED_NO_STATE("The authorization server response has incorrectly encoded state"),

    AUTH_FAILED_BAD_STATE("The authorization server response has no encoded state"),

    AUTH_FAILED_NO_TOKEN("The requested access token could not be found"),

    AUTH_FAILED_CANCELLED("The user cancelled the authorization request"),

    AUTH_FAILED_INTERNAL_ERROR("Invalid parameters for authorization operation"),

    DEVICE_INTERNET_IS_NOT_AVAILABLE("Internet permissions are not set for the app"),

    /**
     * onActivityResult is called with null intent data. Activity may be
     * terminated directly
     */
    ON_ACTIVITY_RESULT_INTENT_NULL("onActivityResult is called with null intent data"),

    ON_ACTIVITY_RESULT_CALLBACK_NOT_FOUND("onActivityResult is called, but callback is not found"),

    DEVICE_SHARED_PREF_IS_NOT_AVAILABLE("Shared preferences are not available"),

    DEVICE_CACHE_IS_NOT_WORKING(
            "Cache is not saving the changes. This error will be returned to developer, if cache returns error"),

    DEVICE_FILE_CACHE_IS_NOT_LOADED_FROM_FILE("Cache is not loaded from File"),

    DEVICE_FILE_CACHE_IS_NOT_WRITING_TO_FILE("FileCache could not write to the File"),

    DEVICE_FILE_CACHE_FORMAT_IS_WRONG("Wrong cache file format"),

    DEVICE_CONNECTION_IS_NOT_AVAILABLE("Connection is not available"),

    /**
     * IdToken is normally returned from token endpoint.
     */
    IDTOKEN_PARSING_FAILURE("Cannot parse IdToken"),

    AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN("Authorization code not exchanged for token"),

    BROADCAST_CANCEL_NOT_SUCCESSFUL(
            "Cancel message is not successfully delivered to broadcast receiver. It may be not registered yet. AuthenticationActivity will register that at onResume."),

    CORRELATION_ID_FORMAT("Correlationid is not in UUID format"),

    CORRELATION_ID_NOT_MATCHING_REQUEST_RESPONSE("Correlationid provided in requrest is not matching the response"),

    ENCODING_IS_NOT_SUPPORTED("Encoding format is not supported"),

    SERVER_INVALID_JSON_RESPONSE("Server returned invalid JSON response"),

    AUTH_REFRESH_FAILED("Refresh token request failed"),

    ENCRYPTION_FAILED("Encryption failed"),

    ANDROIDKEYSTORE_FAILED("Failed to use AndroidKeyStore"),

    DISCOVERY_NOT_SUPPORTED(
            "Authority validation is not supported for ADFS authority. Authority validation needs to be disabled for ADFS."),

    BROKER_PACKAGE_NAME_NOT_FOUND("Broker is not installed in your system"),

    BROKER_AUTHENTICATOR_NOT_RESPONDING("Authenticator is not responding"),

    BROKER_AUTHENTICATOR_IO_EXCEPTION("Authenticator has IO Exception"),

    BROKER_VERIFICATION_FAILED("Signature could not be verified"),

    PACKAGE_NAME_NOT_FOUND("Package name is not resolved."),

    DIGEST_ERROR("Error in generating hash with MessageDigest"),

    BROKER_AUTHENTICATION_REQUEST_IS_NULL("Authentication request is null"),

    BROKER_APP_VERIFICATION_FAILED("Calling app could not be verified"),

    BROKER_ACTIVITY_INFO_NOT_FOUND("Activity information is not retrieved"),

    BROKER_SIGNATURE_NOT_SAVED("Signature is not saved"),

    DEVICE_NO_SUCH_ALGORITHM("Device does not support the algorithm"),

    DEVICE_ALGORITHM_PADDING_EXCEPTION("Requested padding is not available"),

    APP_PACKAGE_NAME_NOT_FOUND("App package name is not found in the package manager"),

    ENCRYPTION_ERROR("Encryption related error"),

    BROKER_ACTIVITY_IS_NOT_RESOLVED("Broker activity is not resolved"),

    BROKER_ACTIVITY_INVALID_REQUEST("Invalid request parameters in request"),

    BROKER_ACCOUNT_SAVE_FAILED("Broker could not save the new account"),

    ;
    private String mDescription;

    private ADALError(String message) {
        mDescription = message;
    }

    public String getDescription() {
        return mDescription;
    }
}
