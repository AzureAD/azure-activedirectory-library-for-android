
package com.microsoft.adal.test;

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
import java.util.UUID;

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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.test.AndroidTestCase;

import com.microsoft.adal.PromptBehavior;

public class BrokerProxyTests extends AndroidTestCase {

    private static final String TAG = "BrokerProxyTests";

    private byte[] testSignature;

    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        // ADAL is set to this signature for now
        PackageInfo info = mContext.getPackageManager().getPackageInfo(
                "com.microsoft.adal.testapp", PackageManager.GET_SIGNATURES);

        // Broker App can be signed with multiple certificates. It will look
        // all of them
        // until it finds the correct one for ADAL broker.
        for (Signature signature : info.signatures) {
            MessageDigest md = MessageDigest.getInstance("SHA");
            testSignature = signature.toByteArray();
            break;
        }
    }

    public void testCanSwitchToBroker_InvalidPackage() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException, NoSuchAlgorithmException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");

        String authenticatorType = AuthenticationConstants.Broker.ACCOUNT_TYPE;
        String brokerPackage = "wrong";
        Signature signature = new Signature(testSignature);

        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, signature);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        assertFalse("verify should return false", result);
    }

    public void testCanSwitchToBroker_InvalidAuthenticatorType() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");

        String authenticatorType = "invalid";
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;
        Signature signature = new Signature(testSignature);

        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, signature);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        assertFalse("verify should return false", result);
    }

    public void testCanSwitchToBroker_InvalidSignature() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");

        String authenticatorType = AuthenticationConstants.Broker.ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;

        Signature signature = new Signature("74657374696e67");

        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, signature);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        assertFalse("verify should return false", result);
    }

    public void testCanSwitchToBroker_Valid() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            NameNotFoundException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");

        String authenticatorType = AuthenticationConstants.Broker.ACCOUNT_TYPE;
        String brokerPackage = AuthenticationConstants.Broker.PACKAGE_NAME;
        Signature signature = new Signature(testSignature);

        prepareProxyForTest(brokerProxy, authenticatorType, brokerPackage, signature);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "canSwitchToBroker");
        boolean result = (Boolean)m.invoke(brokerProxy);

        assertTrue("verify should return true", result);
    }

    public void testGetAuthTokenInBackground_emptyAccts() throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, NoSuchFieldException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");

        String authenticatorType = AuthenticationConstants.Broker.ACCOUNT_TYPE;
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", "loginhint", PromptBehavior.Auto, "",
                UUID.randomUUID());
        String acctType = "invalid_type";
        AccountManager mockAcctManager = mock(AccountManager.class);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(new Account[0]);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(null);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                authRequest.getClass());
        String token = (String)m.invoke(brokerProxy, authRequest);
        assertNull("token should return null", token);
    }

    public void testGetAuthTokenInBackground_InvalidAccount() throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, NoSuchFieldException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");

        String authenticatorType = AuthenticationConstants.Broker.ACCOUNT_TYPE;
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", "invalid_username", PromptBehavior.Auto, "",
                UUID.randomUUID());
        Account[] accts = getAccountList("valid", authenticatorType);
        AccountManager mockAcctManager = mock(AccountManager.class);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(accts);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(null);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                authRequest.getClass());
        String token = (String)m.invoke(brokerProxy, authRequest);
        assertNull("token should return null", token);
    }

    public void testGetAuthTokenInBackground_ValidAccount_EmptyBundle()
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            OperationCanceledException, AuthenticatorException, IOException, NoSuchFieldException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.ACCOUNT_TYPE;
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
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                authRequest.getClass());
        String token = (String)m.invoke(brokerProxy, authRequest);
        assertNull("token should return null", token);
    }

    public void testGetAuthTokenInBackground_Positive() throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, OperationCanceledException,
            AuthenticatorException, IOException, NoSuchFieldException {

        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");
        String authenticatorType = AuthenticationConstants.Broker.ACCOUNT_TYPE;
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", "loginhint", PromptBehavior.Auto, "",
                UUID.randomUUID());
        String acctType = "loginhint";
        Account[] accts = getAccountList(acctType, authenticatorType);
        AccountManager mockAcctManager = mock(AccountManager.class);
        Bundle expected = new Bundle();
        expected.putString(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN, "token123");
        AccountManagerFuture<Bundle> mockFuture = mock(AccountManagerFuture.class);
        when(mockFuture.getResult()).thenReturn(expected);
        when(mockAcctManager.getAccountsByType(anyString())).thenReturn(accts);
        when(
                mockAcctManager.getAuthToken(any(Account.class), anyString(), any(Bundle.class),
                        eq(false), (AccountManagerCallback<Bundle>)eq(null), any(Handler.class)))
                .thenReturn(mockFuture);
        Context mockContext = mock(Context.class);
        when(mockContext.getMainLooper()).thenReturn(null);
        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getAuthTokenInBackground",
                authRequest.getClass());
        String token = (String)m.invoke(brokerProxy, authRequest);
        assertEquals("token is expected", "token123", token);
    }

    public void testGetIntentForBrokerActivity_emptyIntent() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            OperationCanceledException, AuthenticatorException, IOException {
        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");
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
        assertNull("intent is null", intent);
    }

    public void testGetIntentForBrokerActivity_hasIntent() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            OperationCanceledException, AuthenticatorException, IOException {
        Object brokerProxy = ReflectionUtils.getInstance("com.microsoft.adal.BrokerProxy");
        Object authRequest = createAuthenticationRequest("https://login.windows.net/omercantest",
                "resource", "client", "redirect", "loginhint", PromptBehavior.Auto, "",
                UUID.randomUUID());
        AccountManager mockAcctManager = mock(AccountManager.class);
        Bundle expected = new Bundle();
        expected.putParcelable(AccountManager.KEY_INTENT, new Intent());
        prepareAddAccount(brokerProxy, mockAcctManager, expected);

        // action
        Method m = ReflectionUtils.getTestMethod(brokerProxy, "getIntentForBrokerActivity",
                authRequest.getClass());
        Intent intent = (Intent)m.invoke(brokerProxy, authRequest);
        assertNotNull("intent is not null", intent);
    }

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
            String brokerPackage, Signature signature) throws NoSuchFieldException,
            IllegalAccessException, NameNotFoundException {
        AccountManager mockAcctManager = mock(AccountManager.class);
        AuthenticatorDescription[] descriptions = getAuthenticator(authenticatorType, brokerPackage);
        Context mockContext = getMockContext(signature, brokerPackage);
        when(mockAcctManager.getAuthenticatorTypes()).thenReturn(descriptions);

        ReflectionUtils.setFieldValue(brokerProxy, "mContext", mockContext);
        ReflectionUtils.setFieldValue(brokerProxy, "mAcctManager", mockAcctManager);
    }

    private Context getMockContext(final Signature signature, final String packageName)
            throws NameNotFoundException {
        Context mockContext = mock(Context.class);
        // insert packagemanager mocks
        PackageManager mockPackageManager = getPackageManager(signature, packageName);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);

        return mockContext;
    }

    private PackageManager getPackageManager(final Signature signature, final String packageName)
            throws NameNotFoundException {
        PackageManager mockPackage = mock(PackageManager.class);
        PackageInfo info = new PackageInfo();
        Signature[] signatures = new Signature[1];
        signatures[0] = signature;
        info.signatures = signatures;
        when(mockPackage.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)).thenReturn(
                info);
        Context mock = mock(Context.class);
        when(mock.getPackageManager()).thenReturn(mockPackage);

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

        Class<?> c = Class.forName("com.microsoft.adal.AuthenticationRequest");

        Constructor<?> constructor = c.getDeclaredConstructor(String.class, String.class,
                String.class, String.class, String.class, PromptBehavior.class, String.class,
                UUID.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(authority, resource, client, redirect, loginhint,
                prompt, extraQueryParams, correlationId);
        return o;
    }
}
