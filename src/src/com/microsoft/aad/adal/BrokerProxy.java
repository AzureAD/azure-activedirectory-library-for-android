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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
 * Handles interactions to authenticator inside the Account Manager.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class BrokerProxy implements IBrokerProxy {

    private static final String TAG = "BrokerProxy";

    private Context mContext;

    private AccountManager mAcctManager;

    private Handler mHandler;

    private final String mBrokerTag;

    private static final String KEY_ACCOUNT_LIST_DELIM = "|";

    private static final String KEY_SHARED_PREF_ACCOUNT_LIST = "com.microsoft.aad.adal.account.list";

    private static final String KEY_APP_ACCOUNTS_FOR_TOKEN_REMOVAL = "AppAccountsForTokenRemoval";

    public static final String DATA_USER_INFO = "com.microsoft.workaccount.user.info";

    private static final int ACCOUNT_MANAGER_ERROR_CODE_BAD_AUTHENTICATION = 9;

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
        String packageName = mContext.getPackageName();

        // ADAL switches broker for following conditions:
        // 1- app is not skipping the broker
        // 2- permissions are set in the manifest,
        // 3- if package is not broker itself
        // 4- signature of the broker is valid
        // 5- account exists
        return !AuthenticationSettings.INSTANCE.getSkipBroker()
                && verifyManifestPermissions()
                && !packageName.equalsIgnoreCase(AuthenticationSettings.INSTANCE
                        .getBrokerPackageName()) && verifyAuthenticator(mAcctManager);
    }

    @Override
    public boolean canUseLocalCache() {
        boolean brokerSwitch = canSwitchToBroker();
        if (!brokerSwitch) {
            Logger.v(TAG, "It does not use broker");
            return true;
        }

        String packageName = mContext.getPackageName();
        if (verifySignature(packageName)) {
            Logger.v(TAG, "Broker installer can use local cache");
            return true;
        }

        return false;
    }

    /**
     * App needs to give permission to AccountManager to use broker.
     */
    private boolean verifyManifestPermissions() {
        PackageManager pm = mContext.getPackageManager();
        boolean permission = PackageManager.PERMISSION_GRANTED == pm.checkPermission(
                "android.permission.GET_ACCOUNTS", mContext.getPackageName())
                && PackageManager.PERMISSION_GRANTED == pm.checkPermission(
                        "android.permission.MANAGE_ACCOUNTS", mContext.getPackageName())
                && PackageManager.PERMISSION_GRANTED == pm.checkPermission(
                        "android.permission.USE_CREDENTIALS", mContext.getPackageName());
        if (!permission) {
            Logger.w(
                    TAG,
                    "Broker related permissions are missing for GET_ACCOUNTS, MANAGE_ACCOUNTS, USE_CREDENTIALS",
                    "", ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING);
        }

        return permission;
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

    private Account findAccount(String accountName, Account[] accountList) {
        if (accountList != null) {
            for (Account account : accountList) {
                if (account != null && account.name != null
                        && account.name.equalsIgnoreCase(accountName)) {
                    return account;
                }
            }
        }

        return null;
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

        Account targetAccount = findAccount(request.getBrokerAccountName(), accountList);

        if (targetAccount != null) {
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
                        null /*
                              * set to null to avoid callback
                              */, mHandler);

                // Making blocking request here
                Logger.v(TAG, "Received result from Authenticator");
                Bundle bundleResult = result.getResult();
                // Authenticator should throw OperationCanceledException if
                // token is not available
                authResult = getResultFromBrokerResponse(bundleResult);
            } catch (OperationCanceledException e) {
                Logger.e(TAG, "Authenticator cancels the request", "",
                        ADALError.AUTH_FAILED_CANCELLED, e);
            } catch (AuthenticatorException e) {
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

        int errCode = bundleResult.getInt(AccountManager.KEY_ERROR_CODE);
        String msg = bundleResult.getString(AccountManager.KEY_ERROR_MESSAGE);
        if (!StringExtensions.IsNullOrBlank(msg)) {
            ADALError adalErrorCode = ADALError.BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN;
            switch (errCode) {
                case AccountManager.ERROR_CODE_BAD_ARGUMENTS:
                    adalErrorCode = ADALError.BROKER_AUTHENTICATOR_BAD_ARGUMENTS;
                    break;
                case ACCOUNT_MANAGER_ERROR_CODE_BAD_AUTHENTICATION:
                    adalErrorCode = ADALError.BROKER_AUTHENTICATOR_BAD_AUTHENTICATION;
                    break;
                case AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION:
                    adalErrorCode = ADALError.BROKER_AUTHENTICATOR_UNSUPPORTED_OPERATION;
                    break;
            }

            throw new AuthenticationException(adalErrorCode, msg);
        } else {
            boolean initialRequest = bundleResult
                    .getBoolean(AuthenticationConstants.Broker.ACCOUNT_INITIAL_REQUEST);
            if (initialRequest) {
                // Initial request from app to Authenticator needs to launch
                // prompt
                return AuthenticationResult.createResultForInitialRequest();
            }

            // IDtoken is not present in the current broker user model
            UserInfo userinfo = UserInfo.getUserInfoFromBrokerResult(bundleResult);
            AuthenticationResult result = new AuthenticationResult(
                    bundleResult.getString(AccountManager.KEY_AUTHTOKEN), "", null, false,
                    userinfo, "", "");
            return result;
        }
    }

    /**
     * Tracks accounts that user of the ADAL accessed from AccountManager. It
     * uses this list, when app calls remove accounts. It limits the account
     * removal to specific subset.
     */
    @Override
    public void saveAccount(String accountName) {
        if (accountName == null || accountName.isEmpty()) {
            return;
        }

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
     * Removes account from AccountManager that ADAL accessed from this context.
     */
    @Override
    public void removeAccounts() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // getAuthToken call will execute in async as well
                Logger.v(TAG, "removeAccounts:");
                Account[] accountList = mAcctManager
                        .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
                if (accountList != null) {
                    for (Account targetAccount : accountList) {
                        Logger.v(TAG, "remove tokens for:" + targetAccount.name);
                        if (targetAccount != null) {
                            Bundle brokerOptions = new Bundle();
                            brokerOptions.putString(
                                    AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS,
                                    AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS_VALUE);

                            // only this API call sets calling UID. We are
                            // setting
                            // special value to indicate that tokens for this
                            // calling UID will be cleaned from this account
                            mAcctManager.getAuthToken(targetAccount,
                                    AuthenticationConstants.Broker.AUTHTOKEN_TYPE, brokerOptions,
                                    false, null /*
                                                 * set to null to avoid callback
                                                 */, mHandler);
                        }
                    }
                }
            }
        }).start();
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
            // intent. Activity needs to be launched from calling app
            // to get the calling app's metadata if needed at BrokerActivity.
            Bundle addAccountOptions = getBrokerOptions(request);
            result = mAcctManager.addAccount(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                    AuthenticationConstants.Broker.AUTHTOKEN_TYPE, null, addAccountOptions, null,
                    null, mHandler);

            // Making blocking request here
            Bundle bundleResult = result.getResult();
            // Authenticator should throw OperationCanceledException if
            // token is not available
            intent = bundleResult.getParcelable(AccountManager.KEY_INTENT);

            // Add flag to this intent to signal that request is for broker
            // logic
            if (intent != null) {
                intent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST,
                        AuthenticationConstants.Broker.BROKER_REQUEST);
            }
        } catch (OperationCanceledException e) {
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
        brokerOptions.putString(AuthenticationConstants.Broker.ADAL_VERSION_KEY,
                request.getVersion());
        if (request.getCorrelationId() != null) {
            brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID, request
                    .getCorrelationId().toString());
        }

        String username = request.getBrokerAccountName();
        if(StringExtensions.IsNullOrBlank(username)){
            username = request.getLoginHint();
        }

        brokerOptions
                .putString(AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT, username);
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_NAME, username);
        
        if (request.getPrompt() != null) {
            brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_PROMPT, request
                    .getPrompt().name());
        }
        return brokerOptions;
    }

    /**
     * Gets current broker user(Single User model).
     * 
     * @return Current account name at {@link AccountManager}
     */
    public String getCurrentUser() {
        // authenticator is not used if there is not any user
        Account[] accountList = mAcctManager
                .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        if (accountList != null && accountList.length > 0) {
            return accountList[0].name;
        }

        return null;
    }

    private boolean verifySignature(final String brokerPackageName) {
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(brokerPackageName,
                    PackageManager.GET_SIGNATURES);

            if (info != null && info.signatures != null) {
                // Broker App can be signed with multiple certificates. It will
                // look all of them until it finds the correct one for ADAL
                // broker.
                for (Signature signature : info.signatures) {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    String tag = Base64.encodeToString(md.digest(), Base64.NO_WRAP);

                    // Company portal(Intune) app and Azure authenticator app
                    // have authenticator.
                    if (tag.equals(mBrokerTag)
                            || tag.equals(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_SIGNATURE)) {
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
            if (authenticator.type.equals(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE)
                    && verifySignature(authenticator.packageName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Waits on AccountManager results, so it should not be called on main
     * thread.
     * 
     * @throws IOException
     * @throws AuthenticatorException
     * @throws OperationCanceledException
     */
    @Override
    public UserInfo[] getBrokerUsers() throws OperationCanceledException, AuthenticatorException,
            IOException {

        // Calling this on main thread will cause exception since this is
        // waiting on AccountManagerFuture
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalArgumentException("Calling getBrokerUsers on main thread");
        }

        Account[] accountList = mAcctManager
                .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        Bundle bundle = new Bundle();
        bundle.putBoolean(DATA_USER_INFO, true);

        if (accountList != null) {

            // get info for each user
            UserInfo[] users = new UserInfo[accountList.length];
            for (int i = 0; i < accountList.length; i++) {

                // Use AccountManager Api method to get extended user info
                AccountManagerFuture<Bundle> result = mAcctManager.updateCredentials(
                        accountList[i], AuthenticationConstants.Broker.AUTHTOKEN_TYPE, bundle,
                        null, null, null);
                Logger.v(TAG, "Waiting for the result");
                Bundle userInfoBundle = result.getResult();

                users[i] = new UserInfo(
                        userInfoBundle
                                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID),
                        userInfoBundle
                                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME),
                        userInfoBundle
                                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME),
                        userInfoBundle
                                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER),
                        userInfoBundle
                                .getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE));
            }

            return users;
        }
        return null;
    }
}
