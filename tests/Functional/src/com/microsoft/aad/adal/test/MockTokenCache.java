
package com.microsoft.aad.adal.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import android.content.Context;

import com.microsoft.aad.adal.TokenCache;
import com.microsoft.aad.adal.TokenCacheItem;
import com.microsoft.aad.adal.UserIdentifier;

public class MockTokenCache extends TokenCache {

    /**
     * 
     */
    private static final long serialVersionUID = -571377076835584408L;

    public static final String CLASS_TOKEN_CACHE_KEY = "com.microsoft.aad.adal.TokenCacheKey";

    public MockTokenCache(Context context) throws NoSuchAlgorithmException, NoSuchPaddingException {
        super(context);
    }

    public void setItem(MockTokenCacheKey mockKey, TokenCacheItem item)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException {
        Method[] ms = TokenCache.class.getDeclaredMethods();
        Method m = TokenCache.class.getDeclaredMethod("setItem",
                Class.forName(CLASS_TOKEN_CACHE_KEY), item.getClass());
        m.setAccessible(true);
        m.invoke(this, mockKey.key, item);
    }

    public TokenCacheItem getItem(MockTokenCacheKey mockKey) throws IllegalArgumentException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Method m = TokenCache.class.getDeclaredMethod("getItem",
                Class.forName(CLASS_TOKEN_CACHE_KEY));
        m.setAccessible(true);
        return (TokenCacheItem)m.invoke(this, mockKey.key);
    }
}
