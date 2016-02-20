package com.microsoft.aad.adal;

import java.net.URL;
import java.util.Locale;

class UrlExtensions {

    public static boolean isADFSAuthority(URL authorizationEndpoint) {
        // similar to ADAL.NET
        String path = authorizationEndpoint.getPath();
        return !StringExtensions.IsNullOrBlank(path)
                && path.toLowerCase(Locale.ENGLISH).equals("/adfs");
    }
}
