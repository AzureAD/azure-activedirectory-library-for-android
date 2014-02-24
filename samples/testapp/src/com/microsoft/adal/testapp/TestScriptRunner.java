
package com.microsoft.adal.testapp;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
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

    private static final String TARGET_URL = "https://adal.azurewebsites.net/";

    private Gson gson = new Gson();

    private Activity mActivity;

    public TestScriptRunner(Activity activity) {
        mActivity = activity;
    }

    public void runRemoteScript() {
        new RetrieveTask(TARGET_URL + "WebRequest/GetTestScript", new ResponseListener() {

            @Override
            public void onResponse(TestScriptInfo script) {

                Log.v(TAG, "received test script");
                TestResultInfo[] results = script.run();
                Log.v(TAG, "executed test script");
                postResults(results);
            }
        });
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
        new TestSubmitTask(TARGET_URL + "api/Values", gson.toJson(results));
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
                        results[i].statusOk = false;
                        results[i].testMsg = e.getMessage();
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

        public TestResultInfo run() throws InterruptedException {
            Log.v(TAG, "running test case:" + testName);
            TestResultInfo testResult = new TestResultInfo(testName);
            TestData testRunData = new TestData();
            for (int i = 0; i < testActions.length; i++) {
                TestAction action = getAction(testActions[i].name, testActions[i].target,
                        testActions[i].targetValue);
                Log.v(TAG, "running test case:" + testName + " action:" + action.name + " target:"
                        + action.target);

                action.perform(testRunData);
                // call api directly if possible or do click actions on UI
                if (testRunData.mErrorInRun) {
                    testResult.statusOk = false;
                    testResult.testMsg = action.target + " failed";
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
        String testName;

        boolean statusOk = true;

        String testMsg;

        public TestResultInfo() {
        }

        public TestResultInfo(String name) {
            testName = name;
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
        } else if (name.equals("Wait")) {
            return new WaitAction(name, target, value);
        } else if (name.equals("Verify")) {
            return new VerifyAction(name, target, value);
        }

        return null;
    }

    /**
     * target can be success or fail that is set at textbox
     * 
     * @author omercan
     */
    class VerifyAction extends TestAction {
        public VerifyAction() {
        }

        public VerifyAction(String name, String target, String value) {
            super(name, target, value);
        }

        public void perform(final TestData testRunData) throws InterruptedException {
            if (testRunData.mErrorInRun)
                return;

            // define factory method and subtypes for asserts if necessary
            if (target.equals("HasAccessToken")) {
                testRunData.mErrorInRun = testRunData.mResult == null
                        || testRunData.mResult.getAccessToken().isEmpty();
            } else if (target.equals("SameAsReferenceToken")) {
                testRunData.mErrorInRun = testRunData.mResult == null
                        || testRunData.mResult.getAccessToken().equals(
                                testRunData.referenceResult.getAccessToken());
            } else if (target.equals("AccessTokenContains")) {
                testRunData.mErrorInRun = testRunData.mResult == null
                        || testRunData.mResult.getAccessToken().contains(targetValue);
            }
        }
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

            if (target.equals("resource")) {
                data.mResource = targetValue;
            } else if (target.equals("clientid")) {
                data.mClientId = targetValue;
            } else if (target.equals("authority")) {
                data.mAuthority = targetValue;
            } else if (target.equals("redirect")) {
                data.mRedirect = targetValue;
            }
        }
    }

    class WaitAction extends TestAction {
        public WaitAction() {
        }

        public WaitAction(String name, String target, String value) {
            super(name, target, value);
        }

        public void perform(final TestData data) throws InterruptedException {
            if (data.mErrorInRun)
                return;
            try {
                int sleeptime = Integer.parseInt(targetValue);

                Thread.sleep(sleeptime);
            } catch (Exception ex) {
                data.mErrorInRun = true;
                data.mErrorMessage = ex.getMessage();
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

        AuthenticationResult referenceResult;

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
        protected String name;

        protected String target;

        protected String targetValue;

        public TestAction() {
            name = "N/A";
            targetValue = "N/A";
            target = "N/A";
        }

        public TestAction(String action_name, String action_target, String action_val) {
            name = action_name;
            target = action_target;
            targetValue = action_val;
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
                response = request.sendPost(new URL(mUrl), headers, mData.getBytes("UTF-8"),
                        "application/json");
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

    interface ResponseListener {
        void onResponse(TestScriptInfo script);

    }

    class RetrieveTask extends AsyncTask<Void, Void, TestScriptInfo> {

        private String mUrl;

        private ResponseListener mCallback;

        public RetrieveTask(String url, ResponseListener callback) {
            mUrl = url;
            mCallback = callback;
        }

        @Override
        protected TestScriptInfo doInBackground(Void... empty) {

            WebRequestHandler request = new WebRequestHandler();
            HashMap<String, String> headers = new HashMap<String, String>();

            headers.put("Accept", "application/json");
            HttpWebResponse response = null;
            try {
                response = request.sendGet(new URL(mUrl), headers);
                String body = new String(response.getBody(), "UTF-8");
                TestScriptInfo scriptInfo = gson.fromJson(body, TestScriptInfo.class);
                return scriptInfo;
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

        @Override
        protected void onPostExecute(TestScriptInfo result) {
            super.onPostExecute(result);
            mCallback.onResponse(result);
        }

    }
}
