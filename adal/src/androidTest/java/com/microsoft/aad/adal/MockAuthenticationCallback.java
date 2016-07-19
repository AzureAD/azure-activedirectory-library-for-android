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

import java.util.concurrent.CountDownLatch;

/**
 * callback to store responses
 */
class MockAuthenticationCallback implements AuthenticationCallback<AuthenticationResult> {

    private Exception mException = null;

    private AuthenticationResult mResult = null;

    private CountDownLatch mSignal;

    public MockAuthenticationCallback() {
        mSignal = null;
    }

    public MockAuthenticationCallback(final CountDownLatch signal) {
        mSignal = signal;
    }

    @Override
    public void onSuccess(AuthenticationResult result) {
        mResult = result;
        if (mSignal != null) {
            mSignal.countDown();
        }
    }

    @Override
    public void onError(Exception exc) {
        mException = exc;
        if (mSignal != null) {
            mSignal.countDown();
        }
    }

    final void setException(final Exception exception) {
        mException = exception;
    }

    final Exception getException() {
        return mException;
    }

    final void setAuthenticationResult(final AuthenticationResult result) {
        mResult = result;
    }

    final AuthenticationResult getAuthenticationResult() {
        return mResult;
    }

    final void setSignal(final CountDownLatch signal) {
        mSignal = signal;
    }

    final CountDownLatch getSignal() {
        return mSignal;
    }
}