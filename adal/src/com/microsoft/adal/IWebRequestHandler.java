
package com.microsoft.adal;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

/**
 * Webrequest interface to send web requests
 */
public interface IWebRequestHandler {
    HttpWebResponse sendGet(URL url, HashMap<String, String> headers)
            throws IllegalArgumentException, IOException;

    HttpWebResponse sendPost(URL url, HashMap<String, String> headers, byte[] content,
            String contentType) throws IllegalArgumentException, IOException;

    public void setRequestCorrelationId(UUID mRequestCorrelationId);
}
