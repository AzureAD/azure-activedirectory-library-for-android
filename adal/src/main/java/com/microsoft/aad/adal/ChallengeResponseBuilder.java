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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class ChallengeResponseBuilder {

    private static final String TAG = "ChallengeResponseBuilder";

    private final IJWSBuilder mJWSBuilder;

    ChallengeResponseBuilder(IJWSBuilder jwsBuilder) {
        mJWSBuilder = jwsBuilder;
    }

    class ChallengeResponse {
        private String mSubmitUrl;

        private String mAuthorizationHeaderValue;

        String getSubmitUrl() {
            return mSubmitUrl;
        }

        void setSubmitUrl(String submitUrl) {
            mSubmitUrl = submitUrl;
        }

        String getAuthorizationHeaderValue() {
            return mAuthorizationHeaderValue;
        }

        void setAuthorizationHeaderValue(String authorizationHeaderValue) {
            mAuthorizationHeaderValue = authorizationHeaderValue;
        }
    }

    enum RequestField {
        Nonce, CertAuthorities, Version, SubmitUrl, Context, CertThumbprint
    }

    class ChallengeRequest {
        private String mNonce = "";

        private String mContext = "";

        /**
         * Authorization endpoint will return accepted authorities.
         */
        private List<String> mCertAuthorities;

        /**
         * Token endpoint will return thumbprint.
         */
        private String mThumbprint = "";

        private String mVersion = null;

        private String mSubmitUrl = "";
    }

    /**
     * This parses the redirectURI for challenge components and produces
     * response object.
     * 
     * @param redirectUri Location: urn:http-auth:CertAuth?Nonce=<noncevalue>
     *            &CertAuthorities=<distinguished names of CAs>&Version=1.0
     *            &SubmitUrl=<URL to submit response>&Context=<server state that
     *            client must convey back>
     * @return Return Device challenge response
     */
    public ChallengeResponse getChallengeResponseFromUri(final String redirectUri)
            throws AuthenticationException  {
        ChallengeRequest request = getChallengeRequest(redirectUri);
        return getDeviceCertResponse(request);
    }

    public ChallengeResponse getChallengeResponseFromHeader(final String challengeHeaderValue,
            final String endpoint) throws UnsupportedEncodingException, AuthenticationException {
        ChallengeRequest request = getChallengeRequestFromHeader(challengeHeaderValue);
        request.mSubmitUrl = endpoint;
        return getDeviceCertResponse(request);
    }

    private ChallengeResponse getDeviceCertResponse(ChallengeRequest request) throws AuthenticationException {
        ChallengeResponse response = getNoDeviceCertResponse(request);
        response.mSubmitUrl = request.mSubmitUrl;

        // If not device cert exists, alias or privatekey will not exist on the
        // device
        @SuppressWarnings("unchecked")
        Class<IDeviceCertificate> certClazz = (Class<IDeviceCertificate>) AuthenticationSettings.INSTANCE
                .getDeviceCertificateProxy();
        if (certClazz != null) {

            IDeviceCertificate deviceCertProxy = getWPJAPIInstance(certClazz);
            if (deviceCertProxy.isValidIssuer(request.mCertAuthorities)
                    || deviceCertProxy.getThumbPrint() != null && deviceCertProxy.getThumbPrint()
                            .equalsIgnoreCase(request.mThumbprint)) {
                RSAPrivateKey privateKey = deviceCertProxy.getRSAPrivateKey();
                if (privateKey == null) {
                    throw new AuthenticationException(ADALError.KEY_CHAIN_PRIVATE_KEY_EXCEPTION);
                }
                String jwt = mJWSBuilder.generateSignedJWT(request.mNonce, request.mSubmitUrl,
                        privateKey, deviceCertProxy.getRSAPublicKey(),
                        deviceCertProxy.getCertificate());
                response.mAuthorizationHeaderValue = String.format(
                        "%s AuthToken=\"%s\",Context=\"%s\",Version=\"%s\"",
                        AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, jwt,
                        request.mContext, request.mVersion);
                Logger.v(TAG, "Challenge response:" + response.mAuthorizationHeaderValue);
            }
        }

        return response;
    }
    
    private boolean isWorkplaceJoined() {
        @SuppressWarnings("unchecked")
        Class<IDeviceCertificate> certClass = (Class<IDeviceCertificate>) AuthenticationSettings.INSTANCE.getDeviceCertificateProxy();
        return certClass != null;
    }

    private IDeviceCertificate getWPJAPIInstance(Class<IDeviceCertificate> certClazz)
            throws AuthenticationException {
        final IDeviceCertificate deviceCertProxy;
        final Constructor<?> constructor;
        try {
            constructor = certClazz.getDeclaredConstructor();
            deviceCertProxy = (IDeviceCertificate) constructor.newInstance((Object[]) null);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_API_EXCEPTION,
                    "WPJ Api constructor is not defined", e);
        }
        return deviceCertProxy;
    }

    private ChallengeResponse getNoDeviceCertResponse(final ChallengeRequest request) {
        ChallengeResponse response = new ChallengeResponse();
        response.mSubmitUrl = request.mSubmitUrl;
        response.mAuthorizationHeaderValue = String.format("%s Context=\"%s\",Version=\"%s\"",
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE, request.mContext,
                request.mVersion);
        return response;
    }

    private ChallengeRequest getChallengeRequestFromHeader(final String headerValue)
            throws UnsupportedEncodingException, AuthenticationException {
        final String methodName = ":getChallengeRequestFromHeader";
        
        if (StringExtensions.isNullOrBlank(headerValue)) {
            throw new AuthenticationServerProtocolException("headerValue");
        }

        // Header value should start with correct challenge type
        if (!StringExtensions.hasPrefixInHeader(headerValue,
                AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE)) {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                    headerValue);
        }

        ChallengeRequest challenge = new ChallengeRequest();
        String authenticateHeader = headerValue
                .substring(AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE.length());
        ArrayList<String> queryPairs = StringExtensions.splitWithQuotes(authenticateHeader, ',');
        Map<String, String> headerItems = new HashMap<>();

        for (String queryPair : queryPairs) {
            ArrayList<String> pair = StringExtensions.splitWithQuotes(queryPair, '=');
            if (pair.size() == 2 && !StringExtensions.isNullOrBlank(pair.get(0))
                    && !StringExtensions.isNullOrBlank(pair.get(1))) {
                String key = pair.get(0);
                String value = pair.get(1);
                key = StringExtensions.urlFormDecode(key);
                value = StringExtensions.urlFormDecode(value);
                key = key.trim();
                value = StringExtensions.removeQuoteInHeaderValue(value.trim());
                headerItems.put(key, value);
            } else {
                // invalid format
                throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                        authenticateHeader);
            }
        }

        validateChallengeRequest(headerItems, false);
        challenge.mNonce = headerItems.get(RequestField.Nonce.name());
        if (StringExtensions.isNullOrBlank(challenge.mNonce)) {
            challenge.mNonce = headerItems.get(RequestField.Nonce.name().toLowerCase(Locale.US));
        }
        
        // When pkeyauth header is present, ADFS is always trying to device auth. When hitting token endpoint(device
        // challenge will be returned via 401 challenge), ADFS is sending back an empty cert thumbprint when they found
        // the device is not managed. To account for the behavior of how ADFS performs device auth, below code is checking 
        // if it's already workplace joined before checking the existence of cert thumprint or authority from returned challenge. 
        if (!isWorkplaceJoined()) {
            Logger.v(TAG + methodName, "Device is not workplace joined. ");
        } else if (!StringExtensions.isNullOrBlank(headerItems.get(RequestField.CertThumbprint.name()))) {
            Logger.v(TAG + methodName, "CertThumbprint exists in the device auth challenge.");
            challenge.mThumbprint = headerItems.get(RequestField.CertThumbprint.name());
        } else if (headerItems.containsKey(RequestField.CertAuthorities.name())) {
            Logger.v(TAG + methodName, "CertAuthorities exists in the device auth challenge.");
            String authorities = headerItems.get(RequestField.CertAuthorities.name());
            challenge.mCertAuthorities = StringExtensions.getStringTokens(authorities, 
                AuthenticationConstants.Broker.CHALLENGE_REQUEST_CERT_AUTH_DELIMETER);
        } else {
            throw new AuthenticationException(ADALError.DEVICE_CERTIFICATE_REQUEST_INVALID,
                "Both certThumbprint and certauthorities are not present");
        }
        
        challenge.mVersion = headerItems.get(RequestField.Version.name());
        challenge.mContext = headerItems.get(RequestField.Context.name());
        return challenge;
    }

    private void validateChallengeRequest(Map<String, String> headerItems,
            boolean redirectFormat) throws AuthenticationException {
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

    private ChallengeRequest getChallengeRequest(final String redirectUri)
            throws AuthenticationException {
        if (StringExtensions.isNullOrBlank(redirectUri)) {
            throw new AuthenticationServerProtocolException("redirectUri");
        }

        ChallengeRequest challenge = new ChallengeRequest();
        HashMap<String, String> parameters = StringExtensions.getUrlParameters(redirectUri);
        validateChallengeRequest(parameters, true);
        challenge.mNonce = parameters.get(RequestField.Nonce.name());
        if (StringExtensions.isNullOrBlank(challenge.mNonce)) {
            challenge.mNonce = parameters.get(RequestField.Nonce.name().toLowerCase(Locale.US));
        }
        String authorities = parameters.get(RequestField.CertAuthorities.name());
        Logger.v(TAG, "Cert authorities:" + authorities);
        challenge.mCertAuthorities = StringExtensions.getStringTokens(authorities,
                AuthenticationConstants.Broker.CHALLENGE_REQUEST_CERT_AUTH_DELIMETER);
        challenge.mVersion = parameters.get(RequestField.Version.name());
        challenge.mSubmitUrl = parameters.get(RequestField.SubmitUrl.name());
        challenge.mContext = parameters.get(RequestField.Context.name());
        return challenge;
    }
}
