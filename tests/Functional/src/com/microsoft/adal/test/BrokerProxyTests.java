
package com.microsoft.adal.test;

import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

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
        PackageInfo info = mContext.getPackageManager().getPackageInfo("com.microsoft.adal.testapp",
                PackageManager.GET_SIGNATURES);

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
}
