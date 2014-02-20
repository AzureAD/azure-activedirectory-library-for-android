
package com.microsoft.adal.test;

import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.test.ActivityUnitTestCase;
import android.test.RenamingDelegatingContext;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.webkit.WebView;
import android.widget.Button;

import com.microsoft.adal.AuthenticationActivity;
import com.microsoft.adal.R;

/**
 * Unit test to verify buttons, webview and other items.
 * 
 * @author omercan
 */
public class AuthenticationActivityUnitTests extends ActivityUnitTestCase<AuthenticationActivity> {

    private static final int TEST_REQUEST_ID = 123;

    private static final long CONTEXT_REQUEST_TIME_OUT = 20000;

    private int buttonId;

    private Intent intentToStartActivity;

    private AuthenticationActivity activity;

    public AuthenticationActivityUnitTests() {
        super(AuthenticationActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Context mockContext = new ActivityMockContext(getInstrumentation().getTargetContext());

        setActivityContext(mockContext);
        intentToStartActivity = new Intent(getInstrumentation().getTargetContext(),
                AuthenticationActivity.class);

        Object authorizationRequest = getTestRequest();
        intentToStartActivity.putExtra(AuthenticationConstants.Browser.REQUEST_MESSAGE,
                (Serializable)authorizationRequest);

    }

    private Object getTestRequest() throws ClassNotFoundException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {

        Class<?> c = Class.forName("com.microsoft.adal.AuthenticationRequest");

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
        buttonId = com.microsoft.adal.R.id.btnCancel;
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

        Method returnToCaller = ReflectionUtils.getTestMethod(activity, "ReturnToCaller",
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
    public void testOnBackPressed() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, InterruptedException {

        // Case1: returns false
        startActivity(intentToStartActivity, null, null);
        activity = getActivity();
        WebView mockWebView = mock(WebView.class);
        ReflectionUtils.setFieldValue(activity, "mWebView", mockWebView);
        when(mockWebView.canGoBack()).thenReturn(false);
         
        // call
        activity.onBackPressed();
        
        //verify that correct method called
        verify(mockWebView).canGoBack();
        verify(mockWebView, never()).goBack();  
        
        // Case 2: returns true
        when(mockWebView.canGoBack()).thenReturn(true);
        
        // call
        activity.onBackPressed();
        
        //verify that correct method called
        verify(mockWebView).canGoBack();
        verify(mockWebView, times(1)).goBack();  
    }
    
    @SmallTest
    @UiThreadTest
    public void testOnBackPressed_BackTrue() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException, InterruptedException {

        AuthenticationActivity mockActivity = mock(AuthenticationActivity.class);
        WebView mockWebView = mock(WebView.class);
        when(mockWebView.canGoBack()).thenReturn(false);
         
        // call
        mockActivity.onBackPressed();
        
        //verify that correct method called
        verify(mockWebView).canGoBack();
        verify(mockWebView, never()).goBack();       
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
