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

import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Builds properties for ClientAnalytics.
 */
class InstrumentationEventBuilder {
    final private List<Pair<String, String>> mProperties = new LinkedList<>();

    InstrumentationEventBuilder(AuthenticationRequest request, Exception exc) {
        addPropertiesForRequest(request);
        final Throwable cause = exc.getCause() != null ? exc.getCause() : exc;
        final Class causeClass = cause.getClass();
        final ADALError errorCode = exc instanceof AuthenticationException ? ((AuthenticationException) exc).getCode() : null;
        addProperty(new Pair<>(InstrumentationIDs.ERROR_CLASS, causeClass.getSimpleName()));
        addProperty(new Pair<>(InstrumentationIDs.ERROR_MESSAGE, cause.getMessage()));
        addProperty(new Pair<>(InstrumentationIDs.ERROR_CODE, errorCode.getDescription()));
    }

    InstrumentationEventBuilder(AuthenticationRequest request, AuthenticationResult result) {
        addPropertiesForRequest(request);
        addProperty(new Pair<>(InstrumentationIDs.ERROR_MESSAGE, result.getErrorDescription()));
        addProperty(new Pair<>(InstrumentationIDs.ERROR_CODE, result.getErrorCode()));
    }

    InstrumentationEventBuilder add(Pair<String, String> property) {
        addProperty(property);
        return this;
    }

    private void addProperty(Pair<String, String> property) {
        if (property != null && property.second != null) {
            mProperties.add(property);
        }
    }

    List<Pair<String, String>> build() {
        return mProperties;
    }

    private void addPropertiesForRequest(AuthenticationRequest request) {
        addProperty(new Pair<>(InstrumentationIDs.USER_ID, request.getUserId()));
        addProperty(new Pair<>(InstrumentationIDs.RESOURCE_ID, request.getResource()));
        addProperty(new Pair<>(InstrumentationIDs.AUTHORITY_ID, request.getAuthority()));
        addProperty(new Pair<>(InstrumentationIDs.CORRELATION_ID, request.getCorrelationId().toString()));
    }
}
