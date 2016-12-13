package com.microsoft.aad.adal;

/**
 * Constants related to HTTP.
 */
final class HttpConstants {

    /**
     * HTTP response status codes.
     */
    static final class StatusCode {

        /**
         * 200 OK.
         *
         * @see <a href="https://tools.ietf.org/html/rfc1945#section-9.2">RFC-1945</a>
         */
        static final int SC_OK = 200;

        /**
         * 400 Bad Request.
         *
         * @see <a href="https://tools.ietf.org/html/rfc1945#section-9.4">RFC-1945</a>
         */
        static final int SC_BAD_REQUEST = 400;
    }

    /**
     * HTTP header fields.
     */
    static final class HeaderField {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc1945#appendix-D.2.1">RFC-1945</a>
         */
        static final String ACCEPT = "Accept";
    }

    /**
     * Identifiers for file formats and format contents.
     */
    static final class MediaType {

        /**
         * @see <a href="https://tools.ietf.org/html/rfc7159">RFC-7159</a>
         */
        static final String APPLICATION_JSON = "application/json";
    }

}
