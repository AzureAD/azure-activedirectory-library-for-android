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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationException;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.CacheKey;
import com.microsoft.aad.adal.ITokenCacheStore;
import com.microsoft.aad.adal.PromptBehavior;
import com.microsoft.aad.adal.TokenCacheItem;
import com.microsoft.aad.adal.UserInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handle the coming request, will gather request info (JSON format of the data contains the authority, resource, clientId,
 * redirect, ect).
 */
public class SignInActivity extends AppCompatActivity {

    public static final String AUTHORITY = "authority";
    public static final String RESOURCE = "resource";
    public static final String CLIENT_ID = "client_id";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String USE_BROKER = "use_broker";
    public static final String PROMPT_BEHAVIOR = "prompt_behavior";
    public static final String EXTRA_QUERY_PARAM = "extra_qp";
    public static final String VALIDATE_AUTHORITY = "validate_authority";
    public static final String USER_IDENTIFIER = "user_identifier";
    public static final String USER_IDENTIFIER_TYPE = "user_identifier_type";
    public static final String CORRELATION_ID = "correlation_id";

    static final String INVALID_REFRESH_TOKEN = "some invalid refresh token";

    private TextView mTextView;
    private String mAuthority;
    private String mResource;
    private String mClientId;
    private String mRedirectUri;
    private boolean mUseBroker;
    private PromptBehavior mPromptBehavior;
    private String mLoginHint;
    private String mUserId;
    private String mExtraQueryParam;
    private AuthenticationContext mAuthenticationContext;
    private boolean mValidateAuthority;
    private UUID mCorrelationId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        mTextView = (EditText) findViewById(R.id.requestInfo);

        final Button goButton = (Button) findViewById(R.id.requestGo);
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performAuthentication();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mTextView.setText("");
        mAuthenticationContext.onActivityResult(requestCode, resultCode, data);
    }

    private void performAuthentication() {
        final Intent receivedIntent = getIntent();
        int flowCode = receivedIntent.getIntExtra(MainActivity.FLOW_CODE, 0);

        final Map<String, String> inputItems;
        try {
            inputItems = readAuthenticationInfo();
        } catch (final JSONException e) {
            sendErrorToResultActivity(Constants.JSON_ERROR, "Unable to read the input JSON info " + e.getMessage());
            return;
        }

        if (inputItems.isEmpty()) {
            return;
        }

        validateUserInput(inputItems, flowCode);

        setAuthenticationData(inputItems);
        AuthenticationSettings.INSTANCE.setUseBroker(mUseBroker);

        mAuthenticationContext = new AuthenticationContext(getApplicationContext(), mAuthority, mValidateAuthority);
        switch (flowCode) {
            case MainActivity.ACQUIRE_TOKEN:
                acquireToken();
                break;
            case MainActivity.ACQUIRE_TOKEN_SILENT:
                acquireTokenSilent();
                break;
            case MainActivity.INVALIDATE_ACCESS_TOKEN:
                processExpireAccessTokenRequest();
                break;
            case MainActivity.INVALIDATE_REFRESH_TOKEN:
                processInvalidateRefreshTokenRequest();
                break;
            case MainActivity.INVALIDATE_FAMILY_REFRESH_TOKEN:
                processInvalidateFamilyRefreshTokenRequest();
                break;
            default:
                sendErrorToResultActivity("unknown_request", "Unknown request is received");
                break;
        }
    }

    static Intent getErrorIntentForResultActivity(final String error, final String errorDescription) {
        final Intent intent = new Intent();
        intent.putExtra(Constants.ERROR, error);
        intent.putExtra(Constants.ERROR_DESCRIPTION, errorDescription);

        return intent;
    }

    private void sendErrorToResultActivity(final String error, final String errorDescription) {
        launchResultActivity(getErrorIntentForResultActivity(error, errorDescription));
    }

    private void processExpireAccessTokenRequest() {
        int count = expireAccessToken();
        final Intent intent = new Intent();
        intent.putExtra(Constants.EXPIRED_ACCESS_TOKEN_COUNT, String.valueOf(count));
        launchResultActivity(intent);
    }

    private void processInvalidateRefreshTokenRequest() {
        int count = invalidateRefreshToken();
        final Intent intent = new Intent();
        intent.putExtra(Constants.INVALIDATED_REFRESH_TOKEN_COUNT, String.valueOf(count));
        launchResultActivity(intent);
    }
    
    private void processInvalidateFamilyRefreshTokenRequest() {
        int count = invalidateFamilyRefreshToken();
        final Intent intent = new Intent();
        intent.putExtra(Constants.INVALIDATED_FAMILY_REFRESH_TOKEN_COUNT, String.valueOf(count));
        launchResultActivity(intent);
    }

    private Map<String, String> readAuthenticationInfo() throws JSONException {
        final String userInputText = mTextView.getText().toString();
        if (TextUtils.isEmpty(userInputText)) {
            // TODO: return error
            sendErrorToResultActivity("empty_requestInfo", "No user input for the request.");
            return Collections.emptyMap();
        }

        // parse Json response
        final Map<String, String> inputItems = new HashMap<>();
        extractJsonObjects(inputItems, userInputText);

        return inputItems;
    }

    private static void extractJsonObjects(Map<String, String> inputItems, String jsonStr)
            throws JSONException {
        final JSONObject jsonObject = new JSONObject(jsonStr);
        final Iterator<?> iterator = jsonObject.keys();

        while (iterator.hasNext()) {
            final String key = (String) iterator.next();
            inputItems.put(key, jsonObject.getString(key));
        }
    }

    private void validateUserInput(final Map<String, String> inputItems, int flowCode) {
        if (inputItems.isEmpty()) {
            throw new IllegalArgumentException("No sign-in data typed in the textBox");
        }

        if (TextUtils.isEmpty(inputItems.get(RESOURCE))) {
            throw new IllegalArgumentException("resource");
        }

        if (TextUtils.isEmpty(inputItems.get(AUTHORITY))) {
            throw new IllegalArgumentException("authority");
        }

        if (TextUtils.isEmpty(inputItems.get(CLIENT_ID))) {
            throw new IllegalArgumentException("clientId");
        }

        final String userIdentifierType = inputItems.get(USER_IDENTIFIER_TYPE);
        final String userIdentifier = inputItems.get(USER_IDENTIFIER);
        if (!TextUtils.isEmpty(userIdentifierType) && TextUtils.isEmpty(userIdentifier)
                || !TextUtils.isEmpty(userIdentifierType) && TextUtils.isEmpty(userIdentifier)) {
            throw new IllegalArgumentException("userIdentifier and userIdentifierType");
        }

        if (flowCode == MainActivity.ACQUIRE_TOKEN && TextUtils.isEmpty(inputItems.get(REDIRECT_URI))) {
            throw new IllegalArgumentException("redirect_uri");
        }

        if (flowCode == MainActivity.INVALIDATE_ACCESS_TOKEN && TextUtils.isEmpty(userIdentifier)) {
            throw new IllegalArgumentException("user identifier");
        }
    }

    private void setAuthenticationData(final Map<String, String> inputItems) {
        mAuthority = inputItems.get(AUTHORITY);
        mResource = inputItems.get(RESOURCE);
        mRedirectUri = inputItems.get(REDIRECT_URI);
        mClientId = inputItems.get(CLIENT_ID);
        mUseBroker = inputItems.get(USE_BROKER) == null ? false : Boolean.valueOf(inputItems.get(USE_BROKER));
        mPromptBehavior = getPromptBehavior(inputItems.get(PROMPT_BEHAVIOR));
        mExtraQueryParam = inputItems.get(EXTRA_QUERY_PARAM);
        mValidateAuthority = inputItems.get(VALIDATE_AUTHORITY) == null ? true : Boolean.valueOf(
                inputItems.get(VALIDATE_AUTHORITY));

        final String uniqueIdentifier = inputItems.get(USER_IDENTIFIER);
        final String uniqueIdentifierType = inputItems.get(USER_IDENTIFIER_TYPE);
        if(!TextUtils.isEmpty(uniqueIdentifierType)){
           if (uniqueIdentifierType.equalsIgnoreCase("unique_id")) {
                mUserId = uniqueIdentifier;
            } else if (uniqueIdentifierType.equalsIgnoreCase("optional_displayable")
                    || uniqueIdentifierType.equalsIgnoreCase("required_displayable")) {
                mLoginHint = uniqueIdentifier;
            } 
        }        

        final String correlationId = inputItems.get(CORRELATION_ID);
        if (!TextUtils.isEmpty(correlationId)) {
            mCorrelationId = UUID.fromString(correlationId);
        }
    }

    PromptBehavior getPromptBehavior(final String inputPromptBehaviorString) {
        if (TextUtils.isEmpty(inputPromptBehaviorString)) {
            return null;
        }

        if (inputPromptBehaviorString.equalsIgnoreCase(PromptBehavior.Always.toString())) {
            return PromptBehavior.Always;
        } else if (inputPromptBehaviorString.equalsIgnoreCase(PromptBehavior.Auto.toString())) {
            return PromptBehavior.Auto;
        } else if (inputPromptBehaviorString.equalsIgnoreCase(PromptBehavior.FORCE_PROMPT.toString())) {
            return PromptBehavior.FORCE_PROMPT;
        } else if (inputPromptBehaviorString.equalsIgnoreCase(PromptBehavior.REFRESH_SESSION.toString())) {
            return PromptBehavior.REFRESH_SESSION;
        }

        return null;
    }

    private void acquireToken() {
        mAuthenticationContext.acquireToken(SignInActivity.this, mResource, mClientId,
                mRedirectUri, mLoginHint, mPromptBehavior, mExtraQueryParam, getAdalCallback());
    }

    private void acquireTokenSilent() {
        mAuthenticationContext.acquireTokenSilentAsync(mResource, mClientId, mUserId, getAdalCallback());
    }

    private int expireAccessToken() {
        final ITokenCacheStore tokenCacheStore = mAuthenticationContext.getCache();

        int count = 0;
        final String cacheKeyWithUserId = CacheKey.createCacheKeyForRTEntry(mAuthority, mResource, mClientId, mUserId);
        final TokenCacheItem itemWithUserId = tokenCacheStore.getItem(cacheKeyWithUserId);
        count += tokenExpired(itemWithUserId, cacheKeyWithUserId, tokenCacheStore);


        final String cacheKeyWithDisplayableId = CacheKey.createCacheKeyForRTEntry(mAuthority, mResource, mClientId, mLoginHint);
        final TokenCacheItem itemWithDisplayable = tokenCacheStore.getItem(cacheKeyWithDisplayableId);
        count += tokenExpired(itemWithDisplayable, cacheKeyWithDisplayableId, tokenCacheStore);

        final String cacheKeyWithNoUser = CacheKey.createCacheKeyForRTEntry(mAuthority, mResource, mClientId, "");
        final TokenCacheItem itemWithNoUser = tokenCacheStore.getItem(cacheKeyWithNoUser);
        count += tokenExpired(itemWithNoUser, cacheKeyWithNoUser, tokenCacheStore);

        return count;
    }

    private int tokenExpired(final TokenCacheItem item, final String key, final ITokenCacheStore tokenCacheStore) {
        final Calendar calendar = new GregorianCalendar();
        calendar.add(Calendar.HOUR, -2);
        final Date expiredTime = calendar.getTime();

        if (item != null && !TokenCacheItem.isTokenExpired(item.getExpiresOn())) {
            item.setExpiresOn(expiredTime);
            tokenCacheStore.setItem(key, item);
            return 1;
        }

        return 0;
    }

    private int invalidateRefreshToken() {
        expireAccessToken();

        int count = 0;
        // invalidate RT
        count += invalidateRefreshToken(CacheKey.createCacheKeyForRTEntry(mAuthority, mResource, mClientId, mUserId));
        count += invalidateRefreshToken(CacheKey.createCacheKeyForRTEntry(mAuthority, mResource, mClientId, mLoginHint));
        count += invalidateRefreshToken(CacheKey.createCacheKeyForRTEntry(mAuthority, mResource, mClientId, ""));

        // invalidate MRRT
        count += invalidateRefreshToken(CacheKey.createCacheKeyForMRRT(mAuthority, mClientId, mUserId));
        count += invalidateRefreshToken(CacheKey.createCacheKeyForMRRT(mAuthority, mClientId, mLoginHint));
        count += invalidateRefreshToken(CacheKey.createCacheKeyForMRRT(mAuthority, mClientId, ""));

        return count;
    }
    
    private int invalidateFamilyRefreshToken() {
        invalidateRefreshToken();

        int count  = 0;
        // invalidate FRT
        count += invalidateRefreshToken(CacheKey.createCacheKeyForFRT(mAuthority, AuthenticationConstants.MS_FAMILY_ID, mUserId));
        count += invalidateRefreshToken(CacheKey.createCacheKeyForFRT(mAuthority, AuthenticationConstants.MS_FAMILY_ID, mLoginHint));

        return count;
    }

    private int invalidateRefreshToken(final String key) {
        final ITokenCacheStore tokenCacheStore = mAuthenticationContext.getCache();
        final TokenCacheItem item = tokenCacheStore.getItem(key);

        if (item != null) {
            item.setRefreshToken(INVALID_REFRESH_TOKEN);
            tokenCacheStore.setItem(key, item);
            return 1;
        }

        return 0;
    }

    private AuthenticationCallback<AuthenticationResult> getAdalCallback() {
        return new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                final Intent intent = createIntentFromAuthenticationResult(authenticationResult);
                launchResultActivity(intent);
            }

            @Override
            public void onError(Exception e) {
                final Intent intent = createIntentFromReturnedException(e);
                launchResultActivity(intent);
            }
        };
    }

    private Intent createIntentFromAuthenticationResult(final AuthenticationResult result) {
        final Intent intent = new Intent();
        intent.putExtra(Constants.ACCESS_TOKEN, result.getAccessToken());
        intent.putExtra(Constants.REFRESH_TOKEN, result.getRefreshToken());
        intent.putExtra(Constants.ACCESS_TOKEN_TYPE, result.getAccessTokenType());
        intent.putExtra(Constants.EXPIRES_ON, result.getExpiresOn().getTime());
        intent.putExtra(Constants.TENANT_ID, result.getTenantId());
        intent.putExtra(Constants.ID_TOKEN, result.getIdToken());

        if (result.getUserInfo() != null) {
            final UserInfo userInfo = result.getUserInfo();
            intent.putExtra(Constants.UNIQUE_ID, userInfo.getUserId());
            intent.putExtra(Constants.DISPLAYABLE_ID, userInfo.getDisplayableId());
            intent.putExtra(Constants.GIVEN_NAME, userInfo.getGivenName());
            intent.putExtra(Constants.FAMILY_NAME, userInfo.getFamilyName());
            intent.putExtra(Constants.IDENTITY_PROVIDER, userInfo.getIdentityProvider());
        }

        return intent;
    }

    private Intent createIntentFromReturnedException(final Exception e) {
        final Intent intent = new Intent();
        if (!(e instanceof AuthenticationException)) {
            intent.putExtra(Constants.ERROR, "unknown_exception");
            intent.putExtra(Constants.ERROR_DESCRIPTION, "unknown exception returned");
        } else {
            final AuthenticationException authenticationException = (AuthenticationException) e;
            intent.putExtra(Constants.ERROR, authenticationException.getCode().toString());
            intent.putExtra(Constants.ERROR_DESCRIPTION, authenticationException.getLocalizedMessage());
            intent.putExtra(Constants.ERROR_CAUSE, authenticationException.getCause());
        }

        return intent;
    }

    private void launchResultActivity(final Intent intent) {
        intent.setClass(this.getApplicationContext(), ResultActivity.class);
        this.startActivity(intent);
        this.finish();
    }
}
