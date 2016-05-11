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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.os.Build;
import android.os.Debug;
import android.os.Process;

/**
 * Webrequest are called in background thread from API level. HttpWebRequest
 * does not create another thread.
 */
class HttpWebRequest {
    static final String REQUEST_METHOD_POST = "POST";
    static final String REQUEST_METHOD_GET = "GET";
    private static final String UNAUTHORIZED_ERROR_MESSAGE_PRE18 = "Received authentication challenge is null";
    private static final String TAG = "HttpWebRequest";
    private static final int CONNECT_TIME_OUT = AuthenticationSettings.INSTANCE.getConnectTimeOut();
    private static final int READ_TIME_OUT = AuthenticationSettings.INSTANCE.getReadTimeOut();
    private static int sDebugSimulateDelay = 0;
    private boolean mUseCaches = false;
    private boolean mInstanceRedirectsFollow = true;
    private final String mRequestMethod;
    private final URL mUrl;
    private final byte[] mRequestContent;
    private final String mRequestContentType;
    private final Map<String, String> mRequestHeaders;

    public HttpWebRequest(URL requestURL, String requestMethod, Map<String, String> headers) {
        this(requestURL, requestMethod, headers, null, null);
    }

    public HttpWebRequest(
            URL requestURL,
            String requestMethod,
            Map<String, String> headers,
            byte[] requestContent,
            String requestContentType) {
        mUrl = requestURL;
        mRequestMethod = requestMethod;
        mRequestHeaders = new HashMap<String, String>();
        if (mUrl != null) {
            mRequestHeaders.put("Host", getURLAuthority(mUrl));
        }
        mRequestHeaders.putAll(headers);
        mRequestContent = requestContent;
        mRequestContentType = requestContentType;
    }

    /**
     * setupConnection before sending the request.
     */
    private HttpURLConnection setupConnection() throws IOException {
        Logger.v(TAG, "HttpWebRequest setupConnection thread:" + android.os.Process.myTid());
        if (mUrl == null) {
            throw new IllegalArgumentException("requestURL");
        }
        if (!mUrl.getProtocol().equalsIgnoreCase("http")
                && !mUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("requestURL");
        }
        HttpURLConnection.setFollowRedirects(true);
        final HttpURLConnection connection = HttpUrlConnectionFactory.createHttpUrlConnection(mUrl);
        connection.setConnectTimeout(CONNECT_TIME_OUT);
        // To prevent EOF exception.
        if (Build.VERSION.SDK_INT > 13) {
            connection.setRequestProperty("Connection", "close");
        }

        // Apply the request headers
        final Iterator<String> headerKeys = mRequestHeaders.keySet().iterator();

        while (headerKeys.hasNext()) {
            String header = headerKeys.next();
            Logger.v(TAG, "Setting header: " + header);
            connection.setRequestProperty(header, mRequestHeaders.get(header));
        }

        // Avoid reuse of existing sockets to avoid random EOF errors
        System.setProperty("http.keepAlive", "false");
        connection.setReadTimeout(READ_TIME_OUT);
        connection.setInstanceFollowRedirects(mInstanceRedirectsFollow);
        connection.setUseCaches(mUseCaches);
        connection.setRequestMethod(mRequestMethod);
        connection.setDoInput(true); // it will at least read status
                                     // code. Default is true.
        setRequestBody(connection, mRequestContent, mRequestContentType);

        return connection;
    }

    /**
     * send the request.
     */
    public HttpWebResponse send() throws IOException {
        Logger.v(TAG, "HttpWebRequest send thread:" + Process.myTid());
        final HttpURLConnection connection = setupConnection();
        final HttpWebResponse response;
        InputStream responseStream = null;
        try {
            try {
                responseStream = connection.getInputStream();
            } catch (IOException ex) {
                Logger.e(TAG, "IOException:" + ex.getMessage(), "", ADALError.SERVER_ERROR);
                // If it does not get the error stream, it will return
                // exception in the httpresponse
                responseStream = connection.getErrorStream();
                if (responseStream == null) {
                    throw ex;
                }
            }
            // GET request should read status after getInputStream to make
            // this work for different SDKs
            final int statusCode = getStatusCode(connection);
            final String responseBody = convertStreamToString(responseStream);

            // It will only run in debugger and set from outside for testing
            if (Debug.isDebuggerConnected() && sDebugSimulateDelay > 0) {
                // sleep background thread in debugging mode
                Logger.v(TAG, "Sleeping to simulate slow network response");
                try {
                    Thread.sleep(sDebugSimulateDelay);
                } catch (InterruptedException e) {
                    Logger.v(TAG, "Thread.sleep got interrupted exception " + e);
                }
            }

            Logger.v(TAG, "Response is received");
            response = new HttpWebResponse(statusCode, responseBody, connection.getHeaderFields());
        } finally {
            safeCloseStream(responseStream);
            connection.disconnect();
        }

        return response;
    }

    /**
     * Convert stream into the string
     *
     * @param is
     *            Input stream
     * @return The converted string
     * @throws IOException
     *             Thrown when failing to access input stream.
     */
    private static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }

            return sb.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private static int getStatusCode(HttpURLConnection connection) throws IOException {
        int statusCode = HttpURLConnection.HTTP_BAD_REQUEST;

        try {
            statusCode = connection.getResponseCode();
        } catch (IOException ex) {

            if (Build.VERSION.SDK_INT < 16) {
                // this exception is hardcoded in HttpUrlConnection class inside
                // Android source code for previous SDKs.
                // Status code handling is throwing exceptions if it does not
                // see challenge
                if (ex.getMessage().equals(UNAUTHORIZED_ERROR_MESSAGE_PRE18)) {
                    statusCode = HttpURLConnection.HTTP_UNAUTHORIZED;
                }
            } else {
                // HttpUrlConnection does not understand Bearer challenge
                // Second time query will get the correct status.
                // it will throw, if it is a different status related to
                // connection problem
                statusCode = connection.getResponseCode();
            }

            // if status is 200 or 401 after reading again, it can read the
            // response body for messages
            if (statusCode != HttpURLConnection.HTTP_OK
                    && statusCode != HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw ex;
            }
        }
        Logger.v(TAG, "Status code:" + statusCode);
        return statusCode;
    }

    private static void setRequestBody(HttpURLConnection connection, byte[] contentRequest, String requestContentType) throws IOException {
        if (null != contentRequest) {
            connection.setDoOutput(true);

            if (null != requestContentType && !requestContentType.isEmpty()) {
                connection.setRequestProperty("Content-Type", requestContentType);
            }

            connection.setRequestProperty("Content-Length",
                    Integer.toString(contentRequest.length));
            connection.setFixedLengthStreamingMode(contentRequest.length);

            OutputStream out = null;
            try {
                out = connection.getOutputStream();
                out.write(contentRequest);
            } finally {
                safeCloseStream(out);
            }
        }
    }

    /**
     * Close the stream safely
     *
     * @param stream stream to be closed
     */
    private static void safeCloseStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // swallow error in this case
                Logger.e(TAG, "Failed to close the stream: ", "", ADALError.IO_EXCEPTION, e);
            }
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
}
