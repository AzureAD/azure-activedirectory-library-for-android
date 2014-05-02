
package com.microsoft.aad.adal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.List;

import android.security.KeyChainException;

class ChallangeResponseBuilder {

    private static final String TAG = "ChallangeResponseBuilder";

    private IJWSBuilder mJWSBuilder;

    ChallangeResponseBuilder(IJWSBuilder jwsBuilder) {
        mJWSBuilder = jwsBuilder;
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
     * This parses the redirectURI for challenge components and produces
     * response object.
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
        Class<IDeviceCertificateProxy> certClazz = (Class<IDeviceCertificateProxy>)AuthenticationSettings.INSTANCE
                .getDeviceCertificateProxy();
        // TODO error handling here

        IDeviceCertificateProxy deviceCertProxy = getDeviceProxyInstance(certClazz);
        if (deviceCertProxy.isValidIssuer(request.mCertAuthorities)) {
            RSAPrivateKey privateKey = deviceCertProxy.getRSAPrivateKey();
            if (privateKey != null) {
                response.mSubmitUrl = request.mSubmitUrl;
                String jwt = mJWSBuilder.generateSignedJWT(request.mNonce, request.mSubmitUrl,
                        privateKey, deviceCertProxy.getRSAPublicKey(),
                        deviceCertProxy.getThumbPrint());
                response.mAuthorizationHeaderValue = String.format(
                        "CertAuth AuthToken=\"%s\",Context=\"%s\"", jwt, request.mContext);
                Logger.v(TAG, "Challange response:" + response.mAuthorizationHeaderValue);
            } else {
                throw new AuthenticationException(ADALError.KEY_CHAIN_PRIVATE_KEY_EXCEPTION);
            }
        }
        return response;
    }

    private IDeviceCertificateProxy getDeviceProxyInstance(Class<IDeviceCertificateProxy> certClazz) {
        IDeviceCertificateProxy deviceCertProxy = null;
        Constructor<?> constructor;
        try {
            constructor = certClazz.getDeclaredConstructor();
            deviceCertProxy = (IDeviceCertificateProxy)constructor.newInstance(null);
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return deviceCertProxy;
    }

    private ChallangeResponse getNoDeviceCertResponse(final ChallangeRequest request) {
        ChallangeResponse response = new ChallangeResponse();
        response.mSubmitUrl = request.mSubmitUrl;
        response.mAuthorizationHeaderValue = String.format("CertAuth Context=\"%s\"",
                request.mContext);
        return response;
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
