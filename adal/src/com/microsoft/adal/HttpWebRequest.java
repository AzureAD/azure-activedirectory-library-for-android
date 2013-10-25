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

import com.microsoft.adal.ErrorCodes.ADALError;

import android.os.AsyncTask;
import android.util.Log;

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

    String mRequestMethod;
    URL mUrl;
    HttpWebRequestCallback mCallback;

    byte[] mRequestContent = null;
    String mRequestContentType = null;

    HashMap<String, String> mRequestHeaders = null;

    boolean mUsedBefore = false;

    public HttpWebRequest(URL requestURL) {
        mUrl = requestURL;
        mRequestHeaders = new HashMap<String, String>();
        mRequestHeaders.put("Host", getURLAuthority(mUrl));
    }

    /**
     * Asynchronous GET.
     * 
     * @param callback
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public void sendAsyncGet(HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException {
        sendAsync(REQUEST_METHOD_GET, null, null, callback);
    }

    /**
     * Async delete
     * 
     * @param callback
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public void sendAsyncDelete(HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException {
        sendAsync(REQUEST_METHOD_DELETE, null, null, callback);
    }

    /**
     * send async put request
     * 
     * @param content
     * @param contentType
     * @param callback
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public void sendAsyncPut(byte[] content, String contentType,
            HttpWebRequestCallback callback) throws IllegalArgumentException,
            IOException {
        sendAsync(REQUEST_METHOD_PUT, content, contentType, callback);
    }

    /**
     * send async post request
     * 
     * @param content
     * @param contentType
     * @param callback
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public void sendAsyncPost(byte[] content, String contentType,
            HttpWebRequestCallback callback) throws IllegalArgumentException,
            IOException {
        sendAsync(REQUEST_METHOD_POST, content, contentType, callback);
    }

    /**
     * Async task Step1: This should not be called directly.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "Async task onPreExecute");

    }

    /**
     * Async task Step2: background work
     */
    @Override
    protected HttpWebResponse doInBackground(Void... empty) {
        // We could be carrying an exception here from the onPreExecute step
        // so only try to send the request if everything is clean.
        HttpWebResponse _response = new HttpWebResponse();
        HttpURLConnection _connection = null;

        _connection = openConnection(_response, _connection);

        if (_connection != null)
        {
            try {
                // Apply the request headers
                final Iterator<String> headerKeys = mRequestHeaders.keySet()
                        .iterator();

                while (headerKeys.hasNext()) {
                    String header = headerKeys.next();
                    Log.d(TAG, "Setting header: " + header);
                    _connection.setRequestProperty(header,
                            mRequestHeaders.get(header));
                }

                _connection.setInstanceFollowRedirects(true);
                _connection.setUseCaches(false);
                _connection.setRequestMethod(mRequestMethod);

                setRequestBody(_connection);

                // Get the response to the request along with the response
                // body
                int statusCode = _connection.getResponseCode();
                _response.setStatusCode(statusCode);
                Log.d(TAG, "Statuscode:" + statusCode);

                byte[] responseBody = null;
                InputStream responseStream = null;

                try {
                    responseStream = _connection.getInputStream();
                } catch (IOException ex) {
                    Log.d(TAG, "IOException:" + ex.getMessage());
                    responseStream = _connection.getErrorStream();
                }

                if (responseStream != null) {

                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead = -1;

                    while (!isCancelled() && (bytesRead = responseStream.read(buffer)) > 0) {
                        byteStream.write(buffer, 0, bytesRead);
                    }

                    responseBody = byteStream.toByteArray();
                }

                Log.d(TAG, "Response is received");
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
                Log.e(TAG, "Exception" + e.getMessage());

                _response.setResponseException(e);
            } finally {
                _connection.disconnect();
                _connection = null;
            }
        }

        return _response;
    }

    /**
     * Async task final step
     */
    @Override
    protected void onPostExecute(HttpWebResponse response) {
        super.onPostExecute(response);
        Log.d(TAG, "OnPostExecute");
        if (response != null) {
            if (null != mCallback) {
                mCallback.onComplete(response);
            }
        }
    }

    /**
     * Asynchronous POST/PUT.
     */
    private void sendAsync(String requestmethod, byte[] content,
            String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException {

        if (!mUsedBefore) {
            mUsedBefore = true;
        } else {
            // Async task will throw exception by Android system if it is reused again, but this is catching
            // misuse early.
            throw new AuthenticationError(ADALError.DEVELOPER_ASYNC_TASK_REUSED);
        }

        if (mUrl == null) {
            throw new IllegalArgumentException("requestURL");
        }

        if (!mUrl.getProtocol().equalsIgnoreCase("http")
                && !mUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("requestURL");
        }

        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        mRequestMethod = requestmethod;
        mCallback = callback;
        mRequestContent = content;
        mRequestContentType = contentType;

        // At Honeycomb, execute was changed to run on a serialized thread pool
        // but we want the request to run fully parallel so we force use of
        // the THREAD_POOL_EXECUTOR
        executeOnExecutor(THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    /**
     * open connection. If there is any error, set exception inside the response
     * 
     * @param _response
     * @param _connection
     * @return
     */
    private HttpURLConnection openConnection(HttpWebResponse _response,
            HttpURLConnection _connection) {
        try {

            _connection = (HttpURLConnection) mUrl.openConnection();
            _connection.setConnectTimeout(CONNECT_TIME_OUT);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            _response.setResponseException(e);
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
