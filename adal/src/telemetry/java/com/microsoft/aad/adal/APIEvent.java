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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * This class tracks flows for certain API calls. Most notably in those all acquireToken* calls
 * All other Events will be called within the timeline of APIEvent
 */
class APIEvent extends DefaultEvent {

    private static final String TAG = DefaultEvent.class.getSimpleName();
    private final String mEventName;

    APIEvent(final String eventName) {
        setEvent(EventStrings.EVENT_NAME, eventName);
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

        setEvent(EventStrings.AUTHORITY_NAME, authority);
        final URL authorityUrl = StringExtensions.getUrl(authority);
        if (authorityUrl == null) {
            return;
        }

        if (UrlExtensions.isADFSAuthority(authorityUrl)) {
            setAuthorityType(EventStrings.AUTHORITY_TYPE_ADFS);
        } else {
            setAuthorityType(EventStrings.AUTHORITY_TYPE_AAD);
        }
    }

    void setAuthorityType(final String authorityType) {
        setEvent(EventStrings.AUTHORITY_TYPE, authorityType);
    }

    void setIsDeprecated(final Boolean isDeprecated) {
        setEvent(EventStrings.API_DEPRECATED, isDeprecated.toString());
    }

    void setValidationStatus(final String validationStatus) {
        setEvent(EventStrings.AUTHORITY_VALIDATION, validationStatus);
    }

    void setExtendedExpiresOnSetting(final Boolean extendedExpiresOnSetting) {
        setEvent(EventStrings.EXTENDED_EXPIRES_ON_SETTING, extendedExpiresOnSetting.toString());
    }

    void setSilentRequestPromptBehavior(final String promptBehavior) {
        setEvent(EventStrings.PROMPT_BEHAVIOR, promptBehavior);
    }

    void setAPIId(final String id) {
        setEvent(EventStrings.API_ID, id);
    }

    String getEventName() {
        return mEventName;
    }

    void setWasApiCallSuccessful(final Boolean isSuccess, final Exception exception) {
        setEvent(EventStrings.WAS_SUCCESSFUL, isSuccess.toString());

        if (exception != null) {
            if (exception instanceof AuthenticationException) {
                final AuthenticationException authException = (AuthenticationException) exception;
                setEvent(EventStrings.API_ERROR_CODE, authException.getCode().toString());
            }
        }
    }

    void stopTelemetryAndFlush() {
        Telemetry.getInstance().stopEvent(getRequestId(), this, getEventName());
        Telemetry.getInstance().flush(getRequestId());
    }

    void setIdToken(final String rawIdToken) {
        if (StringExtensions.isNullOrBlank(rawIdToken)) {
            return;
        }

        final IdToken idToken;
        try {
            idToken = new IdToken(rawIdToken);
        } catch (AuthenticationException ae) {
            return;
        }

        final UserInfo userInfo = new UserInfo(idToken);
        setEvent(EventStrings.IDP_NAME, idToken.getIdentityProvider());

        try {
            setEvent(EventStrings.TENANT_ID, StringExtensions.createHash(idToken.getTenantId()));
            setEvent(EventStrings.USER_ID, StringExtensions.createHash(userInfo.getDisplayableId()));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Logger.i(TAG, "Skipping TENANT_ID and USER_ID", "");
        }
    }

    void setLoginHint(final String loginHint) {
        try {
            setEvent(EventStrings.LOGIN_HINT,  StringExtensions.createHash(loginHint));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Logger.i(TAG, "Skipping telemetry for LOGIN_HINT", "");
        }
    }

    @Override
    public void processEvent(final Map<String, String> dispatchMap) {
        super.processEvent(dispatchMap);
        final List eventList = getEventList();
        final int size = eventList.size();

        for (int i = 0; i < size; i++) {
            final Pair eventPair = (Pair<String, String>) eventList.get(i);
            final String name = (String) eventPair.first;

            // API Event specific parameters, push all except the time values
            if (name.equals(EventStrings.AUTHORITY_TYPE) || name.equals(EventStrings.API_DEPRECATED)
                    || name.equals(EventStrings.AUTHORITY_VALIDATION)
                    || name.equals(EventStrings.EXTENDED_EXPIRES_ON_SETTING)
                    || name.equals(EventStrings.PROMPT_BEHAVIOR) || name.equals(EventStrings.WAS_SUCCESSFUL)
                    || name.equals(EventStrings.IDP_NAME) || name.equals(EventStrings.TENANT_ID)
                    || name.equals(EventStrings.USER_ID) || name.equals(EventStrings.LOGIN_HINT)
                    || name.equals(EventStrings.RESPONSE_TIME) || name.equals(EventStrings.CORRELATION_ID)
                    || name.equals(EventStrings.REQUEST_ID) || name.equals(EventStrings.API_ID)
                    || name.equals(EventStrings.API_ERROR_CODE)) {
                dispatchMap.put(name, (String) eventPair.second);
            }
        }

    }
}
