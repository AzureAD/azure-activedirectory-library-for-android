package com.microsoft.aad.adal;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.UiThread;
import android.test.AndroidTestCase;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.webkit.WebView;

import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    private static final String sTestCancellationUrl =
            "https://cancel.com?error=cancel&error_description=bye";
    private static final String sTestExternalSiteUrl =
            AuthenticationConstants.Broker.BROWSER_EXT_PREFIX + "https://graph.microsoft.io";
    private static final String sTestInstallRequestUrl =
            AuthenticationConstants.Broker.BROWSER_EXT_INSTALL_PREFIX + "foo";

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
    public void testUrlOverrideHandlesExternalSiteRequests() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final BasicWebViewClient dummyClient = new BasicWebViewClient(
                getContext(),
                "www.redirect.com",
                new AuthenticationRequest(
                        "NA",
                        "NA",
                        "NA",
                        "NA",
                        "user",
                        false
                ),
                new UIEvent("")) {
            @Override
            public void showSpinner(boolean status) {
                // Not under test
            }

            @Override
            public void sendResponse(int returnCode, Intent responseIntent) {
                // Not under test
            }

            @Override
            public void cancelWebViewRequest() {
                countDownLatch.countDown();
            }

            @Override
            public void prepareForBrokerResumeRequest() {
                // Not under test
            }

            @Override
            public void setPKeyAuthStatus(boolean status) {
                // Not under test
            }

            @Override
            public void postRunnable(Runnable item) {
                // Not under test
            }

            @Override
            public void processRedirectUrl(WebView view, String url) {
                // Not under test
            }

            @Override
            public boolean processInvalidUrl(WebView view, String url) {
                return false;
            }

            @Override
            protected void openLinkInBrowser(String url) {
                assertEquals(url, sTestExternalSiteUrl);
                countDownLatch.countDown();
            }
        };

        // Load the external url
        dummyClient.shouldOverrideUrlLoading(mMockWebView, sTestExternalSiteUrl);

        // Since we can neither spy() nor mock() this class (pkg private)
        // we're going to use a CountDownLatch that gets decremented in the
        // overridden methods we would normally verify()
        if (!countDownLatch.await(1, TimeUnit.SECONDS)) {
            fail();
        }

        Mockito.verify(mMockWebView).stopLoading();
    }

    @SmallTest
    public void testUrlOverrideHandlesInstallRequests() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final BasicWebViewClient dummyClient = new BasicWebViewClient(
                getContext(),
                sTestInstallRequestUrl,
                new AuthenticationRequest(
                        "NA",
                        "NA",
                        "NA",
                        "NA",
                        "user",
                        false
                ),
                new UIEvent("")) {
            @Override
            public void showSpinner(boolean status) {
                // Not under test
            }

            @Override
            public void sendResponse(int returnCode, Intent responseIntent) {
                // Not under test
            }

            @Override
            public void cancelWebViewRequest() {
                // Not under test
            }

            @Override
            public void prepareForBrokerResumeRequest() {
                countDownLatch.countDown();
            }

            @Override
            public void setPKeyAuthStatus(boolean status) {
                // Not under test
            }

            @Override
            public void postRunnable(Runnable item) {
                // Not under test
            }

            @Override
            public void processRedirectUrl(WebView view, String url) {
                // Not under test
            }

            @Override
            public boolean processInvalidUrl(WebView view, String url) {
                return false;
            }

            @Override
            protected void openLinkInBrowser(String url) {
                assertEquals(url, "myapplink.com");
                countDownLatch.countDown();
            }
        };

        dummyClient.shouldOverrideUrlLoading(
                mMockWebView,
                AuthenticationConstants.Broker.BROWSER_EXT_INSTALL_PREFIX
                        + "https://testdomain.com?app_link=myapplink.com"
        );

        if (!countDownLatch.await(1, TimeUnit.SECONDS)) {
            fail();
        }
    }
}
