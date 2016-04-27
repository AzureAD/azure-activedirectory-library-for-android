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
import java.io.ObjectStreamClass;
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
import com.google.gson.JsonSyntaxException;

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

    private SharedPreferences mPrefs;
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
    public DefaultTokenCacheStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context is null");
        }
        mContext = context;
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
        if (mPrefs == null) {
            throw new IllegalStateException(ADALError.DEVICE_SHARED_PREF_IS_NOT_AVAILABLE.getDescription());
        }

        try {
            getStorageHelper().loadSecretKeyForAPI();
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed to get private key from AndroidKeyStore", e);
        }
    }

    /**
     * Method that allows to mock StorageHelper class and use custom encryption in UTs
     * @return
     */
    protected StorageHelper getStorageHelper() {
        synchronized (sLock) {
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
        if (StringExtensions.IsNullOrBlank(key)) {
            throw new IllegalArgumentException("key is null or blank");
        }
        
        try {
            return getStorageHelper().decrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Logger.e(TAG, "Decryption failure", "", ADALError.ENCRYPTION_FAILED, e);
            removeItem(key);
            Logger.v(TAG, String.format("Decryption error, item removed for key: '%s'", key));
        }

        return null;
    }

    @Override
    public TokenCacheItem getItem(String key) {
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
     */
    @Override
    public Iterator<TokenCacheItem> getAll() {
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
        Set<String> users = new HashSet<String>();
        
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
        List<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        while (results.hasNext()) {
            final TokenCacheItem tokenCacheItem = results.next();
            if (tokenCacheItem.getResource().equals(resource)) {
                tokenItems.add(tokenCacheItem);
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
        Iterator<TokenCacheItem> results = this.getAll();
        List<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();
        
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
        Iterator<TokenCacheItem> results = this.getAll();
        List<TokenCacheItem> tokenItems = new ArrayList<TokenCacheItem>();

        while (results.hasNext()) {
            final TokenCacheItem tokenCacheItem = results.next();
                if (isAboutToExpire(tokenCacheItem.getExpiresOn())) {
                    tokenItems.add(tokenCacheItem);
                }
            }


        return tokenItems;
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
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        return mPrefs.contains(key);
    }
    
    public TokenCacheItem getFamilyRefreshTokenItemForUser(String userId) {
        Iterator<TokenCacheItem> results = this.getAll();
        TokenCacheItem tokenItem = new TokenCacheItem();
        while (results.hasNext()) {
            final TokenCacheItem tokenCacheItem = results.next();
            if(tokenCacheItem.getUserInfo() != null
                    && tokenCacheItem.getUserInfo().getUserId().equalsIgnoreCase(userId)
                    && !StringExtensions.IsNullOrBlank(tokenCacheItem.getFamilyClientId())
                    && tokenCacheItem.getFamilyClientId().equalsIgnoreCase("1")){
                tokenItem = tokenCacheItem;
                //return the cacheItem which contains the FRT for this userId
                //there should be at most only one FRT record for each userId
                return tokenItem;
            }
        }
        //If the cache does not contain the FRT for this user return null
        return null;
    }
    
    public String serialize(String uniqueUserId) throws AuthenticationException{
        if (StringExtensions.IsNullOrBlank(uniqueUserId)) {
            throw new IllegalArgumentException("uniqueUserId");
        }
        
        if((new BrokerProxy(mContext)).canSwitchToBroker()){
            throw new AuthenticationException(ADALError.FAIL_TO_EXPORT,"Failed to export the FID because broker is enabled.");
        } else {
            TokenCacheItem tokenItem = getFamilyRefreshTokenItemForUser(uniqueUserId);
            
            if(tokenItem!=null) {
                BlobContainer blobContainer = new BlobContainer(tokenItem);
                Gson gson = new Gson();  
                
                if (StringExtensions.IsNullOrBlank(blobContainer.getFamilyRefreshToken())) {
                    Logger.i(TAG, "serialization of FRT cache item: family refresh token is null", "");
                    //throw new AuthenticationException(ADALError.FAIL_TO_EXPORT,"serialization of FRT cache item: family refresh token is null");
                    return null;
                }
                
                Logger.i(TAG, "family refresh token cache item:" + blobContainer.toString(), "");
                String json = gson.toJson(blobContainer);
                //should json be encrypted?
                return json;
                
            } else {
                Logger.i(TAG, "no FRT is found for this user" + uniqueUserId, "");
                return null;
            }
        }      
    }
    
    public void deserialize(String serializedBlob) throws AuthenticationException{
        if (StringExtensions.IsNullOrBlank(serializedBlob)) {
            throw new IllegalArgumentException("serializedBlob");
        }
            
        if((new BrokerProxy(mContext)).canSwitchToBroker()) {
            throw new AuthenticationException(ADALError.FAIL_TO_IMPORT,"Failed to import the serialized blob because broker is enabled.");
        } else {
            Gson gson = new Gson();  
            BlobContainer blobContainer;
            
            try {
                blobContainer = gson.fromJson(serializedBlob, BlobContainer.class);
            } catch (JsonSyntaxException exp) {
                throw new AuthenticationException(ADALError.FAIL_TO_IMPORT,
                        exp.getMessage());
            }
            //@Heidi
            //Q. how to compare the SerialVersionUID?
            long serialVersionID_act = ObjectStreamClass.lookup(blobContainer.getClass()).getSerialVersionUID();
            long serialVersionID_exp = ObjectStreamClass.lookup(BlobContainer.class).getSerialVersionUID();
            if(serialVersionID_act != serialVersionID_exp){
                throw new AuthenticationException(ADALError.FAIL_TO_DESERIALIZE,
                        "Failed to import the serialized blob because the deserialized object version UID is not supported.");
            } 

            Logger.i(TAG + "deserialize", "Json parsing for serializedBlob :" + blobContainer.toString(), "");
            TokenCacheItem tokenCacheItem = blobContainer.getTokenItem();
            String cacheKey = CacheKey.createCacheKey(tokenCacheItem);
            this.setItem(cacheKey, tokenCacheItem);            
        }
    }
}
