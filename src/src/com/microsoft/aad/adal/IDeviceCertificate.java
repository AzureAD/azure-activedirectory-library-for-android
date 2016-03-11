
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
