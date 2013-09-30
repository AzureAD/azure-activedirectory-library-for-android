package com.microsoft.adal;

import java.util.HashMap;
import java.util.UUID;


public class AuthenticationOptions {

    public enum Endpoint {
        AuthCodeEndpoint,
        AuthTokenEndpoint,
        DiscoveryEndpoint
    }
    
    public enum PromptBehavior
    {
        /**
         * Acquire token will prompt the user for credentials only when necessary. 
         */
        Auto,
        
        /**
         * The user will be prompted for credentials even if it is available in the cache or in the form of refresh token. New acquired access token and refrefsh token will
         * be used to replace previous value. If Settings switched to Auto, new request will use this latest token from cache. 
         */
        Always,
    }

    private PromptBehavior mPromptBehaviour;
    private HashMap<Endpoint, String> mExtraQueryParams;
    private boolean mIgnoreSSLError;
    private boolean mAskForBrokerDownload;
    private boolean mCheckForBrokerApp;
    private boolean mValidateAuthority;
    private UUID mCorrelationID;
    
    public PromptBehavior getPromptBehaviour() {
        return mPromptBehaviour;
    }
    public void setPromptBehaviour(PromptBehavior mPromptBehaviour) {
        this.mPromptBehaviour = mPromptBehaviour;
    }
    public boolean getAskForBrokerDownload() {
        return mAskForBrokerDownload;
    }
    public void setAskForBrokerDownload(boolean mAskForBrokerDownload) {
        this.mAskForBrokerDownload = mAskForBrokerDownload;
    }
    public boolean getValidateAuthority() {
        return mValidateAuthority;
    }
    public void setValidateAuthority(boolean mValidateAuthority) {
        this.mValidateAuthority = mValidateAuthority;
    }
    public boolean getCheckForBrokerApp() {
        return mCheckForBrokerApp;
    }
    public void setCheckForBrokerApp(boolean mCheckForBrokerApp) {
        this.mCheckForBrokerApp = mCheckForBrokerApp;
    }
    public boolean getIgnoreSSLError() {
        return mIgnoreSSLError;
    }
    public void setIgnoreSSLError(boolean mIgnoreSSLError) {
        this.mIgnoreSSLError = mIgnoreSSLError;
    }
    public UUID getCorrelationID() {
        return mCorrelationID;
    }
    public void setCorrelationID(UUID mCorrelationID) {
        this.mCorrelationID = mCorrelationID;
    }
    
}
