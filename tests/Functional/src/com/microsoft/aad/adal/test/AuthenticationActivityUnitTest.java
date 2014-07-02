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

package com.microsoft.aad.adal.test;

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
import java.nio.charset.Charset;
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
import android.app.ProgressDialog;
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
import android.widget.Button;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationActivity;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.HttpWebResponse;
import com.microsoft.aad.adal.R;

/**
 * Unit test to verify buttons, webview and other items.
 */
public class AuthenticationActivityUnitTest extends ActivityUnitTestCase<AuthenticationActivity> {

    private static final int TEST_REQUEST_ID = 123;

    private static final long CONTEXT_REQUEST_TIME_OUT = 20000;

    private static final long DEVICE_RESPONSE_WAIT = 500;

    private int buttonId;

    private Intent intentToStartActivity;

    private AuthenticationActivity activity;

    public AuthenticationActivityUnitTest() {
        super(AuthenticationActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext()
                .getCacheDir().getPath());
        Context mockContext = new ActivityMockContext(getInstrumentation().getTargetContext());
        setActivityContext(mockContext);
        intentToStartActivity = new Intent(getInstrumentation().getTargetContext(),
                AuthenticationActivity.class);
        Object authorizationRequest = getTestRequest();
        intentToStartActivity.putExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE,
                (Serializable)authorizationRequest);
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            // use same key for tests
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes("UTF-8"), 100, 256));
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
                String.class, String.class, String.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance("authority", "client", "resource", "redirect",
                "loginhint");
        ReflectionUtils.setFieldValue(o, "mRequestId", TEST_REQUEST_ID);

        return o;
    }

    @SmallTest
    @UiThreadTest
    public void testLayout() throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {

        startActivity(intentToStartActivity, null, null);
        activity = getActivity();

        // Cancel button
        buttonId = com.microsoft.aad.adal.R.id.btnCancel;
        assertNotNull(activity.findViewById(buttonId));
        Button view = (Button)activity.findViewById(buttonId);
        String text = activity.getResources().getString(R.string.button_cancel);
        assertEquals("Incorrect label of the button", text, view.getText());

        // Webview
        WebView webview = (WebView)activity.findViewById(R.id.webView1);
        assertNotNull(webview);

        // Javascript enabled
        assertTrue(webview.getSettings().getJavaScriptEnabled());

        // Spinner
        Field f = AuthenticationActivity.class.getDeclaredField("spinner");
        f.setAccessible(true);
        ProgressDialog spinner = (ProgressDialog)f.get(getActivity());
        assertNotNull(spinner);
    }

    @SmallTest
    @UiThreadTest
    public void testReturnToCaller() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {

        startActivity(intentToStartActivity, null, null);
        activity = getActivity();

        Method returnToCaller = ReflectionUtils.getTestMethod(activity, "returnToCaller",
                int.class, Intent.class);

        // call null intent
        returnToCaller.invoke(activity, AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL,
                null);
        assertTrue(isFinishCalled());

        // verify result code that includes requestid
        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL);
        assertEquals(TEST_REQUEST_ID,
                data.getIntExtra(AuthenticationConstants.Browser.REQUEST_ID, 0));
    }

    /**
     * Return authentication exception at setResult so that activity receives at
     * onActivityResult
     * 
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @SmallTest
    @UiThreadTest
    public void testWebview_AuthenticationException() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InterruptedException, ExecutionException {
        startActivity(intentToStartActivity, null, null);
        activity = getActivity();
        AuthenticationSettings.INSTANCE.setDeviceCertificateProxyClass(MockDeviceCertProxy.class);
        MockDeviceCertProxy.reset();
        MockDeviceCertProxy.sValidIssuer = true;
        MockDeviceCertProxy.sPrivateKey = null;
        String url = AuthenticationConstants.Broker.CLIENT_TLS_REDIRECT
                + "?Nonce=nonce1234&CertAuthorities=ABC&Version=1.0&SubmitUrl=submiturl&Context=serverContext";
        WebViewClient client = getCustomWebViewClient();
        WebView mockview = new WebView(getActivity().getApplicationContext());
        ReflectionUtils.setFieldValue(activity, "spinner", null);

        // Act
        client.shouldOverrideUrlLoading(mockview, url);

        // Verify result code that includes requestid. Activity will set the
        // result back to caller.
        TestLogResponse response = new TestLogResponse();
        final CountDownLatch signal = new CountDownLatch(1);
        response.listenForLogMessage("It is failed to create device certificate response", signal);
        int counter = 0;
        while (!isFinishCalled() && counter < 20) {
            Thread.sleep(DEVICE_RESPONSE_WAIT);
            counter++;
        }

        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION);
        Serializable serialazable = data
                .getSerializableExtra(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION);
        AuthenticationException exception = (AuthenticationException)serialazable;
        assertNotNull("Exception is not null", exception);
        assertEquals("Exception has AdalError for key", ADALError.KEY_CHAIN_PRIVATE_KEY_EXCEPTION,
                exception.getCode());
    }

    private WebViewClient getCustomWebViewClient() throws NoSuchMethodException,
            ClassNotFoundException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Class clazz = Class
                .forName("com.microsoft.aad.adal.AuthenticationActivity$CustomWebViewClient");
        Constructor[] constructors = clazz.getDeclaredConstructors();
        constructors[0].setAccessible(true);
        return (WebViewClient)constructors[0].newInstance(getActivity());
    }

    /**
     * mocks webresponse and passes json with idtoken to verify that broker
     * response returns idtoken info
     * 
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @SmallTest
    @UiThreadTest
    public void testBroker_ReturnUserInfo() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InterruptedException, ExecutionException {
        startActivity(intentToStartActivity, null, null);
        activity = getActivity();
        String urlRequest = "http://taskapp/?code=AwABAAAAvPM1KaPlrEqdFSBzjqfTGMgw4YlsUtpp6LtqhSXUApDSgwF7HWFTPxA9ZKafC_NUbwToIMQl86JD09cKDlRI-2_oxx3o0U3cyFwBGeBvKkBDiP89zMj7hPhe6inwRgjLKbL0qla6OIV9gm54_rrCow3G1bWsH5zuXM3j5YWNV-e9K14G6r6B9Z8etd0a_CgNO7_GkleEHw3voXbJL7v8eeW74tLHHSA46wO0T8JRrnhrUydHGzCSLDJQaYyL5FlQQhkZcN5L6I0G472VEpXNwaviEAkNNcg3BPfe2PUswjwM_OqUBz5xE6KwqJ40GQS53eghcVeZNEUNZXG0KzKbxwDgsPFNQ6XZcaK0uZGmzRm8z8xz9hqfPEJtAl7kAhJ1tltL0nuC-0VoyBEdMLo2JyAA&state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L29tZXJjYW50ZXN0Lm9ubWljcm9zb2Z0LmNvbSZyPWh0dHBzOi8vb21lcmNhbnRlc3Qub25taWNyb3NvZnQuY29tL0FsbEhhbmRzVHJ5&session_state=cba8edc9-91b8-4bb9-8510-2ff9db663258";
        MockWebRequestHandler webrequest = setMockWebResponse();
        ReflectionUtils.setFieldValue(activity, "mWebRequestHandler", webrequest);
        String username = "admin@aaltests.onmicrosoft.com";
        Object authRequest = AuthenticationContextTest.createAuthenticationRequest(
                "https://login.windows.net/test.test.com",
                "https://omercantest.onmicrosoft.com/AllHandsTry", "client", "redirect", username);
        Method setAcctName = ReflectionUtils.getTestMethod(authRequest, "setBrokerAccountName",
                String.class);
        setAcctName.invoke(authRequest, username);
        AsyncTask<String, ?, ?> tokenTask = (AsyncTask<String, ?, ?>)getTokenTask();
        Method executeDirect = ReflectionUtils.getTestMethod(tokenTask, "doInBackground",
                String[].class);
        Method executePostResult = ReflectionUtils.getTestMethod(tokenTask, "onPostExecute",
                Class.forName("com.microsoft.aad.adal.AuthenticationActivity$TokenTaskResult"));
        AccountManager mockAct = mock(AccountManager.class);
        Account userAccount = new Account(username,
                AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        when(mockAct.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE))
                .thenReturn(new Account[] {
                    userAccount
                });
        ReflectionUtils.setFieldValue(tokenTask, "mRequest", authRequest);
        ReflectionUtils.setFieldValue(tokenTask, "mPackageName", "testpackagename");
        ReflectionUtils.setFieldValue(tokenTask, "mAccountManager", mockAct);
        ReflectionUtils.setFieldValue(tokenTask, "mRequestHandler", webrequest);
        ReflectionUtils.setFieldValue(tokenTask, "mAppCallingUID", 333);
        Object result = executeDirect.invoke(tokenTask, (Object)new String[] {
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
    public void testBroker_ReturnUserInfo_SingleUser() throws IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            InterruptedException, ExecutionException {
        startActivity(intentToStartActivity, null, null);
        activity = getActivity();
        String urlRequest = "http://taskapp/?code=AwABAAAAvPM1KaPlrEqdFSBzjqfTGMgw4YlsUtpp6LtqhSXUApDSgwF7HWFTPxA9ZKafC_NUbwToIMQl86JD09cKDlRI-2_oxx3o0U3cyFwBGeBvKkBDiP89zMj7hPhe6inwRgjLKbL0qla6OIV9gm54_rrCow3G1bWsH5zuXM3j5YWNV-e9K14G6r6B9Z8etd0a_CgNO7_GkleEHw3voXbJL7v8eeW74tLHHSA46wO0T8JRrnhrUydHGzCSLDJQaYyL5FlQQhkZcN5L6I0G472VEpXNwaviEAkNNcg3BPfe2PUswjwM_OqUBz5xE6KwqJ40GQS53eghcVeZNEUNZXG0KzKbxwDgsPFNQ6XZcaK0uZGmzRm8z8xz9hqfPEJtAl7kAhJ1tltL0nuC-0VoyBEdMLo2JyAA&state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L29tZXJjYW50ZXN0Lm9ubWljcm9zb2Z0LmNvbSZyPWh0dHBzOi8vb21lcmNhbnRlc3Qub25taWNyb3NvZnQuY29tL0FsbEhhbmRzVHJ5&session_state=cba8edc9-91b8-4bb9-8510-2ff9db663258";
        MockWebRequestHandler webrequest = setMockWebResponse();
        ReflectionUtils.setFieldValue(activity, "mWebRequestHandler", webrequest);
        Object authRequest = AuthenticationContextTest.createAuthenticationRequest(
                "https://login.windows.net/test.test.com",
                "https://omercantest.onmicrosoft.com/AllHandsTry", "client", "redirect",
                "different@aaltests.onmicrosoft.com");
        AsyncTask<String, ?, ?> tokenTask = (AsyncTask<String, ?, ?>)getTokenTask();
        Method executeDirect = ReflectionUtils.getTestMethod(tokenTask, "doInBackground",
                String[].class);
        Method executePostResult = ReflectionUtils.getTestMethod(tokenTask, "onPostExecute",
                Class.forName("com.microsoft.aad.adal.AuthenticationActivity$TokenTaskResult"));
        AccountManager mockAct = mock(AccountManager.class);
        when(mockAct.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE))
                .thenReturn(null);
        ReflectionUtils.setFieldValue(tokenTask, "mRequest", authRequest);
        ReflectionUtils.setFieldValue(tokenTask, "mPackageName", "testpackagename");
        ReflectionUtils.setFieldValue(tokenTask, "mAccountManager", mockAct);
        ReflectionUtils.setFieldValue(tokenTask, "mRequestHandler", webrequest);
        ReflectionUtils.setFieldValue(tokenTask, "mAppCallingUID", 333);
        Object result = executeDirect.invoke(tokenTask, (Object)new String[] {
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
    public void testBroker_SaveCacheKey() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InterruptedException, ExecutionException {
        startActivity(intentToStartActivity, null, null);
        activity = getActivity();
        String urlRequest = "http://taskapp/?code=AwABAAAAvPM1KaPlrEqdFSBzjqfTGMgw4YlsUtpp6LtqhSXUApDSgwF7HWFTPxA9ZKafC_NUbwToIMQl86JD09cKDlRI-2_oxx3o0U3cyFwBGeBvKkBDiP89zMj7hPhe6inwRgjLKbL0qla6OIV9gm54_rrCow3G1bWsH5zuXM3j5YWNV-e9K14G6r6B9Z8etd0a_CgNO7_GkleEHw3voXbJL7v8eeW74tLHHSA46wO0T8JRrnhrUydHGzCSLDJQaYyL5FlQQhkZcN5L6I0G472VEpXNwaviEAkNNcg3BPfe2PUswjwM_OqUBz5xE6KwqJ40GQS53eghcVeZNEUNZXG0KzKbxwDgsPFNQ6XZcaK0uZGmzRm8z8xz9hqfPEJtAl7kAhJ1tltL0nuC-0VoyBEdMLo2JyAA&state=YT1odHRwczovL2xvZ2luLndpbmRvd3MubmV0L29tZXJjYW50ZXN0Lm9ubWljcm9zb2Z0LmNvbSZyPWh0dHBzOi8vb21lcmNhbnRlc3Qub25taWNyb3NvZnQuY29tL0FsbEhhbmRzVHJ5&session_state=cba8edc9-91b8-4bb9-8510-2ff9db663258";
        MockWebRequestHandler webrequest = setMockWebResponse();
        String username = "admin@aaltests.onmicrosoft.com";
        Object authRequest = AuthenticationContextTest.createAuthenticationRequest(
                "https://login.windows.net/test.test.com",
                "https://omercantest.onmicrosoft.com/AllHandsTry", "client", "redirect", username);
        Method setAcctName = ReflectionUtils.getTestMethod(authRequest, "setBrokerAccountName",
                String.class);
        setAcctName.invoke(authRequest, username);
        AsyncTask<String, ?, ?> tokenTask = (AsyncTask<String, ?, ?>)getTokenTask();
        Method executeDirect = ReflectionUtils.getTestMethod(tokenTask, "doInBackground",
                String[].class);
        Method executePostResult = ReflectionUtils.getTestMethod(tokenTask, "onPostExecute",
                Class.forName("com.microsoft.aad.adal.AuthenticationActivity$TokenTaskResult"));
        AccountManager mockAct = mock(AccountManager.class);
        when(
                mockAct.getUserData(any(Account.class),
                        eq(AuthenticationConstants.Broker.USERDATA_CALLER_CACHEKEYS + 333)))
                .thenReturn("test");
        Account userAccount = new Account(username,
                AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        when(mockAct.getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE))
                .thenReturn(new Account[] {
                    userAccount
                });
        ReflectionUtils.setFieldValue(tokenTask, "mRequest", authRequest);
        ReflectionUtils.setFieldValue(tokenTask, "mPackageName", "testpackagename");
        ReflectionUtils.setFieldValue(tokenTask, "mAccountManager", mockAct);
        ReflectionUtils.setFieldValue(tokenTask, "mRequestHandler", webrequest);
        ReflectionUtils.setFieldValue(tokenTask, "mAppCallingUID", 333);
        Object result = executeDirect.invoke(tokenTask, (Object)new String[] {
            urlRequest
        });

        executePostResult.invoke(tokenTask, result);

        // Verification from returned intent data
        Intent data = assertFinishCalledWithResult(AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE);
        verify(mockAct, times(3)).setUserData(any(Account.class), anyString(), anyString());
    }

    private MockWebRequestHandler setMockWebResponse() throws NoSuchFieldException,
            IllegalAccessException {
        MockWebRequestHandler webrequest = new MockWebRequestHandler();
        String idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";
        String json = "{\"id_token\":"
                + idToken
                + ",\"access_token\":\"TokentestBroker\",\"token_type\":\"Bearer\",\"expires_in\":\"28799\",\"expires_on\":\"1368768616\",\"refresh_token\":\"refresh112\",\"scope\":\"*\"}";
        webrequest.setReturnResponse(new HttpWebResponse(200, json.getBytes(Charset
                .defaultCharset()), null));
        ReflectionUtils.setFieldValue(activity, "mWebRequestHandler", webrequest);
        return webrequest;
    }

    @SmallTest
    @UiThreadTest
    public void testOnPauseSetsRestartWebview() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException {

        startActivity(intentToStartActivity, null, null);
        activity = getActivity();

        Method onPause = ReflectionUtils.getTestMethod(activity, "onPause");

        onPause.invoke(activity);

        // get field value to check
        boolean restartWebView = (Boolean)ReflectionUtils
                .getFieldValue(activity, "mRestartWebview");
        assertTrue("Restart flag is set", restartWebView);
    }

    @SmallTest
    @UiThreadTest
    public void testOnResumeRestartWebview() throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, NoSuchFieldException,
            InterruptedException {
        startActivity(intentToStartActivity, null, null);
        activity = getActivity();
        final TestLogResponse logResponse = new TestLogResponse();
        logResponse.listenForLogMessage(
                "Webview onResume register broadcast receiver for requestId" + TEST_REQUEST_ID,
                null);
        ReflectionUtils.setFieldValue(activity, "mRestartWebview", true);
        Method methodOnResume = ReflectionUtils.getTestMethod(activity, "onResume");
        methodOnResume.invoke(activity);

        // get field value to check
        assertTrue("verify log message",
                logResponse.message.startsWith("Webview onResume register broadcast"));
    }

    @SmallTest
    @UiThreadTest
    public void testOnRestart() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, InterruptedException {

        startActivity(intentToStartActivity, null, null);
        activity = getActivity();

        ReflectionUtils.setFieldValue(activity, "mRestartWebview", false);
        Method methodOnResume = ReflectionUtils.getTestMethod(activity, "onRestart");

        methodOnResume.invoke(activity);

        // get field value to check
        boolean fieldVal = (Boolean)ReflectionUtils.getFieldValue(activity, "mRestartWebview");
        assertTrue("RestartWebview set to true", fieldVal);
    }

    @SmallTest
    @UiThreadTest
    public void testEmptyIntentData() throws IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException {

        intentToStartActivity.putExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE, "");
        startActivity(intentToStartActivity, null, null);
        activity = getActivity();

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

        startActivity(intentToStartActivity, null, null);
        activity = getActivity();
        String broadcastCancelMsg1 = "ActivityBroadcastReceiver onReceive action is for cancelling Authentication Activity";
        String broadcastCancelMsg2 = "Waiting requestId is same and cancelling this activity";

        // Test onReceive call with wrong request id
        TestLogResponse response = new TestLogResponse();
        final CountDownLatch signal = new CountDownLatch(1);
        response.listenForLogMessage(broadcastCancelMsg1, signal);
        BroadcastReceiver receiver = (BroadcastReceiver)ReflectionUtils.getFieldValue(activity,
                "mReceiver");
        final Intent intent = new Intent(AuthenticationConstants.Browser.ACTION_CANCEL);
        final Bundle extras = new Bundle();
        intent.putExtras(extras);
        intent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, TEST_REQUEST_ID + 43);

        receiver.onReceive(getInstrumentation().getTargetContext(), intent);

        // Test onReceive call with correct request id
        signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
        assertTrue("log the message for correct Intent",
                response.message.equals(broadcastCancelMsg1));

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
                response2.message.equals(broadcastCancelMsg2));
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
        int actualResultCode = (Integer)f.get(getActivity());
        assertEquals(actualResultCode, resultCode);

        f = Activity.class.getDeclaredField("mResultData");
        f.setAccessible(true);
        return (Intent)f.get(getActivity());
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
