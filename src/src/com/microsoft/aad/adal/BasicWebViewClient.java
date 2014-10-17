
package com.microsoft.aad.adal;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

abstract class BasicWebViewClient extends WebViewClient {

    private static final String TAG = "BasicWebViewClient";

    public static final String BLANK_PAGE = "about:blank";

    protected String mRedirect;

    protected int mRequestCode;

    protected AuthenticationRequest mRequest;

    public BasicWebViewClient() {
        mRedirect = null;
        mRequestCode = 0;
        mRequest = null;
    }

    public BasicWebViewClient(String redirect, int requestCode, AuthenticationRequest request) {
        mRedirect = redirect;
        mRequestCode = requestCode;
        mRequest = request;
    }

    public abstract void showSpinner(boolean status);

    public abstract void sendResponse(int returnCode, Intent responseIntent);

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
        super.onPageStarted(view, url, favicon);
        showSpinner(true);
    }
}
