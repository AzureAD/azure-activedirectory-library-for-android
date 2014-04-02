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

package com.microsoft.adal;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

/**
 * Handles interactions to authenticator inside the Account Manager
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class BrokerProxy implements IBrokerProxy {

    private static final String TAG = "BrokerProxy";

    private Context mContext;

    private AccountManager mAcctManager;

    private Handler mHandler;

    private final String mBrokerTag;

    private static final String KEY_ACCOUNT_LIST_DELIM = "||";

    private static final String KEY_SHARED_PREF_ACCOUNT_LIST = "com.microsoft.adal.account.list";

    private static final String KEY_APP_ACCOUNTS_FOR_TOKEN_REMOVAL = "AppAccountsForTokenRemoval";

    public BrokerProxy() {
        mBrokerTag = AuthenticationSettings.INSTANCE.getBrokerSignature();
    }

    public BrokerProxy(final Context ctx) {
        mContext = ctx;
        mAcctManager = AccountManager.get(mContext);
        mHandler = new Handler(mContext.getMainLooper());
        mBrokerTag = AuthenticationSettings.INSTANCE.getBrokerSignature();
    }

    /**
     * Verifies the broker related app and AD-Authenticator in Account Manager
     * ADAL directs call to AccountManager if component is valid and present. It
     * does not direct call if the caller is from Authenticator itself.
     */
    @Override
    public boolean canSwitchToBroker() {
        return !mContext.getPackageName().equalsIgnoreCase(
                AuthenticationSettings.INSTANCE.getBrokerPackageName())
                && verifyBroker() && verifyAuthenticator(mAcctManager);
    }

    private void verifyNotOnMainThread() {
        final Looper looper = Looper.myLooper();
        if (looper != null && looper == mContext.getMainLooper()) {
            final IllegalStateException exception = new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
            Logger.e(TAG, "calling this from your main thread can lead to deadlock and/or ANRs",
                    "", ADALError.DEVELOPER_CALLING_ON_MAIN_THREAD, exception);
            if (mContext.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.FROYO) {
                throw exception;
            }
        }
    }

    /**
     * Gets accessToken from Broker component.
     */
    @Override
    public AuthenticationResult getAuthTokenInBackground(final AuthenticationRequest request) {

        AuthenticationResult authResult = null;
        verifyNotOnMainThread();

        // if there is not any user added to account, it returns empty
        Account[] accountList = mAcctManager
                .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        Logger.v(TAG, "Account list length:" + accountList.length);
        Account targetAccount = getAccount(accountList, getAccountLookupUsername(request));

        if (targetAccount != null) {
            // add some dummy values to make a test call
            Bundle brokerOptions = getBrokerOptions(request);

            // blocking call to get token from cache or refresh request in
            // background at Authenticator
            AccountManagerFuture<Bundle> result = null;
            try {
                // It does not expect activity to be launched.
                // AuthenticatorService is handling the request at
                // AccountManager.
                //
                result = mAcctManager.getAuthToken(targetAccount,
                        AuthenticationConstants.Broker.AUTHTOKEN_TYPE, brokerOptions, false,
                        null /* set to null to avoid callback */, mHandler);

                // Making blocking request here
                Logger.v(TAG, "Received result from Authenticator");
                Bundle bundleResult = result.getResult();
                // Authenticator should throw OperationCanceledException if
                // token is not available
                // TODO add test to broker side
                authResult = getResultFromBrokerResponse(bundleResult);
            } catch (OperationCanceledException e) {
                // TODO verify that authenticator exceptions are recorded in the
                // calling app
                Logger.e(TAG, "Authenticator cancels the request", "",
                        ADALError.AUTH_FAILED_CANCELLED, e);
            } catch (AuthenticatorException e) {
                // TODO add retry logic since authenticator is not responding to
                // the request
                Logger.e(TAG, "Authenticator cancels the request", "",
                        ADALError.BROKER_AUTHENTICATOR_NOT_RESPONDING);
            } catch (IOException e) {
                // Authenticator gets problem from webrequest or file read/write
                Logger.e(TAG, "Authenticator cancels the request", "",
                        ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION);
            }

            Logger.v(TAG, "Returning result from Authenticator");
            return authResult;
        }

        return null;
    }

    private AuthenticationResult getResultFromBrokerResponse(Bundle bundleResult) {
        if (bundleResult == null) {
            throw new IllegalArgumentException("bundleResult");
        }

        String accountName = bundleResult.getString(AccountManager.KEY_ACCOUNT_NAME);
        // record this account for calling app so that clear token can remove
        // this account
        saveAccount(accountName);
        UserInfo userinfo = UserInfo.getUserInfoFromBrokerResult(bundleResult);
        AuthenticationResult result = new AuthenticationResult(
                bundleResult.getString(AccountManager.KEY_AUTHTOKEN), "", null, false, userinfo);
        return result;
    }

    /**
     * Tracks accounts that user of the ADAL accessed from AccountManager. It
     * uses this list, when app calls remove accounts. It limits the account
     * removal to specific subset.
     */
    @Override
    public void saveAccount(String accountName) {
        if (accountName == null || accountName.isEmpty())
            return;

        SharedPreferences prefs = mContext.getSharedPreferences(KEY_SHARED_PREF_ACCOUNT_LIST,
                Activity.MODE_PRIVATE);
        String accountList = prefs.getString(KEY_APP_ACCOUNTS_FOR_TOKEN_REMOVAL, "");
        if (!accountList.contains(KEY_ACCOUNT_LIST_DELIM + accountName)) {
            accountList += KEY_ACCOUNT_LIST_DELIM + accountName;
            Editor prefsEditor = prefs.edit();
            prefsEditor.putString(KEY_APP_ACCOUNTS_FOR_TOKEN_REMOVAL, accountList);

            // apply will do Async disk write operation.
            prefsEditor.apply();
        }
    }

    /**
     * Removes account from AccountManager that ADAL accessed from this context
     */
    @Override
    public void removeAccounts() {
        Account[] accountList = mAcctManager
                .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        SharedPreferences prefs = mContext.getSharedPreferences(KEY_SHARED_PREF_ACCOUNT_LIST,
                Activity.MODE_PRIVATE);
        String delAccount = prefs.getString(KEY_APP_ACCOUNTS_FOR_TOKEN_REMOVAL, "");
        StringTokenizer st = new StringTokenizer(delAccount, KEY_ACCOUNT_LIST_DELIM);
        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            if (name != null && !name.isEmpty()) {
                Logger.v(TAG, "Removing account:" + name);
                Account targetAccount = getAccount(accountList, name);
                if (targetAccount != null) {
                    mAcctManager.removeAccount(targetAccount, null, null);
                    Logger.v(TAG, "Account exists and removed:" + name);
                } else {
                    Logger.w(TAG, "Account does not exists" + name, "",
                            ADALError.BROKER_ACCOUNT_DOES_NOT_EXIST);
                }
            }
        }
    }

    /**
     * Gets intent for authentication activity from Broker component to start
     * from calling app's activity to control the lifetime of the activity.
     */
    @Override
    public Intent getIntentForBrokerActivity(final AuthenticationRequest request) {
        Intent intent = null;
        AccountManagerFuture<Bundle> result = null;
        try {
            // Callback is not passed since it is making a blocking call to get
            // intent.
            // Activity needs to be launched from calling app to get the calling
            // app's metadata if needed at BrokerActivity.
            Bundle addAccountOptions = getBrokerOptions(request);
            result = mAcctManager.addAccount(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                    AuthenticationConstants.Broker.AUTHTOKEN_TYPE, null, addAccountOptions, null,
                    null, mHandler);

            // Making blocking request here
            Bundle bundleResult = result.getResult();
            // Authenticator should throw OperationCanceledException if
            // token is not available
            intent = bundleResult.getParcelable(AccountManager.KEY_INTENT);
        } catch (OperationCanceledException e) {
            // TODO verify that authenticator exceptions are recorded in the
            // calling app
            Logger.e(TAG, "Authenticator cancels the request", "", ADALError.AUTH_FAILED_CANCELLED,
                    e);
        } catch (AuthenticatorException e) {
            //
            // TODO add retry logic since authenticator is not responding to
            // the request
            Logger.e(TAG, "Authenticator cancels the request", "",
                    ADALError.BROKER_AUTHENTICATOR_NOT_RESPONDING);
        } catch (IOException e) {
            // Authenticator gets problem from webrequest or file read/write
            Logger.e(TAG, "Authenticator cancels the request", "",
                    ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION);
        }

        return intent;
    }

    private Bundle getBrokerOptions(final AuthenticationRequest request) {
        Bundle brokerOptions = new Bundle();
        // request needs to be parcelable to send across process
        brokerOptions.putInt(AuthenticationConstants.Browser.REQUEST_ID, request.getRequestId());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY,
                request.getAuthority());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_RESOURCE,
                request.getResource());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_REDIRECT,
                request.getRedirectUri());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY,
                request.getClientId());
        // TODO: this will be linked to account name
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT,
                request.getLoginHint());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_NAME,
                getAccountLookupUsername(request));
        return brokerOptions;
    }

    private String getAccountLookupUsername(final AuthenticationRequest request) {
        if (!StringExtensions.IsNullOrBlank(request.getLoginHint())) {
            // TODO ADAL uses loginhint to cache tokens for user. Cache changes
            // will affect this.
            return request.getLoginHint();
        }

        // If idtoken is not present, userid is unknown. Authenticator will
        // group the tokens based on account, so it needs to pass clientid to
        // group unknown users. Different apps signed by same certificates may
        // use same clientid, but they will have differnt packagenames.
        return request.getClientId();
    }

    private boolean verifyBroker() {
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(
                    AuthenticationSettings.INSTANCE.getBrokerPackageName(),
                    PackageManager.GET_SIGNATURES);

            if (info != null && info.signatures != null) {
                // Broker App can be signed with multiple certificates. It will
                // look
                // all of them
                // until it finds the correct one for ADAL broker.
                for (Signature signature : info.signatures) {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    String tag = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                    if (tag.equals(mBrokerTag)) {
                        return true;
                    }
                }
            }
        } catch (NameNotFoundException e) {
            Logger.e(TAG, "Broker related package does not exist", "",
                    ADALError.BROKER_PACKAGE_NAME_NOT_FOUND);
        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG, "Digest SHA algorithm does not exists", "",
                    ADALError.DEVICE_NO_SUCH_ALGORITHM);
        } catch (Exception e) {
            Logger.e(TAG, "Error in verifying signature", "", ADALError.BROKER_VERIFICATION_FAILED,
                    e);
        }

        return false;
    }

    private boolean verifyAuthenticator(final AccountManager am) {
        // there may be multiple authenticators from same package
        // , but there is only one entry for an authenticator type in
        // AccountManager.
        // If another app tries to install same authenticator type, it will
        // queue up and will be active after first one is uninstalled.
        AuthenticatorDescription[] authenticators = am.getAuthenticatorTypes();
        for (AuthenticatorDescription authenticator : authenticators) {
            if (authenticator.packageName.equals(AuthenticationSettings.INSTANCE
                    .getBrokerPackageName())
                    && authenticator.type
                            .equals(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE)) {
                return true;
            }
        }

        return false;
    }

    private Account getAccount(Account[] accounts, String username) {
        if (accounts != null && username != null) {
            for (Account account : accounts) {
                if (account.name.equalsIgnoreCase(username)) {
                    return account;
                }
            }
        }

        return null;
    }
}
