
package com.microsoft.adal;

/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Does cache related actions such as putResult, getResult, removeResult
 * This stores cache in SharedPreferences. It is not ideal for large number of tokens.
 * Shared preferences are Thread-Safe, but not process safe. Each app writes to its own data. Multiple edits can be active within the app.
 * TODO: watch for: If broker app is hosted in caller's process, this will make it complicated.
 * 
 * @author omercan
 */
public class TokenCache implements ITokenCache {

    private static String SHARED_PREFERENCE_NAME = "com.microsoft.adal.cache";

    SharedPreferences mPrefs;
    private Context mContext;
    private Gson gson = new Gson();

    TokenCache()
    {
        mContext = null;
        mPrefs = null;
    }

    TokenCache(Context context)
    {
        mContext = context;
        mPrefs = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME,
                Activity.MODE_PRIVATE);
    }
     
    /**
     * Gets AuthenticationResult for a given key
     */
    public AuthenticationResult getResult(String key)
    {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key");

        if (mPrefs != null && mPrefs.contains(key))
        {
            String json = mPrefs.getString(key, "");
            return gson.fromJson(json, AuthenticationResult.class);
        }

        return null;
    }

    /*
     * @key Lookup key that may be composed of authorization, resourceId, and
     * clientId
     * @result Result which has min of access token, refresh token and expire
     * time
     */
    public boolean putResult(String key, AuthenticationResult result)
    {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key");

        if (result == null)
            throw new IllegalArgumentException("result");
        
        if (mPrefs != null)
        {
            String json = gson.toJson(result);
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.putString(key, json);

            // TODO: Check the source code for this editor
            // when two editors are modifying preferences at the same time, the last one to call commit wins
            if(!prefsEditor.commit())
            {
                return prefsEditor.commit();
            }
        }
        
        return false;
    }

    /*
     * remove result for this key from cache (non-Javadoc)
     * @see com.microsoft.adal.ITokenCache#removeResult(java.lang.String)
     */
    public boolean removeResult(String key)
    {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("key");

        if (mPrefs != null && mPrefs.contains(key))
        {
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.remove(key);
            return prefsEditor.commit();
        }
        
        return false;
    }

    /*
     * remove all objects (non-Javadoc)
     * @see com.microsoft.adal.ITokenCache#removeAll()
     */
    public boolean removeAll()
    {
        if (mPrefs != null)
        {
            Editor prefsEditor = mPrefs.edit();
            prefsEditor.clear();
            if (!prefsEditor.commit())
            {
                return prefsEditor.commit();
            }
        }

        return false;
    }

    @Override
    public HashMap<String, AuthenticationResult> getAllResults() {

        if (mPrefs != null)
        {
            HashMap<String, AuthenticationResult> output = new HashMap<String, AuthenticationResult>();

            Map<String, ?> results = (Map<String, AuthenticationResult>) mPrefs.getAll();
            for (Map.Entry<String, ?> entry : results.entrySet()) {
                Log.d("map values", entry.getKey() + ": " +
                        entry.getValue().toString());
                output.put(entry.getKey(),
                        gson.fromJson(entry.getValue().toString(), AuthenticationResult.class));
            }
            return output;
        }
        return null;
    }
}
