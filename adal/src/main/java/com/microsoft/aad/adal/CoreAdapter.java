// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.aad.adal;

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
     * Gets the supplied {@link UserInfo} as an {@link AzureActiveDirectoryAccount}.
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
     * Gets the supplied {@link AzureActiveDirectoryAccount} as a {@link UserInfo}.
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
        adTokenResponse.setClientId(result.getClientId());
        adTokenResponse.setResource(result.getResource());

        // This is kind of weird, but because the v1 endpoint doesn't have any notion of scopes
        // we treat the resource as the scope and, in cases where this issued-token is sent to
        // AAD v2, we append '/.default' to end of the resource value
        adTokenResponse.setScope(result.getResource());

        if (null != result.getClientInfo()) {
            adTokenResponse.setClientInfo(result.getClientInfo().getRawClientInfo());
        }

        // TODO populate other missing fields...
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
