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

public class PackageHelperTests extends AndroidTestCase {

    private static final String TEST_PACKAGE_NAME = "com.microsoft.aad.adal.test";

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
