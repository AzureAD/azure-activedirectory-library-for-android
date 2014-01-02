
package com.microsoft.adal;

public enum ADALError {
    /**
     * Authority is not valid instance
     */
    DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE("Authority is not valid instance"),
    /**
     * Authority url is not valid
     */
    DEVELOPER_AUTHORITY_IS_NOT_VALID_URL("Authority url is not valid"),

    /**
     * Authority is empty
     */
    DEVELOPER_AUTHORITY_IS_EMPTY("Authority is empty"),

    /**
     * Async tasks can only be executed one time. They are not supposed to be
     * reused.
     */
    DEVELOPER_ASYNC_TASK_REUSED(
            "Async tasks can only be executed one time. They are not supposed to be reused."),

    /**
     * Resource is empty
     */
    DEVELOPER_RESOURCE_IS_EMPTY("Resource is empty"),

    /**
     * Context is not provided
     */
    DEVELOPER_CONTEXT_IS_NOT_PROVIDED("Context is not provided"),

    /**
     * Key/value pair list contains redundant items in the header
     */
    DEVELOPER_BEARER_HEADER_MULTIPLE_ITEMS(
            "Key/value pair list contains redundant items in the header"),

    /**
     * Active callback is not found
     */
    CALLBACK_IS_NOT_FOUND("Active callback is not found"),

    /**
     * Activity is not resolved
     */
    DEVELOPER_ACTIVITY_IS_NOT_RESOLVED("Activity is not resolved"),
    /**
     * Invalid request to server
     */
    SERVER_INVALID_REQUEST("Invalid request to server"),

    SERVER_ERROR("Server returned an error"),

    IO_EXCEPTION("I/O exception"),

    ARGUMENT_EXCEPTION("Invalid argument"),

    /**
     * webview has ssl related error
     */
    ERROR_FAILED_SSL_HANDSHAKE("Webview returned error for SSL"),

    /**
     * webview has an error
     */
    ERROR_WEBVIEW("Webview returned an error"),

    /**
     * Broadcast receiver has an error
     */
    BROADCAST_RECEIVER_ERROR("Broadcast receiver has an error"),

    /**
     * Authorization Failed
     */
    AUTH_FAILED("Authorization failed"),

    /**
     * Refresh token is failed and prompt is not allowed
     */
    AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED("Refresh token is failed and prompt is not allowed"),

    /**
     * The Authorization Server returned an unrecognized response
     */
    AUTH_FAILED_SERVER_ERROR("The Authorization Server returned an unrecognized response"),

    /**
     * The required resource bundle could not be loaded
     */
    AUTH_FAILED_NO_RESOURCES("The required resource bundle could not be loaded"),

    /**
     * The authorization server response has incorrectly encoded state
     */
    AUTH_FAILED_NO_STATE("The authorization server response has incorrectly encoded state"),

    /**
     * The authorization server response has no encoded state
     */
    AUTH_FAILED_BAD_STATE("The authorization server response has no encoded state"),

    /**
     * The requested access token could not be found
     */
    AUTH_FAILED_NO_TOKEN("The requested access token could not be found"),

    /**
     * The user cancelled the authorization request
     */
    AUTH_FAILED_CANCELLED("The user cancelled the authorization request"),

    /**
     * Invalid parameters for authorization operation
     */
    AUTH_FAILED_INTERNAL_ERROR("Invalid parameters for authorization operation"),

    /**
     * Internet permissions are not set for the app
     */
    DEVICE_INTERNET_IS_NOT_AVAILABLE("Internet permissions are not set for the app"),

    /**
     * onActivityResult is called with null intent data
     */
    ON_ACTIVITY_RESULT_INTENT_NULL("onActivityResult is called with null intent data"),

    /**
     * Shared preferences are not available
     */
    DEVICE_SHARED_PREF_IS_NOT_AVAILABLE("Shared preferences are not available"),

    /**
     * Cache is not saving the changes. This error will be returned to
     * developer, if cache returns error
     */
    DEVICE_CACHE_IS_NOT_WORKING(
            "Cache is not saving the changes. This error will be returned to developer, if cache returns error"),

    /**
     * Cache is not loaded from File
     */
    DEVICE_FILE_CACHE_IS_NOT_LOADED_FROM_FILE("Cache is not loaded from File"),

    /**
     * FileCache could not write to the File
     */
    DEVICE_FILE_CACHE_IS_NOT_WRITING_TO_FILE("FileCache could not write to the File"),

    /**
     * Cache file format is wrong.
     */
    DEVICE_FILE_CACHE_FORMAT_IS_WRONG("Cache file format is wrong"),

    /**
     * IdToken is failed to parse
     */
    IDTOKEN_PARSING_FAILURE("IdToken is failed to parse"),

    /**
     * Authorization code not exchanged for token
     */
    AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN("Authorization code not exchanged for token"),

    /**
     * Cancel message is not successfully delivered to broadcast receiver. It
     * may be not registered yet. AuthenticationActivity will register that at
     * onresume.
     */
    BROADCAST_CANCEL_NOT_SUCCESSFUL(
            "Cancel message is not successfully delivered to broadcast receiver. It may be not registered yet. AuthenticationActivity will register that at onResume."),

    /**
     * Correlationid is not in GUID format
     */
    CORRELATION_ID_FORMAT("Correlationid is not in GUID format"),

    ENCODING_IS_NOT_SUPPORTED("Encoding format is not supported"),

    SERVER_INVALID_JSON_RESPONSE("Server returned invalid json response"), 
    
    AUTH_REFRESH_FAILED("Refresh token request failed"), ;

    private String mDescription;

    private ADALError(String message) {
        mDescription = message;
    }

    public String getDescription() {
        return mDescription;
    }
    
    public static ADALError parseErrorCode(String code){
        
        
        return ADALError.AUTH_FAILED_SERVER_ERROR;
    }
}
