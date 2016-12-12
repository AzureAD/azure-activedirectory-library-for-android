package com.microsoft.aad.adal;

/**
 * Constants related to HTTP
 */
public final class HttpConstants {

    /**
     * HTTP response status codes
     */
    public static final class StatusCode {
        public static final int SC_OK = 200;
        public static final int SC_BAD_REQUEST = 400;
    }

    /**
     * HTTP header fields
     */
    public static final class HeaderField {
        public static final String ACCEPT = "Accept";
    }

    /**
     * Identifiers for file formats and format contents
     */
    public static final class MediaType {
        public static final String APPLICATION_JSON = "application/json";
    }

}
