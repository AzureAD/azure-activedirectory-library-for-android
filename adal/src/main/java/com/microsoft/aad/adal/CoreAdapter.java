package com.microsoft.aad.adal;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryTokenResponse;

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
                account.getUserIdentifier(),
                account.getName(),
                null, // TODO Need to get the 'family name' attribute
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
        adTokenResponse.setExpiresOn(String.valueOf(result.getExpiresOn().getTime()));
        adTokenResponse.setExtExpiresIn(String.valueOf(result.getExtendedExpiresOn().getTime()));
        adTokenResponse.setIdToken(result.getIdToken());
        // TODO populate other missing fields...
        return adTokenResponse;
    }

}
