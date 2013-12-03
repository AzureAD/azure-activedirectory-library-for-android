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

    /**
     * Get error message from resource file. It will get the translated text.
     * Strings.xml inside the resource folder can be customized for your users.
     * 
     * @param context required to get message.
     * @param enumId
     * @return Translated text
     */
    public static String getMessage(Context appContext, ADALError enumId) {
        if (appContext != null) {
            int id = appContext.getResources().getIdentifier(enumId.name(), "string",
                    appContext.getPackageName());
            return appContext.getString(id);
        }

        throw new IllegalArgumentException("appContext is null");
    }
}
