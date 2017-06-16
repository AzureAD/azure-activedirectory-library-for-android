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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.UUID;
import java.util.List;

import org.mockito.Matchers;
import org.mockito.Mockito;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;
import android.util.Log;
import junit.framework.Assert;

public class BrokerProxyTests extends AndroidTestCase {

    static final String TEST_AUTHORITY = "https://login.windows.net/common/";

    private static final String TAG = "BrokerProxyTests";

    public static final String TEST_AUTHORITY_ADFS = "https://fs.ade2eadfs30.com/adfs";

    private byte[] mTestSignature;

    private String mTestTag;

    @Override
    @SuppressLint("PackageManagerGetSignatures")
    protected void setUp() throws Exception {
        super.setUp();
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        // ADAL is set to this signature for now
        PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(),
                PackageManager.GET_SIGNATURES);

        // Broker App can be signed with multiple certificates. It will look
        // all of them
        // until it finds the correct one for ADAL broker.
        for (Signature signature : info.signatures) {
            mTestSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(mTestSignature);
            mTestTag = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            break;
        }

        mContext = new FileMockContext(getContext());
        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);
        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Log.d(TAG, "mTestSignature is set");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        AuthenticationSettings.INSTANCE.setUseBroker(false);
    }

    public void testCanSwitchToBrokerInvalidPackage() throws NameNotFoundException {
        final String brokerPackage = "wrong";
        final Signature signature = new Signature(mTestSignature);
        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(
                signature, brokerPackage, false));
        context.setMockedAccountManager(getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE, brokerPackage));

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    public void testCanSwitchToBrokerInvalidAuthenticatorType() throws NameNotFoundException {
        final String authenticatorType = "invalid";
        final String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        final Signature signature = new Signature(mTestSignature);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(
                signature, brokerPackage, false));
        context.setMockedAccountManager(getMockedAccountManager(authenticatorType, brokerPackage));
        final BrokerProxy brokerProxy = new BrokerProxy(context);

        // assert
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    public void testCanSwitchToBrokerInvalidSignature() throws NameNotFoundException {
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        Signature signature = new Signature("74657374696e67");

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(
                signature, brokerPackage, false));
        context.setMockedAccountManager(getMockedAccountManager(authenticatorType, brokerPackage));

        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    public void testCannotSwitchToBrokerWhenADFS()
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException, NameNotFoundException {
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        Signature signature = new Signature(mTestSignature);

        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType, brokerPackage);
        final PackageManager mockedPackageManager = getMockedPackageManagerWithBrokerAccountServiceDisabled(signature, brokerPackage, true);
        when(mockedPackageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.<ResolveInfo>emptyList());

        final Account[] accts = getAccountList("valid", authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(mockedPackageManager);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY_ADFS));
    }

    public void testCanSwitchToBrokerNoAccountChooserActivity() throws NameNotFoundException {
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        Signature signature = new Signature(mTestSignature);

        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType, brokerPackage);
        final PackageManager mockedPackageManager = getMockedPackageManagerWithBrokerAccountServiceDisabled(signature, brokerPackage, true);
        when(mockedPackageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.<ResolveInfo>emptyList());

        final Account[] accts = getAccountList("valid", authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(mockedPackageManager);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CAN_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    public void testCanSwitchToBrokerWithAccountChooser() throws NameNotFoundException {
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        Signature signature = new Signature(mTestSignature);

        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType, brokerPackage);
        final PackageManager mockedPackageManager = getMockedPackageManagerWithBrokerAccountServiceDisabled(signature, brokerPackage, true);
        mockIntentActivityQuery(mockedPackageManager);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(mockedPackageManager);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CAN_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    public void testCanSwitchToBrokerValidSkip()
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException, NameNotFoundException {
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        Signature signature = new Signature(mTestSignature);

        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType, brokerPackage);
        final PackageManager mockedPackageManager = getMockedPackageManagerWithBrokerAccountServiceDisabled(signature, brokerPackage, true);
        when(mockedPackageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(null);

        final Account[] accts = getAccountList("valid", authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(mockedPackageManager);

        final BrokerProxy brokerProxy = new BrokerProxy(context);
        AuthenticationSettings.INSTANCE.setUseBroker(false);

        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    public void testGetCurrentUser() throws NameNotFoundException {
        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);

        Account[] accts = getAccountList("currentUserName", authenticatorType);
        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType, brokerPackage);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final String result = brokerProxy.getCurrentUser();

        // assert
        assertEquals("Username is not equal", "currentUserName", result);
    }

    public void testGetBrokerUsers() throws NameNotFoundException, OperationCanceledException, AuthenticatorException,
            IOException {
        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        final Signature signature = new Signature(mTestSignature);
        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);

        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType, brokerPackage);
        Account[] accounts = getAccountList("currentUserName", authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accounts);

        final AccountManagerFuture<Bundle> mockResult = mock(AccountManagerFuture.class);
        final Bundle testBundle = new Bundle();
        testBundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, "userid");
        testBundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME, "given_name");
        testBundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME, "family_name");
        testBundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER, "idp");
        testBundle.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE, "displayableid_upn");
        when(mockResult.getResult()).thenReturn(testBundle);

        final Bundle bundle = new Bundle();
        bundle.putBoolean("com.microsoft.workaccount.user.info", true);
        when(mockedAccountManager.updateCredentials(eq(accounts[0]), eq(AuthenticationConstants.Broker.AUTHTOKEN_TYPE),
                any(Bundle.class), (Activity) eq(null), (AccountManagerCallback) eq(null), (Handler) eq(null)))
                        .thenReturn(mockResult);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final UserInfo[] result = brokerProxy.getBrokerUsers();

        // assert
        assertNotNull("It returns one user", result);
        assertEquals("displayableid_upn", result[0].getDisplayableId());
    }

    public void testCanSwitchToBrokerMissingBrokerPermission()
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException, NameNotFoundException {

        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        final Signature signature = new Signature(mTestSignature);

        final FileMockContext context = new FileMockContext(getContext());
        final PackageManager mockedPackageManager = getMockedPackageManagerWithBrokerAccountServiceDisabled(
                signature, brokerPackage, false);
        mockIntentActivityQuery(mockedPackageManager);
        context.setMockedPackageManager(mockedPackageManager);

        context.setMockedAccountManager(getMockedAccountManager(authenticatorType, brokerPackage));

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final BrokerProxy brokerProxy = new BrokerProxy(context);

        // assert
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertEquals(BrokerProxy.SwitchToBroker.NEED_PERMISSIONS_TO_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
        }
    }

    public void testCanSwitchToBrokerValidBrokerAuthenticatorInternalCall()
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException, NameNotFoundException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        Signature signature = new Signature(mTestSignature);
        AccountManager mockAcctManager = mock(AccountManager.class);
        AuthenticatorDescription[] descriptions = getAuthenticator(authenticatorType, brokerPackage);
        Context mockContext = getMockContext(signature, brokerPackage, brokerPackage, true);
        when(mockAcctManager.getAuthenticatorTypes()).thenReturn(descriptions);
        when(mockAcctManager.getAccountsByType(Matchers.refEq(authenticatorType))).thenReturn(new Account[] {});
        when(mockContext.getPackageName()).thenReturn(brokerPackage);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);
        AuthenticationSettings.INSTANCE.setBrokerPackageName(brokerPackage);
        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);
        ReflectionUtils.setFieldValue(brokerProxy, "mBrokerTag", mTestTag);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker", String.class);
        BrokerProxy.SwitchToBroker result = (BrokerProxy.SwitchToBroker) m.invoke(brokerProxy, TEST_AUTHORITY);

        // assert
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, result);
    }

    public void testGetAuthTokenInBackgroundEmptyAccts() throws NameNotFoundException {
        final AuthenticationRequest request = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", "loginhint", PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final FileMockContext context = new FileMockContext(getContext());
        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(new Account[0]);
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        try {
            brokerProxy.getAuthTokenInBackground(request);
        } catch (Exception ex) {
            assertTrue("Exception type check", ex.getCause() instanceof AuthenticationException);
            assertEquals("Check error code", ADALError.BROKER_AUTHENTICATOR_BAD_ARGUMENTS,
                    ((AuthenticationException) ex.getCause()).getCode());
        }
    }

    @SuppressWarnings("unchecked")
    public void testGetAuthTokenInBackgroundValidAccountEmptyBundle() throws NameNotFoundException, OperationCanceledException,
            AuthenticatorException, IOException, AuthenticationException {

        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", "loginhint", PromptBehavior.Auto, "", UUID.randomUUID(), false);
        final String acctType = "loginhint";


        final FileMockContext context = new FileMockContext(getContext());
        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);

        final Account[] accts = getAccountList(acctType, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final Bundle expected = new Bundle();
        AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);

        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        updateContextToSaveAccount("", "test");
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest);

        // assert
        assertNull("token should return null", result.getAccessToken());
    }

    @SuppressWarnings("unchecked")
    public void testGetAuthTokenInBackgroundPositive() throws NameNotFoundException, OperationCanceledException,
            AuthenticatorException, IOException, AuthenticationException {
        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String acctName = "LoginHint234FDFs";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final FileMockContext context = new FileMockContext(getContext());
        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);

        final Account[] accts = getAccountList(acctName, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final Bundle expected = new Bundle();
        expected.putString(AccountManager.KEY_AUTHTOKEN, "token123");
        AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);

        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // assert
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest);
        assertEquals("token is expected", "token123", result.getAccessToken());
    }

    @SuppressWarnings("unchecked")
    public void testGetAuthTokenInBackgroundVerifyUserInfo() throws NameNotFoundException,
            OperationCanceledException, IOException, AuthenticationException, AuthenticatorException {

        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        // check case sensitivity for account name
        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        final Account[] accounts = getAccountList(acctName, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accounts);

        final Bundle expected = new Bundle();
        expected.putString(AccountManager.KEY_AUTHTOKEN, "token123");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, acctName);
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME, "givenName");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME, "familyName");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER, "idp");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE, acctName);
        final AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));
        updateContextToSaveAccount("", acctName);

        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest);

        // assert
        assertNotNull("userinfo is expected", result.getUserInfo());
        assertEquals("userid in userinfo is expected", acctName, result.getUserInfo().getUserId());
        assertEquals("givenName in userinfo is expected", "givenName", result.getUserInfo().getGivenName());
        assertEquals("familyName in userinfo is expected", "familyName", result.getUserInfo().getFamilyName());
        assertEquals("idp in userinfo is expected", "idp", result.getUserInfo().getIdentityProvider());
        assertEquals("displayable in userinfo is expected", acctName, result.getUserInfo().getDisplayableId());
    }

    public void testGetAuthTokenInBackgroundVerifyAuthenticationResult()
            throws NameNotFoundException, OperationCanceledException,
            IOException, NoSuchFieldException, AuthenticatorException, AuthenticationException {
        final int tokenExpiresDate = 1000;

        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/authtest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        final Account[] accts = getAccountList(acctName, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final Bundle expected = new Bundle();
        expected.putString(AccountManager.KEY_AUTHTOKEN, "token123");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID, "testTenant");
        expected.putLong(AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE, tokenExpiresDate);
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, acctName);
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME, "givenName");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME, "familyName");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER, "idp");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE, acctName);

        final AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest);

        // Make sure what returned from broker is consistent with what returned
        // from adal
        assertEquals("tenant id is expected", "testTenant", result.getTenantId());
        assertEquals("token expires is expected", new Date(tokenExpiresDate), result.getExpiresOn());
        assertNotNull("userinfo is expected", result.getUserInfo());
        assertEquals("userid in userinfo is expected", acctName, result.getUserInfo().getUserId());
        assertEquals("givenName in userinfo is expected", "givenName", result.getUserInfo().getGivenName());
        assertEquals("familyName in userinfo is expected", "familyName", result.getUserInfo().getFamilyName());
        assertEquals("idp in userinfo is expected", "idp", result.getUserInfo().getIdentityProvider());
        assertEquals("displayable in userinfo is expected", acctName, result.getUserInfo().getDisplayableId());
    }

    public void testGetAuthTokenInBackgroundVerifyAuthenticationResultNotReturnExpires() throws NameNotFoundException,
            AuthenticationException, OperationCanceledException, IOException, NoSuchFieldException, AuthenticatorException {
        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/authtest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        final Account[] accts = getAccountList(acctName, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final Bundle expected = new Bundle();
        expected.putString(AccountManager.KEY_AUTHTOKEN, "token123");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID, "testTenant");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, acctName);
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME, "givenName");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME, "familyName");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER, "idp");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE, acctName);

        final AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);

        updateContextToSaveAccount("", acctName);
        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest);

        final Calendar expires = new GregorianCalendar();
        expires.add(Calendar.SECOND, AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC);

        // Make sure what returned from broker is consistent with what returned
        // from adal
        assertEquals("tenant id is expected", "testTenant", result.getTenantId());
        assertEquals("token expires is expected", expires.getTime().getDate(), result.getExpiresOn().getDate());
        assertNotNull("userinfo is expected", result.getUserInfo());
        assertEquals("userid in userinfo is expected", acctName, result.getUserInfo().getUserId());
        assertEquals("givenName in userinfo is expected", "givenName", result.getUserInfo().getGivenName());
        assertEquals("familyName in userinfo is expected", "familyName", result.getUserInfo().getFamilyName());
        assertEquals("idp in userinfo is expected", "idp", result.getUserInfo().getIdentityProvider());
        assertEquals("displayable in userinfo is expected", acctName, result.getUserInfo().getDisplayableId());
    }

    private void setMockProxyForErrorCheck(final AccountManager mockedAccountManager, String acctName, int errCode, String msg)
            throws OperationCanceledException,
            IOException, AuthenticatorException {
        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final Account[] accts = getAccountList(acctName, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final Bundle expected = new Bundle();
        expected.putInt(AccountManager.KEY_ERROR_CODE, errCode);
        expected.putString(AccountManager.KEY_ERROR_MESSAGE, msg);
        final AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);
        updateContextToSaveAccount("", acctName);
    }

    public void testGetAuthTokenInBackgroundVerifyErrorMessageBadArgs() throws NameNotFoundException, OperationCanceledException,
            IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        setMockProxyForErrorCheck(mockedAccountManager, acctName, AccountManager.ERROR_CODE_BAD_ARGUMENTS, "testErrorMessage");
        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest);
            Assert.fail("should throw");
        } catch (Exception ex) {
            assertTrue("Exception type check", ex instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_BAD_ARGUMENTS,
                    ((AuthenticationException) ex).getCode());
        }
    }

    @SuppressLint("InlinedApi")
    public void testGetAuthTokenInBackgroundVerifyErrorMessageBadAuth() throws OperationCanceledException,
            IOException, NameNotFoundException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        setMockProxyForErrorCheck(mockedAccountManager, acctName, AccountManager.ERROR_CODE_BAD_AUTHENTICATION, "testErrorMessage");
        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest);
            Assert.fail("should throw");
        } catch (Exception ex) {
            assertTrue("Exception type check", ex instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_BAD_AUTHENTICATION,
                    ((AuthenticationException) ex).getCode());
        }
    }

    public void testGetAuthTokenInBackgroundVerifyErrorMessageNotSupported() throws NameNotFoundException,
            OperationCanceledException, IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        setMockProxyForErrorCheck(mockedAccountManager, acctName, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "testErrorMessage");
        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest);
            Assert.fail("should throw");
        } catch (Exception ex) {
            assertTrue("Exception type check", ex instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_UNSUPPORTED_OPERATION,
                    ((AuthenticationException) ex).getCode());
        }
    }

    public void testGetAuthTokenInBackgroundVerifyErrorNoNetworkConnection() throws NameNotFoundException,
    OperationCanceledException, IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);
        final FileMockContext context = new FileMockContext(getContext());
        context.setConnectionAvailable(false);
        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        setMockProxyForErrorCheck(mockedAccountManager, acctName, AccountManager.ERROR_CODE_NETWORK_ERROR, ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE.getDescription());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        //action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest);
            Assert.fail("should throw");
        } catch (final Exception exception) {
            assertTrue("Exception type check", exception instanceof AuthenticationException);
            assertEquals("check error code", ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE,
                    ((AuthenticationException) exception).getCode());
        }
    }

    public void testGetIntentForBrokerActivityEmptyIntent() throws NameNotFoundException, OperationCanceledException,
            IOException, AuthenticatorException {
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/test", "resource", "client",
                "redirect", "loginhint", PromptBehavior.Auto, "", UUID.randomUUID(), false);
        final AccountManager mockAcctManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Bundle expected = new Bundle();
        prepareAddAccount(mockAcctManager, expected);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockAcctManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final Intent intent = brokerProxy.getIntentForBrokerActivity(authRequest);

        // assert
        assertNull("Intent is null", intent);
    }

    public void testGetIntentForBrokerActivityHasIntent() throws NameNotFoundException, OperationCanceledException,
            IOException, AuthenticatorException {
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", "loginhint", PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final AccountManager mockAcctManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        final Bundle expected = new Bundle();
        final Intent expectedIntent = new Intent();
        expectedIntent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST,
                AuthenticationConstants.Broker.BROKER_REQUEST);
        expected.putParcelable(AccountManager.KEY_INTENT, expectedIntent);
        prepareAddAccount(mockAcctManager, expected);

        final FileMockContext context = new FileMockContext(getContext());
        context.setMockedAccountManager(mockAcctManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final Intent intent = brokerProxy.getIntentForBrokerActivity(authRequest);

        // assert
        assertNotNull("intent is not null", intent);
        assertEquals("intent is not null", AuthenticationConstants.Broker.BROKER_REQUEST,
                intent.getStringExtra(AuthenticationConstants.Broker.BROKER_REQUEST));
    }
    
    /**
     * Test verifying if always is set when speaking to new broker, intent for doing auth via broker will have prompt behavior
     * reset as always. 
     */
    @SmallTest
    public void testForcePromptFlagOldBroker() throws OperationCanceledException, IOException, AuthenticatorException, NameNotFoundException {
        final Intent intent = new Intent();
        final AuthenticationRequest authenticationRequest = getAuthRequest(PromptBehavior.FORCE_PROMPT);
        intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT, authenticationRequest.getPrompt().name());

        final AccountManager mockedAccoutManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        mockAddAccountResponse(mockedAccoutManager, getMockedAccountManagerFuture(intent));
        
        final FileMockContext mockedContext = new FileMockContext(getContext());
        mockedContext.setMockedAccountManager(mockedAccoutManager);
        mockedContext.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, true));
        final BrokerProxy brokerProxy = new BrokerProxy(mockedContext);
        final Intent returnedIntent = brokerProxy.getIntentForBrokerActivity(authenticationRequest);
        assertTrue(returnedIntent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT).equalsIgnoreCase(PromptBehavior.Always.name()));
    }
    
    /**
     * Test verifying if force_prmopt is set when speaking to new broker, intent for doing auth via broker will have prompt behavior
     * as Force_Prompt. 
     */
    @SmallTest
    public void testForcePromptNewBroker() throws OperationCanceledException, IOException, AuthenticatorException, NameNotFoundException {
        final Intent intent = new Intent();
        final AuthenticationRequest authenticationRequest = getAuthRequest(PromptBehavior.FORCE_PROMPT);
        intent.putExtra(AuthenticationConstants.Broker.BROKER_VERSION, AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION);
        intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT, authenticationRequest.getPrompt().name());
        
        // mock account manager
        final AccountManager mockedAccoutManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        mockAddAccountResponse(mockedAccoutManager, getMockedAccountManagerFuture(intent));

        final FileMockContext mockedContext = new FileMockContext(getContext());
        mockedContext.setMockedAccountManager(mockedAccoutManager);
        mockedContext.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, true));

        final BrokerProxy brokerProxy = new BrokerProxy(mockedContext);
        final Intent returnedIntent = brokerProxy.getIntentForBrokerActivity(authenticationRequest);
        assertTrue(returnedIntent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT).equalsIgnoreCase(PromptBehavior.FORCE_PROMPT.name()));
    }
    
    /**
     * Test verifying if always is set when speaking to new broker, intent for doing auth via broker will have prompt behavior
     * as always. 
     */
    @SmallTest
    public void testPromptAlwaysNewBroker() throws OperationCanceledException, IOException, AuthenticatorException, NameNotFoundException {
        final Intent intent = new Intent();
        final AuthenticationRequest authenticationRequest = getAuthRequest(PromptBehavior.Always);
        intent.putExtra(AuthenticationConstants.Broker.BROKER_VERSION, AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION);
        intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT, authenticationRequest.getPrompt().name());
        
        // mock account manager
        final AccountManager mockedAccoutManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        mockAddAccountResponse(mockedAccoutManager, getMockedAccountManagerFuture(intent));

        final FileMockContext mockedContext = new FileMockContext(getContext());
        mockedContext.setMockedAccountManager(mockedAccoutManager);
        mockedContext.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, true));

        final BrokerProxy brokerProxy = new BrokerProxy(mockedContext);
        final Intent returnedIntent = brokerProxy.getIntentForBrokerActivity(authenticationRequest);

        assertTrue(returnedIntent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT).equalsIgnoreCase(PromptBehavior.Always.name()));
    }
    
    private FileMockContext getMockedContext(final AccountManager mockedAccountManager) {
        final FileMockContext mockedContext = new FileMockContext(getContext());
        mockedContext.setMockedAccountManager(mockedAccountManager);
        
        return mockedContext;
    }
    
    private AuthenticationRequest getAuthRequest(final PromptBehavior promptBehavior) {
        final AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setPrompt(promptBehavior);
        
        return authenticationRequest;
    }
    
    private AccountManagerFuture<Bundle> getMockedAccountManagerFuture(final Intent intent)
            throws OperationCanceledException, IOException, AuthenticatorException {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        
        final AccountManagerFuture<Bundle> mockedAccountManagerFuture = Mockito.mock(AccountManagerFuture.class);
        Mockito.when(mockedAccountManagerFuture.getResult()).thenReturn(bundle);
        
        return mockedAccountManagerFuture;
    }
    
    private AccountManager getMockedAccountManager(final AccountManagerFuture<Bundle> mockedAccountManagerFuture) {
        final AccountManager mockedAccoutManager = Mockito.mock(AccountManager.class);
        Mockito.when(mockedAccoutManager.addAccount(Matchers.anyString(), Mockito.anyString(), Matchers.any(String[].class), 
                Matchers.any(Bundle.class), Matchers.any(Activity.class), Matchers.any(AccountManagerCallback.class), 
                Mockito.any(Handler.class))).thenReturn(mockedAccountManagerFuture);
        
        return mockedAccoutManager;
    }

    private void mockAddAccountResponse(final AccountManager mockedAccountManager,
                                        final AccountManagerFuture<Bundle> mockedAccountManagerFuture) {
        Mockito.when(mockedAccountManager.addAccount(Matchers.anyString(), Mockito.anyString(), Matchers.any(String[].class),
                Matchers.any(Bundle.class), Matchers.any(Activity.class), Matchers.any(AccountManagerCallback.class),
                Mockito.any(Handler.class))).thenReturn(mockedAccountManagerFuture);

    }

    @SuppressLint("CommitPrefEdits")
    private void updateContextToSaveAccount(String initialList, String savingAccount) {
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getString(anyString(), Mockito.eq(""))).thenReturn(initialList);
        Editor mockEditor = mock(Editor.class);
        when(mockPrefs.edit()).thenReturn(mockEditor);
    }

    @SuppressWarnings("unchecked")
    private void prepareAddAccount(final AccountManager mockAcctManager, final Bundle expected)
            throws OperationCanceledException, IOException, AuthenticatorException {
        final AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockAcctManager.addAccount(anyString(), anyString(), (String[]) eq(null), any(Bundle.class),
                (Activity) eq(null), (AccountManagerCallback<Bundle>) eq(null), any(Handler.class)))
                        .thenReturn(mockFuture);
        when(mockAcctManager.getAccountsByType(anyString()))
                .thenReturn(getAccountList("test", AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE));
    }

    private Account[] getAccountList(String accountname, String authenticatorType) {
        Account account = new Account(accountname, authenticatorType);
        Account[] accts = new Account[1];
        accts[0] = account;
        return accts;
    }

    private AccountManager getMockedAccountManager(final String authenticatorType, final String brokerPackage) {
        final AccountManager mockAcctManager = Mockito.mock(AccountManager.class);
        final AuthenticatorDescription authenticatorDescription
                = new AuthenticatorDescription(authenticatorType,
                brokerPackage, 0, 0, 0, 0);
        final AuthenticatorDescription mockedAuthenticator = Mockito.spy(authenticatorDescription);
        final AuthenticatorDescription[] mockedAuthenticatorTypes
                = new AuthenticatorDescription[] {mockedAuthenticator};

        Mockito.when(mockAcctManager.getAuthenticatorTypes()).thenReturn(mockedAuthenticatorTypes);

        return mockAcctManager;
    }

    private Context getMockContext(final Signature signature, final String brokerPackageName,
            final String contextPackageName, boolean permissionStatus) throws NameNotFoundException {
        Context mockContext = mock(Context.class);
        // insert packagemanager mocks
        PackageManager mockPackageManager = getPackageManager(signature, brokerPackageName, permissionStatus);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        when(mockContext.getPackageName()).thenReturn(contextPackageName);
        List<ResolveInfo> mockList = new ArrayList<>();
        mockList.add(new ResolveInfo());
        when(mockPackageManager.queryIntentActivities(Matchers.any(Intent.class), anyInt())).thenReturn(mockList);
        return mockContext;
    }

    @SuppressLint("PackageManagerGetSignatures")
    private PackageManager getPackageManager(final Signature signature, final String packageName,
            boolean permissionStatus) throws NameNotFoundException {
        PackageManager mockPackage = mock(PackageManager.class);
        PackageInfo info = new PackageInfo();
        Signature[] signatures = new Signature[1];
        signatures[0] = signature;
        info.signatures = signatures;
        when(mockPackage.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)).thenReturn(info);
        when(mockPackage.checkPermission(anyString(), anyString()))
                .thenReturn(permissionStatus ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED);
        return mockPackage;
    }

    private AuthenticatorDescription[] getAuthenticator(final String authenticatorType, final String packagename) {
        AuthenticatorDescription[] items = new AuthenticatorDescription[1];
        items[0] = new AuthenticatorDescription(authenticatorType, packagename, 0, 0, 0, 0, true);

        return items;
    }

    private static AuthenticationRequest createAuthenticationRequest(final String authority, final String resource, final String client,
                                                                     final String redirect, final String loginHint, PromptBehavior prompt,
                                                                     final String extraQueryParams, final UUID correlationId,
                                                                     boolean isExtendedLifetimeEnabled) {
        return new AuthenticationRequest(authority, resource, client, redirect, loginHint, prompt, extraQueryParams,
                correlationId, isExtendedLifetimeEnabled);
    }

    private PackageManager getMockedPackageManagerWithBrokerAccountServiceDisabled(final Signature signature,
                                                                                   final String brokerPackageName,
                                                                                   boolean permission) throws NameNotFoundException {
        final PackageManager mockedPackageManager = getPackageManager(signature, brokerPackageName, permission);
        when(mockedPackageManager.queryIntentServices(Mockito.any(Intent.class), Mockito.anyInt())).thenReturn(null);

        return mockedPackageManager;
    }

    private void mockIntentActivityQuery(final PackageManager mockedPackageManager) {
        final List<ResolveInfo> activities = new ArrayList<>(1);
        activities.add(Mockito.mock(ResolveInfo.class));
        when(mockedPackageManager.queryIntentActivities(Mockito.any(Intent.class), Mockito.anyInt()))
                .thenReturn(activities);
    }
}
