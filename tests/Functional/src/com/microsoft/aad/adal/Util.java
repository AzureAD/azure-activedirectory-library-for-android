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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.mockito.Mockito;

import android.util.Base64;
import android.util.Log;

class Util {
    static final String TEST_IDTOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";
    
    static String getIdToken() throws UnsupportedEncodingException {
        final String sIdTokenClaims = "{\"aud\":\"c3c7f5e5-7153-44d4-90e6-329686d48d76\",\"iss\":\"https://sts.windows.net/6fd1f5cd-a94c-4335-889b-6c598e6d8048/\",\"iat\":1387224169,\"nbf\":1387224170,\"exp\":1387227769,\"pwd_exp\":1387227772,\"pwd_url\":\"pwdUrl\",\"ver\":\"1.0\",\"tid\":\"%s\",\"oid\":\"%s\",\"upn\":\"%s\",\"unique_name\":\"%s\",\"sub\":\"%s\",\"family_name\":\"%s\",\"given_name\":\"%s\",\"altsecid\":\"%s\",\"idp\":\"%s\",\"email\":\"%s\"}";
        final String sIdTokenHeader = "{\"typ\":\"JWT\",\"alg\":\"none\"}";
        final String tid = "6fd1f5cd-a94c-4335-889b-6c598e6d8048";
        final String oid = "53c6acf2-2742-4538-918d-e78257ec8516";
        final String upn = "test@test.onmicrosoft.com";
        final String unique_name = "testUnique@test.onmicrosoft.com";
        final String sub = "0DxnAlLi12IvGL";
        final String family_name = "familyName";
        final String given_name = "givenName";
        final String altsecid = "altsecid";
        final String idp = "idpProvider";
        final String email = "emailField";
        final String claims = String.format(sIdTokenClaims, tid, oid, upn, unique_name, sub, family_name,
                given_name, altsecid, idp, email);
        return String.format("%s.%s.", 
                new String(Base64.encode(sIdTokenHeader.getBytes(AuthenticationConstants.ENCODING_UTF8), 
                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE), 
                        AuthenticationConstants.ENCODING_UTF8), 
                new String(Base64.encode(claims.getBytes(AuthenticationConstants.ENCODING_UTF8), 
                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        AuthenticationConstants.ENCODING_UTF8));
        }
    
    static TokenCacheItem getTokenCacheItem(final String authority,final String resource, final String clientId, 
            final String userId, final String displayableId) {
        Calendar expiredTime = new GregorianCalendar();
        Log.d("Test", "Time now:" + expiredTime.toString());
        expiredTime.add(Calendar.MINUTE, -60);
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
    
    static String getSuccessTokenResponse(final boolean isMrrt, final boolean withFociFlag) {
        final String tokenResponse = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"I am a new access token\",\"token_type\":\"Bearer\",\"expires_in\":\"10\",\"expires_on\":\"1368768616\",\"refresh_token\":\"I am a new refresh token\",\"scope\":\"*\"";
        
        final StringBuilder tokenResponseBuilder = new StringBuilder(tokenResponse);
        if (isMrrt) {
            tokenResponseBuilder.append(",\"resource\":\"resource\"");
        }
        
        if (withFociFlag) {
            tokenResponseBuilder.append(",\"foci\":\"familyClientId\"");
        } 
            
        tokenResponseBuilder.append("}");

        return tokenResponseBuilder.toString();
    }
    
    static String getSuccessResponseWithoutRefreshToken() {
        final String tokenResponse = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"access_token\":\"I am a new access token\",\"token_type\":\"Bearer\",\"expires_in\":\"10\",\"expires_on\":\"1368768616\",\"scope\":\"*\"}";
        return tokenResponse;
    }
    
    static String getErrorResponseBody(final String errorCode) {
        final String errorDescription = "\"error_description\":\"AADSTS70000: Authentication failed. Refresh Token is not valid.\r\nTrace ID: bb27293d-74e4-4390-882b-037a63429026\r\nCorrelation ID: b73106d5-419b-4163-8bc6-d2c18f1b1a13\r\nTimestamp: 2014-11-06 18:39:47Z\",\"error_codes\":[70000],\"timestamp\":\"2014-11-06 18:39:47Z\",\"trace_id\":\"bb27293d-74e4-4390-882b-037a63429026\",\"correlation_id\":\"b73106d5-419b-4163-8bc6-d2c18f1b1a13\",\"submit_url\":null,\"context\":null";
        
        if (errorCode != null) {
            return "{\"error\":\"" + errorCode + "\"," + errorDescription + "}";
        }
        
        return "{" + errorDescription + "}";
    }
    
    static byte[] getPoseMessage(final String refreshToken, final String clientId, final String resource) 
            throws UnsupportedEncodingException {
        return String.format("%s=%s&%s=%s&%s=%s&%s=%s",
                AuthenticationConstants.OAuth2.GRANT_TYPE, urlFormEncode(AuthenticationConstants.OAuth2.REFRESH_TOKEN),
                AuthenticationConstants.OAuth2.REFRESH_TOKEN, urlFormEncode(refreshToken),
                AuthenticationConstants.OAuth2.CLIENT_ID, urlFormEncode(clientId), 
                AuthenticationConstants.AAD.RESOURCE, urlFormEncode(resource)).getBytes();
    }
    
    static String urlFormEncode(String source) throws UnsupportedEncodingException {
        return URLEncoder.encode(source, "UTF_8");
    }
    
    static AuthenticationResult getAuthenticationResult(final boolean isMRRT, final String displayableId, 
            final String userId, final String familyClientId) {
        Calendar expiredTime = new GregorianCalendar();
        Logger.d("Test", "Time now:" + expiredTime.toString());
        expiredTime.add(Calendar.MINUTE, -60);
        final UserInfo userInfo = new UserInfo(userId, "GivenName", "FamilyName", "idp", displayableId);
        final AuthenticationResult authResult = new AuthenticationResult("accessToken", "refresh_token", expiredTime.getTime(), 
                isMRRT, userInfo, "TenantId", "IdToken");
        
        if (StringExtensions.IsNullOrBlank(familyClientId)) {
            return authResult;
        }
        
        authResult.setFamilyClientId(familyClientId);
        return authResult;
    }
    
    static InputStream createInputStream(final String input) {
        return  new ByteArrayInputStream(input.getBytes());
    }
    
    static void prepareMockedUrlConnection(final HttpURLConnection mockedConnection) throws IOException {
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Mockito.doNothing().when(mockedConnection).setConnectTimeout(Mockito.anyInt());
        Mockito.doNothing().when(mockedConnection).setDoInput(Mockito.anyBoolean());
    }
}
