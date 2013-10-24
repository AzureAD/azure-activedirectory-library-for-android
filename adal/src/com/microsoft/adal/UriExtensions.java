/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 */

package com.microsoft.adal;

import java.util.HashMap;

import android.net.Uri;

/**
 * MOVED from Hervey's code
 * @author omercan
 *
 */
final class UriExtensions
{
    static final HashMap<String, String> getFragmentParameters(Uri uri)
    {
        String parameters = uri.getFragment();

        if (parameters == null || parameters.length() == 0)
            return new HashMap<String, String>();
        else
            return HashMapExtensions.URLFormDecode(parameters);
    }

    static final HashMap<String, String> getQueryParameters(Uri uri)
    {
        String parameters = uri.getQuery();

        if (parameters == null || parameters.length() == 0)
            return new HashMap<String, String>();
        else
            return HashMapExtensions.URLFormDecode(parameters);
    }
}
