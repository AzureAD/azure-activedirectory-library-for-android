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

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Default connection service check network connectivity. 
 * TODO: No need for {@link IConnectionService}. Interface was created for testing purpose. 
 * Same purpose could be achieved via mocking the context. Since it's a public interface, should
 * be removed in the next major version update. 
 * https://github.com/AzureAD/azure-activedirectory-library-for-android/issues/626
 */
class DefaultConnectionService implements IConnectionService {

    private final Context mConnectionContext;

    private static final String TAG = "DefaultConnectionService";

    DefaultConnectionService(Context ctx) {
        mConnectionContext = ctx;
    }

    public boolean isConnectionAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mConnectionContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && !isNetworkDisabledFromOptimizations();
    }

    /**
     * Determines if the client app cannot access the network due to power saving optimizations introduced in API 23.
     *
     * @return true if the device is API23 and one or both of the following is true: the device is in doze or the company
     * portal is in standby, false otherwise.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean isNetworkDisabledFromOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final UsageStatsManagerWrapper usageStatsManagerWrapper = UsageStatsManagerWrapper.getInstance();
            if (usageStatsManagerWrapper.isAppInactive(mConnectionContext)) {
                Logger.w(TAG, "Client app is inactive. Network is disabled.", "", ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION);
                return true;
            }

            final PowerManagerWrapper powerManagerWrapper = PowerManagerWrapper.getInstance();
            if (powerManagerWrapper.isDeviceIdleMode(mConnectionContext) && !powerManagerWrapper.isIgnoringBatteryOptimizations(mConnectionContext)) {
                Logger.w(TAG, "Device is dozing. Network is disabled.", "", ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION);
                return true;
            }
        }

        return false;
    }
}
