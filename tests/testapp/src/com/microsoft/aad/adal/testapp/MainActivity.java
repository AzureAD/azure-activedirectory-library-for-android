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

package com.microsoft.aad.adal.testapp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.CacheKey;
import com.microsoft.aad.adal.DefaultTokenCacheStore;
import com.microsoft.aad.adal.ITokenCacheStore;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.Logger.ILogger;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.TokenCacheItem;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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

public class MainActivity extends Activity {

    public static final String GETTING_TOKEN = "Getting token...";

    Button btnGetToken, btnResetToken, btnRefresh, btnSetExpired, buttonVerify,
            buttonRemoveCookies;

    TextView textViewStatus;

    EditText mAuthority, mResource, mClientId, mUserid, mPrompt, mRedirect;

    CheckBox mValidate;

    private final static String TAG = "MainActivity";

    public final static String FAILED = "TEST_FAILED";

    public final static String PASSED = "TEST_PASSED";

    public final static String TOKEN_USED = "TOKEN_USED";

    public static final String TEST_CERT_ALIAS = "My Key Chain";

    public static final String PKCS12_PASS = "changeit";

    public final String PKCS12_FILENAME = "keychain.p12";

    public final String JWS_ALGORITHM = "SHA256withRSA";

    /**
     * result from recent request
     */
    private AuthenticationResult mResult;
    
    private Exception mLastException;

    private String mActiveUser, mExtraQueryParam;

    final static String AUTHORITY_URL = "https://login.windows.net/omercantest.onmicrosoft.com";

    final static String CLIENT_ID = "650a6609-5463-4bc4-b7c6-19df7990a8bc";

    final static String RESOURCE_ID = "https://omercantest.onmicrosoft.com/AllHandsTry";

    final static String AUTHORIZATION_HEADER = "Authorization";

    final static String AUTHORIZATION_HEADER_BEARER = "Bearer ";

    final static String REDIRECT_URL = "http://taskapp";

    // Endpoint we are targeting for the deployed WebAPI service
    final static String SERVICE_URL = "https://android.azurewebsites.net/api/values";

    final static String LOG_STATUS_FORMAT = "Status:%s Expires:%s";

    private AuthenticationContext mContext = null;

    private UUID mRequestCorrelationId;

    private Handler handler = new Handler();

    public Handler getTestAppHandler() {
        return handler;
    }

    public AuthenticationContext getAuthenticationContext(){
        return mContext;
    }
    
    class AdalCallback implements AuthenticationCallback<AuthenticationResult> {

        UUID mId;

        public AdalCallback() {
            mId = UUID.randomUUID();
        }

        @Override
        public void onError(Exception exc) {
            Log.e(TAG, "Callback returned error", exc);
            mLastException = exc;
            if (exc instanceof AuthenticationException) {
                AuthenticationException authException = (AuthenticationException)exc;
                textViewStatus.setText("Authentication error:" + authException.getCode().name());
                if (authException.getCode() == ADALError.AUTH_FAILED_CANCELLED) {
                    textViewStatus.setText("Cancelled");
                    Log.d(TAG, "Cancelled");
                }
            }
            Log.d(TAG, "Authentication error:" + exc.getMessage());
        }

        @Override
        public void onSuccess(AuthenticationResult result) {
            Log.d(TAG, "Callback has result");
            setResult(result);

            if (result == null || result.getAccessToken() == null
                    || result.getAccessToken().isEmpty()) {
                textViewStatus.setText(FAILED);
                Log.d(TAG, "Token is empty");
                if (result != null) {
                    Log.d(TAG,
                            "Error  code:" + result.getErrorCode() + " Description:"
                                    + result.getErrorDescription());
                }
            } else {
                // request is successful
                Log.d(TAG, String.format(LOG_STATUS_FORMAT, result.getStatus(), result
                        .getExpiresOn().toString()));
                textViewStatus.setText(PASSED);
            }
        }
    };

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
        btnSetExpired = (Button)findViewById(R.id.buttonExpired);
        btnRefresh = (Button)findViewById(R.id.buttonRefresh);
        buttonVerify = (Button)findViewById(R.id.buttonVerify);
        buttonRemoveCookies = (Button)findViewById(R.id.buttonRemoveCookies);
        mAuthority = (EditText)findViewById(R.id.editAuthority);
        mResource = (EditText)findViewById(R.id.editResource);
        mClientId = (EditText)findViewById(R.id.editClientid);
        mUserid = (EditText)findViewById(R.id.editUserId);
        mPrompt = (EditText)findViewById(R.id.editPrompt);
        mRedirect = (EditText)findViewById(R.id.editRedirect);
        mValidate = (CheckBox)findViewById(R.id.checkBoxValidate);

        findViewById(R.id.buttonTestScriptRun).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TestScriptRunner runner = new TestScriptRunner(MainActivity.this);
                runner.runRemoteScript();
            }
        });

        buttonRemoveCookies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCookies();
            }
        });

        buttonVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useToken();
            }
        });

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

        btnSetExpired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTokenExpired();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTokenByRefreshToken();
            }
        });
    }

    public void initDeviceCertificateMock() throws NoSuchAlgorithmException,
            UnrecoverableKeyException, CertificateException, KeyStoreException, IOException {
        KeyStore keystore = loadTestCertificate();
        Key key = keystore.getKey(TEST_CERT_ALIAS, PKCS12_PASS.toCharArray());
        RSAPrivateKey privateKey = (RSAPrivateKey)key;
        Certificate cert = keystore.getCertificate(TEST_CERT_ALIAS);
        RSAPublicKey publicKey = (RSAPublicKey)cert.getPublicKey();
        MockDeviceCertProxy.sValidIssuer = true;
        MockDeviceCertProxy.sPrivateKey = privateKey;
        MockDeviceCertProxy.sPublicKey = publicKey;
        MockDeviceCertProxy.sThumbPrint = "test";
        MockDeviceCertProxy.sCertificate = (X509Certificate)cert;
        AuthenticationSettings.INSTANCE.setDeviceCertificateProxyClass(MockDeviceCertProxy.class);
    }

    private KeyStore loadTestCertificate() throws IOException, CertificateException,
            UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        KeyStore caKs = KeyStore.getInstance("PKCS12");
        BufferedInputStream stream = new BufferedInputStream(MainActivity.this.getAssets().open(
                PKCS12_FILENAME));
        caKs.load(stream, PKCS12_PASS.toCharArray());
        return caKs;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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

    public void acquireTokenByRefreshToken(String refreshToken) {
        textViewStatus.setText(GETTING_TOKEN);
        if (mContext == null) {
            initContext();
        }

        String clientId = mClientId.getText().toString();
        if (clientId == null || clientId.isEmpty()) {
            clientId = CLIENT_ID;
        }
        mContext.setRequestCorrelationId(mRequestCorrelationId);
        mContext.acquireTokenByRefreshToken(refreshToken, clientId, new AdalCallback());
    }

    private void getTokenByRefreshToken() {
        Logger.v(TAG, "get Token with refresh token");
        textViewStatus.setText(GETTING_TOKEN);
        if (mResult != null && mResult.getRefreshToken() != null
                && !mResult.getRefreshToken().isEmpty()) {
            String clientId = mClientId.getText().toString();

            if (clientId == null || clientId.isEmpty()) {
                clientId = CLIENT_ID;
            }
            mContext.setRequestCorrelationId(mRequestCorrelationId);
            mContext.acquireTokenByRefreshToken(mResult.getRefreshToken(), clientId,
                    new AdalCallback());
        } else {
            textViewStatus.setText(FAILED);
        }
    }

    private void getToken() {
        Logger.v(TAG, "get Token");
        textViewStatus.setText(GETTING_TOKEN);
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
        mResult = null;
        mContext.setRequestCorrelationId(mRequestCorrelationId);
        mContext.acquireToken(MainActivity.this, resource, clientId, redirect, userid, prompt,
                mExtraQueryParam, new AdalCallback());
    }

    public void getTokenSilent() {
        Logger.v(TAG, "get Token Silent");
        textViewStatus.setText(GETTING_TOKEN);
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

        mResult = null;
        mContext.setRequestCorrelationId(mRequestCorrelationId);
        mContext.acquireTokenSilent(resource, clientId, userid, new AdalCallback());
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mContext != null) {
            mContext.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void setContextForScriptRun(final AuthenticationContext context) {
        this.mContext = context;
    }

    private void removeCookies() {
        // Clear browser cookies
        CookieSyncManager.createInstance(MainActivity.this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        CookieSyncManager.getInstance().sync();
    }

    /**
     * only remove from cache but keep cookies
     */
    public void removeTokens() {
        if (mContext == null) {
            initContext();
        }

        mContext.getCache().removeAll();
        textViewStatus.setText("");
    }

    /**
     * reset all
     */
    private void resetToken() {
        Log.d(TAG, "reset Token");
        removeTokens();
        removeCookies();
    }

    /**
     * set all expired
     */
    private void setTokenExpired() {
        Log.d(TAG, "Setting item to expire...");

        Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.MINUTE, -30);
        Date date = calendar.getTime();
        ITokenCacheStore cache = mContext.getCache();
        String key = CacheKey.createCacheKeyForRTEntry(mAuthority.getText().toString(), 
                mResource.getText().toString(), mClientId.getText().toString(), mUserid.getText().toString());
        TokenCacheItem item = cache.getItem(key);
        setTime(cache, date, key, item);

        key = CacheKey.createCacheKeyForMRRT(mAuthority.getText().toString(), mClientId.getText().toString(),  
                mUserid.getText().toString());
        item = cache.getItem(key);
        setTime(cache, date, key, item);
    }

    private void setTime(ITokenCacheStore cache, Date date, String key, TokenCacheItem item) {
        if (item != null) {
            Calendar calendar = new GregorianCalendar();
            calendar.add(Calendar.MINUTE, -30);
            item.setExpiresOn(calendar.getTime());

            cache.setItem(key, item);
            Log.d(TAG, "Item is set to expire for key:" + key);
        } else {
            Log.d(TAG, "item is null: setTokenExpired");
        }
    }

    /**
     * set all expired
     */
    public ArrayList<TokenCacheItem> getTokens() {
        Log.d(TAG, "Setting item to expire...");
        ArrayList<TokenCacheItem> items = new ArrayList<TokenCacheItem>();
        DefaultTokenCacheStore cache = (DefaultTokenCacheStore)mContext.getCache();
        final Iterator<TokenCacheItem> allItems = cache.getAll();
        while (allItems.hasNext()) {
            TokenCacheItem tokenCacheItem = allItems.next();
            if (tokenCacheItem != null) {
                items.add(tokenCacheItem);
            }
        }

        return items;
    }

    /**
     * send token in the header with async task
     * 
     * @param result
     */
    private void useToken() {
        if (mResult != null && mResult.getRefreshToken() != null
                && !mResult.getRefreshToken().isEmpty()) {
            textViewStatus.setText("");
            new RequestTask(SERVICE_URL, mResult.getAccessToken()).execute();
        } else {
            textViewStatus.setText(FAILED);
        }
    }

    public AuthenticationResult getResult() {
        return mResult;
    }

    public String getActiveUser() {
        return mActiveUser;
    }

    public void setResult(AuthenticationResult result) {
        this.mResult = result;
        if (result != null && result.getUserInfo() != null) {
            Log.v(TAG, "Active UserId:" + mActiveUser);
            mActiveUser = result.getUserInfo().getUserId();
        }
    }

    public UUID getRequestCorrelationId() {
        return mRequestCorrelationId;
    }

    public void setRequestCorrelationId(UUID mRequestCorrelationId) {
        this.mRequestCorrelationId = mRequestCorrelationId;
    }

    public void setExtraQueryParam(String extraQueryParam) {
        mExtraQueryParam = extraQueryParam;
    }

    public Exception getLastException() {
        return mLastException;
    }

    /**
     * Simple get request for test
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
                textViewStatus.setText(TOKEN_USED);
            }
        }
    }

}
