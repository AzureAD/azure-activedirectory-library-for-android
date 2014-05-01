package com.microsoft.aad.adal;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * Work place join related certificate is required to respond device challange.
 *
 */
public interface IDeviceCertificateProxy {
    
    public boolean isValidIssuer(final List<String> certAuthorities);
    
    public X509Certificate getCertificate();
    
    public RSAPrivateKey   getRSAPrivateKey();
    
    public String getThumbPrint();

    public RSAPublicKey getRSAPublicKey();
}
