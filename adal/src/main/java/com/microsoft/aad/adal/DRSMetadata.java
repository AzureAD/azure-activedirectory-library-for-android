package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

/**
 * Data container for the DRS discovery document.
 */
final class DRSMetadata {

    @SerializedName("IdentityProviderService")
    private IdentityProviderService mIdentityProviderService;

    /**
     * Gets the IdentityProviderService.
     *
     * @return the IdentityProviderService
     */
    IdentityProviderService getIdentityProviderService() {
        return mIdentityProviderService;
    }

    /**
     * Sets the IdentityProviderService.
     *
     * @param identityProviderService the IdentityProviderService to set
     */
    void setIdentityProviderService(IdentityProviderService identityProviderService) {
        this.mIdentityProviderService = identityProviderService;
    }
}
