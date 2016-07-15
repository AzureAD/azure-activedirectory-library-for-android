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

/**
 * Interface to construct jws message for responding certificate challenge.
 */
public interface IJWSBuilder {

    /**
     * @param nonce A unique value issued by the server in its challenge. The
     *            client is expected to return this value to the server in its
     *            signed JWT response in order to perform device authentication.
     *            The nonce is also persisted within the encrypted context
     *            parameter.
     * @param submitUrl The version number of the challenge-response based
     *            device authentication protocol. This is set to 1.0.
     * @param privateKey Private Key of the Device Certificate to sign the
     *            response
     * @param pubKey Public Key of the Device Certificate
     * @param x509Certificate X509 certificate
     * @return Signed JWT
     * @throws AuthenticationException when errors happens for generating signed JWT.
     */
    String generateSignedJWT(String nonce, String submitUrl, RSAPrivateKey privateKey,
            RSAPublicKey pubKey, X509Certificate x509Certificate) throws AuthenticationException;
}
