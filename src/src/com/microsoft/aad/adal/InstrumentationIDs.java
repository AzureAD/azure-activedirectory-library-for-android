package com.microsoft.aad.adal;

/**
 * Index of instrumentation event IDs used for logging
 */
public final class InstrumentationIDs {
    public static final String REFRESH_TOKEN_REQUEST_FAILED = "RefreshTokenRequestFailed";
    public static final String AUTH_TOKEN_NOT_RETURNED = "AuthTokenNotReturned";
    public static final String REFRESH_TOKEN_REQUEST_SUCCEEDED = "RefreshTokenRequestSucceeded";

    public static final String ERROR_CLASS = "ErrorClass";
    public static final String ERROR_CODE = "ErrorCode";
    public static final String ERROR_MESSAGE = "ErrorMessage";
    public static final String USER_ID = "UserId";
    public static final String RESOURCE_ID = "ResourceName";
    public static final String CORRELATION_ID = "CorrelationId";
    public static final String AUTHORITY_ID = "Authority";
}
