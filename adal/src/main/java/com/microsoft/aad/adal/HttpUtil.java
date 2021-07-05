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

import android.content.Context;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;

/**
 * Webrequest are called in background thread from API level. HttpUtil
 * does not create another thread.
 */
class HttpUtil {

    static void throwIfNetworkNotAvailable(final Context context) throws AuthenticationException {
        try {
            com.microsoft.identity.common.adal.internal.net.HttpWebRequest.throwIfNetworkNotAvailable(context, false);
        } catch (final ClientException e) {
            final String errorCode = e.getErrorCode();
            final String errorMessage = e.getMessage();
            ADALError error;

            switch (errorCode) {
                case ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION:
                    error = ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION;
                    break;
                case ErrorStrings.DEVICE_NETWORK_NOT_AVAILABLE:
                    error = ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE;
                    break;
                default:
                    error = ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE;

                    com.microsoft.identity.common.internal.logging.Logger.warn(
                            "HttpUtil",
                            "Unrecognized error code: "
                                    + errorCode
                    );
            }

            throw new AuthenticationException(error, errorMessage);
        }
    }
}
