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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import com.google.gson.Gson;
import com.microsoft.aad.adal.AuthenticationConstants.AAD;

import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

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
     * @throws IOException 
     */
    @SmallTest
    public void testCorrelationIdInRequest() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, IOException {
        final String testUrl = "https://login.windows.net/omercantest.onmicrosoft.com/oauth2/token";
        final UUID testID = UUID.randomUUID();
        Log.d(TAG, "Test correlationid:" + testID.toString());
        final HttpWebResponse testResponse = sendCorrelationIdRequest(testUrl, testID, false);

        assertEquals("400 error code", 400, testResponse.getStatusCode());
        String responseBody = testResponse.getBody();
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
            final boolean withoutHeader) throws IOException {
        Log.d(TAG, "test get" + android.os.Process.myTid());

        WebRequestHandler request = new WebRequestHandler();
        request.setRequestCorrelationId(testID);
        HashMap<String, String> headers = null;
        if (!withoutHeader) {
            headers = new HashMap<String, String>();
            headers.put("Accept", "application/json");
        }
        return request.sendPost(getUrl(message), headers, null, 
                "application/x-www-form-urlencoded");
    }

    public void testNullUrl() {
        assertThrowsException(IllegalArgumentException.class, "url", new ThrowableRunnable() {
            public void run() throws IOException {
                WebRequestHandler request = new WebRequestHandler();
                request.sendGet(null, null);
            }
        });
    }

    public void testWrongSchemeUrl() {

        assertThrowsException(IllegalArgumentException.class, "url", new ThrowableRunnable() {
            public void run() throws IOException {
                WebRequestHandler request = new WebRequestHandler();
                request.sendGet(getUrl("ftp://test.com"), null);
            }
        });
    }

    class TestResponse {
        HttpWebResponse httpResponse;

        Exception exception;
    }

    public void testGetRequest() throws IOException {
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
     * @throws IOException 
     */
    public void testClientTraceInHeaders() throws IOException {
        Log.d(TAG, "test get" + android.os.Process.myTid());

        WebRequestHandler request = new WebRequestHandler();
        HttpWebResponse httpResponse = request.sendGet(getUrl(TEST_WEBAPI_URL),
                getTestHeaders("testClientTraceInHeaders", "valueYes"));

        assertNotNull(httpResponse != null);
        assertTrue("status is 200", httpResponse.getStatusCode() == 200);
        String responseMsg = httpResponse.getBody();
        assertTrue("request header check", responseMsg.contains(AAD.ADAL_ID_PLATFORM + "-Android"));
        assertTrue(
                "request header check",
                responseMsg.contains(AAD.ADAL_ID_VERSION + "-"
                        + AuthenticationContext.getVersionName()));
    }

    public void testNonExistentUrl() {
        WebRequestHandler request = new WebRequestHandler();
        try {
            request.sendGet(
                    getUrl("http://www.somethingabcddnotexists.com"), null);
            fail("Unreachable host, should throw IOException");
        } catch (final IOException e) {
            assertTrue(e instanceof UnknownHostException);
            assertTrue(e.getMessage().toLowerCase(Locale.US).contains("unable to resolve host"));
        }
    }

    public void testGetWithIdRequest() throws IOException {
        WebRequestHandler request = new WebRequestHandler();
        HttpWebResponse httpResponse = request.sendGet(getUrl(TEST_WEBAPI_URL + "/1"), null);

        assertTrue("status is 200", httpResponse.getStatusCode() == 200);
        String responseMsg = new String(httpResponse.getBody());
        assertTrue("request body check", responseMsg.contains("test get with id"));
    }

    public void testPostRequest() throws IOException {
        final TestMessage message = new TestMessage("messagetest", "12345");
        HttpWebResponse httpResponse = null;
        WebRequestHandler request = new WebRequestHandler();
        String json = new Gson().toJson(message);

        httpResponse = request.sendPost(getUrl(TEST_WEBAPI_URL), null,
                json.getBytes(ENCODING_UTF8), "application/json");

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
