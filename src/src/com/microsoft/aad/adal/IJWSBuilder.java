
package com.microsoft.aad.adal;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * simple jws message to use in responding certificate challange
 */
interface IJWSBuilder {
    public String generateSignedJWT(String nonce, String submitUrl, RSAPrivateKey privateKey,
            RSAPublicKey pubKey, String thumbPrint);
}
