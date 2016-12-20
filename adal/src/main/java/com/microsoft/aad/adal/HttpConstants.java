package com.microsoft.aad.adal;

/**
 * Constants related to HTTP.
 */
final class HttpConstants {

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
