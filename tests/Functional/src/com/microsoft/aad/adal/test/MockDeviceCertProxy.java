
package com.microsoft.aad.adal.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import android.security.KeyChain;
import android.security.KeyChainException;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.IDeviceCertificateProxy;
import com.microsoft.aad.adal.Logger;

public class MockDeviceCertProxy implements IDeviceCertificateProxy {

    /**
     * 
     */
    private final ClientCertHandlerTests clientCertHandlerTests;

    public MockDeviceCertProxy(ClientCertHandlerTests clientCertHandlerTests) {
        this.clientCertHandlerTests = clientCertHandlerTests;

    }

    private void setMockCertificate() {

    }

    public String getThumbPrint(X509Certificate cert) throws NoSuchAlgorithmException,
            CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return hexify(digest);
    }

    public static String hexify(byte bytes[]) {
        char[] hexDigits = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        };
        StringBuffer buf = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
        }

        return buf.toString();
    }

    @Override
    public X509Certificate getCertificate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RSAPrivateKey getRSAPrivateKey() {
        // TODO Auto-generated method stub
        return null;
    }

    public RSAPublicKey getRSAPublicKey() {
        return null;
    }

    @Override
    public String getThumbPrint() {
        // TODO Auto-generated method stub
        return null;
    }

    private PrivateKey getPrivateKey(String alias) throws KeyChainException, InterruptedException {
        // Get cert from keychain for given authorities
        PrivateKey privateKey = null;
        // TODO problem with key??

        X509Certificate[] certificateChain = KeyChain.getCertificateChain(
                this.clientCertHandlerTests.getInstrumentation().getTargetContext(), alias);
        Logger.v(ClientCertHandlerTests.TAG, "Alias:" + alias + " certificate length:"
                + certificateChain.length);
        for (int i = 0; i < certificateChain.length; i++) {

            // Get subject
            Principal principal = certificateChain[i].getIssuerDN();
            String issuerDn = principal.getName();
            Logger.v(ClientCertHandlerTests.TAG, "Alias:" + alias + " Issuer:" + issuerDn);

            Logger.v(ClientCertHandlerTests.TAG, "Issuer:" + issuerDn + " is valid");
            try {
                privateKey = KeyChain.getPrivateKey(this.clientCertHandlerTests
                        .getInstrumentation().getTargetContext(), alias);
                Logger.v(ClientCertHandlerTests.TAG, "KeyChain have private key for alias:" + alias);
            } catch (KeyChainException e) {
                Logger.e(ClientCertHandlerTests.TAG, "KeyChain exception in getting privatekey",
                        "", ADALError.KEY_CHAIN_PRIVATE_KEY_EXCEPTION, e);
            } catch (InterruptedException e) {
                // TODO Logger.e(TAG,"Interrupted","",ADALError.);
            }
        }

        return privateKey;
    }

    @Override
    public boolean isValidIssuer(List<String> certAuthorities) {
        // TODO Auto-generated method stub
        return false;
    }

}
