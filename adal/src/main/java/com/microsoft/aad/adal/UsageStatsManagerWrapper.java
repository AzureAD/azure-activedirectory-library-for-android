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
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;

/**
 * Wrapper class for UsageStatsManager.
 */

public class UsageStatsManagerWrapper {

    private static UsageStatsManagerWrapper sInstance;

    static synchronized void setInstance(final UsageStatsManagerWrapper instance) {
        sInstance = instance;
    }

    /**
     * Singleton implementation for UsageStatsManagerWrapper.
     * @return UsageStatsManagerWrapper singleton instance
     */
    public static synchronized UsageStatsManagerWrapper getInstance() {
        if (sInstance == null) {
            sInstance = new UsageStatsManagerWrapper();
        }
        return sInstance;
    }

    /**
     * Wrap the final class function UsageStatsManager.isAppInactive(). And make the code testable.
     * @param connectionContext Context used to query app active state
     * @return true if the app is inactive
     */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean isAppInactive(final Context connectionContext) {
        return ((UsageStatsManager) connectionContext.getSystemService(Context.USAGE_STATS_SERVICE)).isAppInactive(connectionContext.getPackageName());
    }
}
