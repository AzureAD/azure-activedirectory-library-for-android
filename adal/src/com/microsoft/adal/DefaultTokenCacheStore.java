/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Revised-10-22-13 Stores token related info such as access token, refresh
 * token, and expiration
 */
public class DefaultTokenCacheStore implements ITokenCacheStore {

    @Override
    public TokenCacheItem GetItem(CacheKey key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void RemoveItem(TokenCacheItem item) {
        // TODO Auto-generated method stub
    }

    @Override
    public void SetItem(TokenCacheItem item) {
        // TODO Auto-generated method stub
    }

    @Override
    public void RemoveItem(CacheKey key) {
        // TODO Auto-generated method stub
    }

    @Override
    public void RemoveAll() {
        // TODO Auto-generated method stub
    }

    // Extra helper methods can be implemented here for queries

    /**
     * User can query over iterator values
     */
    public Iterator<TokenCacheItem> getAll() {
        throw new UnsupportedOperationException("come back later");
    }

    /**
     * Sample code is provided to show token query operations.
     */

    public HashSet<String> getUniqueUsersWithTokenCache() {
        Iterator<TokenCacheItem> results = this.getAll();
        HashSet<String> users = new HashSet<String>();

        while (results.hasNext()) {
            TokenCacheItem item = results.next();
            if (!users.contains(item.getUserInfo().getUserId())) {
                users.add(item.getUserInfo().getUserId());
            }
        }

        return users;
    }

    public ArrayList<TokenCacheItem> getTokensForResource(String resource) {
        Iterator<TokenCacheItem> results = this.getAll();
        ArrayList<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        while (results.hasNext()) {
            TokenCacheItem item = results.next();
            if (item.getResource() == resource) {
                tokenItems.add(item);
            }
        }

        return tokenItems;
    }

    public ArrayList<TokenCacheItem> getTokensForUser(String userid) {
        Iterator<TokenCacheItem> results = this.getAll();
        ArrayList<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        while (results.hasNext()) {
            TokenCacheItem item = results.next();
            if (item.getUserInfo().getUserId() == userid) {
                tokenItems.add(item);
            }
        }

        return tokenItems;
    }

    public void clearTokensForUser(String userid) {
        ArrayList<TokenCacheItem> results = this.getTokensForUser(userid);

        for (TokenCacheItem item : results) {
            if (item.getUserInfo().getUserId() == userid) {
                this.RemoveItem(item);
            }
        }
    }

    public ArrayList<TokenCacheItem> getTokensAboutToExpire() {
        Iterator<TokenCacheItem> results = this.getAll();
        ArrayList<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        while (results.hasNext()) {
            TokenCacheItem item = results.next();
            if (isExpired(item.getExpiresOn())) {
                tokenItems.add(item);
            }
        }

        return tokenItems;
    }

    private boolean isExpired(Date expires) {
        Date validity = getTokenValidityTime().getTime();

        if (expires.before(validity))
            return true;

        return false;
    }

    /**
     * Sample
     */
    private final static int TOKEN_VALIDITY_WINDOW = 10;

    /**
     * Sample
     */
    private static Calendar getTokenValidityTime() {
        Calendar timeAhead = Calendar.getInstance();
        timeAhead.roll(Calendar.SECOND, TOKEN_VALIDITY_WINDOW);
        return timeAhead;
    }

}
