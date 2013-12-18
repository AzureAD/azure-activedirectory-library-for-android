
package com.microsoft.adal.testapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationException;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.CacheKey;
import com.microsoft.adal.ITokenCacheStore;
import com.microsoft.adal.Logger;
import com.microsoft.adal.Logger.ILogger;
import com.microsoft.adal.PromptBehavior;
import com.microsoft.adal.TokenCacheItem;

public class MainActivity extends Activity {

    Button btnGetToken, btnResetToken, btnShowUsers, btnSetExpired, btnCancel;

    TextView textViewStatus;

    EditText mAuthority, mResource, mClientId, mUserid, mPrompt, mRedirect;

    CheckBox mValidate;

    private final static String TAG = "MainActivity";

    final static String AUTHORITY_URL = "https://login.windows.net/omercantest.onmicrosoft.com";

    final static String CLIENT_ID = "650a6609-5463-4bc4-b7c6-19df7990a8bc";

    final static String RESOURCE_ID = "https://omercantest.onmicrosoft.com/AllHandsTry";

    final static String AUTHORIZATION_HEADER = "Authorization";

    final static String AUTHORIZATION_HEADER_BEARER = "Bearer ";

    final static String REDIRECT_URL = "http://taskapp";

    // Endpoint we are targeting for the deployed WebAPI service
    final static String SERVICE_URL = "https://android.azurewebsites.net/api/values";

    private AuthenticationContext mContext = null;

    public void setLoggerCallback(ILogger loggerCallback) {
        // callback hook to insert from test instrumentation to check loaded url
        // on webview
        // Test project adds callback to track the completion
        // loggerCallback
        if (loggerCallback != null) {
            Logger.getInstance().setExternalLogger(loggerCallback);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the current window
        Window window = getWindow();
        // Add the Flag from the window manager class
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        textViewStatus = (TextView)findViewById(R.id.textViewStatus);
        btnGetToken = (Button)findViewById(R.id.buttonGetToken);
        btnResetToken = (Button)findViewById(R.id.buttonReset);
        btnShowUsers = (Button)findViewById(R.id.buttonUsers);
        btnSetExpired = (Button)findViewById(R.id.buttonExpired);
        btnCancel = (Button)findViewById(R.id.buttonCancel);
        mAuthority = (EditText)findViewById(R.id.editAuthority);
        mResource = (EditText)findViewById(R.id.editResource);
        mClientId = (EditText)findViewById(R.id.editClientid);
        mUserid = (EditText)findViewById(R.id.editUserId);
        mPrompt = (EditText)findViewById(R.id.editPrompt);
        mRedirect = (EditText)findViewById(R.id.editRedirect);
        mValidate = (CheckBox)findViewById(R.id.checkBoxValidate);

        btnGetToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getToken();
            }
        });

        btnResetToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToken();
            }
        });

        btnShowUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUsers();
            }
        });

        btnSetExpired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTokenExpired();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                cancelAuthentication();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void cancelAuthentication() {
        Log.d(TAG, "Cancel authentication Activity");
    }

    private void initContext() {
        Log.d(TAG, "Init authentication context based on input");

        // UI automation tests will fill in these fields to test different
        // scenarios
        String authority = mAuthority.getText().toString();
        if (authority == null || authority.isEmpty()) {
            authority = AUTHORITY_URL;
        }

        mContext = new AuthenticationContext(MainActivity.this, authority, mValidate.isChecked());
    }

    private void getToken() {
        Log.d(TAG, "get Token");
        textViewStatus.setText("Getting token...");
        if (mContext == null) {
            initContext();
        }

        String resource = mResource.getText().toString();
        if (resource == null || resource.isEmpty()) {
            resource = RESOURCE_ID;
        }

        String clientId = mClientId.getText().toString();
        if (clientId == null || clientId.isEmpty()) {
            clientId = CLIENT_ID;
        }

        // Optional field, so acquireToken accepts null fields
        String userid = mUserid.getText().toString();

        String promptInput = mPrompt.getText().toString();
        if (promptInput == null || promptInput.isEmpty()) {
            promptInput = PromptBehavior.Auto.name();
        }

        PromptBehavior prompt = PromptBehavior.valueOf(promptInput);

        // Optional field, so acquireToken accepts null fields
        String redirect = mRedirect.getText().toString();
        mContext.acquireToken(MainActivity.this, resource, clientId, redirect, userid, prompt, "",
                new AuthenticationCallback<AuthenticationResult>() {

                    @Override
                    public void onError(Exception exc) {
                        if (exc instanceof AuthenticationException) {
                            Toast.makeText(MainActivity.this, "Authentication is cancelled",
                                    Toast.LENGTH_LONG);
                            textViewStatus.setText("Cancelled");
                            Log.d(TAG, "Cancelled");
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Authentication error:" + exc.getMessage(), Toast.LENGTH_LONG);
                            textViewStatus.setText("Authentication error:" + exc.getMessage());
                            Log.d(TAG, "Authentication error:" + exc.getMessage());
                        }
                    }

                    @Override
                    public void onSuccess(AuthenticationResult result) {
                        if (result == null || result.getAccessToken() == null
                                || result.getAccessToken().isEmpty()) {
                            Toast.makeText(MainActivity.this, "Authentication result is null",
                                    Toast.LENGTH_LONG);
                            textViewStatus.setText("Token is empty");
                            Log.d(TAG, "Token is empty");
                        } else {
                            // request is successful
                            textViewStatus.setText("Status:" + result.getStatus() + " Expired:"
                                    + result.getExpiresOn().toString());
                            Log.d(TAG, "Status:" + result.getStatus() + " Expired:"
                                    + result.getExpiresOn().toString());
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mContext != null) {
            mContext.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * reset all
     */
    private void resetToken() {
        Log.d(TAG, "reset Token");
        if (mContext == null) {
            initContext();
        }

        mContext.getCache().removeAll();
        textViewStatus.setText("");

        // Clear browser cookies
        CookieSyncManager.createInstance(MainActivity.this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        CookieSyncManager.getInstance().sync();
    }

    private void showUsers() {
        Log.d(TAG, "show Users");
        // TODO
    }

    /**
     * set all expired
     */
    private void setTokenExpired() {
        Log.d(TAG, "Setting item to expire...");

        ITokenCacheStore cache = mContext.getCache();
        TokenCacheItem item = cache.getItem(CacheKey.createCacheKey(AUTHORITY_URL, RESOURCE_ID,
                CLIENT_ID, false, ""));
        if (item != null) {
            Calendar calendar = new GregorianCalendar();
            calendar.add(Calendar.MINUTE, -30);
            item.setExpiresOn(calendar.getTime());
            cache.setItem(CacheKey.createCacheKey(item), item);
            Log.d(TAG, "Item is set to expire");
        } else {
            Log.d(TAG, "item is null: setTokenExpired");
        }
    }

    /**
     * send token in the header
     * 
     * @param result
     */
    private void useToken(AuthenticationResult result) {
        new RequestTask(SERVICE_URL, result.getAccessToken()).execute();
    }

    /**
     * Simple get request for test
     * 
     * @author omercan
     */
    class RequestTask extends AsyncTask<Void, String, String> {

        private String mUrl;

        private String mToken;

        public RequestTask(String url, String token) {
            mUrl = url;
            mToken = token;
        }

        @Override
        protected String doInBackground(Void... empty) {

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse httpResponse;
            String responseString = "";

            try {
                HttpGet getRequest = new HttpGet(mUrl);
                getRequest.addHeader(AUTHORIZATION_HEADER, AUTHORIZATION_HEADER_BEARER + mToken);
                getRequest.addHeader("Accept", "application/json");

                httpResponse = httpclient.execute(getRequest);
                StatusLine statusLine = httpResponse.getStatusLine();

                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    // negative for unknown
                    if (httpResponse.getEntity().getContentLength() != 0) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        httpResponse.getEntity().writeTo(out);
                        out.close();
                        responseString = out.toString();
                    }
                } else {
                    // Closes the connection.
                    httpResponse.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                responseString = e.getMessage();
            } catch (IOException e) {
                responseString = e.getMessage();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            textViewStatus.setText("");
            if (result != null && !result.isEmpty()) {
                textViewStatus.setText(result);
            }
        }
    }

    class Response {

    }

}
