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
    DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED(com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED),

    /**
     * Authority is not a valid instance.
     */
    DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE(com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE),

    /**
     * Authority url is not valid.
     */
    DEVELOPER_AUTHORITY_IS_NOT_VALID_URL(com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL),

    /**
     * Authority is empty.
     */
    DEVELOPER_AUTHORITY_IS_EMPTY(com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_AUTHORITY_IS_EMPTY),

    /**
     * Async tasks can only be executed one time. They are not supposed to be
     * reused.
     *
     */
    DEVELOPER_ASYNC_TASK_REUSED(
            com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_ASYNC_TASK_REUSED),

    /**
     * Resource is empty.
     */
    DEVELOPER_RESOURCE_IS_EMPTY(com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_RESOURCE_IS_EMPTY),

    /**
     * Context is not provided.
     */
    DEVELOPER_CONTEXT_IS_NOT_PROVIDED(com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_CONTEXT_IS_NOT_PROVIDED),

    /**
     * Key/value pair list contains redundant items in the header.
     */
    DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS(
            com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS),

    /**
     * Active callback is not found.
     */
    CALLBACK_IS_NOT_FOUND(com.microsoft.identity.common.adal.error.ADALError.CALLBACK_IS_NOT_FOUND),

    /**
     * Activity is not resolved.
     */
    DEVELOPER_ACTIVITY_IS_NOT_RESOLVED(
            com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED),

    /**
     * android.permission.INTERNET is not added to AndroidManifest file.
     */
    DEVELOPER_INTERNET_PERMISSION_MISSING(
            com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_INTERNET_PERMISSION_MISSING),

    /**
     * GET_ACCOUNTS, MANAGE_ACCOUNTS, USE_CREDENTIALS are not added to
     * AndroidManifest file.
     */
    DEVELOPER_BROKER_PERMISSIONS_MISSING(
            com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING),

    /**
     * Calling from main thread for background operation.
     */
    DEVELOPER_CALLING_ON_MAIN_THREAD(com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_CALLING_ON_MAIN_THREAD),

    /**
     * Layout file does not have correct elements such as different webview id.
     */
    DEVELOPER_DIALOG_LAYOUT_INVALID(com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_DIALOG_LAYOUT_INVALID),

    /**
     * Invalid request to server.
     */
    SERVER_INVALID_REQUEST(com.microsoft.identity.common.adal.error.ADALError.SERVER_INVALID_REQUEST),

    /**
     * Server returned an error.
     */
    SERVER_ERROR(com.microsoft.identity.common.adal.error.ADALError.SERVER_ERROR),

    /**
     * I/O exception.
     */
    IO_EXCEPTION(com.microsoft.identity.common.adal.error.ADALError.IO_EXCEPTION),

    /**
     * Invalid argument.
     */
    ARGUMENT_EXCEPTION(com.microsoft.identity.common.adal.error.ADALError.ARGUMENT_EXCEPTION),

    /**
     * WebView has ssl related error and returned error code for that.
     */
    ERROR_FAILED_SSL_HANDSHAKE(com.microsoft.identity.common.adal.error.ADALError.ERROR_FAILED_SSL_HANDSHAKE),

    /**
     * WebView has some generic error.
     */
    ERROR_WEBVIEW(com.microsoft.identity.common.adal.error.ADALError.ERROR_WEBVIEW),

    /**
     * Request object is needed to start {@link AuthenticationActivity}.
     */
    ACTIVITY_REQUEST_INTENT_DATA_IS_NULL(com.microsoft.identity.common.adal.error.ADALError.ACTIVITY_REQUEST_INTENT_DATA_IS_NULL),

    /**
     * Broadcast receiver has an error.
     */
    BROADCAST_RECEIVER_ERROR(com.microsoft.identity.common.adal.error.ADALError.BROADCAST_RECEIVER_ERROR),

    /**
     * Authorization failed.
     */
    AUTH_FAILED(com.microsoft.identity.common.adal.error.ADALError.AUTH_FAILED),

    /**
     * Refresh token is failed and prompt is not allowed.
     */
    AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED(com.microsoft.identity.common.adal.error.ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED),

    /**
     * The Authorization Server returned an unrecognized response.
     */
    AUTH_FAILED_SERVER_ERROR(com.microsoft.identity.common.adal.error.ADALError.AUTH_FAILED_SERVER_ERROR),

    /**
     * The required resource bundle could not be loaded.
     */
    AUTH_FAILED_NO_RESOURCES(com.microsoft.identity.common.adal.error.ADALError.AUTH_FAILED_NO_RESOURCES),

    /**
     * The authorization server response has incorrectly encoded state.
     */
    AUTH_FAILED_NO_STATE(com.microsoft.identity.common.adal.error.ADALError.AUTH_FAILED_NO_STATE),

    /**
     * The authorization server response has no encoded state.
     */
    AUTH_FAILED_BAD_STATE(com.microsoft.identity.common.adal.error.ADALError.AUTH_FAILED_BAD_STATE),

    /**
     * The requested access token could not be found.
     */
    AUTH_FAILED_NO_TOKEN(com.microsoft.identity.common.adal.error.ADALError.AUTH_FAILED_NO_TOKEN),

    /**
     * The user cancelled the authorization request.
     */
    AUTH_FAILED_CANCELLED(com.microsoft.identity.common.adal.error.ADALError.AUTH_FAILED_CANCELLED),

    /**
     * Invalid parameters for authorization operation.
     */
    AUTH_FAILED_INTERNAL_ERROR(com.microsoft.identity.common.adal.error.ADALError.AUTH_FAILED_INTERNAL_ERROR),

    /**
     * User returned by service does not match the one in the request.
     */
    AUTH_FAILED_USER_MISMATCH(com.microsoft.identity.common.adal.error.ADALError.AUTH_FAILED_USER_MISMATCH),

    /**
     * Internet permissions are not set for the app.
     */
    DEVICE_INTERNET_IS_NOT_AVAILABLE(com.microsoft.identity.common.adal.error.ADALError.DEVICE_INTERNET_IS_NOT_AVAILABLE),

    /**
     * Unable to access the network due to power optimizations.
     */
    NO_NETWORK_CONNECTION_POWER_OPTIMIZATION(com.microsoft.identity.common.adal.error.ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION),

    /**
     * onActivityResult is called with null intent data. Activity may be
     * terminated directly.
     */
    ON_ACTIVITY_RESULT_INTENT_NULL(com.microsoft.identity.common.adal.error.ADALError.ON_ACTIVITY_RESULT_INTENT_NULL),

    /**
     * onActivityResult is called, but callback is not found.
     */
    ON_ACTIVITY_RESULT_CALLBACK_NOT_FOUND(com.microsoft.identity.common.adal.error.ADALError.ON_ACTIVITY_RESULT_CALLBACK_NOT_FOUND),

    /**
     * Shared preferences are not available.
     */
    DEVICE_SHARED_PREF_IS_NOT_AVAILABLE(com.microsoft.identity.common.adal.error.ADALError.DEVICE_SHARED_PREF_IS_NOT_AVAILABLE),

    /**
     * Cache is not saving the changes. This error will be returned to
     * developer, if cache returns error.
     */
    DEVICE_CACHE_IS_NOT_WORKING(com.microsoft.identity.common.adal.error.ADALError.DEVICE_CACHE_IS_NOT_WORKING),

    /**
     * Cache is not loaded from File.
     */
    DEVICE_FILE_CACHE_IS_NOT_LOADED_FROM_FILE(com.microsoft.identity.common.adal.error.ADALError.DEVICE_FILE_CACHE_IS_NOT_LOADED_FROM_FILE),

    /**
     * FileCache could not write to the File.
     */
    DEVICE_FILE_CACHE_IS_NOT_WRITING_TO_FILE(com.microsoft.identity.common.adal.error.ADALError.DEVICE_FILE_CACHE_IS_NOT_WRITING_TO_FILE),

    /**
     * Wrong cache file format.
     */
    DEVICE_FILE_CACHE_FORMAT_IS_WRONG(com.microsoft.identity.common.adal.error.ADALError.DEVICE_FILE_CACHE_FORMAT_IS_WRONG),

    /**
     * Connection is not available.
     */
    DEVICE_CONNECTION_IS_NOT_AVAILABLE(com.microsoft.identity.common.adal.error.ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE),

    /**
     * PRNG fixes are not applied.
     */
    DEVICE_PRNG_FIX_ERROR(com.microsoft.identity.common.adal.error.ADALError.DEVICE_PRNG_FIX_ERROR),

    /**
     * IdToken is normally returned from token endpoint.
     */
    IDTOKEN_PARSING_FAILURE(com.microsoft.identity.common.adal.error.ADALError.IDTOKEN_PARSING_FAILURE),

    /**
     * Dateformat is invalid.
     */
    DATE_PARSING_FAILURE(com.microsoft.identity.common.adal.error.ADALError.DATE_PARSING_FAILURE),

    /**
     * Authorization code not exchanged for token.
     */
    AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN(com.microsoft.identity.common.adal.error.ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN),

    /**
     * Cancel message is not successfully delivered to broadcast receiver. It
     * may be not registered yet. AuthenticationActivity will register that at
     * onResume.
     */
    BROADCAST_CANCEL_NOT_SUCCESSFUL(
            com.microsoft.identity.common.adal.error.ADALError.BROADCAST_CANCEL_NOT_SUCCESSFUL),

    /**
     * Correlationid is not in UUID format.
     */
    CORRELATION_ID_FORMAT(com.microsoft.identity.common.adal.error.ADALError.CORRELATION_ID_FORMAT),

    /**
     * Correlationid provided in request is not matching the response.
     */
    CORRELATION_ID_NOT_MATCHING_REQUEST_RESPONSE(
            com.microsoft.identity.common.adal.error.ADALError.CORRELATION_ID_NOT_MATCHING_REQUEST_RESPONSE),

    /**
     * Encoding format is not supported.
     */
    ENCODING_IS_NOT_SUPPORTED(com.microsoft.identity.common.adal.error.ADALError.ENCODING_IS_NOT_SUPPORTED),

    /**
     * Server returned invalid JSON response.
     */
    SERVER_INVALID_JSON_RESPONSE(com.microsoft.identity.common.adal.error.ADALError.SERVER_INVALID_JSON_RESPONSE),

    /**
     * Refresh token request failed.
     */
    AUTH_REFRESH_FAILED(com.microsoft.identity.common.adal.error.ADALError.AUTH_REFRESH_FAILED),

    /**
     * Encryption failed.
     */
    ENCRYPTION_FAILED(com.microsoft.identity.common.adal.error.ADALError.ENCRYPTION_FAILED),

    /**
     * Decryption failed.
     */
    DECRYPTION_FAILED(com.microsoft.identity.common.adal.error.ADALError.DECRYPTION_FAILED),

    /**
     * Failed to use AndroidKeyStore.
     */
    ANDROIDKEYSTORE_FAILED(com.microsoft.identity.common.adal.error.ADALError.ANDROIDKEYSTORE_FAILED),

    /**
     * Failed to use KeyPairGeneratorSpec.
     */
    ANDROIDKEYSTORE_KEYPAIR_GENERATOR_FAILED(com.microsoft.identity.common.adal.error.ADALError.ANDROIDKEYSTORE_KEYPAIR_GENERATOR_FAILED),

    /**
     * Authority validation is not supported for ADFS authority. Authority
     * validation needs to be disabled for ADFS.
     */
    DISCOVERY_NOT_SUPPORTED(com.microsoft.identity.common.adal.error.ADALError.DISCOVERY_NOT_SUPPORTED),

    /**
     * Broker is not installed in your system.
     */
    BROKER_PACKAGE_NAME_NOT_FOUND(com.microsoft.identity.common.adal.error.ADALError.BROKER_PACKAGE_NAME_NOT_FOUND),

    /**
     * Authenticator is not responding.
     */
    BROKER_AUTHENTICATOR_NOT_RESPONDING(com.microsoft.identity.common.adal.error.ADALError.BROKER_AUTHENTICATOR_NOT_RESPONDING),

    /**
     * Authenticator error.
     */
    BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN(com.microsoft.identity.common.adal.error.ADALError.BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN),

    /**
     * Invalid arguments for Authenticator request.
     */
    BROKER_AUTHENTICATOR_BAD_ARGUMENTS(com.microsoft.identity.common.adal.error.ADALError.BROKER_AUTHENTICATOR_BAD_ARGUMENTS),

    /**
     * Authentication request failed.
     */
    BROKER_AUTHENTICATOR_BAD_AUTHENTICATION(com.microsoft.identity.common.adal.error.ADALError.BROKER_AUTHENTICATOR_BAD_AUTHENTICATION),

    /**
     * Authenticator is not supporting this operation.
     */
    BROKER_AUTHENTICATOR_UNSUPPORTED_OPERATION(com.microsoft.identity.common.adal.error.ADALError.BROKER_AUTHENTICATOR_UNSUPPORTED_OPERATION),

    /**
     * Authenticator has IO Exception.
     */
    BROKER_AUTHENTICATOR_IO_EXCEPTION(com.microsoft.identity.common.adal.error.ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION),

    /**
     * Authenticator returned exception.
     */
    BROKER_AUTHENTICATOR_EXCEPTION(com.microsoft.identity.common.adal.error.ADALError.BROKER_AUTHENTICATOR_EXCEPTION),

    /**
     * Signature could not be verified.
     */
    BROKER_VERIFICATION_FAILED(com.microsoft.identity.common.adal.error.ADALError.BROKER_VERIFICATION_FAILED),

    /**
     * Package name is not resolved.
     */
    PACKAGE_NAME_NOT_FOUND(com.microsoft.identity.common.adal.error.ADALError.PACKAGE_NAME_NOT_FOUND),

    /**
     * Error in generating hash with MessageDigest.
     */
    DIGEST_ERROR(com.microsoft.identity.common.adal.error.ADALError.DIGEST_ERROR),

    /**
     * Authentication request is null.
     */
    BROKER_AUTHENTICATION_REQUEST_IS_NULL(com.microsoft.identity.common.adal.error.ADALError.BROKER_AUTHENTICATION_REQUEST_IS_NULL),

    /**
     * Calling app could not be verified.
     */
    BROKER_APP_VERIFICATION_FAILED(com.microsoft.identity.common.adal.error.ADALError.BROKER_APP_VERIFICATION_FAILED),

    /**
     * Activity information is not retrieved.
     */
    BROKER_ACTIVITY_INFO_NOT_FOUND(com.microsoft.identity.common.adal.error.ADALError.BROKER_ACTIVITY_INFO_NOT_FOUND),

    /**
     * Signature is not saved.
     */
    BROKER_SIGNATURE_NOT_SAVED(com.microsoft.identity.common.adal.error.ADALError.BROKER_SIGNATURE_NOT_SAVED),

    /**
     * Device does not support the algorithm.
     */
    DEVICE_NO_SUCH_ALGORITHM(com.microsoft.identity.common.adal.error.ADALError.DEVICE_NO_SUCH_ALGORITHM),

    /**
     * Requested padding is not available.
     */
    DEVICE_ALGORITHM_PADDING_EXCEPTION(com.microsoft.identity.common.adal.error.ADALError.DEVICE_ALGORITHM_PADDING_EXCEPTION),

    /**
     * App package name is not found in the package manager.
     */
    APP_PACKAGE_NAME_NOT_FOUND(com.microsoft.identity.common.adal.error.ADALError.APP_PACKAGE_NAME_NOT_FOUND),

    /**
     * Encryption related error.
     */
    ENCRYPTION_ERROR(com.microsoft.identity.common.adal.error.ADALError.ENCRYPTION_ERROR),

    /**
     * Broker activity is not resolved.
     */
    BROKER_ACTIVITY_IS_NOT_RESOLVED(com.microsoft.identity.common.adal.error.ADALError.BROKER_ACTIVITY_IS_NOT_RESOLVED),

    /**
     * Invalid request parameters.
     */
    BROKER_ACTIVITY_INVALID_REQUEST(com.microsoft.identity.common.adal.error.ADALError.BROKER_ACTIVITY_INVALID_REQUEST),

    /**
     * Broker could not save the new account.
     */
    BROKER_ACCOUNT_SAVE_FAILED(com.microsoft.identity.common.adal.error.ADALError.BROKER_ACCOUNT_SAVE_FAILED),

    /**
     * Broker account does not exist.
     */
    BROKER_ACCOUNT_DOES_NOT_EXIST(com.microsoft.identity.common.adal.error.ADALError.BROKER_ACCOUNT_DOES_NOT_EXIST),

    /**
     * Single user is expected.
     */
    BROKER_SINGLE_USER_EXPECTED(com.microsoft.identity.common.adal.error.ADALError.BROKER_SINGLE_USER_EXPECTED),

    /**
     * Key Chain private key exception.
     */
    KEY_CHAIN_PRIVATE_KEY_EXCEPTION(com.microsoft.identity.common.adal.error.ADALError.KEY_CHAIN_PRIVATE_KEY_EXCEPTION),

    /**
     * Signature exception.
     */
    SIGNATURE_EXCEPTION(com.microsoft.identity.common.adal.error.ADALError.SIGNATURE_EXCEPTION),

    /**
     * It is failed to create device certificate response.
     */
    DEVICE_CERTIFICATE_RESPONSE_FAILED(com.microsoft.identity.common.adal.error.ADALError.DEVICE_CERTIFICATE_RESPONSE_FAILED),

    /**
     * WebView returned Authentication Exception.
     */
    WEBVIEW_RETURNED_AUTHENTICATION_EXCEPTION(com.microsoft.identity.common.adal.error.ADALError.WEBVIEW_RETURNED_AUTHENTICATION_EXCEPTION),

    /**
     * WebView returned invalid or null Authentication Exception.
     */
    WEBVIEW_RETURNED_INVALID_AUTHENTICATION_EXCEPTION(
            com.microsoft.identity.common.adal.error.ADALError.WEBVIEW_RETURNED_INVALID_AUTHENTICATION_EXCEPTION),

    /**
     * WebView returned empty redirect url.
     */
    WEBVIEW_RETURNED_EMPTY_REDIRECT_URL(com.microsoft.identity.common.adal.error.ADALError.WEBVIEW_RETURNED_EMPTY_REDIRECT_URL),

    /**
     * WebView  redirect url is not SSL protected.
     */
    WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED(com.microsoft.identity.common.adal.error.ADALError.WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED),

    /**
     * Device certificate API has exception.
     */
    DEVICE_CERTIFICATE_API_EXCEPTION(com.microsoft.identity.common.adal.error.ADALError.DEVICE_CERTIFICATE_API_EXCEPTION),

    /**
     * Device certificate request is valid.
     */
    DEVICE_CERTIFICATE_REQUEST_INVALID(com.microsoft.identity.common.adal.error.ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID),

    /**
     * Resource is not found in your project. Please include resource files.
     */
    RESOURCE_NOT_FOUND(com.microsoft.identity.common.adal.error.ADALError.RESOURCE_NOT_FOUND),

    /**
     * Certificate encoding is not generated.
     */
    CERTIFICATE_ENCODING_ERROR(com.microsoft.identity.common.adal.error.ADALError.CERTIFICATE_ENCODING_ERROR),

    /**
     * Error in silent token request.
     */
    ERROR_SILENT_REQUEST(com.microsoft.identity.common.adal.error.ADALError.ERROR_SILENT_REQUEST),

    /**
     * The redirectUri for broker is invalid.
     */
    DEVELOPER_REDIRECTURI_INVALID(com.microsoft.identity.common.adal.error.ADALError.DEVELOPER_REDIRECTURI_INVALID),

    /**
     * Device challenge failure.
     */
    DEVICE_CHALLENGE_FAILURE(com.microsoft.identity.common.adal.error.ADALError.DEVICE_CHALLENGE_FAILURE),

    /**
     * Resource authentication challenge failure.
     */
    RESOURCE_AUTHENTICATION_CHALLENGE_FAILURE(com.microsoft.identity.common.adal.error.ADALError.RESOURCE_AUTHENTICATION_CHALLENGE_FAILURE),

    /**
     * The token cache item is invalid, cannot use it to create cachekey. 
     */
    INVALID_TOKEN_CACHE_ITEM(com.microsoft.identity.common.adal.error.ADALError.INVALID_TOKEN_CACHE_ITEM),

    /**
     * Export of FID failure.
     */
    FAIL_TO_EXPORT(com.microsoft.identity.common.adal.error.ADALError.FAIL_TO_EXPORT),

    /**
     * Import of FID failure.
     */
    FAIL_TO_IMPORT(com.microsoft.identity.common.adal.error.ADALError.FAIL_TO_IMPORT),

    /**
     * Incompatible blob version.
     */
    INCOMPATIBLE_BLOB_VERSION(com.microsoft.identity.common.adal.error.ADALError.INCOMPATIBLE_BLOB_VERSION),

    /**
     * Fail to get the token cache item from the cache item.
     */
    TOKEN_CACHE_ITEM_NOT_FOUND(com.microsoft.identity.common.adal.error.ADALError.TOKEN_CACHE_ITEM_NOT_FOUND),

    /**
     * Fail to parse JSON because of the problem with the JSON API.
     */
    JSON_PARSE_ERROR(com.microsoft.identity.common.adal.error.ADALError.JSON_PARSE_ERROR),

    /**
     * Malformed DRS metadata URL.
     */
    DRS_METADATA_URL_INVALID(com.microsoft.identity.common.adal.error.ADALError.DRS_METADATA_URL_INVALID),

    /**
     * Enrollment server returned an unrecognized response.
     */
    DRS_FAILED_SERVER_ERROR(com.microsoft.identity.common.adal.error.ADALError.DRS_FAILED_SERVER_ERROR),

    /**
     * DRS discovery failed: unknown host.
     */
    DRS_DISCOVERY_FAILED_UNKNOWN_HOST(com.microsoft.identity.common.adal.error.ADALError.DRS_DISCOVERY_FAILED_UNKNOWN_HOST),

    /**
     *  Broker is not installed. The process is kicked off to to install broker but ADAL cannot wait for it to finish.
     */
    BROKER_APP_INSTALLATION_STARTED(com.microsoft.identity.common.adal.error.ADALError.BROKER_APP_INSTALLATION_STARTED),

    /**
     * The version field of x-ms-clitelem contained an unknown or unsupported value.
     */
    X_MS_CLITELEM_VERSION_UNRECOGNIZED(com.microsoft.identity.common.adal.error.ADALError.X_MS_CLITELEM_VERSION_UNRECOGNIZED),

    /**
     * The value of the x-ms-clitelem header contained malformed data.
     */
    X_MS_CLITELEM_MALFORMED(com.microsoft.identity.common.adal.error.ADALError.X_MS_CLITELEM_MALFORMED),

    /**
     * Failed to bind the service in broker app.
     */
    BROKER_BIND_SERVICE_FAILED(com.microsoft.identity.common.adal.error.ADALError.BROKER_BIND_SERVICE_FAILED);

    private final com.microsoft.identity.common.adal.error.ADALError mCommonADALError;

    ADALError(com.microsoft.identity.common.adal.error.ADALError error) {
        mCommonADALError = error;
    }

    public static ADALError fromCommon(com.microsoft.identity.common.adal.error.ADALError error){
        for(ADALError err : ADALError.values()){
            if(err.mCommonADALError.equals(error)){
                return err;
            }
        }
        //TODO: Return unknown error mapping error
        return null;
    }

    /**
     * Gets error description.
     *
     * @return Error description
     */
    public String getDescription() {
        return mCommonADALError.getDescription();
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
        return mCommonADALError.getDescription();
    }
}