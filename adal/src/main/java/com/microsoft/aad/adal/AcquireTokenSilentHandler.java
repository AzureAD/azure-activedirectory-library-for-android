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

import android.content.Context;
import android.util.Log;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.IWebRequestHandler;
import com.microsoft.identity.common.adal.internal.net.WebRequestHandler;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Internal class handling the detailed acquireToken silent logic, including cache lookup and also
 * interact with web request(The class represents the state machine for acquireToken silent flow).
 */
class AcquireTokenSilentHandler {
    private static final String TAG = AcquireTokenSilentHandler.class.getSimpleName();

    private final Context mContext;
    private final TokenCacheAccessor mTokenCacheAccessor;
    private final AuthenticationRequest mAuthRequest;

    private boolean mAttemptedWithMRRT = false;
    private TokenCacheItem mMrrtTokenCacheItem;

    /**
     * TODO: Remove(https://github.com/AzureAD/azure-activedirectory-library-for-android/issues/626).
     * {@link com.microsoft.identity.common.adal.internal.net.HttpUrlConnectionFactory} provides the possibility to
     * mock the real connection. Needs to update the class to make different response based on
     * post message.
     */
    private IWebRequestHandler mWebRequestHandler = null;

    /**
     * Constructor for {@link AcquireTokenSilentHandler}.
     * {@link TokenCacheAccessor} could be null. If null, won't handle with cache.
     * TODO: Consider have a separate handler for refresh token without cache interaction.
     * (https://github.com/AzureAD/azure-activedirectory-library-for-android/issues/626)
     */
    AcquireTokenSilentHandler(final Context context, final AuthenticationRequest authRequest,
                              final TokenCacheAccessor tokenCacheAccessor) {
        if (context == null) {
            throw new IllegalArgumentException("context");
        }

        if (authRequest == null) {
            throw new IllegalArgumentException("authRequest");
        }

        mContext = context;
        mAuthRequest = authRequest;
        mTokenCacheAccessor = tokenCacheAccessor;

        mWebRequestHandler = new WebRequestHandler();
    }

    /**
     * Request for access token by looking up cache(Initial start point.).
     * Detailed token cache lookup:
     * 1) try to find an AT, if AT exists and not expired, return it.
     * 2) Use RT:
     * i>   If RT exists, and it's an MRRT or there is an MRRT item exists, try to find the MRRT.
     * ii>  If RT exists, and no MRRT existed, tried with the RT.
     * iii> If RT does not exist, try to find MRRT.
     * 3) Use MRRT:
     * i>   If MRRT exists, and if the MRRT is also an FRT, try to find FRT.
     * ii>  If MRRT exists, but it's not FRT, use the MRRT. If MRRT request fails,
     * still do one more try with FRT(FoCI will be hard-coded as MS family Id. )
     * iii> If no MRRT exists, try to find FRT.
     * 4) Use FRT:
     * i>   If FRT exists, use the FRT.
     * ii>  If FRT request fails, and we haven't tried with MRRT, use MRRT.
     * iii> If FRT does not exist, if we haven't tried with MRRT, use the MRRT.
     * VI>  If FRT exists, and we've already tried with MRRT, return the MRRT result.
     */
    AuthenticationResult getAccessToken() throws AuthenticationException {
        // If mTokenCacheAccessor is null, won't handle with token cache lookup.
        final String methodName = ":getAccessToken";
        if (mTokenCacheAccessor == null) {
            return null;
        }

        // Check for if there is valid access token item in the cache.
        final TokenCacheItem accessTokenItem = mTokenCacheAccessor.getATFromCache(mAuthRequest.getResource(),
                mAuthRequest.getClientId(), mAuthRequest.getUserFromRequest());
        // If accessToken is null or if the user requested force refresh or if claims challenge is present then get a new access token using local refresh tokens
        if (accessTokenItem == null || mAuthRequest.getForceRefresh() || mAuthRequest.isClaimsChallengePresent()) {
            Logger.v(TAG + methodName, "No valid access token exists, try with refresh token.");
            return tryRT();
        }

        Logger.v(TAG + methodName, "Return AT from cache.");
        return AuthenticationResult.createResult(accessTokenItem);
    }

    /**
     * Send token request with grant_type as refresh_token to token endpoint for getting new access token.
     */
    AuthenticationResult acquireTokenWithRefreshToken(final String refreshToken)
            throws AuthenticationException {
        final String methodName = ":acquireTokenWithRefreshToken";
        Logger.v(TAG + methodName, "Try to get new access token with the found refresh token.",
                mAuthRequest.getLogInfo(), null);

        // Check if network is available, if not throw exception. 
        HttpWebRequest.throwIfNetworkNotAvailable(mContext);

        final AuthenticationResult result;
        try {
            final JWSBuilder jwsBuilder = new JWSBuilder();
            final Oauth2 oauthRequest = new Oauth2(mAuthRequest, mWebRequestHandler, jwsBuilder);
            result = oauthRequest.refreshToken(refreshToken);
            if (result != null && StringExtensions.isNullOrBlank(result.getRefreshToken())) {
                Logger.i(TAG + methodName, "Refresh token is not returned or empty", "");
                result.setRefreshToken(refreshToken);
            }
        } catch (final ServerRespondingWithRetryableException exc) {
            Logger.i(TAG + methodName, "The server is not responding after the retry with error code: " + exc.getCode(), "");
            final TokenCacheItem accessTokenItem = mTokenCacheAccessor.getStaleToken(mAuthRequest);
            if (accessTokenItem != null) {
                final AuthenticationResult retryResult = AuthenticationResult.createExtendedLifeTimeResult(accessTokenItem);
                Logger.i(TAG + methodName, "The result with stale access token is returned.", "");
                return retryResult;
            }

            Logger.e(TAG + methodName,
                    "Error in refresh token for request. ",
                    "Request: " + mAuthRequest.getLogInfo()
                            + " " + ExceptionExtensions.getExceptionMessage(exc)
                            + " " + Log.getStackTraceString(exc),
                    ADALError.AUTH_FAILED_NO_TOKEN,
                    null);

            throw new AuthenticationException(
                    ADALError.AUTH_FAILED_NO_TOKEN, ExceptionExtensions.getExceptionMessage(exc),
                    new AuthenticationException(ADALError.SERVER_ERROR, exc.getMessage(), exc));
        } catch (final IOException | AuthenticationException exc) {
            // Server side error or similar
            Logger.e(TAG + methodName,
                    "Error in refresh token for request.",
                    "Request: " + mAuthRequest.getLogInfo()
                            + " " + ExceptionExtensions.getExceptionMessage(exc)
                            + " " + Log.getStackTraceString(exc),
                    ADALError.AUTH_FAILED_NO_TOKEN,
                    null);

            throw new AuthenticationException(
                    ADALError.AUTH_FAILED_NO_TOKEN, ExceptionExtensions.getExceptionMessage(exc),
                    new AuthenticationException(ADALError.SERVER_ERROR, exc.getMessage(), exc));
        }

        return result;
    }

    /**
     * For testing purpose, inject the dependency.
     */
    void setWebRequestHandler(final IWebRequestHandler webRequestHandler) {
        this.mWebRequestHandler = webRequestHandler;
    }

    /**
     * Attempt to get new access token with regular RT.
     */
    private AuthenticationResult tryRT() throws AuthenticationException {
        final String methodName = ":tryRT";
        final TokenCacheItem regularRTItem;

        try {
            regularRTItem = mTokenCacheAccessor.getRegularRefreshTokenCacheItem(mAuthRequest.getResource(),
                    mAuthRequest.getClientId(), mAuthRequest.getUserFromRequest());
        } catch (final MalformedURLException ex) {
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, ex.getMessage(), ex);
        }

        if (regularRTItem == null) {
            Logger.v(TAG + methodName, "Regular token cache entry does not exist, try with MRRT.");
            return tryMRRT();
        }

        // When MRRT is returned, we store separate entries for both regular RT entry and MRRT entry, 
        // we should look for MRRT entry if the token in regular RT entry is marked as MRRT.
        // However, the current cache implementation never mark the token stored in regular RT entry
        // as MRRT. To support the backward compatibility and improve cache lookup, when successfully
        // retrieved regular RT entry token and if the mrrt flag is false, check the existence of MRRT.
        if (regularRTItem.getIsMultiResourceRefreshToken() || isMRRTEntryExisted()) {
            final String statusMessage = regularRTItem.getIsMultiResourceRefreshToken()
                    ? "Found RT and it's also a MRRT, retry with MRRT"
                    : "RT is found and there is a MRRT entry existed, try with MRRT";
            Logger.v(TAG + methodName, statusMessage);
            return tryMRRT();
        }

        if (StringExtensions.isNullOrBlank(mAuthRequest.getUserFromRequest()) && mTokenCacheAccessor.isMultipleRTsMatchingGivenAppAndResource(
                mAuthRequest.getClientId(), mAuthRequest.getResource())) {
            throw new AuthenticationException(ADALError.AUTH_FAILED_USER_MISMATCH, "Multiple refresh tokens exists for the given client id and resource");
        }

        Logger.v(TAG + methodName, "Send request to use regular RT for new AT.");
        return acquireTokenWithCachedItem(regularRTItem);
    }

    /**
     * Attempt to get new access token with MRRT.
     * 1) If MRRT does not exist, try with FRT.
     * 2) If MRRT is also a FRT, try FRT first.
     * 3) If MRRT request fails, fall back to FRT.
     */
    private AuthenticationResult tryMRRT() throws AuthenticationException {
        final String methodName = ":tryMRRT";
        // Try to get it from cache
        try {
            mMrrtTokenCacheItem = mTokenCacheAccessor.getMRRTItem(mAuthRequest.getClientId(),
                    mAuthRequest.getUserFromRequest());
        } catch (final MalformedURLException ex) {
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, ex.getMessage(), ex);
        }


        // MRRT does not exist, try with FRT.
        if (mMrrtTokenCacheItem == null) {
            Logger.v(TAG + methodName, "MRRT token does not exist, try with FRT");
            return tryFRT(AuthenticationConstants.MS_FAMILY_ID, null);
        }

        // If MRRT is also a FRT, we try FRT first. 
        if (mMrrtTokenCacheItem.isFamilyToken()) {
            Logger.v(TAG + methodName, "MRRT item exists but it's also a FRT, try with FRT.");
            return tryFRT(mMrrtTokenCacheItem.getFamilyClientId(), null);
        }

        AuthenticationResult mrrtResult = useMRRT();
        if (isTokenRequestFailed(mrrtResult)) {
            // If MRRT fails, we still want to retry on FRT in case there is one there. 
            // MRRT may not be marked as FRT, hard-code it as "1" in this case. 
            final String familyClientId = StringExtensions.isNullOrBlank(mMrrtTokenCacheItem.getFamilyClientId())
                    ? AuthenticationConstants.MS_FAMILY_ID : mMrrtTokenCacheItem.getFamilyClientId();

            // Pass the failed MRRT result to tryFRT, if FRT does not exist, return the MRRT result. 
            mrrtResult = tryFRT(familyClientId, mrrtResult);
        }

        if (StringExtensions.isNullOrBlank(mAuthRequest.getUserFromRequest()) && mTokenCacheAccessor.isMultipleMRRTsMatchingGivenApp(mAuthRequest.getClientId())) {
            throw new AuthenticationException(ADALError.AUTH_FAILED_USER_MISMATCH, "No User provided and multiple MRRTs exist for the given client id");
        }

        return mrrtResult;
    }

    /**
     * Attempt to get access token with FRT.
     * 1) If FRT does not exist, and we haven't tried with MRRT(the only possible to enter try FRT is after we call tryMRRT,
     * then we either already have a MRRT or MRRT does not exist)
     * 2) If FRT request fails, and we haven't tried with MRRT yet, use it.
     */
    private AuthenticationResult tryFRT(final String familyClientId, final AuthenticationResult mrrtResult)
            throws AuthenticationException {
        final String methodName = ":tryFRT";
        final TokenCacheItem frtTokenCacheItem;

        try {
            frtTokenCacheItem = mTokenCacheAccessor.getFRTItem(familyClientId,
                    mAuthRequest.getUserFromRequest());
        } catch (final MalformedURLException ex) {
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, ex.getMessage(), ex);
        }

        if (frtTokenCacheItem == null) {
            // If we haven't tried with MRRT, use the MRRT. MRRT either exists or not, if it does not exist, we've 
            // already tried our best, null will be returned. If it exists, try with it.
            // If we have already tried an MRRT and no FRT found, we return the MRRT result passed in. 
            if (!mAttemptedWithMRRT) {
                Logger.v(TAG + methodName, "FRT cache item does not exist, fall back to try MRRT.");
                return useMRRT();
            } else {
                return mrrtResult;
            }
        }

        Logger.v(TAG + methodName, "Send request to use FRT for new AT.");
        AuthenticationResult frtResult = acquireTokenWithCachedItem(frtTokenCacheItem);
        if (isTokenRequestFailed(frtResult) && !mAttemptedWithMRRT) {
            // FRT request fails, fallback to MRRT if we haven't tried with MRRT. 
            final AuthenticationResult retryMrrtResult = useMRRT();
            frtResult = retryMrrtResult == null ? frtResult : retryMrrtResult;
        }

        return frtResult;
    }

    /**
     * Attempt to use MRRT.
     */
    private AuthenticationResult useMRRT() throws AuthenticationException {
        final String methodName = ":useMRRT";
        Logger.v(TAG + methodName, "Send request to use MRRT for new AT.");
        mAttemptedWithMRRT = true;
        if (mMrrtTokenCacheItem == null) {
            Logger.v(TAG + methodName, "MRRT does not exist, cannot proceed with MRRT for new AT.");
            return null;
        }

        return acquireTokenWithCachedItem(mMrrtTokenCacheItem);
    }

    /**
     * Acquire token with retrieved token cache item and update cache.
     */
    private AuthenticationResult acquireTokenWithCachedItem(final TokenCacheItem cachedItem)
            throws AuthenticationException {
        final String methodName = ":acquireTokenWithCachedItem";
        if (StringExtensions.isNullOrBlank(cachedItem.getRefreshToken())) {
            Logger.v(TAG + methodName, "Token cache item contains empty refresh token, cannot continue refresh "
                    + "token request", mAuthRequest.getLogInfo(), null);
            return null;
        }

        final AuthenticationResult result = acquireTokenWithRefreshToken(cachedItem.getRefreshToken());

        if (result != null && !result.isExtendedLifeTimeToken()) {
            mTokenCacheAccessor.updateCachedItemWithResult(mAuthRequest,
                    result, cachedItem);
        }

        return result;
    }

    /**
     * Old version of ADAL doesn't mark token stored in regular RT entry as MRRT even it is. The logic to look for
     * MRRT is when RT is not found or found RT is also MRRT. To support the old behavior, do a separate check on
     * the existence for MRRT token entry.
     */
    private boolean isMRRTEntryExisted() throws AuthenticationException {
        final TokenCacheItem mrrtItem;

        try {
            mrrtItem = mTokenCacheAccessor.getMRRTItem(mAuthRequest.getClientId(),
                    mAuthRequest.getUserFromRequest());
        } catch (final MalformedURLException ex) {
            throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, ex.getMessage(), ex);
        }

        return mrrtItem != null && !StringExtensions.isNullOrBlank(mrrtItem.getRefreshToken());
    }

    /**
     * Check if the {@link AuthenticationResult} contains oauth2 error.
     */
    private boolean isTokenRequestFailed(final AuthenticationResult result) {
        return result != null && !StringExtensions.isNullOrBlank(result.getErrorCode());
    }
}
