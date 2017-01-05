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

import android.net.Uri;

import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Instance and Tenant discovery. It takes authorization endpoint and sends
 * query to known hard coded instances to get tenant discovery endpoint. If
 * instance is valid, it will return tenant discovery endpoint info. Instance
 * discovery endpoint does not verify tenant info, so Discovery implementation
 * sends common as a tenant name. Discovery checks only authorization endpoint.
 * It does not do tenant verification. Initialize and call from UI thread.
 */
final class Discovery {

    private static final String TAG = "Discovery";

    private static final String API_VERSION_KEY = "api-version";

    private static final String API_VERSION_VALUE = "1.0";

    private static final String AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";

    private static final String INSTANCE_DISCOVERY_SUFFIX = "common/discovery/instance";

    private static final String AUTHORIZATION_COMMON_ENDPOINT = "/common/oauth2/authorize";

    private static final String TENANT_DISCOVERY_ENDPOINT = "tenant_discovery_endpoint";

    /**
     * Sync set of valid hosts to skip query to server if host was verified
     * before.
     */
    private static final Set<String> VALID_HOSTS = Collections
            .synchronizedSet(new HashSet<String>());

    /**
     * Sync map of validated AD FS authorities and domains. Skips query to server
     * if already verified
     */
    private static final Map<String, Set<URI>> ADFS_VALIDATED_AUTHORITIES =
            Collections.synchronizedMap(new HashMap<String, Set<URI>>());

    /**
     * Discovery query will go to the prod only for now.
     */
    private static final String TRUSTED_QUERY_INSTANCE = "login.windows.net";

    private UUID mCorrelationId;

    /**
     * interface to use in testing.
     */
    private final IWebRequestHandler mWebrequestHandler;

    public Discovery() {
        initValidList();
        mWebrequestHandler = new WebRequestHandler();
    }

    void validateAuthorityADFS(final URL authorizationEndpoint, final String domain)
            throws AuthenticationException {
        if (StringExtensions.isNullOrBlank(domain)) {
            throw new IllegalArgumentException("Cannot validate AD FS Authority with domain [null]");
        }
        validateADFS(authorizationEndpoint, domain);
        validateAuthority(authorizationEndpoint);
    }

    void validateAuthority(final URL authorizationEndpoint) throws AuthenticationException {
        verifyAuthorityValidInstance(authorizationEndpoint);

        if (!VALID_HOSTS.contains(authorizationEndpoint.getHost().toLowerCase(Locale.US))) {
            // host can be the instance or inside the validated list.
            // Valid hosts will help to skip validation if validated before
            // call Callback and skip the look up
            // Only query from Prod instance for now, not all of the instances in the list
            queryInstance(authorizationEndpoint);
        }
    }

    private static void validateADFS(final URL authorizationEndpoint, final String domain)
            throws AuthenticationException {
        // Maps & Sets of URLs perform domain name resolution for equals() & hashCode()
        // To prevent this from happening, store/consult the cache using the URI value
        final URI authorityUri;
        try {
            authorityUri = authorizationEndpoint.toURI();
        } catch (URISyntaxException e) {
            throw new AuthenticationException(
                    ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL,
                    "Authority URL/URI must be RFC 2396 compliant to use AD FS validation"
            );
        }

        // First, consult the cache
        if (ADFS_VALIDATED_AUTHORITIES.get(domain) != null
                && ADFS_VALIDATED_AUTHORITIES.get(domain).contains(authorityUri)) {
            // Trust has already been established, do not requery
            return;
        }

        // Get the DRS metadata
        final DRSMetadata drsMetadata = new DRSMetadataRequestor().requestMetadata(domain);

        // Get the WebFinger metadata
        final WebFingerMetadata webFingerMetadata =
                new WebFingerMetadataRequestor() // create the requestor
                        .requestMetadata(// request the data
                                new WebFingerMetadataRequestParameters(// using these params
                                        authorizationEndpoint,
                                        drsMetadata
                                )
                        );

        // Verify trust
        if (!ADFSWebFingerValidator.realmIsTrusted(authorityUri, webFingerMetadata)) {
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE);
        }

        // Trust established, add it to the cache

        // If this authorization endpoint doesn't already have a Set, create it
        if (ADFS_VALIDATED_AUTHORITIES.get(domain) == null) {
            ADFS_VALIDATED_AUTHORITIES.put(domain, new HashSet<URI>());
        }

        // Add the entry
        ADFS_VALIDATED_AUTHORITIES.get(domain).add(authorityUri);
    }

    /**
     * Set correlation id for the tenant discovery call.
     *
     * @param requestCorrelationId The correlation id for the tenant discovery.
     */
    public void setCorrelationId(final UUID requestCorrelationId) {
        mCorrelationId = requestCorrelationId;
    }

    /**
     * initialize initial valid host list with known instances.
     */
    private void initValidList() {
        // mValidHosts is a sync set
        if (VALID_HOSTS.isEmpty()) {
            VALID_HOSTS.add("login.windows.net"); // Microsoft Azure Worldwide - Used in validation scenarios where host is not this list
            VALID_HOSTS.add("login.microsoftonline.com"); // Microsoft Azure Worldwide
            VALID_HOSTS.add("login.chinacloudapi.cn"); // Microsoft Azure China
            VALID_HOSTS.add("login.microsoftonline.de"); // Microsoft Azure Germany
            VALID_HOSTS.add("login-us.microsoftonline.com"); // Microsoft Azure US Government
        }
    }

    private void queryInstance(final URL authorizationEndpointUrl) throws AuthenticationException {

        // It will query prod instance to verify the authority
        // construct query string for this instance
        URL queryUrl;
        final boolean result;
        try {
            queryUrl = buildQueryString(TRUSTED_QUERY_INSTANCE, getAuthorizationCommonEndpoint(authorizationEndpointUrl));
            result = sendRequest(queryUrl);
        } catch (final IOException | JSONException e) {
            Logger.e(TAG, "Error when validating authority", "", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, e);
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE, e.getMessage(), e);
        }

        if (!result) {
            // throw exception in the false case
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE);
        }

        addValidHostToList(authorizationEndpointUrl);
    }

    private boolean sendRequest(final URL queryUrl) throws IOException, JSONException, AuthenticationException {

        Logger.v(TAG, "Sending discovery request to:" + queryUrl);
        final Map<String, String> headers = new HashMap<>();
        headers.put(WebRequestHandler.HEADER_ACCEPT, WebRequestHandler.HEADER_ACCEPT_JSON);

        // CorrelationId is used to track the request at the Azure services
        if (mCorrelationId != null) {
            headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, mCorrelationId.toString());
            headers.put(AuthenticationConstants.AAD.RETURN_CLIENT_REQUEST_ID, "true");
        }

        final HttpWebResponse webResponse;
        try {
            ClientMetrics.INSTANCE.beginClientMetricsRecord(queryUrl, mCorrelationId, headers);
            webResponse = mWebrequestHandler.sendGet(queryUrl, headers);
            ClientMetrics.INSTANCE.setLastError(null);

            // parse discovery response to find tenant info
            final Map<String, String> discoveryResponse = parseResponse(webResponse);
            if (discoveryResponse.containsKey(AuthenticationConstants.OAuth2.ERROR_CODES)) {
                final String errorCodes = discoveryResponse.get(
                        AuthenticationConstants.OAuth2.ERROR_CODES);
                ClientMetrics.INSTANCE.setLastError(errorCodes);
                throw new AuthenticationException(
                        ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE,
                        "Fail to valid authority with errors: " + errorCodes);
            }

            return discoveryResponse.containsKey(TENANT_DISCOVERY_ENDPOINT);
        } finally {
            ClientMetrics.INSTANCE.endClientMetricsRecord(
                    ClientMetricsEndpointType.INSTANCE_DISCOVERY, mCorrelationId);
        }
    }

    static void verifyAuthorityValidInstance(final URL authorizationEndpoint) throws AuthenticationException {
        // For comparison purposes, convert to lowercase Locale.US
        // getProtocol returns scheme and it is available if it is absolute url
        // Authority is in the form of https://Instance/tenant/somepath
        if (authorizationEndpoint == null || StringExtensions.isNullOrBlank(authorizationEndpoint.getHost())
                || !authorizationEndpoint.getProtocol().equals("https")
                || !StringExtensions.isNullOrBlank(authorizationEndpoint.getQuery())
                || !StringExtensions.isNullOrBlank(authorizationEndpoint.getRef())
                || StringExtensions.isNullOrBlank(authorizationEndpoint.getPath())) {
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE);
        }
    }

    /**
     * add this host as valid to skip another query to server.
     *
     * @param validhost
     */
    private void addValidHostToList(URL validhost) {
        String validHost = validhost.getHost();
        if (!StringExtensions.isNullOrBlank(validHost)) {
            // for comparisons it uses Locale.US, so it needs to be same
            // here
            VALID_HOSTS.add(validHost.toLowerCase(Locale.US));
        }
    }

    /**
     * get Json output from web response body. If it is well formed response, it
     * will have tenant discovery endpoint.
     *
     * @param webResponse HttpWebResponse from which Json has to be extracted
     * @return true if tenant discovery endpoint is reported. false otherwise.
     * @throws JSONException
     */
    private Map<String, String> parseResponse(HttpWebResponse webResponse) throws JSONException {
        return HashMapExtensions.getJsonResponse(webResponse);
    }

    /**
     * service side does not validate tenant, so it is sending common keyword as
     * tenant.
     *
     * @param authorizationEndpointUrl converts the endpoint URL to authorization endpoint
     * @return https://hostname/common
     */
    private String getAuthorizationCommonEndpoint(final URL authorizationEndpointUrl) {
        return new Uri.Builder().scheme("https")
                .authority(authorizationEndpointUrl.getHost())
                .appendPath(AUTHORIZATION_COMMON_ENDPOINT).build().toString();
    }

    /**
     * It will build query url to check the authorization endpoint.
     *
     * @param instance                 authority instance
     * @param authorizationEndpointUrl authorization endpoint
     * @return URL
     * @throws MalformedURLException
     */
    private URL buildQueryString(final String instance, final String authorizationEndpointUrl)
            throws MalformedURLException {

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https").authority(instance);
        // replacing tenant to common since instance validation does not check
        // tenant name
        builder.appendEncodedPath(INSTANCE_DISCOVERY_SUFFIX)
                .appendQueryParameter(API_VERSION_KEY, API_VERSION_VALUE)
                .appendQueryParameter(AUTHORIZATION_ENDPOINT_KEY, authorizationEndpointUrl);
        return new URL(builder.build().toString());
    }

    Set<String> getValidHosts() {
        return VALID_HOSTS;
    }
}
