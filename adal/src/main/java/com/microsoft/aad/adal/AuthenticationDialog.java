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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

import java.io.UnsupportedEncodingException;

@SuppressLint({
        "InflateParams", "SetJavaScriptEnabled", "ClickableViewAccessibility"
})
class AuthenticationDialog {
    protected static final String TAG = "AuthenticationDialog";

    private final Context mContext;

    private final AcquireTokenRequest mAcquireTokenRequest;

    private final AuthenticationRequest mRequest;

    private final Handler mHandlerInView;

    private Dialog mDialog;

    private WebView mWebView;

    AuthenticationDialog(final Handler handler,
                         final Context context,
                         final AcquireTokenRequest acquireTokenRequest,
                         final AuthenticationRequest request) {
        mHandlerInView = handler;
        mContext = context;
        mAcquireTokenRequest = acquireTokenRequest;
        mRequest = request;
    }

    private int getResourceId(final String name, final String type) {
        return mContext.getResources().getIdentifier(name, type, mContext.getPackageName());
    }

    /**
     * Create dialog using the context. Inflate the layout with inflater
     * service. This will run with the handler.
     */
    void show() {
        final String methodName = ":show";
        mHandlerInView.post(new Runnable() {

            @Override
            public void run() {
                LayoutInflater inflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

                //Need to be sure that the resource id is actually found
                int dialogAuthenticationResourceId = getResourceId("dialog_authentication", "layout");

                View webviewInDialog = null;
                // using static layout
                try {
                    webviewInDialog = inflater.inflate(dialogAuthenticationResourceId, null);
                } catch (final InflateException e) {
                    //This code was added to debug a threading issue; however there could be other cases when this would occur... so leaving in.
                    //NOTE: With the threading issue even though the exception was caught the test app still interrupted (looks like a crash)... presumably because of
                    //The Android System Webview (Chromium) crashed; however the app does continue after Android restarts the system web view
                    Logger.e(TAG, "Failed to inflate authentication dialog", "", ADALError.DEVELOPER_DIALOG_INFLATION_ERROR, e);
                }

                if (webviewInDialog != null) {
                    mWebView = (WebView) webviewInDialog.findViewById(
                            getResourceId("com_microsoft_aad_adal_webView1", "id")
                    );
                }

                if (mWebView == null) {
                    Logger.e(
                            TAG + methodName,
                            "Expected resource name for webview is com_microsoft_aad_adal_webView1. It is not in your layout file",
                            "", ADALError.DEVELOPER_DIALOG_LAYOUT_INVALID);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(AuthenticationConstants.Browser.REQUEST_ID,
                            mRequest.getRequestId());
                    mAcquireTokenRequest.onActivityResult(AuthenticationConstants.UIRequest.BROWSER_FLOW,
                            AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, resultIntent);

                    mHandlerInView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mDialog != null && mDialog.isShowing()) {
                                mDialog.dismiss();
                            }
                        }
                    });
                    return;
                }

                // Disable hardware acceleration in WebView if needed
                if (!AuthenticationSettings.INSTANCE.getDisableWebViewHardwareAcceleration()) {
                    mWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                    Logger.d(TAG + methodName, "Hardware acceleration is disabled in WebView");
                }

                final WebSettings webSettings = mWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setAllowContentAccess(false);
                mWebView.requestFocus(View.FOCUS_DOWN);
                String userAgent = webSettings.getUserAgentString();
                webSettings.setUserAgentString(
                        userAgent + AuthenticationConstants.Broker.CLIENT_TLS_NOT_SUPPORTED);
                userAgent = webSettings.getUserAgentString();
                Logger.v(TAG + methodName, "UserAgent:" + userAgent);

                // Set focus to the view for touch event
                mWebView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        int action = event.getAction();
                        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP)
                                && !view.hasFocus()) {
                            view.requestFocus();
                        }
                        return false;
                    }
                });

                webSettings.setLoadWithOverviewMode(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setUseWideViewPort(true);
                webSettings.setBuiltInZoomControls(true);

                try {
                    Oauth2 oauth = new Oauth2(mRequest);
                    final String startUrl = oauth.getCodeRequestUrl();
                    final String stopRedirect = mRequest.getRedirectUri();
                    mWebView.setWebViewClient(new DialogWebViewClient(mContext, stopRedirect, mRequest));
                    mWebView.post(new Runnable() {
                        @Override
                        public void run() {
                            mWebView.loadUrl(BasicWebViewClient.BLANK_PAGE);
                            mWebView.loadUrl(startUrl);
                        }
                    });

                } catch (UnsupportedEncodingException e) {
                    Logger.e(TAG + methodName, "Encoding error", "", ADALError.ENCODING_IS_NOT_SUPPORTED, e);
                }

                builder.setView(webviewInDialog).setCancelable(true);
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancelFlow(null);
                    }
                });
                mDialog = builder.create();
                Logger.i(TAG + methodName, "Showing authenticationDialog", "");
                mDialog.show();
            }
        });
    }

    private void cancelFlow(@Nullable final Intent errorIntent) {
        Logger.i(TAG, "Cancelling dialog", "");
        Intent resultIntent = errorIntent;

        int resultCode;
        if (resultIntent == null) {
            resultIntent = new Intent();
            resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL;
        } else {
            resultCode = AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR;
        }

        resultIntent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, mRequest.getRequestId());

        mAcquireTokenRequest.onActivityResult(AuthenticationConstants.UIRequest.BROWSER_FLOW,
                resultCode, resultIntent);
        if (mHandlerInView != null) {
            mHandlerInView.post(new Runnable() {
                @Override
                public void run() {
                    if (mDialog != null && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                }
            });
        }
    }

    class DialogWebViewClient extends BasicWebViewClient {

        DialogWebViewClient(final Context ctx,
                            final String stopRedirect,
                            final AuthenticationRequest request) {
            super(ctx, stopRedirect, request, null);
        }

        public void showSpinner(final boolean status) {
            if (mHandlerInView != null) {
                mHandlerInView.post(new Runnable() {

                    @Override
                    public void run() {
                        if (mDialog != null && mDialog.isShowing()) {
                            ProgressBar progressBar = (ProgressBar) mDialog
                                    .findViewById(getResourceId(
                                            "com_microsoft_aad_adal_progressBar", "id"));
                            if (progressBar != null) {
                                int showFlag = status ? View.VISIBLE : View.INVISIBLE;
                                progressBar.setVisibility(showFlag);
                            }
                        }
                    }
                });
            }
        }

        public void sendResponse(int returnCode, final Intent responseIntent) {
            // Close this dialog
            mDialog.dismiss();
            mAcquireTokenRequest.onActivityResult(
                    AuthenticationConstants.UIRequest.BROWSER_FLOW,
                    returnCode,
                    responseIntent
            );
        }

        public void postRunnable(Runnable item) {
            mHandlerInView.post(item);
        }

        public void processRedirectUrl(final WebView view, final String url) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, url);
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO, mRequest);
            resultIntent.putExtra(AuthenticationConstants.Browser.REQUEST_ID,
                    mRequest.getRequestId());
            sendResponse(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, resultIntent);
            view.stopLoading();
        }

        @Override
        public void cancelWebViewRequest(@Nullable final Intent errorIntent) {
            cancelFlow(errorIntent);
        }

        @Override
        public void setPKeyAuthStatus(boolean status) {
            // no back press handling is needed here, so it is not required.
        }

        @Override
        public boolean processInvalidUrl(WebView view, String url) {
            return false;
        }

        @Override
        public void prepareForBrokerResumeRequest() {
            // Method intentionally left blank
        }
    }
}
