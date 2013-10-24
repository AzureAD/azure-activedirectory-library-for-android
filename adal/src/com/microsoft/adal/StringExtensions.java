/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 */

package com.microsoft.adal;

import android.net.Uri;

final class StringExtensions
{
    static final String URLFormEncode(String source)
    {
        // Encode everything except spaces
        String target = Uri.encode(source, " ");

        // Encode spaces to +
        return target.replace(' ', '+');
    }

    static final String URLFormDecode(String source)
    {
        // Decode + to spaces
        String target = source.replace('+', ' ');

        // Decode everything else
        return Uri.decode(target);
    }

    public static boolean IsNullOrBlank(String param) {
        return param == null || param.trim().length() == 0;
    }
}
