
package com.microsoft.adal.test;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

import com.microsoft.adal.ADALError;
import com.microsoft.adal.AuthenticationActivity;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.Logger.ILogger;
import com.microsoft.adal.Logger.LogLevel;
import com.microsoft.adal.PromptBehavior;
import com.microsoft.adal.testapp.MainActivity;
import com.microsoft.adal.testapp.R;

/**
 * This requires device to be connected to not deal with Inject_events security
 * exception. UI functional tests that enter credentials to test token
 * processing end to end.
 * 
 * @author omercan
 */
public class AuthenticationActivityInstrumentationTests extends
        ActivityInstrumentationTestCase2<MainActivity> {

    private static final int KEY_PAUSE_SLEEP_TIME = 500;

    private static final int ACTIVITY_WAIT_TIMEOUT = 5000;

    protected final static int PAGE_LOAD_WAIT_TIME_OUT = 25000; // miliseconds

    private static final String TAG = "AuthenticationActivityInstrumentationTests";

    private MainActivity activity;

    /**
     * until page content has something about login page
     */
    private static int PAGE_LOAD_TIMEOUT = 120;

    private static final int LOGIN_DISPLAY_TIME_OUT = PAGE_LOAD_TIMEOUT * 10;

    private static final int PAGE_STATUS_SET_TIME_OUT = PAGE_LOAD_TIMEOUT * 3;

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
    }

    @Override
    protected void tearDown() throws Exception {
        finishActivity();
        super.tearDown();
    }

    @MediumTest
    public void testAcquireTokenADFS30Federated() throws Exception {
        acquireTokenAfterReset(TestTenant.ADFS30FEDERATED, "", PromptBehavior.Auto, null, false,
                true, "https://fs.ade2eadfs30.com");
    }

    @MediumTest
    public void testAcquireTokenADFS20Federated() throws Exception {
        acquireTokenAfterReset(TestTenant.ADFS20FEDERATED, "", PromptBehavior.Auto, null, false,
                true, "https://fs.ade2eadfs20.com");
    }

    @MediumTest
    public void testAcquireTokenADFS30() throws Exception {
        acquireTokenAfterReset(TestTenant.ADFS30, "", PromptBehavior.Auto, null, false, false, null);
    }

    @MediumTest
    public void testAcquireTokenPromptNever() throws Exception {
        TestTenant tenant = TestTenant.MANAGED;
        // Activity runs at main thread. Test runs on different thread
        Log.v(TAG, "testAcquireToken_Prompt starts for authority:" + tenant.getAuthority());
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        setAuthenticationRequest(tenant, "", PromptBehavior.Never, "", false);

        // press clear all button to clear tokens and cookies
        clickResetTokens();
        clickGetToken();

        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "Status:" + tokenMsg);
        assertTrue("Token status", tokenMsg.contains("AUTH_REFRESH_FAILED_PROMPT_NOT_ALLOWED"));
    }

    @MediumTest
    public void testCorrelationId() throws Exception {
        Log.v(TAG, "Started testing correlationId");
        
        // Get token to test refresh token request with correlationId
        acquireTokenAfterReset(TestTenant.MANAGED, "", PromptBehavior.Auto, null, false, false,
                null);
        
        UUID correlationId = UUID.randomUUID();
        activity.setRequestCorrelationId(correlationId);
        assertNotNull("Has token before checking correlationid", activity.getResult());
        assertNotNull("Has token before checking correlationid", activity.getResult().getAccessToken());
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        
        // Make token expired in the instrumentation App
        clickExpire();

        // Modify resource to create a failure for refresh token request. acquireToken will try to refresh the token if it is expired.
        activity.getTestAppHandler().post(new Runnable() {
            @Override
            public void run() {
                final EditText mClient, mPrompt;
                mClient = (EditText)activity.findViewById(R.id.editClientid);
                mPrompt = (EditText)activity.findViewById(R.id.editPrompt);
                mClient.setText("invalid");
                
                // We dont want to try Webview to launch
                mPrompt.setText(PromptBehavior.Never.name());
            }
        });
        
        clickRefresh();

        // waiting for the page to set result
        Log.v(TAG, "Wait for the page to set the result");

        waitUntil(PAGE_STATUS_SET_TIME_OUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                String tokenMsg = (String)textViewStatus.getText();
                return tokenMsg != MainActivity.GETTING_TOKEN;
            }
        });

        Log.v(TAG, "Finished waiting for the result");
        
        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "acquireTokenExpired Status:" + tokenMsg);
        AuthenticationResult result = activity.getResult();
        assertNotNull("Result is not null", result);
        assertEquals("Result status is failed", AuthenticationResult.AuthenticationStatus.Failed,
                result.getStatus());
        assertEquals("CorrelationId in response same as in request header", correlationId,
                result.getCorrelationId());
        assertNull("No token", activity.getResult().getAccessToken());
        
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
        acquireTokenAfterReset(TestTenant.MANAGED, "", PromptBehavior.Auto, null, false, false,
                null);

        // Validation set to true
        acquireTokenAfterReset(TestTenant.MANAGED, "", PromptBehavior.Auto, null, true, false, null);

        // use existing token
        acquireTokenByRefreshToken();

        // verify with webservice call
        verifyToken();

        verifyRefreshRequest();       
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

    private void setAuthenticationRequest(final TestTenant tenant, final String loginhint,
            final PromptBehavior prompt, String extraQueryParam, final boolean validate) {
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
                mResource.setText(tenant.getResource());
                mClientId.setText(tenant.getClientId());
                mUserid.setText(loginhint);
                mPrompt.setText(prompt.name());
                mRedirect.setText(tenant.getRedirect());
                mValidate.setChecked(validate);
            }
        });
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

    private void verifyTokenExists() {
        AuthenticationResult result = activity.getResult();
        assertNotNull("Authentication result is not null", result);
        assertTrue("Token in Authentication result is not null", result.getAccessToken() != null
                && !result.getAccessToken().isEmpty());
    }

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
        sleepUntilLoginDisplays(startedActivity);

        Log.v(TAG, "Entering credentials to login page");
        assertTrue("Activity has login page", hasLoginPage(getLoginPage(startedActivity)));
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
    private void acquireTokenAfterReset(TestTenant tenant, String loginhint, PromptBehavior prompt,
            String extraQueryParam, boolean validate, boolean federated, String federatedPageUrl)
            throws Exception {
        Log.v(TAG, "acquireTokenAfterReset starts for authority:" + tenant.getAuthority());

        // Activity runs at main thread. Test runs on different thread
        final TextView textViewStatus = (TextView)activity.findViewById(R.id.textViewStatus);
        // add monitor to check for the auth activity
        final ActivityMonitor monitor = getInstrumentation().addMonitor(
                AuthenticationActivity.class.getName(), null, false);
        setAuthenticationRequest(tenant, loginhint, prompt, extraQueryParam, validate);

        // press clear all button to clear tokens and cookies
        clickResetTokens();
        clickGetToken();
        handleCredentials(monitor, tenant.getUserName(), tenant.getPassword(), federated,
                federatedPageUrl);

        String tokenMsg = (String)textViewStatus.getText();
        Log.v(TAG, "Status:" + tokenMsg);
        assertTrue("Token is received", tokenMsg.contains(MainActivity.PASSED));
    }

    private void enterCredentials(boolean waitForRedirect, String redirectUrl, AuthenticationActivity startedActivity, String username,
            String password) throws InterruptedException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {

        // Get Webview to enter credentials for testing
        WebView webview = (WebView)startedActivity.findViewById(com.microsoft.adal.R.id.webView1);
        assertNotNull("Webview is not null", webview);
        webview.requestFocus();

        String page = getLoginPage(startedActivity);
        if (!page.contains(username)) {
            Log.v(TAG, "Page does not have this username");
            // Send username after sleeping to wait for the focus on the field           
            Thread.sleep(KEY_PAUSE_SLEEP_TIME);
            getInstrumentation().sendStringSync(username);
            // Redirect page tracking can 
        }else{
            Log.v(TAG, "Page has this username");
        }

        pressKey(KeyEvent.KEYCODE_TAB);
        // After pressing tab key, page will redirect to federated login page for federated account
        if (waitForRedirect) {
            // federation page redirects to login page
            Log.v(TAG, "Sleep for redirect");
            sleepUntilFederatedPageDisplays(redirectUrl);

            Log.v(TAG, "Sleeping until it gets login page");
            sleepUntilLoginDisplays(startedActivity);
            
            Log.v(TAG, "Entering credentials to login page");
            enterCredentials(false, null, startedActivity, username, password);
        }
        
        getInstrumentation().sendStringSync(password);
        
        // Enter event sometimes is failing to submit form.
        pressKey(KeyEvent.KEYCODE_ENTER);
        Log.v(TAG, "Credentials are passed");
    }
    
    private void pressKey(int keycode) throws InterruptedException{
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

    private void sleepUntilLoginDisplays(final AuthenticationActivity startedActivity)
            throws InterruptedException, IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException {

        Log.v(TAG, "sleepUntilLoginDisplays start");

        // This depends on connection
        waitUntil(LOGIN_DISPLAY_TIME_OUT, new ResponseVerifier() {
            @Override
            public boolean hasCondition() throws IllegalArgumentException, NoSuchFieldException,
                    IllegalAccessException {
                return hasLoginPage(getLoginPage(startedActivity));
            }
        });

        Log.v(TAG, "sleepUntilLoginDisplays end");
    }

    private void waitUntil(int timeOut, ResponseVerifier item) throws InterruptedException,
            IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        int waitcount = 0;
        Log.v(TAG, "waitUntil started");
        while (waitcount < timeOut) {
            Log.v(TAG, "waiting...");
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

    /**
     * Login page content is written to the script object with javascript
     * injection
     * 
     * @param startedActivity
     * @return
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private String getLoginPage(AuthenticationActivity startedActivity)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        Object scriptInterface = ReflectionUtils.getFieldValue(startedActivity, "mScriptInterface");

        Object content = ReflectionUtils.getFieldValue(scriptInterface, "mHtml");

        // skip empty page
        if (content != null
                && !content.toString().equalsIgnoreCase("<html><head></head><body></body></html>"))
            return content.toString();

        return null;
    }

    /**
     * this can change based on login page implementation
     * 
     * @param htmlContent
     * @return
     */
    private boolean hasLoginPage(String htmlContent) {
        return htmlContent != null && !htmlContent.isEmpty() && htmlContent.contains("password");
    }
}
