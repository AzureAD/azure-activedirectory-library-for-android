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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.NoSuchPaddingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * Store/Retrieve TokenCacheItem from private SharedPreferences.
 * SharedPreferences saves items when it is committed in an atomic operation.
 * One more retry is attempted in case there is a lock in commit.
 */
public class DefaultTokenCacheStore implements ITokenCacheStore, ITokenStoreQuery {

    private static final long serialVersionUID = 1L;

    private static final String SHARED_PREFERENCE_NAME = "com.microsoft.aad.adal.cache";

    private static final String TAG = "DefaultTokenCacheStore";

    SharedPreferences mPrefs;

    private Context mContext;

    private Gson mGson = new GsonBuilder()
    .registerTypeAdapter(Date.class, new DateTimeAdapter())
    .create();

    private static StorageHelper sHelper;

    private static Object sLock = new Object();
    
    /**
     * @param context {@link Context}
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    public DefaultTokenCacheStore(Context context) throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        mContext = context;
        if (context != null) {

            if (!StringExtensions.IsNullOrBlank(AuthenticationSettings.INSTANCE
                    .getSharedPrefPackageName())) {
                try {
                    // Context is created from specified packagename in order to
                    // use same file. Reading private data is only allowed if apps specify same
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

    private String encrypt(String value) {
        try {
            return sHelper.encrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Logger.e(TAG, "Encryption failure", "", ADALError.ENCRYPTION_FAILED, e);
        }

        return null;
    }

    private String decrypt(final String key, final String value) {
        if (StringExtensions.IsNullOrBlank(key)) {
            throw new IllegalArgumentException("key is null or blank");
        }
        
        try {
            return sHelper.decrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Logger.e(TAG, "Decryption failure", "", ADALError.ENCRYPTION_FAILED, e);
            removeItem(key);
            Logger.v(TAG, String.format("Decryption error, item removed for key: '%s'", key));
        }

        return null;
    }

    @Override
    public TokenCacheItem getItem(String key) {

        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        if (mPrefs.contains(key)) {
            String json = mPrefs.getString(key, "");
            String decrypted = decrypt(key, json);
            if (decrypted != null) {
                return mGson.fromJson(decrypted, TokenCacheItem.class);
            }
        }

        return null;
    }

    @Override
    public void removeItem(String key) {

        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        if (mPrefs.contains(key)) {
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.remove(key);
            // apply will do Async disk write operation.
            prefsEditor.apply();
        }
    }

    @Override
    public void setItem(String key, TokenCacheItem item) {

        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        if (item == null) {
            throw new IllegalArgumentException("item");
        }

        String json = mGson.toJson(item);
        String encrypted = encrypt(json);
        if (encrypted != null) {
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.putString(key, encrypted);

            // apply will do Async disk write operation.
            prefsEditor.apply();
        } else {
            Logger.e(TAG, "Encrypted output is null", "", ADALError.ENCRYPTION_FAILED);
        }
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
     * User can query over iterator values.
     */
    @Override
    public Iterable<TokenCacheItem> getAll() {

        argumentCheck();

        @SuppressWarnings("unchecked")
        Map<String, String> results = (Map<String, String>)mPrefs.getAll();

        // create objects
        ArrayList<TokenCacheItem> tokens = new ArrayList<TokenCacheItem>(results.values().size());
        
        Iterator<Entry<String, String>> tokenResultEntrySet = results.entrySet().iterator();
        while (tokenResultEntrySet.hasNext())
        {
            final Entry<String, String> tokenEntry = tokenResultEntrySet.next();
            final String tokenKey = tokenEntry.getKey();
            final String tokenValue = tokenEntry.getValue();
            
            final String decryptedValue = decrypt(tokenKey, tokenValue);
            if (decryptedValue != null)
            {
                final TokenCacheItem tokenCacheItem = mGson.fromJson(decryptedValue, TokenCacheItem.class);
                tokens.add(tokenCacheItem);
            }
        }

        return tokens;
    }

    /**
     * Unique users with tokens.
     * 
     * @return unique users
     */
    @Override
    public Set<String> getUniqueUsersWithTokenCache() {
        Iterable<TokenCacheItem> results = this.getAll();
        Set<String> users = new HashSet<String>();
        
        for (final TokenCacheItem tokenCacheItem : results) {
            if (tokenCacheItem.getUserInfo() != null && !users.contains(tokenCacheItem.getUserInfo().getUserId())) {
                users.add(tokenCacheItem.getUserInfo().getUserId());
            }
        }

        return users;
    }

    /**
     * Tokens for resource.
     * 
     * @param resource Resource identifier
     * @return list of {@link TokenCacheItem}
     */
    @Override
    public List<TokenCacheItem> getTokensForResource(String resource) {
        Iterable<TokenCacheItem> results = this.getAll();
        List<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        if (results != null) {
            for (final TokenCacheItem tokenCacheItem : results) {
                if (tokenCacheItem.getResource().equals(resource)) {
                    tokenItems.add(tokenCacheItem);
                }
            }
        }

        return tokenItems;
    }

    /**
     * Get tokens for user.
     * 
     * @param userid userId
     * @return list of {@link TokenCacheItem}
     */
    @Override
    public List<TokenCacheItem> getTokensForUser(String userId) {
        Iterable<TokenCacheItem> results = this.getAll();
        List<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();
        
        if (results != null) {
            for (final TokenCacheItem tokenCacheItem : results) {
                if (tokenCacheItem.getUserInfo() != null 
                        && tokenCacheItem.getUserInfo().getUserId().equalsIgnoreCase(userId)) {
                    tokenItems.add(tokenCacheItem);
                }
            }
        }

        return tokenItems;
    }

    /**
     * Clear tokens for user without additional retry.
     * 
     * @param userid UserId
     */
    @Override
    public void clearTokensForUser(String userid) {
        List<TokenCacheItem> results = this.getTokensForUser(userid);

        for (TokenCacheItem item : results) {
            if (item.getUserInfo() != null
                    && item.getUserInfo().getUserId().equalsIgnoreCase(userid)) {
                this.removeItem(CacheKey.createCacheKey(item));
            }
        }
    }

    /**
     * Get tokens about to expire.
     * 
     * @return list of {@link TokenCacheItem}
     */
    @Override
    public List<TokenCacheItem> getTokensAboutToExpire() {
        Iterable<TokenCacheItem> results = this.getAll();
        List<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        if (results != null) {
            for (final TokenCacheItem tokenCacheItem : results) {
                if (isAboutToExpire(tokenCacheItem.getExpiresOn())) {
                    tokenItems.add(tokenCacheItem);
                }
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

        if (expires != null && expires.before(validity)) {
            return true;
        }

        return false;
    }

    private static final int TOKEN_VALIDITY_WINDOW = 10;

    private static Calendar getTokenValidityTime() {
        Calendar timeAhead = Calendar.getInstance();
        timeAhead.add(Calendar.SECOND, TOKEN_VALIDITY_WINDOW);
        return timeAhead;
    }

    @Override
    public boolean contains(String key) {
        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        return mPrefs.contains(key);
    }
}
