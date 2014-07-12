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
     */
    public String generateSignedJWT(String nonce, String submitUrl, RSAPrivateKey privateKey,
            RSAPublicKey pubKey, X509Certificate x509Certificate);
}
