
package com.microsoft.aad.adal.test;

import java.lang.reflect.InvocationTargetException;

import com.microsoft.aad.adal.UserIdentifier;
import com.microsoft.aad.adal.UserIdentifier.UserIdentifierType;

class MockTokenCacheKey {

    public Object key;

    public MockTokenCacheKey createCacheKey(String authority, String[] scope, String policy,
            String clientId, boolean mrrt, String uniqueId, String displayableId)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        Object keyObj = ReflectionUtils.getInstance(MockTokenCache.CLASS_TOKEN_CACHE_KEY,
                authority, scope, policy, clientId, mrrt, uniqueId, displayableId);
        MockTokenCacheKey key = new MockTokenCacheKey();
        key.key = keyObj;
        return key;
    }

    public static MockTokenCacheKey createCacheKey(String validAuthority, String[] testScope,
            String policy, String clientId, boolean mrrt, UserIdentifier userid)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        String uniqueId = userid.getType() == UserIdentifierType.UniqueId ? userid.getId() : "";
        String displayableId = userid.getType() != UserIdentifierType.UniqueId ? userid.getId()
                : "";
        Object keyObj = ReflectionUtils.getInstance(MockTokenCache.CLASS_TOKEN_CACHE_KEY,
                validAuthority, testScope, policy, clientId, mrrt, uniqueId, displayableId);
        MockTokenCacheKey key = new MockTokenCacheKey();
        key.key = keyObj;
        return key;
    }
}
