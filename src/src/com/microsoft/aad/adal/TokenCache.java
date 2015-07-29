// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.NoSuchPaddingException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Store/Retrieve TokenCacheItem from private SharedPreferences.
 * SharedPreferences saves items when it is committed in an atomic operation.
 * One more retry is attempted in case there is a lock in commit.
 */
public class TokenCache implements ITokenCacheStore {

    private static final String CACHE_BLOB = "CACHE_BLOB";

    private static final long serialVersionUID = 1L;

    private static final String SHARED_PREFERENCE_NAME = "com.microsoft.aad.adal.cache";

    private static final String TAG = "DefaultTokenCacheStore";

    private HashMap<TokenCacheKey, TokenCacheItem> mCacheItems;

    SharedPreferences mPrefs;

    private Context mContext;

    private Gson mGson = new GsonBuilder().registerTypeAdapter(Date.class, new DateTimeAdapter())
            .create();

    private static StorageHelper sHelper;

    private static Object sLock = new Object();

    public TokenCache() {
        mCacheItems = new HashMap<TokenCacheKey, TokenCacheItem>();
    }

    /**
     * @param context {@link Context}
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public TokenCache(Context context) throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        mContext = context;
        if (context != null) {

            mCacheItems = new HashMap<TokenCacheKey, TokenCacheItem>();

            if (!StringExtensions.IsNullOrBlank(AuthenticationSettings.INSTANCE
                    .getSharedPrefPackageName())) {
                try {
                    // Context is created from specified packagename in order to
                    // use same file. Reading private data is only allowed if
                    // apps specify same
                    // sharedUserId. Android OS will assign same UID, if they
                    // are signed with same certificates.
                    mContext = context.createPackageContext(
                            AuthenticationSettings.INSTANCE.getSharedPrefPackageName(),
                            Context.MODE_PRIVATE);
                } catch (NameNotFoundException e) {
                    throw new IllegalArgumentException("Package name:"
                            + AuthenticationSettings.INSTANCE.getSharedPrefPackageName()
                            + " is not found");
                }
            }
            mPrefs = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME, Activity.MODE_PRIVATE);
        } else {
            throw new IllegalArgumentException("Context is null");
        }

        synchronized (sLock) {
            if (sHelper == null) {
                Logger.v(TAG, "Started to initialize storage helper");
                sHelper = new StorageHelper(mContext);
                Logger.v(TAG, "Finished to initialize storage helper");
            }
        }
    }

    private SharedPreferences getSharedPreferences(){
        mPrefs = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME, Activity.MODE_PRIVATE);
        return mPrefs;
    }
    
    public final String serialize() {

        if (mCacheItems != null && !mCacheItems.isEmpty()) {

            String cacheJson = mGson.toJson(mCacheItems.values().toArray(), TokenCacheItem[].class);
            return encrypt(cacheJson);
        }

        return "";
    }

    public final void deSerialize(String input) {
        try {
            mCacheItems = new HashMap<TokenCacheKey, TokenCacheItem>();

            if (!TextUtils.isEmpty(input)) {

                String json = sHelper.decrypt(input);
                TokenCacheItem[] items = mGson.fromJson(json, TokenCacheItem[].class);

                if (items != null && items.length > 0) {

                    // Create internal representation of cache items for easy
                    // lookup
                    for (TokenCacheItem item : items) {
                        TokenCacheKey key = TokenCacheKey.createCacheKey(item);
                        Logger.v(TAG, "Cache key:" + key.toJsonString() + " inserted to TokenCache");
                        mCacheItems.put(key, item);
                    }
                }
            } else {
                Logger.v(TAG, "Cache serialization input is empty.");
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage(), "at serialize", ADALError.ENCODING_IS_NOT_SUPPORTED, e);
        }
    }

    /**
     * Get cache items.
     * 
     * @return Token cache item
     */
    public List<TokenCacheItem> readItems() {
        beforeAccess(new TokenCacheNotificationArgs());
        return (List<TokenCacheItem>)new ArrayList<TokenCacheItem>(mCacheItems.values());
    }

    /**
     * Removes item with key.
     * 
     * @param item {@link TokenCacheItem}
     */
    public void deleteItem(TokenCacheItem item) {
        TokenCacheKey key = TokenCacheKey.createCacheKey(item);
        TokenCacheNotificationArgs args = TokenCacheNotificationArgs.create(key);
        beforeAccess(args);
        beforeWrite(args);

        mCacheItems.remove(key);
        stateChanged();
        afterAccess(args);
    }

    /**
     * Removes all items from cache.
     */
    public void clear() {
        TokenCacheNotificationArgs args = new TokenCacheNotificationArgs();
        beforeAccess(args);
        beforeWrite(args);

        mCacheItems.clear();
        stateChanged();
        afterAccess(args);
    }

    /**
     * Called before using the cache.
     * 
     * @param args
     */
    public void beforeAccess(TokenCacheNotificationArgs args) {

    }

    public void beforeWrite(TokenCacheNotificationArgs args) {

    }

    public void afterAccess(TokenCacheNotificationArgs args) {

    }

    private String encrypt(String value) {
        try {
            return sHelper.encrypt(value);
        } catch (Exception e) {
            Logger.e(TAG, "Encryption failure", "", ADALError.ENCRYPTION_FAILED, e);
        }

        return null;
    }

    void removeItem(TokenCacheKey key) {
        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        
        TokenCacheNotificationArgs args = new TokenCacheNotificationArgs();
        beforeAccess(args);
        beforeWrite(args);

        Iterator<Entry<TokenCacheKey, TokenCacheItem>> it = mCacheItems.entrySet().iterator();
        TokenCacheKey itemToRemove = null;
        while (it.hasNext()) {
            Map.Entry<TokenCacheKey, TokenCacheItem> pair = (Map.Entry<TokenCacheKey, TokenCacheItem>)it
                    .next();
            TokenCacheItem item = (TokenCacheItem)pair.getValue();
            if (key.matches(item)) {
                itemToRemove = pair.getKey();
                break;
            }
        }
        mCacheItems.remove(itemToRemove);
        stateChanged();
        afterAccess(args);
    }

    TokenCacheItem getItem(TokenCacheKey key) {

        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        if (StringExtensions.createStringFromArray(key.getScope(), " ").contains(key.getClientId())) {
            Logger.v(TAG, "Looking for id token...");
        }
        
        TokenCacheNotificationArgs args = TokenCacheNotificationArgs.create(key);
        beforeAccess(args);

        Collection<TokenCacheItem> c = mCacheItems.values();
        Iterator<TokenCacheItem> itr = c.iterator();
        List<TokenCacheItem> items = new ArrayList<TokenCacheItem>();
        while (itr.hasNext()) {
        	TokenCacheItem item = itr.next();
        	if(key.matches(item)){
        		items.add(item);
        	}
        }
        
        // multiple entries for empty user
        if(items.size() > 1 && key.isUserEmpty()){
        	Logger.e(TAG, "Multiple entries in the cache for key:" + key.getLog(), " TokenCache:getItem", ADALError.CACHE_MULTIPLE_ENTRIES);
        	throw new AuthenticationException(ADALError.CACHE_MULTIPLE_ENTRIES);
        }
        
        afterAccess(args);
        return !items.isEmpty() ? items.get(0) : null;
    }
    
    void setItem(TokenCacheKey key, TokenCacheItem item) {

        argumentCheck();

        TokenCacheNotificationArgs args = TokenCacheNotificationArgs.create(key);
        beforeAccess(args);
        beforeWrite(args);

        mCacheItems.put(key, item);
        stateChanged();
        afterAccess(args);
    }

    private void argumentCheck() {
        if (mContext == null) {
            throw new AuthenticationException(ADALError.DEVELOPER_CONTEXT_IS_NOT_PROVIDED);
        }

        if (mPrefs == null) {
            throw new AuthenticationException(ADALError.DEVICE_SHARED_PREF_IS_NOT_AVAILABLE);
        }
    }

    /**
     * Override this method to define custom persistence.
     */
    @Override
    public void stateChanged() {
        String cacheData = serialize();
        Editor prefsEditor = getSharedPreferences().edit();
        prefsEditor.putString(CACHE_BLOB, cacheData);
        // apply will do Async disk write operation.
        prefsEditor.apply();
    }
    
    /**
     * internal usage to setup sharedpref based cache.
     */
    void initCache(){
        if (mPrefs.contains(CACHE_BLOB)) {
            String json = mPrefs.getString(CACHE_BLOB, "");
            deSerialize(json);
        }
    }

    /**
     * Delete items that have scopes intersecting with this scope to remove
     * duplicates since service will return full supported scopes.
     * 
     * @param key
     */
    void deleteIntersectingScope(TokenCacheKey key) {

        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        TokenCacheNotificationArgs args = new TokenCacheNotificationArgs();
        beforeAccess(args);
        beforeWrite(args);

        Iterator<Entry<TokenCacheKey, TokenCacheItem>> it = mCacheItems.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<TokenCacheKey, TokenCacheItem> pair = (Map.Entry<TokenCacheKey, TokenCacheItem>)it
                    .next();
            TokenCacheItem item = (TokenCacheItem)pair.getValue();
            if (key.matches(item)) {
                it.remove();
            }
        }

        stateChanged();
        afterAccess(args);
    }
}
