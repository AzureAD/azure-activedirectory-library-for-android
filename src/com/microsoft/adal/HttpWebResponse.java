/*
 * Copyright Microsoft Corporation (c) All Rights Reserved.
 */

package com.microsoft.adal;

import java.util.List;
import java.util.Map;

/**
 * MOVED from Hervey's code
 * A simple class that represents the response from an HttpWebRequeset
 */
class HttpWebResponse
{
    private int                       _statusCode;
    private byte[]                    _responseBody;
    private Map<String, List<String>> _responseHeaders;

    HttpWebResponse( int statusCode, byte[] responseBody, Map<String, List<String>> responseHeaders )
    {
        _statusCode      = statusCode;
        _responseBody    = responseBody;
        _responseHeaders = responseHeaders;
    }

    int getStatusCode()
    {
        return _statusCode;
    }

    Map<String, List<String>> getResponseHeaders()
    {
        return _responseHeaders;
    }

    byte[] getBody()
    {
        return _responseBody;
    }
}
