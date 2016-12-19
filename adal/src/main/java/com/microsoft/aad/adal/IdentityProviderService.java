package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

/**
 * Data container for the IdentityProviderService.
 *
 * @see DRSMetadata
 */
final class IdentityProviderService {

    @SerializedName("PassiveAuthEndpoint")
    private String mPassiveAuthEndpoint;

    /**
     * Gets the PassiveAuthEndpoint.
     *
     * @return the PassiveAuthEndpoint
     */
    String getPassiveAuthEndpoint() {
        return mPassiveAuthEndpoint;
    }

    /**
     * Sets the PassiveAuthEndpoint.
     *
     * @param passiveAuthEndpoint the PassiveAuthEndpoint to set
     */
    void setPassiveAuthEndpoint(String passiveAuthEndpoint) {
        this.mPassiveAuthEndpoint = passiveAuthEndpoint;
    }
}
