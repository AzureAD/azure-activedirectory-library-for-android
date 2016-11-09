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

import java.net.URL;
import java.util.List;
import java.util.Map;

class HttpEvent extends DefaultEvent {
    HttpEvent(final String eventName) {
        getEventList().add(Pair.create(EventStrings.EVENT_NAME, eventName));
    }

    void setUserAgent(final String userAgent) {
        setEvent(EventStrings.HTTP_USER_AGENT, userAgent);
    }

    void setMethod(final String method) {
        setEvent(EventStrings.HTTP_METHOD, method);
    }

    void setQueryParameters(final String queryParameters) {
        setEvent(EventStrings.HTTP_QUERY_PARAMETERS, queryParameters);
    }

    void setResponseCode(final Integer responseCode) {
        setEvent(EventStrings.HTTP_RESPONSE_CODE, responseCode.toString());
    }

    void setApiVersion(final String apiVersion) {
        setEvent(EventStrings.HTTP_API_VERSION, apiVersion);
    }

    void setHttpPath(final URL httpPath) {
        setEvent(EventStrings.HTTP_PATH, httpPath.toString());
    }

    void setOauthErrorCode(final String errorCode) {
        setEvent(EventStrings.OAUTH_ERROR_CODE, errorCode);
    }

    void setRequestIdHeader(final String requestIdHeader) {
        setEvent(EventStrings.REQUEST_ID_HEADER, requestIdHeader);
    }

    @Override
    public void processEvent(final Map<String, String> dispatchMap) {
        final Object countObject = dispatchMap.get(EventStrings.HTTP_EVENT_COUNT);

        if (countObject == null) {
            dispatchMap.put(EventStrings.HTTP_EVENT_COUNT, "1");
        } else {
            dispatchMap.put(EventStrings.HTTP_EVENT_COUNT,
                    Integer.toString(Integer.parseInt((String) countObject) + 1));
        }

        final List eventList = getEventList();
        final int size = eventList.size();
        for (int i = 0; i < size; i++) {
            final Pair eventPair = (Pair<String, String>) eventList.get(i);
            final String name = (String) eventPair.first;

            if (name.equals(EventStrings.HTTP_RESPONSE_CODE) || name.equals(EventStrings.REQUEST_ID_HEADER)
                    || name.equals(EventStrings.OAUTH_ERROR_CODE) || name.equals(EventStrings.HTTP_PATH)) {
                dispatchMap.put(name, (String) eventPair.second);
            }
        }
    }
}
