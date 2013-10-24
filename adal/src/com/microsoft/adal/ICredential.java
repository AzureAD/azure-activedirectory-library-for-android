package com.microsoft.adal;

public interface ICredential {
    
    /**
     * creates key to be used in caching the result for this credential
     * @return
     */
    String CreateCacheKey();
}
