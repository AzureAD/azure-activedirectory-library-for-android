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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.google.gson.Gson;

import android.util.Base64;

/**
 * JWS response builder for certificate challenge response.
 */
class JWSBuilder implements IJWSBuilder {
    private static final long SECONDS_MS = 1000L;

    /**
     * Algorithm is fixed to RSA PKCS v1.5.
     */
    private static final String JWS_HEADER_ALG = "RS256";

    /**
     * Algorithm name in this provider.
     */
    private static final String JWS_ALGORITHM = "SHA256withRSA";

    private static final String TAG = "JWSBuilder";

    /**
     * Payload for JWS.
     */
    class Claims {
        @com.google.gson.annotations.SerializedName("aud")
        private String mAudience;

        @com.google.gson.annotations.SerializedName("iat")
        private long mIssueAt;

        @com.google.gson.annotations.SerializedName("nonce")
        private String mNonce;
    }

    /**
     * Header that includes algorithm, type, thumbprint, keys, and keyid.
     */
    class JwsHeader {
        @com.google.gson.annotations.SerializedName("alg")
        private String mAlgorithm;

        @com.google.gson.annotations.SerializedName("typ")
        private String mType;

        @com.google.gson.annotations.SerializedName("x5c")
        private String[] mCert;
    }

    /**
     * Generate the signed JWT.
     */
    public String generateSignedJWT(String nonce, String audience, RSAPrivateKey privateKey,
            RSAPublicKey pubKey, X509Certificate cert) throws AuthenticationException {
        // http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-25
        // In the JWS Compact Serialization, a JWS object is represented as the
        // combination of these three string values,
        // BASE64URL(UTF8(JWS Protected Header)),
        // BASE64URL(JWS Payload), and
        // BASE64URL(JWS Signature),
        // concatenated in that order, with the three strings being separated by
        // two period ('.') characters.
        // Base64 encoding without padding, wrapping and urlsafe.
        if (StringExtensions.isNullOrBlank(nonce)) {
            throw new IllegalArgumentException("nonce");
        }
        if (StringExtensions.isNullOrBlank(audience)) {
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
        claims.mIssueAt = System.currentTimeMillis() / SECONDS_MS;

        JwsHeader header = new JwsHeader();
        header.mAlgorithm = JWS_HEADER_ALG;
        header.mType = "JWT"; // recommended UpperCase in JWT Spec

        final String signingInput;
        final String signature;
        try {

            // Server side expects x5c in the header to verify the signer and
            // lookup the certificate from device registration
            // Each string in the array is a base64
            // encoded ([RFC4648] Section 4 -- not base64url encoded) DER
            // [ITU.X690.1994] PKIX certificate value. The certificate
            // containing the public key corresponding to the key used
            // to digitally sign the JWS MUST be the first certificate
            // http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-27
            header.mCert = new String[1];
            header.mCert[0] = new String(Base64.encode(cert.getEncoded(), Base64.NO_WRAP),
                    AuthenticationConstants.ENCODING_UTF8);

            // redundant but current ADFS code base is looking for
            String headerJsonString = gson.toJson(header);
            String claimsJsonString = gson.toJson(claims);
            Logger.v(TAG, "Client certificate challenge response JWS Header:" + headerJsonString);
            signingInput = StringExtensions.encodeBase64URLSafeString(headerJsonString
                    .getBytes(AuthenticationConstants.ENCODING_UTF8))
                    + "."
                    + StringExtensions.encodeBase64URLSafeString(claimsJsonString
                            .getBytes(AuthenticationConstants.ENCODING_UTF8));

            signature = sign(privateKey,
                    signingInput.getBytes(AuthenticationConstants.ENCODING_UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new AuthenticationException(ADALError.ENCODING_IS_NOT_SUPPORTED,
                    "Unsupported encoding", e);
        } catch (CertificateEncodingException e) {
            throw new AuthenticationException(ADALError.CERTIFICATE_ENCODING_ERROR,
                    "Certifiante encoding error", e);
        }
        return signingInput + "." + signature;
    }

    /**
     * Signs the input with the private key.
     * 
     * @param privateKey the key to sign input with
     * @param input the data that needs to be signed
     * @return String signed string
     */
    private static String sign(RSAPrivateKey privateKey, final byte[] input) throws AuthenticationException {
        final Signature signer;
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
