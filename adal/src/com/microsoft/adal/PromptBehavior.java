package com.microsoft.adal;

public enum PromptBehavior {
    /**
     * Acquire token will prompt the user for credentials only when
     * necessary.
     */
    Auto,

    /**
     * The user will be prompted for credentials even if it is available in
     * the cache or in the form of refresh token. New acquired access token
     * and refresh token will be used to replace previous value. If Settings
     * switched to Auto, new request will use this latest token from cache.
     */
    Always,

    /**
     * Don't show UI
     */
    Never
}