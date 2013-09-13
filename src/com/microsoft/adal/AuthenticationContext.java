/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import org.json.JSONObject;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/*
 *TODO: error messages define
 *TODO: serialization to restore the context onresume inside the client's app so that user does not need to authenticate again
 *TODO: Browser flow add
 *TODO: 
 */

/**
 * ADAL context to get access token, refresh token
 * 
 * @author omercan
 */
public class AuthenticationContext {

    /**
     * TAG to check messages
     */
    private final static String TAG = "AuthenticationContext";
    static final String BROWSER_REQUEST_MESSAGE = "com.microsoft.adal:BrowserRequestMessage";
    static final String BROWSER_RESPONSE_ERROR_REQUEST = "com.microsoft.adal:BrowserErrorRequestInfo";
    static final String BROWSER_RESPONSE_ERROR_CODE = "com.microsoft.adal:BrowserErrorCode";
    static final String BROWSER_RESPONSE_ERROR_MESSAGE = "com.microsoft.adal:BrowserErrorCode";
    static final String BROWSER_RESPONSE_FINAL_URL = "com.microsoft.adal:BrowserFinalUrl";
    static final int GET_AUTHORIZATION = 1;

    private String mAuthority;
    private String mClientId;
    private String mRedirectUri;
    private String mLoginHint;
    private String mBroadRefreshToken;
    private transient Context mContext;
    private transient ActivityDelegate mActivityDelegate;
    private transient AuthenticationCallback mExternalCallback;
    static AuthenticationRequest pendingRequest;

    private static AuthenticationContext sInstance;

    /**
     * Delegate to use for starting browser flow from activity
     */
    public interface ActivityDelegate {
        public void startActivityForResult(Intent intent, int requestCode);

        public Activity getActivityContext();
    }

    /**
     * Response listener for web requests
     */
    public interface OnResponseListener {

        /**
         * Called when response is received
         * 
         * @param values on success, contains the values returned by the dialog
         * @param error on an error, contains an exception describing the error
         */
        void onComplete(HashMap<String, String> response);
    }

    /**
     * Constructs context to use with known authority to get the token
     * 
     * @param authority
     * @param clientId
     * @param redirectUri
     * @param loginHint
     */
    public AuthenticationContext(String authority, String clientId, String redirectUri,
            String loginHint)
    {
        mAuthority = authority;
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mLoginHint = loginHint;
    }

    public void getToken(Activity activity, String resource, UUID correlationID,
            AuthenticationCallback callback) {

        if (callback == null)
            throw new IllegalArgumentException("listener is null");

        if (activity == null)
            throw new IllegalArgumentException("context is null");

        
        if (pendingRequest != null) {
            throw new IllegalArgumentException("Attempted to get token while a request is pending.");
        }

        // Check authority url
        ExtractUrl();

        // If user has enabled validation, it will call the discovery service to
        // verify the instance
        ValidateAuthority();

        final AuthenticationRequest request = new AuthenticationRequest(this, resource);

        // Check cached authorization object
        final AuthenticationResult cachedResult = getCachedResult(request
                .getCacheKey());

        if (cachedResult != null) {
            if (!cachedResult.isExpired()) {
                callback.onCompleted(cachedResult);
            } else if (cachedResult.isRefreshable()) {
                // refreshToken will try to refresh. If it fails, it removes
                // authorization object and returns error
                refreshToken(cachedResult, resource, callback);
            }
        } else {
            mExternalCallback = callback;
            setTokenActivityDelegate(activity);
            pendingRequest = request;
            mContext = activity.getApplicationContext();
            startLoginActivity(request);
        }
    }

    private void setTokenActivityDelegate(Activity activity)
    {
        final Activity callingActivity = activity;
        mActivityDelegate = new ActivityDelegate() {
            @Override
            public void startActivityForResult(Intent intent, int requestCode) {
                Log.d(TAG, "Delegate calling startActivityForResult");
                callingActivity.startActivityForResult(intent, requestCode);
            }

            @Override
            public Activity getActivityContext() {
                return callingActivity;
            }
        };
    }

    ActivityDelegate getTokenActivityDelegate() {
        return mActivityDelegate;
    }

    private boolean startLoginActivity(AuthenticationRequest request) {
        Intent intent = getLoginActivityIntent(request);

        if (!resolveIntent(intent)) {
            return false;
        }

        try {
            getTokenActivityDelegate().startActivityForResult(intent,
                    AuthenticationConstants.UIRequest.BROWSER_FLOW);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "Activity login not found");
            return false;
        }

        return true;
    }

    /**
     * Resolve activity
     * @param intent
     * @return
     */
    private boolean resolveIntent(Intent intent) {
        
        ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return false;
        }
        return true;
    }

    private Intent getLoginActivityIntent(AuthenticationRequest request) {
        Intent intent = new Intent();
        intent.setClass(mContext, LoginActivity.class);
        intent.putExtra(AuthenticationContext.BROWSER_REQUEST_MESSAGE, request);
        return intent;
    }

    /**
     * Call from your onActivityResult method inside your activity that started
     * token request
     * 
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AuthenticationConstants.UIRequest.BROWSER_FLOW)
        {
            Bundle extras = data.getExtras();
            if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL)
            {
                // Clean any pending requests so that new browser flow can start
                pendingRequest = null;
                mExternalCallback.onCancelled();
            } else if (resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR)
            {
                // Clean any pending requests so that new browser flow can start
                pendingRequest = null;
                AuthenticationRequest errRequest = (AuthenticationRequest) extras
                        .getSerializable(AuthenticationContext.BROWSER_RESPONSE_ERROR_REQUEST);
                String errCode = extras.getString(BROWSER_RESPONSE_ERROR_CODE);
                String errMessage = extras.getString(BROWSER_RESPONSE_ERROR_MESSAGE);
                mExternalCallback.onError(new AuthException(errRequest, errCode, errMessage));
            } else
            {
                String endingUrl = extras.getString(BROWSER_RESPONSE_FINAL_URL);
                if (endingUrl.isEmpty())
                {
                    Log.d(TAG, "ending url is empty");
                    mExternalCallback.onError(new IllegalArgumentException("Final url is empty"));
                }

                Log.d(TAG, "Process this url");
                processUIResponse(endingUrl);
                // Clean any pending requests so that new browser flow can start
                pendingRequest = null;         
            }

        }
    }

    /**
     * TODO: cleanup
     * 
     * @param endUrl
     */
    private void processUIResponse(String endUrl) {
        // Success
        Uri response = Uri.parse(endUrl);
        HashMap<String, String> parameters = UriExtensions
                .getFragmentParameters(response);

        if (parameters == null || parameters.isEmpty())
            parameters = UriExtensions.getQueryParameters(response);

        // TODO: No encoded state
        String encodedState = parameters.get("state");
        String state = AuthenticationRequest.decodeProtocolState(encodedState);

        if (null != state && !state.isEmpty()) {
            // We have encoded state, crack it open
            Uri stateUri = Uri.parse("http://state/path?" + state);
            String authorizationUri = stateUri.getQueryParameter("a");
            String resource = stateUri.getQueryParameter("r");
            String scope = stateUri.getQueryParameter("s");

            if (null != authorizationUri && !authorizationUri.isEmpty()
                    && null != resource && !resource.isEmpty()
                    && resource.equalsIgnoreCase(pendingRequest.getResource())) {

                // ToDo: if token, ask to token endpoint and post the message

                // Same authority
                final AuthenticationRequest request = new AuthenticationRequest(this, resource);

                // another browserflow can start now if requested
                pendingRequest = null;

                AuthenticationResult result = processUIResponseParams(
                        parameters, request);

                if (result.getStatus() == com.microsoft.adal.AuthenticationResult.AuthenticationStatus.Succeeded) {
                    if (!result.getCode().isEmpty()) {
                        // Need to get exchange code to get token
                        HashMap<String, String> tokenRequestMessage = buildTokenRequestMessage(
                                result);

                        sendRequest(request.getTokenEndpoint(),
                                tokenRequestMessage, new OnResponseListener() {
                                    @Override
                                    public void onComplete(
                                            HashMap<String, String> response) {
                                        AuthenticationResult result = processUIResponseParams(
                                                response, request);

                                        if (result.getStatus() == com.microsoft.adal.AuthenticationResult.AuthenticationStatus.Succeeded) {
                                            setCachedResult(request
                                                    .getCacheKey(), result);
                                            mExternalCallback.onCompleted(result);
                                        } else {
                                            // did not get token
                                            mExternalCallback
                                                    .onError(new AuthException(request, result
                                                            .getErrorCode(), result
                                                            .getErrorDescription()));
                                        }
                                    }
                                });
                    }
                    else
                    {
                        // We have token directly with implicit flow

                    }

                }
            } else {

                mExternalCallback.onError(new AzureException(
                        AuthenticationConstants.AUTH_FAILED_BAD_STATE));
            }
        } else {
            // The response from the server had no state
            // information
            mExternalCallback.onError(new AzureException(
                    AuthenticationConstants.AUTH_FAILED_NO_STATE));
        }
    }

    /**
     * Sends a request with the specified parameters to the given endpoint TODO:
     * add correlationid headers TODO: abstract sendrequest to set headers,
     * method type, data
     */
    private static void sendRequest(final String endpoint,
            HashMap<String, String> requestData,
            final OnResponseListener callback) {
        HashMap<String, String> response = null;

        try {
            String body = HashMapExtensions.URLFormEncode(requestData);
            HttpWebRequest webRequest = new HttpWebRequest(new URL(endpoint));

            webRequest.getRequestHeaders().put("Accept", "application/json");

            webRequest.sendAsync(
                    body.getBytes(AuthenticationConstants.ENCODING_UTF8),
                    "application/x-www-form-urlencoded",
                    new HttpWebRequestCallback() {
                        @Override
                        public void onComplete(Exception exception,
                                HttpWebResponse webResponse) {
                            HashMap<String, String> response = new HashMap<String, String>();
                            // Response should have Oauth2 token
                            if (exception != null) {
                                Log.e(TAG, ExceptionExtensions
                                        .getExceptionMessage(exception));

                                response.put(
                                        AuthenticationConstants.OAuth2.ERROR,
                                        AuthenticationConstants.AUTH_FAILED);
                                response.put(
                                        AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                                        ExceptionExtensions
                                                .getExceptionMessage(exception));
                            } else if (webResponse.getStatusCode() <= 400) {
                                try {
                                    JSONObject jsonObject = new JSONObject(
                                            new String(webResponse.getBody()));

                                    @SuppressWarnings("unchecked")
                                    Iterator<String> i = jsonObject.keys();

                                    while (i.hasNext()) {
                                        String key = i.next();

                                        response.put(key,
                                                jsonObject.getString(key));
                                    }
                                } catch (Exception ex) {
                                    // There is no recovery possible here, so
                                    // catch the generic Exception
                                    Log.e(TAG, ExceptionExtensions
                                            .getExceptionMessage(ex));

                                    response.put(
                                            AuthenticationConstants.OAuth2.ERROR,
                                            AuthenticationConstants.AUTH_FAILED);
                                    response.put(
                                            AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                                            ExceptionExtensions
                                                    .getExceptionMessage(ex));
                                }
                            } else {
                                response.put(
                                        AuthenticationConstants.OAuth2.ERROR,
                                        AuthenticationConstants.AUTH_FAILED);
                                response.put(
                                        AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                                        new String(webResponse.getBody()));
                            }

                            // Run the callback
                            Log.d(TAG, "Callback complete call");
                            callback.onComplete(response);
                        }
                    });
        } catch (Exception ex) {
            // There is no recovery possible here, so catch the generic
            // Exception
            Log.e(TAG, ExceptionExtensions.getExceptionMessage(ex));

            response = new HashMap<String, String>();
            response.put(AuthenticationConstants.OAuth2.ERROR,
                    AuthenticationConstants.AUTH_FAILED);
            response.put(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION,
                    ExceptionExtensions.getExceptionMessage(ex));
        }

        Log.d(TAG, "sendRequest at the end");
        if (response != null) {
            callback.onComplete(response);
        }
    }

    private static AuthenticationResult processUIResponseParams(
            HashMap<String, String> response, AuthenticationRequest request) {

        AuthenticationResult result = new AuthenticationResult(request);

        if (response.containsKey(AuthenticationConstants.OAuth2.ERROR)) {
            // Error response from the server
            // TODO: Should we kill the authorization object?
            result = new AuthenticationResult(
                    response.get(AuthenticationConstants.OAuth2.ERROR),
                    response.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION));
        } else if (response.containsKey(AuthenticationConstants.OAuth2.CODE)) {
            // Code response
            Calendar expires = new GregorianCalendar();
            expires.add(Calendar.SECOND, 300);
            result.setAccessTokenType(null);
            result.setAccessTokenType(null);
            result.setCode(response
                    .get(AuthenticationConstants.OAuth2.CODE));
            result.setExpires(expires.getTime());
            result.setRefreshToken(null);
        } else if (response
                .containsKey(AuthenticationConstants.OAuth2.ACCESS_TOKEN)) {
            // Token response
            String expires_in = response.get("expires_in");
            Calendar expires = new GregorianCalendar();

            // Compute token expiration
            expires.add(
                    Calendar.SECOND,
                    expires_in == null || expires_in.isEmpty() ? AuthenticationConstants.DEFAULT_EXPIRATION_TIME_SEC
                            : Integer.parseInt(expires_in));

            result.setAccessToken(response
                    .get(AuthenticationConstants.OAuth2.ACCESS_TOKEN));
            result.setAccessTokenType(response
                    .get(AuthenticationConstants.OAuth2.TOKEN_TYPE));
            result.setCode(null);
            result.setExpires(expires.getTime());

            if (response
                    .containsKey(AuthenticationConstants.OAuth2.REFRESH_TOKEN)) {
                result.setRefreshToken(response
                        .get(AuthenticationConstants.OAuth2.REFRESH_TOKEN));

                if (response.containsKey(AuthenticationConstants.AAD.RESOURCE))
                {
                    result.setBroadRefreshToken(true);
                }
            }

        } else {

            result = new AuthenticationResult(
                    AuthenticationConstants.AUTH_FAILED,
                    AuthenticationConstants.AUTH_FAILED_SERVER_ERROR);
        }

        return result;
    }

    /**
     * Build token request message which uses code to get token
     * 
     * @param result Authentication result which has code
     * @return Hashmap request params
     */
    private HashMap<String, String> buildTokenRequestMessage(
            AuthenticationResult result) {
        HashMap<String, String> reqParameters = new HashMap<String, String>();

        reqParameters.put(AuthenticationConstants.OAuth2.GRANT_TYPE,
                AuthenticationConstants.OAuth2.AUTHORIZATION_CODE);

        reqParameters.put(AuthenticationConstants.OAuth2.CODE, result
                .getCode());

        reqParameters.put(AuthenticationConstants.OAuth2.CLIENT_ID,
                result.getClientId());

        reqParameters.put(AuthenticationConstants.OAuth2.REDIRECT_URI,
                result.getRedirectUri());

        return reqParameters;
    }

    /**
     * Callback to use for web ui login completed
     */
    interface WebLoginCallback {
        /**
         * Method to call if the operation finishes successfully
         * 
         * @param url The final login URL
         * @param e An exception
         */
        void onCompleted(String url, Exception exception);

        /*
         * User or UI cancelled login
         */
        void onCancelled();
    }

    private void refreshToken(AuthenticationResult cachedResult, String resource,
            AuthenticationCallback callback) {
        // TODO Auto-generated method stub

    }

    private AuthenticationResult getCachedResult(String cacheKey) {
        if (getSettings().getEnableTokenCaching())
        {
            return getSettings().getCache().getResult(cacheKey);
        }

        return null;
    }

    private void setCachedResult(String cacheKey, AuthenticationResult result) {
        if (getSettings().getEnableTokenCaching())
        {
            getSettings().getCache().putResult(cacheKey, result);
        }

    }

    private void ValidateAuthority()
    {
        if (getSettings().getValidateAuthority())
        {
            if (!getSettings().getDiscovery().IsValidAuthority(mAuthority))
            {
                throw new IllegalArgumentException("Authority is not valid");
            }
        }
    }

    private void ExtractUrl()
    {
        // Authorization server URL is like
        // "https://login.windows.net/somewhere.onmicrosoft.com"
        // - must not be empty
        // - must be absolute
        // - must not have query or fragment
        // - must be https
        if (mAuthority == null || mAuthority.isEmpty())
            throw new IllegalArgumentException("authorizationServer");

        Uri uri = Uri.parse(mAuthority);

        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("authorizationServer");
        }
        if (!uri.getScheme().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("authorizationServer");
        }
        if (uri.getFragment() != null || uri.getQuery() != null) {
            throw new IllegalArgumentException("authorizationServer has query or fragments");
        }

        // Normalize authority url to remove extra url parts
        int thirdSlash = mAuthority.indexOf("/", 8); // exclude starting
                                                     // https:// or http://
        if (thirdSlash >= 0)
        {
            if (thirdSlash != (mAuthority.length() - 1))
            {
                // Extract url
                int fourthSlash = mAuthority.indexOf("/", thirdSlash + 1);
                if (fourthSlash > thirdSlash + 1)
                {
                    mAuthority.substring(0, fourthSlash);
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("Authority url");
        }
    }

    public static AuthenticationSettings getSettings() {
        return AuthenticationSettings.getInstance();
    }

    public String getAuthority() {
        return mAuthority;
    }

    public String getClientId() {
        return mClientId;
    }

    public String getRedirectUri() {
        return mRedirectUri;
    }

}
