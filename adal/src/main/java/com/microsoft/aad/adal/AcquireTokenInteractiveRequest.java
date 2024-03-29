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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.WebRequestHandler;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.java.exception.ClientException;

import java.io.IOException;
import java.net.MalformedURLException;

import static com.microsoft.identity.common.java.AuthenticationConstants.UIRequest.BROWSER_FLOW;

/**
 * Internal class handling the detailed acquire token interactive logic. Will be responsible for showing the webview,
 * auth code acquisition, token acquisition.
 */
final class AcquireTokenInteractiveRequest {
    private static final String TAG = AcquireTokenInteractiveRequest.class.getSimpleName();

    private final Context mContext;
    private final TokenCacheAccessor mTokenCacheAccessor;
    private final AuthenticationRequest mAuthRequest;

    /**
     * Constructor for {@link AcquireTokenInteractiveRequest}.
     * {@link TokenCacheAccessor} could be null. If null, won't handle with cache.
     */
    AcquireTokenInteractiveRequest(final Context context, final AuthenticationRequest authRequest,
                                   final TokenCacheAccessor tokenCacheAccessor) {
        mContext = context;
        mTokenCacheAccessor = tokenCacheAccessor;
        mAuthRequest = authRequest;
    }

    void acquireToken(final IWindowComponent activity, final AuthenticationDialog dialog)
            throws AuthenticationException {
        final String methodName = ":acquireToken";
        //Check if there is network connection
        HttpUtil.throwIfNetworkNotAvailable(mContext);

        // Update the PromptBehavior. Since we add the new prompt behavior(force_prompt) for broker apps to
        // force prompt, if this flag is set in the embedded flow, we need to update it to always. For embed
        // flow, force_prompt is the same as always.
        if (PromptBehavior.FORCE_PROMPT == mAuthRequest.getPrompt()) {
            Logger.v(TAG + methodName, "FORCE_PROMPT is set for embedded flow, reset it as Always.");
            mAuthRequest.setPrompt(PromptBehavior.Always);
        }

        // start activity if other options are not available
        // delegate map is used to remember callback if another
        // instance of authenticationContext is created for config
        // change or similar at client app.

        if (dialog != null) {
            dialog.show();
        } else {
            // onActivityResult will receive the response
            if (!startAuthenticationActivity(activity)) {
                throw new AuthenticationException(ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED);
            }
        }
    }

    /**
     * @param url Url containing the auth code.
     * @return {@link AuthenticationResult} for acquire token request with grant_type as code.
     * @throws AuthenticationException
     */
    AuthenticationResult acquireTokenWithAuthCode(final String url) throws AuthenticationException {
        final String methodName = ":acquireTokenWithAuthCode";
        Logger.v(TAG + methodName, "Start token acquisition with auth code.", mAuthRequest.getLogInfo(), null);

        final Oauth2 oauthRequest = new Oauth2(mAuthRequest, new WebRequestHandler());
        final AuthenticationResult result;
        try {
            result = oauthRequest.getToken(url);
            Logger.v(TAG + methodName, "OnActivityResult processed the result.");
        } catch (final IOException | AuthenticationException exc) {
            final String msg = "Error in processing code to get token. " + getCorrelationInfo();
            throw new AuthenticationException(
                    ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN,
                    msg, exc);
        }

        if (result == null) {
            Logger.e(TAG + methodName, "Returned result with exchanging auth code for token is null" + getCorrelationInfo(), "",
                    ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN);
            throw new AuthenticationException(
                    ADALError.AUTHORIZATION_CODE_NOT_EXCHANGED_FOR_TOKEN, getCorrelationInfo());
        }

        if (!StringExtensions.isNullOrBlank(result.getErrorCode())) {
            Logger.e(TAG + methodName, " ErrorCode:" + result.getErrorCode(), " ErrorDescription:" + result.getErrorDescription(), ADALError.AUTH_FAILED);
            throw new AuthenticationException(ADALError.AUTH_FAILED,
                    " ErrorCode:" + result.getErrorCode());
        }

        if (!StringExtensions.isNullOrBlank(result.getAccessToken()) && mTokenCacheAccessor != null) {
            // Developer may pass null for the acquireToken flow.
            try {
                mTokenCacheAccessor.updateTokenCache(mAuthRequest, result);
            } catch (MalformedURLException e) {
                throw new AuthenticationException(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL, e.getMessage(), e);
            } catch (ClientException e) {
                throw ADALError.fromCommon(e);
            }
        }

        return result;
    }


    /**
     * @return True if intent is sent to start the activity, false otherwise.
     */
    private boolean startAuthenticationActivity(final IWindowComponent activity) {
        final String methodName = ":startAuthenticationActivity";
        final Intent intent = getAuthenticationActivityIntent();

        if (!resolveIntent(intent)) {
            Logger.e(TAG + methodName, "Intent is not resolved", "",
                    ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED);
            return false;
        }

        try {
            // Start activity from callers context so that caller can intercept
            // when it is done
            activity.startActivityForResult(intent, BROWSER_FLOW);
        } catch (ActivityNotFoundException e) {
            Logger.e(TAG + methodName, "Activity login is not found after resolving intent", "",
                    ADALError.DEVELOPER_ACTIVITY_IS_NOT_RESOLVED, e);
            return false;
        }

        return true;
    }

    /**
     * Get intent to start authentication activity.
     */
    private Intent getAuthenticationActivityIntent() {
        final Intent intent = new Intent();
        if (AuthenticationSettings.INSTANCE.getActivityPackageName() != null) {
            // This will use the activity from another given package.
            intent.setClassName(AuthenticationSettings.INSTANCE.getActivityPackageName(),
                    AuthenticationActivity.class.getName());
        } else {
            // This will lookup the authentication activity within this context
            intent.setClass(mContext, AuthenticationActivity.class);
        }

        intent.putExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE, mAuthRequest);
        return intent;
    }

    /**
     * Resolve activity from the package. If developer did not declare the
     * activity, it will not resolve.
     * True if activity is defined in the package, false otherwise.
     */
    private boolean resolveIntent(final Intent intent) {
        final ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null;
    }

    private String getCorrelationInfo() {
        return String.format(" CorrelationId: %s", mAuthRequest.getCorrelationId().toString());
    }
}
