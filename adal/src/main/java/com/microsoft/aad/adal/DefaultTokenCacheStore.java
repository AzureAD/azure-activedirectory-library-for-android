/// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

/**
 * Store/Retrieve TokenCacheItem from private SharedPreferences.
 * SharedPreferences saves items when it is committed in an atomic operation.
 * One more retry is attempted in case there is a lock in commit.
 */
public class DefaultTokenCacheStore implements ITokenCacheStore, ITokenStoreQuery {

    private static final long serialVersionUID = 1L;

    private static final String SHARED_PREFERENCE_NAME = "com.microsoft.aad.adal.cache";

    private static final String TAG = "DefaultTokenCacheStore";

    private SharedPreferences mPrefs;
    private Context mContext;

    private Gson mGson = new GsonBuilder()
    .registerTypeAdapter(Date.class, new DateTimeAdapter())
    .create();

    @SuppressLint("StaticFieldLeak")
    private static StorageHelper sHelper;

    private static final Object LOCK = new Object();
    /**
     * @param context {@link Context}
     */
    @SuppressLint("WrongConstant")
    public DefaultTokenCacheStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is null");
        }
        mContext = context;
        if (!StringExtensions.isNullOrBlank(AuthenticationSettings.INSTANCE
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
        if (mPrefs == null) {
            throw new IllegalStateException(ADALError.DEVICE_SHARED_PREF_IS_NOT_AVAILABLE.getDescription());
        }
        
        // Check upfront when initializing DefaultTokenCacheStore. 
        // If it's under API 18 and secretkey is not provided, we should fail upfront to inform 
        // notify developers. 
        validateSecretKeySetting();
    }

    /**
     * Method that allows to mock StorageHelper class and use custom encryption in UTs.
     */
    protected StorageHelper getStorageHelper() {
        synchronized (LOCK) {
            if (sHelper == null) {
                Logger.v(TAG, "Started to initialize storage helper");
                sHelper = new StorageHelper(mContext);
                Logger.v(TAG, "Finished to initialize storage helper");
            }
        }
        return sHelper;
    }

    private String encrypt(String value) {
        try {
            return getStorageHelper().encrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Logger.e(TAG, "Encryption failure", "", ADALError.ENCRYPTION_FAILED, e);
        }

        return null;
    }

    private String decrypt(final String key, final String value) {
        if (StringExtensions.isNullOrBlank(key)) {
            throw new IllegalArgumentException("key is null or blank");
        }
        
        try {
            return getStorageHelper().decrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Logger.e(TAG, "Decryption failure", "", ADALError.DECRYPTION_FAILED, e);
            removeItem(key);
            Logger.v(TAG, String.format("Decryption error, item removed for key: '%s'", key));
        }

        return null;
    }

    @Override
    public TokenCacheItem getItem(String key) {
        if (key == null) {
            throw new IllegalArgumentException("The key is null.");
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
        Editor prefsEditor = mPrefs.edit();
        prefsEditor.clear();
        // apply will do Async disk write operation.
        prefsEditor.apply();
    }

    // Extra helper methods can be implemented here for queries

    /**
     * User can query over iterator values.
     * 
     * @return TokenCacheItem list iterator
     */
    @Override
    public Iterator<TokenCacheItem> getAll() {
        @SuppressWarnings("unchecked")
        Map<String, String> results = (Map<String, String>) mPrefs.getAll();

        // create objects
        final List<TokenCacheItem> tokens = new ArrayList<>(results.values().size());
        
        Iterator<Entry<String, String>> tokenResultEntrySet = results.entrySet().iterator();
        while (tokenResultEntrySet.hasNext()) {
            final Entry<String, String> tokenEntry = tokenResultEntrySet.next();
            final String tokenKey = tokenEntry.getKey();
            final String tokenValue = tokenEntry.getValue();
            
            final String decryptedValue = decrypt(tokenKey, tokenValue);
            if (decryptedValue != null) {
                final TokenCacheItem tokenCacheItem = mGson.fromJson(decryptedValue, TokenCacheItem.class);
                tokens.add(tokenCacheItem);
            }
        }

        return tokens.iterator();
    }

    /**
     * Unique users with tokens.
     * 
     * @return unique users
     */
    @Override
    public Set<String> getUniqueUsersWithTokenCache() {
        Iterator<TokenCacheItem> results = this.getAll();
        final Set<String> users = new HashSet<>();
        
        while (results.hasNext()) {
            final TokenCacheItem tokenCacheItem = results.next();
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
        Iterator<TokenCacheItem> results = this.getAll();
        final List<TokenCacheItem> tokenItems = new ArrayList<>();

        while (results.hasNext()) {
            final TokenCacheItem tokenCacheItem = results.next();
            // MRRT and FRT don't store resource in the token cache item. 
            if (resource.equals(tokenCacheItem.getResource())) {
                tokenItems.add(tokenCacheItem);
            }
        }

        return tokenItems;
    }

    /**
     * Get tokens for user.
     * 
     * @param userId userId
     * @return list of {@link TokenCacheItem}
     */
    @Override
    public List<TokenCacheItem> getTokensForUser(String userId) {
        Iterator<TokenCacheItem> results = this.getAll();
        final List<TokenCacheItem> tokenItems = new ArrayList<>();
        
        while (results.hasNext()) {
            final TokenCacheItem tokenCacheItem = results.next();
            if (tokenCacheItem.getUserInfo() != null
                    && tokenCacheItem.getUserInfo().getUserId().equalsIgnoreCase(userId)) {
                tokenItems.add(tokenCacheItem);
            }
        }

        return tokenItems;
    }

    /**
     * Clear tokens for user without additional retry.
     * 
     * @param userId UserId
     */
    @Override
    public void clearTokensForUser(String userId) {
        final List<TokenCacheItem> results = this.getTokensForUser(userId);

        for (TokenCacheItem item : results) {
            if (item.getUserInfo() != null
                    && item.getUserInfo().getUserId() != null
                    && item.getUserInfo().getUserId().equalsIgnoreCase(userId)) {
                try {
                    this.removeItem(CacheKey.createCacheKey(item));
                } catch (final AuthenticationException exception) {
                    // Catch the exception because clearTokensForUser is an API in public 
                    // interface ITokenCacheQuery.
                    Logger.e(TAG, "Fail to create cachekey", "", exception.getCode(), exception);
                }
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
        Iterator<TokenCacheItem> results = this.getAll();
        final List<TokenCacheItem> tokenItems = new ArrayList<>();

        while (results.hasNext()) {
            final TokenCacheItem tokenCacheItem = results.next();
                if (isAboutToExpire(tokenCacheItem.getExpiresOn())) {
                    tokenItems.add(tokenCacheItem);
                }
            }


        return tokenItems;
    }

    private void validateSecretKeySetting() {
        final byte[] secretKeyData = AuthenticationSettings.INSTANCE.getSecretKeyData();
        if (secretKeyData == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            throw new IllegalArgumentException("Secret key must be provided for API < 18. "
                    + "Use AuthenticationSettings.INSTANCE.setSecretKey()");
        }
    }

    private boolean isAboutToExpire(Date expires) {
        Date validity = getTokenValidityTime().getTime();

        return expires != null && expires.before(validity);
    }

    private static final int TOKEN_VALIDITY_WINDOW = 10;

    private static Calendar getTokenValidityTime() {
        Calendar timeAhead = Calendar.getInstance();
        timeAhead.add(Calendar.SECOND, TOKEN_VALIDITY_WINDOW);
        return timeAhead;
    }

    @Override
    public boolean contains(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        return mPrefs.contains(key);
    }
    
}
