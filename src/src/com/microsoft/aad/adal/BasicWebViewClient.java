// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import java.util.HashMap;
import java.util.Locale;

import com.microsoft.aad.adal.ChallangeResponseBuilder.ChallangeResponse;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

abstract class BasicWebViewClient extends WebViewClient {

    private static final String TAG = "BasicWebViewClient";

    public static final String BLANK_PAGE = "about:blank";

    protected String mRedirect;
    
    protected String mQueryParam;

    protected AuthenticationRequest mRequest;
    
    protected Context mCallingContext;

    public BasicWebViewClient() {
        mRedirect = null;
        mRequest = null;
    }

    public BasicWebViewClient(Context appContext, String redirect, String queryParam, AuthenticationRequest request) {
        mCallingContext = appContext;
        mRedirect = redirect;
        mRequest = request;
        mQueryParam = queryParam;
    }

    public abstract void showSpinner(boolean status);

    public abstract void sendResponse(int returnCode, Intent responseIntent);
    
    public abstract void cancelWebViewRequest();
    
    public abstract void setPKeyAuthStatus(boolean status);
    
    public abstract void postRunnable(Runnable item);    

    @Override
    public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler,
            String host, String realm) {

        // Create a dialog to ask for creds and post it to the handler.
        Logger.i(TAG, "onReceivedHttpAuthRequest for host:" + host, "");
        HttpAuthDialog authDialog = new HttpAuthDialog(mCallingContext, host, realm);

        authDialog.setOkListener(new HttpAuthDialog.OkListener() {
            public void onOk(String host, String realm, String username, String password) {
                Logger.i(TAG, "onReceivedHttpAuthRequest: handler proceed" + host, "");
                handler.proceed(username, password);
            }
        });

        authDialog.setCancelListener(new HttpAuthDialog.CancelListener() {
            public void onCancel() {
                Logger.i(TAG, "onReceivedHttpAuthRequest: handler cancelled", "");
                handler.cancel();
                cancelWebViewRequest();
            }
        });

        Logger.i(TAG, "onReceivedHttpAuthRequest: show dialog", "");
        authDialog.show();
    }
    
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        showSpinner(false);
        Logger.e(TAG, "Webview received an error. Errorcode:" + errorCode + " " + description, "",
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
        Logger.e(TAG, "Received ssl error", "", ADALError.ERROR_FAILED_SSL_HANDSHAKE);
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
        Logger.v(TAG, "Page finished:" + url);

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
    	Logger.v(TAG + "onPageStarted", "page is started with the url " + url);
        super.onPageStarted(view, url, favicon);
        showSpinner(true);
    }

    @Override
    //Give the host application a chance to take over the control when a new url is about to be loaded in the current WebView.
    public boolean shouldOverrideUrlLoading(final WebView view, String url) {
        Logger.v(TAG, "Navigation is detected");
        if (url.startsWith(AuthenticationConstants.Broker.PKEYAUTH_REDIRECT)) {
            Logger.v(TAG, "Webview detected request for client certificate");
            view.stopLoading();
            setPKeyAuthStatus(true);
            final String challangeUrl = url;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ChallangeResponseBuilder certHandler = new ChallangeResponseBuilder(
                                new JWSBuilder());
                        final ChallangeResponse challangeResponse = certHandler
                                .getChallangeResponseFromUri(challangeUrl);
                        final HashMap<String, String> headers = new HashMap<String, String>();
                        headers.put(AuthenticationConstants.Broker.CHALLANGE_RESPONSE_HEADER,
                                challangeResponse.mAuthorizationHeaderValue);
                        postRunnable(new Runnable() {

                            @Override
                            public void run() {
                                String loadUrl = challangeResponse.mSubmitUrl;
                                HashMap<String, String> parameters = StringExtensions
                                        .getUrlParameters(challangeResponse.mSubmitUrl);
                                Logger.v(TAG, "SubmitUrl:" + challangeResponse.mSubmitUrl);
                                if (!parameters
                                        .containsKey(AuthenticationConstants.OAuth2.CLIENT_ID)) {
                                    loadUrl = loadUrl + "?" + mQueryParam;
                                }
                                Logger.v(TAG, "Loadurl:" + loadUrl);
                                view.loadUrl(loadUrl, headers);
                            }
                        });
                    } catch (IllegalArgumentException e) {
                        Logger.e(TAG, "Argument exception", e.getMessage(),
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
                        Logger.e(TAG, "It is failed to create device certificate response",
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
                    } catch (Exception e) {
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
            
            if (hasCancelError(url)) {
                // Catch WEB-UI cancel request
                Logger.i(TAG, "Sending intent to cancel authentication activity", "");
                view.stopLoading();
                cancelWebViewRequest();
                return true;
            }
            
            processRedirectUrl(view, url);
            return true;
        } else if (url.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX)) {
            Logger.v(TAG, "It is an external website request");
            openLinkInBrowser(url);
            view.stopLoading();
            cancelWebViewRequest();
            return true;
        } else if (url.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_INSTALL_PREFIX)) {
            Logger.v(TAG, "It is an install request");
            ApplicationReceiver.saveRequest(mCallingContext, mRequest, url);
            HashMap<String, String> parameters = StringExtensions
                    .getUrlParameters(url);
            openLinkInBrowser(parameters.get(ApplicationReceiver.INSTALL_URL_KEY));
            view.stopLoading();
            cancelWebViewRequest();
            return true;
        }

        return processInvalidUrl(view, url);
    }
    
    public abstract void processRedirectUrl(final WebView view, String url);

    public abstract boolean processInvalidUrl(final WebView view, String url);
    
    protected void openLinkInBrowser(String url) {
        String link = url
                .replace(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, "https://");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        mCallingContext.startActivity(intent);
    }
    
    private boolean hasCancelError(String redirectUrl) {
        try {
            HashMap<String, String> parameters = StringExtensions.getUrlParameters(redirectUrl);
            String error = parameters.get("error");
            String errorDescription = parameters.get("error_description");

            if (!StringExtensions.IsNullOrBlank(error)) {
                Logger.v(TAG, "Cancel error:" + error + " " + errorDescription);
                return true;
            }
        } catch (Exception exc) {
            Logger.e(TAG, "Error in processing url parameters", "Url:" + redirectUrl,
                    ADALError.ERROR_WEBVIEW);
        }

        return false;
    }
}
