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

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.HttpUrlConnectionFactory;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

final class Util {
    public static final int TEST_PASSWORD_EXPIRATION = 1387227772;
    static final String TEST_IDTOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";
    static final String TEST_CLIENT_INFO = "eyJ1aWQiOiI0Zjg1OTk4OS1hMmZmLTQxMWUtOTA0OC1jMzIyMjQ3YWM2MmMiLCJ1dGlkIjoiMzBiYWE2NjYtOGRmOC00OGU3LTk3ZTYtNzdjZmQwOTk1OTYzIn0=";

    static final String ENCODED_SIGNATURE = "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";

    static final int TOKENS_EXPIRES_MINUTE = 60;

    static String getIdToken() throws UnsupportedEncodingException {
        final String sIdTokenClaims = "{\"aud\":\"c3c7f5e5-7153-44d4-90e6-329686d48d76\",\"iss\":\"https://sts.windows.net/6fd1f5cd-a94c-4335-889b-6c598e6d8048/\",\"iat\":1387224169,\"nbf\":1387224170,\"exp\":1387227769,\"pwd_exp\":1387227772,\"pwd_url\":\"pwdUrl\",\"ver\":\"1.0\",\"tid\":\"%s\",\"oid\":\"%s\",\"upn\":\"%s\",\"unique_name\":\"%s\",\"sub\":\"%s\",\"family_name\":\"%s\",\"given_name\":\"%s\",\"altsecid\":\"%s\",\"idp\":\"%s\",\"email\":\"%s\"}";
        final String sIdTokenHeader = "{\"typ\":\"JWT\",\"alg\":\"none\"}";
        final String tid = "6fd1f5cd-a94c-4335-889b-6c598e6d8048";
        final String oid = "53c6acf2-2742-4538-918d-e78257ec8516";
        final String upn = "test@test.onmicrosoft.com";
        final String uniqueName = "testUnique@test.onmicrosoft.com";
        final String sub = "0DxnAlLi12IvGL";
        final String familyName = "familyName";
        final String givenName = "givenName";
        final String altsecid = "altsecid";
        final String idp = "idpProvider";
        final String email = "emailField";
        final String claims = String.format(sIdTokenClaims, tid, oid, upn, uniqueName, sub, familyName,
                givenName, altsecid, idp, email);
        return String.format("%s.%s.",
                new String(Base64.encode(sIdTokenHeader.getBytes(StandardCharsets.UTF_8),
                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        StandardCharsets.UTF_8),
                new String(Base64.encode(claims.getBytes(StandardCharsets.UTF_8),
                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        StandardCharsets.UTF_8));
    }

    static TokenCacheItem getTokenCacheItem(final String authority, final String resource,
                                            final String clientId, final String userId,
                                            final String displayableId) {
        Calendar expiredTime = new GregorianCalendar();
        Logger.d("Test", "Time now:" + expiredTime.toString());
        expiredTime.add(Calendar.MINUTE, -TOKENS_EXPIRES_MINUTE);
        TokenCacheItem tokenCacheItem = new TokenCacheItem();
        tokenCacheItem.setAuthority(authority);
        tokenCacheItem.setResource(resource);
        tokenCacheItem.setClientId(clientId);
        tokenCacheItem.setAccessToken("accessToken");
        tokenCacheItem.setRefreshToken("refreshToken=");
        tokenCacheItem.setExpiresOn(expiredTime.getTime());
        tokenCacheItem.setUserInfo(new UserInfo(userId, "givenName", "familyName",
                "identityProvider", displayableId));

        return tokenCacheItem;
    }

    static String getSuccessTokenResponseWithFamilyClientId() throws JSONException {
        final JSONObject responseJsonObject = getCommonSuccessResponseData();

        responseJsonObject.put("refresh_token", "I am a new refresh token");
        responseJsonObject.put("resource", "resource");
        responseJsonObject.put("foci", "1");

        return responseJsonObject.toString();
    }

    static String getSuccessTokenResponse(final boolean isMrrt, final boolean withFociFlag) throws JSONException {
        final JSONObject responseJsonObject = getCommonSuccessResponseData();
        responseJsonObject.put(AuthenticationConstants.OAuth2.REFRESH_TOKEN, "I am a new refresh token");
        if (isMrrt) {
            responseJsonObject.put(AuthenticationConstants.AAD.RESOURCE, "resource");
        }

        if (withFociFlag) {
            responseJsonObject.put(AuthenticationConstants.OAuth2.ADAL_CLIENT_FAMILY_ID, "familyClientId");
        }

        return responseJsonObject.toString();
    }

    static String getSuccessResponseWithoutRefreshToken() throws JSONException {
        return getCommonSuccessResponseData().toString();
    }

    private static JSONObject getCommonSuccessResponseData() throws JSONException {
        final JSONObject responseJsonObject = new JSONObject();
        responseJsonObject.put(AuthenticationConstants.OAuth2.ID_TOKEN, TEST_IDTOKEN);
        responseJsonObject.put(AuthenticationConstants.OAuth2.CLIENT_INFO, TEST_CLIENT_INFO);
        responseJsonObject.put(AuthenticationConstants.OAuth2.ACCESS_TOKEN, "I am a new access token");
        responseJsonObject.put(AuthenticationConstants.OAuth2.TOKEN_TYPE, "Bearer");
        responseJsonObject.put(AuthenticationConstants.OAuth2.EXPIRES_IN, "10");
        responseJsonObject.put("expires_on", "1368768616");
        responseJsonObject.put(AuthenticationConstants.OAuth2.SCOPE, "*");

        return responseJsonObject;
    }

    static String getErrorResponseBody(final String errorCode) throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put(AuthenticationConstants.OAuth2.ERROR, errorCode);
        jsonObject.put(
                AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                "AADSTS70000: Authentication failed. Refresh Token is not valid.\n"
                        + "Trace ID: bb27293d-74e4-4390-882b-037a63429026\n"
                        + "Correlation ID: b73106d5-419b-4163-8bc6-d2c18f1b1a13\n"
                        + "Timestamp: 2014-11-06 18:39:47Z"
        );
        jsonObject.put(AuthenticationConstants.OAuth2.ERROR_CODES, "[70000]");
        jsonObject.put("timestamp", "2014-11-06 18:39:47Z");
        jsonObject.put("trace_id", "bb27293d-74e4-4390-882b-037a63429026");
        jsonObject.put(AuthenticationConstants.AAD.CORRELATION_ID, "b73106d5-419b-4163-8bc6-d2c18f1b1a13");
        return jsonObject.toString();
    }

    static byte[] getPostMessage(final String refreshToken, final String clientId, final String resource)
            throws UnsupportedEncodingException {
        String string = String.format("%s=%s&%s=%s&%s=%s&%s=%s&%s=%s&%s=%s&%s=%s",
                AuthenticationConstants.OAuth2.GRANT_TYPE, urlFormEncode(AuthenticationConstants.OAuth2.REFRESH_TOKEN),
                AuthenticationConstants.OAuth2.REFRESH_TOKEN, urlFormEncode(refreshToken),
                AuthenticationConstants.OAuth2.CLIENT_ID, urlFormEncode(clientId),
                AuthenticationConstants.OAuth2.CLIENT_INFO, urlFormEncode(AuthenticationConstants.OAuth2.CLIENT_INFO_TRUE),
                AuthenticationConstants.AAD.RESOURCE, urlFormEncode(resource),
                AuthenticationConstants.AAD.APP_PACKAGE_NAME, urlFormEncode("test.mock."),
                AuthenticationConstants.AAD.APP_VERSION, urlFormEncode("test"));

        return string.getBytes();
    }

    static String urlFormEncode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, AuthenticationConstants.ENCODING_UTF8);
    }

    static AuthenticationResult getAuthenticationResult(final boolean isMRRT, final String displayableId,
                                                        final String userId, final String familyClientId) {
        Calendar expiredTime = new GregorianCalendar();
        Logger.d("Test", "Time now:" + expiredTime.toString());
        expiredTime.add(Calendar.MINUTE, -TOKENS_EXPIRES_MINUTE);
        final UserInfo userInfo = new UserInfo(userId, "GivenName", "FamilyName", "idp", displayableId);
        final AuthenticationResult authResult = new AuthenticationResult(
                "accessToken", "refresh_token",
                expiredTime.getTime(),
                isMRRT,
                userInfo,
                "TenantId",
                "IdToken",
                null,
                "ClientId"
        );

        if (StringExtensions.isNullOrBlank(familyClientId)) {
            return authResult;
        }

        authResult.setFamilyClientId(familyClientId);
        return authResult;
    }

    static InputStream createInputStream(final String input) {
        return new ByteArrayInputStream(input.getBytes());
    }

    static void prepareMockedUrlConnection(final HttpURLConnection mockedConnection) throws IOException {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Mockito.doNothing().when(mockedConnection).setConnectTimeout(Mockito.anyInt());
        Mockito.doNothing().when(mockedConnection).setDoInput(Mockito.anyBoolean());
    }

    static Map<String, byte[]> getSecretKeys() throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        SecretKey tempkey1 = keyFactory.generateSecret(new PBEKeySpec("test1".toCharArray(),
                "abcdedfdfd".getBytes(StandardCharsets.UTF_8), 100, 256));
        SecretKey secretKey1 = new SecretKeySpec(tempkey1.getEncoded(), "AES");
        SecretKey tempkey2 = keyFactory.generateSecret(new PBEKeySpec("test2".toCharArray(),
                "abcdedfdfd".getBytes(StandardCharsets.UTF_8), 100, 256));
        SecretKey secretKey2 = new SecretKeySpec(tempkey2.getEncoded(), "AES");
        Map<String, byte[]> secretKeys = new HashMap<String, byte[]>(2);
        secretKeys.put(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, secretKey1.getEncoded());
        secretKeys.put(AuthenticationSettings.INSTANCE.getBrokerPackageName(), secretKey2.getEncoded());
        return secretKeys;
    }
}
