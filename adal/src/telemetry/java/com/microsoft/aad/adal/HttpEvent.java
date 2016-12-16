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

final class HttpEvent extends DefaultEvent {
    HttpEvent(final String eventName) {
        getEventList().add(Pair.create(EventStrings.EVENT_NAME, eventName));
    }

    void setUserAgent(final String userAgent) {
        setProperty(EventStrings.HTTP_USER_AGENT, userAgent);
    }

    void setMethod(final String method) {
        setProperty(EventStrings.HTTP_METHOD, method);
    }

    void setQueryParameters(final String queryParameters) {
        setProperty(EventStrings.HTTP_QUERY_PARAMETERS, queryParameters);
    }

    void setResponseCode(final int responseCode) {
        setProperty(EventStrings.HTTP_RESPONSE_CODE, String.valueOf(responseCode));
    }

    void setApiVersion(final String apiVersion) {
        setProperty(EventStrings.HTTP_API_VERSION, apiVersion);
    }

    void setHttpPath(final URL httpPath) {
        final String authority = httpPath.getAuthority();
        final Discovery discovery = new Discovery();
        if (!discovery.getValidHosts().contains(authority)) {
            return;
        }

        final String[] splitArray = httpPath.getPath().split("/");

        final StringBuilder logPath = new StringBuilder();
        logPath.append(httpPath.getProtocol());
        logPath.append("://");
        logPath.append(authority);
        logPath.append("/");

        // we do not want to send tenant information
        // index 0 is blank
        // index 1 is tenant
        for (int i = 2; i < splitArray.length; i++) {
            logPath.append(splitArray[i]);
            logPath.append("/");
        }
        setProperty(EventStrings.HTTP_PATH, logPath.toString());
    }

    void setOauthErrorCode(final String errorCode) {
        setProperty(EventStrings.OAUTH_ERROR_CODE, errorCode);
    }

    void setRequestIdHeader(final String requestIdHeader) {
        setProperty(EventStrings.REQUEST_ID_HEADER, requestIdHeader);
    }

    /**
     * Each event chooses which of its members get picked on aggregation.
     * Http event adds an event count field
     * @param dispatchMap the Map that is filled with the aggregated event properties
     */
    @Override
    public void processEvent(final Map<String, String> dispatchMap) {
        final Object countObject = dispatchMap.get(EventStrings.HTTP_EVENT_COUNT);

        if (countObject == null) {
            dispatchMap.put(EventStrings.HTTP_EVENT_COUNT, "1");
        } else {
            dispatchMap.put(EventStrings.HTTP_EVENT_COUNT,
                    Integer.toString(Integer.parseInt((String) countObject) + 1));
        }

        // If there was a previous entry clear out its fields.
        if (dispatchMap.containsKey(EventStrings.HTTP_RESPONSE_CODE)) {
            dispatchMap.put(EventStrings.HTTP_RESPONSE_CODE, "");
        }

        if (dispatchMap.containsKey(EventStrings.OAUTH_ERROR_CODE)) {
            dispatchMap.put(EventStrings.OAUTH_ERROR_CODE, "");
        }

        if (dispatchMap.containsKey(EventStrings.HTTP_PATH)) {
            dispatchMap.put(EventStrings.HTTP_PATH, "");
        }

        if (dispatchMap.containsKey(EventStrings.REQUEST_ID_HEADER)) {
            dispatchMap.put(EventStrings.REQUEST_ID_HEADER, "");
        }

        final List<Pair<String, String>> eventList = getEventList();
        for (Pair<String, String> eventPair : eventList) {
            final String name = eventPair.first;

            if (name.equals(EventStrings.HTTP_RESPONSE_CODE) || name.equals(EventStrings.REQUEST_ID_HEADER)
                    || name.equals(EventStrings.OAUTH_ERROR_CODE) || name.equals(EventStrings.HTTP_PATH)) {
                dispatchMap.put(name, eventPair.second);
            }
        }
    }
}
