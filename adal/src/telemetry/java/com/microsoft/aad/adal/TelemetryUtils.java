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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.aad.adal.EventStrings.LOGIN_HINT;
import static com.microsoft.aad.adal.EventStrings.TENANT_ID;
import static com.microsoft.aad.adal.EventStrings.USER_ID;

public final class TelemetryUtils {

    static final Set<String> GDPR_FILTERED_FIELDS = new HashSet<>();

    private static final String TAG = TelemetryUtils.class.getSimpleName();

    static {
        initializeGdprFilteredFields();
    }

    private TelemetryUtils() {
        // Intentionally left blank.
    }

    private static void initializeGdprFilteredFields() {
        GDPR_FILTERED_FIELDS.addAll(
                Arrays.asList(
                        LOGIN_HINT,
                        USER_ID,
                        TENANT_ID
                )
        );
    }

    public static class CliTelemInfo
            extends com.microsoft.identity.common.internal.telemetry.CliTelemInfo {
        // Looking for this class? It's moved! (See parent class)

        public CliTelemInfo() {
            super();
        }

        private CliTelemInfo(final com.microsoft.identity.common.internal.telemetry.CliTelemInfo in) {
            super(in);
        }

        void _setVersion(final String version) {
            super.setVersion(version);
        }

        void _setServerErrorCode(final String serverErrorCode) {
            super.setServerErrorCode(serverErrorCode);
        }

        void _setServerSubErrorCode(final String serverSubErrorCode) {
            super.setServerSubErrorCode(serverSubErrorCode);
        }

        void _setRefreshTokenAge(final String refreshTokenAge) {
            super.setRefreshTokenAge(refreshTokenAge);
        }

        void _setSpeRing(final String speRing) {
            super.setSpeRing(speRing);
        }
    }

    public static CliTelemInfo parseXMsCliTelemHeader(final String headerValue) {
        return new CliTelemInfo(
                com.microsoft.identity.common.internal.telemetry.CliTelemInfo.fromXMsCliTelemHeader(
                        headerValue
                )
        );
    }

}
