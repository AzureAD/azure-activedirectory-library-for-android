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
import android.util.Pair;

import java.net.URL;

class APIEvent extends DefaultEvent {

    private final String mEventName;
    APIEvent(final String eventName) {
        getEventList().add(Pair.create(EventStrings.EVENT_NAME, eventName));
        mEventName = eventName;
    }

    APIEvent(final String eventName, final Context context, final String clientId) {
        this(eventName);
        setDefaults(context, clientId);
    }

    void setAuthority(final String authority) {
        if (StringExtensions.isNullOrBlank(authority)) {
            return;
        }

        getEventList().add(new Pair<>(EventStrings.AUTHORITY_NAME, authority));
        final URL authorityUrl = StringExtensions.getUrl(authority);
        if(authorityUrl == null) {
            return;
        }

        if (UrlExtensions.isADFSAuthority(authorityUrl)) {
            setAuthorityType(EventStrings.AUTHORITY_TYPE_ADFS);
        } else {
            setAuthorityType(EventStrings.AUTHORITY_TYPE_AAD);
        }
    }

    void setAuthorityType(final String authorityType) {
        getEventList().add(new Pair<>(EventStrings.AUTHORITY_TYPE, authorityType));
    }

    void setIsDeprecated(final Boolean isDeprecated) {
        getEventList().add(new Pair<>(EventStrings.API_DEPRECATED, isDeprecated.toString()));
    }

    void setValidationStatus(final String validationStatus) {
        getEventList().add(new Pair<>(EventStrings.AUTHORITY_VALIDATION, validationStatus));
    }

    void setExtendedExpiresOnSetting(final Boolean extendedExpiresOnSetting) {
        getEventList().add(new Pair<>(EventStrings.EXTENDED_EXPIRES_ON_SETTING, extendedExpiresOnSetting.toString()));
    }

    void setSilentRequestPromptBehavior(final String promptBehavior) {
        getEventList().add(new Pair<>(EventStrings.PROMPT_BEHAVIOR, promptBehavior));
    }

    void setAPIId(final String id) {
        getEventList().add(new Pair<>(EventStrings.PROMPT_BEHAVIOR, id));
    }

    String getEventName() {
        return mEventName;
    }

    void setSuccessStatus(final Boolean isSuccess) {
        getEventList().add(new Pair<>(EventStrings.WAS_SUCCESSFUL, isSuccess.toString()));
    }
}
