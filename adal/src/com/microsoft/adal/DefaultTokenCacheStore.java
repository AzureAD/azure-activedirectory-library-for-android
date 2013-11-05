/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.gson.Gson;

/**
 * Revised-10-22-13 Stores token related info such as access token, refresh
 * token, and expiration
 */
public class DefaultTokenCacheStore implements ITokenCacheStore {

    private static String SHARED_PREFERENCE_NAME = "com.microsoft.adal.cache";

    SharedPreferences mPrefs;

    private Context mContext;

    private Gson gson = new Gson();

    public DefaultTokenCacheStore() {
        mContext = null;
        mPrefs = null;
    }

    public DefaultTokenCacheStore(Context context) {
        mContext = context;
        if (context != null) {
            mPrefs = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME, Activity.MODE_PRIVATE);
        }
    }

    @Override
    public TokenCacheItem getItem(CacheKey key) {
        if (key == null)
            throw new IllegalArgumentException("key");

        if (mPrefs != null && mPrefs.contains(key.toString())) {
            String json = mPrefs.getString(key.toString(), "");
            return gson.fromJson(json, TokenCacheItem.class);
        }

        return null;
    }

    @Override
    public boolean removeItem(TokenCacheItem item) {
        if (item == null)
            throw new IllegalArgumentException("item");

        CacheKey key = CacheKey.createCacheKey(item);

        if (mPrefs != null && mPrefs.contains(key.toString())) {
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.remove(key.toString());
            if (!prefsEditor.commit()) {
                return prefsEditor.commit();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean setItem(TokenCacheItem item) {
        if (item == null)
            throw new IllegalArgumentException("item");

        if (mPrefs != null) {
            String json = gson.toJson(item);
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.putString(CacheKey.createCacheKey(item).toString(), json);

            // when two editors are modifying preferences at the same time, the
            // last one commit wins
            // simply one more retry
            if (!prefsEditor.commit()) {
                return prefsEditor.commit();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean removeItem(CacheKey key) {
        if (key == null)
            throw new IllegalArgumentException("key");

        if (mPrefs != null && mPrefs.contains(key.toString())) {
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.remove(key.toString());
            if (!prefsEditor.commit()) {
                return prefsEditor.commit();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean removeAll() {
        if (mPrefs != null) {
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.clear();
            if (!prefsEditor.commit()) {
                return prefsEditor.commit();
            }
            return true;
        }

        return false;
    }

    // Extra helper methods can be implemented here for queries

    /**
     * User can query over iterator values
     */
    public Iterator<TokenCacheItem> getAll() {
        if (mPrefs != null) {

            Map<String, String> results = (Map<String, String>)mPrefs.getAll();
            Iterator<String> values = results.values().iterator();

            // create objects
            ArrayList<TokenCacheItem> tokens = new ArrayList<TokenCacheItem>(results.values()
                    .size());

            while (values.hasNext()) {
                String json = values.next();
                TokenCacheItem cacheItem = gson.fromJson(json, TokenCacheItem.class);
                tokens.add(cacheItem);
            }

            return tokens.iterator();
        }

        return null;
    }

    /**
     * unique users with tokens
     * 
     * @return unique users
     */
    public HashSet<String> getUniqueUsersWithTokenCache() {
        Iterator<TokenCacheItem> results = this.getAll();
        HashSet<String> users = new HashSet<String>();

        while (results.hasNext()) {
            TokenCacheItem item = results.next();
            if (item.getUserInfo() != null && !users.contains(item.getUserInfo().getUserId())) {
                users.add(item.getUserInfo().getUserId());
            }
        }

        return users;
    }

    /**
     * tokens for resource
     * 
     * @param resource
     * @return
     */
    public ArrayList<TokenCacheItem> getTokensForResource(String resource) {
        Iterator<TokenCacheItem> results = this.getAll();
        ArrayList<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        while (results.hasNext()) {
            TokenCacheItem item = results.next();
            if (item.getResource().equalsIgnoreCase(resource)) {
                tokenItems.add(item);
            }
        }

        return tokenItems;
    }

    /**
     * get tokens for user
     * 
     * @param userid
     * @return
     */
    public ArrayList<TokenCacheItem> getTokensForUser(String userid) {
        Iterator<TokenCacheItem> results = this.getAll();
        ArrayList<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        while (results.hasNext()) {
            TokenCacheItem item = results.next();
            if (item.getUserInfo() != null
                    && item.getUserInfo().getUserId().equalsIgnoreCase(userid)) {
                tokenItems.add(item);
            }
        }

        return tokenItems;
    }

    /**
     * clear tokens for user without additional retry
     * 
     * @param userid
     */
    public void clearTokensForUser(String userid) {
        ArrayList<TokenCacheItem> results = this.getTokensForUser(userid);

        for (TokenCacheItem item : results) {
            if (item.getUserInfo() != null
                    && item.getUserInfo().getUserId().equalsIgnoreCase(userid)) {
                this.removeItem(item);
            }
        }
    }

    /**
     * get tokens about to expire
     * 
     * @return
     */
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
