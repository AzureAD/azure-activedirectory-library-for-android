/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.IOException;
import java.security.DigestException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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

    private static final String TAG = "DefaultTokenCacheStore";

    SharedPreferences mPrefs;

    private Context mContext;

    private Gson gson = new Gson();

    private static StorageHelper sHelper;

    public DefaultTokenCacheStore(Context context) throws NoSuchAlgorithmException,
            NoSuchPaddingException {
        mContext = context;
        if (context != null) {
            mPrefs = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME, Activity.MODE_PRIVATE);
        } else {
            throw new IllegalArgumentException("Context is null");
        }

        if (sHelper == null) {
            Logger.v(TAG, "Started to initialize storage helper");
            sHelper = new StorageHelper(context);
            Logger.v(TAG, "Finished to initialize storage helper");
        }
    }

    private String encrypt(String value) {
        try {
            return sHelper.encrypt(value);
        } catch (Exception e) {
            Logger.e(TAG, "Encryption failure", "", ADALError.ENCRYPTION_FAILED, e);
        }

        return null;
    }

    private String decrypt(String value) {
        try {
            return sHelper.decrypt(value);
        } catch (Exception e) {
            Logger.e(TAG, "Decryption failure", "", ADALError.ENCRYPTION_FAILED, e);
            Logger.v(TAG,
                    String.format("Decryption error for key: '%s'. Item will be removed", value));
            removeItem(value);
            Logger.v(TAG, String.format("Item removed for key: '%s'", value));
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
            String decrypted = decrypt(json);
            if (decrypted != null) {
                return gson.fromJson(decrypted, TokenCacheItem.class);
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

        String json = gson.toJson(item);
        String encrypted = encrypt(json);
        if (encrypted != null) {
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.putString(key, json);

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
     * User can query over iterator values
     */
    @Override
    public Iterator<TokenCacheItem> getAll() {

        argumentCheck();

        @SuppressWarnings("unchecked")
        Map<String, String> results = (Map<String, String>)mPrefs.getAll();
        Iterator<String> values = results.values().iterator();

        // create objects
        ArrayList<TokenCacheItem> tokens = new ArrayList<TokenCacheItem>(results.values().size());

        while (values.hasNext()) {
            String json = values.next();
            String decrypted = decrypt(json);
            if (decrypted != null) {
                TokenCacheItem cacheItem = gson.fromJson(decrypted, TokenCacheItem.class);
                tokens.add(cacheItem);
            }
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

        if (expires != null && expires.before(validity)) {
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
    public boolean contains(String key) {
        argumentCheck();

        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        return mPrefs.contains(key);
    }
}
