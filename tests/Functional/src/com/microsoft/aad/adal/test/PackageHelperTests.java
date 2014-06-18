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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.security.MessageDigest;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.test.AndroidTestCase;
import android.util.Base64;

import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.Logger;

public class PackageHelperTests extends AndroidTestCase {

    private static final String TEST_PACKAGE_NAME = "com.microsoft.aad.adal.testapp";

    private byte[] testSignature;

    private String testTag;

    protected void setUp() throws Exception {
        super.setUp();
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            // use same key for tests
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes("UTF-8"), 100, 256));
            SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }
        AuthenticationSettings.INSTANCE.setBrokerPackageName("invalid_do_not_switch");
        AuthenticationSettings.INSTANCE.setBrokerSignature("invalid_do_not_switch");
        // ADAL is set to this signature for now
        PackageInfo info = mContext.getPackageManager().getPackageInfo(TEST_PACKAGE_NAME,
                PackageManager.GET_SIGNATURES);

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
    }

    protected void tearDown() throws Exception {
        Logger.getInstance().setExternalLogger(null);
        super.tearDown();
    }

    public void testGetCurrentSignatureForPackage() throws NameNotFoundException,
            IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        Context mockContext = getMockContext(new Signature(testSignature), TEST_PACKAGE_NAME, 0);
        Object packageHelper = getInstance(mockContext);
        Method m = ReflectionUtils.getTestMethod(packageHelper, "getCurrentSignatureForPackage",
                String.class);

        // act
        String actual = (String)m.invoke(packageHelper, TEST_PACKAGE_NAME);

        // assert
        assertEquals("should be same info", testTag, actual);

        // act
        actual = (String)m.invoke(packageHelper, (String)null);

        // assert
        assertNull("should return null", actual);
    }

    public void testGetUIDForPackage() throws NameNotFoundException, IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        int expectedUID = 13;
        Context mockContext = getMockContext(new Signature(testSignature), TEST_PACKAGE_NAME,
                expectedUID);
        Object packageHelper = getInstance(mockContext);
        Method m = ReflectionUtils.getTestMethod(packageHelper, "getUIDForPackage", String.class);

        // act
        int actual = (Integer)m.invoke(packageHelper, TEST_PACKAGE_NAME);

        // assert
        assertEquals("should be same UID", expectedUID, actual);

        // act
        actual = (Integer)m.invoke(packageHelper, (String)null);

        // assert
        assertEquals("should return 0", 0, actual);
    }

    public void testRedirectUrl() throws NameNotFoundException, IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, UnsupportedEncodingException {
        Context mockContext = getMockContext(new Signature(testSignature), TEST_PACKAGE_NAME, 0);
        Object packageHelper = getInstance(mockContext);
        Method m = ReflectionUtils.getTestMethod(packageHelper, "getBrokerRedirectUrl",
                String.class, String.class);

        // act
        String actual = (String)m.invoke(packageHelper, TEST_PACKAGE_NAME, testTag);

        // assert
        assertTrue("should have packagename", actual.contains(TEST_PACKAGE_NAME));
        assertTrue("should have signature url encoded",
                actual.contains(URLEncoder.encode(testTag, AuthenticationConstants.ENCODING_UTF8)));
    }

    private static Object getInstance(Context mockContext) throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Class<?> c = Class.forName("com.microsoft.aad.adal.PackageHelper");
        Constructor<?> constructorParams = c.getDeclaredConstructor(Context.class);
        constructorParams.setAccessible(true);
        Object o = constructorParams.newInstance(mockContext);
        return o;
    }

    private Context getMockContext(final Signature signature, final String packageName,
            final int callingUID) throws NameNotFoundException {
        Context mockContext = mock(Context.class);
        // insert packagemanager mocks
        PackageManager mockPackageManager = getPackageManager(signature, packageName, callingUID);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        when(mockContext.getPackageName()).thenReturn(packageName);
        return mockContext;
    }

    private PackageManager getPackageManager(final Signature signature, final String packageName,
            final int callingUID) throws NameNotFoundException {
        PackageManager mockPackage = mock(PackageManager.class);
        PackageInfo info = new PackageInfo();
        Signature[] signatures = new Signature[1];
        signatures[0] = signature;
        info.signatures = signatures;
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.name = packageName;
        appInfo.uid = callingUID;
        when(mockPackage.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)).thenReturn(
                info);
        when(mockPackage.getApplicationInfo(packageName, 0)).thenReturn(appInfo);
        Context mock = mock(Context.class);
        when(mock.getPackageManager()).thenReturn(mockPackage);
        return mockPackage;
    }
}
