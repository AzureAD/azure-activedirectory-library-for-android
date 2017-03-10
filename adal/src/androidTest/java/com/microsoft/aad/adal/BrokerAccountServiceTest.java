//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.adal;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Base64;

import junit.framework.Assert;

import org.mockito.Mockito;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test cases for brokerAccountService and related operations in {@link BrokerProxy}.
 */
public final class BrokerAccountServiceTest extends ServiceTestCase<MockBrokerAccountService> {
    private static ExecutorService sThreadExecutor = Executors.newSingleThreadExecutor();
    private static final String VALID_AUTHORITY = "https://login.microsoftonline.com";

    private IBinder mIBinder;
    public BrokerAccountServiceTest() {
        super(MockBrokerAccountService.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        mIBinder = bindService(new Intent(mContext, MockBrokerAccountService.class));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        AuthenticationSettings.INSTANCE.setBrokerSignature(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_SIGNATURE);
        AuthenticationSettings.INSTANCE.setUseBroker(false);
    }

    @SmallTest
    public void testGetBrokerUsers() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        sThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Context context = getMockContext();
                try {
                    final UserInfo[] userInfos = BrokerAccountServiceHandler.getInstance().getBrokerUsers(context);
                    Assert.assertTrue(userInfos.length == 1);

                    final UserInfo userInfo = userInfos[0];
                    Assert.assertTrue(userInfo.getDisplayableId().equals(MockBrokerAccountService.DISPLAYABLE));
                    Assert.assertTrue(userInfo.getUserId().equals(MockBrokerAccountService.UNIQUE_ID));
                    Assert.assertTrue(userInfo.getFamilyName().equals(MockBrokerAccountService.FAMILY_NAME));
                    Assert.assertTrue(userInfo.getGivenName().equals(MockBrokerAccountService.GIVEN_NAME));
                    Assert.assertTrue(userInfo.getIdentityProvider().equals(MockBrokerAccountService.IDENTITY_PROVIDER));
                } catch (final IOException e) {
                    fail();
                } finally {
                    latch.countDown();
                }
            }
        });
        latch.await();
    }

    @SmallTest
    public void testBrokerProxyGetUsers() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        sThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Context context = getMockContext();
                final BrokerProxy brokerProxy = new BrokerProxy(context);
                try {
                    final UserInfo[] userInfos = brokerProxy.getBrokerUsers();
                    Assert.assertTrue(userInfos.length == 1);

                    final UserInfo userInfo = userInfos[0];
                    Assert.assertTrue(userInfo.getDisplayableId().equals(MockBrokerAccountService.DISPLAYABLE));
                    Assert.assertTrue(userInfo.getUserId().equals(MockBrokerAccountService.UNIQUE_ID));
                    Assert.assertTrue(userInfo.getFamilyName().equals(MockBrokerAccountService.FAMILY_NAME));
                    Assert.assertTrue(userInfo.getGivenName().equals(MockBrokerAccountService.GIVEN_NAME));
                    Assert.assertTrue(userInfo.getIdentityProvider().equals(MockBrokerAccountService.IDENTITY_PROVIDER));
                } catch (final IOException | OperationCanceledException | AuthenticatorException e) {
                    fail();
                } finally {
                    latch.countDown();
                }
            }
        });

        latch.await();
    }

    @SmallTest
    public void testGetAuthToken() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        sThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Context context = getMockContext();
                try {
                    final Bundle bundle = BrokerAccountServiceHandler.getInstance().getAuthToken(context, new Bundle());
                    Assert.assertTrue(bundle.getString(AccountManager.KEY_AUTHTOKEN).equals(MockBrokerAccountService.ACCESS_TOKEN));
                } catch (final AuthenticationException e) {
                    fail();
                } finally {
                    latch.countDown();
                }
            }
        });
        latch.await();
    }

    public void testGetAuthTokenVerifyNoNetwork() throws InterruptedException, AuthenticatorException,OperationCanceledException, IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        sThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Context mockContext = getMockContext();
                Bundle requestBundle = new Bundle();
                requestBundle.putString("isConnectionAvaliable","false");

                try {
                    final Bundle bundle = BrokerAccountServiceHandler.getInstance().getAuthToken(mockContext, requestBundle);
                    Assert.assertTrue(bundle.getInt(AccountManager.KEY_ERROR_CODE) == AccountManager.ERROR_CODE_NETWORK_ERROR);
                    Assert.assertTrue(bundle.getString(AccountManager.KEY_ERROR_MESSAGE).equals(ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE.getDescription()));
                } catch (final AuthenticationException e) {
                    fail();
                } finally {
                    latch.countDown();
                }
            }
        });
        latch.await();

    }

    @SmallTest
    public void testBrokerProxyGetAuthToken() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        sThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final Context context = getMockContext();
                final BrokerProxy brokerProxy = new BrokerProxy(context);
                try {
                    final AuthenticationResult result = brokerProxy.getAuthTokenInBackground(
                            AuthenticationContextTest.createAuthenticationRequest(VALID_AUTHORITY, "resource", "clientid", "redirect", "", false));
                    Assert.assertTrue(result.getAccessToken().equals(MockBrokerAccountService.ACCESS_TOKEN));
                } catch (final AuthenticationException e) {
                    fail();
                } finally {
                    latch.countDown();
                }
            }
        });
        latch.await();
    }

    /**
     * Verify even if GET_ACCOUNTS permission is not granted, if BrokerAccountService exists,
     * {@link BrokerProxy#canSwitchToBroker(String)} will return true.
     */
    @SmallTest
    public void testBrokerProxySwitchBrokerPermissionNotGranted()
            throws PackageManager.NameNotFoundException, NoSuchAlgorithmException {
        final Context context = getMockContext();
        final PackageManager mockedPackageManager = context.getPackageManager();

        final SignatureData signatureData = getSignature(mContext, getContext().getPackageName());
        mockPackageManagerBrokerSignatureAndPermission(mockedPackageManager, signatureData.getSignature());

        AuthenticationSettings.INSTANCE.setBrokerSignature(signatureData.getSignatureHash());
        AuthenticationSettings.INSTANCE.setUseBroker(true);

        final BrokerProxy brokerProxy = new BrokerProxy(context);
        Assert.assertTrue(brokerProxy.canSwitchToBroker(BrokerProxyTests.TEST_AUTHORITY).equals(BrokerProxy.SwitchToBroker.CAN_SWITCH_TO_BROKER));
    }

    /**
     * Verify  if GET_ACCOUNTS permission is not granted and BrokerAccountService exists,
     * {@link BrokerProxy#canSwitchToBroker(String)} will return false if there is no valid broker app exists.
     */
    @SmallTest
    public void testBrokerProxySwitchToBrokerInvalidBrokerPackageName()
            throws PackageManager.NameNotFoundException, NoSuchAlgorithmException {
        final Context context = getMockContext();
        final PackageManager mockedPackageManager = context.getPackageManager();

        final SignatureData signatureData = getSignature(mContext, getContext().getPackageName());
        mockPackageManagerBrokerSignatureAndPermission(mockedPackageManager, signatureData.getSignature());

        AuthenticationSettings.INSTANCE.setUseBroker(true);
        AuthenticationSettings.INSTANCE.setBrokerSignature(signatureData.getSignatureHash());
        final BrokerProxy brokerProxy = new BrokerProxy(context);
        Assert.assertFalse(brokerProxy.canSwitchToBroker(BrokerProxyTests.TEST_AUTHORITY).equals(BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER));
    }

    private Context getMockContext() {
        final BrokerAccountServiceContext mockContext = new BrokerAccountServiceContext(getContext());

        final PackageManager mockedPackageManager = Mockito.mock(PackageManager.class);
        Mockito.when(mockedPackageManager.queryIntentServices(Mockito.any(Intent.class), Mockito.anyInt())).thenReturn(
                Collections.singletonList(Mockito.mock(ResolveInfo.class)));
        mockContext.setMockedPackageManager(mockedPackageManager);

        final AccountManager mockedAccountManager = Mockito.mock(AccountManager.class);
        final AuthenticatorDescription authenticatorDescription = new AuthenticatorDescription(
                AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE, getContext().getPackageName(), 0, 0, 0, 0);
        Mockito.when(mockedAccountManager.getAuthenticatorTypes()).thenReturn(new AuthenticatorDescription[] {authenticatorDescription});
        mockContext.setMockedAccountManager(mockedAccountManager);
        return mockContext;
    }

    private void mockPackageManagerBrokerSignatureAndPermission(final PackageManager packageManager, final Signature signature)
            throws PackageManager.NameNotFoundException {
        Mockito.when(packageManager.checkPermission(Mockito.contains("android.permission.GET_ACCOUNTS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_DENIED);

        final PackageInfo packageInfo = Mockito.mock(PackageInfo.class);
        packageInfo.signatures = new Signature[] {signature};
        Mockito.when(packageManager.getPackageInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(packageInfo);

        Mockito.when(packageManager.checkPermission(Mockito.contains("android.permission.GET_ACCOUNTS"),
                Mockito.anyString())).thenReturn(PackageManager.PERMISSION_DENIED);
    }

    private SignatureData getSignature(final Context context, final String packageName)
            throws PackageManager.NameNotFoundException, NoSuchAlgorithmException {
        PackageInfo info = context.getPackageManager().getPackageInfo(packageName,
                PackageManager.GET_SIGNATURES);

        // Broker App can be signed with multiple certificates. It will look
        // all of them
        // until it finds the correct one for ADAL broker.
        byte[] signatureByte;
        String signatureTag;
        for (final Signature signature : info.signatures) {
            signatureByte = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(signatureByte);
            signatureTag = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            return new SignatureData(new Signature(signatureByte), signatureTag);
        }

        return null;
    }

    private static class SignatureData {
        private Signature mSignature;
        private String mSignatureHash;

        SignatureData(final Signature signature, final String signatureHash) {
            mSignature = signature;
            mSignatureHash = signatureHash;
        }

        Signature getSignature() {
            return mSignature;
        }

        String getSignatureHash() {
            return mSignatureHash;
        }
    }

    final class BrokerAccountServiceContext extends FileMockContext {
        private final Context mContext;
        BrokerAccountServiceContext(final Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public boolean bindService(Intent service, ServiceConnection conn, int flags) {
            conn.onServiceConnected(new ComponentName(mContext.getPackageName(), "test"), mIBinder);
            return true;
        }

        @Override
        public void unbindService(ServiceConnection connection) {
            return;
        }
    }
}
