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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.io.IOException;

interface IBrokerProxy {
    /**
     * Checks if broker package correct and authenticator valid.
     * 
     * @return True package is available and authenticator is installed at
     *         Account manager
     */
    BrokerProxy.SwitchToBroker canSwitchToBroker(final String authorityUrlStr);
    
    boolean verifyUser(String username, String uniqueid);

    boolean canUseLocalCache(final String authorityUrlStr);

    void removeAccounts();

    void saveAccount(String accountName);

    /**
     * Gets current broker user(Single User model).
     * 
     * @return Current user from AccountManager
     */
    String getCurrentUser();

    /**
     * gets token using authenticator service.
     * 
     * @param request AuthenticationRequest object
     * @return AuthenticationResult
     */
    AuthenticationResult getAuthTokenInBackground(final AuthenticationRequest request)
            throws AuthenticationException;

    /**
     * only gets intent to start from calling app's activity.
     * 
     * @param request AuthenticationRequest
     * @return Intent
     */
    Intent getIntentForBrokerActivity(final AuthenticationRequest request);

    /*
     * Gets user info from broker.
     * @return user {@link UserInfo}
     */
    UserInfo[] getBrokerUsers() throws OperationCanceledException, AuthenticatorException,
            IOException;
    
    /**
     * @return The package name for the active broker. Should be either Azure Authenticator 
     * or Company Portal. 
     */
    String getCurrentActiveBrokerPackageName();

    /**
     * @param brokerAppPackageName Package name for currently active broker.
     * @return The current broker app version.
     */
    String getBrokerAppVersion(final String brokerAppPackageName) throws PackageManager.NameNotFoundException;
}
