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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.aad.adal.DRSMetadataRequestor.Type.CLOUD;
import static com.microsoft.aad.adal.DRSMetadataRequestor.Type.ON_PREM;
import static com.microsoft.aad.adal.AuthenticationConstants.HeaderField.ACCEPT;
import static com.microsoft.aad.adal.AuthenticationConstants.MediaType.APPLICATION_JSON;

/**
 * Delegate class capable of fetching DRS discovery metadata documents.
 *
 * @see DRSMetadata
 */
final class DRSMetadataRequestor extends AbstractMetadataRequestor<DRSMetadata, String> {

    /**
     * Tag used for logging.
     */
    private static final String TAG = DRSMetadataRequestor.class.getSimpleName();

    // DRS doc constants
    private static final String DRS_URL_PREFIX = "https://enterpriseregistration.";
    private static final String CLOUD_RESOLVER_DOMAIN = "windows.net/";

    /**
     * The DRS configuration.
     */
    enum Type {
        ON_PREM,
        CLOUD
    }

    /**
     * Request the DRS discovery metadata for a supplied domain.
     *
     * @param domain the domain to validate
     * @return the metadata
     * @throws AuthenticationException
     */
    @Override
    DRSMetadata requestMetadata(final String domain) throws AuthenticationException {
        try {
            return requestOnPrem(domain);
        } catch (UnknownHostException e) {
            return requestCloud(domain);
        }
    }

    /**
     * Requests DRS discovery metadata from on-prem configurations.
     *
     * @param domain the domain to validate
     * @return the DRS discovery metadata
     * @throws UnknownHostException    if the on-prem enrollment server cannot be resolved
     * @throws AuthenticationException if there exists an enrollment/domain mismatch (lack of trust)
     */
    private DRSMetadata requestOnPrem(final String domain)
            throws UnknownHostException, AuthenticationException {
        Logger.v(TAG, "Requesting DRS discovery (on-prem)");
        return requestDrsDiscoveryInternal(ON_PREM, domain);
    }

    /**
     * Requests DRS discovery metadata from cloud configurations.
     *
     * @param domain the domain to validate
     * @return the DRS discovery metadata
     * @throws AuthenticationException if there exists an enrollment/domain mismatch (lack of trust)
     *                                 or the trust cannot be verified
     */
    private DRSMetadata requestCloud(final String domain) throws AuthenticationException {
        Logger.v(TAG, "Requesting DRS discovery (cloud)");
        try {
            return requestDrsDiscoveryInternal(CLOUD, domain);
        } catch (UnknownHostException e) {
            throw new AuthenticationException(ADALError.DRS_DISCOVERY_FAILED_UNKNOWN_HOST);
        }
    }

    private DRSMetadata requestDrsDiscoveryInternal(final Type type, final String domain)
            throws AuthenticationException, UnknownHostException {
        final URL requestURL;

        try {
            // create the request URL
            requestURL = new URL(buildRequestUrlByType(type, domain));
        } catch (MalformedURLException e) {
            throw new AuthenticationException(ADALError.DRS_METADATA_URL_INVALID);
        }

        // init the headers to use in the request
        final Map<String, String> headers = new HashMap<>();
        headers.put(ACCEPT, APPLICATION_JSON);
        if (null != getCorrelationId()) {
            headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, getCorrelationId().toString());
        }

        final DRSMetadata metadata;
        final HttpWebResponse webResponse;

        // make the request
        try {
            webResponse = getWebrequestHandler().sendGet(requestURL, headers);
            final int statusCode = webResponse.getStatusCode();
            if (HttpURLConnection.HTTP_OK == statusCode) {
                metadata = parseMetadata(webResponse);
            } else {
                // unexpected status code
                throw new AuthenticationException(
                        ADALError.DRS_FAILED_SERVER_ERROR,
                        "Unexpected error code: [" + statusCode + "]"
                );
            }
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            throw new AuthenticationException(ADALError.IO_EXCEPTION);
        }

        return metadata;
    }

    @Override
    DRSMetadata parseMetadata(final HttpWebResponse response) throws AuthenticationException {
        Logger.v(TAG, "Parsing DRS metadata response");
        try {
            return parser().fromJson(response.getBody(), DRSMetadata.class);
        } catch (JsonSyntaxException e) {
            throw new AuthenticationException(ADALError.JSON_PARSE_ERROR);
        }
    }

    /**
     * Construct the URL used to request the DRS metadata.
     *
     * @param type   enum indicating how the URL should be forged
     * @param domain the domain to use in the request
     * @return the DRS metadata URL to query
     */
    String buildRequestUrlByType(final Type type, final String domain) {
        // All DRS urls begin the same
        StringBuilder requestUrl = new StringBuilder(DRS_URL_PREFIX);

        if (CLOUD == type) {
            requestUrl.append(CLOUD_RESOLVER_DOMAIN).append(domain);
        } else if (ON_PREM == type) {
            requestUrl.append(domain);
        }

        requestUrl.append("/enrollmentserver/contract?api-version=1.0");

        final String requestUrlStr = requestUrl.toString();

        Logger.v(TAG, "Requestor will use DRS url: " + requestUrlStr);

        return requestUrlStr;
    }

}
