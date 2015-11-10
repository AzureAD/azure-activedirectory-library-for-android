// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import java.util.concurrent.CountDownLatch;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationResult;

/**
 * callback to store responses
 */
class MockAuthenticationCallback implements AuthenticationCallback<AuthenticationResult> {

    Exception mException = null;

    AuthenticationResult mResult = null;

    CountDownLatch mSignal;

    public MockAuthenticationCallback() {
        mSignal = null;
    }

    public MockAuthenticationCallback(final CountDownLatch signal) {
        mSignal = signal;
    }

    @Override
    public void onSuccess(AuthenticationResult result) {
        mResult = result;
        if (mSignal != null)
            mSignal.countDown();
    }

    @Override
    public void onError(Exception exc) {
        mException = exc;
        if (mSignal != null)
            mSignal.countDown();
    }
}