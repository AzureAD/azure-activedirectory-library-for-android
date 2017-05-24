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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
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

    private Handler mHandler = null;
    private BrokerResumeResultReceiver mBrokerResumeResultReceiver = null;

    /**
     * Instance validation related calls are serviced inside Discovery as a
     * module.
     */
    private Discovery mDiscovery = new Discovery();

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

        if (authContext.getCache() != null && apiEvent != null) {
            mTokenCacheAccessor = new TokenCacheAccessor(authContext.getCache(),
                    authContext.getAuthority(), apiEvent.getTelemetryRequestId());
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
        final CallbackHandler callbackHandle = new CallbackHandler(getHandler(), authenticationCallback);
        // Executes all the calls inside the Runnable to return immediately to
        // user. All UI
        // related actions will be performed using Handler.
        Logger.setCorrelationId(authRequest.getCorrelationId());
        Logger.v(TAG, "Sending async task from thread:" + android.os.Process.myTid());
        THREAD_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Logger.v(TAG, "Running task in thread:" + android.os.Process.myTid());
                try {
                    // Validate acquire token call first.
                    validateAcquireTokenRequest(authRequest);
                    performAcquireTokenRequest(callbackHandle, activity, useDialog, authRequest);
                } catch (final AuthenticationException authenticationException) {
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
        Logger.setCorrelationId(authenticationRequest.getCorrelationId());
        Logger.v(TAG, "Refresh token without cache");

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
        Telemetry.getInstance().startEvent(authenticationRequest.getTelemetryRequestId(),
                EventStrings.AUTHORITY_VALIDATION_EVENT);
        APIEvent apiEvent = new APIEvent(EventStrings.AUTHORITY_VALIDATION_EVENT);
        apiEvent.setCorrelationId(authenticationRequest.getCorrelationId().toString());
        apiEvent.setRequestId(authenticationRequest.getTelemetryRequestId());

        if (mAuthContext.getValidateAuthority()) {
            try {
                validateAuthority(authorityUrl, authenticationRequest.getUpnSuffix(), authenticationRequest.isSilent(), authenticationRequest.getCorrelationId());
                apiEvent.setValidationStatus(EventStrings.AUTHORITY_VALIDATION_SUCCESS);
            } catch (AuthenticationException ex) {
                apiEvent.setValidationStatus(EventStrings.AUTHORITY_VALIDATION_FAILURE);
                throw ex;
            } finally {
                Telemetry.getInstance().stopEvent(authenticationRequest.getTelemetryRequestId(), apiEvent,
                        EventStrings.AUTHORITY_VALIDATION_EVENT);
            }
        } else {
            apiEvent.setValidationStatus(EventStrings.AUTHORITY_VALIDATION_NOT_DONE);
            Telemetry.getInstance().stopEvent(authenticationRequest.getTelemetryRequestId(), apiEvent,
                    EventStrings.AUTHORITY_VALIDATION_EVENT);
        }

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

    /**
     * Perform authority validation.
     * True if the passed in authority is valid, false otherwise.
     */
    private void validateAuthority(final URL authorityUrl,
                                   @Nullable final String domain,
                                   boolean isSilent,
                                   final UUID correlationId) throws AuthenticationException {
        if (mAuthContext.getIsAuthorityValidated()) {
            return;
        }

        Logger.v(TAG, "Start validating authority");
        mDiscovery.setCorrelationId(correlationId);

        Discovery.verifyAuthorityValidInstance(authorityUrl);


        if (!isSilent && UrlExtensions.isADFSAuthority(authorityUrl) && domain != null) {
            mDiscovery.validateAuthorityADFS(authorityUrl, domain);
        } else {
            if (isSilent && UrlExtensions.isADFSAuthority(authorityUrl)) {
                Logger.v(TAG, "Silent request. Skipping AD FS authority validation");
            }
            mDiscovery.validateAuthority(authorityUrl);
        }

        Logger.v(TAG, "The passed in authority is valid.");
        mAuthContext.setIsAuthorityValidated(true);
    }

    /**
     * 1. For Silent flow, we should always try to look local cache first.
     *    i> If valid AT is returned from cache, use it.
     *    ii> If no valid AT is returned, but RT is returned, use the RT.
     *    iii> If RT request fails, and if we can talk to broker, go to broker and check if there is a valid token.
     * 2. For Non-Silent flow.
     *    i> Do silent cache lookup first, same as 1.
     *       a) If we can talk to broker, go to broker for auth.
     *       b) If not, launch webview with embedded flow.
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
        final AuthenticationResult authenticationResultFromSilentRequest = tryAcquireTokenSilent(authenticationRequest);
        if (isAccessTokenReturned(authenticationResultFromSilentRequest)) {
            mAPIEvent.setWasApiCallSuccessful(true, null);
            mAPIEvent.setCorrelationId(authenticationRequest.getCorrelationId().toString());
            mAPIEvent.setIdToken(authenticationResultFromSilentRequest.getIdToken());
            mAPIEvent.stopTelemetryAndFlush();
            callbackHandle.onSuccess(authenticationResultFromSilentRequest);
            return;
        }

        Logger.d(TAG, "Trying to acquire token interactively.");
        acquireTokenInteractiveFlow(callbackHandle, activity, useDialog, authenticationRequest);
    }

    private AuthenticationResult tryAcquireTokenSilent(final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {
        AuthenticationResult authenticationResult = null;

        if (shouldTrySilentFlow(authenticationRequest)) {
            Logger.v(TAG, "Try to acquire token silently, return valid AT or use RT in the cache.");
            authenticationResult = acquireTokenSilentFlow(authenticationRequest);

            final boolean isAccessTokenReturned = isAccessTokenReturned(authenticationResult);
            // Silent request, if token not returned, return AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED back
            // to developer.
            if (!isAccessTokenReturned && authenticationRequest.isSilent()) {
                // TODO: investigate which server response actually should force user to sign in again
                // and which error actually should just notify user that some resource require extra steps

                final String errorInfo = authenticationResult == null
                        ? "No result returned from acquireTokenSilent" : authenticationResult.getErrorLogInfo();
                // User does not want to launch activity
                Logger.e(TAG, "Prompt is not allowed and failed to get token:", authenticationRequest.getLogInfo()
                                + " " + errorInfo,
                        ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED);
                throw new AuthenticationException(
                        ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED, authenticationRequest.getLogInfo()
                        + " " + errorInfo);
            }

            if (isAccessTokenReturned) {
                Logger.v(TAG, "Token is successfully returned from silent flow. ");
            }
        }

        return authenticationResult;
    }


    private boolean shouldTrySilentFlow(final AuthenticationRequest authenticationRequest) {
        return authenticationRequest.getPrompt() == PromptBehavior.Auto || authenticationRequest.isSilent();
    }

    /**
     * Handles the silent flow. Will always lookup local cache. If there is a valid AT in local cache, will use it. If
     * AT in local cache is already expired, will try RT in the local cache. If RT requst failed, and if we can switch
     * to broker for auth, will switch to broker for authentication.
     */
    private AuthenticationResult acquireTokenSilentFlow(final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {

        // Always try with local cache first.
        final AuthenticationResult authResult = tryAcquireTokenSilentLocally(authenticationRequest);
        if (isAccessTokenReturned(authResult)) {
            return authResult;
        }

        // If we cannot switch to broker, return the result from local flow.
        final BrokerProxy.SwitchToBroker switchToBrokerFlag = mBrokerProxy.canSwitchToBroker(authenticationRequest.getAuthority());
        if (switchToBrokerFlag == BrokerProxy.SwitchToBroker.CANNOT_SWITCH_TO_BROKER
                || !mBrokerProxy.verifyUser(authenticationRequest.getLoginHint(), authenticationRequest.getUserId())) {
            return authResult;
        } else if (switchToBrokerFlag == BrokerProxy.SwitchToBroker.NEED_PERMISSIONS_TO_SWITCH_TO_BROKER) {
            //For android M and above
            throw new UsageAuthenticationException(
                        ADALError.DEVELOPER_BROKER_PERMISSIONS_MISSING,
                        "Broker related permissions are missing for GET_ACCOUNTS");
        }

        // If we can try with broker for silent flow, it indicates ADAL can switch to broker for auth. Even broker does
        // not return the token back silently, and we go to interactive flow, we'll still go to broker. The token in
        // app local cache is no longer useful, when user uninstalls broker, we should prompt user in the next sign-in.
        Logger.d(TAG, "Cannot get AT from local cache, switch to Broker for auth, "
                + "clear tokens from local cache for the user.");
        removeTokensForUser(authenticationRequest);

        return tryAcquireTokenSilentWithBroker(authenticationRequest);
    }

    /**
     * Try acquire token silent locally.
     */
    private AuthenticationResult tryAcquireTokenSilentLocally(final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {
        Logger.v(TAG, "Try to silently get token from local cache.");
        final AcquireTokenSilentHandler acquireTokenSilentHandler = new AcquireTokenSilentHandler(mContext,
                authenticationRequest, mTokenCacheAccessor);

        return acquireTokenSilentHandler.getAccessToken();
    }

    /**
     * Try acquire token silent with broker.
     */
    private AuthenticationResult tryAcquireTokenSilentWithBroker(final AuthenticationRequest authenticationRequest)
            throws AuthenticationException {

        final AuthenticationResult authResult;

        final AcquireTokenWithBrokerRequest acquireTokenWithBrokerRequest
                = new AcquireTokenWithBrokerRequest(authenticationRequest, mBrokerProxy);
        authResult = acquireTokenWithBrokerRequest.acquireTokenWithBrokerSilent();

        return authResult;
    }

    private void removeTokensForUser(final AuthenticationRequest request) throws AuthenticationException {
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
        final TokenCacheItem frtItem = mTokenCacheAccessor.getFRTItem(AuthenticationConstants.MS_FAMILY_ID, user);
        if (frtItem != null) {
            mTokenCacheAccessor.removeTokenCacheItem(frtItem, request.getResource());
        }

        // Check if there is a MRRT existed for the user, if there is an MRRT, TokenCacheAccessor will also
        // delete the regular RT entry
        // When there is no MRRT token cache item exist, try to check if there is regular RT cache item for the user.
        final TokenCacheItem mrrtItem = mTokenCacheAccessor.getMRRTItem(request.getClientId(), user);
        final TokenCacheItem regularTokenCacheItem = mTokenCacheAccessor.getRegularRefreshTokenCacheItem(
                request.getResource(), request.getClientId(), user);
        if (mrrtItem != null) {
            mTokenCacheAccessor.removeTokenCacheItem(mrrtItem, request.getResource());
        } else if (regularTokenCacheItem != null) {
            mTokenCacheAccessor.removeTokenCacheItem(regularTokenCacheItem, request.getResource());
        } else {
            Logger.v(TAG, "No token items need to be deleted for the user.");
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

        if (activity == null && !useDialog) {
            throw new AuthenticationException(
                    ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED, authenticationRequest.getLogInfo()
                    + " Cannot launch webview, acitivity is null.");
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
            Logger.v(TAG, "Launch activity for interactive authentication via broker with callback: "
                    + callbackHandle.getCallback().hashCode());
            final AcquireTokenWithBrokerRequest acquireTokenWithBrokerRequest
                    = new AcquireTokenWithBrokerRequest(authenticationRequest, mBrokerProxy);

            acquireTokenWithBrokerRequest.acquireTokenWithBrokerInteractively(activity);
        } else {
            Logger.v(TAG, "Starting Authentication Activity for embedded flow. Callback is:"
                    + callbackHandle.getCallback().hashCode());
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
            errMsg = "The redirectUri is null or blank. "
                    + "so the redirect uri is expected to be:" + actualRedirectUri;
            Logger.e(TAG + methodName, errMsg, "", ADALError.DEVELOPER_REDIRECTURI_INVALID);
            throw new UsageAuthenticationException(ADALError.DEVELOPER_REDIRECTURI_INVALID, errMsg);
        }

        // verify that redirect uri passed in by developer has the correct prefix msauth://
        if (!inputUri.startsWith(AuthenticationConstants.Broker.REDIRECT_PREFIX + "://")) {
            errMsg = "The prefix of the redirect uri does not match the expected value. "
                    + " The valid broker redirect URI prefix: " + AuthenticationConstants.Broker.REDIRECT_PREFIX
                    + " so the redirect uri is expected to be: " + actualRedirectUri;
            Logger.e(TAG + methodName, errMsg, "", ADALError.DEVELOPER_REDIRECTURI_INVALID);
            throw new UsageAuthenticationException(ADALError.DEVELOPER_REDIRECTURI_INVALID, errMsg);
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
            Logger.e(TAG + methodName, e.getMessage(), "", ADALError.ENCODING_IS_NOT_SUPPORTED, e);
            throw new UsageAuthenticationException(ADALError.ENCODING_IS_NOT_SUPPORTED, "The verifying "
                    + "BrokerRedirectUri process failed because the base64 url encoding is not supported.", e);
        }

        // verify package name
        if (!inputUri.startsWith(
                AuthenticationConstants.Broker.REDIRECT_PREFIX + "://" + base64URLEncodePackagename + "/")) {
            errMsg = "The base64 url encoded package name component of the redirect uri does not "
                    + "match the expected value. This apps package name is: " + base64URLEncodePackagename
                    + " so the redirect uri is expected to be: " + actualRedirectUri;
            Logger.e(TAG + methodName, errMsg, "", ADALError.DEVELOPER_REDIRECTURI_INVALID);
            throw new UsageAuthenticationException(ADALError.DEVELOPER_REDIRECTURI_INVALID, errMsg);
        }

        // last thing is to make sure the signature matches
        if (!inputUri.equalsIgnoreCase(actualRedirectUri)) {
            errMsg = "The base64 url encoded signature component of the redirect uri does not match the "
                    + "expected value. This apps signature is: " + base64URLEncodeSignature
                    + " so the redirect uri is expected to be: " + actualRedirectUri;
            Logger.e(TAG + methodName, errMsg, "", ADALError.DEVELOPER_REDIRECTURI_INVALID);
            throw new UsageAuthenticationException(ADALError.DEVELOPER_REDIRECTURI_INVALID, errMsg);
        }

        Logger.v(TAG + methodName, "The broker redirect URI is valid: " + inputUri);
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

            if (data == null) {
                // If data is null, RequestId is unknown. It could not find
                // callback to respond to this request.
                Logger.e(TAG, "onActivityResult BROWSER_FLOW data is null.", "",
                        ADALError.ON_ACTIVITY_RESULT_INTENT_NULL);
            } else {
                final Bundle extras = data.getExtras();
                final int requestId = extras.getInt(AuthenticationConstants.Browser.REQUEST_ID);

                final AuthenticationRequestState waitingRequest;
                try {
                    waitingRequest = mAuthContext.getWaitingRequest(requestId);
                    Logger.v(TAG, "onActivityResult RequestId:" + requestId);
                } catch (final AuthenticationException authenticationException) {
                    Logger.e(TAG, "onActivityResult did not find waiting request for RequestId:"
                            + requestId, "", ADALError.ON_ACTIVITY_RESULT_INTENT_NULL);
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
                    final AuthenticationResult brokerResult = new AuthenticationResult(accessToken, null,
                            expire, false, userinfo, tenantId, idtoken, null);
                    if (brokerResult.getAccessToken() != null) {
                        waitingRequest.getDelegate().onSuccess(brokerResult);
                    }
                } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL) {
                    // User cancelled the flow by clicking back button or
                    // activating another activity
                    Logger.v(TAG, "User cancelled the flow RequestId:" + requestId
                            + correlationInfo);
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
                        Logger.w(TAG, "Webview returned exception", exception.getMessage(),
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
                    Logger.v(TAG, "Error info:" + errCode + " " + errMessage + " for requestId: "
                            + requestId + correlationInfo);
                    waitingRequestOnError(waitingRequest, requestId, new AuthenticationException(
                            ADALError.SERVER_INVALID_REQUEST, errCode + " " + errMessage + correlationInfo));
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
                        Logger.e(TAG, e.getMessage(), "", e.getCode());
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
                                        Logger.v(TAG, "Sending result to callback. "
                                                + waitingRequest.getRequest().getLogInfo());
                                        callbackHandle.onSuccess(authenticationResult);
                                    }
                                } catch (final AuthenticationException authenticationException) {
                                    final StringBuilder message
                                            = new StringBuilder(authenticationException.getMessage());
                                    if (authenticationException.getCause() != null) {
                                        message.append(authenticationException.getCause().getMessage());
                                    }

                                    Logger.e(TAG, message.toString(),
                                            ExceptionExtensions.getExceptionMessage(authenticationException),
                                            ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN,
                                            authenticationException);
                                    waitingRequestOnError(callbackHandle, waitingRequest, requestId,
                                            authenticationException);
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
        if (mHandler == null) {
            // Use current main looper
            mHandler = new Handler(mContext.getMainLooper());
        }

        return mHandler;
    }

    private void waitingRequestOnError(final AuthenticationRequestState waitingRequest,
                                       int requestId, AuthenticationException exc) {

        waitingRequestOnError(null, waitingRequest, requestId, exc);
    }

    private void waitingRequestOnError(final CallbackHandler handler, final AuthenticationRequestState waitingRequest,
                                       final int requestId, final AuthenticationException exc) {
        try {
            if (waitingRequest != null && waitingRequest.getDelegate() != null) {
                Logger.v(TAG, "Sending error to callback"
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

        public CallbackHandler(Handler ref, AuthenticationCallback<AuthenticationResult> callbackExt) {
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

    /**
     * Responsible for receiving message from broker indicating the broker has completed the token acquisition.
     */
    protected class BrokerResumeResultReceiver extends BroadcastReceiver {
        public BrokerResumeResultReceiver() { }

        private boolean mReceivedResultFromBroker = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String methodName = ":BrokerResumeResultReceiver:onReceive";
            Logger.d(TAG + methodName, "Received result from broker.");
            final int receivedWaitingRequestId = intent.getIntExtra(AuthenticationConstants.Browser.REQUEST_ID, 0);

            if (receivedWaitingRequestId == 0) {
                Logger.v(TAG + methodName, "Received waiting request is 0, error will be thrown, cannot find correct "
                        + "callback to send back the result.");
                // Cannot throw AuthenticationException which no longer
                // extending from RuntimeException. Will log the error
                // and return back to caller.
                return;
            }

            // Setting flag to show that receiver already receive result from broker
            mReceivedResultFromBroker = true;
            final AuthenticationRequestState waitingRequest;
            try {
                waitingRequest = mAuthContext.getWaitingRequest(receivedWaitingRequestId);
            } catch (final AuthenticationException authenticationException) {
                Logger.e(TAG, "No waiting request exists", "", ADALError.CALLBACK_IS_NOT_FOUND,
                        authenticationException);
                (new ContextWrapper(mContext)).unregisterReceiver(mBrokerResumeResultReceiver);
                return;
            }

            final String errorCode = intent.getStringExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE);
            if (!StringExtensions.isNullOrBlank(errorCode)) {
                final String errorMessage = intent.getStringExtra(
                        AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE);
                final String returnedErrorMessage = "ErrorCode: " + errorCode + " ErrorMessage" + errorMessage
                        + mAuthContext.getCorrelationInfoFromWaitingRequest(waitingRequest);
                Logger.v(TAG + methodName, returnedErrorMessage);
                waitingRequestOnError(waitingRequest, receivedWaitingRequestId,
                        new AuthenticationException(ADALError.AUTH_FAILED, returnedErrorMessage));
            } else {
                final boolean isBrokerCompleteTokenRequest = intent.getBooleanExtra(
                        AuthenticationConstants.Broker.BROKER_RESULT_RETURNED, false);
                if (isBrokerCompleteTokenRequest) {
                    Logger.v(TAG + methodName, "Broker already completed the token request, calling "
                            + "acquireTokenSilentSync to retrieve token from broker.");
                    final AuthenticationRequest authenticationRequest = waitingRequest.getRequest();
                    String userId = intent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID);

                    // For acquireTokenSilentSync, uniqueId should be passed.
                    if (StringExtensions.isNullOrBlank(userId)) {
                        userId = authenticationRequest.getUserId();
                    }

                    authenticationRequest.setSilent(true);
                    authenticationRequest.setUserId(userId);
                    authenticationRequest.setUserIdentifierType(AuthenticationRequest.UserIdentifierType.UniqueId);
                    acquireToken(null, false, authenticationRequest, waitingRequest.getDelegate());
                } else {
                    Logger.v(TAG + methodName, "Broker doesn't send back error nor the completion notification.");
                    waitingRequestOnError(waitingRequest, receivedWaitingRequestId,
                            new AuthenticationException(ADALError.AUTH_FAILED,
                                    "Broker doesn't send back error nor the completion notification."));
                }
            }
            (new ContextWrapper(mContext)).unregisterReceiver(mBrokerResumeResultReceiver);
        }

        public boolean isResultReceivedFromBroker() {
            return mReceivedResultFromBroker;
        }
    }

}
