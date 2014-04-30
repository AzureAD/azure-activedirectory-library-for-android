
package com.microsoft.aad.adal;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PSSParameterSpec;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.KeyChain;
import android.security.KeyChainException;

class ClientCertificateHandler {

    private static final String JWS_HEADER_ALG = "RS256";

    private static final String JWS_ALGORITHM = "SHA256withRSA";

    private static final String TAG = "ClientCertificateHandler";

    private static final String SHARED_PREFERENCE_NAME = "AADAuthenticatorPreferences";

    private static final String WPJ_ALIAS = "WPJ_ALIAS";

    private Context mContext;

    ClientCertificateHandler(Context ctx) {
        mContext = ctx;
    }

    class ChallangeResponse {
        String mSubmitUrl;

        String mAuthorizationHeaderValue;

        public String getSubmitUrl() {
            return mSubmitUrl;
        }

        public String getAuthorizationHeaderValue() {
            return mAuthorizationHeaderValue;
        }
    }

    enum RequestField {
        Nonce, CertAuthorities, Version, SubmitUrl, Context
    }

    class ChallangeRequest {
        String mNonce;

        String mContext;

        List<String> mCertAuthorities;

        String mVersion;

        String mSubmitUrl;
    }

    /**
     * This parses the redirectURI for challange components produces response
     * header value to be used in the new start url.
     * 
     * @param redirectUri Location: urn:http-auth:CertAuth?Nonce=<noncevalue>
     *            &CertAuthorities=<distinguished names of CAs>&Version=1.0
     *            &SubmitUrl=<URL to submit response>&Context=<server state that
     *            client must convey back>
     * @return
     * @throws KeyChainException
     */
    public ChallangeResponse getChallangeResponse(final String redirectUri)
            throws KeyChainException {
        ChallangeRequest request = getChallangeRequest(redirectUri);
        ChallangeResponse response = getNoDeviceCertResponse(request);

        // If not device cert exists, alias or privatekey will not exist on the
        // device
        String alias = getCertificateAlias();
        if (!StringExtensions.IsNullOrBlank(alias)) {
            PrivateKey privateKey = getPrivateKey(request, alias);
            if (privateKey != null) {
                response.mSubmitUrl = request.mSubmitUrl;
                String jwt = getSignedJWT(request, privateKey);
                response.mAuthorizationHeaderValue = String.format(
                        "CertAuth AuthToken=\"%s\",Context=\"%s\"", jwt, request.mContext);
                Logger.v(TAG, "Challange response:" + response.mAuthorizationHeaderValue);
            }
        }
        return response;
    }

    private ChallangeResponse getNoDeviceCertResponse(final ChallangeRequest request) {
        ChallangeResponse response = new ChallangeResponse();
        response.mSubmitUrl = request.mSubmitUrl;
        response.mAuthorizationHeaderValue = String.format("CertAuth Context=\"%s\"",
                request.mContext);
        return response;
    }

    class Claims {
        @com.google.gson.annotations.SerializedName("aud")
        private String mAudience;

        @com.google.gson.annotations.SerializedName("iat")
        private long mIssueAt;

        @com.google.gson.annotations.SerializedName("nonce")
        private String mNonce;
    }

    class JwsHeader {
        @com.google.gson.annotations.SerializedName("alg")
        private String mAlgorithm;

        @com.google.gson.annotations.SerializedName("typ")
        private String mType;

        @com.google.gson.annotations.SerializedName("x5t")
        private String mCertThumbprint;

        @com.google.gson.annotations.SerializedName("keys")
        private String mKeys;
    }

    private String getSignedJWT(final ChallangeRequest request, final PrivateKey privateKey)
            throws UnsupportedEncodingException {
        // http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-25
        // In the JWS Compact Serialization, a JWS object is represented as the
        // combination of these three string values,
        // BASE64URL(UTF8(JWS Protected Header)),
        // BASE64URL(JWS Payload), and
        // BASE64URL(JWS Signature),
        // concatenated in that order, with the three strings being separated by
        // two period ('.') characters.
        Gson gson = new Gson();
        Claims claims = new Claims();
        claims.mNonce = request.mNonce;
        claims.mAudience = request.mSubmitUrl;
        claims.mIssueAt = (System.currentTimeMillis() / 1000L);

        JwsHeader header = new JwsHeader();
        header.mAlgorithm = JWS_HEADER_ALG;
        header.mType = "jwt";
        header.mCertThumbprint = "TODO thumbprint";
        header.mKeys = "keys";
        String headerJsonString = gson.toJson(header);
        String claimsJsonString = gson.toJson(claims);
        String signingInput = StringExtensions.encodeBase64URLSafeString(headerJsonString
                .getBytes(AuthenticationConstants.ENCODING_UTF8))
                + "."
                + StringExtensions.encodeBase64URLSafeString(claimsJsonString
                        .getBytes(AuthenticationConstants.ENCODING_UTF8));
        String signature = sign((RSAPrivateKey)privateKey,
                signingInput.getBytes(AuthenticationConstants.ENCODING_UTF8));
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

    private PrivateKey getPrivateKey(final ChallangeRequest request, String alias)
            throws KeyChainException {
        // Get cert from keychain for given authorities
        PrivateKey privateKey = null;
        // TODO problem with key??
        try {
            X509Certificate[] certificateChain = KeyChain.getCertificateChain(mContext, alias);
            Logger.v(TAG, "Alias:" + alias + " certificate length:" + certificateChain.length);
            for (int i = 0; i < certificateChain.length; i++) {

                // Get subject
                Principal principal = certificateChain[i].getIssuerDN();
                String issuerDn = principal.getName();
                Logger.v(TAG, "Alias:" + alias + " Issuer:" + issuerDn);

                if (request.mCertAuthorities.contains(issuerDn)) {
                    Logger.v(TAG, "Issuer:" + issuerDn + " is valid");
                    try {
                        privateKey = KeyChain.getPrivateKey(mContext, alias);
                        Logger.v(TAG, "KeyChain have private key for alias:" + alias);
                    } catch (KeyChainException e) {
                        Logger.e(TAG, "KeyChain exception in getting privatekey", "",
                                ADALError.KEY_CHAIN_PRIVATE_KEY_EXCEPTION, e);
                    } catch (InterruptedException e) {
                        // TODO Logger.e(TAG,"Interrupted","",ADALError.);
                    }
                }
            }
        } catch (InterruptedException e) {
            // TODO Logger.e(TAG,"Interrupted","",ADALError.);
        }

        return privateKey;
    }

    private String getCertificateAlias() {
        // TBD
        // 1- WPJ currently support one cert per device. It can store in a
        // SharedPref file. This context can access that since it will be in
        // same process.
        SharedPreferences pref = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME,
                Activity.MODE_PRIVATE);
        return pref.getString(WPJ_ALIAS, "");
    }

    private ChallangeRequest getChallangeRequest(final String redirectUri) {
        if (StringExtensions.IsNullOrBlank(redirectUri)) {
            throw new IllegalArgumentException("redirectUri");
        }
        ChallangeRequest challange = new ChallangeRequest();
        HashMap<String, String> parameters = StringExtensions.getUrlParameters(redirectUri);
        if (!parameters.containsKey(RequestField.Nonce.name())) {
            throw new IllegalArgumentException("Nonce");
        }
        if (!parameters.containsKey(RequestField.CertAuthorities.name())) {
            throw new IllegalArgumentException("CertAuthorities");
        }
        if (!parameters.containsKey(RequestField.Version.name())) {
            throw new IllegalArgumentException("Version");
        }
        if (!parameters.containsKey(RequestField.SubmitUrl.name())) {
            throw new IllegalArgumentException("SubmitUrl");
        }
        if (!parameters.containsKey(RequestField.Context.name())) {
            throw new IllegalArgumentException("Context");
        }
        challange.mNonce = parameters.get(RequestField.Nonce.name());
        String authorities = parameters.get(RequestField.CertAuthorities.name());
        challange.mCertAuthorities = StringExtensions.getStringTokens(authorities,
                AuthenticationConstants.Broker.CHALLANGE_REQUEST_CERT_AUTH_DELIMETER);
        if (challange.mCertAuthorities == null || challange.mCertAuthorities.size() == 0) {
            throw new IllegalArgumentException("CertAuthorities");
        }
        challange.mVersion = parameters.get(RequestField.Version.name());
        challange.mSubmitUrl = parameters.get(RequestField.SubmitUrl.name());
        challange.mContext = parameters.get(RequestField.Context.name());
        return challange;
    }
}
