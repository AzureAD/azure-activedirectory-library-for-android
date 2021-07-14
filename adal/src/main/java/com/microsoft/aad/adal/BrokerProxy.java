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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.core.content.pm.PackageInfoCompat;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.RT_AGE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_ERROR;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SERVER_SUBERROR;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CliTelemInfo.SPE_RING;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2ErrorCode.INVALID_GRANT;

/**
 * Handles interactions to authenticator inside the Account Manager.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class BrokerProxy implements IBrokerProxy {

    private static final String TAG = "BrokerProxy";

    private Context mContext;

    private AccountManager mAcctManager;

    private Handler mHandler;

    private BrokerValidator mBrokerValidator;

    private static final String KEY_ACCOUNT_LIST_DELIM = "|";

    private static final String KEY_SHARED_PREF_ACCOUNT_LIST = "com.microsoft.aad.adal.account.list";

    private static final String KEY_APP_ACCOUNTS_FOR_TOKEN_REMOVAL = "AppAccountsForTokenRemoval";

    public static final String DATA_USER_INFO = "com.microsoft.workaccount.user.info";

    private static final int ACCOUNT_MANAGER_ERROR_CODE_BAD_AUTHENTICATION = 9;

    private static final String AUTHENTICATOR_CANCELS_REQUEST = "Authenticator cancels the request";

    BrokerProxy() {
    }

    BrokerProxy(final Context ctx) {
        mContext = ctx;
        mAcctManager = AccountManager.get(mContext);
        mHandler = new Handler(mContext.getMainLooper());
        mBrokerValidator = new BrokerValidator(ctx);
    }

    enum SwitchToBroker {
        CAN_SWITCH_TO_BROKER,
        CANNOT_SWITCH_TO_BROKER,
        NEED_PERMISSIONS_TO_SWITCH_TO_BROKER
    }

    /**
     * Verifies the broker related app and AD-Authenticator in Account Manager
     * ADAL directs call to AccountManager if component is valid and present.
     */
    @Override
    public SwitchToBroker canSwitchToBroker(final String authorityUrlStr) {
        // TODO: Get CompanyPortal to verify that they can also switch to broker when acquiring token with ADAL.

        final String methodName = ":canSwitchToBroker";
        final URL authorityUrl;
        try {
            authorityUrl = new URL(authorityUrlStr);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL.name()
            );
        }
        boolean canSwitchToBroker = AuthenticationSettings.INSTANCE.getUseBroker()
                && verifyAuthenticator(mAcctManager)
                && !UrlExtensions.isADFSAuthority(authorityUrl);

        // We don't need any permissions if the broker has the service supported.
        // checkAccount will also be skipped. checkAccount is looking for a matching Authenticator, and if it doesn't
        // AccountChooserActivity supported, we'll need to make sure there is an account existed; otherwise account can
        // be added directly from the AccountChooser. If the broker supports the new service, it MUST support AccountChooser,
        // no need to do checkAccount.
        if (!canSwitchToBroker) {
            Logger.v(TAG + methodName, "Broker auth is turned off or no valid broker is available on the device, cannot switch to broker.");
            return SwitchToBroker.CANNOT_SWITCH_TO_BROKER;
        }

        if (!isBrokerAccountServiceSupported()) {
            canSwitchToBroker = canSwitchToBroker && checkAccount(mAcctManager, "", "");
            if (!canSwitchToBroker) {
                Logger.v(TAG + methodName, "No valid account existed in broker, cannot switch to broker for auth.");
                return SwitchToBroker.CANNOT_SWITCH_TO_BROKER;
            }

            try {
                verifyBrokerPermissionsAPI23AndHigher();
            } catch (final UsageAuthenticationException exception) {
                Logger.v(TAG + methodName, "Missing GET_ACCOUNTS permission, cannot switch to broker.");
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

    public boolean verifyBrokerForSilentRequest(AuthenticationRequest request) throws AuthenticationException {
        // If we cannot switch to broker, return the result from local flow.
        final BrokerProxy.SwitchToBroker switchToBrokerFlag = canSwitchToBroker(request.getAuthority());
        if (switchToBrokerFlag == SwitchToBroker.CAN_SWITCH_TO_BROKER) {
            return verifyUser(request.getLoginHint(), request.getUserId());
        } else if (switchToBrokerFlag == BrokerProxy.SwitchToBroker.NEED_PERMISSIONS_TO_SWITCH_TO_BROKER) {
            //For android M and above
            throw new UsageAuthenticationException(
                    ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING,
                    "Broker related permissions are missing for GET_ACCOUNTS");
        }

        return false;
    }

    @Override
    public boolean canUseLocalCache(final String authorityUrlStr) {
        final String methodName = ":canUseLocalCache";
        if (canSwitchToBroker(authorityUrlStr) == SwitchToBroker.CANNOT_SWITCH_TO_BROKER) {
            Logger.v(TAG + methodName, "It does not use broker");
            return true;
        }

        String packageName = mContext.getPackageName();
        if (mBrokerValidator.verifySignature(packageName)) {
            Logger.v(TAG + methodName, "Broker installer can use local cache");
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

        Logger.v(TAG, "Device runs on 23 and above, skip the check for 22 and below.");
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
    public AuthenticationResult getAuthTokenInBackground(final AuthenticationRequest request, final BrokerEvent brokerEvent)
            throws AuthenticationException {

        verifyNotOnMainThread();

        final Bundle requestBundle = getBrokerOptions(request);

        // check if broker supports the new service, if it does not we need to switch back to the old way
        final Bundle bundleResult;
        if (isBrokerAccountServiceSupported()) {
            bundleResult = BrokerAccountServiceHandler.getInstance().getAuthToken(mContext, requestBundle, brokerEvent);
        } else {
            bundleResult = getAuthTokenFromAccountManager(request, requestBundle);
        }

        if (bundleResult == null) {
            Logger.v(TAG, "No bundle result returned from broker for silent request.");
            return null;
        }

        return getResultFromBrokerResponse(bundleResult, request);
    }

    private Bundle getAuthTokenFromAccountManager(final AuthenticationRequest request,
                                                  final Bundle requestBundle)
            throws AuthenticationException {
        // if there is not any user added to account, it returns empty
        final String methodName = ":getAuthTokenFromAccountManager";
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
                Logger.v(TAG + methodName, "Received result from broker");
                bundleResult = result.getResult();
            } catch (final OperationCanceledException e) {
                // Error code AUTH_FAILED_CANCELLED will be thrown if the request was canceled for any reason.
                Logger.e(TAG + methodName, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.AUTH_FAILED_CANCELLED, e);
                throw new AuthenticationException(ADALError.AUTH_FAILED_CANCELLED, e.getMessage(), e);
            } catch (final AuthenticatorException e) {
                // Error code BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN will be thrown if there was an error
                // communicating with the authenticator or if the authenticator returned an invalid response.
                if (!StringExtensions.isNullOrBlank(e.getMessage()) && e.getMessage().contains(INVALID_GRANT)) {
                    Logger.e(TAG + methodName, AUTHENTICATOR_CANCELS_REQUEST,
                            "Acquire token failed with 'invalid grant' error, cannot proceed with silent request.",
                            ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED);
                    throw new AuthenticationException(ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED, e.getMessage());
                } else {
                    Logger.e(TAG + methodName, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN);
                    throw new AuthenticationException(ADALError.BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN, e.getMessage());
                }
            } catch (final IOException e) {
                //  Error code BROKER_AUTHENTICATOR_IO_EXCEPTION will be thrown
                //  when Authenticator gets problem from webrequest or file read/write or network error
                Logger.e(TAG + methodName, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION);

                if (e.getMessage() != null && e.getMessage().contains(ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE.getDescription())) {
                    throw new AuthenticationException(ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE,
                            "Received error from broker, errorCode: " + e.getMessage());
                } else if (e.getMessage() != null && e.getMessage().contains(ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION.getDescription())) {
                    throw new AuthenticationException(ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION,
                            "Received error from broker, errorCode: " + e.getMessage());
                } else {
                    throw new AuthenticationException(ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION, e.getMessage(), e);
                }
            }

            Logger.v(TAG + methodName, "Returning result from broker");
            return bundleResult;
        } else {
            Logger.v(TAG + methodName, "Target account is not found");
            return null;
        }
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
        final String methodName = ":getTargetAccount";
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
                Logger.e(TAG + methodName, "Exception is thrown when trying to get target account.",
                        e.getMessage(), ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION, e);
            }
        }

        return targetAccount;
    }

    private AuthenticationResult getResultFromBrokerResponse(final Bundle bundleResult,
                                                             final AuthenticationRequest request)
            throws AuthenticationException {
        final String methodName = ":getResultFromBrokerResponse";
        if (bundleResult == null) {
            throw new IllegalArgumentException("bundleResult");
        }

        int errCode = bundleResult.getInt(AccountManager.KEY_ERROR_CODE);
        final String msg = bundleResult.getString(AccountManager.KEY_ERROR_MESSAGE);

        final String oauth2ErrorCode = bundleResult.getString(AuthenticationConstants.OAuth2.ERROR);
        final String oauth2ErrorDescription = bundleResult.getString(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION);
        final TelemetryUtils.CliTelemInfo cliTelemInfo = getCliTelemInfoFromBundle(bundleResult);

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
                case AccountManager.ERROR_CODE_CANCELED:
                    adalErrorCode = ADALError.AUTH_FAILED_CANCELLED;
                    break;
                case AccountManager.ERROR_CODE_NETWORK_ERROR:
                    if (msg.contains(ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION.getDescription())) {
                        adalErrorCode = ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION;
                        break;
                    } else if (msg.contains(ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE.getDescription())) {
                        adalErrorCode = ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE;
                        break;
                    } else {
                        adalErrorCode = ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION;
                        break;
                    }
                default:
                    adalErrorCode = ADALError.BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN;
            }

            final AuthenticationException authException = new AuthenticationException(adalErrorCode, msg);

            // Set Spe Ring / Client Telemetry
            authException.setSpeRing(cliTelemInfo.getSpeRing());
            authException.setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
            authException.setCliTelemErrorCode(cliTelemInfo.getServerErrorCode());
            authException.setCliTelemSubErrorCode(cliTelemInfo.getServerSubErrorCode());

            throw authException;
        } else if (!StringExtensions.isNullOrBlank(oauth2ErrorCode) && request.isSilent()) {
            final AuthenticationException exception = getAuthenticationExceptionForResult(
                    oauth2ErrorCode,
                    oauth2ErrorDescription,
                    bundleResult
            );
            final Serializable responseBody = bundleResult.getSerializable(AuthenticationConstants.OAuth2.HTTP_RESPONSE_BODY);
            final Serializable responseHeaders = bundleResult.getSerializable(AuthenticationConstants.OAuth2.HTTP_RESPONSE_HEADER);

            if (null != responseBody && responseBody instanceof HashMap) {
                exception.setHttpResponseBody((HashMap) responseBody);
            }

            if (null != responseHeaders && responseHeaders instanceof HashMap) {
                exception.setHttpResponseHeaders((HashMap) responseHeaders);
            }

            exception.setServiceStatusCode(bundleResult.getInt(AuthenticationConstants.OAuth2.HTTP_STATUS_CODE));
            throw exception;
        } else {
            boolean initialRequest = bundleResult.getBoolean(AuthenticationConstants.Broker.ACCOUNT_INITIAL_REQUEST);
            if (initialRequest) {
                // Initial request from app to Authenticator needs to launch
                // prompt
                return AuthenticationResult.createResultForInitialRequest(request.getClientId());
            }

            // IDtoken is not present in the current broker user model
            UserInfo userinfo = UserInfo.getUserInfoFromBrokerResult(bundleResult);
            final String tenantId = bundleResult.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID,
                    "");
            final String idToken = bundleResult.getString(AuthenticationConstants.Broker.ACCOUNT_IDTOKEN, "");

            final Date expires;
            if (bundleResult.getLong(AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE) == 0) {
                Logger.v(TAG + methodName, "Broker doesn't return expire date, set it current date plus one hour");
                final Calendar currentTime = new GregorianCalendar();
                currentTime.add(Calendar.SECOND, AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC);
                expires = currentTime.getTime();
            } else {
                expires = new Date(bundleResult.getLong(AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE));
            }

            final AuthenticationResult result = new AuthenticationResult(
                    bundleResult.getString(AccountManager.KEY_AUTHTOKEN),
                    "",
                    expires,
                    false,
                    userinfo,
                    tenantId,
                    idToken,
                    null,
                    request.getClientId()
            );

            // set the x-ms-clitelem data
            result.setCliTelemInfo(cliTelemInfo);

            return result;
        }
    }

    private TelemetryUtils.CliTelemInfo getCliTelemInfoFromBundle(final Bundle brokerResult) {
        final TelemetryUtils.CliTelemInfo cliTelemInfo = new TelemetryUtils.CliTelemInfo();
        cliTelemInfo._setServerErrorCode(brokerResult.getString(SERVER_ERROR));
        cliTelemInfo._setServerSubErrorCode(brokerResult.getString(SERVER_SUBERROR));
        cliTelemInfo._setRefreshTokenAge(brokerResult.getString(RT_AGE));
        cliTelemInfo._setSpeRing(brokerResult.getString(SPE_RING));
        return cliTelemInfo;
    }

    private AuthenticationException getAuthenticationExceptionForResult(final String oauth2ErrorCode,
                                                                        final String oauth2ErrorDescription,
                                                                        final Bundle bundleResult) {
        final String message = String.format("Received error from broker, errorCode: %s; ErrorDescription: %s",
                oauth2ErrorCode, oauth2ErrorDescription);

        // check the response body for the "unauthorized_client" error and the "protection_policy_required" suberror
        final Serializable responseBody = bundleResult.getSerializable(AuthenticationConstants.OAuth2.HTTP_RESPONSE_BODY);
        final TelemetryUtils.CliTelemInfo cliTelemInfo = getCliTelemInfoFromBundle(bundleResult);

        AuthenticationException authenticationException = null;

        if (null != responseBody && responseBody instanceof HashMap) {
            final HashMap<String, String> responseMap = (HashMap<String, String>) responseBody;
            final String error = responseMap.get(AuthenticationConstants.OAuth2.ERROR);
            final String suberror = responseMap.get(AuthenticationConstants.OAuth2.SUBERROR);

            if (!StringExtensions.isNullOrBlank(error) && !StringExtensions.isNullOrBlank(suberror) &&
                    AuthenticationConstants.OAuth2ErrorCode.UNAUTHORIZED_CLIENT.compareTo(error) == 0 &&
                    AuthenticationConstants.OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED.compareTo(suberror) == 0) {

                final String accountUpn = bundleResult.getString(AuthenticationConstants.Broker.ACCOUNT_NAME);
                final String accountUserId = bundleResult.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID);
                final String tenantId = bundleResult.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID);
                final String authorityUrl = bundleResult.getString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY);

                AuthenticationException exception = new IntuneAppProtectionPolicyRequiredException(
                        message,
                        accountUpn,
                        accountUserId,
                        tenantId,
                        authorityUrl
                );

                authenticationException = exception;
            }
        }

        if (null == authenticationException) {
            authenticationException = new AuthenticationException(
                    ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED,
                    message
            );
        }

        authenticationException.setSpeRing(cliTelemInfo.getSpeRing());
        authenticationException.setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
        authenticationException.setCliTelemErrorCode(cliTelemInfo.getServerErrorCode());
        authenticationException.setCliTelemSubErrorCode(cliTelemInfo.getServerSubErrorCode());

        return authenticationException;
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

        SharedPreferencesFileManager prefs = SharedPreferencesFileManager.getSharedPreferences(mContext, KEY_SHARED_PREF_ACCOUNT_LIST, null);
        String accountList = prefs.getString(KEY_APP_ACCOUNTS_FOR_TOKEN_REMOVAL);
        accountList = null != accountList ? accountList : "";
        if (!accountList.contains(KEY_ACCOUNT_LIST_DELIM + accountName)) {
            accountList += KEY_ACCOUNT_LIST_DELIM + accountName;
            prefs.putString(KEY_APP_ACCOUNTS_FOR_TOKEN_REMOVAL, accountList);
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
        final String methodName = ":removeAccountFromAccountManager";
        // getAuthToken call will execute in async as well
        Logger.v(TAG + methodName, "Try to remove account from account manager.");
        Account[] accountList = mAcctManager
                .getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        if (accountList.length != 0) {
            for (Account targetAccount : accountList) {
                Logger.v(TAG + methodName, "Remove tokens for account. ", "Account: " + targetAccount.name, null);

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
    public Intent getIntentForBrokerActivity(final AuthenticationRequest request, final BrokerEvent brokerEvent)
            throws AuthenticationException {
        final String methodName = ":getIntentForBrokerActivity";
        final Bundle requestBundle = getBrokerOptions(request);
        final Intent intent;
        if (isBrokerAccountServiceSupported()) {
            intent = BrokerAccountServiceHandler.getInstance().getIntentForInteractiveRequest(mContext, brokerEvent);
            if (intent == null) {
                Logger.e(TAG, "Received null intent from broker interactive request.", null, ADALError.BROKER_AUTHENTICATOR_NOT_RESPONDING);
                throw new AuthenticationException(ADALError.BROKER_AUTHENTICATOR_NOT_RESPONDING, "Received null intent from broker interactive request.");
            } else {
                intent.putExtras(requestBundle);
            }
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
                Logger.v(TAG + methodName, "FORCE_PROMPT is set for broker auth via old version of broker app, reset to ALWAYS.");
                intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT, PromptBehavior.Always.name());
            }
        }

        return intent;
    }

    private Intent getIntentForBrokerActivityFromAccountManager(final Bundle addAccountOptions) {
        final String methodName = ":getIntentForBrokerActivityFromAccountManager";
        Intent intent = null;
        try {
            // Callback is not passed since it is making a blocking call to get
            // intent. Activity needs to be launched from calling app
            // to get the calling app's metadata if needed at BrokerActivity.
            final AccountManagerFuture<Bundle> result =
                    mAcctManager.addAccount(
                            AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                            AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                            null,
                            addAccountOptions,
                            null,
                            null,
                            mHandler
                    );

            // Making blocking request here
            Bundle bundleResult = result.getResult();
            // Authenticator should throw OperationCanceledException if
            // token is not available
            intent = bundleResult.getParcelable(AccountManager.KEY_INTENT);
            // Add flag to this intent to signal that request is for broker logic

        } catch (OperationCanceledException e) {
            Logger.e(TAG + methodName, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.AUTH_FAILED_CANCELLED, e);
        } catch (AuthenticatorException e) {
            //
            // TODO add retry logic since authenticator is not responding to
            // the request
            Logger.e(TAG + methodName, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.BROKER_AUTHENTICATOR_NOT_RESPONDING, e);
        } catch (IOException e) {
            // Authenticator gets problem from webrequest or file read/write
            Logger.e(TAG + methodName, AUTHENTICATOR_CANCELS_REQUEST, "", ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION, e);
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

        return "VersionName=" + packageInfo.versionName + ";VersonCode=" + PackageInfoCompat.getLongVersionCode(packageInfo) + ".";
    }

    private Bundle getBrokerOptions(final AuthenticationRequest request) {
        Bundle brokerOptions = new Bundle();
        // request needs to be parcelable to send across process
        brokerOptions.putInt(AuthenticationConstants.Browser.REQUEST_ID, request.getRequestId());
        brokerOptions.putInt(AuthenticationConstants.Broker.EXPIRATION_BUFFER, AuthenticationSettings.INSTANCE.getExpirationBuffer());
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

        if (request.isClaimsChallengePresent() || request.getClientCapabilities() != null) {
            brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CLAIMS,
                    AuthenticationContext.mergeClaimsWithClientCapabilities(
                            request.getClaimsChallenge(),
                            request.getClientCapabilities()
                    )
            );
        }

        if (request.getForceRefresh() || request.isClaimsChallengePresent()) {
            // set force refresh to true if claims challenge is present, ad-accounts and adal-unity consumes this parameter
            // to refresh token if claims is present on Request.
            // Note: Even though client capabilities are sent as claims, they should not be treated as claims set to request and
            // token should not be refreshed if they are present.
            brokerOptions.putString(AuthenticationConstants.Broker.BROKER_FORCE_REFRESH, Boolean.toString(true));
        }

        brokerOptions.putString(AuthenticationConstants.AAD.APP_VERSION, request.getAppVersion());

        brokerOptions.putString(AuthenticationConstants.AAD.APP_PACKAGE_NAME, request.getAppName());


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
        final String methodName = ":getCurrentUser";
        // authenticator is not used if there is not any user
        if (isBrokerAccountServiceSupported()) {
            verifyNotOnMainThread();

            final UserInfo[] users;
            try {
                users = BrokerAccountServiceHandler.getInstance().getBrokerUsers(mContext);
            } catch (final IOException e) {
                Logger.e(TAG + methodName, "No current user could be retrieved.", "", null, e);
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
                final BrokerValidator brokerValidator = new BrokerValidator(mContext);
                if (brokerValidator.isValidBrokerPackage(authenticator.packageName) || authenticator.packageName
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
        final String methodName = ":verifyAccount";
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
                Logger.e(TAG + methodName, "Exception thrown when verifying accounts in broker. ", e.getMessage(), ADALError.BROKER_AUTHENTICATOR_EXCEPTION, e);
            }

            Logger.v(TAG + methodName, "It could not check the uniqueid from broker. It is not using broker");
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

    private boolean verifyAuthenticator(final AccountManager am) {
        // there may be multiple authenticators from same package
        // , but there is only one entry for an authenticator type in
        // AccountManager.
        // If another app tries to install same authenticator type, it will
        // queue up and will be active after first one is uninstalled.
        AuthenticatorDescription[] authenticators = am.getAuthenticatorTypes();
        for (AuthenticatorDescription authenticator : authenticators) {
            if (authenticator.type.equals(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE)
                    && mBrokerValidator.verifySignature(authenticator.packageName)) {
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
        final String methodName = ":getUserInfoFromAccountManager";
        final Account[] accountList = mAcctManager.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        final Bundle bundle = new Bundle();
        bundle.putBoolean(DATA_USER_INFO, true);
        Logger.v(TAG + methodName, "Retrieve all the accounts from account manager with broker account type, "
                + "and the account length is: " + accountList.length);

        // accountList will never be null, getAccountsByType will return an empty list if no matching account returned.
        // get info for each user
        final UserInfo[] users = new UserInfo[accountList.length];
        for (int i = 0; i < accountList.length; i++) {

            // Use AccountManager Api method to get extended user info
            final AccountManagerFuture<Bundle> result = mAcctManager.updateCredentials(accountList[i],
                    AuthenticationConstants.Broker.AUTHTOKEN_TYPE, bundle, null, null, null);
            Logger.v(TAG + methodName, "Waiting for userinfo retrieval result from Broker.");
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
