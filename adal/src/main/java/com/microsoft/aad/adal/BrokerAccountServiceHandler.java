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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Internal class to handle the interaction with BrokerAccountService declared in the broker itelf.
 */
final class BrokerAccountServiceHandler {
    private static final String TAG = BrokerAccountServiceHandler.class.getSimpleName();
    private static final String BROKER_ACCOUNT_SERVICE_INTENT_FILTER = "com.microsoft.workaccount.BrokerAccount";

    private ConcurrentMap<BrokerAccountServiceConnection, CallbackExecutor<BrokerAccountServiceConnection>> mPendingConnections = new ConcurrentHashMap<>();
    private static ExecutorService sThreadExecutor = Executors.newCachedThreadPool();

    private static final class InstanceHolder {
        static final BrokerAccountServiceHandler INSTANCE = new BrokerAccountServiceHandler();
    }

    public static BrokerAccountServiceHandler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Private constructor to prevent class from being instantiate.
     */
    private BrokerAccountServiceHandler() { }

    /**
     * Get Broker users is a blocking call, cannot be executed on the main thread.
     * @return An array of {@link UserInfo}s in the broker. If no user exists in the broker, empty array will be returned.
     */
    UserInfo[] getBrokerUsers(final Context context) throws IOException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Bundle> userBundle = new AtomicReference<>(null);
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);

        performAsyncCallOnBound(context, new Callback<BrokerAccountServiceConnection>() {
            @Override
            public void onSuccess(BrokerAccountServiceConnection connection) {
                final IBrokerAccountService brokerAccountService = connection.getBrokerAccountServiceProvider();
                try {
                    userBundle.set(brokerAccountService.getBrokerUsers());
                } catch (final RemoteException ex) {
                    exception.set(ex);
                }

                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                exception.set(throwable);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (final InterruptedException e) {
            exception.set(e);
        }

        final Throwable exceptionForRetrievingBrokerUsers = exception.getAndSet(null);
        if (exceptionForRetrievingBrokerUsers != null) {
            throw new IOException(exceptionForRetrievingBrokerUsers.getMessage(), exceptionForRetrievingBrokerUsers);
        }

        final Bundle userBundleResult = userBundle.getAndSet(null);
        return convertUserInfoBundleToArray(userBundleResult);
    }

    /**
     * Silently acquire the token from BrokerAccountService.
     * @param context The application {@link Context}.
     * @param requestBundle The request data for the silent request.
     * @return The {@link Bundle} result from the BrokerAccountService.
     * @throws {@link AuthenticationException} if failed to get token from the service.
     */
    public Bundle getAuthToken(final Context context, final Bundle requestBundle) throws AuthenticationException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Bundle> bundleResult = new AtomicReference<>(null);
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);

        performAsyncCallOnBound(context, new Callback<BrokerAccountServiceConnection>() {
            @Override
            public void onSuccess(BrokerAccountServiceConnection result) {
                final IBrokerAccountService brokerAccountService = result.getBrokerAccountServiceProvider();
                try {
                    bundleResult.set(brokerAccountService.acquireTokenSilently(prepareGetAuthTokenRequestData(context, requestBundle)));
                } catch (final RemoteException remoteException) {
                    exception.set(remoteException);
                }

                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                exception.set(throwable);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (final InterruptedException e) {
            exception.set(e);
        }

        final Throwable throwable = exception.getAndSet(null);
        if (throwable != null) {
            throw new AuthenticationException(ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED, throwable.getMessage(), throwable);
        }

        return bundleResult.getAndSet(null);
    }

    /**
     * Get the intent for launching the interactive request with broker.
     * @param context The application {@link Context}.
     * @return The {@link Intent} to launch the interactive request.
     */
    public Intent getIntentForInteractiveRequest(final Context context) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Intent> bundleResult = new AtomicReference<>(null);
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);

        performAsyncCallOnBound(context, new Callback<BrokerAccountServiceConnection>() {
            @Override
            public void onSuccess(BrokerAccountServiceConnection result) {
                final IBrokerAccountService brokerAccountService = result.getBrokerAccountServiceProvider();
                try {
                    bundleResult.set(brokerAccountService.getIntentForInteractiveRequest());
                } catch (final RemoteException remoteException) {
                    exception.set(remoteException);
                }

                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                exception.set(throwable);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (final InterruptedException e) {
            exception.set(e);
        }

        final Throwable throwable = exception.getAndSet(null);
        if (throwable != null) {
            Logger.e(TAG, "Didn't receive the activity to launch from broker: " + throwable.getMessage(), "", null, throwable);
        }

        return bundleResult.getAndSet(null);
    }

    /**
     * Removing all the accounts from broker.
     * @param context The application {@link Context}.
     */
    public void removeAccounts(final Context context) {
        performAsyncCallOnBound(context, new Callback<BrokerAccountServiceConnection>() {
            @Override
            public void onSuccess(BrokerAccountServiceConnection result) {
                try {
                    result.getBrokerAccountServiceProvider().removeAccounts();
                } catch (final RemoteException remoteException) {
                    Logger.e(TAG, "Encounter exception when removing accounts from broker",
                            remoteException.getMessage(), null, remoteException);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                Logger.e(TAG, "Encounter exception when removing accounts from broker",
                        throwable.getMessage(), null, throwable);
            }
        });
    }

    public static Intent getIntentForBrokerAccountService(final Context context) {
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final String brokerAppName = brokerProxy.getCurrentActiveBrokerPackageName();
        if (brokerAppName == null) {
            Logger.v(TAG, "No recognized broker is installed on the device.");
            return null;
        }

        final Intent brokerAccountServiceToBind = new Intent(BROKER_ACCOUNT_SERVICE_INTENT_FILTER);
        brokerAccountServiceToBind.setPackage(brokerAppName);
        brokerAccountServiceToBind.setClassName(brokerAppName, "com.microsoft.aad.adal.BrokerAccountService");

        return brokerAccountServiceToBind;
    }

    private Map<String, String> prepareGetAuthTokenRequestData(final Context context, final Bundle requestBundle) {
        final Set<String> requestBundleKeys = requestBundle.keySet();

        final Map<String, String> requestData = new HashMap<>();
        for (final String key : requestBundleKeys) {
            if (key == AuthenticationConstants.Browser.REQUEST_ID) {
                requestData.put(key, String.valueOf(requestBundle.getInt(key)));
                continue;
            }
            requestData.put(key, requestBundle.getString(key));
        }
        requestData.put(AuthenticationConstants.Broker.CALLER_INFO_PACKAGE, context.getPackageName());

        return requestData;
    }

    private UserInfo[] convertUserInfoBundleToArray(final Bundle usersBundle) {
        if (usersBundle == null) {
            Logger.v(TAG, "No user info returned from broker account service.");
            return new UserInfo[] {};
        }

        final ArrayList<UserInfo> brokerUsers = new ArrayList<>();
        final Set<String> users = usersBundle.keySet();
        for (final String user : users) {
            final Bundle userBundle = usersBundle.getBundle(user);
            final String userId = userBundle.getString(
                    AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID);
            final String givenName = userBundle.getString(
                    AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME);
            final String familyName = userBundle.getString(
                    AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME);
            final String identityProvider = userBundle.getString(
                    AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER);
            final String displayableId = userBundle.getString(
                    AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE);

            brokerUsers.add(new UserInfo(userId, givenName, familyName, identityProvider, displayableId));
        }

        return brokerUsers.toArray(new UserInfo[brokerUsers.size()]);
    }

    private void performAsyncCallOnBound(final Context context, final Callback<BrokerAccountServiceConnection> callback) {
        bindToBrokerAccountService(context, new Callback<BrokerAccountServiceConnection>() {
            @Override
            public void onSuccess(final BrokerAccountServiceConnection result) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    callback.onSuccess(result);
                    result.unBindService(context);
                } else {
                    sThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(result);
                            result.unBindService(context);
                        }
                    });
                }
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }

    private void bindToBrokerAccountService(final Context context, final Callback<BrokerAccountServiceConnection> callback) {
        Logger.v(TAG, "Binding to BrokerAccountService for caller uid: " + android.os.Process.myUid());
        final Intent brokerAccountServiceToBind = getIntentForBrokerAccountService(context);

        final BrokerAccountServiceConnection connection = new BrokerAccountServiceConnection();
        final CallbackExecutor<BrokerAccountServiceConnection> callbackExecutor = new CallbackExecutor<>(callback);
        mPendingConnections.put(connection, callbackExecutor);
        context.bindService(brokerAccountServiceToBind, connection, Context.BIND_AUTO_CREATE);
    }

    private class BrokerAccountServiceConnection implements android.content.ServiceConnection {
        private IBrokerAccountService mBrokerAccountService;
        private boolean mBound;

        public IBrokerAccountService getBrokerAccountServiceProvider() {
            return mBrokerAccountService;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.v(TAG, "Broker Account service is connected.");
            mBrokerAccountService = IBrokerAccountService.Stub.asInterface(service);
            mBound = true;
            final CallbackExecutor<BrokerAccountServiceConnection> callbackExecutor = mPendingConnections.remove(this);
            if (callbackExecutor != null) {
                callbackExecutor.onSuccess(this);
            } else {
                Logger.v(TAG, "No callback is found.");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.v(TAG, "Broker Account service is disconnected.");
            mBound = false;
        }

        public void unBindService(final Context context) {
            // Service disconnect is async operation, in case of race condition, having the service binding check queued up
            // in main message looper and unbind it.
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBound) {
                        try {
                            context.unbindService(BrokerAccountServiceConnection.this);
                        } catch (final IllegalArgumentException exception) {
                            // unbindService throws "Service not registered" IllegalArgumentException. We are still investigating
                            // why this is happening. Meanwhile to unblock the release we are adding this workaround.
                            // Issue #808 tracks the future investigation.
                            Logger.e(TAG, "Unbind threw IllegalArgumentException", "", null, exception);
                        } finally {
                            mBound = false;
                        }
                    }
                }
            });
        }
    }
}
