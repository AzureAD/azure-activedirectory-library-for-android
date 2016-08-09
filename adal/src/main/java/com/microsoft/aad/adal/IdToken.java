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

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * IdToken class is used to initialize the IdToken object with the raw idToken
 * String from the server.
 */
class IdToken {

    private static final String TAG = "IdToken";

    private String mSubject;

    private String mTenantId;

    private String mUpn;

    private String mGivenName;

    private String mFamilyName;

    private String mEmail;

    private String mIdentityProvider;

    private String mObjectId;

    private long mPasswordExpiration;

    private String mPasswordChangeUrl;

    public IdToken(String idtoken) throws AuthenticationException {
        // Message segments: Header.Body.Signature
        final Map<String, String> responseItems = this.parseJWT(idtoken);

        if (responseItems != null && !responseItems.isEmpty()) {
            this.mSubject = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_SUBJECT);
            this.mTenantId = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_TENANTID);
            this.mUpn = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_UPN);
            this.mEmail = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_EMAIL);
            this.mGivenName = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_GIVEN_NAME);
            this.mFamilyName = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_FAMILY_NAME);
            this.mIdentityProvider = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_IDENTITY_PROVIDER);
            this.mObjectId = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_OBJECT_ID);
            final String expiration = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_PASSWORD_EXPIRATION);

            if (!StringExtensions.isNullOrBlank(expiration)) {
                this.mPasswordExpiration = Long.parseLong(expiration);
            }

            this.mPasswordChangeUrl = responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN_PASSWORD_CHANGE_URL);
        }
    }

    public String getSubject() {
        return mSubject;
    }

    public String getTenantId() {
        return mTenantId;
    }

    public String getUpn() {
        return mUpn;
    }

    public String getGivenName() {
        return mGivenName;
    }

    public String getFamilyName() {
        return mFamilyName;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getIdentityProvider() {
        return mIdentityProvider;
    }

    public String getObjectId() {
        return mObjectId;
    }

    public long getPasswordExpiration() {
        return mPasswordExpiration;
    }

    public String getPasswordChangeUrl() {
        return mPasswordChangeUrl;
    }

    private Map<String, String> parseJWT(final String idtoken) throws AuthenticationException {
        final String idbody = extractJWTBody(idtoken);
        // URL_SAFE: Encoder/decoder flag bit to use
        // "URL and filename safe" variant of Base64
        // (see RFC 3548 section 4) where - and _ are used in place of +
        // and /.
        final byte[] data = Base64.decode(idbody, Base64.URL_SAFE);

        try {
            final String decodedBody = new String(data, "UTF-8");
            return extractJsonObjects(decodedBody);
        } catch (UnsupportedEncodingException exception) {
            Logger.e(TAG, "The encoding is not supported.", "", ADALError.ENCODING_IS_NOT_SUPPORTED, exception);
            throw new AuthenticationException(ADALError.ENCODING_IS_NOT_SUPPORTED, exception.getMessage(), exception);
        } catch (JSONException exception) {
            Logger.e(TAG, "Failed to parse the decoded body into JsonObject.", "", ADALError.JSON_PARSE_ERROR,
                    exception);
            throw new AuthenticationException(ADALError.JSON_PARSE_ERROR, exception.getMessage(), exception);
        }
    }

    private String extractJWTBody(final String idToken) throws AuthenticationException {
        final int firstDot = idToken.indexOf('.');
        final int secondDot = idToken.indexOf('.', firstDot + 1);
        final int invalidDot = idToken.indexOf('.', secondDot + 1);

        if (invalidDot == -1 && firstDot > 0 && secondDot > 0) {
            return idToken.substring(firstDot + 1, secondDot);
        } else {
            throw new AuthenticationException(ADALError.IDTOKEN_PARSING_FAILURE, "Failed to extract the ClientID");
        }
    }

    private static Map<String, String> extractJsonObjects(final String jsonStr) throws JSONException {
        final JSONObject jsonObject = new JSONObject(jsonStr);
        final Map<String, String> responseItems = new HashMap<>();
        final Iterator<?> i = jsonObject.keys();
        while (i.hasNext()) {
            final String key = (String) i.next();
            responseItems.put(key, jsonObject.getString(key));
        }
        return responseItems;
    }
}
