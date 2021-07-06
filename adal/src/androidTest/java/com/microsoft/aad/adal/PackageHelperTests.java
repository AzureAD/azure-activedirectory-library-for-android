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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.broker.PackageHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class PackageHelperTests {

    private static final int TEST_UID = 13;

    private byte[] mTestSignature;

    private String mTestTag;

    private Context mContext;

    @SuppressLint("PackageManagerGetSignatures")
    @Before
    public void setUp() throws Exception {
        mContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext();
        System.setProperty("dexmaker.dexcache", mContext.getCacheDir().getPath());

        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            // use same key for tests
            final int iterationCount = 100;
            final int keyLength = 256;
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes(StandardCharsets.UTF_8), iterationCount, keyLength));
            SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }

        AuthenticationSettings.INSTANCE.setBrokerPackageName("invalid_do_not_switch");
        AuthenticationSettings.INSTANCE.setBrokerSignature("invalid_do_not_switch");

        // Broker App can be signed with multiple certificates. It will look
        // all of them
        // until it finds the correct one for ADAL broker.
        for (final Signature signature : PackageHelper.getSignatures(mContext)) {
            mTestSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(mTestSignature);
            mTestTag = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            break;
        }
    }

    @After
    public void tearDown() {
        Logger.getInstance().setExternalLogger(null);
    }

    @Test
    public void testGetUIDForPackage() throws NameNotFoundException, IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        final Context mockContext = getMockContext(new Signature(mTestSignature), mContext.getPackageName(),
                TEST_UID);
        final Object packageHelper = getInstance(mockContext);
        final Method m = ReflectionUtils.getTestMethod(
                packageHelper,
                "getUIDForPackage", // method name
                String.class
        );

        // act
        int actual = (Integer) m.invoke(packageHelper, mContext.getPackageName());

        // assert
        assertEquals("should be same UID", TEST_UID, actual);

        // act
        actual = (Integer) m.invoke(packageHelper, (String) null);

        // assert
        assertEquals("should return 0", 0, actual);
    }

    @Test
    public void testRedirectUrl() throws NameNotFoundException, IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, UnsupportedEncodingException {
        final Context mockContext = getMockContext(
                new Signature(mTestSignature),
                mContext.getPackageName(),
                0 // calling uid
        );
        final Object packageHelper = getInstance(mockContext);
        final Method m = ReflectionUtils.getTestMethod(
                packageHelper,
                "getBrokerRedirectUrl", // method name
                String.class,
                String.class
        );

        // act
        final String actual = (String) m.invoke(packageHelper, mContext.getPackageName(), mTestTag);

        // assert
        assertTrue("should have packagename", actual.contains(mContext.getPackageName()));
        assertTrue("should have signature url encoded",
                actual.contains(
                        URLEncoder.encode(mTestTag, AuthenticationConstants.ENCODING_UTF8)
                )
        );
    }

    private static Object getInstance(final Context mockContext) throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        final Class<?> c = Class.forName("com.microsoft.identity.common.internal.broker.PackageHelper");
        final Constructor<?> constructorParams = c.getDeclaredConstructor(PackageManager.class);
        constructorParams.setAccessible(true);
        return constructorParams.newInstance(mockContext.getPackageManager());
    }

    private Context getMockContext(final Signature signature,
                                   final String packageName,
                                   final int callingUID) throws NameNotFoundException {
        final Context mockContext = mock(Context.class);
        // insert packagemanager mocks
        PackageManager mockPackageManager = getPackageManager(signature, packageName, callingUID);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
        when(mockContext.getPackageName()).thenReturn(packageName);
        return mockContext;
    }

    @SuppressLint("PackageManagerGetSignatures")
    private PackageManager getPackageManager(final Signature signature,
                                             final String packageName,
                                             final int callingUID) throws NameNotFoundException {
        final PackageManager mockPackage = mock(PackageManager.class);
        final MockedPackageInfo mockedPackageInfo = new MockedPackageInfo(new Signature[]{signature});

        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.name = packageName;
        appInfo.uid = callingUID;
        when(
                mockPackage.getPackageInfo(
                        packageName,
                        PackageHelper.getPackageManagerSignaturesFlag()
                )
        ).thenReturn(mockedPackageInfo);

        when(mockPackage.getApplicationInfo(packageName, 0)).thenReturn(appInfo);
        Context mock = mock(Context.class);
        when(mock.getPackageManager()).thenReturn(mockPackage);
        return mockPackage;
    }
}
