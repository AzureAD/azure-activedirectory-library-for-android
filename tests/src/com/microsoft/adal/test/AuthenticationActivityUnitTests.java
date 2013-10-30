
package com.microsoft.adal.test;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.SmallTest;
import android.webkit.WebView;
import android.widget.Button;

import com.microsoft.adal.AuthenticationActivity;
import com.microsoft.adal.AuthenticationConstants;

/**
 * Unit test to verify buttons, webview and other items.
 * E2E tests will check if this activity really working or not.
 * @author omercan
 *
 */
public class AuthenticationActivityUnitTests extends ActivityUnitTestCase<AuthenticationActivity> {

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
            InvocationTargetException {

        Class<?> c = Class.forName("com.microsoft.adal.AuthenticationRequest");

        // getConstructor() returns only public constructors,

        Constructor<?> constructor = c.getDeclaredConstructor(String.class, String.class,
                String.class, String.class, String.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance("authority", "client", "resource", "redirect",
                "loginhint");

        return o;
    }

    @SmallTest
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

        private static final String TAG = "ActivityMockContext";

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
