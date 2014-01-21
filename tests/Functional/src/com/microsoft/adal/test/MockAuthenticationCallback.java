package com.microsoft.adal.test;

import java.util.concurrent.CountDownLatch;

import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationResult;

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