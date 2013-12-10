
package com.microsoft.adal;

public enum ADALError {
    /**
     * Authority is not valid instance
     */
    DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE,
    /**
     * Authority url is not valid
     */
    DEVELOPER_AUTHORITY_IS_NOT_VALID_URL,

    /**
     * Authority is empty
     */
    DEVELOPER_AUTHORITY_IS_EMPTY,

    /**
     * Async tasks can only be executed one time. They are not supposed to be
     * reused.
     */
    DEVELOPER_ASYNC_TASK_REUSED,

    /**
     * Resource is empty
     */
    DEVELOPER_RESOURCE_IS_EMPTY,

    /**
     * Context is not provided
     */
    DEVELOPER_CONTEXT_IS_NOT_PROVIDED,

    /**
     * User can only have one login activity in use
     */
    DEVELOPER_ONLY_ONE_LOGIN_IS_ALLOWED,
    
    /**
     * Active callback is not found
     */
    CALLBACK_IS_NOT_FOUND,

    /**
     * Activity is not resolved
     */
    DEVELOPER_ACTIVITY_IS_NOT_RESOLVED,
    /**
     * Invalid request to server
     */
    SERVER_INVALID_REQUEST,

    /**
     * Authorization Failed
     */
    AUTH_FAILED,

    /**
     * Refresh token is failed and prompt is not allowed
     */
    AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED,

    /**
     * Authorization Failed: %d
     */
    AUTH_FAILED_ERROR_CODE,

    /**
     * The Authorization Server returned an unrecognized response
     */
    AUTH_FAILED_SERVER_ERROR,

    /**
     * The Application does not have a current ViewController
     */
    AUTH_FAILED_NO_CONTROLLER,

    /**
     * The required resource bundle could not be loaded
     */
    AUTH_FAILED_NO_RESOURCES,

    /**
     * The authorization server response has incorrectly encoded state
     */
    AUTH_FAILED_NO_STATE,

    /**
     * The authorization server response has no encoded state
     */
    AUTH_FAILED_BAD_STATE,

    /**
     * The requested access token could not be found
     */
    AUTH_FAILED_NO_TOKEN,

    /**
     * The user cancelled the authorization request
     */
    AUTH_FAILED_CANCELLED,

    /**
     * Invalid parameters for authorization operation
     */
    AUTH_FAILED_INTERNAL_ERROR,

    /**
     * Internet permissions are not set for the app
     */
    DEVICE_INTERNET_IS_NOT_AVAILABLE,

    /**
     * onActivityResult is called with null intent data
     */
    ON_ACTIVITY_RESULT_INTENT_NULL,

    /**
     * Shared preferences are not available
     */
    DEVICE_SHARED_PREF_IS_NOT_AVAILABLE,

    /**
     * Cache is not saving the changes. This error will be returned to
     * developer, if cache returns error
     */
    DEVICE_CACHE_IS_NOT_WORKING,

    /**
     * IdToken is failed to parse
     */
    IDTOKEN_PARSING_FAILURE,
}
