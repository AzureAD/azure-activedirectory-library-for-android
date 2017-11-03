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

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicReference;

/**
 * ADAL internal class for handling callback to be executed on the correct thread and message queue.
 */
final class CallbackExecutor<T> {
    private static final String TAG = CallbackExecutor.class.getSimpleName();

    private final AtomicReference<Callback<T>> mCallbackReference = new AtomicReference<>(null);
    private final Handler mHandler;

    CallbackExecutor(final Callback<T> callback) {
        // check if the current thread has the looper; if so, create Handler with the current thread looper to send message
        // back to the correct thread.
        mHandler = Looper.myLooper() == null ? null : new Handler();
        mCallbackReference.set(callback);
    }

    public void onSuccess(final T result) {
        final Callback<T> callback = mCallbackReference.getAndSet(null);
        if (callback == null) {
            Logger.v(TAG, "Callback does not exist.");
            return;
        }

        if (mHandler == null) {
            callback.onSuccess(result);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(result);
                }
            });
        }
    }

    public void onError(final Throwable throwable) {
        final Callback<T> callback = mCallbackReference.getAndSet(null);
        if (callback == null) {
            Logger.v(TAG, "Callback does not exist.");
            return;
        }

        if (mHandler == null) {
            callback.onError(throwable);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(throwable);
                }
            });
        }
    }
}
