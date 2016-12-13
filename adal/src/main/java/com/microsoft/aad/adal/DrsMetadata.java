package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

/**
 * Data container for the DRS discovery document.
 */
class DrsMetadata {

    @SerializedName("IdentityProviderService")
    private IdentityProviderService mIdentityProviderService;

    /**
     * Gets the IdentityProviderService.
     *
     * @return the IdentityProviderService
     */
    public IdentityProviderService getIdentityProviderService() {
        return mIdentityProviderService;
    }

    /**
     * Sets the IdentityProviderService.
     *
     * @param identityProviderService the IdentityProviderService to set
     */
    public void setIdentityProviderService(IdentityProviderService identityProviderService) {
        this.mIdentityProviderService = identityProviderService;
    }
}
