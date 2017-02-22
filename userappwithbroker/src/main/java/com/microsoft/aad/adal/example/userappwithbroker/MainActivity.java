// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal.example.userappwithbroker;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.IDispatcher;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.Telemetry;
import com.microsoft.aad.adal.UserInfo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Sample for acquiring token via broker. 
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    // AAD PARAMETERS
    /**
     * https://login.windows.net/tenantInfo
     */
    private static final String AUTHORITY_URL = "https://login.microsoftonline.com/yourtenantinfo";

    /**
     * Client id is given from AAD page when you register your native app. 
     */
    private static final String CLIENT_ID = "your-clientid";

    /**
     * To use broker, Developer needs to register special redirectUri in Azure Portal for broker usage. RedirectUri is 
     * in the format of msauth://packagename/Base64UrlencodedSignature.
     */
    private static final String REDIRECT_URL = "msauth://packagename/Base64EncodedSignature";

    /**
     * URI for the resource. You need to setup this resource at AAD. 
     * @note: With the new broker with PRT support, even resource or user don't have policy on to enforce
     * conditional access, when you have broker app installed, you'll still be able to talk to broker. And
     * broker will support multiple users, one WPJ account and multiple aad users. 
     */
    private static final String RESOURCE_ID = "your-resource-with-CA-policy";
    
    private static final String RESOURCE_ID2 = "your-resource";
    
    private static final String SHARED_PREFERENCE_STORE_USER_UNIQUEID = "user.app.withbroker.uniqueidstorage";

    private AuthenticationContext mAuthContext;

    private EditText mEditText;
    
    private SharedPreferences mSharedPreference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // see the java doc for the detailed requirements for 
        // talking to broker. 
        setUpADALForCallingBroker();

        Button buttonAcquireTokenInteractiveForR1 = (Button)findViewById(R.id.AcquireTokenInteractiveForR1);
        buttonAcquireTokenInteractiveForR1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                callAcquireTokenWithResource(RESOURCE_ID, PromptBehavior.Auto, getUserLoginHint());
            }
        });
        
        Button buttonAcquireTokenSilentForR1 = (Button)findViewById(R.id.AcquireTokenSilentForR1);
        buttonAcquireTokenSilentForR1.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                final String userId = getUserIdBasedOnUPN(getUserLoginHint());
                if (userId != null) {
                    callAcquireTokenSilent(RESOURCE_ID, userId);
                } else {
                    showMessage("Cannot make acquire token silent call for "
                            + "resource 1 since no user unique id is passed.");
                }
            }
        });

        Button buttonAcquireTokenInteractiveForR2 = (Button)findViewById(R.id.AcquireTokenInteractiveForR2);
        buttonAcquireTokenInteractiveForR2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                callAcquireTokenWithResource(RESOURCE_ID2, PromptBehavior.Auto, getUserLoginHint());
            }
        });
        
        Button buttonAcquireTokenSilentForR2 = (Button)findViewById(R.id.AcquireTokenSilentForR2);
        buttonAcquireTokenSilentForR2.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                final String userId = getUserIdBasedOnUPN(getUserLoginHint());
                if (userId != null) {
                    callAcquireTokenSilent(RESOURCE_ID2, userId);
                } else {
                    showMessage("Cannot make acquire token silent call for "
                            + "resource 2 since no user unique id is passed.");
                }
            }
        });

        Button buttonForGetBrokerUsers = (Button) findViewById(R.id.GetBrokerUsers);
        buttonForGetBrokerUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final UserInfo[] users = mAuthContext.getBrokerUsers();
                            String displayMessage = "Broker users list length: " + users.length + "; ";
                            for (final UserInfo userInfo : users) {
                                displayMessage += "account name: " + userInfo.getDisplayableId() + ";";
                            }

                            showMessage(displayMessage);
                        } catch (final IOException | OperationCanceledException | AuthenticatorException e) {
                            Logger.v(TAG, "Error occurred when retrieving broker users.");
                        }
                    }
                }).start();
            }
        });

        mEditText = (EditText)findViewById(R.id.editTextUsername);
        mEditText.setText("");
    }

    /**
     * To call broker, you have to ensure the following:
     * 1) You have to call {@link AuthenticationSettings#INSTANCE#setUseBroker(boolean)}  
     *    and the supplied value has to be true
     * 2) You have to have to correct set of permissions. 
     *    If target API version is lower than 23:
     *    i) You have to have GET_ACCOUNTS, USE_CREDENTIAL, MANAGE_ACCOUNTS declared
     *       in manifest. 
     *    If target API version is 23:
     *    i)  USE_CREDEINTIAL and MANAGE_ACCOUNTS is already deprecated. 
     *    ii) GET_ACCOUNTS permission is now at protection level "dangerous" calling app
     *        is responsible for requesting it. 
     * 3) If you're talking to the broker app without PRT support, you have to have an 
     *    WPJ account existed in broker(enroll with intune, or register with Azure
     *    Authentication app). 
     * 4) The two broker apps(Company Portal or Azure Authenticator) cannot go through 
     *    broker auth.
     */
    private void setUpADALForCallingBroker() {
        // Set the calling app will talk to broker
        // Note: Starting from version 1.1.14, calling app has to explicitly call
        // AuthenticationSettings.Instance.setUserBroker(true) to call broker. 
        // AuthenticationSettings.Instance.setSkipBroker(boolean) is already deprecated. 
        AuthenticationSettings.INSTANCE.setUseBroker(true);
        
        // Provide secret key for token encryption.
        try {
            // For API version lower than 18, you have to provide the secret key. The secret key 
            // needs to be 256 bits. You can use the following way to generate the secret key. And 
            // use AuthenticationSettings.Instance.setSecretKey(secretKeyBytes) to supply us the key. 
            // For API version 18 and above, we use android keystore to generate keypair, and persist
            // the keypair in AnroidKeyStore. Current investigation shows 1)Keystore may be locked with
            // a lock screen, if calling app has a lot of background activity, keystore cannot be 
            // accessed when locked, we'll be unable to decrypt the cache items 2) AndroidKeystore could
            // be reset when gesture to unlock the device is changed.
            // We do recommend the calling app the supply us the key with the above two limitations. 
            if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
                // use same key for tests
                SecretKeyFactory keyFactory = SecretKeyFactory
                        .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
                SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                        "abcdedfdfd".getBytes("UTF-8"), 100, 256));
                SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
                AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnsupportedEncodingException ex) {
            showMessage("Fail to generate secret key:" + ex.getMessage());
        } 

        ApplicationInfo appInfo = getApplicationContext().getApplicationInfo();
        Log.v(TAG, "App info:" + appInfo.uid + " package:" + appInfo.packageName);

        // If you're directly talking to ADFS server, you should set validateAuthority=false. 
        mAuthContext = new AuthenticationContext(getApplicationContext(), AUTHORITY_URL, true);
        SampleTelemetry telemetryDispatcher = new SampleTelemetry();
        Telemetry.getInstance().registerDispatcher(telemetryDispatcher, true);
    }
    
    /**
     * Call {@link AuthenticationContext#acquireToken(Activity, String, String, String, PromptBehavior, AuthenticationCallback)}. 
     */
    private void callAcquireTokenWithResource(final String resource, PromptBehavior prompt, final String loginHint) {
        mAuthContext.acquireToken(MainActivity.this, resource, CLIENT_ID, REDIRECT_URL, loginHint,
                prompt, null, new AuthenticationCallback<AuthenticationResult>() {

                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        showMessage("Respnse from broker: " + authenticationResult.getAccessToken());

                        // Update this user for next call
                        if (authenticationResult.getUserInfo() != null) {
                            saveUserIdFromAuthenticationResult(authenticationResult);
                        }
                    }

                    @Override
                    public void onError(Exception exc) {
                        showMessage("MainActivity userapp:" + exc.getMessage());
                    }
                });
    }
    
    /**
     * Retrieves user unique id from {@link AuthenticationResult}, and save it into shared preference
     * for later use.  
     * To make the sample app easier, the saved data will be keyed by displayable id. 
     */
    private void saveUserIdFromAuthenticationResult(final AuthenticationResult authResult) {
        mSharedPreference = getSharedPreferences(SHARED_PREFERENCE_STORE_USER_UNIQUEID, MODE_PRIVATE);
        
        final Editor prefEditor = mSharedPreference.edit();
        prefEditor.putString(authResult.getUserInfo().getDisplayableId(), authResult.getUserInfo().getUserId());
        
        prefEditor.apply();
    }
    
    /**
     * For the sake of simplicily of the sample app, used id stored in the shared preference is keyed
     * by displayable id. 
     */
    private String getUserIdBasedOnUPN(final String upn) {
        mSharedPreference = getSharedPreferences(SHARED_PREFERENCE_STORE_USER_UNIQUEID, MODE_PRIVATE);
        
        return mSharedPreference.getString(upn, null);
    }
    
    /**
     * Silent acquire token call. Requires to pass the user unique id. If user unique id is not passed, 
     * silent call to broker will be skipped. 
     */
    private void callAcquireTokenSilent(final String resource, final String userUniqueId) {
        mAuthContext.acquireTokenSilentAsync(resource, CLIENT_ID, userUniqueId, new AuthenticationCallback<AuthenticationResult>() {

            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                showMessage("Response from broker: " + authenticationResult.getAccessToken());
            }

            @Override
            public void onError(Exception exc) {
                showMessage("Error occured when acquiring token silently: " + exc.getMessage());
            }
        });
    }
    
    private String getUserLoginHint() {
        return mEditText.getText().toString();
    }

    /**
     * Sample code for getting signature for current package name. 
     * @param packagename
     * @return
     */
    public String getSignatureForPackage(final String packagename) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(packagename,
                    PackageManager.GET_SIGNATURES);
            if (info != null && info.signatures != null && info.signatures.length > 0) {
                Signature signature = info.signatures[0];
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            }
        } catch (NameNotFoundException | NoSuchAlgorithmException e) {
            Logger.e(TAG, "Calling App's package does not exist in PackageManager", "",
                    ADALError.APP_PACKAGE_NAME_NOT_FOUND, e);
        } 
        
        return null;
    }
    
    private Handler getHandler() {
        return new Handler(MainActivity.this.getMainLooper());
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAuthContext.onActivityResult(requestCode, resultCode, data);
    }

    private void showMessage(final String msg) {
        Log.v(TAG, msg);
        getHandler().post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
class SampleTelemetry implements IDispatcher {
    private static final String TAG = "SampleTelemetry";
    @Override
    public void dispatchEvent(final Map<String, String> events) {
        final Iterator iterator = events.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry pair = (Map.Entry) iterator.next();
            Log.e(TAG, pair.getKey() + ":" + pair.getValue());
        }
    }
}
