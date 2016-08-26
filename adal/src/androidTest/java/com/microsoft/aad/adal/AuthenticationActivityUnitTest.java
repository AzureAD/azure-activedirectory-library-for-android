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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.test.ActivityUnitTestCase;
import android.test.RenamingDelegatingContext;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Unit test to verify buttons, webview and other items.
 */
public class AuthenticationActivityUnitTest extends ActivityUnitTestCase<AuthenticationActivity> {

    private static final int TEST_REQUEST_ID = 123;

    private static final int TEST_REQUEST_ID2 = 166;

    private static final long CONTEXT_REQUEST_TIME_OUT = 20000;

    private static final long DEVICE_RESPONSE_WAIT = 500;

    private static final int TEST_CALLING_UID = 333;

    private Intent mIntentToStartActivity;

    private AuthenticationActivity mActivity;

    public AuthenticationActivityUnitTest() {
        super(AuthenticationActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getInstrumentation().getTargetContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext()
                .getCacheDir().getPath());
        Context mockContext = new ActivityMockContext(getInstrumentation().getTargetContext());
        setActivityContext(mockContext);
        mIntentToStartActivity = new Intent(getInstrumentation().getTargetContext(),
                AuthenticationActivity.class);
        Object authorizationRequest = getTestRequest();
        mIntentToStartActivity.putExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE,
                (Serializable) authorizationRequest);
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            // use same key for tests
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
            final int iterations = 100;
            final int keySize = 256;
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes("UTF-8"), iterations, keySize));
            SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }
    }

    private Object getTestRequest() throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {

        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationRequest");

        // getConstructor() returns only public constructors,

        Constructor<?> constructor = c.getDeclaredConstructor(String.class, String.class,
                String.class, String.class, String.class, boolean.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance("authority", "client", "resource", "redirect",
                "loginhint", false);
        ReflectionUtils.setFieldValue(o, "mRequestId", TEST_REQUEST_ID);
        ReflectionUtils.setFieldValue(o, "mCorrelationId", UUID.randomUUID());

        return o;
    }

    @SmallTest
    @UiThreadTest
    public void testLayout() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {

        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();

        // Webview
        WebView webview = (WebView) mActivity.findViewById(R.id.webView1);
        assertNotNull(webview);

        // Javascript enabled
        assertTrue(webview.getSettings().getJavaScriptEnabled());
    }

    @SmallTest
    @UiThreadTest
    public void testReturnToCaller() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {

        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();

        Method returnToCaller = ReflectionUtils.getTestMethod(mActivity, "returnToCaller",
                int.class, Intent.class);

        // call null intent
        returnToCaller.invoke(mActivity, AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL,
                null);
        assertTrue(isFinishCalled());

        // verify result code that includes requestid
        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL);
        assertEquals(TEST_REQUEST_ID,
                data.getIntExtra(AuthenticationConstants.Browser.REQUEST_ID, 0));
    }

    @SmallTest
    @UiThreadTest
    public void testWebviewInstallLink() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InterruptedException, ExecutionException {
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();
        String url = AuthenticationConstants.Broker.BROWSER_EXT_INSTALL_PREFIX
                + "?username=abc@outlook.com&app_link=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dcom.azure.authenticator";
        WebViewClient client = getCustomWebViewClient();
        WebView mockview = new WebView(getActivity().getApplicationContext());
        ReflectionUtils.setFieldValue(mActivity, "mSpinner", null);

        // Act
        client.shouldOverrideUrlLoading(mockview, url);

        // Verify result code that includes requestid. Activity will set the
        // result back to caller.
        TestLogResponse response = new TestLogResponse();
        final CountDownLatch signal = new CountDownLatch(1);
        response.listenForLogMessage("It is an install request", signal);
        int counter = 0;
        final int maxWaitCycles = 20;
        while (!isFinishCalled() && counter < maxWaitCycles) {
            Thread.sleep(DEVICE_RESPONSE_WAIT);
            counter++;
        }

        String savedData = ApplicationReceiver.getInstallRequestInthisApp(getInstrumentation().getTargetContext());
        assertNotNull(savedData);
        assertTrue(savedData.contains("abc@outlook.com"));
    }

    /**
     * Return authentication exception at setResult so that mActivity receives at
     * onActivityResult
     */
    @SmallTest
    @UiThreadTest
    public void testWebviewAuthenticationException() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InterruptedException, ExecutionException {
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();
        AuthenticationSettings.INSTANCE.setDeviceCertificateProxyClass(MockDeviceCertProxy.class);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.setIsValidIssuer(true);
        MockDeviceCertProxy.setPrivateKey(null);
        String url = AuthenticationConstants.Broker.PKEYAUTH_REDIRECT
                + "?Nonce=nonce1234&CertAuthorities=ABC&Version=1.0&SubmitUrl=submiturl&Context=serverContext";
        WebViewClient client = getCustomWebViewClient();
        WebView mockview = new WebView(getActivity().getApplicationContext());
        ReflectionUtils.setFieldValue(mActivity, "mSpinner", null);

        // Act
        client.shouldOverrideUrlLoading(mockview, url);

        // Verify result code that includes requestid. Activity will set the
        // result back to caller.
        TestLogResponse response = new TestLogResponse();
        final CountDownLatch signal = new CountDownLatch(1);
        response.listenForLogMessage("It is failed to create device certificate response", signal);
        int counter = 0;
        final int maxWaitIterations = 20;
        while (!isFinishCalled() && counter < maxWaitIterations) {
            Thread.sleep(DEVICE_RESPONSE_WAIT);
            counter++;
        }

        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION);
        Serializable serialazable = data
                .getSerializableExtra(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION);
        AuthenticationException exception = (AuthenticationException) serialazable;
        assertNotNull("Exception is not null", exception);
        assertEquals("Exception has AdalError for key", ADALError.KEY_CHAIN_PRIVATE_KEY_EXCEPTION,
                exception.getCode());
    }


    @SmallTest
    @UiThreadTest
    public void testWebviewSslprotectedredirectURL() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InterruptedException, ExecutionException {
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();
        /*
         * case 1: url = "http://login.microsoftonline.com/"
         * case 2: url = "https://login.microsoftonline.com/"
         */
        String url = "https://login.microsoftonline.com/";
        WebViewClient client = getCustomWebViewClient();
        WebView mockview = new WebView(getActivity().getApplicationContext());
        ReflectionUtils.setFieldValue(mActivity, "mSpinner", null);
        assertEquals(false, client.shouldOverrideUrlLoading(mockview, url));
    }

    @SmallTest
    @UiThreadTest
    public void testWebviewBlankredirectURL() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InterruptedException, ExecutionException {
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();
        /*
         * case 1: url = "about:blank"
         */
        String url = "about:blank";
        WebViewClient client = getCustomWebViewClient();
        WebView mockview = new WebView(getActivity().getApplicationContext());
        ReflectionUtils.setFieldValue(mActivity, "mSpinner", null);
        assertEquals(true, client.shouldOverrideUrlLoading(mockview, url));
    }


    private WebViewClient getCustomWebViewClient() throws NoSuchMethodException,
            ClassNotFoundException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Class clazz = Class
                .forName("com.microsoft.aad.adal.AuthenticationActivity$CustomWebViewClient");
        Constructor[] constructors = clazz.getDeclaredConstructors();
        constructors[0].setAccessible(true);
        return (WebViewClient) constructors[0].newInstance(getActivity());
    }

    /**
     * mocks webresponse and passes json with idtoken to verify that broker
     * response returns idtoken info
     */
    @SmallTest
    @UiThreadTest
    public void testBrokerReturnUserInfo() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InterruptedException, ExecutionException {
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();
        String urlRequest = "http://taskapp/?code=AwABAAAAvPM1KaPlrEqdFSBzjqfTGMgw4YlsUtpp6LtqhSXUApDSgwF7HWFTPxA9ZKafC_NUbwToIMQl86JD09cKDlRI-2_oxx3o0U3cyFwBGeBvKkBDiP89zMj7hPhe6inwRgjLKbL0qla6OIV9gm54_rrCow3G1bWsH5zuXM3j5YWNV-e9K14G6r6B9Z8etd0a_CgNO7_GkleEHw3voXbJL7v8eeW74tLHHSA46wO0T8JRrnhrUydHGzCSLDJQaYyL5FlQQhkZcN5L6I0G472VEpXNwaviEAkNNcg3BPfe2PUswjwM_OqUBz5xE6KwqJ40GQS53eghcVeZNEUNZXG0KzKbxwDgsPFNQ6XZcaK0uZGmzRm8z8xz9hqfPEJtAl7kAhJ1tltL0nuC-0VoyBEdMLo2JyAA&state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L29tZXJjYW50ZXN0Lm9ubWljcm9zb2Z0LmNvbSZyPWh0dHBzOi8vb21lcmNhbnRlc3Qub25taWNyb3NvZnQuY29tL0FsbEhhbmRzVHJ5&session_state=cba8edc9-91b8-4bb9-8510-2ff9db663258";
        MockWebRequestHandler webrequest = setMockWebResponse();
        ReflectionUtils.setFieldValue(mActivity, "mWebRequestHandler", webrequest);
        String username = "admin@aaltests.onmicrosoft.com";
        final AuthenticationRequest authRequest = new AuthenticationRequest(
                "https://login.windows.net/test.test.com",
                "https://omercantest.onmicrosoft.com/AllHandsTry", "client", "redirect", username, UUID.randomUUID(),
                false);
        Method setAcctName = ReflectionUtils.getTestMethod(authRequest, "setBrokerAccountName",
                String.class);
        setAcctName.invoke(authRequest, username);
        AsyncTask<String, ?, ?> tokenTask = (AsyncTask<String, ?, ?>) getTokenTask();
        Method executeDirect = ReflectionUtils.getTestMethod(tokenTask, "doInBackground",
                String[].class);
        Method executePostResult = ReflectionUtils.getTestMethod(tokenTask, "onPostExecute",
                Class.forName("com.microsoft.aad.adal.AuthenticationActivity$TokenTaskResult"));
        AccountManager mockAct = mock(AccountManager.class);
        Account userAccount = new Account(username,
                AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        when(mockAct.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE))
                .thenReturn(new Account[]{
                        userAccount
                });
        ReflectionUtils.setFieldValue(tokenTask, "mRequest", authRequest);
        ReflectionUtils.setFieldValue(tokenTask, "mPackageName", "testpackagename");
        ReflectionUtils.setFieldValue(tokenTask, "mAccountManager", mockAct);
        ReflectionUtils.setFieldValue(tokenTask, "mRequestHandler", webrequest);
        ReflectionUtils.setFieldValue(tokenTask, "mAppCallingUID", TEST_CALLING_UID);
        Object result = executeDirect.invoke(tokenTask, (Object) new String[]{
                urlRequest
        });

        executePostResult.invoke(tokenTask, result);

        // Verification from returned intent data
        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE);
        assertEquals("token is same in the result", "TokentestBroker",
                data.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_ACCESS_TOKEN));
        assertEquals("Name is same in the result", "admin@aaltests.onmicrosoft.com",
                data.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_NAME));
        assertEquals("UserId is same in the result", "4f859989-a2ff-411e-9048-c322247ac62c",
                data.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID));
        assertEquals(
                "UserId is same in the result",
                "admin@aaltests.onmicrosoft.com",
                data.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID_DISPLAYABLE));
        assertNotNull(data
                .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_GIVEN_NAME));
        assertNotNull(data
                .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_USERINFO_FAMILY_NAME));
    }

    @SmallTest
    @UiThreadTest
    public void testBrokerReturnUserInfoSingleUser() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InterruptedException, ExecutionException {
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();
        String urlRequest = "http://taskapp/?code=AwABAAAAvPM1KaPlrEqdFSBzjqfTGMgw4YlsUtpp6LtqhSXUApDSgwF7HWFTPxA9ZKafC_NUbwToIMQl86JD09cKDlRI-2_oxx3o0U3cyFwBGeBvKkBDiP89zMj7hPhe6inwRgjLKbL0qla6OIV9gm54_rrCow3G1bWsH5zuXM3j5YWNV-e9K14G6r6B9Z8etd0a_CgNO7_GkleEHw3voXbJL7v8eeW74tLHHSA46wO0T8JRrnhrUydHGzCSLDJQaYyL5FlQQhkZcN5L6I0G472VEpXNwaviEAkNNcg3BPfe2PUswjwM_OqUBz5xE6KwqJ40GQS53eghcVeZNEUNZXG0KzKbxwDgsPFNQ6XZcaK0uZGmzRm8z8xz9hqfPEJtAl7kAhJ1tltL0nuC-0VoyBEdMLo2JyAA&state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L29tZXJjYW50ZXN0Lm9ubWljcm9zb2Z0LmNvbSZyPWh0dHBzOi8vb21lcmNhbnRlc3Qub25taWNyb3NvZnQuY29tL0FsbEhhbmRzVHJ5&session_state=cba8edc9-91b8-4bb9-8510-2ff9db663258";
        MockWebRequestHandler webrequest = setMockWebResponse();
        ReflectionUtils.setFieldValue(mActivity, "mWebRequestHandler", webrequest);
        final AuthenticationRequest authRequest = new AuthenticationRequest(
                "https://login.windows.net/test.test.com",
                "https://omercantest.onmicrosoft.com/AllHandsTry", "client", "redirect",
                "different@aaltests.onmicrosoft.com", UUID.randomUUID(), false);
        AsyncTask<String, ?, ?> tokenTask = (AsyncTask<String, ?, ?>) getTokenTask();
        Method executeDirect = ReflectionUtils.getTestMethod(tokenTask, "doInBackground",
                String[].class);
        Method executePostResult = ReflectionUtils.getTestMethod(tokenTask, "onPostExecute",
                Class.forName("com.microsoft.aad.adal.AuthenticationActivity$TokenTaskResult"));
        AccountManager mockAct = mock(AccountManager.class);
        when(mockAct.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE))
                .thenReturn(new Account[] {});
        ReflectionUtils.setFieldValue(tokenTask, "mRequest", authRequest);
        ReflectionUtils.setFieldValue(tokenTask, "mPackageName", "testpackagename");
        ReflectionUtils.setFieldValue(tokenTask, "mAccountManager", mockAct);
        ReflectionUtils.setFieldValue(tokenTask, "mRequestHandler", webrequest);
        ReflectionUtils.setFieldValue(tokenTask, "mAppCallingUID", TEST_CALLING_UID);
        Object result = executeDirect.invoke(tokenTask, (Object) new String[]{
                urlRequest
        });

        executePostResult.invoke(tokenTask, result);

        // Verification from returned intent data
        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR);
        assertTrue("Returns error about user",
                data.getStringExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE)
                        .contains(ADALError.BROKER_SINGLE_USER_EXPECTED.getDescription()));

    }

    @SmallTest
    @UiThreadTest
    public void testBrokerSaveCacheKey() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InterruptedException, ExecutionException {
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();
        String urlRequest = "http://taskapp/?code=AwABAAAAvPM1KaPlrEqdFSBzjqfTGMgw4YlsUtpp6LtqhSXUApDSgwF7HWFTPxA9ZKafC_NUbwToIMQl86JD09cKDlRI-2_oxx3o0U3cyFwBGeBvKkBDiP89zMj7hPhe6inwRgjLKbL0qla6OIV9gm54_rrCow3G1bWsH5zuXM3j5YWNV-e9K14G6r6B9Z8etd0a_CgNO7_GkleEHw3voXbJL7v8eeW74tLHHSA46wO0T8JRrnhrUydHGzCSLDJQaYyL5FlQQhkZcN5L6I0G472VEpXNwaviEAkNNcg3BPfe2PUswjwM_OqUBz5xE6KwqJ40GQS53eghcVeZNEUNZXG0KzKbxwDgsPFNQ6XZcaK0uZGmzRm8z8xz9hqfPEJtAl7kAhJ1tltL0nuC-0VoyBEdMLo2JyAA&state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L29tZXJjYW50ZXN0Lm9ubWljcm9zb2Z0LmNvbSZyPWh0dHBzOi8vb21lcmNhbnRlc3Qub25taWNyb3NvZnQuY29tL0FsbEhhbmRzVHJ5&session_state=cba8edc9-91b8-4bb9-8510-2ff9db663258";
        MockWebRequestHandler webrequest = setMockWebResponse();
        String username = "admin@aaltests.onmicrosoft.com";
        final AuthenticationRequest authRequest = new AuthenticationRequest(
                "https://login.windows.net/test.test.com",
                "https://omercantest.onmicrosoft.com/AllHandsTry", "client", "redirect", username, UUID.randomUUID(),
                false);
        Method setAcctName = ReflectionUtils.getTestMethod(authRequest, "setBrokerAccountName",
                String.class);
        setAcctName.invoke(authRequest, username);
        AsyncTask<String, ?, ?> tokenTask = (AsyncTask<String, ?, ?>) getTokenTask();
        Method executeDirect = ReflectionUtils.getTestMethod(tokenTask, "doInBackground",
                String[].class);
        Method executePostResult = ReflectionUtils.getTestMethod(tokenTask, "onPostExecute",
                Class.forName("com.microsoft.aad.adal.AuthenticationActivity$TokenTaskResult"));
        final int additionalCallerCacheKeys = 333;
        AccountManager mockAct = mock(AccountManager.class);
        when(
                mockAct.getUserData(any(Account.class),
                        eq(AuthenticationConstants.Broker.USERDATA_CALLER_CACHEKEYS + additionalCallerCacheKeys)))
                .thenReturn("test");
        Account userAccount = new Account(username,
                AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        when(mockAct.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE))
                .thenReturn(new Account[]{
                        userAccount
                });
        ReflectionUtils.setFieldValue(tokenTask, "mRequest", authRequest);
        ReflectionUtils.setFieldValue(tokenTask, "mPackageName", "testpackagename");
        ReflectionUtils.setFieldValue(tokenTask, "mAccountManager", mockAct);
        ReflectionUtils.setFieldValue(tokenTask, "mRequestHandler", webrequest);
        ReflectionUtils.setFieldValue(tokenTask, "mAppCallingUID", TEST_CALLING_UID);
        Object result = executeDirect.invoke(tokenTask, (Object) new String[]{
                urlRequest
        });

        executePostResult.invoke(tokenTask, result);

        // Verification from returned intent data
        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE);
        final int numerOfCalls = 8;
        verify(mockAct, times(numerOfCalls)).setUserData(any(Account.class), anyString(), anyString());
    }

    private MockWebRequestHandler setMockWebResponse() throws NoSuchFieldException,
            IllegalAccessException {
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";
        String json = "{\"id_token\":"
                + idToken
                + ",\"access_token\":\"TokentestBroker\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        ReflectionUtils.setFieldValue(mActivity, "mWebRequestHandler", webrequest);
        webrequest.setReturnResponse(new HttpWebResponse(HttpURLConnection.HTTP_OK, json, null));
        return webrequest;
    }

    @SmallTest
    @UiThreadTest
    public void testOnResumeRestartWebview() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            InterruptedException {
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();
        final TestLogResponse logResponse = new TestLogResponse();
        logResponse.listenForLogMessage(
                "Webview onResume register broadcast receiver for requestId" + TEST_REQUEST_ID,
                null);
        ReflectionUtils.setFieldValue(mActivity, "mRegisterReceiver", true);
        Method methodOnResume = ReflectionUtils.getTestMethod(mActivity, "onResume");
        methodOnResume.invoke(mActivity);

        // get field value to check
        assertTrue("verify log message",
                logResponse.getMessage().contains("Webview onResume register broadcast"));
    }

    @SmallTest
    @UiThreadTest
    public void testOnBackPressed() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, InterruptedException {
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();

        mActivity.onBackPressed();

        assertTrue(isFinishCalled());

        // verify result code that includes requestid
        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL);
        assertEquals(TEST_REQUEST_ID,
                data.getIntExtra(AuthenticationConstants.Browser.REQUEST_ID, 0));
    }

    @SmallTest
    @UiThreadTest
    public void testOnRestart() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, InterruptedException {

        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();

        ReflectionUtils.setFieldValue(mActivity, "mRegisterReceiver", false);
        Method methodOnResume = ReflectionUtils.getTestMethod(mActivity, "onRestart");

        methodOnResume.invoke(mActivity);

        // get field value to check
        boolean fieldVal = (Boolean) ReflectionUtils.getFieldValue(mActivity, "mRegisterReceiver");
        assertTrue("RestartWebview set to true", fieldVal);
    }

    @SmallTest
    @UiThreadTest
    public void testEmptyIntentData() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException {

        mIntentToStartActivity.putExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE, "");
        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();

        assertTrue(isFinishCalled());

        // verify result code
        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR);
        assertEquals(AuthenticationConstants.Browser.WEBVIEW_INVALID_REQUEST,
                data.getStringExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE));
    }

    @SmallTest
    @UiThreadTest
    public void testReceiver() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, InvocationTargetException, InterruptedException {

        startActivity(mIntentToStartActivity, null, null);
        mActivity = getActivity();
        String broadcastCancelMsg1 = "ActivityBroadcastReceiver onReceive action is for cancelling Authentication Activity";
        String broadcastCancelMsg2 = "Waiting requestId is same and cancelling this activity";

        // Test onReceive call with wrong request id
        TestLogResponse response = new TestLogResponse();
        final CountDownLatch signal = new CountDownLatch(1);
        response.listenForLogMessage(broadcastCancelMsg1, signal);
        BroadcastReceiver receiver = (BroadcastReceiver) ReflectionUtils.getFieldValue(mActivity,
                "mReceiver");
        final Intent intent = new Intent(AuthenticationConstants.Browser.ACTION_CANCEL);
        final Bundle extras = new Bundle();
        intent.putExtras(extras);
        intent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, TEST_REQUEST_ID2);

        receiver.onReceive(getInstrumentation().getTargetContext(), intent);

        // Test onReceive call with correct request id
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        assertTrue("log the message for correct Intent",
                response.getMessage().contains(broadcastCancelMsg1));

        // update requestId to match the AuthenticationRequest
        final CountDownLatch signal2 = new CountDownLatch(1);
        TestLogResponse response2 = new TestLogResponse();
        response2.listenForLogMessage(broadcastCancelMsg2, signal2);
        final Intent intent2 = new Intent(AuthenticationConstants.Browser.ACTION_CANCEL);
        intent2.putExtras(extras);
        intent2.putExtra(AuthenticationConstants.Browser.REQUEST_ID, TEST_REQUEST_ID);
        receiver.onReceive(getInstrumentation().getTargetContext(), intent2);

        // verify that it received intent
        signal2.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        assertTrue("log the message for correct Intent",
                response2.getMessage().contains(broadcastCancelMsg2));
    }

    @SmallTest
    @UiThreadTest
    public void testWebviewHardwareAccelerationDisable() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException {

        //By default hardware acceleration should be enable.
        assertTrue(AuthenticationSettings.INSTANCE.getDisableWebViewHardwareAcceleration());

        // Disable webview hardware acceleration
        AuthenticationSettings.INSTANCE.setDisableWebViewHardwareAcceleration(false);

        startActivity(mIntentToStartActivity, null, null);

        mActivity = getActivity();

        // get field value to check
        WebView webView = (WebView) ReflectionUtils.getFieldValue(mActivity, "mWebView");

        // Assert WebView is not null
        assertNotNull("WebView:: ", webView);

        // If LayerType is LAYER_TYPE_SOFTWARE then HardwareAcceleration would be disabled
        assertEquals("LayerType", WebView.LAYER_TYPE_SOFTWARE, webView.getLayerType());

        // Reset hardware acceleration to default value.
        AuthenticationSettings.INSTANCE.setDisableWebViewHardwareAcceleration(true);
    }

    @SmallTest
    @UiThreadTest
    public void testWebviewHardwareAccelerationEnable() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException {

        startActivity(mIntentToStartActivity, null, null);

        mActivity = getActivity();

        // get field value to check
        WebView webView = (WebView) ReflectionUtils.getFieldValue(mActivity, "mWebView");

        // Assert WebView is not null
        assertNotNull("WebView:: ", webView);

        // In case if webview is hardware accelerated then 
        // its layer type should not be LAYER_TYPE_SOFTWARE
        assertNotSame("LayerType", WebView.LAYER_TYPE_SOFTWARE, webView.getLayerType());
    }

    @Override
    protected void tearDown() throws Exception {

        super.tearDown();
    }

    protected Intent assertFinishCalledWithResult(int resultCode) throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        assertTrue(isFinishCalled());
        Field f = Activity.class.getDeclaredField("mResultCode");
        f.setAccessible(true);
        int actualResultCode = (Integer) f.get(getActivity());
        assertEquals(actualResultCode, resultCode);

        f = Activity.class.getDeclaredField("mResultData");
        f.setAccessible(true);
        return (Intent) f.get(getActivity());
    }

    private Object getTokenTask() throws ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Class<?> c = Class.forName("com.microsoft.aad.adal.AuthenticationActivity$TokenTask");
        Constructor<?> constructorParams = c.getConstructor(getActivity().getClass());
        constructorParams.setAccessible(true);
        return constructorParams.newInstance(getActivity());
    }

    /**
     * this is a class which delegates to the given context, but performs
     * database and file operations with a renamed database/file name (prefixes
     * default names with a given prefix).
     */
    class ActivityMockContext extends RenamingDelegatingContext {

        private static final String MOCK_FILE_PREFIX = "test.";

        /**
         * @param context
         * @param filePrefix
         */
        public ActivityMockContext(Context context) {
            super(context, MOCK_FILE_PREFIX);
            makeExistingFilesAndDbsAccessible();
        }
    }
}
