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
         * Authority is empty
         */
        DEVELOPER_ASYNC_TASK_REUSED,
        /**
         * Resource is empty
         */
        DEVELOPER_RESOURCE_IS_EMPTY,
        /**
         * Invalid reques to server
         */
        SERVER_INVALID_REQUEST,
        /**
         * Internet permissions are not set for the app
         */
        DEVICE_INTERNET_IS_NOT_AVAILABLE
    }

    /**
     * Get error message from resource file. It will get the translated text.
     * Strings.xml inside the resource folder can be customized for your users.
     * 
     * @param context
     * @param enumId
     * @return Translated text
     */
    public String getMessage(Context appContext, ADALError enumId)
    {
        throw new UnsupportedOperationException("come back later");
    }
}
