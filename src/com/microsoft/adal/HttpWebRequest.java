/*
 * Copyright Microsoft Corporation (c) All Rights Reserved.
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

import android.os.AsyncTask;
import android.util.Log;

/**
 * MOVED from Hervey's code HttpWebRequest is a simple wrapper over
 * HttpUrlConnection that makes sending requests and receiving responses a
 * little simpler. This class is intentionally simple; it does not support
 * authentication or custom certificate validation, etc.
 */
public class HttpWebRequest extends AsyncTask<Void, Void, WebResponse>
{
    private static final String TAG = "HttpWebRequest";

    final static String REQUEST_METHOD_POST = "POST";
    final static String REQUEST_METHOD_GET = "GET";
    final static String REQUEST_METHOD_PUT = "PUT";
    final static String REQUEST_METHOD_DELETE = "DELETE";

    String _requestMethod;
    URL _url;
    HttpWebRequestCallback _callback;

    byte[] _content = null;
    String _contentType = null;

    HttpURLConnection _connection = null;

    HashMap<String, String> _requestHeaders = null;

    WebResponse _response = null;

    public HttpWebRequest(URL requestURL)
    {
        if (requestURL == null)
        {
            throw new IllegalArgumentException("requestURL");
        }

        if (!requestURL.getProtocol().equalsIgnoreCase("http") &&
                !requestURL.getProtocol().equalsIgnoreCase("https"))
        {
            throw new IllegalArgumentException("requestURL");
        }

        _url = requestURL;
        _requestHeaders = new HashMap<String, String>();

        _requestHeaders.put("Host", getURLAuthority(_url));
    }

    @Override
    protected WebResponse doInBackground(Void... empty)
    {
        // We could be carrying an exception here from the onPreExecute step
        // so only try to send the request if everything is clean.
        if (_response.getResponseException() == null && !_response.isProcessing())
        {
            _response.setProcessing(true);
            _connection.setConnectTimeout(10000); // 10 second timeout

            try
            {
                // Apply the users request headers
                final Iterator<String> headerKeys = _requestHeaders.keySet().iterator();

                while (headerKeys.hasNext())
                {
                    String header = headerKeys.next();
                    Log.d(TAG, "setting header" + header);
                    _connection.setRequestProperty(header, _requestHeaders.get(header));
                }
                Log.d(TAG, "request method" + _requestMethod);
                _connection.setInstanceFollowRedirects(false);
                _connection.setUseCaches(false);
                _connection.setRequestMethod(_requestMethod);
                setRequestBody();

                // Get the response to the request along with the response body
                int statusCode = 200;
                try{
                    statusCode = _connection.getResponseCode();
                }
                catch(IOException ex)
                {
                    // second time it will return correct code.
                    statusCode = _connection.getResponseCode();
                }
                byte[] responseBody = null;
                InputStream responseStream = null;
                Log.d(TAG, "Statuscode" + statusCode);

                try
                {
                    responseStream = _connection.getInputStream();
                } catch (IOException ex)
                {
                    // For some failures, the data we need is in the error
                    // stream
                    Log.d(TAG, "IOException" + ex.getMessage());
                    responseStream = _connection.getErrorStream();
                }

                if (responseStream != null)
                {
                    // Always treat the response as an array of bytes and do not
                    // attempt any kind of content type parsing. Most responses
                    // should
                    // be below the 4096 bytes that is the buffer size.
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

                    byte[] buffer = new byte[4096];
                    int bytesRead = -1;

                    while ((bytesRead = responseStream.read(buffer)) > 0)
                    {
                        byteStream.write(buffer, 0, bytesRead);
                    }

                    responseBody = byteStream.toByteArray();
                }

                Log.d(TAG, " new HttpWebResponse");
                _response.setResponse(new HttpWebResponse(statusCode, responseBody, _connection
                        .getHeaderFields()));
            }
            // TODO: Exceptions that occur here need to be channeled back
            // to the original caller instead of returning a null response.
            // NOTE: On Android API 17, incorrectly formatted WWW-Authenticate
            // headers can cause this exception on a 401 or 407 response from
            // the server: all parameters in the challenge must have quote
            // marks.
            catch (Exception e)
            {
                Log.e(TAG, "Exception" + e.getMessage());
                
                if (e.getMessage() == "No authentication challenges found")
                {
                    // work around for improper 401 response
                    _response.setResponse(new HttpWebResponse(401));
                }
                else
                {
                    _response.setResponseException(e);
                }
            } finally
            {
                _connection.disconnect();
                _connection = null;
            }
            _response.setProcessing(false);
        }

        return _response;
    }

    private void setRequestBody() throws IOException {
        if (null != _content)
        {
            _connection.setDoOutput(true);

            if (null != _contentType && !_contentType.isEmpty())
            {
                _connection.setRequestProperty("Content-Type", _contentType);
            }

            _connection.setRequestProperty("Content-Length", Integer.toString(_content.length));
            _connection.setFixedLengthStreamingMode(_content.length);

            OutputStream out = _connection.getOutputStream();
            out.write(_content);
            out.close();
        }
    }

    @Override
    protected void onPreExecute()
    {
        _response = new WebResponse();
        Log.e(TAG, "onPreExecute");
        _response.setProcessing(false);
        try
        {
            _connection = (HttpURLConnection) _url.openConnection();
        } catch (IOException e)
        {
            Log.e(TAG, e.getMessage());

            _response.setResponseException(e);

            _connection.disconnect();
            _connection = null;
        }
    }

    @Override
    protected void onPostExecute(WebResponse response)
    {
        Log.d(TAG, "OnPostExecute");
        _response.setProcessing(false);
        if (null != _callback)
        {
            _callback.onComplete(response.getResponseException(), response.getResponse());
        }
    }

    /**
     * Synchronous GET.
     */
    public HttpWebResponse send() throws Exception
    {
        // TODO: Prevent reuse after send
        onPreExecute();
        _requestMethod = REQUEST_METHOD_GET;
        WebResponse response = doInBackground((Void[]) null);

        if (response.getResponseException() != null)
        {
            throw response.getResponseException();
        }

        return response.getResponse();
    }

    /**
     * Synchronous POST.
     */
    HttpWebResponse send(byte[] content, String contentType) throws Exception
    {
        // TODO: Prevent reuse after send
        if (null == content || content.length == 0)
        {
            throw new IllegalArgumentException("content");
        }

        _content = content;
        _contentType = contentType;
        _requestMethod = REQUEST_METHOD_POST;
        onPreExecute();
        WebResponse response = doInBackground((Void[]) null);

        if (response.getResponseException() != null)
        {
            throw response.getResponseException();
        }

        return response.getResponse();
    }

    /**
     * Asynchronous GET.
     */
    public void sendAsync(HttpWebRequestCallback callback) throws IllegalArgumentException
    {
        // TODO: Prevent reuse after send
        if (callback == null)
        {
            throw new IllegalArgumentException("callback");
        }

        _callback = callback;
        _requestMethod = REQUEST_METHOD_GET;
        // At Honeycomb, execute was changed to run on a serialized thread pool
        // but we want the request to run fully parallel so we force use of
        // the THREAD_POOL_EXECUTOR
        executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void[]) null);
    }

    /**
     * Async delete
     * 
     * @param callback
     * @throws IllegalArgumentException
     */
    public void sendAsyncDelete(HttpWebRequestCallback callback) throws IllegalArgumentException
    {
        // TODO: Prevent reuse after send
        if (callback == null)
        {
            throw new IllegalArgumentException("callback");
        }

        _callback = callback;
        _requestMethod = REQUEST_METHOD_GET;

        // At Honeycomb, execute was changed to run on a serialized thread pool
        // but we want the request to run fully parallel so we force use of
        // the THREAD_POOL_EXECUTOR
        executeOnExecutor(THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    public void sendAsyncPut(byte[] content, String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException
    {
        sendAsync(REQUEST_METHOD_PUT, content, contentType, callback);
    }

    public void sendAsyncPost(byte[] content, String contentType, HttpWebRequestCallback callback)
            throws IllegalArgumentException, IOException
    {
        sendAsync(REQUEST_METHOD_POST, content, contentType, callback);
    }

    /**
     * Asynchronous POST/PUT.
     */
    private void sendAsync(String requestmethod, byte[] content, String contentType,
            HttpWebRequestCallback callback) throws IllegalArgumentException, IOException
    {
        // TODO: Prevent reuse after send
        if (null == content || content.length == 0)
        {
            throw new IllegalArgumentException("content");
        }

        if (callback == null)
        {
            throw new IllegalArgumentException("callback");
        }

        _requestMethod = requestmethod;
        _callback = callback;
        _content = content;
        _contentType = contentType;

        // At Honeycomb, execute was changed to run on a serialized thread pool
        // but we want the request to run fully parallel so we force use of
        // the THREAD_POOL_EXECUTOR
        executeOnExecutor(THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    private static String getURLAuthority(URL requestURL)
    {
        // We assume that the parameter has already passed the tests in
        // validateRequestURI
        String authority = requestURL.getAuthority();

        if (requestURL.getPort() == -1)
        {
            // No port in the URI so append a default using the
            // scheme specified in the URI; only http and https are
            // supported
            if (requestURL.getProtocol().equalsIgnoreCase("http"))
            {
                authority = authority + ":80";
            }
            else if (requestURL.getProtocol().equalsIgnoreCase("https"))
            {
                authority = authority + ":443";
            }
        }

        return authority;
    }

    /**
     * The requests target URL
     */
    URL getURL()
    {
        return _url;
    }

    /**
     * The request headers
     */
    public HashMap<String, String> getRequestHeaders()
    {
        return _requestHeaders;
    }
}
