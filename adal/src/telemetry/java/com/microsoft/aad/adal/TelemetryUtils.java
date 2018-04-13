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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static class CliTelemInfo implements Serializable {

        private String mVersion;
        private String mServerErrorCode;
        private String mServerSubErrorCode;
        private String mRefreshTokenAge;
        private String mSpeRing;

        public String getVersion() {
            return mVersion;
        }

        void setVersion(String version) {
            this.mVersion = version;
        }

        public String getServerErrorCode() {
            return mServerErrorCode;
        }

        void setServerErrorCode(String serverErrorCode) {
            this.mServerErrorCode = serverErrorCode;
        }

        public String getServerSubErrorCode() {
            return mServerSubErrorCode;
        }

        void setServerSubErrorCode(String serverSubErrorCode) {
            this.mServerSubErrorCode = serverSubErrorCode;
        }

        public String getRefreshTokenAge() {
            return mRefreshTokenAge;
        }

        void setRefreshTokenAge(String refreshTokenAge) {
            this.mRefreshTokenAge = refreshTokenAge;
        }

        public String getSpeRing() {
            return mSpeRing;
        }

        void setSpeRing(String speRing) {
            this.mSpeRing = speRing;
        }
    }

    public static CliTelemInfo parseXMsCliTelemHeader(final String headerValue) {
        // if the header isn't present, do nothing
        if (StringExtensions.isNullOrBlank(headerValue)) {
            return null;
        }

        // split the header based on the delimiter
        String[] headerSegments = headerValue.split(",");

        // make sure the header isn't empty
        if (0 == headerSegments.length) {
            Logger.w(TAG, "SPE Ring header missing version field.", null, ADALError.X_MS_CLITELEM_VERSION_UNRECOGNIZED);
            return null;
        }

        // get the version of this header
        final String headerVersion = headerSegments[0];

        // The eventual result
        final CliTelemInfo cliTelemInfo = new CliTelemInfo();
        cliTelemInfo.setVersion(headerVersion);

        if (headerVersion.equals("1")) {
            // The expected delimiter count of the v1 header
            final int delimCount = 4;

            // Verify the expected format "<version>, <error_code>, <sub_error_code>, <token_age>, <ring>"
            Pattern headerFmt = Pattern.compile("^[1-9]+\\.?[0-9|\\.]*,[0-9|\\.]*,[0-9|\\.]*,[^,]*[0-9\\.]*,[^,]*$");
            Matcher matcher = headerFmt.matcher(headerValue);
            if (!matcher.matches()) {
                Logger.w(TAG, "", "", ADALError.X_MS_CLITELEM_MALFORMED);
                return null;
            }

            headerSegments = headerValue.split(",", delimCount + 1);

            // Constants used to identify value position
            final int indexErrorCode = 1;
            final int indexSubErrorCode = 2;
            final int indexTokenAge = 3;
            final int indexSpeInfo = 4;

            cliTelemInfo.setServerErrorCode(headerSegments[indexErrorCode]);
            cliTelemInfo.setServerSubErrorCode(headerSegments[indexSubErrorCode]);
            cliTelemInfo.setRefreshTokenAge(headerSegments[indexTokenAge]);
            cliTelemInfo.setSpeRing(headerSegments[indexSpeInfo]);
        } else { // unrecognized version
            Logger.w(TAG, "Unexpected header version. ",
                    "Header version: " + headerVersion, ADALError.X_MS_CLITELEM_VERSION_UNRECOGNIZED);
            return null;
        }

        return cliTelemInfo;
    }

}
