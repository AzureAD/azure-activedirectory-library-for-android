/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import android.net.Uri;

final class StringExtensions
{
    /**
     * checks if string is null or empty
     * 
     * @param param
     * @return
     */
    static boolean IsNullOrBlank(String param) {
        if (param == null || param.trim().length() == 0) {
            return true;
        }
        
        return false;
    }

    /**
     * encode string with url form encoding. Space will be +
     * 
     * @param source
     * @return
     */
    static final String URLFormEncode(String source)
    {
        // Encode everything except spaces
        String target = Uri.encode(source, " ");

        // Encode spaces to +
        return target.replace(' ', '+');
    }

    /**
     * replace + to space and decode
     * 
     * @param source
     * @return
     */
    static final String URLFormDecode(String source)
    {
        // Decode + to spaces
        String target = source.replace('+', ' ');

        // Decode everything else
        return Uri.decode(target);
    }
}
