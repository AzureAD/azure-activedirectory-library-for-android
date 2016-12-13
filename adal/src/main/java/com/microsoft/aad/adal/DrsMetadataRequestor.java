package com.microsoft.aad.adal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.aad.adal.DrsMetadataRequestor.Type.CLOUD;
import static com.microsoft.aad.adal.DrsMetadataRequestor.Type.ON_PREM;
import static com.microsoft.aad.adal.HttpConstants.HeaderField.ACCEPT;
import static com.microsoft.aad.adal.HttpConstants.MediaType.APPLICATION_JSON;
import static com.microsoft.aad.adal.HttpConstants.StatusCode.SC_OK;

/**
 * Delegate class capable of fetching DRS discovery metadata documents.
 *
 * @see DrsMetadata
 */
class DrsMetadataRequestor extends AbstractRequestor {

    /**
     * Tag used for logging.
     */
    private static final String TAG = "DrsRequest";

    // DRS doc constants
    private static final String DRS_URL_PREFIX = "https://enterpriseregistration.";
    private static final String API_VERSION = "1.0";
    private static final String CLOUD_RESOLVER_DOMAIN = "windows.net/";
    private static final String ENROLLMENT_PATH = "/enrollmentserver/contract?api-version=%s";

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
    DrsMetadata requestDrsDiscovery(String domain) throws AuthenticationException {
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
    private DrsMetadata requestOnPrem(String domain)
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
    private DrsMetadata requestCloud(String domain) throws AuthenticationException {
        Logger.v(TAG, "Requesting DRS discovery (cloud)");
        try {
            return requestDrsDiscoveryInternal(CLOUD, domain);
        } catch (UnknownHostException e) {
            // TODO get specific with this Exception
            throw new AuthenticationException();
        }
    }

    private DrsMetadata requestDrsDiscoveryInternal(final Type type, final String domain)
            throws AuthenticationException, UnknownHostException {
        final URL requestURL;

        try {
            // create the request URL
            requestURL = new URL(forgeRequestUrlByType(type, domain));
        } catch (MalformedURLException e) {
            throw new AuthenticationException(ADALError.DRS_METADATA_URL_INVALID);
        }

        // init the headers to use in the request
        final Map<String, String> headers = new HashMap<>();
        headers.put(ACCEPT, APPLICATION_JSON);
        if (null != getCorrelationId()) {
            headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, getCorrelationId().toString());
        }

        final DrsMetadata metadata;
        final HttpWebResponse webResponse;

        // make the request
        try {
            webResponse = getWebrequestHandler().sendGet(requestURL, headers);
            if (SC_OK == webResponse.getStatusCode()) {
                String responseBody = webResponse.getBody();
                metadata = parser().fromJson(responseBody, DrsMetadata.class);
            } else {
                // TODO something went wrong, find out what and throw an appropriate Exception for it
                throw new AuthenticationException();
            }
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            // TODO what went wrong? Find out and throw a sane Exception
            throw new AuthenticationException();
        }

        return metadata;
    }

    /**
     * Construct the URL used to request the DRS metadata.
     *
     * @param type   enum indicating how the URL should be forged
     * @param domain the domain to use in the request
     * @return the DRS metadata URL to query
     */
    private String forgeRequestUrlByType(final Type type, final String domain) {
        // All DRS urls begin the same
        String drsRequestUrl = DRS_URL_PREFIX;

        if (CLOUD == type) {
            drsRequestUrl += CLOUD_RESOLVER_DOMAIN + domain;
        } else if (ON_PREM == type) {
            drsRequestUrl += domain;
        }

        drsRequestUrl += String.format(ENROLLMENT_PATH, API_VERSION);

        Logger.v(TAG, "Requestor will use DRS url: " + drsRequestUrl);

        return drsRequestUrl;
    }

}
