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

import java.io.UnsupportedEncodingException;

import android.util.Base64;

class MockedIdToken {

    private static final String ID_TOKEN_CLAIMS = "{\"aud\":\"c3c7f5e5-7153-44d4-90e6-329686d48d76\",\"iss\":\"https://sts.windows.net/6fd1f5cd-a94c-4335-889b-6c598e6d8048/\",\"iat\":1387224169,\"nbf\":1387224170,\"exp\":1387227769,\"pwd_exp\":1387227772,\"pwd_url\":\"pwdUrl\",\"ver\":\"1.0\",\"tid\":\"%s\",\"oid\":\"%s\",\"upn\":\"%s\",\"unique_name\":\"%s\",\"sub\":\"%s\",\"family_name\":\"%s\",\"given_name\":\"%s\",\"altsecid\":\"%s\",\"idp\":\"%s\",\"email\":\"%s\"}";

    private static final String ID_TOKEN_HEADER = "{\"typ\":\"JWT\",\"alg\":\"none\"}";

    private static final String TID = "6fd1f5cd-a94c-4335-889b-6c598e6d8048";

    private static final String UNIQUE_NAME = "testUnique@test.onmicrosoft.com";

    private static final String SUB = "0DxnAlLi12IvGL";

    private static final String FAMILY_NAME = "familyName";

    private static final String GIVEN_NAME = "givenName";

    private static final String ALTSECID = "altsecid";

    private static final String IDP = "idpProvider";

    private static final String EMAIL = "emailField";

    private String mOid = "53c6acf2-2742-4538-918d-e78257ec8516";

    private String mUpn = "test@test.onmicrosoft.com";

    public String getIdToken() throws UnsupportedEncodingException {
        String claims = String.format(ID_TOKEN_CLAIMS, TID, mOid, mUpn, UNIQUE_NAME, SUB, FAMILY_NAME,
                GIVEN_NAME, ALTSECID, IDP, EMAIL);
        return String.format(
                "%s.%s.",
                new String(Base64.encode(ID_TOKEN_HEADER.getBytes(AuthenticationConstants.ENCODING_UTF8),
                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        AuthenticationConstants.ENCODING_UTF8),
                new String(Base64.encode(claims.getBytes(AuthenticationConstants.ENCODING_UTF8),
                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        AuthenticationConstants.ENCODING_UTF8));
    }

    public String getOid() {
        return mOid;
    }

    public void setOid(String oid) {
        this.mOid = oid;
    }

    public String getUpn() {
        return mUpn;
    }

    public void setUpn(String upn) {
        this.mUpn = upn;
    }
}
