package com.microsoft.aad.adal;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.UiThread;
import android.test.AndroidTestCase;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.webkit.WebView;

import org.mockito.Mockito;

public class BasicWebViewClientTests extends AndroidTestCase {

    private static final String sNonce = "123123-123213-123";
    private static final String sContext = "ABcdeded";
    private static final String sTestPkeyAuthUrl = AuthenticationConstants.Broker.PKEYAUTH_REDIRECT
            + "?Nonce="
            + sNonce
            + "&CertAuthorities=ABC"
            + "&Version=1.0"
            + "&SubmitUrl=http://fs.contoso.com/adfs/services/trust&Context="
            + sContext;
    private static final String sTestCancellationUrl = "https://cancel.com?error=cancel&error_description=bye";
    private static final String sTestExternalSiteUrl = "";
    private static final String sTestInstallRequestUrl = "";

    private WebView mMockWebView;

    @UiThread
    @Override
    protected void setUp() throws Exception {
        mMockWebView = Mockito.mock(WebView.class);
        // Since BasicWebViewClient is abstract,
        // we'll subclass the entire class under test
        MockDeviceCertProxy.reset();
    }

    private BasicWebViewClient setUpWebViewClient(
            final Context context,
            final String redirect,
            final AuthenticationRequest request,
            final UIEvent uiEvent) {
        return new BasicWebViewClient(context, redirect, request, uiEvent) {
            @Override
            public void showSpinner(boolean status) {
                // Do nothing. Test Object.
            }

            @Override
            public void sendResponse(int returnCode, Intent responseIntent) {
                // Do nothing. Test Object.
            }

            @Override
            public void cancelWebViewRequest() {
                // Do nothing. Test Object.
            }

            @Override
            public void prepareForBrokerResumeRequest() {
                // Do nothing. Test Object.
            }

            @Override
            public void setPKeyAuthStatus(boolean status) {
                // Do nothing. Test Object.
            }

            @Override
            public void postRunnable(Runnable item) {
                // Do nothing. Test Object.
            }

            @Override
            public void processRedirectUrl(WebView view, String url) {
                // Do nothing. Test Object.
            }

            @Override
            public boolean processInvalidUrl(WebView view, String url) {
                return false;
            }
        };
    }

    @UiThreadTest
    @SmallTest
    public void testUrlOverrideHandlesPKeyAuthRedirect() {
        final BasicWebViewClient basicWebViewClient =
                setUpWebViewClient(
                        getContext(),
                        "",
                        new AuthenticationRequest(
                                "NA", // authority
                                "NA", // resource
                                "NA", // client
                                "NA", // redirect
                                "user", // loginhint,
                                false
                        ),
                        new UIEvent("")
                );
        assertTrue(basicWebViewClient.shouldOverrideUrlLoading(mMockWebView, sTestPkeyAuthUrl));
    }

    @UiThreadTest
    @SmallTest
    public void testUrlOverrideHandlesCancellation() {
        final BasicWebViewClient basicWebViewClient =
                setUpWebViewClient(
                        getContext(),
                        sTestCancellationUrl,
                        new AuthenticationRequest(
                                "NA", // authority
                                "NA", // resource
                                "NA", // client
                                sTestCancellationUrl, // redirect
                                "user", // loginhint,
                                false
                        ),
                        new UIEvent("")
                );
        assertTrue(basicWebViewClient.shouldOverrideUrlLoading(mMockWebView, sTestCancellationUrl));
        Mockito.verify(mMockWebView, Mockito.times(1)).stopLoading();
    }

    @SmallTest
    public void testUrlOverrideHandlesExternalSiteRequests() {

    }

    @SmallTest
    public void testUrlOverrideHandlesInstallRequests() {

    }
}
