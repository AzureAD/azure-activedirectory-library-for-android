package com.microsoft.aad.adal;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryTokenResponse;

/**
 * Utility class for object transformations between :common and :adal.
 */
final class CoreAdapter {

    private CoreAdapter() {
        // Util class. No reason to instantiate.
    }

    /**
     * Gets the supplied {@link UserInfo} as an {@link Account}.
     *
     * @param userInfo The UserInfo to transform.
     * @return The newly created Account.
     */
    public static AzureActiveDirectoryAccount asAadAccount(final UserInfo userInfo) {
        final AzureActiveDirectoryAccount account = new AzureActiveDirectoryAccount();
        account.setDisplayableId(userInfo.getDisplayableId());
        account.setName(userInfo.getGivenName());
        account.setIdentityProvider(account.getIdentityProvider());
        account.setUid(userInfo.getUserId());
        // TODO Need to get the UTID attribute.
        return account;
    }

    /**
     * Gets the supplied {@link Account} as a {@link UserInfo}.
     *
     * @param account The Account to transform.
     * @return The newly created UserInfo.
     */
    public static UserInfo asUserInfo(final AzureActiveDirectoryAccount account) {
        return new UserInfo(
                account.getUserId(),
                account.getName(),
                account.getFamilyName(),
                account.getIdentityProvider(),
                account.getDisplayableId()
        );
    }

    /**
     * Gets the supplied {@link AuthenticationResult} as a {@link AzureActiveDirectoryTokenResponse}.
     *
     * @param result The AuthenticationResult to transform.
     * @return The newly created AzureActiveDirectoryTokenResponse.
     */
    public static AzureActiveDirectoryTokenResponse asAadTokenResponse(final AuthenticationResult result) {
        final AzureActiveDirectoryTokenResponse adTokenResponse = new AzureActiveDirectoryTokenResponse();
        adTokenResponse.setAccessToken(result.getAccessToken());
        adTokenResponse.setTokenType(result.getAccessTokenType());
        adTokenResponse.setRefreshToken(result.getRefreshToken());
        adTokenResponse.setExpiresOn(result.getExpiresOn());
        adTokenResponse.setExtExpiresOn(result.getExtendedExpiresOn());
        adTokenResponse.setIdToken(result.getIdToken());
        adTokenResponse.setExpiresIn(result.getExpiresIn());
        adTokenResponse.setResponseReceivedTime(result.getResponseReceived());
        adTokenResponse.setFamilyId(result.getFamilyClientId());
        // TODO populate other missing fields...
        return adTokenResponse;

    }

    public static com.microsoft.aad.adal.AuthenticationException asAuthenticationException(com.microsoft.identity.common.adal.error.AuthenticationException ex) {
        com.microsoft.aad.adal.AuthenticationException newException = new com.microsoft.aad.adal.AuthenticationException(ADALError.fromCommon(ex.getCode()), ex.getMessage(), ex);
        newException.setHttpResponseBody(ex.getHttpResponseBody());
        newException.setHttpResponseHeaders(ex.getHttpResponseHeaders());
        newException.setServiceStatusCode(ex.getServiceStatusCode());
        return newException;
    }


    public static AzureActiveDirectoryCloud asAadCloud(final InstanceDiscoveryMetadata cloud) {
        final AzureActiveDirectoryCloud adCloud = new AzureActiveDirectoryCloud(cloud.getPreferredNetwork(), cloud.getPreferredCache(), cloud.getAliases());
        return adCloud;
    }

}
