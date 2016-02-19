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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
        Nonce, CertAuthorities, Version, SubmitUrl, Context, CertThumbprint
    }

    class ChallangeRequest {
        String mNonce = "";

        String mContext = "";

        /**
         * Authorization endpoint will return accepted authorities.
         */
        List<String> mCertAuthorities;

        /**
         * Token endpoint will return thumbprint.
         */
        String mThumbprint = "";

        String mVersion = null;

        String mSubmitUrl = "";
    }

    /**
     * This parses the redirectURI for challenge components and produces
     * response object.
     * 
     * @param redirectUri Location: urn:http-auth:CertAuth?Nonce=<noncevalue>
     *            &CertAuthorities=<distinguished names of CAs>&Version=1.0
     *            &SubmitUrl=<URL to submit response>&Context=<server state that
     *            client must convey back>
     * @return Return Device challange response
     */
    public ChallangeResponse getChallangeResponseFromUri(final String redirectUri) {
        ChallangeRequest request = getChallangeRequest(redirectUri);
        return getDeviceCertResponse(request);
    }

    public ChallangeResponse getChallangeResponseFromHeader(final String challangeHeaderValue,
            final String endpoint) throws UnsupportedEncodingException {
        ChallangeRequest request = getChallangeRequestFromHeader(challangeHeaderValue);
        request.mSubmitUrl = endpoint;
        return getDeviceCertResponse(request);
    }

    private ChallangeResponse getDeviceCertResponse(ChallangeRequest request) {
        ChallangeResponse response = getNoDeviceCertResponse(request);
        response.mSubmitUrl = request.mSubmitUrl;

        // If not device cert exists, alias or privatekey will not exist on the
        // device
        @SuppressWarnings("unchecked")
        Class<IDeviceCertificate> certClazz = (Class<IDeviceCertificate>)AuthenticationSettings.INSTANCE
                .getDeviceCertificateProxy();
        if (certClazz != null) {

            IDeviceCertificate deviceCertProxy = getWPJAPIInstance(certClazz);
            if (deviceCertProxy.isValidIssuer(request.mCertAuthorities)
                    || (deviceCertProxy.getThumbPrint() != null && deviceCertProxy.getThumbPrint()
                            .equalsIgnoreCase(request.mThumbprint))) {
                RSAPrivateKey privateKey = deviceCertProxy.getRSAPrivateKey();
                if (privateKey != null) {
                    String jwt = mJWSBuilder.generateSignedJWT(request.mNonce, request.mSubmitUrl,
                            privateKey, deviceCertProxy.getRSAPublicKey(),
                            deviceCertProxy.getCertificate());
                    response.mAuthorizationHeaderValue = String.format(
                            "%s AuthToken=\"%s\",Context=\"%s\",Version=\"%s\"",
                            AuthenticationConstants.Broker.CHALLANGE_RESPONSE_TYPE, jwt,
                            request.mContext, request.mVersion);
                    Logger.v(TAG, "Challange response:" + response.mAuthorizationHeaderValue);
                } else {
                    throw new AuthenticationException(ADALError.KEY_CHAIN_PRIVATE_KEY_EXCEPTION);
                }
            }
        }

        return response;
    }

    private IDeviceCertificate getWPJAPIInstance(Class<IDeviceCertificate> certClazz) {
        IDeviceCertificate deviceCertProxy = null;
        Constructor<?> constructor;
        try {
            constructor = certClazz.getDeclaredConstructor();
            deviceCertProxy = (IDeviceCertificate)constructor.newInstance((Object[])null);
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
        response.mAuthorizationHeaderValue = String.format("%s Context=\"%s\",Version=\"%s\"",
                AuthenticationConstants.Broker.CHALLANGE_RESPONSE_TYPE, request.mContext,
                request.mVersion);
        return response;
    }

    private ChallangeRequest getChallangeRequestFromHeader(final String headerValue)
            throws UnsupportedEncodingException {
        if (StringExtensions.IsNullOrBlank(headerValue)) {
            throw new IllegalArgumentException("headerValue");
        }

        // Header value should start with correct challenge type
        if (!StringExtensions.hasPrefixInHeader(headerValue,
                AuthenticationConstants.Broker.CHALLANGE_RESPONSE_TYPE)) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    headerValue);
        }

        ChallangeRequest challange = new ChallangeRequest();
        String authenticateHeader = headerValue
                .substring(AuthenticationConstants.Broker.CHALLANGE_RESPONSE_TYPE.length());
        ArrayList<String> queryPairs = StringExtensions.splitWithQuotes(authenticateHeader, ',');
        HashMap<String, String> headerItems = new HashMap<String, String>();

        for (String queryPair : queryPairs) {
            ArrayList<String> pair = StringExtensions.splitWithQuotes(queryPair, '=');
            if (pair.size() == 2 && !StringExtensions.IsNullOrBlank(pair.get(0))
                    && !StringExtensions.IsNullOrBlank(pair.get(1))) {
                String key = pair.get(0);
                String value = pair.get(1);
                key = StringExtensions.URLFormDecode(key);
                value = StringExtensions.URLFormDecode(value);
                key = key.trim();
                value = StringExtensions.removeQuoteInHeaderValue(value.trim());
                headerItems.put(key, value);
            } else {

                // invalid format
                throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                        authenticateHeader);
            }
        }

        validateChallangeRequest(headerItems, false);
        challange.mNonce = headerItems.get(RequestField.Nonce.name());
        if (StringExtensions.IsNullOrBlank(challange.mNonce)) {
            challange.mNonce = headerItems.get(RequestField.Nonce.name().toLowerCase(Locale.US));
        }
        
        if (!StringExtensions.IsNullOrBlank(headerItems.get(RequestField.CertThumbprint.name()))){
        	challange.mThumbprint = headerItems.get(RequestField.CertThumbprint.name());
        }
        else if (headerItems.containsKey(RequestField.CertAuthorities.name())) {
        	String authorities = headerItems.get(RequestField.CertAuthorities.name());
        	challange.mCertAuthorities = StringExtensions.getStringTokens(authorities, 
    				AuthenticationConstants.Broker.CHALLANGE_REQUEST_CERT_AUTH_DELIMETER);
        }
        else {
        	throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID, 
        			"Both certThumbprint and certauthorities are not present");
        }
        
        challange.mVersion = headerItems.get(RequestField.Version.name());
        challange.mContext = headerItems.get(RequestField.Context.name());
        return challange;
    }

    private void validateChallangeRequest(HashMap<String, String> headerItems,
            boolean redirectFormat) {
        if (!(headerItems.containsKey(RequestField.Nonce.name()) || headerItems
                .containsKey(RequestField.Nonce.name().toLowerCase(Locale.US)))) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID, "Nonce");
        }
        if (!headerItems.containsKey(RequestField.Version.name())) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "Version");
        }
        if (redirectFormat && !headerItems.containsKey(RequestField.SubmitUrl.name())) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "SubmitUrl");
        }
        if (!headerItems.containsKey(RequestField.Context.name())) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "Context");
        }
        if (redirectFormat && !headerItems.containsKey(RequestField.CertAuthorities.name())) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    "CertAuthorities");
        }
    }

    private ChallangeRequest getChallangeRequest(final String redirectUri) {
        if (StringExtensions.IsNullOrBlank(redirectUri)) {
            throw new IllegalArgumentException("redirectUri");
        }

        ChallangeRequest challange = new ChallangeRequest();
        HashMap<String, String> parameters = StringExtensions.getUrlParameters(redirectUri);
        validateChallangeRequest(parameters, true);
        challange.mNonce = parameters.get(RequestField.Nonce.name());
        if (StringExtensions.IsNullOrBlank(challange.mNonce)) {
            challange.mNonce = parameters.get(RequestField.Nonce.name().toLowerCase(Locale.US));
        }
        String authorities = parameters.get(RequestField.CertAuthorities.name());
        Logger.v(TAG, "Cert authorities:" + authorities);
        challange.mCertAuthorities = StringExtensions.getStringTokens(authorities,
                AuthenticationConstants.Broker.CHALLANGE_REQUEST_CERT_AUTH_DELIMETER);
        challange.mVersion = parameters.get(RequestField.Version.name());
        challange.mSubmitUrl = parameters.get(RequestField.SubmitUrl.name());
        challange.mContext = parameters.get(RequestField.Context.name());
        return challange;
    }
}
