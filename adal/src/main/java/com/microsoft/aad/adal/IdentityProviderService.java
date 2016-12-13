package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

/**
 * Data container for the IdentityProviderService.
 *
 * @see DrsMetadata
 */
class IdentityProviderService {

    @SerializedName("PassiveAuthEndpoint")
    private String mPassiveAuthEndpoint;

    /**
     * Gets the PassiveAuthEndpoint.
     *
     * @return the PassiveAuthEndpoint
     */
    public String getPassiveAuthEndpoint() {
        return mPassiveAuthEndpoint;
    }

    /**
     * Sets the PassiveAuthEndpoint.
     *
     * @param passiveAuthEndpoint the PassiveAuthEndpoint to set
     */
    public void setPassiveAuthEndpoint(String passiveAuthEndpoint) {
        this.mPassiveAuthEndpoint = passiveAuthEndpoint;
    }
}
