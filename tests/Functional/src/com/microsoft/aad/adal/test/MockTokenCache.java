
package com.microsoft.aad.adal.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.microsoft.aad.adal.TokenCache;
import com.microsoft.aad.adal.TokenCacheItem;
import com.microsoft.aad.adal.UserIdentifier;

public class MockTokenCache extends TokenCache {

    /**
     * 
     */
    private static final long serialVersionUID = -571377076835584408L;

    public static final String CLASS_TOKEN_CACHE_KEY = "com.microsoft.aad.adal.TokenCacheKey";

    public void setItem(MockTokenCacheKey mockKey, TokenCacheItem item)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException {
        Method m = ReflectionUtils.getTestMethod(this, "setItem",
                Class.forName(CLASS_TOKEN_CACHE_KEY), item.getClass());
        m.invoke(this, mockKey.key, item);
    }

    public TokenCacheItem getItem(MockTokenCacheKey mockKey) throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Method m = ReflectionUtils.getTestMethod(this, "getItem",
                Class.forName(CLASS_TOKEN_CACHE_KEY));
        return (TokenCacheItem)m.invoke(this, mockKey.key);
    }
}
