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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

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

    private static final String AUTHENTICATOR_CANCELS_REQUEST = "Authenticator cancels the request";

    public BrokerProxy() {
        mBrokerTag = AuthenticationSettings.INSTANCE.getBrokerSignature();
    }

    public BrokerProxy(final Context ctx) {
        mContext = ctx;
        mAcctManager = AccountManager.get(mContext);
        mHandler = new Handler(mContext.getMainLooper());
        mBrokerTag = AuthenticationSettings.INSTANCE.getBrokerSignature();
    }

    enum SwitchToBroker {
        CAN_SWITCH_TO_BROKER,
        CANNOT_SWITCH_TO_BROKER,
        NEED_PERMISSIONS_TO_SWITCH_TO_BROKER
    }

    /**
     * Verifies the broker related app and AD-Authenticator in Account Manager
     * ADAL directs call to AccountManager if component is valid and present. It
     * does not direct call if the caller is from Authenticator itself.
     */
    @Override
    public SwitchToBroker canSwitchToBroker(final String authorityUrlStr) {
        final URL authorityUrl;
        try {
            authorityUrl = new URL(authorityUrlStr);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL.name()
            );
        }
        final String packageName = mContext.getPackageName();
        boolean canSwitchToBroker = AuthenticationSettings.INSTANCE.getUseBroker()
                && !packageName.equalsIgnoreCase(AuthenticationSettings.INSTANCE.getBrokerPackageName())
                && !packageName.equalsIgnoreCase(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME)
                && verifyAuthenticator(mAcctManager)
                && !UrlExtensions.isADFSAuthority(authorityUrl);

        // We don't need any permissions if the broker has the service supported.
        // checkAccount will also be skipped. checkAccount is looking for a matching Authenticator, and if it doesn't
        // AccountChooserActivity supported, we'll need to make sure there is an account existed; otherwise account can
        // be added directly from the AccountChooser. If the broker supports the new service, it MUST support AccountChooser,
        // no need to do checkAccount.
        if (!canSwitchToBroker) {
            Logger.v(TAG, "Broker auth is turned off or no valid broker is available on the device, cannot switch to broker.");
            return SwitchToBroker.CANNOT_SWITCH_TO_BROKER;
        }

        if (!isBrokerAccountServiceSupported()) {
            canSwitchToBroker = canSwitchToBroker && checkAccount(mAcctManager, "", "");
            if (!canSwitchToBroker) {
                Logger.v(TAG, "No valid account existed in broker, cannot switch to broker for auth.");
                return SwitchToBroker.CANNOT_SWITCH_TO_BROKER;
            }

            try {
                verifyBrokerPermissionsAPI23AndHigher();
            } catch (final UsageAuthenticationException exception) {
                Logger.v(TAG, "Missing GET_ACCOUNTS permission, cannot switch to broker.");
                return SwitchToBroker.NEED_PERMISSIONS_TO_SWITCH_TO_BROKER;
            }
        }

        return SwitchToBroker.CAN_SWITCH_TO_BROKER;
    }

    /**
     * Do this check after other checks.
     */
    public boolean verifyUser(String username, String uniqueid) {
        if (!isBrokerAccountServiceSupported()) {
            return checkAccount(mAcctManager, username, uniqueid);
        }

        // VerifyUser is to check the user existence in broker if AccountChooser is not supported.
        return true;
    }

    @Override
    public boolean canUseLocalCache(final String authorityUrlStr) {
        if (canSwitchToBroker(authorityUrlStr) == SwitchToBroker.CANNOT_SWITCH_TO_BROKER) {
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
     * To verify if App gives permissions to AccountManager to use broker.
     * If target version is lower than 23, calling app has to have GET_ACCOUNTS,
     * MANAGE_ACCOUNTS, USE_CREDENTIALS permissions granted in the Manifest.xml.
     *
     * @return true if all required permissions are granted, otherwise the exception will be thrown.
     * @throws UsageAuthenticationException
     */
    public boolean verifyBrokerPermissionsAPI22AndLess() throws UsageAuthenticationException {
        final StringBuilder permissionMissing = new StringBuilder();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            permissionMissing.append(checkPermission("android.permission.GET_ACCOUNTS"));
            permissionMissing.append(checkPermission("android.permission.MANAGE_ACCOUNTS"));
            permissionMissing.append(checkPermission("android.permission.USE_CREDENTIALS"));

            if (permissionMissing.length() == 0) {
                return true;
            }

            throw new UsageAuthenticationException(
                    ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING,
                    "Broker related permissions are missing for " + permissionMissing.toString());
        }

        Logger.v(TAG, "Misused of the checking function. This is function is only used to checking the broker permissions of API 22 or less.");
        return true;
    }

    /**
     * To verify if App gives permissions to AccountManager to use broker.
     * Beginning in Android 6.0 (API level 23), the run-time permission GET_ACCOUNTS is required
     * which need to be requested in the runtime by the calling app.
     *
     * @return true if all required permissions are granted, otherwise the exception will be thrown.
     * @throws UsageAuthenticationException
     */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean verifyBrokerPermissionsAPI23AndHigher() throws UsageAuthenticationException {
        final StringBuilder permissionMissing = new StringBuilder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionMissing.append(checkPermission("android.permission.GET_ACCOUNTS"));

            if (permissionMissing.length() == 0) {
                return true;
            }

            throw new UsageAuthenticationException(
                    ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING,
                    "Broker related permissions are missing for " + permissionMissing.toString());
        }

        Logger.v(TAG, "Device is lower than 23, skip the GET_ACCOUNTS permission check.");
        return true;
    }

    private String checkPermission(final String permissionName) {
        final PackageManager pm = mContext.getPackageManager();
        if (pm.checkPermission(permissionName, mContext.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            Logger.w(
                    TAG,
                    "Broker related permissions are missing for " + permissionName,
                    "", ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING);
            return permissionName + ' ';
        }

        return "";
    }

    private void verifyNotOnMainThread() {
        final Looper looper = Looper.myLooper();
        if (looper != null && looper == mContext.getMainLooper()) {
            final IllegalStateException exception = new IllegalStateException(
                    "calling this from your main thread can lead to deadlock");
            Logger.e(TAG, "calling this from your main thread can lead to deadlock and/or ANRs", "",
                    ADALError.DEVELOPER_CALLING_ON_MAIN_THREAD, exception);
            if (mContext.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.FROYO) {
                throw exception;
            }
        }
    }

    private Account findAccount(String accountName, Account[] accountList) {
        if (accountList != null) {
            for (Account account : accountList) {
                if (account != null && account.name != null && account.name.equalsIgnoreCase(accountName)) {
                    return account;
                }
            }
        }

        return null;
    }

    private UserInfo findUserInfo(String userid, UserInfo[] userList) {
        if (userList != null) {
            for (UserInfo user : userList) {
                if (user != null && !TextUtils.isEmpty(user.getUserId()) && user.getUserId().equalsIgnoreCase(userid)) {
                    return user;
                }
            }
        }

        return null;
    }

    /**
     * Gets accessToken from Broker component.
     */
    @Override
    public AuthenticationResult getAuthTokenInBackground(final AuthenticationRequest request)
            throws AuthenticationException {

        verifyNotOnMainThread();

        final Bundle requestBundle = getBrokerOptions(request);

        // check if broker supports the new service, if it does not we need to switch back to the old way
        final Bundle bundleResult;
        if (isBrokerAccountServiceSupported()) {
            bundleResult = BrokerAccountServiceHandler.getInstance().getAuthToken(mContext, requestBundle);
        } else {
            bundleResult = getAuthTokenFromAccountManager(request, requestBundle);
        }
        if (bundleResult == null) {
            Logger.v(TAG, "No bundle result returned from broker for silent request.");
            return null;
        }

        return getResultFromBrokerResponse(bundleResult, request);
    }

    private Bundle getAuthTokenFromAccountManager(final AuthenticationRequest request, final Bundle requestBundle) throws AuthenticationException {
        // if there is not any user added to account, it returns empty
        final Account targetAccount = getTargetAccount(request);

        Bundle bundleResult = null;
        if (targetAccount != null) {

            // blocking call to get token from cache or refresh request in
            // background at Authenticator
            try {
                // It does not expect activity to be launched.
                // AuthenticatorService is handling the request at
                // AccountManager.
                //
                final AccountManagerFuture<Bundle> result = mAcctManager.getAuthToken(targetAccount,
                        AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                        requestBundle, false,
                        null, //set to null to avoid callback
                        mHandler);

                // Making blocking request here
                Logger.v(TAG, "Received result from broker");
                bundleResult = result.getResult();
            } catch (OperationCanceledException e) {
                Logger.e(TAG, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.AUTH_FAILED_CANCELLED, e);
            } catch (AuthenticatorException e) {
                Logger.e(TAG, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.BROKER_AUTHENTICATOR_NOT_RESPONDING);
                if (e.getMessage() != null && e.getMessage().contains(ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE.getDescription())) {
                    throw new AuthenticationException(ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE,
                            "Received error from broker, errorCode: " + e.getMessage());
                }
            } catch (IOException e) {
                // Authenticator gets problem from webrequest or file read/write
                Logger.e(TAG, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION);
            }

            Logger.v(TAG, "Returning result from broker");
            return bundleResult;
        } else {
            Logger.v(TAG, "Target account is not found");
        }

        return null;
    }

    private boolean isBrokerAccountServiceSupported() {
        final Intent brokerAccountServiceIntent = BrokerAccountServiceHandler.getIntentForBrokerAccountService(mContext);
        return isServiceSupported(mContext, brokerAccountServiceIntent);
    }

    private boolean isServiceSupported(final Context context, final Intent intent) {
        if (intent == null) {
            return false;
        }

        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> infos = packageManager.queryIntentServices(intent, 0);
        return infos != null && infos.size() > 0;
    }

    private Account getTargetAccount(final AuthenticationRequest request) {
        Account targetAccount = null;
        final Account[] accountList = mAcctManager.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);

        if (!TextUtils.isEmpty(request.getBrokerAccountName())) {
            targetAccount = findAccount(request.getBrokerAccountName(), accountList);
        } else {
            try {
                UserInfo[] users = getBrokerUsers();
                UserInfo matchingUser = findUserInfo(request.getUserId(), users);
                if (matchingUser != null) {
                    targetAccount = findAccount(matchingUser.getDisplayableId(), accountList);
                }
            } catch (IOException | AuthenticatorException | OperationCanceledException e) {
                Logger.e(TAG, e.getMessage(), "", ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION, e);
            }
        }

        return targetAccount;
    }

    private AuthenticationResult getResultFromBrokerResponse(final Bundle bundleResult, final AuthenticationRequest request)
            throws AuthenticationException {
        if (bundleResult == null) {
            throw new IllegalArgumentException("bundleResult");
        }

        int errCode = bundleResult.getInt(AccountManager.KEY_ERROR_CODE);
        final String msg = bundleResult.getString(AccountManager.KEY_ERROR_MESSAGE);

        final String oauth2ErrorCode = bundleResult.getString(AuthenticationConstants.OAuth2.ERROR);
        final String oauth2ErrorDescription = bundleResult.getString(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION);
        if (!StringExtensions.isNullOrBlank(msg)) {
            final ADALError adalErrorCode;
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
            case AccountManager.ERROR_CODE_NETWORK_ERROR:
                adalErrorCode = ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE;
                break;
            default:
                adalErrorCode = ADALError.BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN;
            }

            throw new AuthenticationException(adalErrorCode, msg);
        } else if (!StringExtensions.isNullOrBlank(oauth2ErrorCode) && request.isSilent()) {
            throw new AuthenticationException(ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED,
                    "Received error from broker, errorCode: " + oauth2ErrorCode + "; ErrorDescription: " + oauth2ErrorDescription);
        } else {
            boolean initialRequest = bundleResult.getBoolean(AuthenticationConstants.Broker.ACCOUNT_INITIAL_REQUEST);
            if (initialRequest) {
                // Initial request from app to Authenticator needs to launch
                // prompt
                return AuthenticationResult.createResultForInitialRequest();
            }

            // IDtoken is not present in the current broker user model
            UserInfo userinfo = UserInfo.getUserInfoFromBrokerResult(bundleResult);
            final String tenantId = bundleResult.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID,
                    "");

            final Date expires;
            if (bundleResult.getLong(AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE) == 0) {
                Logger.v(TAG, "Broker doesn't return expire date, set it current date plus one hour");
                final Calendar currentTime = new GregorianCalendar();
                currentTime.add(Calendar.SECOND, AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC);
                expires = currentTime.getTime();
            } else {
                expires = new Date(bundleResult.getLong(AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE));
            }

            return new AuthenticationResult(bundleResult.getString(AccountManager.KEY_AUTHTOKEN),
                    "", expires, false, userinfo, tenantId, "", null);
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

        SharedPreferences prefs = mContext.getSharedPreferences(KEY_SHARED_PREF_ACCOUNT_LIST, Activity.MODE_PRIVATE);
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
                if (isBrokerAccountServiceSupported()) {
                    BrokerAccountServiceHandler.getInstance().removeAccounts(mContext);
                } else {
                    removeAccountFromAccountManager();
                }
            }
        }).start();
    }

    private void removeAccountFromAccountManager() {
        // getAuthToken call will execute in async as well
        Logger.v(TAG, "removeAccounts:");
        Account[] accountList = mAcctManager
                .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        if (accountList.length != 0) {
            for (Account targetAccount : accountList) {
                Logger.v(TAG, "remove tokens for:" + targetAccount.name);

                Bundle brokerOptions = new Bundle();
                brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS,
                        AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS_VALUE);

                // only this API call sets calling UID. We are
                // setting
                // special value to indicate that tokens for this
                // calling UID will be cleaned from this account
                mAcctManager.getAuthToken(targetAccount, AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                        brokerOptions, false,
                        null /*
                                      * set to null to avoid callback
                                      */, mHandler);

            }
        }
    }

    /**
     * Gets intent for authentication activity from Broker component to start
     * from calling app's activity to control the lifetime of the activity.
     */
    @Override
    public Intent getIntentForBrokerActivity(final AuthenticationRequest request) {
        final Bundle requestBundle = getBrokerOptions(request);
        final Intent intent;
        if (isBrokerAccountServiceSupported()) {
            intent = BrokerAccountServiceHandler.getInstance().getIntentForInteractiveRequest(mContext);
            intent.putExtras(requestBundle);
        } else {
            intent = getIntentForBrokerActivityFromAccountManager(requestBundle);
        }

        if (intent != null) {
            intent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST,
                    AuthenticationConstants.Broker.BROKER_REQUEST);

            // Only the new broker with PRT support can read the new PromptBehavior force_prompt.
            // If talking to the old broker, and PromptBehavior is set as force_prompt, reset it as
            // Always.
            if (!isBrokerWithPRTSupport(intent) && PromptBehavior.FORCE_PROMPT == request.getPrompt()) {
                Logger.v(TAG, "FORCE_PROMPT is set for broker auth via old version of broker app, reset to ALWAYS.");
                intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT, PromptBehavior.Always.name());
            }
        }

        return intent;
    }

    private Intent getIntentForBrokerActivityFromAccountManager(final Bundle addAccountOptions) {
        Intent intent = null;
        try {
            // Callback is not passed since it is making a blocking call to get
            // intent. Activity needs to be launched from calling app
            // to get the calling app's metadata if needed at BrokerActivity.
            final AccountManagerFuture<Bundle> result = mAcctManager.addAccount(
                    AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                    AuthenticationConstants.Broker.AUTHTOKEN_TYPE, null, addAccountOptions, null, null, mHandler);

            // Making blocking request here
            Bundle bundleResult = result.getResult();
            // Authenticator should throw OperationCanceledException if
            // token is not available
            intent = bundleResult.getParcelable(AccountManager.KEY_INTENT);
            // Add flag to this intent to signal that request is for broker logic

        } catch (OperationCanceledException e) {
            Logger.e(TAG, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.AUTH_FAILED_CANCELLED, e);
        } catch (AuthenticatorException e) {
            //
            // TODO add retry logic since authenticator is not responding to
            // the request
            Logger.e(TAG, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.BROKER_AUTHENTICATOR_NOT_RESPONDING, e);
        } catch (IOException e) {
            // Authenticator gets problem from webrequest or file read/write
            Logger.e(TAG, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION, e);
        }

        return intent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCurrentActiveBrokerPackageName() {
        AuthenticatorDescription[] authenticators = mAcctManager.getAuthenticatorTypes();
        for (AuthenticatorDescription authenticator : authenticators) {
            if (authenticator.type.equals(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE)) {
                return authenticator.packageName;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBrokerAppVersion(final String brokerAppPackageName) throws NameNotFoundException {
        final PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(brokerAppPackageName, 0);

        return "VersionName=" + packageInfo.versionName + ";VersonCode=" + packageInfo.versionCode + ".";
    }

    private Bundle getBrokerOptions(final AuthenticationRequest request) {
        Bundle brokerOptions = new Bundle();
        // request needs to be parcelable to send across process
        brokerOptions.putInt(AuthenticationConstants.Browser.REQUEST_ID, request.getRequestId());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY, request.getAuthority());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_RESOURCE, request.getResource());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_REDIRECT, request.getRedirectUri());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY, request.getClientId());
        brokerOptions.putString(AuthenticationConstants.Broker.ADAL_VERSION_KEY, request.getVersion());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, request.getUserId());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_EXTRA_QUERY_PARAM,
                request.getExtraQueryParamsAuthentication());
        if (request.getCorrelationId() != null) {
            brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID,
                    request.getCorrelationId().toString());
        }

        String username = request.getBrokerAccountName();
        if (StringExtensions.isNullOrBlank(username)) {
            username = request.getLoginHint();
        }

        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT, username);
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_NAME, username);

        if (request.getPrompt() != null) {
            brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_PROMPT, request.getPrompt().name());
        }
        return brokerOptions;
    }

    /**
     * Check if the broker is the new one with PRT support by checking the version returned from intent.
     * Only new broker will send {@link AuthenticationConstants.Broker.BROKER_VERSION}, and the version number
     * will be v2.
     */
    private boolean isBrokerWithPRTSupport(final Intent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("intent");
        }

        // Only new broker with PRT support will send down the value and the version will be v2
        final String brokerVersion = intent.getStringExtra(AuthenticationConstants.Broker.BROKER_VERSION);
        return AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION.equalsIgnoreCase(brokerVersion);
    }

    /**
     * Gets current broker user(Single User model).
     *
     * @return Current account name at {@link AccountManager}
     */
    public String getCurrentUser() {
        // authenticator is not used if there is not any user
        if (isBrokerAccountServiceSupported()) {
            verifyNotOnMainThread();

            final UserInfo[] users;
            try {
                users = BrokerAccountServiceHandler.getInstance().getBrokerUsers(mContext);
            } catch (final IOException e) {
                Logger.e(TAG, "No current user could be retrieved.", "", null, e);
                return null;
            }

            return users.length == 0 ? null : users[0].getDisplayableId();
        } else {
            Account[] accountList = mAcctManager.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
            if (accountList.length > 0) {
                return accountList[0].name;
            }
        }

        return null;
    }

    private boolean checkAccount(final AccountManager am, String username, String uniqueId) {
        AuthenticatorDescription[] authenticators = am.getAuthenticatorTypes();
        for (AuthenticatorDescription authenticator : authenticators) {
            if (authenticator.type.equals(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE)) {

                Account[] accountList = mAcctManager
                        .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);

                // For new broker with PRT support, both company portal and
                // azure authenticator will be able to support multi-user.
                if (authenticator.packageName
                        .equalsIgnoreCase(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME)
                        || authenticator.packageName
                        .equalsIgnoreCase(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME)
                        || authenticator.packageName
                        .equalsIgnoreCase(AuthenticationSettings.INSTANCE.getBrokerPackageName())) {
                    // Existing broker logic only connects to broker for token
                    // requests if account exists. New version can allow to
                    // add accounts through Adal.
                    if (hasSupportToAddUserThroughBroker(authenticator.packageName)) {
                        return true;
                    } else if (accountList.length > 0) {
                        return verifyAccount(accountList, username, uniqueId);
                    }
                }
            }
        }

        return false;
    }

    private boolean verifyAccount(Account[] accountList, String username, String uniqueId) {
        if (!StringExtensions.isNullOrBlank(username)) {
            return username.equalsIgnoreCase(accountList[0].name);
        }

        if (!StringExtensions.isNullOrBlank(uniqueId)) {
            // Uniqueid for account at authenticator is not available with
            // Account
            UserInfo[] users;
            try {
                users = getBrokerUsers();
                UserInfo matchingUser = findUserInfo(uniqueId, users);
                return matchingUser != null;
            } catch (IOException | AuthenticatorException | OperationCanceledException e) {
                Logger.e(TAG, "VerifyAccount:" + e.getMessage(), "", ADALError.BROKER_AUTHENTICATOR_EXCEPTION, e);
            }

            Logger.v(TAG, "It could not check the uniqueid from broker. It is not using broker");
            return false;
        }

        // if username or uniqueid not specified, it should use the broker
        // account.
        return true;
    }

    /**
     * True if broker has multi-user support. ADAL is checking the existence of
     * .ui.AccountChooserActivity which only exists in the new broker with PRT
     * support.
     */
    private boolean hasSupportToAddUserThroughBroker(final String brokerPackageName) {
        final Intent intent = new Intent();
        intent.setPackage(brokerPackageName);
        intent.setClassName(brokerPackageName, brokerPackageName + ".ui.AccountChooserActivity");
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> infos = packageManager.queryIntentActivities(intent, 0);
        return infos.size() > 0;
    }

    private boolean verifySignature(final String brokerPackageName) {
        try {
            // Read all the certificates associated with the package name. In higher version of
            // android sdk, package manager will only returned the cert that is used to sign the
            // APK. Even a cert is claimed to be issued by another certificates, sdk will return
            // the signing cert. However, for the lower version of android, it will return all the
            // certs in the chain. We need to verify that the cert chain is correctly chained up.
            final List<X509Certificate> certs = readCertDataForBrokerApp(brokerPackageName);

            // Verify the cert list contains the cert we trust.
            verifySignatureHash(certs);

            // Perform the certificate chain validation. If there is only one cert returned,
            // no need to perform certificate chain validation.
            if (certs.size() > 1) {
                verifyCertificateChain(certs);
            }

            return true;
        } catch (NameNotFoundException e) {
            Logger.e(TAG, "Broker related package does not exist", "", ADALError.BROKER_PACKAGE_NAME_NOT_FOUND);
        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG, "Digest SHA algorithm does not exists", "", ADALError.DEVICE_NO_SUCH_ALGORITHM);
        } catch (final AuthenticationException | IOException | GeneralSecurityException e) {
            Logger.e(TAG, e.getMessage(), "", ADALError.BROKER_VERIFICATION_FAILED, e);
        }

        return false;
    }

    private void verifySignatureHash(final List<X509Certificate> certs) throws NoSuchAlgorithmException,
            CertificateEncodingException, AuthenticationException {
        for (final X509Certificate x509Certificate : certs) {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA");
            messageDigest.update(x509Certificate.getEncoded());

            // Check the hash for signer cert is the same as what we hardcoded.
            final String signatureHash = Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP);
            if (mBrokerTag.equals(signatureHash)
                    || AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_SIGNATURE.equals(signatureHash)) {
                return;
            }
        }

        throw new AuthenticationException(ADALError.BROKER_APP_VERIFICATION_FAILED);
    }

    @SuppressLint("PackageManagerGetSignatures")
    private List<X509Certificate> readCertDataForBrokerApp(final String brokerPackageName)
            throws NameNotFoundException, AuthenticationException, IOException,
            GeneralSecurityException {
        final PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(brokerPackageName,
                PackageManager.GET_SIGNATURES);
        if (packageInfo == null) {
            throw new AuthenticationException(ADALError.APP_PACKAGE_NAME_NOT_FOUND,
                    "No broker package existed.");
        }

        if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
            throw new AuthenticationException(ADALError.BROKER_APP_VERIFICATION_FAILED,
                    "No signature associated with the broker package.");
        }

        final List<X509Certificate> certificates = new ArrayList<>(packageInfo.signatures.length);
        for (final Signature signature : packageInfo.signatures) {
            final byte[] rawCert = signature.toByteArray();
            final InputStream certStream = new ByteArrayInputStream(rawCert);

            final CertificateFactory certificateFactory;
            final X509Certificate x509Certificate;
            try {
                certificateFactory = CertificateFactory.getInstance("X509");
                x509Certificate = (X509Certificate) certificateFactory.generateCertificate(
                        certStream);
                certificates.add(x509Certificate);
            } catch (final CertificateException e) {
                throw new AuthenticationException(ADALError.BROKER_APP_VERIFICATION_FAILED);
            }
        }

        return certificates;
    }

    private void verifyCertificateChain(final List<X509Certificate> certificates)
            throws GeneralSecurityException, AuthenticationException {
        // create certificate chain, find the self signed cert first and chain all the way back
        // to the signer cert. Also perform certificate signing validation when chaining them back.
        final X509Certificate issuerCert = getSelfSignedCert(certificates);
        final TrustAnchor trustAnchor = new TrustAnchor(issuerCert, null);
        final PKIXParameters pkixParameters = new PKIXParameters(Collections.singleton(trustAnchor));
        pkixParameters.setRevocationEnabled(false);
        final CertPath certPath = CertificateFactory.getInstance("X.509")
                .generateCertPath(certificates);

        final CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");
        certPathValidator.validate(certPath, pkixParameters);
    }

    // Will throw if there is more than one self-signed cert found.
    private X509Certificate getSelfSignedCert(final List<X509Certificate> certs)
            throws AuthenticationException {
        int count = 0;
        X509Certificate selfSignedCert = null;
        for (final X509Certificate x509Certificate : certs) {
            if (x509Certificate.getSubjectDN().equals(x509Certificate.getIssuerDN())) {
                selfSignedCert = x509Certificate;
                count++;
            }
        }

        if (count > 1 || selfSignedCert == null) {
            throw new AuthenticationException(ADALError.BROKER_APP_VERIFICATION_FAILED,
                    "Multiple self signed certs found or no self signed cert existed.");
        }

        return selfSignedCert;
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
    public UserInfo[] getBrokerUsers() throws OperationCanceledException, AuthenticatorException, IOException {
        // Calling this on main thread will cause exception since this is
        // waiting on AccountManagerFuture
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalArgumentException("Calling getBrokerUsers on main thread");
        }

        if (isBrokerAccountServiceSupported()) {
            return BrokerAccountServiceHandler.getInstance().getBrokerUsers(mContext);
        }

        return getUserInfoFromAccountManager();
    }

    private UserInfo[] getUserInfoFromAccountManager() throws OperationCanceledException, AuthenticatorException, IOException {
        final Account[] accountList = mAcctManager.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        final Bundle bundle = new Bundle();
        bundle.putBoolean(DATA_USER_INFO, true);
        Logger.v(TAG, "Retrieve all the accounts from account manager with broker account type, "
                + "and the account length is: " + accountList.length);

        // accountList will never be null, getAccountsByType will return an empty list if no matching account returned.
        // get info for each user
        final UserInfo[] users = new UserInfo[accountList.length];
        for (int i = 0; i < accountList.length; i++) {

            // Use AccountManager Api method to get extended user info
            final AccountManagerFuture<Bundle> result = mAcctManager.updateCredentials(accountList[i],
                    AuthenticationConstants.Broker.AUTHTOKEN_TYPE, bundle, null, null, null);
            Logger.v(TAG, "Waiting for userinfo retrieval result from Broker.");
            final Bundle userInfoBundle = result.getResult();

            users[i] = new UserInfo(
                    userInfoBundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID),
                    userInfoBundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME),
                    userInfoBundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME),
                    userInfoBundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER),
                    userInfoBundle.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE));
        }

        return users;
    }
}
