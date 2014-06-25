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

package com.microsoft.aad.adal.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

import junit.framework.Assert;

import org.mockito.Mockito;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.PromptBehavior;

public class BrokerProxyTests extends AndroidTestCase {

    private static final String TAG = "BrokerProxyTests";

    private byte[] testSignature;

    private String testTag;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        // ADAL is set to this signature for now
        PackageInfo info = mContext.getPackageManager().getPackageInfo(
                "com.microsoft.aad.adal.testapp", PackageManager.GET_SIGNATURES);

        // Broker App can be signed with multiple certificates. It will look
        // all of them
        // until it finds the correct one for ADAL broker.
        for (Signature signature : info.signatures) {
            testSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(testSignature);
            testTag = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            break;
        }
        AuthenticationSettings.INSTANCE.setBrokerSignature(testTag);
        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.PACKAGE_NAME);
        Log.d(TAG, "testSignature is set");
    }

    public void testCanSwitchToBroker_InvalidPackage() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException, NoSuchAlgorithmException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = "wrong";
        Signature signature = new Signature(testSignature);
        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, "test", signature, true,
                null);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        // assert
        assertFalse("verify should return false", result);
    }

    public void testCanSwitchToBroker_InvalidAuthenticatorType() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = "invalid";
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;
        Signature signature = new Signature(testSignature);
        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, "test", signature, true,
                null);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        // assert
        assertFalse("verify should return false", result);
    }

    public void testCanSwitchToBroker_InvalidSignature() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;
        Signature signature = new Signature("74657374696e67");
        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, "test", signature, true,
                null);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        // assert
        assertFalse("verify should return false", result);
    }

    public void testCanSwitchToBroker_Valid() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;
        String contextPackage = "com.test";
        Signature signature = new Signature(testSignature);
        AuthenticationSettings.INSTANCE.setBrokerSignature(testTag);
        AuthenticationSettings.INSTANCE.setSkipBroker(false);
        Account[] accts = getAccountList("valid", authenticatorType);
        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, contextPackage,
                signature, true, accts);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        // assert
        assertTrue("verify should return true", result);
    }

    public void testCanSwitchToBrokerValid_Skip() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException {
        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;
        String contextPackage = "com.test";
        Signature signature = new Signature(testSignature);
        AuthenticationSettings.INSTANCE.setBrokerSignature(testTag);
        Account[] accts = getAccountList("valid", authenticatorType);
        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, contextPackage,
                signature, true, accts);
        AuthenticationSettings.INSTANCE.setSkipBroker(true);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        // assert
        assertFalse("This should skip broker", result);
    }

    public void testGetCurrentUser() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, NameNotFoundException {
        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;
        String contextPackage = "com.test";
        Signature signature = new Signature(testSignature);
        AuthenticationSettings.INSTANCE.setBrokerSignature(testTag);
        Account[] accts = getAccountList("currentUserName", authenticatorType);
        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, contextPackage,
                signature, true, accts);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getCurrentUser");
        String result = (String)m.invoke(brokerProxy);

        // assert
        assertEquals("Username is not equal", "currentUserName", result);
    }

    public void testCanSwitchToBroker_MissingBrokerPermission() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;
        String contextPackage = "com.test";
        Signature signature = new Signature(testSignature);
        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, contextPackage,
                signature, false, null);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        // assert
        assertFalse("missing permissions in the manifest", result);
    }

    public void testCanSwitchToBroker_ValidBroker_AuthenticatorInternalCall()
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchFieldException, NameNotFoundException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;
        Signature signature = new Signature(testSignature);
        AccountManager mockAcctManager = mock(AccountManager.class);
        AuthenticatorDescription[] descriptions = getAuthenticator(authenticatorType, brokerPackage);
        Context mockContext = getMockContext(signature, brokerPackage, brokerPackage, true);
        when(mockAcctManager.getAuthenticatorTypes()).thenReturn(descriptions);
        when(mockContext.getPackageName()).thenReturn(brokerPackage);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);
        AuthenticationSettings.INSTANCE.setBrokerPackageName(brokerPackage);
        AuthenticationSettings.INSTANCE.setBrokerSignature(testTag);
        ReflectionUtils.setFieldValue(brokerProxy, "mBrokerTag", testTag);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        // assert
        assertFalse(
                "It should not try to call Ad-Authenticator again for internal call from Ad-Authenticator",
                result);
    }

    public void testGetAuthTokenInBackground_emptyAccts() throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, NoSuchFieldException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", "loginhint", PromptBehavior.Auto, "",
                UUID.randomUUID());
        AccountManager mockAcctManager = mock(AccountManager.class);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(new Account[0]);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(null);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                authRequest.getClass());

        try {
            m.invoke(brokerProxy, authRequest);
        } catch (Exception ex) {
            assertTrue("Exception type check", ex.getCause() instanceof AuthenticationException);
            assertEquals("Check error code", ADALError.BROKER_AUTHENTICATOR_BAD_ARGUMENTS,
                    ((AuthenticationException)ex.getCause()).getCode());
        }
    }

    @SuppressWarnings("unchecked")
    public void testGetAuthTokenInBackground_ValidAccount_EmptyBundle()
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            OperationCanceledException, AuthenticatorException, IOException, NoSuchFieldException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", "loginhint", PromptBehavior.Auto, "",
                UUID.randomUUID());
        String acctType = "loginhint";
        Account[] accts = getAccountList(acctType, authenticatorType);
        AccountManager mockAcctManager = mock(AccountManager.class);
        Bundle expected = new Bundle();
        AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(accts);
        when(
                mockAcctManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class),
                        eq(false), (AccountManagerCallback<Bundle>)eq(null), any(Handler.class)))
                .thenReturn(mockFuture);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(null);
        updateContextToSaveAccount(mockContext, "", "test");
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                authRequest.getClass());
        AuthenticationResult result = (AuthenticationResult)m.invoke(brokerProxy, authRequest);

        // assert
        assertNull("token should return null", result.getAccessToken());
    }

    @SuppressWarnings("unchecked")
    public void testGetAuthTokenInBackground_Positive() throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, OperationCanceledException,
            AuthenticatorException, IOException, NoSuchFieldException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String acctName = "LoginHint234FDFs";
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", acctName.toLowerCase(), PromptBehavior.Auto, "",
                UUID.randomUUID());
        // check case sensitivity for account name
        Account[] accts = getAccountList(acctName, authenticatorType);
        AccountManager mockAcctManager = mock(AccountManager.class);
        Bundle expected = new Bundle();
        expected.putString(AccountManager.KEY_AUTHTOKEN, "token123");
        AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(accts);
        when(
                mockAcctManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class),
                        eq(false), (AccountManagerCallback<Bundle>)eq(null), any(Handler.class)))
                .thenReturn(mockFuture);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(null);
        updateContextToSaveAccount(mockContext, "", acctName);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                authRequest.getClass());
        AuthenticationResult result = (AuthenticationResult)m.invoke(brokerProxy, authRequest);

        // assert
        assertEquals("token is expected", "token123", result.getAccessToken());
    }

    @SuppressWarnings("unchecked")
    public void testGetAuthTokenInBackground_VerifyUserInfo() throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, OperationCanceledException,
            AuthenticatorException, IOException, NoSuchFieldException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        String acctName = "testAcct123";
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", acctName.toLowerCase(Locale.US),
                PromptBehavior.Auto, "", UUID.randomUUID());
        // check case sensitivity for account name
        Account[] accts = getAccountList(acctName, authenticatorType);
        AccountManager mockAcctManager = mock(AccountManager.class);
        Bundle expected = new Bundle();
        expected.putString(AccountManager.KEY_AUTHTOKEN, "token123");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, acctName);
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME, "givenName");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME,
                "familyName");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_IDENTITY_PROVIDER, "idp");
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE,
                acctName);
        AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(accts);
        when(
                mockAcctManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class),
                        eq(false), (AccountManagerCallback<Bundle>)eq(null), any(Handler.class)))
                .thenReturn(mockFuture);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(null);
        updateContextToSaveAccount(mockContext, "", acctName);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                authRequest.getClass());
        AuthenticationResult result = (AuthenticationResult)m.invoke(brokerProxy, authRequest);

        // assert
        assertNotNull("userinfo is expected", result.getUserInfo());
        assertEquals("userid in userinfo is expected", acctName, result.getUserInfo().getUserId());
        assertEquals("givenName in userinfo is expected", "givenName", result.getUserInfo()
                .getGivenName());
        assertEquals("familyName in userinfo is expected", "familyName", result.getUserInfo()
                .getFamilyName());
        assertEquals("idp in userinfo is expected", "idp", result.getUserInfo()
                .getIdentityProvider());
        assertEquals("displayable in userinfo is expected", acctName, result.getUserInfo()
                .getDisplayableId());
    }

    private void setMockProxyForErrorCheck(Object brokerProxy, String acctName, int errCode,
            String msg) throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException, OperationCanceledException, AuthenticatorException, IOException {
        String authenticatorType = AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;
        Account[] accts = getAccountList(acctName, authenticatorType);
        AccountManager mockAcctManager = mock(AccountManager.class);
        Bundle expected = new Bundle();
        expected.putInt(AccountManager.KEY_ERROR_CODE, errCode);
        expected.putString(AccountManager.KEY_ERROR_MESSAGE, msg);
        AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(accts);
        when(
                mockAcctManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class),
                        eq(false), (AccountManagerCallback<Bundle>)eq(null), any(Handler.class)))
                .thenReturn(mockFuture);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(null);
        updateContextToSaveAccount(mockContext, "", acctName);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);
    }

    public void testGetAuthTokenInBackground_VerifyErrorMessage_Bad_Args()
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            OperationCanceledException, AuthenticatorException, IOException, NoSuchFieldException {
        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String acctName = "testAcct123";
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", acctName.toLowerCase(Locale.US),
                PromptBehavior.Auto, "", UUID.randomUUID());
        setMockProxyForErrorCheck(brokerProxy, acctName, AccountManager.ERROR_CODE_BAD_ARGUMENTS,
                "testErrorMessage");

        // action
        try {
            Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                    authRequest.getClass());
            AuthenticationResult result = (AuthenticationResult)m.invoke(brokerProxy, authRequest);
            Assert.fail("should throw");
        } catch (Exception ex) {
            assertTrue("Exception type check", ex.getCause() instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_BAD_ARGUMENTS,
                    ((AuthenticationException)ex.getCause()).getCode());
        }
    }

    public void testGetAuthTokenInBackground_VerifyErrorMessage_Bad_Auth()
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            OperationCanceledException, AuthenticatorException, IOException, NoSuchFieldException {
        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String acctName = "testAcct123";
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", acctName.toLowerCase(Locale.US),
                PromptBehavior.Auto, "", UUID.randomUUID());
        setMockProxyForErrorCheck(brokerProxy, acctName,
                AccountManager.ERROR_CODE_BAD_AUTHENTICATION, "testErrorMessage");

        // action
        try {
            Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                    authRequest.getClass());
            AuthenticationResult result = (AuthenticationResult)m.invoke(brokerProxy, authRequest);
            Assert.fail("should throw");
        } catch (Exception ex) {
            assertTrue("Exception type check", ex.getCause() instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_BAD_AUTHENTICATION,
                    ((AuthenticationException)ex.getCause()).getCode());
        }
    }

    public void testGetAuthTokenInBackground_VerifyErrorMessage_Not_Supported()
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            OperationCanceledException, AuthenticatorException, IOException, NoSuchFieldException {
        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        String acctName = "testAcct123";
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", acctName.toLowerCase(Locale.US),
                PromptBehavior.Auto, "", UUID.randomUUID());
        setMockProxyForErrorCheck(brokerProxy, acctName,
                AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "testErrorMessage");

        // action
        try {
            Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                    authRequest.getClass());
            AuthenticationResult result = (AuthenticationResult)m.invoke(brokerProxy, authRequest);
            Assert.fail("should throw");
        } catch (Exception ex) {
            assertTrue("Exception type check", ex.getCause() instanceof AuthenticationException);
            assertEquals("check error code", ADALError.BROKER_AUTHENTICATOR_UNSUPPORTED_OPERATION,
                    ((AuthenticationException)ex.getCause()).getCode());
        }
    }

    public void testGetIntentForBrokerActivity_emptyIntent() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            OperationCanceledException, AuthenticatorException, IOException {
        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", "loginhint", PromptBehavior.Auto, "",
                UUID.randomUUID());
        AccountManager mockAcctManager = mock(AccountManager.class);
        Bundle expected = new Bundle();
        prepareAddAccount(brokerProxy, mockAcctManager, expected);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getIntentForBrokerActivity",
                authRequest.getClass());
        Intent intent = (Intent)m.invoke(brokerProxy, authRequest);

        // assert
        assertNull("Intent is null", intent);
    }

    public void testGetIntentForBrokerActivity_hasIntent() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            OperationCanceledException, AuthenticatorException, IOException {
        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.aad.adal.BrokerProxy");
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", "loginhint", PromptBehavior.Auto, "",
                UUID.randomUUID());
        AccountManager mockAcctManager = mock(AccountManager.class);
        Bundle expected = new Bundle();
        Intent expectedIntent = new Intent();
        expectedIntent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST,
                AuthenticationConstants.Broker.BROKER_REQUEST);
        expected.putParcelable(AccountManager.KEY_INTENT, expectedIntent);

        prepareAddAccount(brokerProxy, mockAcctManager, expected);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getIntentForBrokerActivity",
                authRequest.getClass());
        Intent intent = (Intent)m.invoke(brokerProxy, authRequest);

        // assert
        assertNotNull("intent is not null", intent);
        assertEquals("intent is not null", AuthenticationConstants.Broker.BROKER_REQUEST,
                intent.getStringExtra(AuthenticationConstants.Broker.BROKER_REQUEST));
    }

    private void updateContextToSaveAccount(Context mockContext, String initialList,
            String savingAccount) {
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getString(anyString(), Mockito.eq(""))).thenReturn(initialList);
        Editor mockEditor = mock(Editor.class);
        when(mockPrefs.edit()).thenReturn(mockEditor);
    }

    @SuppressWarnings("unchecked")
    private void prepareAddAccount(Object brokerProxy, AccountManager mockAcctManager,
            Bundle expected) throws OperationCanceledException, IOException,
            AuthenticatorException, NoSuchFieldException, IllegalAccessException {
        AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(
                mockAcctManager.addAccount(anyString(), anyString(), (String[])eq(null),
                        any(Bundle.class), (Activity)eq(null),
                        (AccountManagerCallback<Bundle>)eq(null), any(Handler.class))).thenReturn(
                mockFuture);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(
                getAccountList("test", AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE));
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(null);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);
    }

    private Account[] getAccountList(String accountname, String authenticatorType) {
        Account account = new Account(accountname, authenticatorType);
        Account[] accts = new Account[1];
        accts[0] = account;
        return accts;
    }

    private void prepareProxyForTest(Object brokerProxy, String authenticatorType,
            String brokerPackage, String contextPackage, Signature signature,
            boolean permissionStatus, Account[] accounts) throws NoSuchFieldException,
            IllegalAccessException, NameNotFoundException {
        AccountManager mockAcctManager = mock(AccountManager.class);
        AuthenticatorDescription[] descriptions = getAuthenticator(authenticatorType, brokerPackage);
        Context mockContext = getMockContext(signature, brokerPackage, contextPackage,
                permissionStatus);
        when(mockAcctManager.getAuthenticatorTypes()).thenReturn(descriptions);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(accounts);

        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);
    }

    private Context getMockContext(final Signature signature, final String brokerPackageName,
            final String contextPackageName, boolean permissionStatus) throws NameNotFoundException {
        Context mockContext = mock(Context.class);
        // insert packagemanager mocks
        PackageManager mockPackageManager = getPackageManager(signature, brokerPackageName,
                permissionStatus);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        when(mockContext.getPackageName()).thenReturn(contextPackageName);
        return mockContext;
    }

    private PackageManager getPackageManager(final Signature signature, final String packageName,
            boolean permissionStatus) throws NameNotFoundException {
        PackageManager mockPackage = mock(PackageManager.class);
        PackageInfo info = new PackageInfo();
        Signature[] signatures = new Signature[1];
        signatures[0] = signature;
        info.signatures = signatures;
        when(mockPackage.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)).thenReturn(
                info);
        when(mockPackage.checkPermission(anyString(), anyString())).thenReturn(
                permissionStatus ? PackageManager.PERMISSION_GRANTED
                        : PackageManager.PERMISSION_DENIED);
        return mockPackage;
    }

    private AuthenticatorDescription[] getAuthenticator(final String authenticatorType,
            final String packagename) {
        AuthenticatorDescription[] items = new AuthenticatorDescription[1];
        items[0] = new AuthenticatorDescription(authenticatorType, packagename, 0, 0, 0, 0, true);

        return items;
    }

    private static Object createAuthenticationRequest(String authority, String resource,
            String client, String redirect, String loginhint, PromptBehavior prompt,
            String extraQueryParams, UUID correlationId) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");

        Constructor<?> constructor = c.getDeclaredConstructor(String.class, String.class,
                String.class, String.class, String.class, PromptBehavior.class, String.class,
                UUID.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authority, resource, client, redirect, loginhint,
                prompt, extraQueryParams, correlationId);
        return o;
    }
}
