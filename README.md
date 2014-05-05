#Windows Azure Active Directory Authentication Library (ADAL) for Android
===========

The ADAL SDK for Android  gives you the ability to add Windows Azure Active Directory authentication to your application with just a few lines of additional code. Using our ADAL SDKs you can quickly and easily extend your existing application to all the employees that use Windows Azure AD and Active Directory on-premises using Active Directory Federation Services, including Office365 customers. This SDK gives your application the full functionality of Windows Azure AD, including industry standard protocol support for OAuth2, Web API integration, and two factor authentication support. Best of all, it’s FOSS (Free and Open Source Software) so that you can participate in the development process as we build these libraries. 

## Latest Preview Release

We have released a Preview of the ADAL for Android! [You can grab the release here] (https://github.com/MSOpenTech/azure-activedirectory-library-for-android/releases/tag/v0.6-alpha)
You can also get it from maven repo.

### Prerequisites

* Maven 3.1.1+
* Git
* Android SDK
* AVD image running (API level 14) or higher.
* Android SDK with *ALL* packages installed
* You may use any IDE that supports Maven. Eclipse ADT will work fine after you complete prereq step.


#### Setup Maven Android SDK Deployer

Some of the Android libraries are not at the maven repo, so you need to have the [Android Maven SDK Deployer](https://github.com/mosabua/maven-android-sdk-deployer) installed and configured. You can read at the Android Maven SDK Deployer GitHub in depth install guide.

Before you run the SDK Deployer, you should have installed ALL PACKAGES in the Android SDK.  Once that has finished, you may run the following.

    git clone https://github.com/mosabua/maven-android-sdk-deployer.git
    cd maven-android-sdk-deployer\platforms\android-19
    mvn clean install
    cd ..\..\extras\compatibility-v4
    mvn clean install

Now Maven will have android-19 and support-v4 as dependencies in local m2 repo.

#### Install This Repo

You can clone and install from cmd line:

    git clone https://github.com/MSOpenTech/azure-activedirectory-library-for-android.git
    cd azure-activedirectory-library-for-android
    mvn clean install

## Usage

1. Follow Prerequisites
2. Add reference to your project as Android library. Please check here: http://developer.android.com/tools/projects/projects-eclipse.html
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
  // Authority is in the form of https://login.windows.net/yourtenant.onmicrosoft.com
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
  * Resource is required, Clientid is required. You can setup redirectUri as your packagename and it is not required to be provided for acquireToken call. PromptBehavior helps to ask for credentials to skip cache and cookie. Callback will be called after authorization code is exchanged for a token. It will have an object of AuthenticationResult, which has accesstoken, date expired, and idtoken info. 
11. You can always call **acquireToken** to handle caching, token refresh and credential prompt if required. Your callback implementation should handle the user cancellation for AuthenticationActivity. ADAL will return a cancellation error, if user cancels the credential entry.

### Customization
Library project resources can be overwritten by your app resources. This happens when app is building. It means that you can customize Authentication Activity layout the way you want. You need to make sure to keep id of two controls that ADAL uses(Webview and button).

### Authority Url and ADFS
ADFS is not recognized as production STS, so you need to turn of instance discovery and pass false at AuthenticationContext constructor.

Authority url needs STS instance and tenant name: https://login.windows.net/yourtenant.onmicrosoft.com

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
### Maven Sample project to run on a device
If you want to build with Maven, you can use the pom.xml at top level
  * Follow the steps at Prerequests section to setup your maven for android
  * Setup emulator with SDK 18
  * go to root folder
  * mvn clean install
  * cd samples\hello
  * mvn install android:deploy android:install
  * You should see app launching
  * Enter test user credentials to try

### Encryption
ADAL encrypts the tokens and store in SharedPreferences by default. You can look at the StorageHelper class to see the details.

### Oauth2 Bearer challange
AuthenticationParameters class provides functionality to get the authorization_uri from Oauth2 bearer challange.


## License

Copyright (c) Microsoft Open Technologies, Inc.  All rights reserved. Licensed under the Apache License, Version 2.0 (the "License"); 
