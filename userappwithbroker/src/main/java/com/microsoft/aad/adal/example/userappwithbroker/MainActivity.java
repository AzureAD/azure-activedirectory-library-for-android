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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.IDispatcher;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.Telemetry;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Sample for acquiring token via broker.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        AcquireTokenFragment.OnFragmentInteractionListener {

    private static final String TAG = "UserAppWithBroker.Main";

    private SharedPreferences mSharedPreference;
    private Context mApplicationContext;

    private static final String SHARED_PREFERENCE_STORE_USER_UNIQUEID = "user.app.withbroker.uniqueidstorage";

    private String mLoginhint;

    private String mAuthority;

    private String mRequestAuthority;

    private AuthenticationResult mAuthResult = null;

    private AuthenticationContext mAuthContext;

    private PromptBehavior mPromptBehavior;

    private RelativeLayout mContentMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mApplicationContext = getApplicationContext();
        setUpADALForCallingBroker();

        mContentMain = (RelativeLayout) findViewById(R.id.content_main);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            // auto select the first item
            onNavigationItemSelected(navigationView.getMenu().getItem(0));
        }
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        final Fragment fragment;
        int menuItemId = item.getItemId();
        if (menuItemId == R.id.nav_acquire) {
            fragment = new AcquireTokenFragment();
        } else if (menuItemId == R.id.nav_result) {
            fragment = new ResultFragment();
            final Bundle bundle = new Bundle();
            if (mAuthResult != null) {
                bundle.putString(ResultFragment.ACCESS_TOKEN, mAuthResult.getAccessToken());
                bundle.putString(ResultFragment.ID_TOKEN, mAuthResult.getIdToken());
            }

            fragment.setArguments(bundle);
            mAuthResult = null;
        } else if (menuItemId == R.id.nav_log) {
            fragment = new LogFragment();
            final String logs = ((ADALSampleApp)this.getApplication()).getLogs();
            final Bundle bundle = new Bundle();
            bundle.putString(LogFragment.LOG_MSG, logs);
            fragment.setArguments(bundle);
        }else {
            fragment = null;
        }

        attachFragment(fragment);
        return true;
    }

    private void attachFragment(final Fragment fragment) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(GravityCompat.START);
        fragmentTransaction.replace(mContentMain.getId(), fragment).addToBackStack(null).commit();
    }

    @Override
    public void onAcquireTokenClicked(final AcquireTokenFragment.RequestOptions requestOptions) {
        prepareRequestParameters(requestOptions);

        callAcquireTokenWithResource(requestOptions.getDataProfile().getText(), requestOptions.getBehavior(),
                requestOptions.getLoginHint(), requestOptions.getClientId().getText(), requestOptions.getRedirectUri().getText(), requestOptions.getExtraQp());
    }

    @Override
    public void onAcquireTokenSilentClicked(final AcquireTokenFragment.RequestOptions requestOptions) {

        prepareRequestParameters(requestOptions);

        callAcquireTokenSilent(requestOptions.getDataProfile().getText(),
                getUserIdBasedOnUPN(requestOptions.getLoginHint(), requestOptions.getAuthorityType().getText()),
                requestOptions.getClientId().getText());
    }

    void prepareRequestParameters(final AcquireTokenFragment.RequestOptions requestOptions) {
        mRequestAuthority = requestOptions.getAuthorityType().getText();
        final String authority = getAuthorityBasedOnUPN(requestOptions.getLoginHint(), mRequestAuthority);
        if (null != authority && !authority.isEmpty()) {
            //Replace the request authority with the preferred authority stored in shared preference
            mAuthority = authority;
            mAuthContext = new AuthenticationContext(mApplicationContext, mAuthority, false);
        } else {
            //If there is no preferred authority stored, use the type-in authority
            mAuthority = mRequestAuthority;
            mAuthContext = new AuthenticationContext(mApplicationContext, mAuthority, true);
        }

        //TODO: We can add UX to set or not set this
        mAuthContext.setClientCapabilites(new ArrayList<>(Arrays.asList("CP1")));
        mLoginhint = requestOptions.getLoginHint();
        mPromptBehavior = requestOptions.getBehavior();
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

    /**
     * To call broker, you have to ensure the following:
     * 1) You have to call {@link AuthenticationSettings#INSTANCE#setUseBroker(boolean)}
     *    and the supplied value has to be true
     * 2) You have to have to correct set of permissions.
     *    If target API version is lower than 23:
     *    i) You have to have GET_ACCOUNTS, USE_CREDENTIAL, MANAGE_ACCOUNTS declared
     *       in manifest.
     *    If target API version is 23:
     *    i)  USE_CREDENTIAL and MANAGE_ACCOUNTS is already deprecated.
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
            // the keypair in AndroidKeyStore. Current investigation shows 1)Keystore may be locked with
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
        SampleTelemetry telemetryDispatcher = new SampleTelemetry();
        Telemetry.getInstance().registerDispatcher(telemetryDispatcher, true);
    }

    private void callAcquireTokenWithResource(final String resource, PromptBehavior prompt, final String loginHint,
                                              final String clientId, final String redirectUri, final String extraQp) {
        mAuthContext.acquireToken(MainActivity.this, resource, clientId, redirectUri, loginHint,
                prompt, extraQp, new AuthenticationCallback<AuthenticationResult>() {

                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        mAuthResult = authenticationResult;
                        showMessage("Response from broker: " + authenticationResult.getAccessToken());

                        // Update this user for next call
                        if (authenticationResult.getUserInfo() != null) {
                            saveUserIdFromAuthenticationResult(authenticationResult, mRequestAuthority);
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
    private void saveUserIdFromAuthenticationResult(final AuthenticationResult authResult, final String authority) {
        mSharedPreference = getSharedPreferences(SHARED_PREFERENCE_STORE_USER_UNIQUEID, MODE_PRIVATE);
        if (null != authResult) {
            if (null != authResult.getAuthority()) {
                final SharedPreferences.Editor prefEditor = mSharedPreference.edit();
                if (null != authResult.getUserInfo() && null != authResult.getUserInfo().getDisplayableId()) {
                    if (null != authResult.getUserInfo() && null != authResult.getUserInfo().getIdentityProvider()) {
                        prefEditor.putString(
                                (authResult.getUserInfo().getDisplayableId().trim() + ":" + authority.trim() +  ":authority").toLowerCase(),
                                authResult.getUserInfo().getIdentityProvider().trim().toLowerCase());
                    } else {
                       prefEditor.putString(
                               (authResult.getUserInfo().getDisplayableId().trim() + ":" + authority.trim() +  ":authority").toLowerCase(),
                               authResult.getAuthority().trim().toLowerCase());
                    }
                }  else {
                    final Toast toast = Toast.makeText(mApplicationContext,
                            "Warning: the result authority is null," +
                                    "Silent auth for Sovereign account will fail. ", Toast.LENGTH_SHORT);
                    toast.show();
                }

                if (null != authResult.getUserInfo()
                    && null != authResult.getUserInfo().getDisplayableId()
                    && null != authResult.getUserInfo().getUserId()) {
                    prefEditor.putString((authResult.getUserInfo().getDisplayableId().trim() + ":" + authority.trim() + ":userId").toLowerCase(), authResult.getUserInfo().getUserId().trim().toLowerCase());
                } else {
                    final Toast toast = Toast.makeText(mApplicationContext,
                            "Warning: the result userInfo is null. " +
                                    "Silent auth without userID will fail. ", Toast.LENGTH_SHORT);
                    toast.show();
                }

                prefEditor.apply();
            }
        }
    }

    /**
     * For the sake of simplicity of the sample app, used id stored in the shared preference is keyed
     * by displayable id.
     */
    private String getUserIdBasedOnUPN(final String upn, final String requestAuthority) {
        mSharedPreference = getSharedPreferences(SHARED_PREFERENCE_STORE_USER_UNIQUEID, MODE_PRIVATE);
        return mSharedPreference.getString((upn.trim() + ":" + requestAuthority.trim() + ":userId").toLowerCase(), null);
    }

    /**
     * Silent acquire token call. Requires to pass the user unique id. If user unique id is not passed, 
     * silent call to broker will be skipped. 
     */
    private void callAcquireTokenSilent(final String resource, final String userUniqueId, final String clientId) {
        mAuthContext.acquireTokenSilentAsync(resource, clientId, userUniqueId, new AuthenticationCallback<AuthenticationResult>() {

            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                mAuthResult = authenticationResult;
                showMessage("Response from broker: " + authenticationResult.getAccessToken());
            }

            @Override
            public void onError(Exception exc) {
                showMessage("Error occurred when acquiring token silently: " + exc.getMessage());
            }
        });
    }

    private String getAuthorityBasedOnUPN(final String upn, final String requestAuthority) {
        mSharedPreference = getSharedPreferences(SHARED_PREFERENCE_STORE_USER_UNIQUEID, MODE_PRIVATE);
        return mSharedPreference.getString((upn.trim() + ":" + requestAuthority.trim() + ":authority").toLowerCase(), null);
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
