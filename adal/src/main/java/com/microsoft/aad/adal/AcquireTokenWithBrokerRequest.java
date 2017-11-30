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

import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Internal class handling the logic for acquire token with Broker app(Either Company Portal or Azure Authenticator).
 * Including the logic for silent flow and interactive flow.
 */
final class AcquireTokenWithBrokerRequest {
    private static final String TAG = AcquireTokenWithBrokerRequest.class.getSimpleName();

    private final AuthenticationRequest mAuthRequest;
    private final IBrokerProxy mBrokerProxy;

    /**
     * Constructor for {@link AcquireTokenWithBrokerRequest}.
     */
    AcquireTokenWithBrokerRequest(final AuthenticationRequest authRequest,
                                  final IBrokerProxy brokerProxy) {
        mAuthRequest = authRequest;
        mBrokerProxy = brokerProxy;
    }

    /**
     * Acquire token silently via broker. This is via account manager API call.
     * see {@link BrokerProxy#getAuthTokenInBackground(AuthenticationRequest, BrokerEvent)} for details.
     */
    AuthenticationResult acquireTokenWithBrokerSilent()
            throws AuthenticationException {
        final String methodName = ":acquireTokenWithBrokerSilent";
        mAuthRequest.setVersion(AuthenticationContext.getVersionName());
        mAuthRequest.setBrokerAccountName(mAuthRequest.getLoginHint());

        // Log the broker version for silent request to broker
        final BrokerEvent brokerEvent = startBrokerTelemetryRequest(EventStrings.BROKER_REQUEST_SILENT);
        logBrokerVersion(brokerEvent);

        final AuthenticationResult authenticationResult;
        if (!StringExtensions.isNullOrBlank(mAuthRequest.getBrokerAccountName()) || !StringExtensions
                .isNullOrBlank(mAuthRequest.getUserId())) {
            Logger.v(TAG + methodName, "User is specified for background(silent) token request, trying to acquire token silently.");
            authenticationResult = mBrokerProxy.getAuthTokenInBackground(mAuthRequest, brokerEvent);
            if (null != authenticationResult && null != authenticationResult.getCliTelemInfo()) {
                brokerEvent.setSpeRing(authenticationResult.getCliTelemInfo().getSpeRing());
            }
        } else {
            Logger.v(TAG + methodName, "User is not specified, skipping background(silent) token request.");
            authenticationResult = null;
        }

        Telemetry.getInstance().stopEvent(brokerEvent.getTelemetryRequestId(), brokerEvent, EventStrings.BROKER_REQUEST_SILENT);
        return authenticationResult;
    }

    /**
     * Acquire token interactively, will prompt user if possible.
     * See {@link BrokerProxy#getIntentForBrokerActivity(AuthenticationRequest, BrokerEvent)} for details.
     */
    void acquireTokenWithBrokerInteractively(final IWindowComponent activity)
            throws AuthenticationException {
        final String methodName = ":acquireTokenWithBrokerInteractively";
        Logger.v(TAG + methodName, "Launch activity for interactive authentication via broker.");
        // Log the broker version for interactive request to broker
        final BrokerEvent brokerEvent = startBrokerTelemetryRequest(EventStrings.BROKER_REQUEST_INTERACTIVE);
        logBrokerVersion(brokerEvent);

        final Intent brokerIntent = mBrokerProxy.getIntentForBrokerActivity(mAuthRequest, brokerEvent);

        if (activity == null) {
            throw new AuthenticationException(ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED);
        }

        if (brokerIntent == null) {
            throw new AuthenticationException(ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED);
        }

        Logger.v(TAG + methodName, "Calling activity. " + "Pid:" + android.os.Process.myPid()
                + " tid:" + android.os.Process.myTid() + "uid:"
                + android.os.Process.myUid());
        Telemetry.getInstance().stopEvent(brokerEvent.getTelemetryRequestId(), brokerEvent, EventStrings.BROKER_REQUEST_INTERACTIVE);
        activity.startActivityForResult(brokerIntent, AuthenticationConstants.UIRequest.BROWSER_FLOW);

        //It will start activity if callback is provided.
        //activity onActivityResult will receive the result, and result will be sent back via callback.
    }

    private void logBrokerVersion(final BrokerEvent brokerEvent) {
        final String methodName = ":logBrokerVersion";
        final String currentActiveBrokerPackageName =
                mBrokerProxy.getCurrentActiveBrokerPackageName();
        if (StringExtensions.isNullOrBlank(currentActiveBrokerPackageName)) {
            Logger.i(TAG + methodName, "Broker app package name is empty.", "");
            return;
        }
        brokerEvent.setBrokerAppName(currentActiveBrokerPackageName);

        String brokerAppVersion;
        try {
            brokerAppVersion = mBrokerProxy.getBrokerAppVersion(currentActiveBrokerPackageName);
        } catch (final PackageManager.NameNotFoundException e) {
            // we don't want to throw for the logging purpose.
            brokerAppVersion = "N/A";
        }
        brokerEvent.setBrokerAppVersion(brokerAppVersion);

        final String brokerLogging = "Broker app is: " + currentActiveBrokerPackageName
                + ";Broker app version: " + brokerAppVersion;
        Logger.i(TAG + methodName, brokerLogging, "");
    }

    private BrokerEvent startBrokerTelemetryRequest(final String brokerEventName) {
        final BrokerEvent brokerEvent = new BrokerEvent(brokerEventName);
        brokerEvent.setRequestId(mAuthRequest.getTelemetryRequestId());
        Telemetry.getInstance().startEvent(mAuthRequest.getTelemetryRequestId(), brokerEventName);

        return brokerEvent;
    }
}
