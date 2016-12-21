package com.microsoft.aad.adal;

import java.net.URL;

/**
 * Encapsulates parameters used to request {@link WebFingerMetadata}.
 */
final class WebFingerMetadataRequestParameters {

    private final URL mDomain;

    private final DRSMetadata mMetadata;

    /**
     * Constructs a new parameter tuple.
     *
     * @param domain
     * @param metadata
     */
    WebFingerMetadataRequestParameters(final URL domain, final DRSMetadata metadata) {
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
    DRSMetadata getDrsMetadata() {
        return mMetadata;
    }
}
