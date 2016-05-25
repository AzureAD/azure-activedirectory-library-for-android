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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.HttpWebResponse;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.WebRequestHandler;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class TestScriptRunner {
    static final String TAG = "TestScriptRunner";

    private static final String TARGET_URL = "https://adal.azurewebsites.net/";

    private Gson gson = new Gson();

    private final MainActivity mActivity;

    private Handler mHandler;

    private static ExecutorService sThreadExecutor = Executors.newSingleThreadExecutor();

    public TestScriptRunner(MainActivity activity) {
        mActivity = activity;
        mHandler = new Handler(activity.getMainLooper());
    }

    public MainActivity getActivity() {
        return mActivity;
    }

    public void runRemoteScript() {
        new RetrieveTask(TARGET_URL + "WebRequest/GetTestScript", new ResponseListener() {

            @Override
            public void onResponse(final ScriptInfo script) {

                sThreadExecutor.submit(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        // this will be called at UI thread and test related API
                        // calls
                        // will be send from UI thread
                        Log.v(TAG, "received test script");
                        script.setActivity(mActivity);
                        script.setHandler(mHandler);
                        TestResult[] results = script.run();
                        Log.v(TAG, "executed test script");
                        postResults(results);
                    }
                });
            }
        }).execute();
    }

    public void processTestScript(String script) {
        // 1- json decode
        // 2- generate TestCommand object
        // 3- run test command
        Log.v(TAG, "received test script");
        ScriptInfo cacheItem = gson.fromJson(script, ScriptInfo.class);
        TestResult[] results = cacheItem.run();
        Log.v(TAG, "executed test script");
        postResults(results);
    }

    private void postResults(final TestResult[] results) {
        // TODO Auto-generated method stub
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                new TestSubmitTask(TARGET_URL + "api/Values", gson.toJson(results)).execute();
            }
        });
    }

    public String makeScript() {
        // to test it
        ScriptInfo script = new ScriptInfo();
        TestCase test1 = new TestCase("checkScript");
        TestAction action1 = new TestAction("Enter", "resource", "resource-test");
        test1.testActions = new TestAction[1];
        test1.testActions[0] = action1;
        script.testCases = new TestCase[1];
        script.testCases[0] = test1;

        String checkText = gson.toJson(script);
        return checkText;
    }

    /**
     * wrapper to have results as key for the array at webapi
     */
    class TestResultInfo {
        TestResult[] results;
    }

    class ScriptInfo {
        TestCase[] testCases;

        TestResult[] results;

        private transient MainActivity mActivity;

        private transient Handler mHandler;

        public void setActivity(MainActivity activity) {
            mActivity = activity;
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        /**
         * Run all of the test cases
         */
        public TestResult[] run() {
            // run each test case and collect result
            if (testCases != null) {

                TestResult[] results = new TestResult[testCases.length];

                for (int i = 0; i < testCases.length; i++) {

                    try {
                        testCases[i].setActivity(this.mActivity);
                        testCases[i].setHandler(this.mHandler);
                        results[i] = testCases[i].run();
                    } catch (Exception e) {
                        Log.e(TestScriptRunner.TAG, "error:", e);
                        results[i] = new TestResult(testCases[i].testName);
                        results[i].statusOk = false;
                        results[i].testMsg = e.getMessage();
                    }
                }

                return results;
            }
            return null;
        }
    }

    class TestCase {

        private transient MainActivity mActivity;

        private transient Handler mHandler;

        public TestCase() {
            testName = null;
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        public void setActivity(MainActivity activity) {
            this.mActivity = activity;
        }

        public TestCase(String name) {
            testName = name;
        }

        String testName;

        TestAction[] testActions;

        public TestResult run() throws InterruptedException {
            Log.v(TestScriptRunner.TAG, "running test case:" + testName);
            TestResult testResult = new TestResult(testName);
            TestData testRunData = new TestData();

            for (int i = 0; i < testActions.length; i++) {

                TestAction command = createAction(testActions[i].name, testActions[i].target,
                        testActions[i].targetValue);

                if (command != null) {
                    Log.v(TestScriptRunner.TAG, "running test case:" + testName + " action:"
                            + command.name + " target:" + command.target);

                    command.execute(testRunData);
                    // call api directly if possible or do click actions on UI
                    if (testRunData.mErrorInRun) {
                        testResult.statusOk = false;
                        testResult.testMsg = command.target + " failed";
                        break;
                    }
                } else {
                    testRunData.mErrorInRun = true;
                    testResult.statusOk = false;
                    testResult.testMsg = testActions[i].target + " failed to parse to action";
                }
            }

            return testResult;
        }

        /**
         * Get action object based on name
         * 
         * @param name
         * @param target
         * @param value
         * @return
         */
        private TestAction createAction(String name, String target, String value) {
            // add different actions related to the API here
            Log.d(TestScriptRunner.TAG, "getAction for " + name + " target:" + target + " value:"
                    + value);
            if (name == null || name.isEmpty())
                return null;

            // Explicitly passing outer class activity to inner since access
            // request from inner to outer will fail
            if (name.equals("InitContext")) {
                return new InitContextAction(this.mActivity, this.mHandler, name, target, value);
            } else if (name.equals("AcquireToken")) {
                return new AcquireTokenAction(this.mActivity, this.mHandler, name, target, value);
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
    }

    /**
     * rsult to submit back
     */
    class TestResult {
        String testName;

        boolean statusOk = true;

        String testMsg;

        public TestResult() {
        }

        public TestResult(String name) {
            testName = name;
        }
    }

    /**
     * target can be success or fail that is set at textbox
     */
    class VerifyAction extends TestAction {
        public VerifyAction() {
        }

        public VerifyAction(final String name, final String target, final String value) {
            super(name, target, value);
        }

        public void execute(final TestData testRunData) throws InterruptedException {
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

        public EnterAction(final String name, final String target, final String value) {
            super(name, target, value);
        }

        public void execute(final TestData data) throws InterruptedException {
            if (data.mErrorInRun)
                return;

            if (target.equals("authority")) {
                data.mAuthority = targetValue;
            } else if (target.equals("resource")) {
                data.mResource = targetValue;
            } else if (target.equals("clientid")) {
                data.mClientId = targetValue;
            } else if (target.equals("redirect")) {
                data.mRedirect = targetValue;
            }
        }
    }

    class WaitAction extends TestAction {
        public WaitAction() {
        }

        public WaitAction(final String name, final String target, final String value) {
            super(name, target, value);
        }

        public void execute(final TestData data) throws InterruptedException {
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

        public ResetAllTokensAction(final String name, final String target, final String value) {
            super(name, target, value);
        }

        public void execute(final TestData data) throws InterruptedException {
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

    class InitContextAction extends TestAction {
        private transient MainActivity mContextActivity;

        transient Handler mHandler;

        public InitContextAction(MainActivity activity, Handler handler, String name, String target,
                String value) {
            super(name, target, value);
            mContextActivity = activity;
            mHandler = handler;
        }

        public InitContextAction(final String name, final String target, final String value) {
            super(name, target, value);
        }

        public void execute(final TestData data) throws InterruptedException {
            if (data.mErrorInRun)
                return;

            try {
                data.mContext = new AuthenticationContext(mContextActivity, data.mAuthority, false);
                data.mContext.getCache().removeAll();
                mContextActivity.setContextForScriptRun(data.mContext);
            } catch (Exception e) {
                data.mErrorInRun = true;
                data.mErrorMessage = e.getMessage();
            }

        }
    }

    class AcquireTokenAction extends TestAction {
        private static final long CONTEXT_REQUEST_TIME_OUT = 60000;

        private transient Activity mContextActivity;

        private transient Handler mHandler;

        public AcquireTokenAction(final Activity activity, final Handler handler,
                final String name, final String target, final String value) {
            super(name, target, value);

            this.mContextActivity = activity;
            this.mHandler = handler;
        }

        public void execute(final TestData data) throws InterruptedException {
            if (data.mErrorInRun)
                return;

            if (data == null || data.mContext == null) {
                data.mErrorMessage = "Context is not initialized";
                data.mErrorInRun = true;
            } else {
                // related api call needs to block until it finishes
                final CountDownLatch signal = new CountDownLatch(1);
                Log.v(TAG, "acquiretoken request");
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // Call API from handler
                        data.mContext.acquireToken(mContextActivity, data.mResource,
                                data.mClientId, data.mRedirect, data.mLoginHint,
                                data.getPromptBehavior(), data.mExtraQuery,
                                new AuthenticationCallback<AuthenticationResult>() {

                                    @Override
                                    public void onSuccess(AuthenticationResult result) {
                                        Log.v(TAG,
                                                "AcquireToken is success:"
                                                        + result.getAccessToken());
                                        data.mResult = result;
                                        signal.countDown();
                                    }

                                    @Override
                                    public void onError(Exception exc) {
                                        Log.v(TAG, "Error in AcquireToken");

                                        data.mErrorInRun = true;
                                        data.mErrorMessage = exc.getMessage();
                                        signal.countDown();
                                    }
                                });

                    }
                });

                signal.await(CONTEXT_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
                Log.v(TAG, "acquiretoken request is processed");
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

        public PromptBehavior getPromptBehavior() {
            if (mPrompt != null && !mPrompt.isEmpty()) {
                return PromptBehavior.valueOf(mPrompt);
            }

            return null;
        }
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

        public void execute(TestData data) throws InterruptedException {

        }
    }

    /**
     * Simple get request for test
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
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Log.v(TAG, "Send data status:" + response.getStatusCode());
            return null;
        }
    }

    interface ResponseListener {
        void onResponse(ScriptInfo script);

    }

    class RetrieveTask extends AsyncTask<Void, Void, ScriptInfo> {

        private String mUrl;

        private ResponseListener mCallback;

        public RetrieveTask(String url, ResponseListener callback) {
            mUrl = url;
            mCallback = callback;
        }

        @Override
        protected ScriptInfo doInBackground(Void... empty) {

            WebRequestHandler request = new WebRequestHandler();
            HashMap<String, String> headers = new HashMap<String, String>();

            headers.put("Accept", "application/json");
            HttpWebResponse response = null;
            try {
                response = request.sendGet(new URL(mUrl), headers);
                String body = response.getBody();
                Log.v(TAG, "testScript:" + body);
                ScriptInfo scriptInfo = gson.fromJson(body, ScriptInfo.class);
                return scriptInfo;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Log.v(TAG, "Send data status:" + response.getStatusCode());
            return null;
        }

        @Override
        protected void onPostExecute(ScriptInfo result) {
            super.onPostExecute(result);
            mCallback.onResponse(result);
        }

    }
}
