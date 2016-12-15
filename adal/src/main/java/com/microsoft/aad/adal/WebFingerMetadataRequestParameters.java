package com.microsoft.aad.adal;

import java.net.URL;

/**
 * Encapsulates parameters used to request {@link WebFingerMetadata}.
 */
class WebFingerMetadataRequestParameters {

    private final URL mDomain;

    private final DrsMetadata mMetadata;

    /**
     * Constructs a new parameter tuple.
     *
     * @param domain
     * @param metadata
     */
    WebFingerMetadataRequestParameters(URL domain, DrsMetadata metadata) {
        this.mDomain = domain;
        this.mMetadata = metadata;
    }

    /**
     * Gets the domain.
     *
     * @return the domain
     */
    URL getDomain() {
        return mDomain;
    }

    /**
     * Gets the DrsMetadata.
     *
     * @return the DrsMetadata
     */
    DrsMetadata getDrsMetadata() {
        return mMetadata;
    }
}
