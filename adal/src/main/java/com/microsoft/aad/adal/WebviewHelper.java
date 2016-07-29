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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import com.microsoft.aad.adal.ChallengeResponseBuilder.ChallengeResponse;

import android.content.Intent;
import android.text.TextUtils;

/**
 * Wrapper class to handle internals for request intent and response for custom
 * webview usage.
 */
public class WebviewHelper {

    private static final String TAG = "WebviewHelper";

    private final Intent mRequestIntent;

    private final AuthenticationRequest mRequest;

    private final Oauth2 mOauth;

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

        if (TextUtils.isEmpty(mRequest.getResource())) {
            throw new IllegalArgumentException("Resource is null");
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
     * @throws UnsupportedEncodingException if the url is malformed
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
     * @param finalUrl url to be passed to the intent
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
            authRequest = (AuthenticationRequest) request;
        }

        return authRequest;
    }

    /**
     *
     * @param challengeUrl URL from which challenge response is received
     * @return PreKeyAuth class filled in
     * @throws UnsupportedEncodingException on malformed exception
     * @throws AuthenticationException on parameter validation failure
     */
    public PreKeyAuthInfo getPreKeyAuthInfo(String challengeUrl)
            throws UnsupportedEncodingException, AuthenticationException {
        IJWSBuilder jwsBuilder = new JWSBuilder();

        ChallengeResponseBuilder certHandler = new ChallengeResponseBuilder(jwsBuilder);

        final ChallengeResponse challengeResponse = certHandler
                .getChallengeResponseFromUri(challengeUrl);

        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER,
                challengeResponse.getAuthorizationHeaderValue());

        String loadUrl = challengeResponse.getSubmitUrl();

        HashMap<String, String> parameters = StringExtensions
                .getUrlParameters(challengeResponse.getSubmitUrl());

        Logger.v(TAG, "SubmitUrl:" + challengeResponse.getSubmitUrl());

        if (!parameters.containsKey(AuthenticationConstants.OAuth2.CLIENT_ID)) {
            loadUrl = loadUrl + "?" + mOauth.getAuthorizationEndpointQueryParameters();
        }
        return new PreKeyAuthInfo(headers, loadUrl);
    }

    public static class PreKeyAuthInfo {

        private final HashMap<String, String> mHttpHeaders;

        private final String mLoadUrl;

        /**
         *  Construct the PreKeyAuthInfo.
         *
         * @param httpHeaders the http headers
         * @param loadUrl load Url
         */
        public PreKeyAuthInfo(HashMap<String, String> httpHeaders, String loadUrl) {
            this.mHttpHeaders = httpHeaders;
            this.mLoadUrl = loadUrl;
        }

        /**
         * Get the HTTP headers.
         * @return HashMap containing http headers
         */
        public HashMap<String, String> getHttpHeaders() {
            return mHttpHeaders;
        }

        /**
         * Get the load Url.
         *
         * @return load Url
         */
        public String getLoadUrl() {
            return mLoadUrl;
        }
    }
}
