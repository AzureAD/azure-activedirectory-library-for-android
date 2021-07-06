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

import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.util.Base64;

import com.microsoft.identity.common.internal.broker.PackageHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.MessageDigest;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, shadows = {
        SigningInfoShadow.class
})
public class PackageHelperCurrentSignatureTests {
    static final String ENCODED_SIGNATURE = "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";
    private String testSignature;
    private Signature mockedSignature;

    @Before
    public void setUp() throws Exception {
        mockedSignature = new Signature(Base64.decode(ENCODED_SIGNATURE, Base64.NO_WRAP));
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(mockedSignature.toByteArray());
        testSignature = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
    }

    @Test
    public void testGetCurrentSignatureForPackage() {
        SigningInfoShadow.setSignatures(new Signature[]{mockedSignature});
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.signatures = new Signature[]{mockedSignature};
        String signature = PackageHelper.getCurrentSignatureForPackage(packageInfo);
        // assert
        assertEquals("should be same info", testSignature, signature);
    }
}