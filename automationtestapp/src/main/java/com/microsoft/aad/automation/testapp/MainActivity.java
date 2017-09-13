//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.automation.testapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.DateTimeAdapter;
import com.microsoft.aad.adal.TokenCacheItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    public static final String FLOW_CODE = "FlowCode";
    public static final int ACQUIRE_TOKEN = 1001;
    public static final int ACQUIRE_TOKEN_SILENT = 1002;
    public static final int INVALIDATE_ACCESS_TOKEN = 1003;
    public static final int INVALIDATE_REFRESH_TOKEN = 1004;
    public static final int INVALIDATE_FAMILY_REFRESH_TOKEN = 1006;
    public static final int READ_CACHE = 1005;
    
    private Context mContext;    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        
        // Button for acquireToken call
        final Button acquireTokenButton = (Button) findViewById(R.id.acquireToken);
        acquireTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchAuthenticationInfoActivity(ACQUIRE_TOKEN);
            }
        });

        // Button for acquireTokenSilent call
        final Button acquireTokenSilentButton = (Button) findViewById(R.id.acquireTokenSilent);
        acquireTokenSilentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAuthenticationInfoActivity(ACQUIRE_TOKEN_SILENT);
            }
        });

        final Button invalidateAccessTokenButton = (Button) findViewById(R.id.expireAccessToken);
        invalidateAccessTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAuthenticationInfoActivity(INVALIDATE_ACCESS_TOKEN);
            }
        });

        final Button invalidateRefreshToken = (Button) findViewById(R.id.invalidateRefreshToken);
        invalidateRefreshToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAuthenticationInfoActivity(INVALIDATE_REFRESH_TOKEN);
            }
        });
        
        final Button invalidateFamilyRefreshToken = (Button) findViewById(R.id.invalidateFamilyRefreshToken);
        invalidateFamilyRefreshToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAuthenticationInfoActivity(INVALIDATE_FAMILY_REFRESH_TOKEN);
            }
        });

        final Button readCacheButton = (Button) findViewById(R.id.readCache);
        readCacheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processReadCacheRequest();
            }
        });

        final Button emptyCacheButton = (Button) findViewById(R.id.clearCache);
        emptyCacheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processEmptyCacheRequest();
            }
        });
    }

    private void launchAuthenticationInfoActivity(int flowCode) {
        final Intent intent = new Intent();
        intent.setClass(mContext, SignInActivity.class);
        intent.putExtra(FLOW_CODE, flowCode);
        this.startActivity(intent);
    }

    private void processEmptyCacheRequest() {
        final AuthenticationContext authenticationContext = createAuthenticationContext();
        Intent intent = new Intent();
        try {
            int numberOfCacheItem = getAllSerializedCacheItem(authenticationContext).size();
            authenticationContext.getCache().removeAll();

            intent.putExtra(Constants.CLEARED_TOKEN_COUNT, String.valueOf(numberOfCacheItem));
        } catch (final JSONException e) {
            intent = SignInActivity.getErrorIntentForResultActivity(Constants.JSON_ERROR, "Unable to convert to Json "
                    + e.getMessage());
        }
        
        launchResultActivity(intent);
    }

    private void processReadCacheRequest() {
        final AuthenticationContext authenticationContext = createAuthenticationContext();
        Intent intent = new Intent();
        try {
            final ArrayList<String> allItems = getAllSerializedCacheItem(authenticationContext);
            intent.putStringArrayListExtra(Constants.READ_CACHE, allItems);
        } catch (JSONException e) {
            intent = SignInActivity.getErrorIntentForResultActivity(Constants.JSON_ERROR, "Unable to convert to Json "
                    + e.getMessage());
        }
        
        launchResultActivity(intent);
    }

    private ArrayList<String> getAllSerializedCacheItem(final AuthenticationContext authenticationContext) throws JSONException {
        final ArrayList<String> allItems = new ArrayList<>();
        final Iterator<TokenCacheItem> allCacheItemIterator = authenticationContext.getCache().getAll();
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateTimeAdapter())
                .create();
        while (allCacheItemIterator.hasNext()) {
            final TokenCacheItem item = allCacheItemIterator.next();
            final JSONObject tokenCacheItem = new JSONObject();
            tokenCacheItem.put(Constants.CACHE_DATA.ACCESS_TOKEN, item.getAccessToken());
            tokenCacheItem.put(Constants.CACHE_DATA.REFRESH_TOKEN, item.getRefreshToken());
            tokenCacheItem.put(Constants.CACHE_DATA.RESOURCE, item.getResource());
            tokenCacheItem.put(Constants.CACHE_DATA.AUTHORITY, item.getAuthority());
            tokenCacheItem.put(Constants.CACHE_DATA.CLIENT_ID, item.getClientId());
            tokenCacheItem.put(Constants.CACHE_DATA.RAW_ID_TOKEN, item.getRawIdToken());
            // unix timestamp
            tokenCacheItem.put(Constants.CACHE_DATA.EXPIRES_ON, item.getExpiresOn().getTime() / 1000);
            tokenCacheItem.put(Constants.CACHE_DATA.IS_MRRT, Boolean.toString(item.getIsMultiResourceRefreshToken()));
            tokenCacheItem.put(Constants.CACHE_DATA.TENANT_ID, item.getTenantId());
            tokenCacheItem.put(Constants.CACHE_DATA.FAMILY_CLIENT_ID, item.getFamilyClientId());
            tokenCacheItem.put(Constants.CACHE_DATA.EXTENDED_EXPIRES_ON, item.getExtendedExpiresOn().getTime() / 1000);

            if (item.getUserInfo() != null) {
                tokenCacheItem.put(Constants.CACHE_DATA.FAMILY_NAME, item.getUserInfo().getFamilyName());
                tokenCacheItem.put(Constants.CACHE_DATA.GIVEN_NAME, item.getUserInfo().getGivenName());
                tokenCacheItem.put(Constants.CACHE_DATA.UNIQUE_USER_ID, item.getUserInfo().getUserId());
                tokenCacheItem.put(Constants.CACHE_DATA.DISPLAYABLE_ID, item.getUserInfo().getDisplayableId());
                tokenCacheItem.put(Constants.CACHE_DATA.IDENTITY_PROVIDER, item.getUserInfo().getIdentityProvider());
            }
            allItems.add(tokenCacheItem.toString());
        }

        return allItems;
    }

    private AuthenticationContext createAuthenticationContext() {
        // this is to clear or read the whole cache, authority does not matter, use the common authority and clean up the whole cache.
        final String authority = "https://login.microsoftonline.com/common";
        return new AuthenticationContext(mContext, authority, false);
    }

    private void launchResultActivity(final Intent intent) {
        intent.setClass(this.getApplicationContext(), ResultActivity.class);
        this.startActivity(intent);
    }
}