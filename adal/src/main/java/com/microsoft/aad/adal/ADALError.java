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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

/**
 * ADAL Error codes.
 */
public enum ADALError {

    /**
     * Authority validation returned an error.
     */
    DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED("Authority validation returned an error"),

    /**
     * Authority is not a valid instance.
     */
    DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE("Authority is not a valid instance"),

    /**
     * Authority url is not valid.
     */
    DEVELOPER_AUTHORITY_IS_NOT_VALID_URL("Authority url is not valid"),

    /**
     * Authority is empty.
     */
    DEVELOPER_AUTHORITY_IS_EMPTY("Authority is empty"),

    /**
     * Async tasks can only be executed one time. They are not supposed to be
     * reused.
     */
    DEVELOPER_ASYNC_TASK_REUSED(
            "Async tasks can only be executed one time. They are not supposed to be reused."),

    /**
     * Resource is empty.
     */
    DEVELOPER_RESOURCE_IS_EMPTY("Resource is empty"),

    /**
     * Context is not provided.
     */
    DEVELOPER_CONTEXT_IS_NOT_PROVIDED("Context is not provided"),

    /**
     * Key/value pair list contains redundant items in the header.
     */
    DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS(
            "Key/value pair list contains redundant items in the header"),

    /**
     * Active callback is not found.
     */
    CALLBACK_IS_NOT_FOUND("Active callback is not found"),

    /**
     * Activity is not resolved.
     */
    DEVELOPER_ACTIVITY_IS_NOT_RESOLVED(
            "Activity is not resolved. Verify the activity name in your manifest file"),

    /**
     * android.permission.INTERNET is not added to AndroidManifest file.
     */
    DEVELOPER_INTERNET_PERMISSION_MISSING(
            "android.permission.INTERNET is not added to AndroidManifest file"),

    /**
     * GET_ACCOUNTS, MANAGE_ACCOUNTS, USE_CREDENTIALS are not added to
     * AndroidManifest file.
     */
    DEVELOPER_BROKER_PERMISSIONS_MISSING(
            "GET_ACCOUNTS, MANAGE_ACCOUNTS, USE_CREDENTIALS are not added to AndroidManifest file"),

    /**
     * Calling from main thread for background operation.
     */
    DEVELOPER_CALLING_ON_MAIN_THREAD("Calling from main thread for background operation"),

    /**
     * Layout file does not have correct elements such as different webview id.
     */
    DEVELOPER_DIALOG_LAYOUT_INVALID("dialog_authentication.xml file has invalid elements"),
    
    /**
     * Invalid request to server.
     */
    SERVER_INVALID_REQUEST("Invalid request to server"),

    /**
     * Server returned an error.
     */
    SERVER_ERROR("Server returned an error"),

    /**
     * I/O exception.
     */
    IO_EXCEPTION("I/O exception"),

    /**
     * Invalid argument.
     */
    ARGUMENT_EXCEPTION("Invalid argument"),

    /**
     * WebView has ssl related error and returned error code for that.
     */
    ERROR_FAILED_SSL_HANDSHAKE("Webview returned error for SSL"),

    /**
     * WebView has some generic error.
     */
    ERROR_WEBVIEW("Webview returned an error"),

    /**
     * Request object is needed to start {@link AuthenticationActivity}.
     */
    ACTIVITY_REQUEST_INTENT_DATA_IS_NULL("Request object is null"),

    /**
     * Broadcast receiver has an error.
     */
    BROADCAST_RECEIVER_ERROR("Broadcast receiver has an error"),

    /**
     * Authorization failed.
     */
    AUTH_FAILED("Authorization failed"),

    /**
     * Refresh token is failed and prompt is not allowed.
     */
    AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED("Refresh token is failed and prompt is not allowed"),

    /**
     * The Authorization Server returned an unrecognized response.
     */
    AUTH_FAILED_SERVER_ERROR("The Authorization Server returned an unrecognized response"),

    /**
     * The required resource bundle could not be loaded.
     */
    AUTH_FAILED_NO_RESOURCES("The required resource bundle could not be loaded"),

    /**
     * The authorization server response has incorrectly encoded state.
     */
    AUTH_FAILED_NO_STATE("The authorization server response has incorrectly encoded state"),

    /**
     * The authorization server response has no encoded state.
     */
    AUTH_FAILED_BAD_STATE("The authorization server response has no encoded state"),

    /**
     * The requested access token could not be found.
     */
    AUTH_FAILED_NO_TOKEN("The requested access token could not be found"),

    /**
     * The user cancelled the authorization request.
     */
    AUTH_FAILED_CANCELLED("The user cancelled the authorization request"),

    /**
     * Invalid parameters for authorization operation.
     */
    AUTH_FAILED_INTERNAL_ERROR("Invalid parameters for authorization operation"),

    /**
     * User returned by service does not match the one in the request.
     */
    AUTH_FAILED_USER_MISMATCH("User returned by service does not match the one in the request"),

    /**
     * Internet permissions are not set for the app.
     */
    DEVICE_INTERNET_IS_NOT_AVAILABLE("Internet permissions are not set for the app"),

    /**
     * onActivityResult is called with null intent data. Activity may be
     * terminated directly.
     */
    ON_ACTIVITY_RESULT_INTENT_NULL("onActivityResult is called with null intent data"),

    /**
     * onActivityResult is called, but callback is not found.
     */
    ON_ACTIVITY_RESULT_CALLBACK_NOT_FOUND("onActivityResult is called, but callback is not found"),

    /**
     * Shared preferences are not available.
     */
    DEVICE_SHARED_PREF_IS_NOT_AVAILABLE("Shared preferences are not available"),

    /**
     * Cache is not saving the changes. This error will be returned to
     * developer, if cache returns error.
     */
    DEVICE_CACHE_IS_NOT_WORKING("Cache is not saving the changes."),

    /**
     * Cache is not loaded from File.
     */
    DEVICE_FILE_CACHE_IS_NOT_LOADED_FROM_FILE("Cache is not loaded from File"),

    /**
     * FileCache could not write to the File.
     */
    DEVICE_FILE_CACHE_IS_NOT_WRITING_TO_FILE("FileCache could not write to the File"),

    /**
     * Wrong cache file format.
     */
    DEVICE_FILE_CACHE_FORMAT_IS_WRONG("Wrong cache file format"),

    /**
     * Connection is not available.
     */
    DEVICE_CONNECTION_IS_NOT_AVAILABLE("Connection is not available"),

    /**
     * PRNG fixes are not applied.
     */
    DEVICE_PRNG_FIX_ERROR("PRNG fixes are not applied"),

    /**
     * IdToken is normally returned from token endpoint.
     */
    IDTOKEN_PARSING_FAILURE("Cannot parse IdToken"),
    
    /**
     * Dateformat is invalid.
     */
    DATE_PARSING_FAILURE("Cannot parse date"),

    /**
     * Authorization code not exchanged for token.
     */
    AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN("Authorization code not exchanged for token"),

    /**
     * Cancel message is not successfully delivered to broadcast receiver. It
     * may be not registered yet. AuthenticationActivity will register that at
     * onResume.
     */
    BROADCAST_CANCEL_NOT_SUCCESSFUL(
            "Cancel message is not successfully delivered to broadcast receiver."),

    /**
     * Correlationid is not in UUID format.
     */
    CORRELATION_ID_FORMAT("Correlationid is not in UUID format"),

    /**
     * Correlationid provided in request is not matching the response.
     */
    CORRELATION_ID_NOT_MATCHING_REQUEST_RESPONSE(
            "Correlationid provided in request is not matching the response"),

    /**
     * Encoding format is not supported.
     */
    ENCODING_IS_NOT_SUPPORTED("Encoding format is not supported"),

    /**
     * Server returned invalid JSON response.
     */
    SERVER_INVALID_JSON_RESPONSE("Server returned invalid JSON response"),

    /**
     * Refresh token request failed.
     */
    AUTH_REFRESH_FAILED("Refresh token request failed"),

    /**
     * Encryption failed.
     */
    ENCRYPTION_FAILED("Encryption failed"),
    
    /**
     * Decryption failed.
     */
    DECRYPTION_FAILED("Decryption failed"),

    /**
     * Failed to use AndroidKeyStore.
     */
    ANDROIDKEYSTORE_FAILED("Failed to use AndroidKeyStore"),
    
    /**
     * Failed to use KeyPairGeneratorSpec.
     */
    ANDROIDKEYSTORE_KEYPAIR_GENERATOR_FAILED("Failed to use KeyPairGeneratorSpec"),

    /**
     * Authority validation is not supported for ADFS authority. Authority
     * validation needs to be disabled for ADFS.
     */
    DISCOVERY_NOT_SUPPORTED("Authority validation is not supported for ADFS authority."),

    /**
     * Broker is not installed in your system.
     */
    BROKER_PACKAGE_NAME_NOT_FOUND("Broker is not installed in your system"),

    /**
     * Authenticator is not responding.
     */
    BROKER_AUTHENTICATOR_NOT_RESPONDING("Authenticator is not responding"),

    /**
     * Authenticator error.
     */
    BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN("Authenticator error"),

    /**
     * Invalid arguments for Authenticator request.
     */
    BROKER_AUTHENTICATOR_BAD_ARGUMENTS("Invalid arguments for Authenticator request"),

    /**
     * Authentication request failed.
     */
    BROKER_AUTHENTICATOR_BAD_AUTHENTICATION("Authentication request failed"),

    /**
     * Authenticator is not supporting this operation.
     */
    BROKER_AUTHENTICATOR_UNSUPPORTED_OPERATION("Authenticator is not supporting this operation"),

    /**
     * Authenticator has IO Exception.
     */
    BROKER_AUTHENTICATOR_IO_EXCEPTION("Authenticator has IO Exception"),
    
    /**
     * Authenticator returned exception.
     */
    BROKER_AUTHENTICATOR_EXCEPTION("Authenticator has an Exception"),

    /**
     * Signature could not be verified.
     */
    BROKER_VERIFICATION_FAILED("Signature could not be verified"),

    /**
     * Package name is not resolved.
     */
    PACKAGE_NAME_NOT_FOUND("Package name is not resolved"),

    /**
     * Error in generating hash with MessageDigest.
     */
    DIGEST_ERROR("Error in generating hash with MessageDigest"),

    /**
     * Authentication request is null.
     */
    BROKER_AUTHENTICATION_REQUEST_IS_NULL("Authentication request is null"),

    /**
     * Calling app could not be verified.
     */
    BROKER_APP_VERIFICATION_FAILED("Calling app could not be verified"),

    /**
     * Activity information is not retrieved.
     */
    BROKER_ACTIVITY_INFO_NOT_FOUND("Activity information is not retrieved"),

    /**
     * Signature is not saved.
     */
    BROKER_SIGNATURE_NOT_SAVED("Signature is not saved"),

    /**
     * Device does not support the algorithm.
     */
    DEVICE_NO_SUCH_ALGORITHM("Device does not support the algorithm"),

    /**
     * Requested padding is not available.
     */
    DEVICE_ALGORITHM_PADDING_EXCEPTION("Requested padding is not available"),

    /**
     * App package name is not found in the package manager.
     */
    APP_PACKAGE_NAME_NOT_FOUND("App package name is not found in the package manager"),

    /**
     * Encryption related error.
     */
    ENCRYPTION_ERROR("Encryption related error"),

    /**
     * Broker activity is not resolved.
     */
    BROKER_ACTIVITY_IS_NOT_RESOLVED("Broker activity is not resolved"),

    /**
     * Invalid request parameters.
     */
    BROKER_ACTIVITY_INVALID_REQUEST("Invalid request parameters"),

    /**
     * Broker could not save the new account.
     */
    BROKER_ACCOUNT_SAVE_FAILED("Broker could not save the new account"),

    /**
     * Broker account does not exist.
     */
    BROKER_ACCOUNT_DOES_NOT_EXIST("Broker account does not exist"),

    /**
     * Single user is expected.
     */
    BROKER_SINGLE_USER_EXPECTED("Single user is expected"),

    /**
     * Key Chain private key exception.
     */
    KEY_CHAIN_PRIVATE_KEY_EXCEPTION("Key Chain private key exception"),

    /**
     * Signature exception.
     */
    SIGNATURE_EXCEPTION("Signature exception"),

    /**
     * It is failed to create device certificate response.
     */
    DEVICE_CERTIFICATE_RESPONSE_FAILED("It is failed to create device certificate response"),

    /**
     * WebView returned Authentication Exception.
     */
    WEBVIEW_RETURNED_AUTHENTICATION_EXCEPTION("Webview returned Authentication Exception"),

    /**
     * WebView returned invalid or null Authentication Exception.
     */
    WEBVIEW_RETURNED_INVALID_AUTHENTICATION_EXCEPTION(
            "Webview returned invalid or null Authentication Exception"),

    /**
     * WebView returned empty redirect url.
     */
    WEBVIEW_RETURNED_EMPTY_REDIRECT_URL("Webview returned empty redirect url"),
    
    /**
     * WebView  redirect url is not SSL protected.
     */
    WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED("The webview was redirected to an unsafe URL"),

    /**
     * Device certificate API has exception.
     */
    DEVICE_CERTIFICATE_API_EXCEPTION("Device certificate API has exception"),

    /**
     * Device certificate request is valid.
     */
    DEVICE_CERTIFICATE_REQUEST_INVALID("Device certificate request is valid"),

    /**
     * Resource is not found in your project. Please include resource files.
     */
    RESOURCE_NOT_FOUND("Resource is not found in your project. Please include resource files."),

    /**
     * Certificate encoding is not generated.
     */
    CERTIFICATE_ENCODING_ERROR("Certificate encoding is not generated"),

    /**
     * Error in silent token request.
     */
    ERROR_SILENT_REQUEST("Error in silent token request"),

    /**
     * The redirectUri for broker is invalid.
     */
    DEVELOPER_REDIRECTURI_INVALID("The redirectUri for broker is invalid"),

    /**
     * Device challenge failure.
     */
    DEVICE_CHALLENGE_FAILURE("Device challenge failure"),

    /**
     * Resource authentication challenge failure.
     */
    RESOURCE_AUTHENTICATION_CHALLENGE_FAILURE("Resource authentication challenge failure"), 
    
    /**
     * The token cache item is invalid, cannot use it to create cachekey. 
     */
    INVALID_TOKEN_CACHE_ITEM("Invalid token cache item"),

    /**
     * Export of FID failure.
     */
    FAIL_TO_EXPORT("Fail to export"),
    
    /**
     * Import of FID failure.
     */
    FAIL_TO_IMPORT("Fail to import"),
        
    /**
     * Incompatible blob version.
     */
    INCOMPATIBLE_BLOB_VERSION("Fail to deserialize because the blob version is incompatible"), 
    
    /**
     * Fail to get the token cache item from the cache item.
     */
    TOKEN_CACHE_ITEM_NOT_FOUND("Token cache item is not found"),
    
    /**
     * Fail to parse JSON because of the problem with the JSON API.
     */
    JSON_PARSE_ERROR("Fail to parse JSON"),

    /**
     * Malformed DRS metadata URL.
     */
    DRS_METADATA_URL_INVALID("Malformed DRS metadata URL"),

    /**
     * Enrollment server returned an unrecognized response.
     */
    DRS_FAILED_SERVER_ERROR("Enrollment server returned an unrecognized response"),

    /**
     * DRS discovery failed: unknown host.
     */
    DRS_DISCOVERY_FAILED_UNKNOWN_HOST("DRS discovery failed: unknown host"),

    /**
     *  Broker is not installed. The process is kicked off to to install broker but ADAL cannot wait for it to finish.
     */
    BROKER_APP_INSTALLATION_STARTED("Broker app installation started");

    private String mDescription;

    ADALError(String message) {
        mDescription = message;
    }

    /**
     * Gets error description.
     * 
     * @return Error description
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Gets localized description if provided with context.
     * 
     * @param context {@link Context}
     * @return Error description
     */
    public String getLocalizedDescription(Context context) {
        // Optional overwrite to error descriptions from resource files.
        // Application can repeat the resource entries from libraries.
        // Application resource
        // merging operation will use the last one according to the import
        // order.
        if (context != null) {
            Configuration conf = context.getResources().getConfiguration();
            Resources resources = new Resources(context.getAssets(), context.getResources()
                    .getDisplayMetrics(), conf);
            return resources.getString(resources.getIdentifier(this.name(), "string",
                    context.getPackageName()));
        }
        return mDescription;
    }
}
