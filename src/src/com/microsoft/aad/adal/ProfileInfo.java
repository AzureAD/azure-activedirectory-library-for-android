// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import java.util.HashMap;

import android.util.Base64;

class ProfileInfo {
    static final String TAG = ProfileInfo.class.getSimpleName();

    String mVersion;

    String mSubject;

    String mTenantId;

    String mName;

    String mPreferredName;
    
    private ProfileInfo() {}

    static ProfileInfo parseProfileInfo(String rawProfileInfo) {
        try {
            // Message Base64 encoded text

            if (!StringExtensions.IsNullOrBlank(rawProfileInfo)) {
                // URL_SAFE: Encoder/decoder flag bit to use
                // "URL and filename safe" variant of Base64
                // (see RFC 3548 section 4) where - and _ are used in place of +
                // and /.
                byte[] data = Base64.decode(rawProfileInfo, Base64.URL_SAFE);
                final String decodedBody = new String(data, "UTF-8");

                HashMap<String, String> responseItems = new HashMap<String, String>();
                JsonHelper.extractJsonObjects(responseItems, decodedBody);
                if (responseItems != null && !responseItems.isEmpty()) {
                    final ProfileInfo profileInfo = new ProfileInfo();
                    profileInfo.mVersion = responseItems.get(ProfileInfoClaim.VERSION);
                    profileInfo.mSubject = getValue(responseItems.get(ProfileInfoClaim.SUBJECT));
                    profileInfo.mTenantId = responseItems.get(ProfileInfoClaim.TENANT_ID);
                    profileInfo.mName = getValue(responseItems.get(ProfileInfoClaim.NAME)); //responseItems.get(ProfileInfoClaim.NAME);
                    profileInfo.mPreferredName = getValue(responseItems.get(ProfileInfoClaim.PREFERRED_USERNAME));//responseItems.get(ProfileInfoClaim.PREFERRED_USERNAME);
                    Logger.v(TAG, "Profile info is extracted from token response");
                    return profileInfo;
                }
            }
        } catch (Exception ex) {
            Logger.e(TAG, "Error in parsing user id token", null, ADALError.IDTOKEN_PARSING_FAILURE, ex);
        }
        return null;
    }
    
    private static String getValue(final String value) {
        if ("null".equalsIgnoreCase(value)) {
            return null;
        }
        
        return value;
    }

    private static final class ProfileInfoClaim {
        static final String VERSION = "ver";
        static final String SUBJECT = "sub";
        static final String TENANT_ID = "tid";
        static final String PREFERRED_USERNAME = "preferred_username";
        static final String NAME = "name";
    }
}
