package com.microsoft.adal.test;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.SmallTest;
import android.webkit.WebView;
import android.widget.Button;

import com.microsoft.adal.AuthenticationConstants;
import com.microsoft.adal.AuthenticationRequest;
import com.microsoft.adal.LoginActivity;

public class LoginActivityUnitTests extends ActivityUnitTestCase<LoginActivity> {

	private int buttonId;
	private LoginActivity activity;

	public LoginActivityUnitTests() {
		super(LoginActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		 
		Context mockContext = new LoginMockContext(getInstrumentation().getTargetContext());

	    setActivityContext(mockContext);
		Intent intent = new Intent(getInstrumentation().getTargetContext(),
				LoginActivity.class);
		
		AuthenticationRequest request = new AuthenticationRequest("authority",
				"client", "resource", "scope", "redirect", "loginhint");

		intent.putExtra(AuthenticationConstants.BROWSER_REQUEST_MESSAGE, request);

		startActivity(intent, null, null);
		activity = getActivity();
	}

	@SmallTest
	public void testLayout() {

		// Cancel button
		buttonId = com.microsoft.adal.R.id.btnCancel;
		assertNotNull(activity.findViewById(buttonId));
		Button view = (Button) activity.findViewById(buttonId);
		String text = activity.getResources().getString(R.string.button_cancel);
		assertEquals("Incorrect label of the button", text, view.getText());

		// Webview
		WebView webview = (WebView) activity.findViewById(R.id.webView1);
		assertNotNull(webview);

		// Javascript enabled
		assertTrue(webview.getSettings().getJavaScriptEnabled());
	}

	@Override
	protected void tearDown() throws Exception {

		super.tearDown();
	}
	
	/**
	 * his is a class which delegates to the given context, but performs database and file operations with a renamed database/file name (prefixes default names with a given prefix).
	 *
	 */
	class LoginMockContext extends RenamingDelegatingContext
	{

		private static final String TAG = "LoginMockContext";    
	    private static final String MOCK_FILE_PREFIX = "test.";

	    /**
	     * @param context
	     * @param filePrefix
	     */
	    public LoginMockContext(Context context) {
	        super(context, MOCK_FILE_PREFIX);
	        makeExistingFilesAndDbsAccessible();
	    }

		
	}
}
