package com.microsoft.aad.adal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.aad.adal.HttpConstants.HeaderField.ACCEPT;
import static com.microsoft.aad.adal.HttpConstants.MediaType.APPLICATION_JSON;
import static com.microsoft.aad.adal.HttpConstants.StatusCode.SC_OK;

class DrsMetadataRequester extends AbstractRequestor {

    private static final String TAG = "DrsRequest";

    private static final String DRS_URL_PREFIX = "https://enterpriseregistration.";
    private static final String API_VERSION = "1.0";
    private static final String CLOUD_RESOLVER_DOMAIN = "windows.net/";
    private static final String ENROLLMENT_PATH = "/enrollmentserver/contract?api-version=%s";

    private enum Type {
        ON_PREM,
        CLOUD
    }

    /**
     * Request the DRS discovery metadata for a supplied domain
     *
     * @param domain the domain to supply the metadata
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

    private DrsMetadata requestOnPrem(String domain) throws UnknownHostException, AuthenticationException {
        return requestDrsDiscoveryInternal(Type.ON_PREM, domain);
    }

    private DrsMetadata requestCloud(String domain) throws AuthenticationException {
        try {
            return requestDrsDiscoveryInternal(Type.CLOUD, domain);
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
        if (null != mCorrelationId) {
            headers.put(AuthenticationConstants.AAD.CLIENT_REQUEST_ID, mCorrelationId.toString());
        }

        final DrsMetadata metadata;
        final HttpWebResponse webResponse;

        // make the request
        try {
            webResponse = mWebrequestHandler.sendGet(requestURL, headers);
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
     * Construct the URL used to request the DRS metadata
     *
     * @param type   enum indicating how the URL should be forged
     * @param domain the domain to use in the request
     * @return the DRS metadata URL to query
     */
    private String forgeRequestUrlByType(final Type type, final String domain) {
        // All DRS urls begin the same
        String drsRequestUrl = DRS_URL_PREFIX;

        switch (type) {
            case CLOUD:
                drsRequestUrl += CLOUD_RESOLVER_DOMAIN;
            case ON_PREM:
                drsRequestUrl += domain;
        }

        drsRequestUrl += String.format(ENROLLMENT_PATH, API_VERSION);

        return drsRequestUrl;
    }

}
