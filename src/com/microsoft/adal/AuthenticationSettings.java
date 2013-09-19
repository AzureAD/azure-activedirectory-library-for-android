/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

/**
 * Context related settings are defined here
 */
public class AuthenticationSettings {
    private static AuthenticationSettings sInstance = new AuthenticationSettings();

    private ITokenCache mCache = null;
    private IDiscovery mDiscovery = null;
    private boolean mEnableTokenCaching = true;
    private String mResourcePackage = "com.microsoft.protection.authentication";
    private boolean mIgnoreSSLErrors = false;
    private boolean mValidateAuthority = true;
    private boolean mEnableInstallRedirect = false;

    /**
     * RequestAuthEndpoint to append in authority url
     */
    private String mOauthRequestAuthEndpoint = "/oauth2/authorize";

    /**
     * RequesttokenEndpoint to append in authority url
     */
    private String mOauthRequestTokenEndpoint = "/oauth2/token";

    AuthenticationSettings()
    {
        // Default cache is memory based
        mCache = TokenCache.getInstance();
        
        // Default discovery implementation
        mDiscovery = new Discovery();
    }

    public static AuthenticationSettings getInstance()
    {
        return sInstance;
    }

    public String getOauthRequestTokenEndpoint()
    {
        return mOauthRequestTokenEndpoint;
    }

    public String getOauthRequestAuthEndpoint()
    {
        return mOauthRequestAuthEndpoint;
    }

    public void setOauthRequestTokenEndpoint(String value)
    {
        mOauthRequestTokenEndpoint = value;
    }

    public void setOauthRequestAuthEndpoint(String value)
    {
        mOauthRequestAuthEndpoint = value;
    }

    public boolean getIgnoreSSLErrors()
    {
        return mIgnoreSSLErrors;
    }

    public void setIgnoreSSLErrors(boolean value)
    {
        mIgnoreSSLErrors = value;
    }

    public boolean getValidateAuthority()
    {
        return mValidateAuthority;
    }

    public void setValidateAuthority(boolean value)
    {
        mValidateAuthority = value;
    }
    
    public ITokenCache getCache()
    {
        return mCache;
    }

    public void setCache(ITokenCache cache)
    {
        if (cache == null)
            throw new IllegalArgumentException("cache");

        mCache = cache;
    }
    
    public IDiscovery getDiscovery()
    {
        return mDiscovery;
    }

    public void setDiscovery(IDiscovery discovery)
    {
        if (discovery == null)
            throw new IllegalArgumentException("discovery");

        mDiscovery = discovery;
    }

    public String getResourcePackage()
    {
        return mResourcePackage;
    }

    public void setResourcePackage(String resourcePackage)
    {
        if (resourcePackage == null || resourcePackage.length() == 0)
            throw new IllegalArgumentException("resourcePackage");

        mResourcePackage = resourcePackage;
    }

    public boolean getEnableTokenCaching()
    {
        return mEnableTokenCaching;
    }

    public void setEnableTokenCaching(boolean value)
    {
        mEnableTokenCaching = value;
    }

    public boolean getEnableInstallRedirect() {
        // TODO Auto-generated method stub
        return mEnableInstallRedirect;
    }
    
    public void setEnableInstallRedirect(boolean val) {
        // TODO Auto-generated method stub
        mEnableInstallRedirect = val;
    }
}
