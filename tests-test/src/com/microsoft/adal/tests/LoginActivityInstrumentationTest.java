package com.microsoft.adal.tests;

import com.microsoft.adal.AuthenticationConstants;
import com.microsoft.adal.AuthenticationOptions;
import com.microsoft.adal.AuthenticationRequest;
import com.microsoft.adal.LoginActivity;
import com.microsoft.adal.AuthenticationOptions.PromptBehavior;
import com.microsoft.adal.testapp.MainActivity;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.webkit.WebView;
import android.widget.Button;

/**
 * This test is testing library indirectly
 *
 */
public class LoginActivityInstrumentationTest extends ActivityUnitTestCase<MainActivity> {

	 
	private MainActivity activity;

	public LoginActivityInstrumentationTest() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Intent intent = new Intent(getInstrumentation().getTargetContext(),
				MainActivity.class);		 
		startActivity(intent, null, null);
		activity = getActivity();
		
	}

	@SmallTest
	public void testTokenNotDisplayLoginWithOption() {

		// Prepare
		AuthenticationOptions options = new AuthenticationOptions();
		options.setPromptBehaviour(PromptBehavior.Auto);
		options.setShowLoginScreen(false);
	    getActivity().createRequest(new AuthenticationRequest(Constants.AUTHORITY_URL,
	    		Constants.CLIENT_ID, 
	    		Constants.RESOURCE_ID,
	    		"scope", 
	    		Constants.REDIRECT_URL, 
	    		Constants.USER_HINT
	    		));
	    
	    // Act
	    getActivity().getToken(options);

		// Verify
	    // Since options specify not to display login, it should not start loginActivity
	    Intent triggeredIntent = getStartedActivityIntent();
	    assertNull("Intent was not null", triggeredIntent);   
	    assertTrue("Result is not null", getActivity().result == null);
	    assertFalse("Not cancelled", getActivity().cancelled);
	}

	@Override
	protected void tearDown() throws Exception {

		super.tearDown();
	}
}
