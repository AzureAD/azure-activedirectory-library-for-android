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

import java.util.HashMap;
import java.util.Map;

/**
 * Builds properties for ClientAnalytics.
 */
class InstrumentationPropertiesBuilder {
    final private Map<String, String> mProperties = new HashMap<>();

    /**
     * Initializes properties for request that had exception during execution
     * @param request
     * @param exc
     */
    InstrumentationPropertiesBuilder(AuthenticationRequest request, Exception exc) {
        addPropertiesForRequest(request);
        final Throwable cause = exc.getCause() != null ? exc.getCause() : exc;
        final Class causeClass = cause.getClass();
        final ADALError errorCode = exc instanceof AuthenticationException ? ((AuthenticationException) exc).getCode() : null;
        addProperty(InstrumentationIDs.ERROR_CLASS, causeClass.getSimpleName());
        addProperty(InstrumentationIDs.ERROR_MESSAGE, cause.getMessage());
        addProperty(InstrumentationIDs.ERROR_CODE, errorCode.getDescription());
    }

    /**
     * Initializes properties for request that got results at the end of execution
     * @param request
     * @param result
     */
    InstrumentationPropertiesBuilder(AuthenticationRequest request, AuthenticationResult result) {
        addPropertiesForRequest(request);
        if (result != null) {
            addProperty(InstrumentationIDs.ERROR_MESSAGE, result.getErrorDescription());
            addProperty(InstrumentationIDs.ERROR_CODE, result.getErrorCode());
        }
    }

    /**
     * Adds property
     * @param propertyName
     * @param propertyValue
     * @return
     */
    InstrumentationPropertiesBuilder add(String propertyName, String propertyValue) {
        addProperty(propertyName, propertyValue);
        return this;
    }

    /**
     * Returns map of properties for event
     * @return
     */
    Map<String, String> build() {
        return mProperties;
    }

    /**
     * Adds property if the value isn't null
     * @param propertyName
     * @param propertyValue
     */
    private void addProperty(String propertyName, String propertyValue) {
        if (propertyValue != null) {
            mProperties.put(propertyName, propertyValue);
        }
    }

    /**
     * Add basic properties for request
     * @param request
     */
    private void addPropertiesForRequest(AuthenticationRequest request) {
        addProperty(InstrumentationIDs.USER_ID, request.getUserId());
        addProperty(InstrumentationIDs.RESOURCE_ID, request.getResource());
        addProperty(InstrumentationIDs.AUTHORITY_ID, request.getAuthority());
        addProperty(InstrumentationIDs.CORRELATION_ID, request.getCorrelationId().toString());
    }
}
