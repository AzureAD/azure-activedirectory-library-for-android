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

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

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
    final class Claims {
        @SerializedName("aud")
        private String mAudience;

        @SerializedName("iat")
        private long mIssueAt;

        @SerializedName("nonce")
        private String mNonce;

        /**
         * No args constructor for use in serialization for Gson to prevent usage of sun.misc.Unsafe.
         */
        @SuppressWarnings("unused")
        private Claims() {
        }
    }

    /**
     * Header that includes algorithm, type, thumbprint, keys, and keyid.
     */
    final class JwsHeader {
        @SerializedName("alg")
        private String mAlgorithm;

        @SerializedName("typ")
        private String mType;

        @SerializedName("x5c")
        private String[] mCert;

        /**
         * No args constructor for use in serialization for Gson to prevent usage of sun.misc.Unsafe.
         */
        @SuppressWarnings("unused")
        private JwsHeader() {
        }
    }

    /**
     * Generate generic String key/value pair JWT.
     *
     * @param header
     * @param body
     * @return String Base64URLSafe(header)+Base64URLSafe(body)
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    public String generateJWT(Map<String, String> header, Map<String, String> body,
            int expTimeInSeconds) throws JSONException, UnsupportedEncodingException {
        Logger.v(TAG, "Generating JWT.");
        JSONObject headerJson = generateJson(header, expTimeInSeconds);
        JSONObject bodyJson = generateJson(body, expTimeInSeconds);
        String signingInput = StringExtensions.encodeBase64URLSafeString(headerJson.toString().getBytes(AuthenticationConstants.ENCODING_UTF8))
                + "." + StringExtensions.encodeBase64URLSafeString(bodyJson.toString().getBytes(AuthenticationConstants.ENCODING_UTF8));
        return signingInput;
    }

    private JSONObject generateJson(Map<String, String> values, int expireSeconds)
            throws JSONException {
        JSONObject json = new JSONObject();
        long iat = (System.currentTimeMillis() / SECONDS_MS);
        long expTimeInSeconds = iat + expireSeconds;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("iat") || entry.getKey().equalsIgnoreCase("nbf")) {
                json.put(entry.getKey(), iat);
            } else if (entry.getKey().equalsIgnoreCase("exp")) {
                json.put(entry.getKey(), expTimeInSeconds);
            } else {
                json.put(entry.getKey(), entry.getValue());
            }
        }

        return json;
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
        final String methodName = ":generateSignedJWT";
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
            Logger.v(TAG + methodName, "Generate client certificate challenge response JWS Header. ",
                    "Header: " + headerJsonString, null);
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
                    "Certificate encoding error", e);
        }
        return signingInput + "." + signature;
    }

    /**
     * Signs the input with the private key.
     *
     * @param privateKey the key to sign input with
     * @param input      the data that needs to be signed
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
