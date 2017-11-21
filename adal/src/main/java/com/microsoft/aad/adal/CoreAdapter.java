package com.microsoft.aad.adal;

import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryTokenResponse;

/**
 * Utility class for object transformations between :common and :adal.
 */
final class CoreAdapter {

    private CoreAdapter() {
        // Util class. No reason to instantiate.
    }

    /**
     * Gets the supplied AuthenticationResult as a {@link AzureActiveDirectoryTokenResponse}.
     *
     * @return The newly created {@link AzureActiveDirectoryTokenResponse}.
     */
    public static AzureActiveDirectoryTokenResponse asAADTokenResponse(final AuthenticationResult result) {
        final AzureActiveDirectoryTokenResponse adTokenResponse = new AzureActiveDirectoryTokenResponse();
        adTokenResponse.setAccessToken(result.getAccessToken());
        adTokenResponse.setTokenType(result.getAccessTokenType());
        adTokenResponse.setRefreshToken(result.getRefreshToken());
        adTokenResponse.setExpiresOn(String.valueOf(result.getExpiresOn().getTime()));
        adTokenResponse.setIdToken(result.getIdToken());
        // TODO populate other missing fields...
        return adTokenResponse;
    }

}
