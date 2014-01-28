#Windows Azure Active Directory Authentication Library (ADAL) for Android
===========

The ADAL SDK for Android  gives you the ability to add Windows Azure Active Directory authentication to your application with just a few lines of additional code. Using our ADAL SDKs you can quickly and easily extend your existing application to all the employees that use Windows Azure AD and Active Directory on-premises using Active Directory Federation Services, including Office365 customers. This SDK gives your application the full functionality of Windows Azure AD, including industry standard protocol support for OAuth2, Web API integration, and two factor authentication support. Best of all, it’s FOSS (Free and Open Source Software) so that you can participate in the development process as we build these libraries.

[Refer to our Wiki](https://github.com/MSOpenTech/azure-activedirectory-library-for-android/wiki) for detailed walkthroughs on how to use this package including accessing a node.js REST API interface secured by Windows Azure Active Directory using the ADAL for Android.

## Latest Preview Release
---

We have released a Preview of the ADAL for iOS! [You can grab the release here] (https://github.com/MSOpenTech/azure-activedirectory-library-for-android/releases/tag/v0.5-alpha)

## Quick Start

1. Clone this repo and import code into your eclipse workspace
2. Add reference to your project as Android library
3. Add project dependency for debugging in your project settings
4. Update your proejct's AndroidManifest.xml file to include:
```Java
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <application
        android:allowBackup="true"
        android:debuggable="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme" >
        
        <activity
            android:name="com.microsoft.adal.AuthenticationActivity"
            android:label="@string/title_login_hello_app" >
        </activity>
  ....
  <application/>
```
5. Register your WEBAPI service app at Azure Active Directory(AAD),https://manage.windowsazure.com 
  * You need APP ID URI parameter to get token
6. Register your client native app at AAD
  * You need clientId and redirectUri parameters 
  * Select webapis in the list and give permission to previously registered(Step5) WebAPI 
7. Create an instance of AuthenticationContext at your main Activity. You can look at sample projects that is used for testing.
 
```Java
  mContext = new AuthenticationContext(MainActivity.this, authority, true); // This will use SharedPreferences as default cache
```
  mContext is a field in your activity
8. Copy this code block to handle the end of AuthenticationActivity after user enters credentials and receives authorization code:
```Java
 @Override
 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
     super.onActivityResult(requestCode, resultCode, data);
     if (mContext != null) {
         mContext.onActivityResult(requestCode, resultCode, data);
     }
 }
```
9. To ask for a token, you need to define a callback:
```Java
private AuthenticationCallback<AuthenticationResult> callback = new AuthenticationCallback<AuthenticationResult>() {

        @Override
        public void onError(Exception exc) {
            if (exc instanceof AuthenticationException) {
                textViewStatus.setText("Cancelled");
                Log.d(TAG, "Cancelled");
            } else {
                textViewStatus.setText("Authentication error:" + exc.getMessage());
                Log.d(TAG, "Authentication error:" + exc.getMessage());
            }
        }

        @Override
        public void onSuccess(AuthenticationResult result) {
            mResult = result;

            if (result == null || result.getAccessToken() == null
                    || result.getAccessToken().isEmpty()) {
                textViewStatus.setText("Token is empty");
                Log.d(TAG, "Token is empty");
            } else {
                // request is successful
                Log.d(TAG, "Status:" + result.getStatus() + " Expired:"
                        + result.getExpiresOn().toString());
                textViewStatus.setText(PASSED);
            }
        }
    };
```
10. Ask for a token:
```Java
 mContext.acquireToken(MainActivity.this, resource, clientId, redirect, userid, PromptBehavior.Auto, "",
                callback);
```
  * Resource is required, Clientid is required. You can setup redirectUri as your packagename and it is not required to be provided for acquireToken call. PromptBehavior helps to ask for credentials to skip cache and cookie. Callback ill be called after authorization code is exchanged for a token. It will have an object of AuthenticationResult, which has accesstoken, date expired, and idtoken info. 
11. You can always call **acquireToken** to handle caching, token refresh and credential prompt if required. Your callback implementation should handle the user cancellation for AuthenticationActivity. ADAL will return a cancellation error, if user cancels the credential entry.


## Usage


### Querying cache items
ADAL provides Default cache in SharedPrefrecens with some simple cache query fucntions. You can get the current cache from AuthenticationContext with:
```Java
 ITokenCacheStore cache = mContext.getCache();
```
You can also provide your cache implementation, if you want to customize it.
```Java
mContext = new AuthenticationContext(MainActivity.this, authority, true, yourCache);
```

### Logger
ADAL provides simple callback logger. You can set your callback for logging.
```Java
Logger.getInstance().setExternalLogger(new ILogger() {
    @Override
    public void Log(String tag, String message, String additionalMessage, LogLevel level, ADALError errorCode) {
    ...
    }
}
// you can manage min log level as well
Logger.getInstance().setLogLevel(Logger.LogLevel.Verbose);
```

### Oauth2 Bearer challange
AuthenticationParameters class provides functionality to get the authorization_uri from Oauth2 bearer challange.


## License

Copyright (c) Microsoft Open Technologies, Inc.  All rights reserved. Licensed under the Apache License, Version 2.0 (the "License"); 
