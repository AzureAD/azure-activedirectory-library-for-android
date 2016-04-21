// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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
