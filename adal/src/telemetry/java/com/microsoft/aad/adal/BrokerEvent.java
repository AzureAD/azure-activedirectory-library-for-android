//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.adal;

import android.util.Pair;

import java.util.List;
import java.util.Map;

/**
 * Event class for tracking broker related telemetry events.
 */

final class BrokerEvent extends DefaultEvent {
    BrokerEvent(final String eventName) {
        setProperty(EventStrings.EVENT_NAME, eventName);
    }

    void setBrokerAppName(final String brokerAppName) {
        setProperty(EventStrings.BROKER_APP, brokerAppName);
    }

    void setBrokerAppVersion(final String brokerAppVersion) {
        setProperty(EventStrings.BROKER_VERSION, brokerAppVersion);
    }

    void setBrokerAccountServerStartsBinding() {
        setProperty(EventStrings.BROKER_ACCOUNT_SERVICE_STARTS_BINDING, Boolean.toString(true));
    }

    void setBrokerAccountServiceBindingSucceed(final boolean succeeded) {
        setProperty(EventStrings.BROKER_ACCOUNT_SERVICE_BINDING_SUCCEED, Boolean.toString(succeeded));
    }

    void setBrokerAccountServiceConnected() {
        setProperty(EventStrings.BROKER_ACCOUNT_SERVICE_CONNECTED, Boolean.toString(true));
    }

    void setServerErrorCode(final String errorCode) {
        if (!StringExtensions.isNullOrBlank(errorCode) && !errorCode.equals("0")) {
            setProperty(EventStrings.SERVER_ERROR_CODE, errorCode.trim());
        }
    }

    void setServerSubErrorCode(final String subErrorCode) {
        if (!StringExtensions.isNullOrBlank(subErrorCode) && !subErrorCode.equals("0")) {
            setProperty(EventStrings.SERVER_SUBERROR_CODE, subErrorCode.trim());

        }
    }

    void setRefreshTokenAge(final String tokenAge) {
        if (!StringExtensions.isNullOrBlank(tokenAge)) {
            setProperty(EventStrings.TOKEN_AGE, tokenAge.trim());

        }
    }

    void setSpeRing(final String speRing) {
        if (!StringExtensions.isNullOrBlank(speRing)) {
            setProperty(EventStrings.SPE_INFO, speRing.trim());
        }
    }

    @Override
    public void processEvent(final Map<String, String> dispatchMap) {
        final List<Pair<String, String>> eventList = getEventList();

        dispatchMap.put(EventStrings.BROKER_APP_USED, Boolean.toString(true));
        for (Pair<String, String> eventPair : eventList) {
            final String name = eventPair.first;
            if (!name.equals(EventStrings.EVENT_NAME)) {
                dispatchMap.put(eventPair.first, eventPair.second);
            }
        }
    }
}
