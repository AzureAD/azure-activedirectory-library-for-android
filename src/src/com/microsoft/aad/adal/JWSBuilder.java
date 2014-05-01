
package com.microsoft.aad.adal;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.google.gson.Gson;

/**
 * JWS response builder for certificate challenge response
 */
class JWSBuilder implements IJWSBuilder {
    private static final String JWS_HEADER_ALG = "RS256";

    private static final String JWS_ALGORITHM = "SHA256withRSA";

    class Claims {
        @com.google.gson.annotations.SerializedName("aud")
        protected String mAudience;

        @com.google.gson.annotations.SerializedName("iat")
        protected long mIssueAt;

        @com.google.gson.annotations.SerializedName("nonce")
        protected String mNonce;
    }

    class JwsHeader {
        @com.google.gson.annotations.SerializedName("alg")
        protected String mAlgorithm;

        @com.google.gson.annotations.SerializedName("typ")
        protected String mType;

        @com.google.gson.annotations.SerializedName("x5t")
        protected String mCertThumbprint;

        @com.google.gson.annotations.SerializedName("keys")
        protected RSAKey[] mKeys;

        @com.google.gson.annotations.SerializedName("kid")
        protected String mKeyId;
    }

    class RSAKey {
        @com.google.gson.annotations.SerializedName("kty")
        protected String mKeyType = "RSA";

        @com.google.gson.annotations.SerializedName("alg")
        protected String mAlgorithm = "RS256";

        @com.google.gson.annotations.SerializedName("n")
        protected String mKeyModulous;

        @com.google.gson.annotations.SerializedName("e")
        protected String mKeyE;

        @com.google.gson.annotations.SerializedName("kid")
        protected String mKeyId;
    }

    public String generateSignedJWT(String nonce, String submitUrl, RSAPrivateKey privateKey,
            RSAPublicKey pubKey, String thumbPrint) {
        // http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-25
        // In the JWS Compact Serialization, a JWS object is represented as the
        // combination of these three string values,
        // BASE64URL(UTF8(JWS Protected Header)),
        // BASE64URL(JWS Payload), and
        // BASE64URL(JWS Signature),
        // concatenated in that order, with the three strings being separated by
        // two period ('.') characters.
        if (StringExtensions.IsNullOrBlank(nonce)) {
            throw new IllegalArgumentException("nonce");
        }
        if (StringExtensions.IsNullOrBlank(submitUrl)) {
            throw new IllegalArgumentException("submitUrl");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("key");
        }
        if (StringExtensions.IsNullOrBlank(thumbPrint)) {
            throw new IllegalArgumentException("thumbPrint");
        }

        Gson gson = new Gson();
        Claims claims = new Claims();
        claims.mNonce = nonce;
        claims.mAudience = submitUrl;
        claims.mIssueAt = (System.currentTimeMillis() / 1000L);

        JwsHeader header = new JwsHeader();
        header.mAlgorithm = JWS_HEADER_ALG;
        header.mType = "jwt";
        header.mCertThumbprint = thumbPrint;
        String keyId = "1";
        header.mKeyId = keyId;
        // TODO keys is a list
        // {"keys":
        // [
        // {"kty":"RSA",
        // "n":"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM
        // .JWSBuilder.",
        // "e":"AQAB",
        // "alg":"RS256",
        // "kid":"1"}
        // ]
        // }

        String signingInput = "", signature = "";
        try {
            RSAKey rsaKey = new RSAKey();
            rsaKey.mKeyId = keyId;
            rsaKey.mKeyE = StringExtensions.encodeBase64URLSafeString(pubKey.getPublicExponent().toByteArray());
            rsaKey.mKeyModulous = StringExtensions.encodeBase64URLSafeString(pubKey.getModulus().toByteArray());
            header.mKeys = new RSAKey[] {
                rsaKey
            };

            String headerJsonString = gson.toJson(header);
            String claimsJsonString = gson.toJson(claims);
            signingInput = StringExtensions.encodeBase64URLSafeString(headerJsonString
                    .getBytes(AuthenticationConstants.ENCODING_UTF8))
                    + "."
                    + StringExtensions.encodeBase64URLSafeString(claimsJsonString
                            .getBytes(AuthenticationConstants.ENCODING_UTF8));

            signature = sign((RSAPrivateKey)privateKey,
                    signingInput.getBytes(AuthenticationConstants.ENCODING_UTF8));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return signingInput + "." + signature;
    }

    
    private static Signature getSigner() throws AuthenticationException {
        Signature signer;
        try {
            signer = Signature.getInstance(JWS_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new AuthenticationException(ADALError.DEVICE_NO_SUCH_ALGORITHM,
                    "Unsupported RSA algorithm: " + e.getMessage(), e);
        }
        return signer;
    }

    private static String sign(RSAPrivateKey privateKey, final byte[] input) {
        Signature signer = getSigner();
        try {
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
