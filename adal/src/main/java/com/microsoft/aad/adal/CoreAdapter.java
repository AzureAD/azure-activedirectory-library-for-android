package com.microsoft.aad.adal;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ServiceException;
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
                account.getLastName(),
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
        adTokenResponse.setClientId(result.getClientId());
        // TODO populate other missing fields...
        // TODO set the clientId on the TokenResponse
        return adTokenResponse;

    }


    public static AuthenticationException asAuthenticationException(BaseException ex) {
        AuthenticationException newException = ADALError.fromCommon(ex);
        if (ex instanceof ServiceException) {
            ServiceException serviceException = (ServiceException) ex;
            newException.setHttpResponseBody(serviceException.getHttpResponseBody());
            newException.setHttpResponseHeaders(serviceException.getHttpResponseHeaders());
            newException.setServiceStatusCode(serviceException.getHttpStatusCode());
        }
        return newException;
    }


    public static AzureActiveDirectoryCloud asAadCloud(final InstanceDiscoveryMetadata cloud) {
        final AzureActiveDirectoryCloud adCloud = new AzureActiveDirectoryCloud(cloud.getPreferredNetwork(), cloud.getPreferredCache(), cloud.getAliases());
        return adCloud;
    }

}
