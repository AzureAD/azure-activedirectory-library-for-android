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

package com.microsoft.aad.adal.testapp;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import com.microsoft.aad.adal.IDeviceCertificate;

/**
 * This class will be used from ADAL after getting the classname from
 * AuthenticationSettings. WPJ class will be referenced inside the authenticator
 * not inside the public ADAL. If class is loaded at Authenticator's process, it
 * will access the WPJ API.
 */
public class MockDeviceCertProxy implements IDeviceCertificate {

    static X509Certificate sCertificate = null;

    static RSAPrivateKey sPrivateKey = null;

    static RSAPublicKey sPublicKey = null;

    static String sThumbPrint = null;

    static boolean sValidIssuer = false;

    public static void reset() {
        sCertificate = null;
        sPrivateKey = null;
        sPublicKey = null;
        sThumbPrint = null;
        sValidIssuer = false;
    }

    @Override
    public X509Certificate getCertificate() {
        return sCertificate;
    }

    @Override
    public RSAPrivateKey getRSAPrivateKey() {
        return sPrivateKey;
    }

    public RSAPublicKey getRSAPublicKey() {
        return sPublicKey;
    }

    @Override
    public String getThumbPrint() {
        return sThumbPrint;
    }

    @Override
    public boolean isValidIssuer(List<String> certAuthorities) {
        // TODO Auto-generated method stub
        return sValidIssuer;
    }
}
