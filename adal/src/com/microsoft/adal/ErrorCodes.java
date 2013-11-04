/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import android.content.Context;

/**
 * Error codes to help developer
 * 
 * @author omercan
 */
public class ErrorCodes {

    public static enum ADALError {
        /**
         * Authority url is not valid
         */
        DEVELOPER_AUTHORITY_IS_NOT_VALID_URL,
        /**
         * Authority is empty
         */
        DEVELOPER_AUTHORITY_IS_EMPTY,
        /**
         * Async tasks can only be executed one time. They are not supposed to
         * be reused.
         */
        DEVELOPER_ASYNC_TASK_REUSED,
        /**
         * Resource is empty
         */
        DEVELOPER_RESOURCE_IS_EMPTY,
        /**
         * Invalid request to server
         */
        SERVER_INVALID_REQUEST,

        /**
         * Authorization Failed
         */
        AUTH_FAILED,
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
        DEVICE_INTERNET_IS_NOT_AVAILABLE
    }

    /**
     * Get error message from resource file. It will get the translated text.
     * Strings.xml inside the resource folder can be customized for your users.
     * 
     * @param context required to get message.
     * @param enumId
     * @return Translated text
     */
    public String getMessage(Context appContext, ADALError enumId) {
        if (appContext != null) {
            int id = appContext.getResources().getIdentifier(enumId.name(), "string",
                    appContext.getPackageName());
            return appContext.getString(id);
        }
        
        throw new IllegalArgumentException("appContext is null");
    }
}
