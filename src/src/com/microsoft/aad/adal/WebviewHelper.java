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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import android.content.Intent;
import android.text.TextUtils;

import com.microsoft.aad.adal.ChallangeResponseBuilder.ChallangeResponse;

/**
 * Wrapper class to handle internals for request intent and response for custom
 * webview usage.
 */
public class WebviewHelper {

    private static final String TAG = "WebviewHelper";

    private Intent mRequestIntent;

    private AuthenticationRequest mRequest;

    private Oauth2 mOauth;

    /**
     * Construct with incoming requestIntent that you receive at
     * startActivityForResult.
     * 
     * @param requestIntent Intent that has request information.
     */
    public WebviewHelper(Intent requestIntent) {
        mRequestIntent = requestIntent;
        mRequest = getAuthenticationRequestFromIntent(mRequestIntent);
        mOauth = new Oauth2(mRequest);
    }

    /**
     * Check request intent fields.
     */
    public void validateRequestIntent() {

        if (mRequest == null) {
            Logger.v(TAG, "Request item is null, so it returns to caller");
            throw new IllegalArgumentException("Request is null");
        }

        if (TextUtils.isEmpty(mRequest.getAuthority())) {
            throw new IllegalArgumentException("Authority is null");
        }

        if (mRequest.getScope() == null || mRequest.getScope().length == 0) {
        	throw new IllegalArgumentException("Scope is null or empty");
        }

        if (TextUtils.isEmpty(mRequest.getClientId())) {
            throw new IllegalArgumentException("ClientId is null");
        }

        if (TextUtils.isEmpty(mRequest.getRedirectUri())) {
            throw new IllegalArgumentException("RedirectUri is null");
        }
    }

    /**
     * Gets startUrl to use as url to start webview.
     * 
     * @return Url
     * @throws UnsupportedEncodingException
     */
    public String getStartUrl() throws UnsupportedEncodingException {
        return mOauth.getCodeRequestUrl();
    }

    /**
     * Gets redirect url to tell the webview to stop before navigating.
     * 
     * @return Url
     */
    public String getRedirectUrl() {
        return mRequest.getRedirectUri();
    }

    /**
     * Creates result intent to pass into onActivityResult method.
     * 
     * @param finalUrl
     * @return Intent
     */
    public Intent getResultIntent(final String finalUrl) {
        if (mRequestIntent != null) {
            AuthenticationRequest authRequest = getAuthenticationRequestFromIntent(mRequestIntent);

            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, finalUrl);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                    authRequest);
            resultIntent.putExtra(AuthenticationConstants.Browser.REQUEST_ID,
                    authRequest.getRequestId());

            return resultIntent;
        }

        throw new IllegalArgumentException("requestIntent is null");
    }

    private AuthenticationRequest getAuthenticationRequestFromIntent(Intent callingIntent) {
        AuthenticationRequest authRequest = null;

        Serializable request = callingIntent
                .getSerializableExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE);

        if (request instanceof AuthenticationRequest) {
            authRequest = (AuthenticationRequest)request;
        }

        return authRequest;
    }

    public PreKeyAuthInfo getPreKeyAuthInfo(String challengeUrl)
            throws UnsupportedEncodingException {
        IJWSBuilder jwsBuilder = new JWSBuilder();

        ChallangeResponseBuilder certHandler = new ChallangeResponseBuilder(jwsBuilder);

        final ChallangeResponse challangeResponse = certHandler
                .getChallangeResponseFromUri(challengeUrl);

        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(AuthenticationConstants.Broker.CHALLANGE_RESPONSE_HEADER,
                challangeResponse.mAuthorizationHeaderValue);

        String loadUrl = challangeResponse.mSubmitUrl;

        HashMap<String, String> parameters = StringExtensions
                .getUrlParameters(challangeResponse.mSubmitUrl);

        Logger.v(TAG, "SubmitUrl:" + challangeResponse.mSubmitUrl);

        if (!parameters.containsKey(AuthenticationConstants.OAuth2.CLIENT_ID)) {
            loadUrl = loadUrl + "?" + mOauth.getAuthorizationEndpointQueryParameters();
        }
        PreKeyAuthInfo preKeyAuthInfo = new PreKeyAuthInfo(headers, loadUrl);
        return preKeyAuthInfo;
    }

    public static class PreKeyAuthInfo {

        private HashMap<String, String> mHttpHeaders;

        private String mLoadUrl;

        public PreKeyAuthInfo(HashMap<String, String> httpHeaders, String loadUrl) {
            this.mHttpHeaders = httpHeaders;
            this.mLoadUrl = loadUrl;
        }

        public HashMap<String, String> getHttpHeaders() {
            return mHttpHeaders;
        }

        public String getLoadUrl() {
            return mLoadUrl;
        }
    }
}
