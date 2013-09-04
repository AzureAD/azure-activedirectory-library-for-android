/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;


import java.util.UUID;

import com.facebook.AuthorizationClient;
import com.facebook.FacebookException;
import com.facebook.LoginActivity;
import com.facebook.Session.AuthorizationRequest;
import com.facebook.Session.StartActivityDelegate;
import com.microsoft.adal.AuthenticationRequest.ActivityDelegate;

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
    
    /**
     * Delegate to use for starting browser flow from activity
     *
     */
    public interface ActivityDelegate {
        public void startActivityForResult(Intent intent, int requestCode);

        public Activity getActivityContext();
    }
    
    /**
     * Constructs context to use with known authority to get the token
     * @param authority
     * @param clientId
     * @param redirectUri
     * @param loginHint
     */
    public AuthenticationContext(String authority, String clientId, String redirectUri, String loginHint)
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
            
            startLoginActivity(request);
        }
    }
 
    private void setTokenActivityDelegate(Activity activity)
    {
        final Activity callingActivity = activity;
        mActivityDelegate = new ActivityDelegate() {
            @Override
            public void startActivityForResult(Intent intent, int requestCode) {
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
            getTokenActivityDelegate().startActivityForResult(intent, AuthenticationConstants.UIRequest.BROWSER_FLOW);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "Activity login not found");
            return false;
        }

        return true;
    }

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
     * Call from your onActivityResult method inside your activity that started token request 
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == AuthenticationConstants.UIRequest.BROWSER_FLOW)
        {
            Bundle extras = data.getExtras();
            if(resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL)
            {
                mExternalCallback.onCancelled();
            }else if(resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR)
            {
                
                AuthenticationRequest errRequest = (AuthenticationRequest) extras.getSerializable(AuthenticationContext.BROWSER_RESPONSE_ERROR_REQUEST);
                String errCode = extras.getString(BROWSER_RESPONSE_ERROR_CODE);
                String errMessage = extras.getString(BROWSER_RESPONSE_ERROR_MESSAGE);
                mExternalCallback.onError(new AuthException(errRequest, errCode, errMessage));
            }else
            {
                String endingUrl = 
            }
            
        }
    }
    
    
    /**
     * Callback to use for web ui login completed
     */
    interface WebLoginCallback {
        /**
         * Method to call if the operation finishes successfully
         * 
         * @param url
         *            The final login URL
         * @param e
         *            An exception
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
        if(getSettings().getEnableTokenCaching())
        {
            return getSettings().getCache().getResult(cacheKey);            
        }
        
        return null;
    }

    private void ValidateAuthority()
    {
        if(getSettings().getValidateAuthority())
        {
            if(!getSettings().getDiscovery().IsValidAuthority(mAuthority))
            {
                throw new IllegalArgumentException("Authority is not valid");
            }
        }
    }
    
    private void ExtractUrl()
    {
     // Authorization server URL is like "https://login.windows.net/somewhere.onmicrosoft.com"
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
        int thirdSlash = mAuthority.indexOf("/", 8); // exclude starting https:// or http://
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
        
        throw new IllegalArgumentException("Authority url");
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
