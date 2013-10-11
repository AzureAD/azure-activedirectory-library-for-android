
package com.microsoft.adal;

/**
 * Messages that are not related to the context All message that can be pulled
 * from static String table should go there
 * 
 * @author omercan
 */
public class ErrorMessages {

    public final static String AUTH_HEADER_INVALID_FORMAT = "Invalid authentication header format";
    public final static String AUTH_HEADER_MISSING = "WWW-Authenticate header was expected in the response";
    public final static String AUTH_HEADER_WRONG_STATUS = "Unauthorized http response (status code 401) was expected";
}
