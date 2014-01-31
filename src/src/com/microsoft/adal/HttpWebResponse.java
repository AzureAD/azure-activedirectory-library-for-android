
package com.microsoft.adal;

import java.util.List;
import java.util.Map;

/**
 * web response to keep status, response body, headers and related exceptions
 * 
 * @author omercan
 */
public class HttpWebResponse {
    private int mStatusCode;
    private byte[] mResponseBody;
    private Map<String, List<String>> mResponseHeaders;
    private Exception responseException = null;

    public HttpWebResponse() {
        mStatusCode = 200;
        mResponseBody = null;
    }

    public HttpWebResponse(int statusCode, byte[] responseBody,
            Map<String, List<String>> responseHeaders) {
        mStatusCode = statusCode;
        mResponseBody = responseBody;
        mResponseHeaders = responseHeaders;
    }

    public Exception getResponseException() {
        return responseException;
    }

    public void setResponseException(Exception responseException) {
        this.responseException = responseException;
    }

    HttpWebResponse(int statusCode) {
        mStatusCode = statusCode;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public void setStatusCode(int status) {
        mStatusCode = status;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return mResponseHeaders;
    }

    public void setResponseHeaders(Map<String, List<String>> headers) {
        mResponseHeaders = headers;
    }

    public byte[] getBody() {
        return mResponseBody;
    }

    public void setBody(byte[] body) {
        mResponseBody = body;
    }
}
