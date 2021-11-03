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

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

@RunWith(AndroidJUnit4.class)
public class JwsBuilderTests extends AndroidTestHelper {

    @Test
    public void testGenerateSignedJWTNegative()
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
                    InstantiationException, IllegalAccessException, InvocationTargetException {
        final Object jwsBuilder = getInstance();
        final Method m =
                ReflectionUtils.getTestMethod(
                        jwsBuilder,
                        "generateSignedJWT",
                        String.class,
                        String.class,
                        PrivateKey.class,
                        PublicKey.class,
                        X509Certificate.class);

        try {
            m.invoke(jwsBuilder, null, "https://someurl", null, null, null);
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("nonce"));
        }

        try {
            m.invoke(jwsBuilder, "nonce", null, null, null, null);
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("audience"));
        }

        try {
            m.invoke(jwsBuilder, "nonce", "url", null, null, null);
            Assert.fail("No exception");
        } catch (final Exception ex) {
            assertTrue("Argument exception", ex.getCause().getMessage().contains("privateKey"));
        }
    }

    public static String getThumbPrintFromCert(final X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        final byte[] der = cert.getEncoded();
        md.update(der);
        final byte[] digest = md.digest();
        return bytesToHexString(digest);
    }

    private static String bytesToHexString(final byte[] bytes) {
        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < bytes.length; ++i) {
            buf.append(String.format("%02x", bytes[i]));
        }
        return buf.toString();
    }

    private Object getInstance()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        final Class clazz = Class.forName("com.microsoft.identity.common.java.util.JWSBuilder");
        final Constructor<?> constructorParams = clazz.getDeclaredConstructor();
        constructorParams.setAccessible(true);
        return constructorParams.newInstance();
    }
}
