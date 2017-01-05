//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.adal;

import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

class WebFingerMetadataRequestor
        extends AbstractMetadataRequestor<WebFingerMetadata, WebFingerMetadataRequestParameters> {

    /**
     * Used for logging.
     */
    private static final String TAG = WebFingerMetadataRequestor.class.getSimpleName();

    @Override
    WebFingerMetadata requestMetadata(final WebFingerMetadataRequestParameters webFingerMetadataRequestParameters)
            throws AuthenticationException {
        final URL domain = webFingerMetadataRequestParameters.getDomain();
        final DRSMetadata drsMetadata = webFingerMetadataRequestParameters.getDrsMetadata();
        Logger.v(TAG, "Validating authority for auth endpoint: " + domain.toString());
        try {
            // create the URL
            URL webFingerUrl = buildWebFingerUrl(domain, drsMetadata);

            // make the request
            final HttpWebResponse webResponse =
                    getWebrequestHandler()
                            .sendGet(
                                    webFingerUrl,
                                    new HashMap<String, String>()
                            );

            // get the status code
            final int statusCode = webResponse.getStatusCode();

            if (HttpURLConnection.HTTP_OK != statusCode) { // check 200 OK
                // non-200 codes mean not valid/trusted
                throw new AuthenticationException(
                        ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE
                );
            }

            // parse the response
            return parseMetadata(webResponse);

        } catch (IOException e) {
            throw new AuthenticationException(ADALError.IO_EXCEPTION, "Unexpected error", e);
        }
    }

    /**
     * Deserializes {@link HttpWebResponse} bodies into {@link WebFingerMetadata}.
     *
     * @param webResponse the HttpWebResponse to deserialize
     * @return the parsed response
     */
    @Override
    WebFingerMetadata parseMetadata(final HttpWebResponse webResponse) throws AuthenticationException {
        Logger.v(TAG, "Parsing WebFinger response");
        try {
            return parser().fromJson(webResponse.getBody(), WebFingerMetadata.class);
        } catch (JsonSyntaxException e) {
            throw new AuthenticationException(ADALError.JSON_PARSE_ERROR);
        }
    }

    /**
     * Create the URL used to retrieve the WebFinger metadata.
     *
     * @param resource    the resource to verify
     * @param drsMetadata the {@link DRSMetadata} to consult
     * @return the URL of the WebFinger document
     * @throws MalformedURLException if the URL could not be constructed
     */
    static URL buildWebFingerUrl(final URL resource, final DRSMetadata drsMetadata)
            throws MalformedURLException {
        final URL passiveAuthEndpoint = new URL(
                drsMetadata
                        .getIdentityProviderService()
                        .getPassiveAuthEndpoint()
        );

        // build the url
        final StringBuilder webFingerUrlBuilder =
                new StringBuilder("https://")
                        .append(passiveAuthEndpoint.getHost())
                        .append("/.well-known/webfinger?resource=")
                        .append(resource.toString());

        final String webFingerUrl = webFingerUrlBuilder.toString();

        Logger.v(TAG, "Validator will use WebFinger URL: " + webFingerUrl);

        return new URL(webFingerUrl);
    }
}
