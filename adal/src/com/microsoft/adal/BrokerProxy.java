
package com.microsoft.adal;

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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class BrokerProxy implements IBrokerProxy {

    private static final String TAG = "BrokerProxy";

    private static final String NULL_USER = "UnknownUserName";

    private Context mContext;

    public BrokerProxy(final Context ctx) {
        mContext = ctx;
    }

    @Override
    public boolean canSwitchToBroker() {
        final AccountManager am = AccountManager.get(mContext);
        return verifyBroker() && verifyAuthenticator(am);
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

    @Override
    public String getAuthTokenInBackground(final AuthenticationRequest request) {
        final AccountManager am = AccountManager.get(mContext);

        String accessToken = null;
        verifyNotOnMainThread();
        Handler handler = new Handler(mContext.getMainLooper());

        // if there is not any user added to account, it returns empty
        Account[] accountList = am.getAccountsByType(AuthenticationConstants.Broker.ACCOUNT_TYPE);
        Logger.v(TAG, "Account list length:" + accountList.length);
        String accountLookupUsername = getAccountLookupUsername(request);
        Account targetAccount = getAccount(accountList, accountLookupUsername);

        if (targetAccount != null) {
            // add some dummy values to make a test call
            Bundle brokerOptions = getBrokerBlockingOptions(request);

            // blocking call to get token from cache or refresh request in
            // background at Authenticator
            AccountManagerFuture<Bundle> result = null;
            try {
                // It does not expect activity to be launched.
                // AuthenticatorService is handling the request at
                // AccountManager.
                // false notifyAuthFailure: auth failure prompt is not requested
                //
                result = am.getAuthToken(targetAccount,
                        AuthenticationConstants.Broker.AUTHTOKEN_TYPE, brokerOptions, false,
                        null /* set to null to avoid callback */, handler);

                // Making blocking request here
                Bundle bundleResult = result.getResult();
                // Authenticator should throw OperationCanceledException if
                // token is not available
                // TODO add test to broker side
                accessToken = bundleResult
                        .getString(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN);

            } catch (OperationCanceledException e) {
                // TODO verify that authenticator exceptions are recorded in the
                // calling app
                Logger.e(TAG, "Authenticator cancels the request", "",
                        ADALError.AUTH_FAILED_CANCELLED, e);
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

            return accessToken;
        }

        return null;
    }

    @Override
    public Intent getIntentForBrokerActivity(final AuthenticationRequest request) {
        // TODO get intent
        // TODO launch from activity

        return null;
    }

    private Bundle getBrokerBlockingOptions(final AuthenticationRequest request) {
        Bundle brokerOptions = new Bundle();
        brokerOptions.putSerializable(AuthenticationConstants.Broker.BROKER_REQUEST, request);
        return brokerOptions;
    }

    private String getAccountLookupUsername(final AuthenticationRequest request){
        if(!StringExtensions.IsNullOrBlank(request.getLoginHint()))
        {
            //TODO ADAL uses loginhint to cache tokens for user. Cache changes will affect this.
            return request.getLoginHint();
        }
        
        // If idtoken is not present, userid is unknown. Authenticator will group the tokens based on account, so it needs to pass some userid.
        return NULL_USER;
    }
    
    private boolean verifyBroker() {
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(
                    AuthenticationConstants.Broker.PACKAGE_NAME, PackageManager.GET_SIGNATURES);

            // Broker App can be signed with multiple certificates. It will look
            // all of them
            // until it finds the correct one for ADAL broker.
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String tag = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                if (tag.equals(AuthenticationConstants.Broker.SIGNATURE)) {
                    return true;
                }
            }
        } catch (NameNotFoundException e) {
            Logger.e(TAG, "Broker related package does not exist", "",
                    ADALError.BROKER_PACKAGE_NAME_NOT_FOUND);
        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG, "Digest SHA algorithm does not exists", "",
                    ADALError.DEVICE_NO_SUCH_ALGORITHM);
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
            if (authenticator.packageName.equals(AuthenticationConstants.Broker.PACKAGE_NAME)
                    && authenticator.type.equals(AuthenticationConstants.Broker.ACCOUNT_TYPE)) {
                return true;
            }
        }

        return false;
    }

    private Account getAccount(Account[] accounts, String username) {
        if(accounts != null){
            for(Account account : accounts){
                if(account.name.equals(username)){
                    return account;
                }
            }
        }
        
        return null;
    }
}
