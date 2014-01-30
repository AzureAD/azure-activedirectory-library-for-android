
package com.microsoft.adal.test;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import android.os.AsyncTask;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.google.gson.Gson;
import com.microsoft.adal.AuthenticationCancelError;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.WebRequestHandler;
import com.microsoft.adal.test.AuthenticationConstants.AAD;

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
     */
    @SmallTest
    public void testCorrelationIdInRequest() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        final String testUrl = "https://login.windows.net/omercantest.onmicrosoft.com/oauth2/token";
        final UUID testID = UUID.randomUUID();
        Log.d(TAG, "Test correlationid:" + testID.toString());
        final TestResponse testResponse = sendCorrelationIdRequest(testUrl, testID, false);

        assertEquals("400 error code", 400, testResponse.httpResponse.getStatusCode());
        assertNotNull("Callback should report 400 for this error", testResponse.exception);
        assertNotNull("webresponse is not null", testResponse.httpResponse);
        assertEquals("same correlationid", testID.toString(), testResponse.httpResponse.getResponseHeaders().get(AuthenticationConstants.AAD.CLIENT_REQUEST_ID).get(0));

        // same id for next request
        TestResponse testResponse2 = sendCorrelationIdRequest(testUrl, testID, true);
        AuthenticationResult result2 = getAuthenticationResult(testResponse2.httpResponse);
        assertEquals("same correlationid", testID.toString(), testResponse2.httpResponse.getResponseHeaders().get(AuthenticationConstants.AAD.CLIENT_REQUEST_ID).get(0));
    }

    private TestResponse sendCorrelationIdRequest(final String message, final UUID testID,
            final boolean withoutHeader) {

        final CountDownLatch signal = new CountDownLatch(1);
        final TestResponse testResponse = new TestResponse();
        final HttpWebRequestCallback callback = setupCallback(signal, testResponse);

        Log.d(TAG, "test get" + android.os.Process.myTid());

        // POst request with invalid request message
        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.setRequestCorrelationId(testID);
                HashMap<String, String> headers = null;
                if (!withoutHeader) {
                    headers = new HashMap<String, String>();
                    headers.put("Accept", "application/json");
                }
                request.sendAsyncPost(getUrl(message), headers, null,
                        "application/x-www-form-urlencoded", callback);
            }
        }, true);

        return testResponse;
    }

    public void testNullUrl() {

        assertThrowsException(IllegalArgumentException.class, "url", new Runnable() {
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(null, null, new HttpWebRequestCallback() {

                    @Override
                    public void onComplete(HttpWebResponse response, Exception exception) {

                    }
                });
            }
        });
    }

    public void testWrongSchemeUrl() {

        assertThrowsException(IllegalArgumentException.class, "url", new Runnable() {
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(getUrl("ftp://test.com"), null, new HttpWebRequestCallback() {

                    @Override
                    public void onComplete(HttpWebResponse response, Exception exception) {

                    }
                });
            }
        });
    }

    public void testEmptyCallback() {

        assertThrowsException(IllegalArgumentException.class, "callback", new Runnable() {
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(getUrl("https://www.bing.com"), null, null);
            }
        });

        assertThrowsException(IllegalArgumentException.class, "callback", new Runnable() {
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncDelete(getUrl("https://www.bing.com"), null, null);
            }
        });

        assertThrowsException(IllegalArgumentException.class, "callback", new Runnable() {
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncPut(getUrl("https://www.bing.com"), null, null, null, null);
            }
        });

        assertThrowsException(IllegalArgumentException.class, "callback", new Runnable() {
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncPost(getUrl("https://www.bing.com"), null, null, null, null);
            }
        });
    }

    class TestResponse {
        HttpWebResponse httpResponse;

        Exception exception;
    }

    public void testGetRequest() {
        final CountDownLatch signal = new CountDownLatch(1);
        final TestResponse testResponse = new TestResponse();
        final HttpWebRequestCallback callback = setupCallback(signal, testResponse);

        Log.d(TAG, "test get" + android.os.Process.myTid());

        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(getUrl(TEST_WEBAPI_URL),
                        getTestHeaders("testabc", "value123"), callback);
            }
        }, true);

        assertNull(testResponse.exception);
        assertNotNull(testResponse.httpResponse != null);
        assertTrue("status is 200", testResponse.httpResponse.getStatusCode() == 200);
        String responseMsg = new String(testResponse.httpResponse.getBody());
        assertTrue("request header check", responseMsg.contains("testabc-value123"));
    }

    /**
     * WebService returns the request headers in the response
     */
    public void testClientTraceInHeaders() {
        final CountDownLatch signal = new CountDownLatch(1);
        final TestResponse testResponse = new TestResponse();
        final HttpWebRequestCallback callback = setupCallback(signal, testResponse);
        Log.d(TAG, "test get" + android.os.Process.myTid());

        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(getUrl(TEST_WEBAPI_URL),
                        getTestHeaders("testClientTraceInHeaders", "valueYes"), callback);
            }
        }, true);

        assertNull(testResponse.exception);
        assertNotNull(testResponse.httpResponse != null);
        assertTrue("status is 200", testResponse.httpResponse.getStatusCode() == 200);
        String responseMsg = new String(testResponse.httpResponse.getBody());
        assertTrue("request header check", responseMsg.contains(AAD.ADAL_ID_PLATFORM + "-Android"));
        assertTrue(
                "request header check",
                responseMsg.contains(AAD.ADAL_ID_VERSION + "-"
                        + AuthenticationContext.getVersionName()));
    }

    private HttpWebRequestCallback setupCallback(final CountDownLatch signal,
            final TestResponse testResponse) {
        final HttpWebRequestCallback callback = new HttpWebRequestCallback() {
            @Override
            public void onComplete(HttpWebResponse response, Exception exc) {
                testResponse.httpResponse = response;
                testResponse.exception = exc;
                Log.d(TAG, "test " + android.os.Process.myTid());
                signal.countDown();
            }
        };
        return callback;
    }

    public void testNonExistentUrl() {
        final CountDownLatch signal = new CountDownLatch(1);
        final TestResponse testResponse = new TestResponse();
        final HttpWebRequestCallback callback = setupCallback(signal, testResponse);

        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(getUrl("http://www.somethingabcddnotexists.com"), null,
                        callback);
            }
        }, true);

        assertNotNull(testResponse.exception);
        assertTrue("Unknown host exception", testResponse.exception instanceof UnknownHostException);
        assertTrue("Unable to resolve host", testResponse.exception.getMessage().toLowerCase()
                .contains("unable to resolve host"));
    }

    public void testGetWithIdRequest() {
        final CountDownLatch signal = new CountDownLatch(1);
        final TestResponse testResponse = new TestResponse();
        final HttpWebRequestCallback callback = setupCallback(signal, testResponse);

        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(getUrl(TEST_WEBAPI_URL + "/1"), null, callback);
            }
        }, true);

        assertNull(testResponse.httpResponse.getResponseException());
        assertTrue("status is 200", testResponse.httpResponse.getStatusCode() == 200);
        String responseMsg = new String(testResponse.httpResponse.getBody());
        assertTrue("request body check", responseMsg.contains("test get with id"));
    }

    public void testPostRequest() {
        final CountDownLatch signal = new CountDownLatch(1);
        final TestResponse testResponse = new TestResponse();
        final HttpWebRequestCallback callback = setupCallback(signal, testResponse);
        final TestMessage message = new TestMessage("messagetest", "12345");

        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                String json = new Gson().toJson(message);

                try {
                    request.sendAsyncPost(getUrl(TEST_WEBAPI_URL), null,
                            json.getBytes(ENCODING_UTF8), "application/json", callback);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }, true);

        assertNull(testResponse.exception);
        assertTrue("status is 200", testResponse.httpResponse.getStatusCode() == 200);
        String responseMsg = new String(testResponse.httpResponse.getBody());
        assertTrue("request body check",
                responseMsg.contains(message.getAccessToken() + message.getUserName()));
    }

    public void testDeleteRequest() {
        final CountDownLatch signal = new CountDownLatch(1);
        final TestResponse testResponse = new TestResponse();
        final HttpWebRequestCallback callback = setupCallback(signal, testResponse);

        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();

                request.sendAsyncDelete(getUrl(TEST_WEBAPI_URL + "/1"), null, callback);
            }
        }, true);

        assertNull(testResponse.exception);
        assertTrue("status is 204", testResponse.httpResponse.getStatusCode() == 204);
    }

    /**
     * test update call
     */
    public void testPutRequest() {
        final CountDownLatch signal = new CountDownLatch(1);
        final TestResponse testResponse = new TestResponse();
        final HttpWebRequestCallback callback = setupCallback(signal, testResponse);
        final TestMessage message = new TestMessage("messagetest", "12345");

        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                String json = new Gson().toJson(message);

                try {
                    request.sendAsyncPut(getUrl(TEST_WEBAPI_URL + "/147"), null,
                            json.getBytes(ENCODING_UTF8), "application/json", callback);

                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }, true);

        assertNull(testResponse.exception);
        assertTrue("status is 200", testResponse.httpResponse.getStatusCode() == 200);
        String responseMsg = new String(testResponse.httpResponse.getBody());
        assertTrue("request body check",
                responseMsg.contains("147" + message.getAccessToken() + message.getUserName()));
    }

    /**
     * test update call
     */
    public void testCancelRequest() {
        final CountDownLatch signal = new CountDownLatch(1);
        final TestResponse testResponse = new TestResponse();
        final HttpWebRequestCallback callback = setupCallback(signal, testResponse);

        Log.d(TAG, "test cancel" + android.os.Process.myTid());

        testAsyncNoExceptionUIOption(signal, new Runnable() {
            @Override
            public void run() {
                AsyncTask<?, ?, ?> handle = null;
                WebRequestHandler request = new WebRequestHandler();
                handle = request.sendAsyncGet(getUrl(TEST_WEBAPI_URL + "/347"), null, callback);

                assertFalse("it is not cancelled", handle.isCancelled());
                handle.cancel(true);
                assertTrue("it is not cancelled", handle.isCancelled());
            }
        }, true);

        Log.d(TAG, "test cancel" + android.os.Process.myTid());
        assertNotNull(testResponse.exception);
        assertTrue(testResponse.exception instanceof AuthenticationCancelError);
        Log.d(TAG, "oncomplete cancel request test");
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

    private AuthenticationResult getAuthenticationResult(HttpWebResponse webResponse)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        AuthenticationResult result = null;
        Object authenticationRequest = OauthTests.createAuthenticationRequest(
                "https://login.windows.net/aaaty", "resource", "client", "redirect", "loginhint",
                null, null, null);
        Object oauth = OauthTests.createOAuthInstance(authenticationRequest);
        Method m = ReflectionUtils.getTestMethod(oauth, "processTokenResponse",
                HttpWebResponse.class);

        // call for empty response
        return (AuthenticationResult)m.invoke(oauth, webResponse);
    }
}
