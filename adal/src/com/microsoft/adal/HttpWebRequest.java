/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

/**
 * AsyncTask can be executed only once. It throws exception for multiple async
 * calls on same object. Do not call onPreExecute, onPostExecute or
 * doInBackground directly
 */
class HttpWebRequest extends AsyncTask<Void, Void, HttpWebResponse> {
    private static final String TAG = "HttpWebRequest";

    final static String REQUEST_METHOD_POST = "POST";

    final static String REQUEST_METHOD_GET = "GET";

    final static String REQUEST_METHOD_PUT = "PUT";

    final static String REQUEST_METHOD_DELETE = "DELETE";

    final static int CONNECT_TIME_OUT = 10000;

    private final static int READ_TIME_OUT = 10000;

    private static int sDebugSimulateDelay = 0;

    private boolean mUseCaches = false;

    private boolean mInstanceRedirectsFollow = true;

    String mRequestMethod;

    URL mUrl;

    HttpWebRequestCallback mCallback;

    HttpURLConnection _connection = null;

    byte[] mRequestContent = null;

    String mRequestContentType = null;

    int mTimeOut = CONNECT_TIME_OUT;

    Exception mException = null;

    HashMap<String, String> mRequestHeaders = null;

    /**
     * Async task can be only used once.
     */
    AtomicBoolean mUsedBefore = new AtomicBoolean(false);

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
     * Asynchronous GET.
     * 
     * @param callback
     */
    public void sendAsyncGet(HttpWebRequestCallback callback) {
        sendAsync(REQUEST_METHOD_GET, null, null, callback);
    }

    /**
     * Async delete
     * 
     * @param callback
     */
    public void sendAsyncDelete(HttpWebRequestCallback callback) {
        sendAsync(REQUEST_METHOD_DELETE, null, null, callback);
    }

    /**
     * send async put request
     * 
     * @param content
     * @param contentType
     * @param callback
     */
    public void sendAsyncPut(byte[] content, String contentType, HttpWebRequestCallback callback) {
        sendAsync(REQUEST_METHOD_PUT, content, contentType, callback);
    }

    /**
     * send async post request
     * 
     * @param content
     * @param contentType
     * @param callback
     */
    public void sendAsyncPost(byte[] content, String contentType, HttpWebRequestCallback callback) {
        sendAsync(REQUEST_METHOD_POST, content, contentType, callback);
    }

    /**
     * Async task Step1: This should not be called directly.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Logger.d(TAG, "HttpWebRequest onPreExecute thread:" + android.os.Process.myTid());
        HttpURLConnection.setFollowRedirects(true);
        _connection = openConnection(_connection);
    }

    /**
     * Async task Step2: background work
     */
    @Override
    protected HttpWebResponse doInBackground(Void... empty) {

        Logger.d(TAG, "HttpWebRequest doInBackground thread:" + android.os.Process.myTid());
        HttpWebResponse _response = new HttpWebResponse();

        if (_connection != null) {
            try {
                // Apply the request headers
                final Iterator<String> headerKeys = mRequestHeaders.keySet().iterator();

                while (headerKeys.hasNext()) {
                    String header = headerKeys.next();
                    Logger.d(TAG, "Setting header: " + header);
                    _connection.setRequestProperty(header, mRequestHeaders.get(header));
                }

                _connection.setReadTimeout(READ_TIME_OUT);
                _connection.setInstanceFollowRedirects(mInstanceRedirectsFollow);
                _connection.setUseCaches(mUseCaches);
                _connection.setRequestMethod(mRequestMethod);
                setRequestBody(_connection);

                // Get the response to the request along with the response
                // body
                int statusCode = HttpURLConnection.HTTP_OK;

                try {
                    statusCode = _connection.getResponseCode();
                } catch (IOException ex) {
                    // HttpUrlConnection does not understand Bearer challenge
                    // Second time query will get the correct status.
                    // it will throw, if it is a different status related to
                    // connection problem
                    statusCode = _connection.getResponseCode();
                    if (statusCode != HttpURLConnection.HTTP_UNAUTHORIZED) {
                        throw ex;
                    }
                }

                _response.setStatusCode(statusCode);
                Logger.d(TAG, "Statuscode:" + statusCode);

                byte[] responseBody = null;
                InputStream responseStream = null;

                try {
                    responseStream = _connection.getInputStream();
                } catch (IOException ex) {
                    Logger.d(TAG, "IOException:" + ex.getMessage());
                    mException = ex;
                    responseStream = _connection.getErrorStream();
                }

                if (responseStream != null) {

                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead = -1;

                    // Continue to read from stream if not cancelled and not EOF
                    while (!isCancelled() && (bytesRead = responseStream.read(buffer)) > 0) {
                        byteStream.write(buffer, 0, bytesRead);
                    }

                    responseBody = byteStream.toByteArray();
                }

                // It will only run in debugger and set from outside for testing
                if (android.os.Debug.isDebuggerConnected() && sDebugSimulateDelay > 0) {
                    // sleep background thread in debugging mode
                    Logger.d(TAG, "Sleeping to simulate slow network response");
                    Thread.sleep(sDebugSimulateDelay);
                }

                Logger.d(TAG, "Response is received");
                _response.setBody(responseBody);
                _response.setResponseHeaders(_connection.getHeaderFields());
            }
            // TODO: Exceptions that occur here need to be channeled back
            // to the original caller instead of returning a null response.
            // NOTE: On Android API 17, incorrectly formatted
            // WWW-Authenticate
            // headers can cause this exception on a 401 or 407 response
            // from
            // the server: all parameters in the challenge must have quote
            // marks.
            catch (Exception e) {
                Logger.d(TAG, "Exception:" + e.getMessage());

                mException = e;
            } finally {
                _connection.disconnect();
                _connection = null;
            }
        }

        return _response;
    }

    /**
     * after task is cancelled and doInBackground finished, it will call this
     * method instead of onPostExecute. This can do UI thread work similar to
     * onPostExecute.
     */
    @Override
    protected void onCancelled() {
        Logger.d(TAG, "HttpWebRequest onCancelled thread:" + android.os.Process.myTid());
        if (null != mCallback) {
            mCallback.onComplete(null, new AuthenticationCancelError());
        }
    }

    /**
     * Async task final step
     */
    @Override
    protected void onPostExecute(HttpWebResponse response) {
        super.onPostExecute(response);
        Logger.d(TAG, "HttpWebRequest OnPostExecute thread:" + android.os.Process.myTid());
        if (null != mCallback) {
            mCallback.onComplete(response, mException);
        }
    }

    /**
     * Asynchronous POST/PUT Argument check before sending the request
     */
    private void sendAsync(String requestmethod, byte[] content, String contentType,
            HttpWebRequestCallback callback) {

        Logger.d(TAG, "HttpWebRequest thread:" + android.os.Process.myTid());

        // Atomically sets to the given value and returns the previous value
        if (mUsedBefore.getAndSet(true)) {

            // Async task will throw exception by Android system if it is reused
            // again, but this is catching
            // misuse early.
            throw new AuthenticationException(ADALError.DEVELOPER_ASYNC_TASK_REUSED);
        }

        if (mUrl == null) {
            throw new IllegalArgumentException("requestURL");
        }

        if (!mUrl.getProtocol().equalsIgnoreCase("http")
                && !mUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("requestURL");
        }

        mRequestMethod = requestmethod;
        mCallback = callback;
        mRequestContent = content;
        mRequestContentType = contentType;
        mException = null;

        // AsyncTasks were initially executed serially on a single background
        // thread.
        // Starting with DONUT, this was changed to a pool of threads allowing
        // multiple tasks to operate in parallel.
        // Starting with HONEYCOMB, tasks are executed on a single thread
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            executeParallel();
        } else {
            execute((Void[])null);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void executeParallel() {
        // At Honeycomb, execute was changed to run on a serialized thread pool
        // but we want the request to run fully parallel so we force use of
        // the THREAD_POOL_EXECUTOR
        executeOnExecutor(THREAD_POOL_EXECUTOR, (Void[])null);
    }

    /**
     * open connection. If there is any error, set exception inside the response
     * 
     * @param _response
     * @param _connection
     * @return
     */
    private HttpURLConnection openConnection(HttpURLConnection _connection) {
        try {

            _connection = (HttpURLConnection)mUrl.openConnection();
            _connection.setConnectTimeout(mTimeOut);

        } catch (IOException e) {
            Logger.d(TAG, e.getMessage());
            mException = e;
            _connection.disconnect();
            _connection = null;
        }
        return _connection;
    }

    private void setRequestBody(HttpURLConnection connection) throws IOException {
        if (null != mRequestContent) {
            connection.setDoOutput(true);

            if (null != mRequestContentType && !mRequestContentType.isEmpty()) {
                connection.setRequestProperty("Content-Type", mRequestContentType);
            }

            connection.setRequestProperty("Content-Length",
                    Integer.toString(mRequestContent.length));
            connection.setFixedLengthStreamingMode(mRequestContent.length);

            OutputStream out = connection.getOutputStream();
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
     * The requests target URL
     */
    URL getURL() {
        return mUrl;
    }

    /**
     * The request headers
     */
    public HashMap<String, String> getRequestHeaders() {
        return mRequestHeaders;
    }
}
