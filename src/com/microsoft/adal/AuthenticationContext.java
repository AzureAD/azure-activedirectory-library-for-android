/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;


import java.util.UUID;

import com.microsoft.protection.authentication.AuthenticationSettings;


import android.content.Context;
import android.net.Uri;

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

    
    private String mAuthority;
    private String mClientId;
    private String mRedirectUri;
    private String mLoginHint;
    private String mBroadRefreshToken;
      
    
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
    
    public void getToken(Context context, String resource, UUID correlationID,
            AuthenticationCallback callback) {

        if (callback == null)
            throw new IllegalArgumentException("listener is null");

        if (context == null)
            throw new IllegalArgumentException("context is null");
        
        // Check authority url
        ExtractUrl();
        
        // If user has enabled validation, it will call the discovery service to verify the instance
        ValidateAuthority();
        
        
        
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
    
    public void getTokenNoUI(TokenRequest request, AuthenticationCallback callback)
    {

    }

}
