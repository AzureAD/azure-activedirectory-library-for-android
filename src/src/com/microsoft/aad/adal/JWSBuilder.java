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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import android.util.Base64;

import com.google.gson.Gson;

/**
 * JWS response builder for certificate challenge response
 */
class JWSBuilder implements IJWSBuilder {
    /**
     * Algorithm is fixed to RSA PKCS v1.5
     */
    private static final String JWS_HEADER_ALG = "RS256";

    /**
     * Algorithm name in this provider
     */
    private static final String JWS_ALGORITHM = "SHA256withRSA";

    private static final String TAG = "JWSBuilder";

    /**
     * Payload for JWS
     */
    class Claims {
        @com.google.gson.annotations.SerializedName("aud")
        protected String mAudience;

        @com.google.gson.annotations.SerializedName("iat")
        protected long mIssueAt;

        @com.google.gson.annotations.SerializedName("nonce")
        protected String mNonce;
    }

    /**
     * Header that includes algorithm, type, thumbprint, keys, and keyid
     */
    class JwsHeader {
        @com.google.gson.annotations.SerializedName("alg")
        protected String mAlgorithm;

        @com.google.gson.annotations.SerializedName("typ")
        protected String mType;

        @com.google.gson.annotations.SerializedName("x5c")
        protected String mCert;
    }

    /**
     * 
     */
    public String generateSignedJWT(String nonce, String audience, RSAPrivateKey privateKey,
            RSAPublicKey pubKey, X509Certificate cert) {
        // http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-25
        // In the JWS Compact Serialization, a JWS object is represented as the
        // combination of these three string values,
        // BASE64URL(UTF8(JWS Protected Header)),
        // BASE64URL(JWS Payload), and
        // BASE64URL(JWS Signature),
        // concatenated in that order, with the three strings being separated by
        // two period ('.') characters.
        // Base64 encoding without padding, wrapping and urlsafe.
        if (StringExtensions.IsNullOrBlank(nonce)) {
            throw new IllegalArgumentException("nonce");
        }
        if (StringExtensions.IsNullOrBlank(audience)) {
            throw new IllegalArgumentException("audience");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("privateKey");
        }
        if (pubKey == null) {
            throw new IllegalArgumentException("pubKey");
        }

        Gson gson = new Gson();
        Claims claims = new Claims();
        claims.mNonce = nonce;
        claims.mAudience = audience;
        claims.mIssueAt = (System.currentTimeMillis() / 1000L);

        JwsHeader header = new JwsHeader();
        header.mAlgorithm = JWS_HEADER_ALG;
        header.mType = "JWT"; // recommended UpperCase in JWT Spec
        String signingInput = "", signature = "";
        try {

            // Server side expects x5c in the header to verify the signer and
            // lookup the certificate from device registration
            // Each string in the array is a base64
            // encoded ([RFC4648] Section 4 -- not base64url encoded) DER
            // [ITU.X690.1994] PKIX certificate value. The certificate
            // containing the public key corresponding to the key used
            // to digitally sign the JWS MUST be the first certificate
            // http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-27
            header.mCert = new String(Base64.encode(cert.getEncoded(), Base64.NO_WRAP),
                    AuthenticationConstants.ENCODING_UTF8);

            String headerJsonString = gson.toJson(header);
            String claimsJsonString = gson.toJson(claims);
            Logger.v(TAG, "Client certificate challange response JWS Header:" + headerJsonString);
            signingInput = StringExtensions.encodeBase64URLSafeString(headerJsonString
                    .getBytes(AuthenticationConstants.ENCODING_UTF8))
                    + "."
                    + StringExtensions.encodeBase64URLSafeString(claimsJsonString
                            .getBytes(AuthenticationConstants.ENCODING_UTF8));

            signature = sign((RSAPrivateKey)privateKey,
                    signingInput.getBytes(AuthenticationConstants.ENCODING_UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new AuthenticationException(ADALError.ENCODING_IS_NOT_SUPPORTED);
        } catch (CertificateEncodingException e) {
            throw new AuthenticationException(ADALError.CERTIFICATE_ENCODING_ERROR);
        }
        return signingInput + "." + signature;
    }

    /**
     * Returns a byte array representation of the specified big integer without
     * the sign bit.
     */
    public static byte[] toBytesUnsigned(final BigInteger bigInt) {

        // Copied from Apache Commons Codec 1.8 with LCA approval
        int bitlen = bigInt.bitLength();

        // round bitlen
        bitlen = ((bitlen + 7) >> 3) << 3;
        final byte[] bigBytes = bigInt.toByteArray();
        if (((bigInt.bitLength() % 8) != 0) && (((bigInt.bitLength() / 8) + 1) == (bitlen / 8))) {
            return bigBytes;
        }

        // set up params for copying everything but sign bit
        int startSrc = 0;
        int len = bigBytes.length;

        // if bigInt is exactly byte-aligned, just skip signbit in copy
        if ((bigInt.bitLength() % 8) == 0) {
            startSrc = 1;
            len--;
        }
        final int startDst = bitlen / 8 - len; // to pad w/ nulls as per spec
        final byte[] resizedBytes = new byte[bitlen / 8];
        System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, len);
        return resizedBytes;
    }

    /**
     * Signs the input with the private key
     * 
     * @param privateKey
     * @param input
     * @return
     */
    private static String sign(RSAPrivateKey privateKey, final byte[] input) {
        Signature signer = null;
        try {
            signer = Signature.getInstance(JWS_ALGORITHM);
            signer.initSign(privateKey);
            signer.update(input);
            return StringExtensions.encodeBase64URLSafeString(signer.sign());
        } catch (InvalidKeyException e) {
            throw new AuthenticationException(ADALError.KEY_CHAIN_PRIVATE_KEY_EXCEPTION,
                    "Invalid private RSA key: " + e.getMessage(), e);
        } catch (SignatureException e) {
            throw new AuthenticationException(ADALError.SIGNATURE_EXCEPTION,
                    "RSA signature exception: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new AuthenticationException(ADALError.ENCODING_IS_NOT_SUPPORTED);
        } catch (NoSuchAlgorithmException e) {
            throw new AuthenticationException(ADALError.DEVICE_NO_SUCH_ALGORITHM,
                    "Unsupported RSA algorithm: " + e.getMessage(), e);
        }
    }
}
