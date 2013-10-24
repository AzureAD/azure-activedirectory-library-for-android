package com.example.com.microsoft.adal.hello;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.azure.webapi.MobileServiceClient;
import com.azure.webapi.MobileServiceConnection;
import com.azure.webapi.MobileServiceUser;
import com.azure.webapi.RequestAsyncTask;
import com.azure.webapi.ServiceFilterRequest;
import com.azure.webapi.ServiceFilterRequestImpl;
import com.azure.webapi.ServiceFilterResponse;
import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationConstants;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.PromptBehavior;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.ExceptionExtensions;
import com.microsoft.adal.HttpWebRequest;
import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.HttpWebResponse;

public class MainActivity extends Activity {

	private final static String TAG = "ADAL_DEMO";
	public static final int MENU_ITEMS = Menu.FIRST;
	public static final int MENU_GET = Menu.FIRST + 1;
	public static final int MENU_POST = Menu.FIRST + 2;
	protected AuthenticationContext mAuthContext;
	protected String mToken;

	Button btnGetToken, btnResetToken, btnGetItems, btnPostItem, btnLauncItems;
	EditText tokenText;
	EditText refreshText;
	EditText expireText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set strict mode for testing
		StrictMode.enableDefaults();

		btnGetToken = (Button) findViewById(R.id.btnGetToken);
		tokenText = (EditText) findViewById(R.id.editText1);
		refreshText = (EditText) findViewById(R.id.editText2);
		expireText = (EditText) findViewById(R.id.editText3);

		btnResetToken = (Button) findViewById(R.id.btnClear);
		btnGetItems = (Button) findViewById(R.id.btnGetItems);
		btnPostItem = (Button) findViewById(R.id.btnPostItem);

		mAuthContext = new AuthenticationContext(this, Constants.AUTHORITY_URL, false);

		btnResetToken.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				mAuthContext.getCache().removeAll();
				mToken = null;
				tokenText
						.setText("Cleared cache, browser cookies, local token value");
				refreshText.setText("");
				expireText.setText("");

				// Clear browser cookies
				CookieSyncManager.createInstance(MainActivity.this);
				CookieManager cookieManager = CookieManager.getInstance();
				CookieSyncManager.getInstance().sync();
				cookieManager.removeAllCookie();
			}
		});

		btnGetItems.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				sampleRequest();
			}
		});

		btnPostItem.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				postItem();
			}
		});

		btnGetToken.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				getToken();
			}
		});
	}

	// private <E>List<E> parseResults(JsonElement results, Class<E> clazz) {
	// Gson gson = new GsonBuilder().create();
	// return JsonEntityParser.parseResults(results, gson, clazz);
	// }

	private void getItems() {
		String endpoint = Constants.SERVICE_URL + "/api/"
				+ Constants.TABLE_WORKITEM;
		HttpWebRequest webRequest = null;
		try {
			URL targetUrl = new URL(endpoint);
			webRequest = new HttpWebRequest(targetUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setHeaders(webRequest);

		try {
			webRequest.sendAsyncGet(new HttpWebRequestCallback() {
				@Override
				public void onComplete(Exception exception,
						HttpWebResponse webResponse) {
					HashMap<String, String> response = new HashMap<String, String>();
					// Response should have Oauth2 token
					if (exception != null) {
						Log.e(TAG,
								ExceptionExtensions.getExceptionMessage(exception));

						showMessage(ExceptionExtensions
								.getExceptionMessage(exception));
					} else if (webResponse.getStatusCode() <= 400) {
						try {
							tokenText.setText(new String(webResponse.getBody()));

							// List<WorkItem> items = parseResults(jsonObject,
							// WorkItem.class);

						} catch (Exception ex) {
							// There is no recovery possible here, so
							// catch the generic Exception
							Log.e(TAG, ExceptionExtensions.getExceptionMessage(ex));

							Log.e(TAG, ExceptionExtensions
									.getExceptionMessage(exception));

							showMessage(ExceptionExtensions
									.getExceptionMessage(exception));
						}
					} else {
						response.put(
								AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
								new String(webResponse.getBody()));
					}

				}
			});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setHeaders(HttpWebRequest webRequest) {
		webRequest.getRequestHeaders().put("Accept", "application/json");
		if (getLocalToken() != null) {
			webRequest.getRequestHeaders().put("Authorization",
					"Bearer " + getLocalToken());
		}
	}

	private String getLocalToken() {
		return mToken;
	}

	private void setLocalToken(String newToken) {
		mToken = newToken;
	}

	private void postItem() {
		String title = refreshText.getText().toString();
		String endpoint = Constants.SERVICE_URL + "/api/"
				+ Constants.TABLE_WORKITEM;
		HttpWebRequest webRequest = null;
		try {
			webRequest = new HttpWebRequest(new URL(endpoint));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setHeaders(webRequest);

		String context = "{\"Title\":\"" + title
				+ "\",\"DueDate\":\"1-1-2014\"}";
		try {
			webRequest.sendAsyncPost(
					context.getBytes(AuthenticationConstants.ENCODING_UTF8),
					"application/json", new HttpWebRequestCallback() {
						@Override
						public void onComplete(Exception exception,
								HttpWebResponse webResponse) {
							// Response should have Oauth2 token
							if (exception != null) {
								Log.e(TAG, ExceptionExtensions
										.getExceptionMessage(exception));

								showMessage(ExceptionExtensions
										.getExceptionMessage(exception));
							} else if (webResponse.getStatusCode() <= 400) {
								try {
									showMessage("POST complete");
								} catch (Exception ex) {
									Log.e(TAG, ExceptionExtensions
											.getExceptionMessage(exception));

									showMessage(ExceptionExtensions
											.getExceptionMessage(exception));
								}
							} else {
								showMessage(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION
										+ " "
										+ new String(webResponse.getBody()));
							}

						}
					});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void getToken() {

		UUID correlationid = UUID.randomUUID(); // store this

		mAuthContext.acquireToken(MainActivity.this, Constants.RESOURCE_ID,Constants.CLIENT_ID, Constants.REDIRECT_URL,
				Constants.USER_HINT, PromptBehavior.Auto, null,
				new AuthenticationCallback() {

					@Override
					public void onError(Exception exc) {
						Toast.makeText(getApplicationContext(),
								exc.getMessage(), Toast.LENGTH_LONG).show();

					}

					@Override
					public void onCompleted(AuthenticationResult result) {
						Toast.makeText(getApplicationContext(), "OnCompleted",
								Toast.LENGTH_LONG).show();
						setLocalToken(result.getAccessToken());
						tokenText.setText(result.getAccessToken().substring(0,
								10));
						refreshText.setText(result.getRefreshToken().substring(
								0, 10));
						expireText.setText(result.getExpires().toString());
					}

					@Override
					public void onCancelled() {
						Toast.makeText(getApplicationContext(), "CANCELLED",
								Toast.LENGTH_LONG).show();
					}
				});
	}

	/*
	 * Try to get items if it returns error, ask for token again.
	 */
	private void sampleRequest() {
		// Create a request
		String endpoint = Constants.SERVICE_URL + "/api/"
				+ Constants.TABLE_WORKITEM;
		final ServiceFilterRequest request = new ServiceFilterRequestImpl(
				new HttpGet(endpoint));

		// Create the Mobile Service Client instance, using the provided
		// Mobile Service URL and key
		MobileServiceClient client = null;
		try {
			// TODO change constructor
			client = new MobileServiceClient(
					"https://someserviceChange.azure-mobile.net/", "fooBar",
					this);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (getLocalToken() != null) {
			MobileServiceUser user = new MobileServiceUser();
			user.setAuthenticationToken(getLocalToken());
			client.setCurrentUser(user);
		}

		final MobileServiceConnection connection = client.createConnection();

		// Create the AsyncTask that will execute the request
		new RequestAsyncTask(request, connection) {
			@Override
			protected void onPostExecute(ServiceFilterResponse response) {

				if (mTaskException == null && response != null) {
					try {
						// Get the user from the response and create a
						// MobileServiceUser object from the JSON
						String content = response.getContent();
						tokenText.setText(content);

					} catch (Exception e) {
						// Something went wrong, call onCompleted method
						// with exception
						showMessage("Error in request task:" + e.getMessage());
						return;
					}

					// Call onCompleted method
					showMessage("Request is finished");
				} else {
					// Something went wrong, call onCompleted method with
					// exception
					showMessage(mTaskException.getMessage());

				}

			}
		}.execute();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "code:" + requestCode);
		super.onActivityResult(requestCode, resultCode, data);
		mAuthContext.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(Menu.NONE, MENU_ITEMS, Menu.NONE, "Items");
		menu.add(Menu.NONE, MENU_GET, Menu.NONE, "Get");
		menu.add(Menu.NONE, MENU_POST, Menu.NONE, "Post");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEMS: {
			Intent intent = new Intent(MainActivity.this, ToDoActivity.class);
			startActivity(intent);
			return true;
		}

		case MENU_GET:
			sampleRequest();
			return true;
		case MENU_POST:
			postItem();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showMessage(String msg) {
		if (!msg.isEmpty()) {
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG)
					.show();
		}
	}
}
