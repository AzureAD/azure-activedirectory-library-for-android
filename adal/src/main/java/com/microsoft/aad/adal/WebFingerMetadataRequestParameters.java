package com.microsoft.aad.adal;

import java.net.URL;

class WebFingerMetadataRequestParameters {

    private final URL mDomain;

    private final DrsMetadata mMetadata;

    WebFingerMetadataRequestParameters(URL mDomain, DrsMetadata mMetadata) {
        this.mDomain = mDomain;
        this.mMetadata = mMetadata;
    }

    URL getDomain() {
        return mDomain;
    }

    DrsMetadata getDrsMetadata() {
        return mMetadata;
    }
}
