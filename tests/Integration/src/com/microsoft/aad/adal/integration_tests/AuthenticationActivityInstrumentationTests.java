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

package com.microsoft.aad.adal.integration_tests;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.app.Instrumentation.ActivityMonitor;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.jayway.android.robotium.solo.By;
import com.jayway.android.robotium.solo.Solo;
import com.jayway.android.robotium.solo.WebElement;
import com.microsoft.aad.adal.ADALError;
import com.microsoft.aad.adal.AuthenticationActivity;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.Logger.ILogger;
import com.microsoft.aad.adal.Logger.LogLevel;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.TokenCacheItem;
import com.microsoft.aad.adal.integration_tests.TenantInfo.TenantType;
import com.microsoft.aad.adal.testapp.MainActivity;
import com.microsoft.aad.adal.testapp.R;

/**
 * This requires device to be connected to not deal with Inject_events security
 * exception. UI functional tests that enter credentials to test token
 * processing end to end.
 */
public class AuthenticationActivityInstrumentationTests extends
        ActivityInstrumentationTestCase2<MainActivity> {

    private static final int SLEEP_NEXT_REQUEST = 3000;

    private Solo solo;

    /**
     * Emulator needs more sleep time than 500ms
     */
    private static final int KEY_PAUSE_SLEEP_TIME = 2000;

    private static final int ACTIVITY_WAIT_TIMEOUT = 5000;

    protected final static int PAGE_LOAD_WAIT_TIME_OUT = 25000; // miliseconds

    private static final String TAG = "AuthenticationActivityInstrumentationTests";

    private MainActivity activity;

    /**
     * until page content has something about login page
     */
    private static int PAGE_LOAD_TIMEOUT = 1200;

    private static final int LOGIN_DISPLAY_TIME_OUT = PAGE_LOAD_TIMEOUT * 10;

    private static final int PAGE_STATUS_SET_TIME_OUT = 400;

    /**
     * Verification depends on external site
     */
    private static final int VERIFY_TIMEOUT = PAGE_LOAD_TIMEOUT * 10;

    final static String[] errorObjectIDs = {
            "cta_error_message_text", "cta_client_error_text", "errorDetails",
            "login_no_cookie_error_text", "cannot_locate_resource", "service_exception_message",
            "errorMessage"
    };

    /**
     * Test setup specific ids to target for automation
     */
    final static String[] expandLinkIDs = {
        "switch_user_link"
    };

    final static String[] signInIDs = {
            "cred_sign_in_button", "ctl00_ContentPlaceHolder1_SubmitButton", "btnSignInMobile",
            "btnSignin", "submitButton"
    };

    final static String[] passwordIDs = {
            "cred_password_inputtext", "ctl00_ContentPlaceHolder1_PasswordTextBox",
            "txtBoxMobilePassword", "txtBoxPassword", "passwordInput"
    };

    final static String[] usernameIDs = {
            "cred_userid_inputtext", "ctl00_ContentPlaceHolder1_UsernameTextBox",
            "txtBoxMobileEmail", "txtBoxEmail", "userNameInput"
    };

    boolean configLoad = false;

    HashMap<TenantType, TenantInfo> tenants = new HashMap<TenantType, TenantInfo>();

    public AuthenticationActivityInstrumentationTests() {
        super(MainActivity.class);
        activity = null;
    }

    public AuthenticationActivityInstrumentationTests(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        activity = getActivity();
        solo = new Solo(getInstrumentation(), activity);
        loadConfigFromResource();
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            // use same key for tests
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
            SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                    "abcdedfdfd".getBytes("UTF-8"), 100, 256));
            SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
            AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        finishActivity();
        activity.setLoggerCallback(null);
        solo.finishOpenedActivities();
        super.tearDown();
    }

    /**
     * add integration_tests.properties file at tests/integration/assets folder
     * 
     * @param name
     */
    private void loadConfigFromResource() {

        if (configLoad)
            return;

        try {
            Log.d(TAG, "Load config from assets");
            InputStream stream = getInstrumentation().getContext().getAssets()
                    .open("integration_tests.properties");
            Properties p = new Properties();
            p.load(stream);
            tenants.put(TenantType.AAD, TenantInfo.parseTenant(TenantType.AAD, p));
            tenants.put(TenantType.ADFS30, TenantInfo.parseTenant(TenantType.ADFS30, p));
            tenants.put(TenantType.ADFS20FEDERATED,
                    TenantInfo.parseTenant(TenantType.ADFS20FEDERATED, p));
            tenants.put(TenantType.ADFS30FEDERATED,
                    TenantInfo.parseTenant(TenantType.ADFS30FEDERATED, p));
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
        configLoad = true;
    }

    @MediumTest
    public void testFakeBackEnd_AcquireToken() throws Exception {
        TenantInfo tenant = new TenantInfo(TenantType.AAD,
                "https://adal.azurewebsites.net/WebRequest", "resource", "resource2",
                "short-live-token", "https://adal.azurewebsites.net/", null, null, null, null, null);

        Log.v(TAG, "testFakeBackEnd_AcquireToken starts for authority:" + tenant.getAuthority());

        // Activity runs at main thread. Test runs on different thread
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        // add monitor to check for the auth activity
        final ActivityMonitor monitor = getInstrumentation().addMonitor(
                AuthenticationActivity.class.getName(), null, false);
        setAuthenticationRequest(tenant, tenant.getResource(), "", PromptBehavior.Auto, null, false);

        verifyTokenFlow(textViewStatus, monitor);
    }

    @MediumTest
    public void testDeviceChallenge() throws Exception {
        TenantInfo tenant = new TenantInfo(TenantType.AAD,
                "https://clientcert.azurewebsites.net/WebRequest", "device-challenge", "resource2",
                "short-live-token", "https://clientcert.azurewebsites.net/", null, null, null,
                null, null);
        Log.v(TAG, "testDeviceChallenge starts for authority:" + tenant.getAuthority());

        // Activity runs at main thread. Test runs on different thread
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        // add monitor to check for the auth activity
        final ActivityMonitor monitor = getInstrumentation().addMonitor(
                AuthenticationActivity.class.getName(), null, false);
        setAuthenticationRequest(tenant, tenant.getResource(), "admin@aaltests.onmicrosoft.com",
                PromptBehavior.Auto, null, false);
        setupDeviceCertificateMock();
        verifyTokenFlow(textViewStatus, monitor);
    }

    @MediumTest
    public void testDeviceChallengeRefreshToken() throws Exception {
        TenantInfo tenant = new TenantInfo(TenantType.AAD,
                "https://clientcert.azurewebsites.net/WebRequest", "device-challenge", "resource2",
                "short-live-token", "https://clientcert.azurewebsites.net/", null, null, null,
                null, null);
        Log.v(TAG, "testDeviceChallenge starts for authority:" + tenant.getAuthority());

        // Activity runs at main thread. Test runs on different thread
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);

        // add monitor to check for the auth activity
        final ActivityMonitor monitor = getInstrumentation().addMonitor(
                AuthenticationActivity.class.getName(), null, false);
        setAuthenticationRequest(tenant, tenant.getResource(), "admin@aaltests.onmicrosoft.com",
                PromptBehavior.Auto, null, false);
        setupDeviceCertificateMock();
        acquireTokenByRefreshToken("DEVICE_CERT_CHALLENGE");
    }

    private void verifyTokenFlow(final TextView textViewStatus, final ActivityMonitor monitor)
            throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        clickResetTokens();
        clickGetToken();
        waitUntil(PAGE_LOAD_TIMEOUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg == MainActivity.GETTING_TOKEN;
            }
        });

        final String startText = textViewStatus.getText().toString();
        assertEquals("Token action", MainActivity.GETTING_TOKEN, startText);

        // Wait to start activity and loading the page
        AuthenticationActivity startedActivity = (AuthenticationActivity)monitor
                .waitForActivityWithTimeout(ACTIVITY_WAIT_TIMEOUT);
        assertNotNull(startedActivity);

        waitUntil(PAGE_STATUS_SET_TIME_OUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg != startText;
            }
        });

        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "Status:" + tokenMsg);
        assertTrue("Token is received", tokenMsg.contains(MainActivity.PASSED));
    }

    @MediumTest
    public void testAcquireTokenADFS30() throws Exception {
        acquireTokenAfterReset(tenants.get(TenantType.ADFS30), "", PromptBehavior.Auto, null,
                false, false, null);
    }

    @MediumTest
    public void testAcquireTokenManaged_MultiUser() throws Exception {
        TenantInfo tenant = tenants.get(TenantType.AAD);
        Log.v(TAG,
                "testAcquireTokenManaged_MultiUser starts for authority:" + tenant.getAuthority());
        acquireTokenAfterReset(tenant, "", PromptBehavior.Auto, null, false, false, null);
        final ActivityMonitor monitor = getInstrumentation().addMonitor(
                AuthenticationActivity.class.getName(), null, false);

        AuthenticationResult result = activity.getResult();
        Log.v(TAG, "Result from initial request. ExpiresOn:" + result.getExpiresOn().getTime());

        Log.v(TAG, "Set second user");
        setUserForAuthenticationRequest(tenant.getUserName2());
        // it will use cookies for different user and just return Authorization
        // code
        clickRemoveCookies();
        clickGetToken();
        handleCredentials(monitor, tenant.getUserName2(), tenant.getPassword2(), false, null);

        Log.v(TAG, "Compare tokens");
        AuthenticationResult result2 = activity.getResult();
        verifyTokenNotSame(result, result2);
        assertTrue("Multi resource token", result2.getIsMultiResourceRefreshToken());
        assertEquals("Username same in idtoken", tenant.getUserName2(), result2.getUserInfo()
                .getUserId());
    }

    @MediumTest
    public void testAcquireTokenSilent() throws Exception {
        TenantInfo tenant = tenants.get(TenantType.AAD);
        // Activity runs at main thread. Test runs on different thread
        Log.v(TAG, "testAcquireTokenSilent starts for authority:" + tenant.getAuthority());
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        final String startText = textViewStatus.getText().toString();
        setAuthenticationRequest(tenant, tenant.getResource(), "", PromptBehavior.Auto, "", false);

        // press clear all button to clear tokens and cookies
        clickResetTokens();
        clickGetTokenSilent();

        waitUntil(PAGE_STATUS_SET_TIME_OUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg != null
                        && tokenMsg.contains("AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED");
            }
        });
        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "Status:" + tokenMsg);
        assertTrue("Token status", tokenMsg.contains("AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED"));
    }

    private void clickGetTokenSilent() {
        activity.getTestAppHandler().post(new Runnable() {
            @Override
            public void run() {
                activity.getTokenSilent();
            }
        });
    }

    /**
     * prompt always setting will force the login prompt again. It should not
     * use the cookies, so webview should display login page.
     * 
     * @throws Exception
     */
    @MediumTest
    public void testAcquireTokenPromptAlways() throws Exception {
        // Get token first
        TenantInfo tenant = tenants.get(TenantType.AAD);
        Log.v(TAG, "testAcquireTokenPromptAlways starts for authority:" + tenant.getAuthority());
        acquireTokenAfterReset(tenant, "", PromptBehavior.Auto, null, false, false, null);

        // click to get token again and monitor authenticationActivity launch
        final ActivityMonitor monitor = getInstrumentation().addMonitor(
                AuthenticationActivity.class.getName(), null, false);

        Log.v(TAG, "Next token request will served from cache");
        setAuthenticationRequest(tenant, tenant.getResource(), "", PromptBehavior.Auto, "", false);
        clickGetToken();

        AuthenticationActivity startedActivity = (AuthenticationActivity)monitor
                .waitForActivityWithTimeout(ACTIVITY_WAIT_TIMEOUT);
        assertNull(startedActivity);

        Log.v(TAG, "Prompt always will launch ");
        setAuthenticationRequest(tenant, tenant.getResource(), "", PromptBehavior.Always, "", false);
        clickGetToken();

        startedActivity = (AuthenticationActivity)monitor
                .waitForActivityWithTimeout(ACTIVITY_WAIT_TIMEOUT);
        assertNotNull(startedActivity);

        sleepUntilVisibleWebElements(startedActivity);
        assertTrue("There should be some visible web elements",
                solo.getCurrentWebElements().size() > 0);

        startedActivity.finish();
    }

    @MediumTest
    public void testAcquireToken_AfterDelay() throws Exception {
        // Get token first
        TenantInfo tenant = tenants.get(TenantType.AAD);
        Log.v(TAG, "testAcquireToken_AfterDelay starts for authority:" + tenant.getAuthority());
        acquireTokenAfterReset(tenant, "", PromptBehavior.Auto, null, false, false, null);
        AuthenticationResult result = activity.getResult();

        Log.d(TAG, "Get token after delay");
        Thread.sleep(SLEEP_NEXT_REQUEST);
        // Stores token based on requested userid
        setAuthenticationRequest(tenant, tenant.getResource(), "", PromptBehavior.Auto, "", false);
        clickGetTokenSilent();
        AuthenticationResult result2 = activity.getResult();
        verifyTokenSame(result, result2);

        Log.d(TAG, "Refresh token compare");
        clickExpire();
        clickGetTokenSilent();
        AuthenticationResult resultRefresh = activity.getResult();

        verifyTokenNotSame(result, resultRefresh);
    }

    private void verifyTokenNotSame(AuthenticationResult result, AuthenticationResult result2) {
        assertFalse("tokens are not same", result.getAccessToken().equals(result2.getAccessToken()));
        assertFalse("refresh tokens are not same",
                result.getRefreshToken().equals(result2.getRefreshToken()));
        assertFalse("expire time diff more than 1000milisecs",
                Math.abs(result.getExpiresOn().getTime() - result2.getExpiresOn().getTime()) < 1000);
    }

    private void verifyTokenSame(AuthenticationResult result, AuthenticationResult result2) {
        assertTrue("tokens are same", result.getAccessToken().equals(result2.getAccessToken()));
        assertTrue("tokens are same", result.getRefreshToken().equals(result2.getRefreshToken()));
        // Date precision issue for miliseconds
        assertTrue("expire time diff less than 1000milisecs",
                Math.abs(result.getExpiresOn().getTime() - result2.getExpiresOn().getTime()) < 1000);
    }

    @MediumTest
    public void testAcquireToken_MultiResource() throws Exception {
        // Get token first
        TenantInfo tenant = tenants.get(TenantType.AAD);
        Log.v(TAG, "testAcquireTokenPromptAlways starts for authority:" + tenant.getAuthority());
        acquireTokenAfterReset(tenant, "", PromptBehavior.Auto, null, false, false, null);
        AuthenticationResult result = activity.getResult();

        Log.d(TAG, "Ask token for resource2");
        assertTrue("Token is multiresource", result.getIsMultiResourceRefreshToken());
        setAuthenticationRequest(tenant, tenant.getResource2(), "", PromptBehavior.Auto, "", false);
        Thread.sleep(SLEEP_NEXT_REQUEST);
        clickGetTokenSilent();
        AuthenticationResult result2 = activity.getResult();

        Log.d(TAG, "Refresh token should be used for second resource");
        verifyTokenNotSame(result, result2);
    }

    @MediumTest
    public void testAcquireToken_CacheMultiple() throws Exception {
        TenantInfo tenant = tenants.get(TenantType.AAD);
        Log.v(TAG, "testAcquireTokenPromptAlways starts for authority:" + tenant.getAuthority());
        acquireTokenAfterReset(tenant, "", PromptBehavior.Auto, null, false, false, null);
        AuthenticationResult result = activity.getResult();

        Log.d(TAG, "Ask token for resource2");
        assertTrue("token is multiresource", result.getIsMultiResourceRefreshToken());
        setAuthenticationRequest(tenant, tenant.getResource(), "", PromptBehavior.Auto, "", false);
        for (int i = 0; i < 10; i++) {
            clickGetTokenSilent();
            AuthenticationResult result2 = activity.getResult();
            verifyTokenSame(result, result2);
        }
    }

    /**
     * Verify that extra query param works at authorization endpoint
     * 
     * @throws Exception
     */
    @MediumTest
    public void testAcquireToken_ExtraQueryParam() throws Exception {
        // Get token first
        TenantInfo tenant = tenants.get(TenantType.AAD);
        Log.v(TAG, "testAcquireTokenPromptAlways starts for authority:" + tenant.getAuthority());
        acquireTokenAfterReset(tenant, "", PromptBehavior.Auto, null, false, false, null);

        // click to get token again and monitor authenticationActivity launch
        final ActivityMonitor monitor = getInstrumentation().addMonitor(
                AuthenticationActivity.class.getName(), null, false);

        Log.v(TAG, "testAcquireToken_ExtraQueryParam trying extra query param");
        setAuthenticationRequest(tenant, tenant.getResource(), "", PromptBehavior.Auto,
                "prompt=login", false);
        removeTokens();
        clickGetToken();

        Log.v(TAG, "prompt=login param will be posted to authorization endpoint");
        AuthenticationActivity startedActivity = (AuthenticationActivity)monitor
                .waitForActivityWithTimeout(ACTIVITY_WAIT_TIMEOUT);
        assertNotNull(startedActivity);
        startedActivity.finish();
    }

    @MediumTest
    public void testCorrelationId() throws Exception {
        Log.v(TAG, "Started testing correlationId");

        // Get token to test refresh token request with correlationId
        TenantInfo tenant = tenants.get(TenantType.AAD);
        acquireTokenAfterReset(tenant, "", PromptBehavior.Auto, null, false, false, null);

        UUID correlationId = UUID.randomUUID();
        activity.setRequestCorrelationId(correlationId);
        assertNotNull("Has token before checking correlationid", activity.getResult());
        assertNotNull("Has token before checking correlationid", activity.getResult()
                .getAccessToken());
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);

        // Make token expired in the instrumentation App
        clickExpire();

        // Modify resource to create a failure for refresh token request.
        // acquireToken will try to refresh the token if it is expired.
        activity.getTestAppHandler().post(new Runnable() {
            @Override
            public void run() {
                final EditText mClient;
                mClient = (EditText)activity.findViewById(R.id.editClientid);
                mClient.setText("invalid");
            }
        });

        Log.v(TAG, "Ask token again with invalid client");
        clickGetTokenSilent();

        // waiting for the page to set result
        Log.v(TAG, "Wait for the page to set the result");

        waitUntil(PAGE_STATUS_SET_TIME_OUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg != null
                        && tokenMsg.contains(ADALError.AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED
                                .name());
            }
        });

        Log.v(TAG, "Finished waiting for the result");
        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "acquireTokenExpired Status:" + tokenMsg);
        AuthenticationResult result = activity.getResult();
        assertNull("Result is null", result);
        assertTrue("CorrelationId in response same as in request header", activity
                .getLastException().getCause().getMessage().contains(correlationId.toString()));
        Log.v(TAG, "Finished testing correlationId");
    }

    /**
     * Sometimes, it could not post the form. Enter key event is not working
     * properly.
     * 
     * @throws Exception
     */
    @LargeTest
    public void testAcquireTokenManaged() throws Exception {

        // Not validating
        TenantInfo tenant = tenants.get(TenantType.AAD);
        Log.v(TAG, "testing acquireTokenAfterReset for managed without validation");
        acquireTokenAfterReset(tenant, "", PromptBehavior.Auto, null, false, false, null);

        // Validation set to true
        Log.v(TAG, "testing acquireTokenAfterReset for managed with validation");
        acquireTokenAfterReset(tenant, "", PromptBehavior.Auto, null, true, false, null);

        // use existing token
        Log.v(TAG, "testing acquireTokenByRefreshToken for managed");
        acquireTokenByRefreshToken();

        // verify with webservice
        Log.v(TAG, "verifying token for managed");
        verifyToken();

        verifyRefreshRequest();
    }

    /**
     * get token with loginhing, refresh and check cache to make sure there are
     * only one entry for normal token
     * 
     * @throws Exception
     */
    @LargeTest
    public void testAcquireToken_RefreshCacheStorage() throws Exception {

        // Not validating
        TenantInfo tenant = tenants.get(TenantType.AAD);
        Log.v(TAG, "testing acquireTokenAfterReset for managed without validation");
        acquireTokenAfterReset(tenant, tenant.getUserName(), PromptBehavior.Auto, null, false,
                false, null);

        // Expire token
        clickExpire();

        // acquireToken call again will find the refresh token and send a
        // request.
        clickGetToken();
        // wait for the page to set result
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        Log.v(TAG, "Waiting for the refresh request to complete");

        waitUntil(PAGE_LOAD_TIMEOUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg != MainActivity.GETTING_TOKEN;
            }
        });

        // Check cache from target app
        activity.getTestAppHandler().post(new Runnable() {
            @Override
            public void run() {
                ArrayList<TokenCacheItem> items = activity.getTokens();
                Log.v(TAG, "Checking token sizes: " + items.size());
                assertTrue("Only normal token and multiresource refresh token", items.size() == 2);
                assertTrue("Only one is MRRT", items.get(0).getIsMultiResourceRefreshToken()
                        ^ items.get(1).getIsMultiResourceRefreshToken());
            }
        });
    }

    private void verifyRefreshRequest() throws IllegalArgumentException, InterruptedException,
            NoSuchFieldException, IllegalAccessException {

        Log.v(TAG, "Started to test refresh token request");
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        clickExpire();
        clickGetToken();
        String startText = (String)textViewStatus.getText();

        // wait for the page to set result
        Log.v(TAG, "Wait for the page to set the result. Initial status:" + startText);

        waitUntil(PAGE_LOAD_TIMEOUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg != MainActivity.GETTING_TOKEN;
            }
        });

        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "acquireTokenExpired Status:" + tokenMsg);
        assertTrue("Token is received", tokenMsg.contains(MainActivity.PASSED));
        Log.v(TAG, "Finished to test refresh token request");
    }

    /**
     * send token to webapi endpoint to get ok
     * 
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void verifyToken() throws IllegalArgumentException, InterruptedException,
            NoSuchFieldException, IllegalAccessException {
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);

        // verify existing token at the target application
        clickVerify();

        waitUntil(VERIFY_TIMEOUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg != null && !tokenMsg.isEmpty();
            }
        });

        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "Status:" + tokenMsg);
        assertTrue("Token status", tokenMsg.contains(MainActivity.TOKEN_USED));
    }

    /**
     * use existing AuthenticationResult in the app to call
     * acquireTokenByRefreshToken
     * 
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void acquireTokenByRefreshToken() throws IllegalArgumentException,
            InterruptedException, NoSuchFieldException, IllegalAccessException {
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);

        // verify existing token at the target application
        verifyTokenExists();
        clickRefresh();

        waitUntil(PAGE_LOAD_TIMEOUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg != null && !tokenMsg.isEmpty();
            }
        });

        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "Status:" + tokenMsg);
        assertTrue("Token status", tokenMsg.contains(MainActivity.PASSED));
    }

    private void setUserForAuthenticationRequest(final String userid) {
        // press clear all button to clear tokens and cookies
        final EditText mUserid;

        mUserid = (EditText)activity.findViewById(R.id.editUserId);

        // Use handler from this app to quickly set the fields instead of
        // sending key events
        activity.getTestAppHandler().post(new Runnable() {
            @Override
            public void run() {

                mUserid.setText(userid);
            }
        });
    }

    private void setAuthenticationRequest(final TenantInfo tenant, final String resource,
            final String loginhint, final PromptBehavior prompt, final String extraQueryParam,
            final boolean validate) {
        // ACtivity runs at main thread. Test runs on different thread
        Log.v(TAG, "acquireTokenAfterReset starts for authority:" + tenant.getAuthority());

        // press clear all button to clear tokens and cookies
        final EditText mAuthority, mResource, mClientId, mUserid, mPrompt, mRedirect;
        final CheckBox mValidate;

        mAuthority = (EditText)activity.findViewById(R.id.editAuthority);
        mResource = (EditText)activity.findViewById(R.id.editResource);
        mClientId = (EditText)activity.findViewById(R.id.editClientid);
        mUserid = (EditText)activity.findViewById(R.id.editUserId);
        mPrompt = (EditText)activity.findViewById(R.id.editPrompt);
        mRedirect = (EditText)activity.findViewById(R.id.editRedirect);
        mValidate = (CheckBox)activity.findViewById(R.id.checkBoxValidate);

        // Use handler from this app to quickly set the fields instead of
        // sending key events
        activity.getTestAppHandler().post(new Runnable() {
            @Override
            public void run() {
                mAuthority.setText(tenant.getAuthority());
                mResource.setText(resource);
                mClientId.setText(tenant.getClientId());
                mUserid.setText(loginhint);
                mPrompt.setText(prompt.name());
                mRedirect.setText(tenant.getRedirect());
                mValidate.setChecked(validate);
            }
        });
        activity.setExtraQueryParam(extraQueryParam);
    }

    private void clickResetTokens() {
        TouchUtils.clickView(this, (Button)activity.findViewById(R.id.buttonReset));
    }

    private void clickGetToken() {
        TouchUtils.clickView(this, (Button)activity.findViewById(R.id.buttonGetToken));
    }

    private void clickExpire() {
        TouchUtils.clickView(this, (Button)activity.findViewById(R.id.buttonExpired));
    }

    private void clickVerify() {
        TouchUtils.clickView(this, (Button)activity.findViewById(R.id.buttonVerify));
    }

    private void clickRemoveCookies() {
        TouchUtils.clickView(this, (Button)activity.findViewById(R.id.buttonRemoveCookies));
    }

    private void clickRefresh() {
        TouchUtils.clickView(this, (Button)activity.findViewById(R.id.buttonRefresh));
    }

    /**
     * finish main activity at test app
     */
    private void finishActivity() {
        if (activity != null && !activity.isFinishing()) {
            Log.v(TAG, "Shutting down activity");
            activity.finish();
        }
    }

    private void setupDeviceCertificateMock() {
        activity.getTestAppHandler().post(new Runnable() {
            @Override
            public void run() {

                try {
                    activity.initDeviceCertificateMock();
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "initDeviceCertificateMock failed", e);
                } catch (UnrecoverableKeyException e) {
                    Log.e(TAG, "initDeviceCertificateMock failed", e);
                } catch (CertificateException e) {
                    Log.e(TAG, "initDeviceCertificateMock failed", e);
                } catch (KeyStoreException e) {
                    Log.e(TAG, "initDeviceCertificateMock failed", e);
                } catch (IOException e) {
                    Log.e(TAG, "initDeviceCertificateMock failed", e);
                }
            }
        });
    }

    private void acquireTokenByRefreshToken(final String refreshToken)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            InterruptedException {
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        Logger.v(TAG, "Send refresh token request");
        activity.getTestAppHandler().post(new Runnable() {
            @Override
            public void run() {
                activity.acquireTokenByRefreshToken(refreshToken);
            }
        });

        waitUntil(PAGE_LOAD_TIMEOUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg.equals(MainActivity.PASSED);
            }
        });

        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "textViewStatus Text:" + tokenMsg);
        assertTrue("Token status", tokenMsg.contains(MainActivity.PASSED));
    }

    private void verifyTokenExists() {
        AuthenticationResult result = activity.getResult();
        assertNotNull("Authentication result is not null", result);
        assertTrue("Token in Authentication result is not null", result.getAccessToken() != null
                && !result.getAccessToken().isEmpty());
    }

    /**
     * Instrumented app is used to send info
     * 
     * @param monitor
     * @param username
     * @param password
     * @param federated
     * @param federatedPageUrl
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void handleCredentials(final ActivityMonitor monitor, String username, String password,
            boolean federated, String federatedPageUrl) throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        waitUntil(PAGE_LOAD_TIMEOUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg == MainActivity.GETTING_TOKEN;
            }
        });

        final String startText = textViewStatus.getText().toString();
        assertEquals("Token action", MainActivity.GETTING_TOKEN, startText);

        // Wait to start activity and loading the page
        AuthenticationActivity startedActivity = (AuthenticationActivity)monitor
                .waitForActivityWithTimeout(ACTIVITY_WAIT_TIMEOUT);
        assertNotNull(startedActivity);

        Log.v(TAG, "Sleeping until it gets the login page");
        sleepUntilVisibleWebElements(startedActivity);

        Log.v(TAG, "Entering credentials to login page");
        checkVisibleWebElements();
        enterCredentials(federated, federatedPageUrl, startedActivity, username, password);

        // wait for the page to set result
        Log.v(TAG, "Wait for the page to set the result");

        waitUntil(PAGE_STATUS_SET_TIME_OUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg != startText;
            }
        });

        if (!startedActivity.isFinishing()) {
            Log.w(TAG, "AuthenticationActivity  was not closed");
            startedActivity.finish();
        }
    }

    /**
     * clear tokens and then ask for token.
     * 
     * @throws Exception
     */
    private void acquireTokenAfterReset(TenantInfo tenant, String loginhint, PromptBehavior prompt,
            String extraQueryParam, boolean validate, boolean federated, String federatedPageUrl)
            throws Exception {
        Log.v(TAG, "acquireTokenAfterReset starts for authority:" + tenant.getAuthority());

        // Activity runs at main thread. Test runs on different thread
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        // add monitor to check for the auth activity
        final ActivityMonitor monitor = getInstrumentation().addMonitor(
                AuthenticationActivity.class.getName(), null, false);
        setAuthenticationRequest(tenant, tenant.getResource(), loginhint, prompt, extraQueryParam,
                validate);

        // press clear all button to clear tokens and cookies
        clickResetTokens();
        clickGetToken();
        handleCredentials(monitor, tenant.getUserName(), tenant.getPassword(), federated,
                federatedPageUrl);

        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "Status:" + tokenMsg);
        assertTrue("Token is received", tokenMsg.contains(MainActivity.PASSED));
    }

    private void enterCredentials(boolean waitForRedirect, String redirectUrl,
            AuthenticationActivity startedActivity, String username, String password)
            throws InterruptedException, IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException {

        // Get Webview to enter credentials for testing
        assertNotNull("startedActivity is expected to be not null at enterCredentials",
                startedActivity);
        sleepUntilVisibleWebElements(startedActivity);
        WebView webview = (WebView)startedActivity
                .findViewById(com.microsoft.aad.adal.R.id.webView1);
        assertNotNull("Webview is not null", webview);
        webview.requestFocus();

        ArrayList<WebElement> elements = checkVisibleWebElements();
        checkErrorInPage(elements);

        enterTextIntoWebElement(elements, usernameIDs, username);
        pressKey(KeyEvent.KEYCODE_TAB);

        if (waitForRedirect) {
            // After pressing tab key, page will redirect to federated login
            // page
            // for federated account
            // federation page redirects to login page
            Log.v(TAG, "Sleep for redirect");
            sleepUntilFederatedPageDisplays(redirectUrl);

            Log.v(TAG, "Sleeping until it gets login page");
            sleepUntilVisibleWebElements(startedActivity);

            Log.v(TAG, "Entering credentials to login page");
            enterCredentials(false, null, startedActivity, username, password);
            return;
        }

        enterTextIntoWebElement(elements, passwordIDs, password);

        // Enter event sometimes is failing to submit form.
        clickWebElement(elements, signInIDs);
        Log.v(TAG, "Credentials are passed");
    }

    private ArrayList<WebElement> checkVisibleWebElements() {
        ArrayList<WebElement> elements = solo.getCurrentWebElements();
        assertNotNull(elements);
        return elements;
    }

    private void checkErrorInPage(ArrayList<WebElement> elements) {

        Log.v(TAG, "Look for error in Page...");
        for (WebElement element : elements) {
            for (String id : errorObjectIDs) {
                assertFalse("Page has an error...", element.getId().equals(id));
            }
        }
    }

    private void clickWebElement(ArrayList<WebElement> elements, String[] ids) {

        Log.v(TAG, "Click on web element");
        for (WebElement element : elements) {
            for (String id : ids) {
                if (element.getId().equals(id)) {
                    // Get element position again
                    solo.clickOnWebElement(By.id(id));
                    Log.v(TAG, "WebElement to click:" + id);
                    return;
                }
            }
        }

        assertFalse("Element is not found at webview", true);
    }

    private void enterTextIntoWebElement(ArrayList<WebElement> elements, String[] ids, String text) {
        Log.v(TAG, "Total elements in the page:" + elements.size());
        for (WebElement element : elements) {
            for (String id : ids) {
                if (element.getId().equals(id)) {
                    if (element.getText().equals(text)) {
                        Log.v(TAG, "WebElement:" + id + " has text:" + text);
                    } else {
                        // Get element position again
                        solo.hideSoftKeyboard();

                        // not use keyboard
                        getInstrumentation().sendStringSync(text);
                        Log.v(TAG, "Entered " + text + " at " + id);
                    }
                    return;
                }
            }
        }

        assertFalse("Element is not found at webview", true);
    }

    private void pressKey(int keycode) throws InterruptedException {
        // It needs sleep time for simulating key press
        Thread.sleep(KEY_PAUSE_SLEEP_TIME);
        getInstrumentation().sendCharacterSync(keycode);
    }

    private void sleepUntilFederatedPageDisplays(final String federatedPageUrl)
            throws IllegalArgumentException, InterruptedException, NoSuchFieldException,
            IllegalAccessException {
        Log.v(TAG, "sleepUntilFederatedPageDisplays:" + federatedPageUrl);

        final CountDownLatch signal = new CountDownLatch(1);
        final ILogger loggerCallback = new ILogger() {
            @Override
            public void Log(String tag, String message, String additionalMessage, LogLevel level,
                    ADALError errorCode) {

                Log.v(TAG, "sleepUntilFederatedPageDisplays Message playback:" + message);
                if (message.toLowerCase(Locale.US).contains("page finished:" + federatedPageUrl)) {
                    Log.v(TAG, "sleepUntilFederatedPageDisplays Page is loaded:" + federatedPageUrl);
                    signal.countDown();
                    Log.v(TAG, "sleepUntilFederatedPageDisplays clears callback for:"
                            + federatedPageUrl);
                    activity.setLoggerCallback(null);
                }
            }
        };

        activity.setLoggerCallback(loggerCallback);

        try {
            signal.await(PAGE_LOAD_WAIT_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            assertFalse("Timeout " + getName(), true);
        }
    }

    private void sleepUntilVisibleWebElements(final AuthenticationActivity startedActivity)
            throws InterruptedException, IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException {

        Log.v(TAG, "sleepUntilVisibleWebElements start");

        waitUntil(LOGIN_DISPLAY_TIME_OUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                return solo.getCurrentWebElements().size() > 2;
            }
        });

        Log.v(TAG, "sleepUntilVisibleWebElements end");
        assertTrue(solo.getCurrentWebElements().size() > 2);
    }

    /**
     * sleep 50ms for each check
     * 
     * @param timeOut
     * @param item
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void waitUntil(int timeOut, ResponseVerifier item) throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        int waitcount = 0;
        Log.v(TAG, "waitUntil started");
        while (waitcount < timeOut) {

            if (waitcount % 40 == 0) {
                Log.v(TAG, "waiting...");
            }

            if (item.hasCondition()) {
                break;
            }

            Thread.sleep(50);
            waitcount++;
        }
        Log.v(TAG, "waitUntil ends");
    }

    interface ResponseVerifier {
        boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                IllegalAccessException;
    }

    private void removeTokens() {
        activity.getTestAppHandler().post(new Runnable() {
            @Override
            public void run() {
                activity.removeTokens();
            }
        });
    }

}
