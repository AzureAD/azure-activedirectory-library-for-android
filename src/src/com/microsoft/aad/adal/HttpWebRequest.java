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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import android.os.Build;

/**
 * Webrequest are called in background thread from API level. HttpWebRequest
 * does not create another thread.
 */
class HttpWebRequest {
    private static final String UNAUTHORIZED_ERROR_MESSAGE_PRE18 = "Received authentication challenge is null";

    private static final String TAG = "HttpWebRequest";

    static final String REQUEST_METHOD_POST = "POST";

    static final String REQUEST_METHOD_GET = "GET";

    static final String REQUEST_METHOD_PUT = "PUT";

    static final String REQUEST_METHOD_DELETE = "DELETE";

    static int CONNECT_TIME_OUT = AuthenticationSettings.INSTANCE.getConnectTimeOut();

    private static int READ_TIME_OUT = AuthenticationSettings.INSTANCE.getReadTimeOut();

    private static int sDebugSimulateDelay = 0;

    private boolean mUseCaches = false;

    private boolean mInstanceRedirectsFollow = true;

    private String mRequestMethod;

    URL mUrl;

    HttpURLConnection mConnection = null;

    byte[] mRequestContent = null;

    private String mRequestContentType = null;

    int mTimeOut = CONNECT_TIME_OUT;

    Exception mException = null;

    HashMap<String, String> mRequestHeaders = null;

    public HttpWebRequest(URL requestURL) {
        mUrl = requestURL;
        mRequestHeaders = new HashMap<String, String>();
        if (mUrl != null) {
            mRequestHeaders.put("Host", getURLAuthority(mUrl));
        }
    }

    public HttpWebRequest(URL requestURL, int timeout) {
        mUrl = requestURL;
        mRequestHeaders = new HashMap<String, String>();
        if (mUrl != null) {
            mRequestHeaders.put("Host", getURLAuthority(mUrl));
        }
        mTimeOut = timeout;
    }

    /**
     * setupConnection before sending the request.
     */
    private void setupConnection() {
        Logger.v(TAG, "HttpWebRequest setupConnection thread:" + android.os.Process.myTid());
        if (mUrl == null) {
            throw new IllegalArgumentException("requestURL");
        }
        if (!mUrl.getProtocol().equalsIgnoreCase("http")
                && !mUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("requestURL");
        }
        HttpURLConnection.setFollowRedirects(true);
        mConnection = openConnection();
        // To prevent EOF exception.
        if (Build.VERSION.SDK_INT > 13) {
            mConnection.setRequestProperty("Connection", "close");
        }
    }

    /**
     * send the request.
     * 
     * @param contentType
     */
    public HttpWebResponse send() {

        Logger.v(TAG, "HttpWebRequest send thread:" + android.os.Process.myTid());
        setupConnection();
        HttpWebResponse response = new HttpWebResponse();

        if (mConnection != null) {
            try {
                // Apply the request headers
                final Iterator<String> headerKeys = mRequestHeaders.keySet().iterator();

                while (headerKeys.hasNext()) {
                    String header = headerKeys.next();
                    Logger.v(TAG, "Setting header: " + header);
                    mConnection.setRequestProperty(header, mRequestHeaders.get(header));
                }

                // Avoid reuse of existing sockets to avoid random EOF errors
                System.setProperty("http.keepAlive", "false");
                mConnection.setReadTimeout(READ_TIME_OUT);
                mConnection.setInstanceFollowRedirects(mInstanceRedirectsFollow);
                mConnection.setUseCaches(mUseCaches);
                mConnection.setRequestMethod(mRequestMethod);
                mConnection.setDoInput(true); // it will at least read status
                                              // code. Default is true.
                setRequestBody();

                byte[] responseBody = null;
                InputStream responseStream = null;

                try {
                    responseStream = mConnection.getInputStream();
                } catch (IOException ex) {
                    Logger.e(TAG, "IOException:" + ex.getMessage(), "", ADALError.SERVER_ERROR);
                    // If it does not get the error stream, it will return
                    // exception in the httpresponse
                    responseStream = mConnection.getErrorStream();
                    mException = ex;
                }

                // GET request should read status after getInputStream to make
                // this work for different SDKs
                getStatusCode(response);

                if (responseStream != null) {

                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead = -1;

                    // Continue to read from stream if not cancelled and not EOF
                    while ((bytesRead = responseStream.read(buffer)) > 0) {
                        byteStream.write(buffer, 0, bytesRead);
                    }

                    responseBody = byteStream.toByteArray();
                }

                // It will only run in debugger and set from outside for testing
                if (android.os.Debug.isDebuggerConnected() && sDebugSimulateDelay > 0) {
                    // sleep background thread in debugging mode
                    Logger.v(TAG, "Sleeping to simulate slow network response");
                    Thread.sleep(sDebugSimulateDelay);
                }

                Logger.v(TAG, "Response is received");
                response.setBody(responseBody);
                response.setResponseHeaders(mConnection.getHeaderFields());
            } catch (Exception e) {
                Logger.e(TAG, "Exception:" + e.getMessage(), " Method:" + mRequestMethod,
                        ADALError.SERVER_ERROR, e);
                mException = e;
            } finally {
                mConnection.disconnect();
                mConnection = null;
            }
        }

        response.setResponseException(mException);
        return response;
    }

    private void getStatusCode(HttpWebResponse response) throws IOException {
        int statusCode = HttpURLConnection.HTTP_BAD_REQUEST;

        try {
            statusCode = mConnection.getResponseCode();
        } catch (IOException ex) {

            if (Build.VERSION.SDK_INT < 16) {
                // this exception is hardcoded in HttpUrlConnection class inside
                // Android source code for previous SDKs.
                // Status code handling is throwing exceptions if it does not
                // see challenge
                if (ex.getMessage() == UNAUTHORIZED_ERROR_MESSAGE_PRE18) {
                    statusCode = HttpURLConnection.HTTP_UNAUTHORIZED;
                }
            } else {
                // HttpUrlConnection does not understand Bearer challenge
                // Second time query will get the correct status.
                // it will throw, if it is a different status related to
                // connection problem
                statusCode = mConnection.getResponseCode();
            }

            // if status is 200 or 401 after reading again, it can read the
            // response body for messages
            if (statusCode != HttpURLConnection.HTTP_OK
                    && statusCode != HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw ex;
            }
        }
        response.setStatusCode(statusCode);
        Logger.v(TAG, "Status code:" + statusCode);
    }

    /**
     * open connection. If there is any error, set exception inside the response
     * 
     * @param _response
     * @return
     */
    private HttpURLConnection openConnection() {
        HttpURLConnection connection = null;
        try {

            connection = (HttpURLConnection)mUrl.openConnection();
            connection.setConnectTimeout(mTimeOut);

        } catch (IOException e) {
            mException = e;
        }
        return connection;
    }

    private void setRequestBody() throws IOException {
        if (null != mRequestContent) {
            mConnection.setDoOutput(true);

            if (null != getRequestContentType() && !getRequestContentType().isEmpty()) {
                mConnection.setRequestProperty("Content-Type", getRequestContentType());
            }

            mConnection.setRequestProperty("Content-Length",
                    Integer.toString(mRequestContent.length));
            mConnection.setFixedLengthStreamingMode(mRequestContent.length);

            OutputStream out = mConnection.getOutputStream();
            out.write(mRequestContent);
            out.close();
        }
    }

    private static String getURLAuthority(URL requestURL) {
        // We assume that the parameter has already passed the tests in
        // validateRequestURI
        String authority = requestURL.getAuthority();

        if (requestURL.getPort() == -1) {
            // No port in the URI so append a default using the
            // scheme specified in the URI; only http and https are
            // supported
            if (requestURL.getProtocol().equalsIgnoreCase("http")) {
                authority = authority + ":80";
            } else if (requestURL.getProtocol().equalsIgnoreCase("https")) {
                authority = authority + ":443";
            }
        }

        return authority;
    }

    /**
     * The requests target URL.
     */
    URL getURL() {
        return mUrl;
    }

    /**
     * The request headers.
     */
    public HashMap<String, String> getRequestHeaders() {
        return mRequestHeaders;
    }

    void setRequestMethod(String requestMethod) {
        this.mRequestMethod = requestMethod;
    }

    String getRequestContentType() {
        return mRequestContentType;
    }

    void setRequestContentType(String requestContentType) {
        this.mRequestContentType = requestContentType;
    }

    void setRequestContent(byte[] data) {
        this.mRequestContent = data;
    }
}
