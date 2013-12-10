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
 * Store/Retrieve TokenCacheItem from private SharedPreferences.
 * SharedPreferences saves items when it is committed in an atomic operation.
 * One more retry is attempted in case there is a lock in commit.
 */
public class DefaultTokenCacheStore implements ITokenCacheStore, ITokenStoreQuery {

    private final static long serialVersionUID = 1L;

    private final static String SHARED_PREFERENCE_NAME = "com.microsoft.adal.cache";

    SharedPreferences mPrefs;

    private Context mContext;

    private Gson gson = new Gson();

    public DefaultTokenCacheStore(Context context) {
        mContext = context;
        if (context != null) {
            mPrefs = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME, Activity.MODE_PRIVATE);
        }
    }

    @Override
    public TokenCacheItem getItem(CacheKey key) {

        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        if (mPrefs.contains(getTokenStoreKey(key))) {
            String json = mPrefs.getString(key.toString(), "");
            return gson.fromJson(json, TokenCacheItem.class);
        }

        return null;
    }

    @Override
    public void removeItem(CacheKey key) {

        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        String hashcode = getTokenStoreKey(key);
        if (mPrefs.contains(hashcode)) {
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.remove(hashcode);
            // apply will do Async disk write operation.
            prefsEditor.apply();
        }
    }

    @Override
    public void setItem(CacheKey key, TokenCacheItem item) {

        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        if (item == null) {
            throw new IllegalArgumentException("item");
        }

        String json = gson.toJson(item);
        Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString(getTokenStoreKey(key), json);

        // apply will do Async disk write operation.
        prefsEditor.apply();
    }

    @Override
    public void removeAll() {

        argumentCheck();
        Editor prefsEditor = mPrefs.edit();
        prefsEditor.clear();
        // apply will do Async disk write operation.
        prefsEditor.apply();
    }

    // Extra helper methods can be implemented here for queries

    /**
     * User can query over iterator values
     */
    @Override
    public Iterator<TokenCacheItem> getAll() {

        argumentCheck();
        Map<String, String> results = (Map<String, String>)mPrefs.getAll();
        Iterator<String> values = results.values().iterator();

        // create objects
        ArrayList<TokenCacheItem> tokens = new ArrayList<TokenCacheItem>(results.values().size());

        while (values.hasNext()) {
            String json = values.next();
            TokenCacheItem cacheItem = gson.fromJson(json, TokenCacheItem.class);
            tokens.add(cacheItem);
        }

        return tokens.iterator();
    }

    /**
     * unique users with tokens
     * 
     * @return unique users
     */
    @Override
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
    @Override
    public ArrayList<TokenCacheItem> getTokensForResource(String resource) {
        Iterator<TokenCacheItem> results = this.getAll();
        ArrayList<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        while (results.hasNext()) {
            TokenCacheItem item = results.next();
            if (item.getResource().equals(resource)) {
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
    @Override
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
    @Override
    public void clearTokensForUser(String userid) {
        ArrayList<TokenCacheItem> results = this.getTokensForUser(userid);

        for (TokenCacheItem item : results) {
            if (item.getUserInfo() != null
                    && item.getUserInfo().getUserId().equalsIgnoreCase(userid)) {
                this.removeItem(CacheKey.createCacheKey(item));
            }
        }
    }

    /**
     * get tokens about to expire
     * 
     * @return
     */
    @Override
    public ArrayList<TokenCacheItem> getTokensAboutToExpire() {
        Iterator<TokenCacheItem> results = this.getAll();
        ArrayList<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        while (results.hasNext()) {
            TokenCacheItem item = results.next();
            if (isAboutToExpire(item.getExpiresOn())) {
                tokenItems.add(item);
            }
        }

        return tokenItems;
    }

    private void argumentCheck() {
        if (mContext == null) {
            throw new AuthenticationException(ADALError.DEVELOPER_CONTEXT_IS_NOT_PROVIDED);
        }

        if (mPrefs == null) {
            throw new AuthenticationException(ADALError.DEVICE_SHARED_PREF_IS_NOT_AVAILABLE);
        }
    }

    private boolean isAboutToExpire(Date expires) {
        Date validity = getTokenValidityTime().getTime();

        if (expires.before(validity)) {
            return true;
        }

        return false;
    }

    private final static int TOKEN_VALIDITY_WINDOW = 10;

    private static Calendar getTokenValidityTime() {
        Calendar timeAhead = Calendar.getInstance();
        timeAhead.add(Calendar.SECOND, TOKEN_VALIDITY_WINDOW);
        return timeAhead;
    }

    @Override
    public boolean contains(CacheKey key) {
        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        return mPrefs.contains(getTokenStoreKey(key));
    }

    private String getTokenStoreKey(CacheKey key) {
        return "Key:" + key.hashCode();
    }
}
