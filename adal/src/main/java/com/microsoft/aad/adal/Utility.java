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

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import android.net.Uri;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

final class Utility {
    /**
     * Private constructor to prevent the class from being initiated.
     */
    private Utility() { }
    
    /**
     * Create an immutable object for the input Date object 
     * to avoid exposing the internal references.
     */
    static Date getImmutableDateObject(final Date date) {
        if (date != null) {
            return new Date(date.getTime());
        }

        return date;
    }

    static boolean isClaimsChallengePresent(final AuthenticationRequest request) {
        // if developer pass claims down through extra qp, we should also skip cache.
        return !StringExtensions.isNullOrBlank(request.getClaimsChallenge());
    }

    static URL constructAuthorityUrl(final URL originalAuthority, final String host) throws MalformedURLException {
        final String path = originalAuthority.getPath().replaceFirst("/", "");
        final Uri.Builder builder = new Uri.Builder().scheme(originalAuthority.getProtocol()).authority(host).appendPath(path);
        return new URL(builder.build().toString());
    }
}
