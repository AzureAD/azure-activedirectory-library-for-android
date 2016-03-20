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

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * Work place join related certificate is required to respond device challenge.
 */
public interface IDeviceCertificate {

    /**
     * Checks valid issuer for cert authorities.
     * 
     * @param certAuthorities list of cert authorities
     * @return status if valid issue
     */
    boolean isValidIssuer(final List<String> certAuthorities);

    /**
     * Gets certificate.
     * 
     * @return {@link X509Certificate}
     */
    X509Certificate getCertificate();

    /**
     * Gets RSA private key.
     * 
     * @return RSA private key
     */
    RSAPrivateKey getRSAPrivateKey();

    /**
     * Gets thumbPrint for certificate.
     * 
     * @return thumbPrint for certificate.
     */
    String getThumbPrint();

    /**
     * Gets RSA public key.
     * 
     * @return RSA public key.
     */
    RSAPublicKey getRSAPublicKey();
}
