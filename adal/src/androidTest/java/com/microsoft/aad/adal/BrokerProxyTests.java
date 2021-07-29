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
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.broker.PackageHelper;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class BrokerProxyTests {

    static final String TEST_AUTHORITY = "https://login.windows.net/common/";

    private static final String TAG = "BrokerProxyTests";

    public static final String TEST_AUTHORITY_ADFS = "https://fs.ade2eadfs30.com/adfs";

    private byte[] mTestSignature;

    private String mTestTag;

    @Before
    @SuppressLint("PackageManagerGetSignatures")
    public void setUp() throws Exception {
        System.setProperty(
                "dexmaker.dexcache",
                androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext()
                        .getCacheDir()
                        .getPath()
        );

        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        // ADAL is set to this signature for now
        PackageInfo info =
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                        .getContext()
                        .getPackageManager()
                        .getPackageInfo(
                                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                        .getContext().getPackageName(),
                                PackageHelper.getPackageManagerSignaturesFlag()
                        );

        // Broker App can be signed with multiple certificates. It will look
        // all of them
        // until it finds the correct one for ADAL broker.
        for (Signature signature : PackageHelper.getSignatures(info)) {
            mTestSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(mTestSignature);
            mTestTag = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            break;
        }

        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);
        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Logger.d(TAG, "mTestSignature is set");
    }

    @After
    public void tearDown() throws Exception {
        AuthenticationSettings.INSTANCE.setUseBroker(false);
    }

    @Test
    public void testCanSwitchToBrokerInvalidPackage() throws NameNotFoundException {
        final String brokerPackage = "wrong";
        final Signature signature = new Signature(mTestSignature);
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(
                signature, brokerPackage, false));
        context.setMockedAccountManager(getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE, brokerPackage));

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    @Test
    public void testCanSwitchToBrokerInvalidAuthenticatorType() throws NameNotFoundException {
        final String authenticatorType = "invalid";
        final String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        final Signature signature = new Signature(mTestSignature);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(
                signature, brokerPackage, false));
        context.setMockedAccountManager(getMockedAccountManager(authenticatorType, brokerPackage));
        final BrokerProxy brokerProxy = new BrokerProxy(context);

        // assert
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    @Test
    public void testCanSwitchToBrokerInvalidSignature() throws NameNotFoundException {
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        Signature signature = new Signature("74657374696e67");

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(
                signature, brokerPackage, false));
        context.setMockedAccountManager(getMockedAccountManager(authenticatorType, brokerPackage));

        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    @Test
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

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(mockedPackageManager);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY_ADFS));
    }

    @Test
    @Ignore
    public void testCanSwitchToBrokerNoAccountChooserActivity() throws NameNotFoundException {
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        Signature signature = new Signature(mTestSignature);

        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType, brokerPackage);
        final PackageManager mockedPackageManager = getMockedPackageManagerWithBrokerAccountServiceDisabled(signature, brokerPackage, true);
        when(mockedPackageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(Collections.<ResolveInfo>emptyList());

        final Account[] accts = getAccountList("valid", authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(mockedPackageManager);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CAN_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    @Test
    @Ignore
    public void testCanSwitchToBrokerWithAccountChooser() throws NameNotFoundException {
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        Signature signature = new Signature(mTestSignature);

        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType, brokerPackage);
        final PackageManager mockedPackageManager = getMockedPackageManagerWithBrokerAccountServiceDisabled(signature, brokerPackage, true);
        mockIntentActivityQuery(mockedPackageManager);

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(mockedPackageManager);

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        assertEquals(BrokerProxy.SwitchToBroker.CAN_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    @Test
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

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(mockedPackageManager);

        final BrokerProxy brokerProxy = new BrokerProxy(context);
        AuthenticationSettings.INSTANCE.setUseBroker(false);

        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, brokerProxy.canSwitchToBroker(TEST_AUTHORITY));
    }

    @Test
    public void testGetCurrentUser() throws NameNotFoundException {
        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);

        Account[] accts = getAccountList("currentUserName", authenticatorType);
        final AccountManager mockedAccountManager = getMockedAccountManager(authenticatorType, brokerPackage);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final String result = brokerProxy.getCurrentUser();

        // assert
        assertEquals("Username is not equal", "currentUserName", result);
    }

    @Test
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

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
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

    @Test
    @Ignore
    public void testCanSwitchToBrokerMissingBrokerPermission()
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException, NameNotFoundException {

        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String brokerPackage = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        final Signature signature = new Signature(mTestSignature);

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
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

    @Test
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
        when(mockAcctManager.getAccountsByType(Matchers.refEq(authenticatorType))).thenReturn(new Account[]{});
        when(mockContext.getPackageName()).thenReturn(brokerPackage);
        BrokerValidator mockBrokerValidator = mock(BrokerValidator.class);
        when(mockBrokerValidator.verifySignature(brokerPackage)).thenReturn(true);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);
        ReflectionUtils.setFieldValue(brokerProxy, "mBrokerValidator", mockBrokerValidator);
        AuthenticationSettings.INSTANCE.setBrokerPackageName(brokerPackage);
        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker", String.class);
        BrokerProxy.SwitchToBroker result = (BrokerProxy.SwitchToBroker) m.invoke(brokerProxy, TEST_AUTHORITY);

        // assert
        assertEquals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER, result);
    }

    @Test
    public void testGetAuthTokenInBackgroundEmptyAccts() throws NameNotFoundException {
        final AuthenticationRequest request = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", "loginhint", PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(new Account[0]);
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        try {
            brokerProxy.getAuthTokenInBackground(request, null);
        } catch (Exception ex) {
            assertTrue("Exception type check", ex.getCause() instanceof AuthenticationException);
            assertEquals("Check error code", ADALError.BROKER_AUTHENTICATOR_BAD_ARGUMENTS,
                    ((AuthenticationException) ex.getCause()).getCode());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAuthTokenInBackgroundValidAccountEmptyBundle() throws NameNotFoundException, OperationCanceledException,
            AuthenticatorException, IOException, AuthenticationException {

        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", "loginhint", PromptBehavior.Auto, "", UUID.randomUUID(), false);
        final String acctType = "loginhint";


        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
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
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest, null);

        // assert
        assertNull("token should return null", result.getAccessToken());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAuthTokenInBackgroundPositive() throws NameNotFoundException, OperationCanceledException,
            AuthenticatorException, IOException, AuthenticationException {
        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final String acctName = "LoginHint234FDFs";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
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
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest, null);
        assertEquals("token is expected", "token123", result.getAccessToken());
    }

    @SuppressWarnings("unchecked")
    @Test
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

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));
        updateContextToSaveAccount("", acctName);

        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest, null);

        // assert
        assertNotNull("userinfo is expected", result.getUserInfo());
        assertEquals("userid in userinfo is expected", acctName, result.getUserInfo().getUserId());
        assertEquals("givenName in userinfo is expected", "givenName", result.getUserInfo().getGivenName());
        assertEquals("familyName in userinfo is expected", "familyName", result.getUserInfo().getFamilyName());
        assertEquals("idp in userinfo is expected", "idp", result.getUserInfo().getIdentityProvider());
        assertEquals("displayable in userinfo is expected", acctName, result.getUserInfo().getDisplayableId());
    }

    @Test
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

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest, null);

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

    @Test
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
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(authRequest, null);

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

    @Test
    public void testGetAuthTokenInBackgroundVerifyErrorMessageBadArgs() throws NameNotFoundException, OperationCanceledException,
            IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        setMockProxyForErrorCheck(mockedAccountManager, acctName, AccountManager.ERROR_CODE_BAD_ARGUMENTS, "testErrorMessage");
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest, null);
            Assert.fail("should throw");
        } catch (Exception ex) {
            assertTrue("Exception type check", ex instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_BAD_ARGUMENTS,
                    ((AuthenticationException) ex).getCode());
        }
    }

    @SuppressLint("InlinedApi")
    @Test
    public void testGetAuthTokenInBackgroundVerifyErrorMessageBadAuth() throws OperationCanceledException,
            IOException, NameNotFoundException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        setMockProxyForErrorCheck(mockedAccountManager, acctName, AccountManager.ERROR_CODE_BAD_AUTHENTICATION, "testErrorMessage");
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest, null);
            Assert.fail("should throw");
        } catch (Exception ex) {
            assertTrue("Exception type check", ex instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_BAD_AUTHENTICATION,
                    ((AuthenticationException) ex).getCode());
        }
    }

    @Test
    public void testGetAuthTokenInBackgroundVerifyErrorMessageNotSupported() throws NameNotFoundException,
            OperationCanceledException, IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);

        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        setMockProxyForErrorCheck(mockedAccountManager, acctName, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "testErrorMessage");
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest, null);
            Assert.fail("should throw");
        } catch (Exception ex) {
            assertTrue("Exception type check", ex instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_UNSUPPORTED_OPERATION,
                    ((AuthenticationException) ex).getCode());
        }
    }

    @Test
    public void testGetAuthTokenInBackgroundVerifyErrorNoNetworkConnection() throws NameNotFoundException,
            OperationCanceledException, IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
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
            brokerProxy.getAuthTokenInBackground(authRequest, null);
            Assert.fail("should throw");
        } catch (final Exception exception) {
            assertTrue("Exception type check", exception instanceof AuthenticationException);
            assertEquals("check error code", ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE,
                    ((AuthenticationException) exception).getCode());
        }
    }

    @Test
    public void testGetAuthTokenInBackgroundVerifyErrorOAuthErrorNoAccount() throws NameNotFoundException,
            OperationCanceledException, IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);
        authRequest.setSilent(true);
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setConnectionAvailable(false);
        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final Account[] accts = getAccountList(acctName, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);
        final Bundle expected = new Bundle();
        expected.putString(AuthenticationConstants.OAuth2.ERROR, ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED.toString());
        expected.putString(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION, ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED.getDescription());
        final AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);
        updateContextToSaveAccount("", acctName);
        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));
        setMockProxyForErrorCheck(mockedAccountManager, acctName, AccountManager.ERROR_CODE_BAD_REQUEST, ADALError.BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN.getDescription());

        //action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest, null);
            Assert.fail("should throw");
        } catch (final Exception exception) {
            assertTrue("Exception type check", exception instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN,
                    ((AuthenticationException) exception).getCode());
        }
    }

    @Test
    public void testGetAuthTokenInBackgroundVerifyThrowOperationCanceledException() throws NameNotFoundException,
            OperationCanceledException, IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);
        authRequest.setSilent(true);
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setConnectionAvailable(false);
        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);

        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final Account[] accts = getAccountList(acctName, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenThrow(OperationCanceledException.class);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);
        updateContextToSaveAccount("", acctName);

        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        //action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest, null);
            Assert.fail("should throw");
        } catch (final Exception exception) {
            assertTrue("Exception type check", exception instanceof AuthenticationException);
            assertEquals("check error code", ADALError.AUTH_FAILED_CANCELLED,
                    ((AuthenticationException) exception).getCode());
        }
    }

    @Test
    public void testGetAuthTokenInBackgroundVerifyThrowAuthenticatorException() throws NameNotFoundException,
            OperationCanceledException, IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);
        authRequest.setSilent(true);
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setConnectionAvailable(false);
        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);

        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final Account[] accts = getAccountList(acctName, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenThrow(AuthenticatorException.class);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);
        updateContextToSaveAccount("", acctName);

        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        //action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest, null);
            Assert.fail("should throw");
        } catch (final Exception exception) {
            assertTrue("Exception type check", exception instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_ERROR_GETAUTHTOKEN,
                    ((AuthenticationException) exception).getCode());
        }
    }

    @Test
    public void testGetAuthTokenInBackgroundVerifyThrowIOException() throws NameNotFoundException,
            OperationCanceledException, IOException, AuthenticatorException {
        final String acctName = "testAcct123";
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/omercantest", "resource", "client",
                "redirect", acctName.toLowerCase(Locale.US), PromptBehavior.Auto, "", UUID.randomUUID(), false);
        authRequest.setSilent(true);
        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setConnectionAvailable(false);
        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);

        final String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        final Account[] accts = getAccountList(acctName, authenticatorType);
        when(mockedAccountManager.getAccountsByType(anyString())).thenReturn(accts);

        final AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenThrow(IOException.class);
        when(mockedAccountManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class), eq(false),
                (AccountManagerCallback<Bundle>) eq(null), any(Handler.class))).thenReturn(mockFuture);
        updateContextToSaveAccount("", acctName);

        context.setMockedAccountManager(mockedAccountManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        //action
        try {
            final BrokerProxy brokerProxy = new BrokerProxy(context);
            brokerProxy.getAuthTokenInBackground(authRequest, null);
            Assert.fail("should throw");
        } catch (final Exception exception) {
            assertTrue("Exception type check", exception instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_IO_EXCEPTION,
                    ((AuthenticationException) exception).getCode());
        }
    }


    @Test
    public void testGetIntentForBrokerActivityEmptyIntent() throws NameNotFoundException, OperationCanceledException,
            IOException, AuthenticatorException, AuthenticationException {
        final AuthenticationRequest authRequest = createAuthenticationRequest("https://login.windows.net/test", "resource", "client",
                "redirect", "loginhint", PromptBehavior.Auto, "", UUID.randomUUID(), false);
        final AccountManager mockAcctManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Bundle expected = new Bundle();
        prepareAddAccount(mockAcctManager, expected);

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockAcctManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final Intent intent = brokerProxy.getIntentForBrokerActivity(authRequest, null);
        // assert
        assertNull("Intent is null", intent);
    }

    @Test
    public void testGetIntentForBrokerActivityHasIntent() throws NameNotFoundException, OperationCanceledException,
            IOException, AuthenticatorException, AuthenticationException {
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

        final FileMockContext context = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        context.setMockedAccountManager(mockAcctManager);
        context.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, true));

        // action
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        final Intent intent = brokerProxy.getIntentForBrokerActivity(authRequest, null);

        // assert
        assertNotNull("intent is not null", intent);
        assertEquals("intent is not null", AuthenticationConstants.Broker.BROKER_REQUEST,
                intent.getStringExtra(AuthenticationConstants.Broker.BROKER_REQUEST));
    }

    /**
     * Test verifying if always is set when speaking to new broker, intent for doing auth via broker will have prompt behavior
     * reset as always.
     */
    @Test
    public void testForcePromptFlagOldBroker() throws OperationCanceledException, IOException, AuthenticatorException, NameNotFoundException, AuthenticationException {
        final Intent intent = new Intent();
        final AuthenticationRequest authenticationRequest = getAuthRequest(PromptBehavior.FORCE_PROMPT);
        intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT, authenticationRequest.getPrompt().name());

        final AccountManager mockedAccountManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        mockAddAccountResponse(mockedAccountManager, getMockedAccountManagerFuture(intent));

        final FileMockContext mockedContext = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        mockedContext.setMockedAccountManager(mockedAccountManager);
        mockedContext.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, true));
        final BrokerProxy brokerProxy = new BrokerProxy(mockedContext);
        final Intent returnedIntent = brokerProxy.getIntentForBrokerActivity(authenticationRequest, null);
        assertTrue(returnedIntent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT).equalsIgnoreCase(PromptBehavior.Always.name()));
    }

    /**
     * Test verifying if force_prmopt is set when speaking to new broker, intent for doing auth via broker will have prompt behavior
     * as Force_Prompt.
     */
    @Test
    public void testForcePromptNewBroker() throws OperationCanceledException, IOException, AuthenticatorException, NameNotFoundException, AuthenticationException {
        final Intent intent = new Intent();
        final AuthenticationRequest authenticationRequest = getAuthRequest(PromptBehavior.FORCE_PROMPT);
        intent.putExtra(AuthenticationConstants.Broker.BROKER_VERSION, AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION);
        intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT, authenticationRequest.getPrompt().name());

        // mock account manager
        final AccountManager mockedAccoutManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        mockAddAccountResponse(mockedAccoutManager, getMockedAccountManagerFuture(intent));

        final FileMockContext mockedContext = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        mockedContext.setMockedAccountManager(mockedAccoutManager);
        mockedContext.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, true));

        final BrokerProxy brokerProxy = new BrokerProxy(mockedContext);
        final Intent returnedIntent = brokerProxy.getIntentForBrokerActivity(authenticationRequest, null);
        assertTrue(returnedIntent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT).equalsIgnoreCase(PromptBehavior.FORCE_PROMPT.name()));
    }

    /**
     * Test verifying if always is set when speaking to new broker, intent for doing auth via broker will have prompt behavior
     * as always.
     */
    @Test
    public void testPromptAlwaysNewBroker() throws OperationCanceledException, IOException, AuthenticatorException, NameNotFoundException, AuthenticationException {
        final Intent intent = new Intent();
        final AuthenticationRequest authenticationRequest = getAuthRequest(PromptBehavior.Always);
        intent.putExtra(AuthenticationConstants.Broker.BROKER_VERSION, AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION);
        intent.putExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT, authenticationRequest.getPrompt().name());

        // mock account manager
        final AccountManager mockedAccoutManager = getMockedAccountManager(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        mockAddAccountResponse(mockedAccoutManager, getMockedAccountManagerFuture(intent));

        final FileMockContext mockedContext = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
        mockedContext.setMockedAccountManager(mockedAccoutManager);
        mockedContext.setMockedPackageManager(getMockedPackageManagerWithBrokerAccountServiceDisabled(mock(Signature.class),
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, true));

        final BrokerProxy brokerProxy = new BrokerProxy(mockedContext);
        final Intent returnedIntent = brokerProxy.getIntentForBrokerActivity(authenticationRequest, null);
        assertTrue(returnedIntent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT).equalsIgnoreCase(PromptBehavior.Always.name()));
    }

    private FileMockContext getMockedContext(final AccountManager mockedAccountManager) {
        final FileMockContext mockedContext = new FileMockContext(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext());
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
        Mockito.when(
                mockedAccountManager.addAccount(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        ArgumentMatchers.nullable(String[].class),
                        Mockito.any(Bundle.class),
                        ArgumentMatchers.nullable(Activity.class),
                        Mockito.<AccountManagerCallback<Bundle>>any(),
                        Mockito.any(Handler.class)
                )
        ).thenReturn(mockedAccountManagerFuture);
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
                = new AuthenticatorDescription[]{mockedAuthenticator};

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

    private PackageManager getPackageManager(final Signature signature, final String packageName,
                                             boolean permissionStatus) throws NameNotFoundException {
        PackageManager mockPackage = mock(PackageManager.class);
        final PackageInfo mockedPackageInfo = new MockedPackageInfo(new Signature[]{signature});
        when(mockPackage.getPackageInfo(packageName, PackageHelper.getPackageManagerSignaturesFlag())).thenReturn(mockedPackageInfo);
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
                correlationId, isExtendedLifetimeEnabled, null);
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
