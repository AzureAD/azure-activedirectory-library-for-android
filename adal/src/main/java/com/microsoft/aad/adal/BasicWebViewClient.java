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
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.text.TextUtils;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.aad.adal.ChallengeResponseBuilder.ChallengeResponse;
import com.microsoft.identity.common.adal.internal.JWSBuilder;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.aad.adal.AuthenticationConstants.Broker.BROWSER_EXT_INSTALL_PREFIX;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.BROWSER_EXT_PREFIX;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER;
import static com.microsoft.aad.adal.AuthenticationConstants.Broker.PKEYAUTH_REDIRECT;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.RESPONSE_ERROR_CODE;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE;
import static com.microsoft.aad.adal.AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO;
import static com.microsoft.aad.adal.AuthenticationConstants.OAuth2.CODE;
import static com.microsoft.aad.adal.AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION;
import static com.microsoft.aad.adal.AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR;

abstract class BasicWebViewClient extends WebViewClient {

    /**
     * Application link to open in the browser.
     */
    private static final String INSTALL_URL_KEY = "app_link";
    private static final String TAG = "BasicWebViewClient";

    static final String BLANK_PAGE = "about:blank";

    final AuthenticationRequest mRequest;

    private String mRedirect;
    private final Context mCallingContext;
    private final UIEvent mUIEvent;

    BasicWebViewClient(@NonNull final Context appContext,
                       @NonNull final String redirect,
                       @NonNull final AuthenticationRequest request,
                       @Nullable final UIEvent uiEvent) {
        mCallingContext = appContext;
        mRedirect = redirect;
        mRequest = request;
        mUIEvent = uiEvent;
    }

    public abstract void showSpinner(boolean status);

    public abstract void sendResponse(int returnCode, Intent responseIntent);

    public abstract void cancelWebViewRequest(@Nullable Intent errorIntent);

    public abstract void prepareForBrokerResumeRequest();

    public abstract void setPKeyAuthStatus(boolean status);

    public abstract void postRunnable(Runnable item);

    public abstract void processRedirectUrl(final WebView view, final String url);

    public abstract boolean processInvalidUrl(final WebView view, final String url);

    @Override
    public void onReceivedHttpAuthRequest(final WebView view,
                                          final HttpAuthHandler handler,
                                          final String host,
                                          final String realm) {
        final String methodName = ":onReceivedHttpAuthRequest";
        // Create a dialog to ask for creds and post it to the handler.
        com.microsoft.identity.common.internal.logging.Logger.infoPII(
                TAG + methodName,
                "Start. Host: " + host
        );

        if (mUIEvent != null) {
            mUIEvent.setNTLM(true);
        }

        final HttpAuthDialog authDialog = new HttpAuthDialog(mCallingContext, host, realm);

        authDialog.setOkListener(new HttpAuthDialog.OkListener() {
            public void onOk(String host, String realm, String username, String password) {
                com.microsoft.identity.common.internal.logging.Logger.infoPII(
                        TAG + methodName,
                        "Handler proceed. Host: " + host
                );

                handler.proceed(username, password);
            }
        });

        authDialog.setCancelListener(new HttpAuthDialog.CancelListener() {
            public void onCancel() {
                com.microsoft.identity.common.internal.logging.Logger.infoPII(
                        TAG + methodName,
                        "Handler cancelled."
                );

                handler.cancel();
                cancelWebViewRequest(null);
            }
        });

        com.microsoft.identity.common.internal.logging.Logger.info(
                TAG + methodName,
                "Show dialog."
        );

        authDialog.show();
    }

    @Override
    public void onReceivedError(final WebView view,
                                final int errorCode,
                                final String description,
                                final String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        showSpinner(false);

        com.microsoft.identity.common.internal.logging.Logger.errorPII(
                TAG,
                "Webview received an error."
                        + " ErrorCode: " + errorCode
                        + " Error description: " + description,
                null
        );

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(RESPONSE_ERROR_CODE, "Error Code:" + errorCode);
        resultIntent.putExtra(RESPONSE_ERROR_MESSAGE, description);
        resultIntent.putExtra(RESPONSE_REQUEST_INFO, mRequest);
        sendResponse(BROWSER_CODE_ERROR, resultIntent);
    }

    @Override
    public void onReceivedSslError(final WebView view,
                                   final SslErrorHandler handler,
                                   final SslError error) {
        // Developer does not have option to control this for now
        super.onReceivedSslError(view, handler, error);
        showSpinner(false);
        handler.cancel();

        com.microsoft.identity.common.internal.logging.Logger.error(
                TAG,
                "Received SSL error.",
                null
        );

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(RESPONSE_ERROR_CODE, "Code:" + ERROR_FAILED_SSL_HANDSHAKE);
        resultIntent.putExtra(RESPONSE_ERROR_MESSAGE, error.toString());
        resultIntent.putExtra(RESPONSE_REQUEST_INFO, mRequest);

        sendResponse(BROWSER_CODE_ERROR, resultIntent);
    }

    @Override
    public void onPageFinished(final WebView view, final String url) {
        super.onPageFinished(view, url);

        /*
         * Once web view is fully loaded,set to visible
         */
        view.setVisibility(View.VISIBLE);

        if (!url.startsWith(BLANK_PAGE)) {
            showSpinner(false);
        }
    }

    @Override
    public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
        logPageStartLoadingUrl(url);
        super.onPageStarted(view, url, favicon);
        showSpinner(true);
    }

    private void logPageStartLoadingUrl(final String url) {
        final String methodName = ":logPageStartLoadingUrl";

        if (TextUtils.isEmpty(url)) {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                    TAG + methodName,
                    "onPageStarted: Null url for page to load."
            );

            return;
        }

        final Uri uri = Uri.parse(url);

        if (uri.isOpaque()) {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                    TAG + methodName,
                    "onPageStarted: Non-hierarchical loading uri."
            );

            com.microsoft.identity.common.internal.logging.Logger.warnPII(
                    TAG + methodName,
                    "Url: " + url
            );

            return;
        }

        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "WebView starts loading."
        );

        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "Host: " + uri.getHost()
                        + "\n"
                        + "Path: " + uri.getPath()
        );

        if (StringExtensions.isNullOrBlank(uri.getQueryParameter(CODE))) {
            com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                    TAG + methodName,
                    "Url did not contain auth code."
                            + "\n"
                            + "Full loading url is: " + url
            );
        } else {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Auth code received."
            );
        }
    }

    @Override
    // Give the host application a chance to take over the control when a new url is about to be
    // loaded in the current WebView.
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        final String methodName = ":shouldOverrideUrlLoading";

        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Navigation is detected."
        );

        if (url.startsWith(PKEYAUTH_REDIRECT)) {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Webview detected request for pkeyauth challenge."
            );

            view.stopLoading();
            setPKeyAuthStatus(true);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final ChallengeResponseBuilder certHandler = new ChallengeResponseBuilder(
                                new JWSBuilder()
                        );

                        final ChallengeResponse challengeResponse = certHandler
                                .getChallengeResponseFromUri(url);

                        final Map<String, String> headers = new HashMap<>();
                        headers.put(
                                CHALLENGE_RESPONSE_HEADER,
                                challengeResponse.getAuthorizationHeaderValue()
                        );

                        postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                String loadUrl = challengeResponse.getSubmitUrl();

                                com.microsoft.identity.common.internal.logging.Logger.verbose(
                                        TAG + methodName,
                                        "Respond to pkeyAuth challenge."
                                );

                                com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                                        TAG + methodName,
                                        "Challenge submit url:"
                                                + challengeResponse.getSubmitUrl()
                                );

                                view.loadUrl(loadUrl, headers);
                            }
                        });
                    } catch (final AuthenticationServerProtocolException e) {
                        com.microsoft.identity.common.internal.logging.Logger.errorPII(
                                TAG + methodName,
                                "Argument exception",
                                e
                        );

                        // It should return error code and finish the
                        // activity, so that onActivityResult implementation
                        // returns errors to callback.
                        final Intent resultIntent = new Intent();
                        resultIntent.putExtra(
                                RESPONSE_AUTHENTICATION_EXCEPTION,
                                e
                        );

                        if (mRequest != null) {
                            resultIntent.putExtra(
                                    RESPONSE_REQUEST_INFO,
                                    mRequest
                            );
                        }

                        sendResponse(
                                BROWSER_CODE_AUTHENTICATION_EXCEPTION,
                                resultIntent
                        );
                    } catch (final AuthenticationException e) {
                        com.microsoft.identity.common.internal.logging.Logger.error(
                                TAG + methodName,
                                "Failed to create device certificate response",
                                null
                        );

                        com.microsoft.identity.common.internal.logging.Logger.errorPII(
                                TAG + methodName,
                                "Error",
                                e
                        );
                        // It should return error code and finish the
                        // activity, so that onActivityResult implementation
                        // returns errors to callback.
                        final Intent resultIntent = new Intent();
                        resultIntent.putExtra(
                                RESPONSE_AUTHENTICATION_EXCEPTION,
                                e
                        );

                        if (mRequest != null) {
                            resultIntent.putExtra(
                                    RESPONSE_REQUEST_INFO,
                                    mRequest
                            );
                        }

                        sendResponse(
                                BROWSER_CODE_AUTHENTICATION_EXCEPTION,
                                resultIntent
                        );
                    }
                }
            }).start();

            return true;
        } else if (url.toLowerCase(Locale.US).startsWith(mRedirect.toLowerCase(Locale.US))) {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Navigation starts with the redirect uri."
            );

            Intent errorIntent = parseError(url);
            if (errorIntent != null) {
                // Catch WEB-UI cancel request
                com.microsoft.identity.common.internal.logging.Logger.info(
                        TAG + methodName,
                        "Sending intent to cancel authentication activity"
                );

                view.stopLoading();
                cancelWebViewRequest(errorIntent);
                return true;
            }

            processRedirectUrl(view, url);
            return true;
        } else if (url.startsWith(BROWSER_EXT_PREFIX)) {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "It is an external website request"
            );

            openLinkInBrowser(url);
            view.stopLoading();
            cancelWebViewRequest(null);
            return true;
        } else if (url.startsWith(BROWSER_EXT_INSTALL_PREFIX)) {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "It is an install request"
            );

            final HashMap<String, String> parameters = StringExtensions.getUrlParameters(url);
            prepareForBrokerResumeRequest();
            // Having thread sleep for 1 second for calling activity to receive the result from
            // prepareForBrokerResumeRequest, thus the receiver for listening broker result return
            // can be registered. openLinkInBrowser will launch activity for going to
            // playstore and broker app download page which brought the calling activity down
            // in the activity stack.
            final int threadSleepForCallingActivity = 1000;

            try {
                Thread.sleep(threadSleepForCallingActivity);
            } catch (InterruptedException e) {
                com.microsoft.identity.common.internal.logging.Logger.warn(
                        TAG + methodName,
                        "Error occurred when having thread sleeping for 1 second."
                );
            }

            openLinkInBrowser(parameters.get(INSTALL_URL_KEY));
            view.stopLoading();
            return true;
        }

        return processInvalidUrl(view, url);
    }

    final Context getCallingContext() {
        return mCallingContext;
    }

    protected void openLinkInBrowser(final String url) {
        final String link = url.replace(
                BROWSER_EXT_PREFIX,
                "https://"
        );
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        mCallingContext.startActivity(intent);
    }

    private Intent parseError(String redirectUrl) {
        final Map<String, String> parameters = StringExtensions.getUrlParameters(redirectUrl);
        final String error = parameters.get(AuthenticationConstants.OAuth2.ERROR);
        final String errorDescription = parameters.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION);

        if (!StringExtensions.isNullOrBlank(error)) {
            com.microsoft.identity.common.internal.logging.Logger.warnPII(
                    TAG,
                    "Cancel error: " + error
                            + "\n"
                            + "Error Description: " + errorDescription
            );

            Intent intent = new Intent();
            intent.putExtra(AuthenticationConstants.OAuth2.ERROR, error);
            intent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE, error);
            intent.putExtra(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION, errorDescription);
            intent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE, errorDescription);
            return intent;
        }

        return null;
    }
}
