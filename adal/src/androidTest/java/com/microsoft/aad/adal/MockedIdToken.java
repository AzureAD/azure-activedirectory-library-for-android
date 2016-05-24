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

    static final String sIdTokenClaims = "{\"aud\":\"c3c7f5e5-7153-44d4-90e6-329686d48d76\",\"iss\":\"https://sts.windows.net/6fd1f5cd-a94c-4335-889b-6c598e6d8048/\",\"iat\":1387224169,\"nbf\":1387224170,\"exp\":1387227769,\"pwd_exp\":1387227772,\"pwd_url\":\"pwdUrl\",\"ver\":\"1.0\",\"tid\":\"%s\",\"oid\":\"%s\",\"upn\":\"%s\",\"unique_name\":\"%s\",\"sub\":\"%s\",\"family_name\":\"%s\",\"given_name\":\"%s\",\"altsecid\":\"%s\",\"idp\":\"%s\",\"email\":\"%s\"}";

    static final String sIdTokenHeader = "{\"typ\":\"JWT\",\"alg\":\"none\"}";

    String tid = "6fd1f5cd-a94c-4335-889b-6c598e6d8048";

    String oid = "53c6acf2-2742-4538-918d-e78257ec8516";

    String upn = "test@test.onmicrosoft.com";

    String unique_name = "testUnique@test.onmicrosoft.com";

    String sub = "0DxnAlLi12IvGL";

    String family_name = "familyName";

    String given_name = "givenName";

    String altsecid = "altsecid";

    String idp = "idpProvider";

    String email = "emailField";

    public String getIdToken() throws UnsupportedEncodingException {
        String claims = String.format(sIdTokenClaims, tid, oid, upn, unique_name, sub, family_name,
                given_name, altsecid, idp, email);
        return String.format(
                "%s.%s.",
                new String(Base64.encode(sIdTokenHeader.getBytes(AuthenticationConstants.ENCODING_UTF8),
                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        AuthenticationConstants.ENCODING_UTF8),
                new String(Base64.encode(claims.getBytes(AuthenticationConstants.ENCODING_UTF8),
                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        AuthenticationConstants.ENCODING_UTF8));
    }
}
