
package com.microsoft.adal.testapp;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.HttpWebResponse;
import com.microsoft.adal.PromptBehavior;
import com.microsoft.adal.WebRequestHandler;

public class TestScriptRunner {
    private static final String TAG = "TestScriptRunner";

    private static final String TARGET_URL = "https://adal.azurewebsites.net/api/values";

    private Gson gson = new Gson();

    private Activity mActivity;

    public TestScriptRunner(Activity activity) {
        mActivity = activity;
    }

    public void processTestScript(String script) {
        // 1- json decode
        // 2- generate TestCommand object
        // 3- run test command
        Log.v(TAG, "received test script");
        TestScriptInfo cacheItem = gson.fromJson(script, TestScriptInfo.class);
        TestResultInfo[] results = cacheItem.run();
        Log.v(TAG, "executed test script");
        postResults(results);
    }

    private void postResults(TestResultInfo[] results) {
        // TODO Auto-generated method stub
        new TestSubmitTask(TARGET_URL, gson.toJson(results));
    }

    public String makeScript() {
        // to test it
        TestScriptInfo script = new TestScriptInfo();
        TestCaseInfo test1 = new TestCaseInfo();
        TestAction action1 = getAction("Enter", "resource", "resource-test");
        test1.testActions = new TestAction[1];
        test1.testActions[0] = action1;
        script.testCases = new TestCaseInfo[1];
        script.testCases[0] = test1;

        String checkText = gson.toJson(script);
        return checkText;
    }

    class TestScriptInfo {
        TestCaseInfo[] testCases;

        TestResultInfo[] results;

        /**
         * Run all of the test cases
         */
        public TestResultInfo[] run() {
            // run each test case and collect result
            if (testCases != null) {

                TestResultInfo[] results = new TestResultInfo[testCases.length];

                for (int i = 0; i < testCases.length; i++) {

                    try {
                        results[i] = testCases[i].run();
                    } catch (Exception e) {
                        results[i] = new TestResultInfo(testCases[i].testName);
                        results[i].mStatusOk = false;
                        results[i].mTestMsg = e.getMessage();
                    }
                }

                return results;
            }
            return null;
        }
    }

    class TestCaseInfo {

        String testName;

        TestAction[] testActions;

        AssertFlag[] testAsserts;

        public TestResultInfo run() throws InterruptedException {
            Log.v(TAG, "running test case:" + testName);
            TestResultInfo testResult = new TestResultInfo(testName);
            TestData testRunData = new TestData();
            for (int i = 0; i < testActions.length; i++) {
                TestAction action = getAction(testActions[i].mName, testActions[i].mTarget,
                        testActions[i].mValue);
                Log.v(TAG, "running test case:" + testName + " action:" + action.mName + " target:"
                        + action.mTarget);

                action.perform(testRunData);
                // call api directly if possible or do click actions on UI
            }

            for (AssertFlag flagCheck : testAsserts) {
                Log.v(TAG, "verifying test case:" + testName + " flag:" + flagCheck.mTarget);
                flagCheck.perform(testRunData);
                if (!flagCheck.mValue) {
                    testResult.mStatusOk = false;
                    testResult.mTestMsg = flagCheck.mTarget + " failed";
                    break;
                }
            }

            return testResult;
        }
    }

    /**
     * rsult to submit back
     * 
     * @author omercan
     */
    class TestResultInfo {
        String mTestName;

        boolean mStatusOk = true;

        String mTestMsg;

        public TestResultInfo() {
        }

        public TestResultInfo(String name) {
            mTestName = name;
        }
    }

    /**
     * target can be success or fail that is set at textbox
     * 
     * @author omercan
     */
    class AssertFlag {
        private String mTarget;

        private boolean mValue;

        public void perform(TestData testRunData) {
            // define factory method and subtypes for asserts if necessary
            if (mTarget.equals("HasAccessToken")) {
                mValue = testRunData != null && testRunData.mResult != null
                        && !testRunData.mResult.getAccessToken().isEmpty();
            }
        }
    }

    private TestAction getAction(String name, String target, String value) {
        // add different actions related to the API here
        if (name.equals("AcquireToken")) {
            return new AcquireTokenAction(name, target, value);
        } else if (name.equals("ResetAllTokens")) {
            return new ResetAllTokensAction(name, target, value);
        } else if (name.equals("Enter")) {
            return new EnterAction(name, target, value);
        }

        return null;
    }

    class EnterAction extends TestAction {

        public EnterAction() {
        }

        public EnterAction(String name, String target, String value) {
            super(name, target, value);
        }

        public void perform(final TestData data) throws InterruptedException {
            if (data.mErrorInRun)
                return;

            if (mTarget.equals("resource")) {
                data.mResource = mValue;
            }
        }
    }

    class ResetAllTokensAction extends TestAction {

        public ResetAllTokensAction() {
        }

        public ResetAllTokensAction(String name, String target, String value) {
            super(name, target, value);
        }

        public void perform(final TestData data) throws InterruptedException {
            if (data.mErrorInRun)
                return;

            if (data == null || data.mContext == null) {
                data.mErrorMessage = "Context is not initialized";
                data.mErrorInRun = true;
            } else {
                data.mContext.getCache().removeAll();
            }
        }
    }

    class AcquireTokenAction extends TestAction {
        private static final long CONTEXT_REQUEST_TIME_OUT = 30000;

        public AcquireTokenAction() {
        }

        public AcquireTokenAction(String name, String target, String value) {
            super(name, target, value);
        }

        public void perform(final TestData data) throws InterruptedException {
            if (data.mErrorInRun)
                return;

            if (data == null || data.mContext == null) {
                data.mErrorMessage = "Context is not initialized";
                data.mErrorInRun = true;
            } else {
                // related api call needs to block until it finishes
                final CountDownLatch signal = new CountDownLatch(1);
                data.mContext.acquireToken(mActivity, data.mResource, data.mClientId,
                        data.mRedirect, data.mLoginHint, PromptBehavior.valueOf(data.mPrompt),
                        data.mExtraQuery, new AuthenticationCallback<AuthenticationResult>() {

                            @Override
                            public void onSuccess(AuthenticationResult result) {
                                data.mResult = result;
                                signal.countDown();
                            }

                            @Override
                            public void onError(Exception exc) {
                                data.mErrorInRun = true;
                                data.mErrorMessage = exc.getMessage();
                                signal.countDown();
                            }
                        });

                signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
            }
        }
    }

    class TestData {
        AuthenticationContext mContext;

        String mAuthority;

        String mResource;

        String mClientId;

        String mRedirect;

        String mLoginHint;

        String mExtraQuery;

        String mPrompt;

        CountDownLatch mSignal;

        AuthenticationResult mResult;

        boolean mErrorInRun;

        String mErrorMessage;
    }

    class TestAction {
        protected String mName;

        protected String mTarget;

        protected String mValue;

        public TestAction() {
            mName = "N/A";
            mValue = "N/A";
            mTarget = "N/A";
        }

        public TestAction(String name, String target, String val) {
            mName = name;
            mTarget = target;
            mValue = val;
        }

        public void perform(TestData data) throws InterruptedException {

        }

    }

    /**
     * Simple get request for test
     * 
     * @author omercan
     */
    class TestSubmitTask extends AsyncTask<Void, Void, Void> {

        private String mUrl;

        private String mData;

        public TestSubmitTask(String url, String data) {
            mUrl = url;
            mData = data;
        }

        @Override
        protected Void doInBackground(Void... empty) {

            WebRequestHandler request = new WebRequestHandler();
            HashMap<String, String> headers = new HashMap<String, String>();

            headers.put("Accept", "application/json");
            HttpWebResponse response = null;
            try {
                response = request.sendPost(new URL(mUrl), headers,
                        mData.getBytes("UTF-8"), "application/json");
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Log.v(TAG, "Send data status:" + response.getStatusCode());
            return null;

        }
    }

}
