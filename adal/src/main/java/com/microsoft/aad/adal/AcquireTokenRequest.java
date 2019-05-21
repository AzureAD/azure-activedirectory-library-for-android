//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.adal;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Internal class for handling acquireToken logic, including the silent flow and interactive flow.
 */
class AcquireTokenRequest {

    private static final String TAG = AcquireTokenRequest.class.getSimpleName();

    /**
     * Singled threaded Executor for async work.
     */
    private static final ExecutorService THREAD_EXECUTOR = Executors.newSingleThreadExecutor();

    private final Context mContext;
    private final AuthenticationContext mAuthContext;
    private TokenCacheAccessor mTokenCacheAccessor;
    private final IBrokerProxy mBrokerProxy;

    private static Handler sHandler = null;

    private static final long CP_LLT_VERSION_CODE = 2950722;

    private static final long AUTHENTICATOR_LLT_VERSION_CODE = 138;

    /**
     * Instance validation related calls are serviced inside Discovery as a
     * module.
     */
    private Discovery mDiscovery;

    /**
     * Event Variable for the acquireToken API called. This will track whether the API succeeded or not.
     */
    private APIEvent mAPIEvent;

    /**
     * Constructor for {@link AcquireTokenRequest}.
     */
    AcquireTokenRequest(final Context appContext, final AuthenticationContext authContext, final APIEvent apiEvent) {
        mContext = appContext;
        mAuthContext = authContext;
        mDiscovery = new Discovery(mContext);

        if (authContext.getCache() != null && apiEvent != null) {
            mTokenCacheAccessor = new TokenCacheAccessor(appContext.getApplicationContext(), authContext.getCache(),
                    authContext.getAuthority(), apiEvent.getTelemetryRequestId());
            mTokenCacheAccessor.setValidateAuthorityHost(mAuthContext.getValidateAuthority());
        }
        mBrokerProxy = new BrokerProxy(appContext);

        mAPIEvent = apiEvent;
    }

    /**
     * Handles the acquire token logic. Will do authority validation first if developer set validateAuthority to be
     * true.
     */
    void acquireToken(final IWindowComponent activity, final boolean useDialog, final AuthenticationRequest authRequest,
                      final AuthenticationCallback<AuthenticationResult> authenticationCallback) {
        final String methodName = ":acquireToken";
        final CallbackHandler callbackHandle = new CallbackHandler(getHandler(), authenticationCallback);
        // Executes all the calls inside the Runnable to return immediately to
        // user. All UI
        // related actions will be performed using Handler.
        Logger.setCorrelationId(authRequest.getCorrelationId());
        Logger.v(TAG + methodName, "Sending async task from thread:" + android.os.Process.myTid());
        THREAD_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                // With the introduction of DiagnosticContext, correlationIds are now tracked
                // per-thread. To support passing the correlationId to the worker thread, we need
                // to call setCorrelationId() again.
                Logger.setCorrelationId(authRequest.getCorrelationId());

                Logger.v(TAG + methodName, "Running task in thread:" + android.os.Process.myTid());
                try {
                    // Validate acquire token call first.
                    validateAcquireTokenRequest(authRequest);
                    performAcquireTokenRequest(callbackHandle, activity, useDialog, authRequest);
                } catch (final AuthenticationException authenticationException) {
                    mAPIEvent.setSpeRing(authenticationException.getSpeRing());
                    mAPIEvent.setRefreshTokenAge(authenticationException.getRefreshTokenAge());
                    mAPIEvent.setServerErrorCode(authenticationException.getCliTelemErrorCode());
                    mAPIEvent.setServerSubErrorCode(authenticationException.getCliTelemSubErrorCode());
                    mAPIEvent.setWasApiCallSuccessful(false, authenticationException);
                    mAPIEvent.setCorrelationId(authRequest.getCorrelationId().toString());
                    mAPIEvent.stopTelemetryAndFlush();

                    callbackHandle.onError(authenticationException);
                }
            }
        });
    }

    /**
     * Developer is using refresh token call to do refresh without cache usage.
     * App context or activity is not needed. Async requests are created, so this
     * needs to be called at UI thread.
     */
    void refreshTokenWithoutCache(final String refreshToken, final AuthenticationRequest authenticationRequest,
                                  final AuthenticationCallback<AuthenticationResult> externalCallback) {
        final String methodName = ":refreshTokenWithoutCache";
        Logger.setCorrelationId(authenticationRequest.getCorrelationId());
        Logger.v(TAG + methodName, "Refresh token without cache");

        final CallbackHandler callbackHandle = new CallbackHandler(getHandler(), externalCallback);

        // Execute all the calls inside Runnable to return immediately. All UI
        // related actions will be performed using Handler.
        THREAD_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // validate acquire token call first.
                    validateAcquireTokenRequest(authenticationRequest);

                    final AcquireTokenSilentHandler acquireTokenSilentHandler = new AcquireTokenSilentHandler(mContext,
                            authenticationRequest, mTokenCacheAccessor);
                    final AuthenticationResult authResult
                            = acquireTokenSilentHandler.acquireTokenWithRefreshToken(refreshToken);
                    mAPIEvent.setWasApiCallSuccessful(true, null);
                    mAPIEvent.setIdToken(authResult.getIdToken());
                    callbackHandle.onSuccess(authResult);
                } catch (final AuthenticationException authenticationException) {
                    mAPIEvent.setWasApiCallSuccessful(false, authenticationException);
                    callbackHandle.onError(authenticationException);
                } finally {
                    mAPIEvent.setCorrelationId(authenticationRequest.getCorrelationId().toString());
                    mAPIEvent.stopTelemetryAndFlush();
                }
            }
        });
    }

    private void validateAcquireTokenRequest(final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {
        final URL authorityUrl = StringExtensions.getUrl(authenticationRequest.getAuthority());
        if (authorityUrl == null) {
            throw new AuthenticationException(
                    ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL);
        }

        // validate authority
        performAuthorityValidation(authenticationRequest, authorityUrl);

        // Verify broker redirect uri for non-silent request
        final BrokerProxy.SwitchToBroker canSwitchToBrokerFlag = mBrokerProxy.canSwitchToBroker(authenticationRequest.getAuthority());

        if (canSwitchToBrokerFlag != BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER
                && mBrokerProxy.verifyUser(authenticationRequest.getLoginHint(), authenticationRequest.getUserId())
                && !authenticationRequest.isSilent()) {

            if (canSwitchToBrokerFlag == BrokerProxy.SwitchToBroker.NEED_PERMISSIONS_TO_SWITCH_TO_BROKER) {
                throw new UsageAuthenticationException(
                        ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING,
                        "Broker related permissions are missing for GET_ACCOUNTS.");
            }
            // Verify redirect uri match what we expecting for broker non-silent request. For silent request,
            // since we don't require developer to pass us the redirect uri, we won't perform the validation.
            verifyBrokerRedirectUri(authenticationRequest);
        }
    }

    private void performAuthorityValidation(final AuthenticationRequest authenticationRequest, final URL authorityUrl)
            throws AuthenticationException {
        // validate authority
        final String methodName = ":performAuthorityValidation";
        Telemetry.getInstance().startEvent(authenticationRequest.getTelemetryRequestId(),
                EventStrings.AUTHORITY_VALIDATION_EVENT);
        APIEvent apiEvent = new APIEvent(EventStrings.AUTHORITY_VALIDATION_EVENT);
        apiEvent.setCorrelationId(authenticationRequest.getCorrelationId().toString());
        apiEvent.setRequestId(authenticationRequest.getTelemetryRequestId());

        if (mAuthContext.getValidateAuthority()) {
            try {
                validateAuthority(authorityUrl, authenticationRequest.getUpnSuffix(), authenticationRequest.isSilent(),
                        authenticationRequest.getCorrelationId());
                apiEvent.setValidationStatus(EventStrings.AUTHORITY_VALIDATION_SUCCESS);
            } catch (final AuthenticationException authenticationException) {
                if (null != authenticationException.getCode()
                        && (authenticationException.getCode().equals(ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE)
                        || authenticationException.getCode().equals(ADALError.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION))) {
                    // The authority validation is not done because of network error.
                    apiEvent.setValidationStatus(EventStrings.AUTHORITY_VALIDATION_NOT_DONE);
                } else {
                    apiEvent.setValidationStatus(EventStrings.AUTHORITY_VALIDATION_FAILURE);
                }

                throw authenticationException;
            } finally {
                Telemetry.getInstance().stopEvent(authenticationRequest.getTelemetryRequestId(), apiEvent,
                        EventStrings.AUTHORITY_VALIDATION_EVENT);
            }
        } else {
            // check if it contains authority url as the key, if key exists, it means that the authority url validation has happened
            // for the authority already.
            if (!UrlExtensions.isADFSAuthority(authorityUrl) && !AuthorityValidationMetadataCache.containsAuthorityHost(authorityUrl)) {
                try {
                    mDiscovery.validateAuthority(authorityUrl);
                } catch (final AuthenticationException authenticationException) {
                    // Ignore the failure, save in the map as a failed instance discovery to avoid it being looked up another times in the same process
                    AuthorityValidationMetadataCache.updateInstanceDiscoveryMap(authorityUrl.getHost(), new InstanceDiscoveryMetadata(false));
                    AzureActiveDirectory.putCloud(authorityUrl.getHost(), new AzureActiveDirectoryCloud(false));
                    Logger.v(TAG + methodName, "Fail to get authority validation metadata back. Ignore the failure since authority validation is turned off.");
                }
            }
            // Even if it succeeds, we cannot mark the authority as validated authority.
            apiEvent.setValidationStatus(EventStrings.AUTHORITY_VALIDATION_NOT_DONE);
            Telemetry.getInstance().stopEvent(authenticationRequest.getTelemetryRequestId(), apiEvent,
                    EventStrings.AUTHORITY_VALIDATION_EVENT);
        }

        final InstanceDiscoveryMetadata metadata = AuthorityValidationMetadataCache.getCachedInstanceDiscoveryMetadata(authorityUrl);
        if (metadata == null || !metadata.isValidated()) {
            return;
        }

        updatePreferredNetworkLocation(authorityUrl, authenticationRequest, metadata);
    }

    private void updatePreferredNetworkLocation(final URL authorityUrl, final AuthenticationRequest request, final InstanceDiscoveryMetadata metadata)
            throws AuthenticationException {
        if (metadata == null || !metadata.isValidated()) {
            return;
        }

        // replace the authority if host is not the same as the original one.
        if (metadata.getPreferredNetwork() != null && !authorityUrl.getHost().equalsIgnoreCase(metadata.getPreferredNetwork())) {
            try {
                final URL replacedAuthority = Discovery.constructAuthorityUrl(authorityUrl, metadata.getPreferredNetwork());
                request.setAuthority(replacedAuthority.toString());
            } catch (final MalformedURLException ex) {
                //Intentionally empty.
                Logger.i(TAG, "preferred network is invalid", "use exactly the same authority url that is passed");
            }
        }
    }

    /**
     * Perform authority validation.
     * True if the passed in authority is valid, false otherwise.
     */
    private void validateAuthority(final URL authorityUrl,
                                   @Nullable final String domain,
                                   boolean isSilent,
                                   final UUID correlationId) throws AuthenticationException {
        final String methodName = ":validateAuthority";
        boolean isAdfsAuthority = UrlExtensions.isADFSAuthority(authorityUrl);
        final boolean isAuthorityValidated = AuthorityValidationMetadataCache.isAuthorityValidated(authorityUrl);
        if (isAuthorityValidated || isAdfsAuthority && mAuthContext.getIsAuthorityValidated()) {
            return;
        }

        Logger.v(TAG + methodName, "Start validating authority");
        mDiscovery.setCorrelationId(correlationId);

        Discovery.verifyAuthorityValidInstance(authorityUrl);

        if (!isSilent && isAdfsAuthority && domain != null) {
            mDiscovery.validateAuthorityADFS(authorityUrl, domain);
        } else {
            if (isSilent && UrlExtensions.isADFSAuthority(authorityUrl)) {
                Logger.v(TAG + methodName, "Silent request. Skipping AD FS authority validation");
            }

            mDiscovery.validateAuthority(authorityUrl);
        }

        Logger.v(TAG + methodName, "The passed in authority is valid.");
        mAuthContext.setIsAuthorityValidated(true);
    }

    /**
     * 1. For Silent flow, we should always try to look local cache first.
     * i> If valid AT is returned from cache, use it.
     * ii> If no valid AT is returned, but RT is returned, use the RT.
     * iii> If RT request fails, and if we can talk to broker, go to broker and check if there is a valid token.
     * 2. For Non-Silent flow.
     * i> Do silent cache lookup first, same as 1.
     * a) If we can talk to broker, go to broker for auth.
     * b) If not, launch webview with embedded flow.
     * If silent request succeeds, we'll return the token back via callback.
     * If silent request fails and no prompt is allowed, we'll return the exception back via callback.
     * If silent request fails and prompt is allowed, we'll prompt the user and launch webview.
     */
    private void performAcquireTokenRequest(final CallbackHandler callbackHandle,
                                            final IWindowComponent activity,
                                            final boolean useDialog,
                                            final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {

        // tryAcquireTokenSilent will:
        // 1) throw AuthenticationException if
        //    a) No access token is returned from local flow and no prompt is allowed
        //    b) Server error or network errors when sending post request to token endpoint with grant_type
        //       as refresh_token in local flow
        //    c) Broker returns ERROR_CODE_BAD_ARGUMENTS, ACCOUNT_MANAGER_ERROR_CODE_BAD_AUTHENTICATION
        //       or ERROR_CODE_UNSUPPORTED_OPERATION
        // 2) Non-null AuthenticationResult if we could try silent request, and silent request either successfully
        //    return the token back or returns the AuthenticationResult containing oauth error
        // 3) Null AuthenticationResult if
        //    a) we cannot try silent request
        //    b) silent request returns a null result. Broker will return a null result if 1) no matching account in
        //       broker 2) broker doesn't return any token back.
        final String methodName = ":performAcquireTokenRequest";
        final AuthenticationResult authenticationResultFromSilentRequest = tryAcquireTokenSilent(authenticationRequest);
        if (isAccessTokenReturned(authenticationResultFromSilentRequest)) {
            mAPIEvent.setWasApiCallSuccessful(true, null);
            mAPIEvent.setCorrelationId(authenticationRequest.getCorrelationId().toString());
            mAPIEvent.setIdToken(authenticationResultFromSilentRequest.getIdToken());
            mAPIEvent.stopTelemetryAndFlush();
            callbackHandle.onSuccess(authenticationResultFromSilentRequest);
            return;
        }

        Logger.d(TAG + methodName, "Trying to acquire token interactively.");
        acquireTokenInteractiveFlow(callbackHandle, activity, useDialog, authenticationRequest);
    }

    private AuthenticationResult tryAcquireTokenSilent(final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {
        final String methodName = ":tryAcquireTokenSilent";
        AuthenticationResult authenticationResult = null;

        if (shouldTrySilentFlow(authenticationRequest)) {
            Logger.v(TAG + methodName, "Try to acquire token silently, return valid AT or use RT in the cache.");
            authenticationResult = acquireTokenSilentFlow(authenticationRequest);

            final boolean isAccessTokenReturned = isAccessTokenReturned(authenticationResult);
            // Silent request, if token not returned, return AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED back
            // to developer.
            if (!isAccessTokenReturned && authenticationRequest.isSilent()) {
                // TODO: investigate which server response actually should force user to sign in again
                // and which error actually should just notify user that some resource require extra steps

                final String errorInfo = authenticationResult == null
                        ? "No result returned from acquireTokenSilent" : " ErrorCode:" + authenticationResult.getErrorCode();
                // User does not want to launch activity
                Logger.e(TAG + methodName,
                        "Prompt is not allowed and failed to get token. " + errorInfo,
                        authenticationRequest.getLogInfo(),
                        ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED);
                final AuthenticationException authenticationException = new AuthenticationException(
                        ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED, authenticationRequest.getLogInfo()
                        + " " + errorInfo);

                addHttpInfoToException(authenticationResult, authenticationException);

                throw authenticationException;
            }

            if (isAccessTokenReturned) {
                Logger.v(TAG + methodName, "Token is successfully returned from silent flow. ");
            }
        }

        return authenticationResult;
    }

    private void addHttpInfoToException(AuthenticationResult result, AuthenticationException exception) {
        if (result != null && exception != null) {
            if (result.getHttpResponseHeaders() != null) {
                exception.setHttpResponseHeaders(result.getHttpResponseHeaders());
            }

            if (result.getHttpResponseBody() != null) {
                exception.setHttpResponseBody(result.getHttpResponseBody());
            }
            exception.setServiceStatusCode(result.getServiceStatusCode());
        }
    }

    private boolean shouldTrySilentFlow(final AuthenticationRequest authenticationRequest) {
        boolean result = true;
        if (authenticationRequest.isClaimsChallengePresent()) {
            result = checkIfBrokerHasLltChanges();
        }
        return authenticationRequest.isSilent() || (result && authenticationRequest.getPrompt() == PromptBehavior.Auto);
    }

    /**
     * The previous behavior to always  do an interactive call if claims challenge is present is changed with long live token feature.
     * However to support the case were the broker app is not updated to have Llt changes, we check the version code of the both the broker
     * and retain the old behavior.
     */
    private boolean checkIfBrokerHasLltChanges() {
        PackageManager packageManager = mContext.getPackageManager();
        int authVersionCode = Integer.MAX_VALUE;
        int cpVersionCode = Integer.MAX_VALUE;

        try {
            PackageInfo authPackageInfo = packageManager.getPackageInfo(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, 0);
            authVersionCode = authPackageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        try {
            PackageInfo cpPackageInfo = packageManager.getPackageInfo(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, 0);
            cpVersionCode = cpPackageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return authVersionCode >= AUTHENTICATOR_LLT_VERSION_CODE && cpVersionCode >= CP_LLT_VERSION_CODE;

    }

    /**
     * Handles the silent flow. Will always lookup local cache. If there is a valid AT in local cache, will use it. If
     * AT in local cache is already expired, will try RT in the local cache. If RT requst failed, and if we can switch
     * to broker for auth, will switch to broker for authentication.
     */
    private AuthenticationResult acquireTokenSilentFlow(final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {

        final boolean requestEligibleForBroker = mBrokerProxy.verifyBrokerForSilentRequest(authenticationRequest);

        //1. if forceRefresh == true OR claimsChallenge is not null AND the request is eligible for the broker
        if ((authenticationRequest.getForceRefresh() || authenticationRequest.isClaimsChallengePresent()) && requestEligibleForBroker) {
            return tryAcquireTokenSilentWithBroker(authenticationRequest);
        }

        //2. Try to acquire silent locally
        final AuthenticationResult authResult = tryAcquireTokenSilentLocally(authenticationRequest);
        if (isAccessTokenReturned(authResult)) {
            return authResult;
        }

        //3. We couldn't get locally...If eligible return via broker... otherwise return local result
        if (requestEligibleForBroker) {
            return tryAcquireTokenSilentWithBroker(authenticationRequest);
        } else {
            return authResult;
        }

    }

    /**
     * Try acquire token silent locally.
     */
    private AuthenticationResult tryAcquireTokenSilentLocally(final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {
        final String methodName = ":tryAcquireTokenSilentLocally";
        Logger.v(TAG + methodName, "Try to silently get token from local cache.");
        final AcquireTokenSilentHandler acquireTokenSilentHandler = new AcquireTokenSilentHandler(mContext,
                authenticationRequest, mTokenCacheAccessor);

        return acquireTokenSilentHandler.getAccessToken();
    }

    /**
     * Try acquire token silent with broker.
     */
    private AuthenticationResult tryAcquireTokenSilentWithBroker(final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {

        final String methodName = ":tryAcquireTokenSilentWithBroker";

        // If we can try with broker for silent flow, it indicates ADAL can switch to broker for auth. Even broker does
        // not return the token back silently, and we go to interactive flow, we'll still go to broker. The token in
        // app local cache is no longer useful, when user uninstalls broker, we should prompt user in the next sign-in.
        Logger.d(TAG + methodName, "Either could not get tokens from local cache or is force refresh request, switch to Broker for auth, "
                + "clear tokens from local cache for the user.");
        removeTokensForUser(authenticationRequest);

        final AuthenticationResult authResult;

        final AcquireTokenWithBrokerRequest acquireTokenWithBrokerRequest
                = new AcquireTokenWithBrokerRequest(authenticationRequest, mBrokerProxy);
        authResult = acquireTokenWithBrokerRequest.acquireTokenWithBrokerSilent();

        return authResult;
    }

    private void removeTokensForUser(final AuthenticationRequest request) throws AuthenticationException {
        final String methodName = ":removeTokensForUser";
        if (mTokenCacheAccessor == null) {
            return;
        }

        final String user = !StringExtensions.isNullOrBlank(request.getUserId()) ? request.getUserId()
                : request.getLoginHint();

        // Usually we only clear tokens for a particular user and a particular client id which identifies an app.
        // Family token could be used across multiple apps within the same family, it's a SSO state across those
        // family apps. If we want to clear the tokens for the user(signout the user with local cahce), have the
        // user to sign-in through broker, we also need to clear the family token.
        // Check if there is a FRT existed for the user
        final TokenCacheItem frtItem;
        try {
            frtItem = mTokenCacheAccessor.getFRTItem(AuthenticationConstants.MS_FAMILY_ID, user);
        } catch (final MalformedURLException ex) {
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, ex.getMessage(), ex);
        }

        if (frtItem != null) {
            mTokenCacheAccessor.removeTokenCacheItem(frtItem, request.getResource());
        }

        // Check if there is a MRRT existed for the user, if there is an MRRT, TokenCacheAccessor will also
        // delete the regular RT entry
        // When there is no MRRT token cache item exist, try to check if there is regular RT cache item for the user.
        final TokenCacheItem mrrtItem;
        final TokenCacheItem regularTokenCacheItem;


        try {
            mrrtItem = mTokenCacheAccessor.getMRRTItem(request.getClientId(), user);
            regularTokenCacheItem = mTokenCacheAccessor.getRegularRefreshTokenCacheItem(
                    request.getResource(), request.getClientId(), user);
        } catch (final MalformedURLException ex) {
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, ex.getMessage(), ex);
        }

        if (mrrtItem != null) {
            mTokenCacheAccessor.removeTokenCacheItem(mrrtItem, request.getResource());
        } else if (regularTokenCacheItem != null) {
            mTokenCacheAccessor.removeTokenCacheItem(regularTokenCacheItem, request.getResource());
        } else {
            Logger.v(TAG + methodName, "No token items need to be deleted for the user.");
        }
    }

    /**
     * Handles the acquire token interactive flow. If we can switch to broker, will always launch webview via broker.
     * If we cannot switch to broker, will launch webview locally.
     */
    private void acquireTokenInteractiveFlow(final CallbackHandler callbackHandle,
                                             final IWindowComponent activity,
                                             final boolean useDialog,
                                             final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {
        final String methodName = ":acquireTokenInteractiveFlow";
        if (activity == null && !useDialog) {
            throw new AuthenticationException(
                    ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED, authenticationRequest.getLogInfo()
                    + " Cannot launch webview, activity is null.");
        }

        HttpWebRequest.throwIfNetworkNotAvailable(mContext);

        final int requestId = callbackHandle.getCallback().hashCode();
        authenticationRequest.setRequestId(requestId);
        mAuthContext.putWaitingRequest(requestId, new AuthenticationRequestState(requestId, authenticationRequest,
                callbackHandle.getCallback(), mAPIEvent));
        final BrokerProxy.SwitchToBroker switchToBrokerFlag = mBrokerProxy.canSwitchToBroker(authenticationRequest.getAuthority());

        if (switchToBrokerFlag != BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER
                && mBrokerProxy.verifyUser(authenticationRequest.getLoginHint(), authenticationRequest.getUserId())) {

            if (switchToBrokerFlag == BrokerProxy.SwitchToBroker.NEED_PERMISSIONS_TO_SWITCH_TO_BROKER) {
                throw new UsageAuthenticationException(
                        ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING,
                        "Broker related permissions are missing for GET_ACCOUNTS");
            }

            // Always go to broker if the sdk can talk to broker for interactive flow
            Logger.v(TAG + methodName, "Launch activity for interactive authentication via broker with callback. ",
                    "" + callbackHandle.getCallback().hashCode(), null);
            final AcquireTokenWithBrokerRequest acquireTokenWithBrokerRequest
                    = new AcquireTokenWithBrokerRequest(authenticationRequest, mBrokerProxy);

            acquireTokenWithBrokerRequest.acquireTokenWithBrokerInteractively(activity);
        } else {
            Logger.v(TAG + methodName, "Starting Authentication Activity for embedded flow. ",
                    " Callback is: " + callbackHandle.getCallback().hashCode(), null);
            final AcquireTokenInteractiveRequest acquireTokenInteractiveRequest
                    = new AcquireTokenInteractiveRequest(mContext, authenticationRequest, mTokenCacheAccessor);
            acquireTokenInteractiveRequest.acquireToken(activity,
                    useDialog ? new AuthenticationDialog(getHandler(), mContext, this, authenticationRequest)
                            : null);
        }
    }

    /**
     * Check the redirectUri before sending the request.
     * If the redirectUri from the client does not match the valid redirectUri, the client app would not jump
     * to the login page. redirectUri format %PREFIX://%PACKAGE_NAME/%SIGNATURE
     */
    private void verifyBrokerRedirectUri(final AuthenticationRequest request) throws UsageAuthenticationException {
        final String methodName = ":verifyBrokerRedirectUri";
        final String inputUri = request.getRedirectUri();
        final String actualRedirectUri = mAuthContext.getRedirectUriForBroker();

        final String errMsg;
        // verify the redirect uri passed in by developer is non-null and non-blank
        if (StringExtensions.isNullOrBlank(inputUri)) {
            Logger.e(TAG + methodName, "The redirectUri is null or blank. ", "The redirect uri is expected to be:" + actualRedirectUri, ADALError.DEVELOPER_REDIRECTURI_INVALID);
            throw new UsageAuthenticationException(ADALError.DEVELOPER_REDIRECTURI_INVALID, "The redirectUri is null or blank.");
        }

        if (inputUri.equalsIgnoreCase(AuthenticationConstants.Broker.BROKER_REDIRECT_URI)) {
            // TODO: Clean this up once we migrate all Logger functions to the common one.
            com.microsoft.identity.common.internal.logging.Logger.info(TAG + methodName, "This is a broker redirectUri. Bypass the check.");
            return;
        }

        // verify that redirect uri passed in by developer has the correct prefix msauth://
        if (!inputUri.startsWith(AuthenticationConstants.Broker.REDIRECT_PREFIX + "://")) {
            errMsg = " The valid broker redirect URI prefix: " + AuthenticationConstants.Broker.REDIRECT_PREFIX
                    + " so the redirect uri is expected to be: " + actualRedirectUri;
            Logger.e(TAG + methodName, "The prefix of the redirect uri does not match the expected value. ", errMsg, ADALError.DEVELOPER_REDIRECTURI_INVALID);
            throw new UsageAuthenticationException(ADALError.DEVELOPER_REDIRECTURI_INVALID, "The prefix of the redirect uri does not match the expected value.");
        }

        // verify that redirect uri passed in by developer has the expected package name and signature
        final String base64URLEncodePackagename;
        final String base64URLEncodeSignature;
        final PackageHelper packageHelper = new PackageHelper(mContext);
        try {
            base64URLEncodePackagename = URLEncoder.encode(mContext.getPackageName(),
                    AuthenticationConstants.ENCODING_UTF8);
            base64URLEncodeSignature = URLEncoder.encode(
                    packageHelper.getCurrentSignatureForPackage(mContext.getPackageName()),
                    AuthenticationConstants.ENCODING_UTF8);
        } catch (final UnsupportedEncodingException e) {
            Logger.e(TAG + methodName, ADALError.ENCODING_IS_NOT_SUPPORTED.getDescription(), e.getMessage(), ADALError.ENCODING_IS_NOT_SUPPORTED, e);
            throw new UsageAuthenticationException(ADALError.ENCODING_IS_NOT_SUPPORTED, "The verifying "
                    + "BrokerRedirectUri process failed because the base64 url encoding is not supported.", e);
        }

        // verify package name
        if (!inputUri.startsWith(
                AuthenticationConstants.Broker.REDIRECT_PREFIX + "://" + base64URLEncodePackagename + "/")) {
            errMsg = "This apps package name is: " + base64URLEncodePackagename
                    + " so the redirect uri is expected to be: " + actualRedirectUri;
            Logger.e(TAG + methodName,
                    "The base64 url encoded package name component of the redirect uri does not match the expected value. ",
                    errMsg,
                    ADALError.DEVELOPER_REDIRECTURI_INVALID);
            throw new UsageAuthenticationException(ADALError.DEVELOPER_REDIRECTURI_INVALID,
                    "The base64 url encoded package name component of the redirect uri does not match the expected value. ");
        }

        // last thing is to make sure the signature matches
        if (!inputUri.equalsIgnoreCase(actualRedirectUri)) {
            errMsg = "This apps signature is: " + base64URLEncodeSignature
                    + " so the redirect uri is expected to be: " + actualRedirectUri;
            Logger.e(TAG + methodName,
                    "The base64 url encoded signature component of the redirect uri does not match the expected value. ",
                    errMsg,
                    ADALError.DEVELOPER_REDIRECTURI_INVALID);
            throw new UsageAuthenticationException(ADALError.DEVELOPER_REDIRECTURI_INVALID,
                    "The base64 url encoded signature component of the redirect uri does not match the expected value.");
        }

        Logger.v(TAG + methodName, "The broker redirect URI is valid.");
    }


    /**
     * This method wraps the implementation for onActivityResult at the related
     * Activity class. This method is called at UI thread.
     *
     * @param resultCode Result code set from the activity.
     * @param data       {@link Intent}
     */
    void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final String methodName = ":onActivityResult";
        // This is called at UI thread when Activity sets result back.
        // ResultCode is set back from AuthenticationActivity. RequestCode is
        // set when we start the activity for result.
        if (requestCode == AuthenticationConstants.UIRequest.BROWSER_FLOW) {
            getHandler();

            if (data == null || data.getExtras() == null) {
                // If data is null, RequestId is unknown. It could not find
                // callback to respond to this request.
                Logger.e(TAG + methodName, "BROWSER_FLOW data is null.", "",
                        ADALError.ON_ACTIVITY_RESULT_INTENT_NULL);
            } else {
                final Bundle extras = data.getExtras();
                final int requestId = extras.getInt(AuthenticationConstants.Browser.REQUEST_ID);

                final AuthenticationRequestState waitingRequest;
                try {
                    waitingRequest = mAuthContext.getWaitingRequest(requestId);
                    Logger.v(TAG + methodName, "Waiting request found. " + "RequestId:" + requestId);
                } catch (final AuthenticationException authenticationException) {
                    Logger.e(TAG + methodName,
                            "Failed to find waiting request. " + "RequestId:" + requestId,
                            "",
                            ADALError.ON_ACTIVITY_RESULT_INTENT_NULL);
                    return;
                }

                // Cancel or browser error can use recorded request to figure
                // out original correlationId send with request.
                final String correlationInfo = mAuthContext.getCorrelationInfoFromWaitingRequest(waitingRequest);
                if (resultCode == AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE) {
                    final String accessToken = data
                            .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN);
                    final String accountName = data
                            .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_NAME);
                    mBrokerProxy.saveAccount(accountName);
                    final long expireTime = data.getLongExtra(
                            AuthenticationConstants.Broker.ACCOUNT_EXPIREDATE, 0);
                    final Date expire = new Date(expireTime);
                    final String idtoken = data.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_IDTOKEN);
                    final String tenantId = data.getStringExtra(
                            AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID);
                    final UserInfo userinfo = UserInfo.getUserInfoFromBrokerResult(data.getExtras());

                    // grab the fields tracked by x-ms-clitelem
                    final String serverErrorCode = data.getStringExtra(AuthenticationConstants.Broker.CliTelemInfo.SERVER_ERROR);
                    final String serverSubErrorCode = data.getStringExtra(AuthenticationConstants.Broker.CliTelemInfo.SERVER_SUBERROR);
                    final String refreshTokenAge = data.getStringExtra(AuthenticationConstants.Broker.CliTelemInfo.RT_AGE);
                    final String speRingInfo = data.getStringExtra(AuthenticationConstants.Broker.CliTelemInfo.SPE_RING);

                    // Use the waitingRequest to get the original request to get the clientId
                    final AuthenticationRequest originalRequest = waitingRequest.getRequest();
                    String clientId = null;

                    if (null != originalRequest) {
                        clientId = originalRequest.getClientId();
                    }

                    // create the broker AuthenticationResult
                    final AuthenticationResult brokerResult =
                            new AuthenticationResult(
                                    accessToken,
                                    null,
                                    expire,
                                    false,
                                    userinfo,
                                    tenantId,
                                    idtoken,
                                    null,
                                    clientId
                            );
                    final String authority = data.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY);
                    brokerResult.setAuthority(authority);

                    // set the x-ms-clitelem fields on the result from the Broker
                    final TelemetryUtils.CliTelemInfo cliTelemInfo = new TelemetryUtils.CliTelemInfo();
                    cliTelemInfo._setServerErrorCode(serverErrorCode);
                    cliTelemInfo._setServerSubErrorCode(serverSubErrorCode);
                    cliTelemInfo._setRefreshTokenAge(refreshTokenAge);
                    cliTelemInfo._setSpeRing(speRingInfo);
                    brokerResult.setCliTelemInfo(cliTelemInfo);

                    if (brokerResult.getAccessToken() != null) {
                        waitingRequest.getAPIEvent().setWasApiCallSuccessful(true, null);
                        waitingRequest.getAPIEvent().setCorrelationId(
                                waitingRequest.getRequest().getCorrelationId().toString());
                        waitingRequest.getAPIEvent().setIdToken(brokerResult.getIdToken());

                        // add the x-ms-clitelem info to the ApiEvent
                        waitingRequest.getAPIEvent().setServerErrorCode(cliTelemInfo.getServerErrorCode());
                        waitingRequest.getAPIEvent().setServerSubErrorCode(cliTelemInfo.getServerSubErrorCode());
                        waitingRequest.getAPIEvent().setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
                        waitingRequest.getAPIEvent().setSpeRing(cliTelemInfo.getSpeRing());

                        // stop the event
                        waitingRequest.getAPIEvent().stopTelemetryAndFlush();

                        waitingRequest.getDelegate().onSuccess(brokerResult);
                    }
                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL) {
                    // User cancelled the flow by clicking back button or
                    // activating another activity
                    Logger.v(TAG + methodName, "User cancelled the flow. "
                            + "RequestId:" + requestId
                            + " " + correlationInfo);
                    waitingRequestOnError(waitingRequest, requestId, new AuthenticationCancelError(
                            "User cancelled the flow RequestId:" + requestId + correlationInfo));
                } else if (resultCode == AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME) {
                    Logger.v(TAG + methodName, "Device needs to have broker installed, we expect the apps to call us"
                            + "back when the broker is installed");

                    waitingRequestOnError(waitingRequest, requestId,
                            new AuthenticationException(ADALError.BROKER_APP_INSTALLATION_STARTED));
                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION) {
                    Serializable authException = extras
                            .getSerializable(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION);
                    if (authException != null && authException instanceof AuthenticationException) {
                        AuthenticationException exception = (AuthenticationException) authException;
                        Logger.w(TAG + methodName, "Webview returned exception.", exception.getMessage(),
                                ADALError.WEBVIEW_RETURNED_AUTHENTICATION_EXCEPTION);
                        waitingRequestOnError(waitingRequest, requestId, exception);
                    } else {
                        waitingRequestOnError(
                                waitingRequest,
                                requestId,
                                new AuthenticationException(
                                        ADALError.WEBVIEW_RETURNED_INVALID_AUTHENTICATION_EXCEPTION, correlationInfo));
                    }
                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR) {
                    String errCode = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE);
                    String errMessage = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE);
                    Logger.v(TAG + methodName, "Error info:" + errCode + " for requestId: "
                            + requestId + " " + correlationInfo, errMessage, null);

                    final String message = String.format("%s %s %s", errCode, errMessage, correlationInfo);
                    if (!StringExtensions.isNullOrBlank(errCode) &&
                            ADALError.AUTH_FAILED_INTUNE_POLICY_REQUIRED.name().compareTo(errCode) == 0) {
                        final String accountUpn = extras.getString(AuthenticationConstants.Broker.ACCOUNT_NAME);
                        final String accountId = extras.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID);
                        final String tenantId = extras.getString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_TENANTID);
                        final String authority = extras.getString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY);

                        AuthenticationException intuneException = new IntuneAppProtectionPolicyRequiredException(message, accountUpn, accountId, tenantId, authority);
                        waitingRequestOnError(waitingRequest, requestId, intuneException);
                    } else {
                        waitingRequestOnError(waitingRequest, requestId, new AuthenticationException(
                                ADALError.SERVER_INVALID_REQUEST, message));
                    }
                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE) {
                    final AuthenticationRequest authenticationRequest = (AuthenticationRequest) extras
                            .getSerializable(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO);
                    final String endingUrl = extras
                            .getString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, "");
                    if (endingUrl.isEmpty()) {
                        final StringBuilder exceptionMessage =
                                new StringBuilder("Webview did not reach the redirectUrl. ");
                        if (authenticationRequest != null) {
                            exceptionMessage.append(authenticationRequest.getLogInfo());
                        }
                        exceptionMessage.append(correlationInfo);

                        AuthenticationException e = new AuthenticationException(
                                ADALError.WEBVIEW_RETURNED_EMPTY_REDIRECT_URL, exceptionMessage.toString());
                        Logger.e(TAG + methodName, "", e.getMessage(), e.getCode());
                        waitingRequestOnError(waitingRequest, requestId, e);
                    } else {
                        // Browser has the url and it will exchange auth code
                        // for token
                        final CallbackHandler callbackHandle = new CallbackHandler(getHandler(),
                                waitingRequest.getDelegate());

                        // Executes all the calls inside the Runnable to return
                        // immediately to
                        // UI thread. All UI
                        // related actions will be performed using the Handler.
                        THREAD_EXECUTOR.execute(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    final AcquireTokenInteractiveRequest acquireTokenInteractiveRequest
                                            = new AcquireTokenInteractiveRequest(mContext, waitingRequest.getRequest(),
                                            mTokenCacheAccessor);
                                    final AuthenticationResult authenticationResult
                                            = acquireTokenInteractiveRequest.acquireTokenWithAuthCode(endingUrl);

                                    waitingRequest.getAPIEvent().setWasApiCallSuccessful(true, null);
                                    waitingRequest.getAPIEvent().setCorrelationId(
                                            waitingRequest.getRequest().getCorrelationId().toString());
                                    waitingRequest.getAPIEvent().setIdToken(authenticationResult.getIdToken());
                                    waitingRequest.getAPIEvent().stopTelemetryAndFlush();

                                    if (waitingRequest.getDelegate() != null) {
                                        Logger.v(TAG + methodName, "Sending result to callback. ",
                                                waitingRequest.getRequest().getLogInfo(), null);
                                        callbackHandle.onSuccess(authenticationResult);
                                    }
                                } catch (final AuthenticationException authenticationException) {
                                    final StringBuilder message
                                            = new StringBuilder(authenticationException.getMessage());
                                    if (authenticationException.getCause() != null) {
                                        message.append(authenticationException.getCause().getMessage());
                                    }

                                    Logger.e(TAG + methodName,
                                            authenticationException.getCode() == null ? ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN.getDescription() : authenticationException.getCode().getDescription(),
                                            message.toString()
                                                    + ' ' + ExceptionExtensions.getExceptionMessage(authenticationException)
                                                    + ' ' + Log.getStackTraceString(authenticationException),
                                            ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN,
                                            null);
                                    waitingRequestOnError(callbackHandle, waitingRequest, requestId, authenticationException);
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private boolean isAccessTokenReturned(final AuthenticationResult authResult) {
        return authResult != null && !StringExtensions.isNullOrBlank(authResult.getAccessToken());
    }

    private synchronized Handler getHandler() {
        if (sHandler == null) {
            sHandler = new Handler(Looper.getMainLooper());
        }

        return sHandler;
    }

    private void waitingRequestOnError(final AuthenticationRequestState waitingRequest,
                                       int requestId, AuthenticationException exc) {

        waitingRequestOnError(null, waitingRequest, requestId, exc);
    }

    private void waitingRequestOnError(final CallbackHandler handler, final AuthenticationRequestState waitingRequest,
                                       final int requestId, final AuthenticationException exc) {
        final String methodName = ":waitingRequestOnError";
        try {
            if (waitingRequest != null && waitingRequest.getDelegate() != null) {
                Logger.v(TAG + methodName, "Sending error to callback"
                        + mAuthContext.getCorrelationInfoFromWaitingRequest(waitingRequest));
                waitingRequest.getAPIEvent().setWasApiCallSuccessful(false, exc);
                waitingRequest.getAPIEvent().setCorrelationId(
                        waitingRequest.getRequest().getCorrelationId().toString());
                waitingRequest.getAPIEvent().stopTelemetryAndFlush();

                if (handler != null) {
                    handler.onError(exc);
                } else {
                    waitingRequest.getDelegate().onError(exc);
                }
            }
        } finally {
            if (exc != null) {
                mAuthContext.removeWaitingRequest(requestId);
            }
        }
    }

    private static class CallbackHandler {
        private Handler mRefHandler;

        private AuthenticationCallback<AuthenticationResult> mCallback;

        CallbackHandler(Handler ref, AuthenticationCallback<AuthenticationResult> callbackExt) {
            mRefHandler = ref;
            mCallback = callbackExt;
        }

        public void onError(final AuthenticationException e) {
            if (mCallback != null) {
                if (mRefHandler != null) {
                    mRefHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onError(e);
                        }
                    });
                } else {
                    mCallback.onError(e);
                }
            }
        }

        public void onSuccess(final AuthenticationResult result) {
            if (mCallback != null) {
                if (mRefHandler != null) {
                    mRefHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.onSuccess(result);
                        }
                    });
                } else {
                    mCallback.onSuccess(result);
                }
            }
        }

        AuthenticationCallback<AuthenticationResult> getCallback() {
            return mCallback;
        }
    }
}
