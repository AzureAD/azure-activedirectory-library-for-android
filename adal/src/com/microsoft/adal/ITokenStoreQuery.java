
package com.microsoft.adal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public interface ITokenStoreQuery {

    Iterator<TokenCacheItem> getAll();

    HashSet<String> getUniqueUsersWithTokenCache();

    ArrayList<TokenCacheItem> getTokensForResource(String resource);

    ArrayList<TokenCacheItem> getTokensForUser(String userid);

    void clearTokensForUser(String userid);

    ArrayList<TokenCacheItem> getTokensAboutToExpire();
}
