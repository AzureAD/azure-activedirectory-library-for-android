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

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.ApplicationReceiver;
import com.microsoft.aad.adal.AuthenticationActivity;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.HttpWebResponse;
import com.microsoft.aad.adal.R;
import com.microsoft.aad.adal.UserIdentifier;
import com.microsoft.aad.adal.UserIdentifier.UserIdentifierType;

/**
 * Unit test to verify buttons, webview and other items.
 */
public class AuthenticationActivityUnitTest extends ActivityUnitTestCase<AuthenticationActivity> {

    private static final int TEST_REQUEST_ID = 123;

    private static final long CONTEXT_REQUEST_TIME_OUT = 20000;

    private static final long DEVICE_RESPONSE_WAIT = 500;

    private Intent intentToStartActivity;

    private AuthenticationActivity activity;

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

        Object o = AuthenticationContextTest.createAuthenticationRequest("authority", new String[] {
            "scope"
        }, "clientid", "redirect", new UserIdentifier("user1",
                UserIdentifierType.RequiredDisplayableId));
        ReflectionUtils.setFieldValue(o, "mRequestId", TEST_REQUEST_ID);

        return o;
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
        ReflectionUtils.setFieldValue(activity, "mSpinner", null);

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
        ReflectionUtils.setFieldValue(activity, "mRegisterReceiver", true);
        Method methodOnResume = ReflectionUtils.getTestMethod(activity, "onResume");
        methodOnResume.invoke(activity);

        // get field value to check
        assertTrue("verify log message",
                logResponse.message.contains("Webview onResume register broadcast"));
    }

    @SmallTest
    @UiThreadTest
    public void testOnBackPressed() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, InterruptedException {
        startActivity(intentToStartActivity, null, null);
        activity = getActivity();
        
        activity.onBackPressed();

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

        startActivity(intentToStartActivity, null, null);
        activity = getActivity();

        ReflectionUtils.setFieldValue(activity, "mRegisterReceiver", false);
        Method methodOnResume = ReflectionUtils.getTestMethod(activity, "onRestart");

        methodOnResume.invoke(activity);

        // get field value to check
        boolean fieldVal = (Boolean)ReflectionUtils.getFieldValue(activity, "mRegisterReceiver");
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
                response.message.contains(broadcastCancelMsg1));

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
                response2.message.contains(broadcastCancelMsg2));
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
