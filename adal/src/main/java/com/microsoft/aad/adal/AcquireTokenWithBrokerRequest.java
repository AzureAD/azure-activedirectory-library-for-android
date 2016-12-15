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
     * see {@link BrokerProxy#getAuthTokenInBackground(AuthenticationRequest)} for details.
     */
    AuthenticationResult acquireTokenWithBrokerSilent()
            throws AuthenticationException {

        mAuthRequest.setVersion(AuthenticationContext.getVersionName());
        mAuthRequest.setBrokerAccountName(mAuthRequest.getLoginHint());

        // Log the broker version for silent request to broker
        logBrokerVersion();

        final AuthenticationResult authenticationResult;
        if (!StringExtensions.isNullOrBlank(mAuthRequest.getBrokerAccountName()) || !StringExtensions
                .isNullOrBlank(mAuthRequest.getUserId())) {
            Logger.v(TAG, "User is specified for background(silent) token request, trying to acquire token silently.");
            authenticationResult = mBrokerProxy.getAuthTokenInBackground(mAuthRequest);
        } else {
            Logger.v(TAG, "User is not specified, skipping background(silent) token request");
            authenticationResult = null;
        }

        return authenticationResult;
    }

    /**
     * Acquire token interactively, will prompt user if possible.
     * See {@link BrokerProxy#getIntentForBrokerActivity(AuthenticationRequest)} for details.
     */
    void acquireTokenWithBrokerInteractively(final IWindowComponent activity)
            throws AuthenticationException {
        Logger.v(TAG, "Launch activity for interactive authentication via broker.");
        // Log the broker version for interactive request to broker
        logBrokerVersion();

        final Intent brokerIntent = mBrokerProxy.getIntentForBrokerActivity(mAuthRequest);

        if (activity == null) {
            throw new AuthenticationException(ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED);
        }

        if (brokerIntent == null) {
            throw new AuthenticationException(ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED);
        }

        Logger.v(TAG, "Calling activity pid:" + android.os.Process.myPid()
                + " tid:" + android.os.Process.myTid() + "uid:"
                + android.os.Process.myUid());
        activity.startActivityForResult(brokerIntent, AuthenticationConstants.UIRequest.BROWSER_FLOW);

        //It will start activity if callback is provided.
        //activity onActivityResult will receive the result, and result will be sent back via callback.
    }

    private void logBrokerVersion() {
        final String currentActiveBrokerPackageName =
                mBrokerProxy.getCurrentActiveBrokerPackageName();
        if (StringExtensions.isNullOrBlank(currentActiveBrokerPackageName)) {
            Logger.i(TAG, "Broker app package name is empty.", "");
            return;
        }

        String brokerAppVersion;
        try {
            brokerAppVersion = mBrokerProxy.getBrokerAppVersion(currentActiveBrokerPackageName);
        } catch (final PackageManager.NameNotFoundException e) {
            // we don't want to throw for the logging purpose.
            brokerAppVersion = "N/A";
        }

        final String brokerLogging = "Broker app is: " + currentActiveBrokerPackageName
                + ";Broker app version: " + brokerAppVersion;
        Logger.i(TAG, brokerLogging, "");
    }
}
