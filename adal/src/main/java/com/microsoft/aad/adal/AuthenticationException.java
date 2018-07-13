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

import android.content.Context;

import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ADAL exception.
 */
public class AuthenticationException extends Exception {
    static final long serialVersionUID = 1;

    private ADALError mCode;

    private HashMap<String, String> mHttpResponseBody = null;

    private int mServiceStatusCode = -1;

    private HashMap<String, List<String>> mHttpResponseHeaders = null;

    /**
     * Default constructor for {@link AuthenticationException}.
     */
    public AuthenticationException() {
        // Default constructor, intentionally empty.
    }

    /**
     * Constructs a new AuthenticationException with error code.
     * 
     * @param code {@link ADALError}
     */
    public AuthenticationException(ADALError code) {
        mCode = code;
    }

    /**
     * @param code Resource file related error code. Message will be derived
     *            from resource with using app context
     * @param details Details related to the error such as query string, request
     *            info
     */
    public AuthenticationException(ADALError code, String details) {
        super(details);
        mCode = code;
    }

    /**
     * @param code Resource file related error code. Message will be derived
     *            from resource with using app context
     * @param details Details related to the error such as query string, request
     *            info
     * @param throwable {@link Throwable}
     */
    public AuthenticationException(ADALError code, String details, Throwable throwable) {
        super(details, throwable);
        mCode = code;

        if (null == throwable) {
            return;
        }

        if (throwable instanceof AuthenticationException) {
            mServiceStatusCode = ((AuthenticationException) throwable).getServiceStatusCode();

            if (null != ((AuthenticationException) throwable).getHttpResponseBody()) {
                mHttpResponseBody = new HashMap<>(((AuthenticationException) throwable).getHttpResponseBody());
            }

            if (null != ((AuthenticationException) throwable).getHttpResponseHeaders()) {
                mHttpResponseHeaders = new HashMap<>(((AuthenticationException) throwable).getHttpResponseHeaders());
            }
        }
    }

    /**
     * @param code Resource file related error code. Message will be derived
     *            from resource using app context
     * @param details Details related to the error such as query string, request info.
     * @param response HTTP web response
     */
    public AuthenticationException(ADALError code, String details, HttpWebResponse response) {
        super(details);
        mCode = code;
        setHttpResponse(response);
    }

    /**
     * @param code Resource file related error code. Message will be derived
     *            from resource using app context
     * @param details Details related to the error such as query string, request info.
     * @param response HTTP web response
     * @param throwable {@link Throwable}
     */
    public AuthenticationException(ADALError code, String details, HttpWebResponse response,
                                   Throwable throwable) {
        this(code, details, throwable);
        setHttpResponse(response);
    }

    /**
     * Gets {@link ADALError} code.
     * 
     * @return {@link ADALError} code
     */
    public ADALError getCode() {
        return mCode;
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage(null);
    }

    /**
     * Gets the status code returned from http layer.
     *
     * @return status code from http layer. Return -1 if status code is not initialized.
     */
    public int getServiceStatusCode() {
        return mServiceStatusCode;
    }

    /**
     * Gets the response body that may be returned by the service.
     *
     * @return response body map, null if not initialized.
     */
    public HashMap<String, String> getHttpResponseBody() {
        return mHttpResponseBody;
    }

    /**
     * Get the response headers that indicated an error.
     *
     * @return The response headers for the network call, null if not initialized.
     */
    public HashMap<String, List<String>> getHttpResponseHeaders() {
        return mHttpResponseHeaders;
    }

    void setHttpResponseBody(HashMap<String, String> body) {
        mHttpResponseBody = body;
    }

    void setHttpResponseHeaders(HashMap<String, List<String>> headers) {
        mHttpResponseHeaders = headers;
    }

    void setServiceStatusCode(int statusCode) {
        mServiceStatusCode = statusCode;
    }

    void setHttpResponse(HttpWebResponse response) {
        if (null != response) {
            mServiceStatusCode = response.getStatusCode();

            if (null != response.getResponseHeaders()) {
                mHttpResponseHeaders = new HashMap<>(response.getResponseHeaders());
            }

            if (null != response.getBody()) {
                try {
                    mHttpResponseBody = new HashMap<>(HashMapExtensions.getJsonResponse(response));
                } catch (final JSONException exception) {
                    Logger.e(AuthenticationException.class.getSimpleName(), "Json exception",
                            ExceptionExtensions.getExceptionMessage(exception),
                            ADALError.SERVER_INVALID_JSON_RESPONSE);
                }
            }
        }
    }

    void setHttpResponse(final AuthenticationResult result) {
        if (null != result) {
            mHttpResponseBody = result.getHttpResponseBody();
            mHttpResponseHeaders = result.getHttpResponseHeaders();
            mServiceStatusCode = result.getServiceStatusCode();
        }
    }

    /**
     * Gets localized {@link ADALError} code if provided by context.
     * 
     * @param context {@link Context}
     * @return Error message
     */
    public String getLocalizedMessage(Context context) {
        if (!StringExtensions.isNullOrBlank(super.getMessage())) {
            return super.getMessage();
        }
        if (mCode != null) {
            return mCode.getLocalizedDescription(context);
        }

        return null;
    }
}
