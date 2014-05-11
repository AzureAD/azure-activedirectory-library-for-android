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
    public ChallangeResponse getChallangeResponse(final String redirectUri) {
        ChallangeRequest request = getChallangeRequest(redirectUri);
        ChallangeResponse response = getNoDeviceCertResponse(request);

        // If not device cert exists, alias or privatekey will not exist on the
        // device
        @SuppressWarnings("unchecked")
        Class<IDeviceCertificate> certClazz = (Class<IDeviceCertificate>)AuthenticationSettings.INSTANCE
                .getDeviceCertificateProxy();
        // TODO error handling here

        IDeviceCertificate deviceCertProxy = getWPJAPIInstance(certClazz);
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

    private IDeviceCertificate getWPJAPIInstance(Class<IDeviceCertificate> certClazz) {
        IDeviceCertificate deviceCertProxy = null;
        Constructor<?> constructor;
        try {
            constructor = certClazz.getDeclaredConstructor();
            deviceCertProxy = (IDeviceCertificate)constructor.newInstance(null);
        } catch (NoSuchMethodException e) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_API_EXCEPTION,
                    "WPJ Api constructor is not defined", e);
        } catch (InstantiationException e) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_API_EXCEPTION,
                    "WPJ Api constructor is not defined", e);
        } catch (IllegalAccessException e) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_API_EXCEPTION,
                    "WPJ Api constructor is not defined", e);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_API_EXCEPTION,
                    "WPJ Api constructor is not defined", e);
        } catch (InvocationTargetException e) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_API_EXCEPTION,
                    "WPJ Api constructor is not defined", e);
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
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID, "Nonce");
        }
        if (!parameters.containsKey(RequestField.CertAuthorities.name())) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "CertAuthorities");
        }
        if (!parameters.containsKey(RequestField.Version.name())) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "Version");
        }
        if (!parameters.containsKey(RequestField.SubmitUrl.name())) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "SubmitUrl");
        }
        if (!parameters.containsKey(RequestField.Context.name())) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "Context");
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
