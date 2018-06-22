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

import com.microsoft.aad.adal.ChallengeResponseBuilder.ChallengeResponse;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

abstract class BasicWebViewClient extends WebViewClient {

    /**
     * Application link to open in the browser.
     */
    private static final String INSTALL_URL_KEY = "app_link";
    private static final String TAG = "BasicWebViewClient";

    public static final String BLANK_PAGE = "about:blank";

    protected String mRedirect;
    protected final AuthenticationRequest mRequest;
    protected final Context mCallingContext;
    private final UIEvent mUIEvent;

    BasicWebViewClient(final Context appContext, final String redirect,
                              final AuthenticationRequest request, final UIEvent uiEvent) {
        mCallingContext = appContext;
        mRedirect = redirect;
        mRequest = request;
        mUIEvent = uiEvent;
    }

    public abstract void showSpinner(boolean status);

    public abstract void sendResponse(int returnCode, Intent responseIntent);

    public abstract void cancelWebViewRequest();

    public abstract void prepareForBrokerResumeRequest();

    public abstract void setPKeyAuthStatus(boolean status);

    public abstract void postRunnable(Runnable item);

    @Override
    public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler,
                                          String host, String realm) {
        final String methodName = ":onReceivedHttpAuthRequest";
        // Create a dialog to ask for creds and post it to the handler.
        Logger.i(TAG + methodName, "Start. ", "Host:" + host);
        if (mUIEvent != null) {
            mUIEvent.setNTLM(true);
        }

        HttpAuthDialog authDialog = new HttpAuthDialog(mCallingContext, host, realm);

        authDialog.setOkListener(new HttpAuthDialog.OkListener() {
            public void onOk(String host, String realm, String username, String password) {
                Logger.i(TAG + methodName, "Handler proceed. ", "Host: " + host);
                handler.proceed(username, password);
            }
        });

        authDialog.setCancelListener(new HttpAuthDialog.CancelListener() {
            public void onCancel() {
                Logger.i(TAG + methodName, "Handler cancelled", "");
                handler.cancel();
                cancelWebViewRequest();
            }
        });

        Logger.i(TAG + methodName, "Show dialog. ", "");
        authDialog.show();
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        showSpinner(false);
        Logger.e(TAG, "Webview received an error. ErrorCode:" + errorCode, description,
                ADALError.ERROR_WEBVIEW);
        Intent resultIntent = new Intent();
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE, "Error Code:"
                + errorCode);
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE, description);
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO, mRequest);
        sendResponse(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        // Developer does not have option to control this for now
        super.onReceivedSslError(view, handler, error);
        showSpinner(false);
        handler.cancel();
        Logger.e(TAG, "Received ssl error. ", "", ADALError.ERROR_FAILED_SSL_HANDSHAKE);
        Intent resultIntent = new Intent();
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE, "Code:"
                + ERROR_FAILED_SSL_HANDSHAKE);
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE,
                error.toString());
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO, mRequest);
        sendResponse(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
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
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        logPageStartLoadingUrl(url);
        super.onPageStarted(view, url, favicon);
        showSpinner(true);
    }

    private void logPageStartLoadingUrl(final String url) {
        final String methodName = ":logPageStartLoadingUrl";
        if (TextUtils.isEmpty(url)) {
            Logger.v(TAG + methodName, "onPageStarted: Null url for page to load.");
            return;
        }

        final Uri uri = Uri.parse(url);
        if (uri.isOpaque()) {
            Logger.v(TAG + methodName, "onPageStarted: Non-hierarchical loading uri. ", "Url: " + url, null);
            return;
        }

        if (StringExtensions.isNullOrBlank(uri.getQueryParameter(
                AuthenticationConstants.OAuth2.CODE))) {
            Logger.v(TAG + methodName, "Webview starts loading. ",
                    " Host: " + uri.getHost() + " Path: " + uri.getPath() + " Full loading url is: " + url, null);
        } else {
            Logger.v(TAG + methodName, "Webview starts loading. ",
                    " Host: " + uri.getHost() + " Path: " + uri.getPath()
                            + " Auth code is returned for the loading url.", null);
        }
    }

    @Override
    //Give the host application a chance to take over the control when a new url is about to be loaded in the current WebView.
    public boolean shouldOverrideUrlLoading(final WebView view, String url) {
        final String methodName = ":shouldOverrideUrlLoading";
        Logger.v(TAG + methodName, "Navigation is detected");
        if (url.startsWith(AuthenticationConstants.Broker.PKEYAUTH_REDIRECT)) {
            Logger.v(TAG + methodName, "Webview detected request for pkeyauth challenge.");
            view.stopLoading();
            setPKeyAuthStatus(true);
            final String challengeUrl = url;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ChallengeResponseBuilder certHandler = new ChallengeResponseBuilder(
                                new JWSBuilder());
                        final ChallengeResponse challengeResponse = certHandler
                                .getChallengeResponseFromUri(challengeUrl);
                        final Map<String, String> headers = new HashMap<>();
                        headers.put(AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER,
                                challengeResponse.getAuthorizationHeaderValue());
                        postRunnable(new Runnable() {

                            @Override
                            public void run() {
                                String loadUrl = challengeResponse.getSubmitUrl();
                                Logger.v(TAG + methodName, "Respond to pkeyAuth challenge",
                                        "Challenge submit url:" + challengeResponse.getSubmitUrl(), null);
                                view.loadUrl(loadUrl, headers);
                            }
                        });
                    } catch (AuthenticationServerProtocolException e) {
                        Logger.e(TAG + methodName, "Argument exception. ", e.getMessage(),
                                ADALError.ARGUMENT_EXCEPTION, e);
                        // It should return error code and finish the
                        // activity, so that onActivityResult implementation
                        // returns errors to callback.
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(
                                AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION,
                                e);
                        if (mRequest != null) {
                            resultIntent.putExtra(
                                    AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                                    mRequest);
                        }
                        sendResponse(
                                AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION,
                                resultIntent);
                    } catch (AuthenticationException e) {
                        Logger.e(TAG + methodName, "It is failed to create device certificate response",
                                e.getMessage(), ADALError.DEVICE_CERTIFICATE_RESPONSE_FAILED, e);
                        // It should return error code and finish the
                        // activity, so that onActivityResult implementation
                        // returns errors to callback.
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(
                                AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION,
                                e);
                        if (mRequest != null) {
                            resultIntent.putExtra(
                                    AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
                                    mRequest);
                        }
                        sendResponse(
                                AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION,
                                resultIntent);
                    }
                }
            }).start();

            return true;
        } else if (url.toLowerCase(Locale.US).startsWith(mRedirect.toLowerCase(Locale.US))) {
            Logger.v(TAG + methodName, "Navigation starts with the redirect uri.");
            if (hasCancelError(url)) {
                // Catch WEB-UI cancel request
                Logger.i(TAG + methodName, "Sending intent to cancel authentication activity", "");
                view.stopLoading();
                cancelWebViewRequest();
                return true;
            }

            processRedirectUrl(view, url);
            return true;
        } else if (url.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX)) {
            Logger.v(TAG + methodName, "It is an external website request");
            openLinkInBrowser(url);
            view.stopLoading();
            cancelWebViewRequest();
            return true;
        } else if (url.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_INSTALL_PREFIX)) {
            Logger.v(TAG + methodName, "It is an install request");
            HashMap<String, String> parameters = StringExtensions
                    .getUrlParameters(url);
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
                Logger.v(TAG + methodName, "Error occurred when having thread sleeping for 1 second.");
            }
            openLinkInBrowser(parameters.get(INSTALL_URL_KEY));
            view.stopLoading();
            return true;
        }

        return processInvalidUrl(view, url);
    }

    public abstract void processRedirectUrl(final WebView view, String url);

    public abstract boolean processInvalidUrl(final WebView view, String url);

    final Context getCallingContext() {
        return mCallingContext;
    }

    protected void openLinkInBrowser(String url) {
        String link = url
                .replace(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, "https://");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        mCallingContext.startActivity(intent);
    }

    protected boolean hasCancelError(String redirectUrl) {
        Map<String, String> parameters = StringExtensions.getUrlParameters(redirectUrl);
        String error = parameters.get("error");
        String errorDescription = parameters.get("error_description");

        if (!StringExtensions.isNullOrBlank(error)) {
            Logger.w(TAG, "Cancel error: " + error, errorDescription, null);
            return true;
        }

        return false;
    }
}
