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

package com.microsoft.aad.adal.test;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import junit.framework.Assert;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.google.gson.Gson;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationConstants.AAD;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.HttpWebResponse;
import com.microsoft.aad.adal.WebRequestHandler;

/**
 * webrequest tests related to get, put, post, delete requests
 */
public class WebRequestHandlerTests extends AndroidTestHelper {

    private final static String TEST_WEBAPI_URL = "https://graphtestrun.azurewebsites.net/api/WebRequestTest";

    protected static final String TAG = "WebRequestHandlerTests";

    /**
     * send invalid request to production service
     * 
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws UnsupportedEncodingException
     */
    @SmallTest
    public void testCorrelationIdInRequest() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, UnsupportedEncodingException {
        final String testUrl = "https://login.windows.net/omercantest.onmicrosoft.com/oauth2/token";
        final UUID testID = UUID.randomUUID();
        Log.d(TAG, "Test correlationid:" + testID.toString());
        final HttpWebResponse testResponse = sendCorrelationIdRequest(testUrl, testID, false);

        assertEquals("400 error code", 400, testResponse.getStatusCode());
        String responseBody = new String(testResponse.getBody(),
                AuthenticationConstants.ENCODING_UTF8);
        Log.v(TAG, "Test response:" + responseBody);
        assertNotNull("webresponse is not null", testResponse);
        assertEquals("same correlationid", testID.toString(), testResponse.getResponseHeaders()
                .get(AuthenticationConstants.AAD.CLIENT_REQUEST_ID).get(0));
        assertTrue("correlationid in response", responseBody.contains(testID.toString()));

        // same id for next request
        HttpWebResponse testResponse2 = sendCorrelationIdRequest(testUrl, testID, true);
        assertEquals("same correlationid", testID.toString(), testResponse2.getResponseHeaders()
                .get(AuthenticationConstants.AAD.CLIENT_REQUEST_ID).get(0));
    }

    private HttpWebResponse sendCorrelationIdRequest(final String message, final UUID testID,
            final boolean withoutHeader) {
        Log.d(TAG, "test get" + android.os.Process.myTid());

        WebRequestHandler request = new WebRequestHandler();
        request.setRequestCorrelationId(testID);
        HashMap<String, String> headers = null;
        if (!withoutHeader) {
            headers = new HashMap<String, String>();
            headers.put("Accept", "application/json");
        }
        return request
                .sendPost(getUrl(message), headers, null, "application/x-www-form-urlencoded");
    }

    public void testNullUrl() {
        assertThrowsException(IllegalArgumentException.class, "url", new Runnable() {
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendGet(null, null);
            }
        });
    }

    public void testWrongSchemeUrl() {

        assertThrowsException(IllegalArgumentException.class, "url", new Runnable() {
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendGet(getUrl("ftp://test.com"), null);
            }
        });
    }

    class TestResponse {
        HttpWebResponse httpResponse;

        Exception exception;
    }

    public void testGetRequest() {
        Log.d(TAG, "test get" + android.os.Process.myTid());

        WebRequestHandler request = new WebRequestHandler();
        HttpWebResponse httpResponse = request.sendGet(getUrl(TEST_WEBAPI_URL),
                getTestHeaders("testabc", "value123"));

        assertNotNull(httpResponse != null);
        assertTrue("status is 200", httpResponse.getStatusCode() == 200);
        String responseMsg = new String(httpResponse.getBody());
        assertTrue("request header check", responseMsg.contains("testabc-value123"));
    }

    /**
     * WebService returns the request headers in the response
     */
    public void testClientTraceInHeaders() {
        Log.d(TAG, "test get" + android.os.Process.myTid());

        WebRequestHandler request = new WebRequestHandler();
        HttpWebResponse httpResponse = request.sendGet(getUrl(TEST_WEBAPI_URL),
                getTestHeaders("testClientTraceInHeaders", "valueYes"));

        assertNotNull(httpResponse != null);
        assertTrue("status is 200", httpResponse.getStatusCode() == 200);
        String responseMsg = new String(httpResponse.getBody());
        assertTrue("request header check", responseMsg.contains(AAD.ADAL_ID_PLATFORM + "-Android"));
        assertTrue(
                "request header check",
                responseMsg.contains(AAD.ADAL_ID_VERSION + "-"
                        + AuthenticationContext.getVersionName()));
    }

    public void testNonExistentUrl() {
        WebRequestHandler request = new WebRequestHandler();
        HttpWebResponse httpResponse = request.sendGet(
                getUrl("http://www.somethingabcddnotexists.com"), null);
        assertNotNull(httpResponse.getResponseException());
        assertTrue("Unknown host exception",
                httpResponse.getResponseException() instanceof UnknownHostException);
        assertTrue("Unable to resolve host", httpResponse.getResponseException().getMessage()
                .toLowerCase(Locale.US).contains("unable to resolve host"));
    }

    public void testGetWithIdRequest() {
        WebRequestHandler request = new WebRequestHandler();
        HttpWebResponse httpResponse = request.sendGet(getUrl(TEST_WEBAPI_URL + "/1"), null);

        assertNull(httpResponse.getResponseException());
        assertTrue("status is 200", httpResponse.getStatusCode() == 200);
        String responseMsg = new String(httpResponse.getBody());
        assertTrue("request body check", responseMsg.contains("test get with id"));
    }

    public void testPostRequest() {
        final TestMessage message = new TestMessage("messagetest", "12345");
        HttpWebResponse httpResponse = null;
        WebRequestHandler request = new WebRequestHandler();
        String json = new Gson().toJson(message);

        try {
            httpResponse = request.sendPost(getUrl(TEST_WEBAPI_URL), null,
                    json.getBytes(ENCODING_UTF8), "application/json");
        } catch (UnsupportedEncodingException e) {
            Assert.fail("Encoding exception is not expected");
        }

        assertNull(httpResponse.getResponseException());
        assertTrue("status is 200", httpResponse.getStatusCode() == 200);
        String responseMsg = new String(httpResponse.getBody());
        assertTrue("request body check",
                responseMsg.contains(message.getAccessToken() + message.getUserName()));
    }

    class TestMessage {
        @com.google.gson.annotations.SerializedName("AccessToken")
        private String mAccessToken;

        @com.google.gson.annotations.SerializedName("UserName")
        private String mUserName;

        public TestMessage(String token, String name) {
            mAccessToken = token;
            mUserName = name;
        }

        public String getAccessToken() {
            return mAccessToken;
        }

        public void setAccessToken(String mAccessToken) {
            this.mAccessToken = mAccessToken;
        }

        public String getUserName() {
            return mUserName;
        }

        public void setUserName(String mUserName) {
            this.mUserName = mUserName;
        }

    }

    private HashMap<String, String> getTestHeaders(String key, String value) {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(key, value);
        return headers;
    }

    private URL getUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
