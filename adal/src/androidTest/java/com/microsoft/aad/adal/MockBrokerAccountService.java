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

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import org.mockito.Mockito;

import java.util.Map;

/**
 * Mocked broker account service for testing purpose.
 */
public class MockBrokerAccountService extends Service {
    public static final String DISPLAYABLE = "some displayable";
    public static final String UNIQUE_ID = "some uniqueid";
    public static final String FAMILY_NAME = "family name";
    public static final String GIVEN_NAME = "given name";
    public static final String IDENTITY_PROVIDER = "some idp";
    public static final String ACCESS_TOKEN = "some access token";

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;

    }

    private final IBrokerAccountService.Stub mBinder = new IBrokerAccountService.Stub() {

        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public synchronized Bundle getBrokerUsers() throws RemoteException {
            final Bundle bundle = new Bundle();
            bundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, UNIQUE_ID);
            bundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE, DISPLAYABLE);
            bundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME, FAMILY_NAME);
            bundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME, GIVEN_NAME);
            bundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER, IDENTITY_PROVIDER);

            final Bundle resultBundle = new Bundle();
            resultBundle.putBundle("test.name", bundle);

            return resultBundle;
        }

        @Override
        public synchronized Bundle acquireTokenSilently(Map requestParameters) throws RemoteException {
            final Bundle bundle = new Bundle();
            if(requestParameters.containsKey("isConnectionAvaliable")) {
                bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR);
                bundle.putString(AccountManager.KEY_ERROR_MESSAGE, ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE.getDescription());
            } else {
                bundle.putString(AccountManager.KEY_AUTHTOKEN, ACCESS_TOKEN);
            }

            return bundle;
        }

        @Override
        public Intent getIntentForInteractiveRequest() throws RemoteException {
            return Mockito.mock(Intent.class);
        }

        @Override
        public void removeAccounts() throws RemoteException {
            return;
        }
    };
}
