
package com.microsoft.adal.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import android.os.AsyncTask;
import android.test.AndroidTestCase;
import android.util.Log;

import com.google.gson.Gson;
import com.microsoft.adal.AuthenticationCancelError;
import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.WebRequestHandler;

/**
 * webrequest tests related to get, put, post, delete requests
 */
public class WebRequestHandlerTests extends AndroidTestHelper {

    private final static String TEST_WEBAPI_URL = "http://graphtestrun.azurewebsites.net/api/WebRequestTest";

    protected static final String TAG = "WebRequestHandlerTests";

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
    }

    public void testGetRequest() {
        final CountDownLatch signal = new CountDownLatch(1);

        testAsyncNoException(signal, new Runnable() {

            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(getUrl(TEST_WEBAPI_URL),
                        getTestHeaders("testabc", "value123"),
                        new HttpWebRequestCallback() {

                            @Override
                            public void onComplete(HttpWebResponse response, Exception exc) {
                                assertTrue("exception is null",
                                        exc == null);
                                assertTrue("status is 200", response.getStatusCode() == 200);
                                String responseMsg = new String(response.getBody());
                                assertTrue("request header check",
                                        responseMsg.contains("testabc-value123"));
                                signal.countDown();
                            }
                        });
            }
        });
    }

    public void testNonExistentUrl() {
        final CountDownLatch signal = new CountDownLatch(1);
        testAsyncNoException(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(getUrl("http://www.somethingabcddnotexists.com"),
                        null,
                        new HttpWebRequestCallback() {

                            @Override
                            public void onComplete(HttpWebResponse response, Exception exc) {
                                assertTrue("exception is not null",
                                        exc != null);
                                assertTrue(
                                        "Unable to resolve host",
                                        exc.getMessage().toLowerCase()
                                                .contains("unable to resolve host"));
                                signal.countDown();
                            }
                        });
            }
        });

    }

    public void testGetWithIdRequest() {
        final CountDownLatch signal = new CountDownLatch(1);

        testAsyncNoException(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                request.sendAsyncGet(getUrl(TEST_WEBAPI_URL
                        + "/1"),
                        null,
                        new HttpWebRequestCallback() {

                            @Override
                            public void onComplete(HttpWebResponse response, Exception exc) {
                                assertTrue("exception is null",
                                        response.getResponseException() == null);
                                assertTrue("status is 200", response.getStatusCode() == 200);
                                String responseMsg = new String(response.getBody());
                                assertTrue("request body check",
                                        responseMsg.contains("test get with id"));
                                signal.countDown();
                            }
                        });
            }
        });
    }

    public void testPostRequest() {
        final CountDownLatch signal = new CountDownLatch(1);

        testAsyncNoException(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                final TestMessage message = new TestMessage("messagetest", "12345");
                String json = new Gson().toJson(message);

                try {
                    request.sendAsyncPost(
                            getUrl(TEST_WEBAPI_URL),
                            null,
                            json.getBytes(ENCODING_UTF8),
                            "application/json", new HttpWebRequestCallback() {

                                @Override
                                public void onComplete(HttpWebResponse response, Exception exc) {
                                    assertTrue("exception is null", exc == null);
                                    assertTrue("status is 200",
                                            response.getStatusCode() == 200);
                                    String responseMsg = new String(response.getBody());
                                    assertTrue("request body check", responseMsg
                                            .contains(message.getAccessToken()
                                                    + message.getUserName()));
                                    signal.countDown();
                                }

                            });
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });
    }

    public void testDeleteRequest() {
        final CountDownLatch signal = new CountDownLatch(1);

        testAsyncNoException(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();

                request.sendAsyncDelete(getUrl(TEST_WEBAPI_URL + "/1"),
                        null,
                        new HttpWebRequestCallback() {

                            @Override
                            public void onComplete(HttpWebResponse response, Exception exc) {
                                assertTrue("exception is null", exc == null);
                                assertTrue("status is 204", response.getStatusCode() == 204);
                                signal.countDown();
                            }

                        });
            }
        });
    }

    /**
     * test update call
     */
    public void testPutRequest() {
        final CountDownLatch signal = new CountDownLatch(1);

        testAsyncNoException(signal, new Runnable() {
            @Override
            public void run() {
                WebRequestHandler request = new WebRequestHandler();
                final TestMessage message = new TestMessage("messagetest", "12345");
                String json = new Gson().toJson(message);

                try {
                    request.sendAsyncPut(getUrl(TEST_WEBAPI_URL + "/147"),
                            null,
                            json.getBytes(ENCODING_UTF8),
                            "application/json", new HttpWebRequestCallback() {

                                @Override
                                public void onComplete(HttpWebResponse response, Exception exc) {
                                    assertTrue("exception is null", exc == null);
                                    assertTrue("status is 200",
                                            response.getStatusCode() == 200);
                                    String responseMsg = new String(response.getBody());
                                    assertTrue(
                                            "request body check",
                                            responseMsg.contains("147"
                                                    + message.getAccessToken()
                                                    + message.getUserName()));
                                    signal.countDown();
                                }

                            });
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * test update call
     */
    public void testCancelRequest() {
        final CountDownLatch signal = new CountDownLatch(1);
        AsyncTask<?, ?, ?> handle = null;
        try {
            WebRequestHandler request = new WebRequestHandler();
            final TestMessage message = new TestMessage("cancelRequest", "3342");
            String json = new Gson().toJson(message);
            handle = request.sendAsyncPut(
                    new URL(TEST_WEBAPI_URL + "/347"),
                    null,
                    json.getBytes(ENCODING_UTF8),
                    "application/json", new HttpWebRequestCallback() {

                        @Override
                        public void onComplete(HttpWebResponse response, Exception exc) {
                            assertTrue("exception is not null", exc != null);
                            assertTrue(exc instanceof AuthenticationCancelError);
                            Log.d(TAG, "oncomplete cancel request test");
                            signal.countDown();
                        }
                    });

            handle.cancel(true); // thread interrupt

        } catch (Exception ex) {
            assertFalse("not expected", true);
            signal.countDown();
        }

        try {
            signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);

            // Verify that it is cancelled
            if (handle != null)
            {
                assertTrue("it is cancelled", handle.isCancelled());
            }
        } catch (InterruptedException e) {
            assertFalse("InterruptedException is not expected", true);
        }
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

    private HashMap<String, String> getTestHeaders(String key, String value)
    {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(key, value);
        return headers;
    }

    private URL getUrl(String url)
    {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
