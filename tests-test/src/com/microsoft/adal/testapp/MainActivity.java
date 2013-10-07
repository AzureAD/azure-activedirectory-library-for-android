package com.microsoft.adal.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationOptions;
import com.microsoft.adal.AuthenticationRequest;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.test.Constants;
import com.microsoft.adal.test.R;

/**
 * Instrumentation activity that will use ADAL to make actual calls. Activity
 * needs to launch from this. Test calls will go through this activity
 * 
 * @author omercan
 * 
 */
public class MainActivity extends Activity {

	public AuthenticationContext mAuthContext;

	TextView textViewStatus;
	String editAuthority, editRedirect, editClientId, editLoginHint,
			editResource;

	/**
	 * Params to pass back result after calling ADAL
	 */
	public Exception exception;
	public AuthenticationResult result;
	public boolean cancelled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		textViewStatus = (TextView) findViewById(R.id.textViewStatus);

	}

	public void createRequest(AuthenticationRequest request) {
		editAuthority = request.getAuthority();
		editRedirect = request.getRedirectUri();
		editClientId = request.getClientId();
		editLoginHint = request.getLoginHint();
		editResource = request.getResource();
		cancelled = false;
		exception = null;
		result = null;

		mAuthContext = new AuthenticationContext(MainActivity.this,
				editAuthority);
	}

	public void getToken(AuthenticationOptions options) {
		mAuthContext.acquireToken(MainActivity.this, editClientId,
				editResource, editRedirect, editLoginHint, options,
				new AuthenticationCallback() {

					@Override
					public void onError(Exception exc) {
						exception = exc;
					}

					@Override
					public void onCompleted(AuthenticationResult authResult) {
						result = authResult;
					}

					@Override
					public void onCancelled() {
						cancelled = true;
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
