#Windows Azure Active Directory Authentication Library (ADAL) for Android
===========

The ADAL SDK for Android  gives you the ability to add Windows Azure Active Directory authentication to your application with just a few lines of additional code. Using our ADAL SDKs you can quickly and easily extend your existing application to all the employees that use Windows Azure AD and Active Directory on-premises using Active Directory Federation Services, including Office365 customers. This SDK gives your application the full functionality of Windows Azure AD, including industry standard protocol support for OAuth2, Web API integration, and two factor authentication support. Best of all, it’s FOSS (Free and Open Source Software) so that you can participate in the development process as we build these libraries. 

## Release

We have released a GA of the ADAL for Android! [You can grab the release here] (https://github.com/MSOpenTech/azure-activedirectory-library-for-android/releases)
You can also get it from maven repo.

## Features
* Oauth2 protocol
* IdToken
* Multi resource refresh token
* Cache
* Encryption
* Broker support(Future)
* 401 Authenticatio challange response in AuthenticationParameters

## Getting Started
This is an Android library with resources, so you have multiple options to use in your project. You could get source code and import project into Eclipse to link to your app. You can use *aar* package format in AndroidStudio if you want to reference binaries.

##Download
###Option 1: Source Zip

To download a copy of the source code, click "Download ZIP" on the right side of the page or click [here](https://github.com/MSOpenTech/azure-activedirectory-library-for-android/archive/master.zip).

###Option 2: Source via Git

To get the source code of the SDK via git just type:

    git clone https://github.com/MSOpenTech/azure-activedirectory-library-for-android.git
    cd ./azure-activedirectory-library-for-android/src

###Option 3: Binaries via Gradle

You can get the binaries from Maven central repo. AAR package can be included as follows in your project at AndroidStudio:

```gradle 
repositories {
    mavenCentral()
    flatDir {
        dirs 'libs'
    }
    maven {
        url "YourLocalMavenRepoPath\\.m2\\repository"
    }
}
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    compile ('com.microsoft.aad:adal:1.0.0')
}
```

###Option 4: aar via Maven

If you are using m2e plugin in Eclipse, you can specify the dependency in your pom.xml file:

```xml
<dependency>
    <groupId>com.microsoft.aad</groupId>
    <artifactId>adal</artifactId>
    <version>1.0.0</version>
    <type>aar</type>
</dependency>
```
####Maven Sample project to run on a device
If you want to build with Maven, you can use the pom.xml at top level
  * Follow the steps at [Prerequests section to setup your maven for android](https://github.com/MSOpenTech/azure-activedirectory-library-for-android/wiki/Setting-up-maven-environment-for-Android)
  * Setup emulator with SDK 18
  * go to root folder
  * mvn clean install
  * cd samples\hello
  * mvn android:deploy android:run
  * You should see app launching
  * Enter test user credentials to try

Jar packages will be also submitted beside aar package.

###Option 5: jar package inside libs folder
You can get the jar file from maven repo and drop into *libs* folder in  your project. You need to copy required resources to your project as well since jar packages don't include them.

### Prerequisites

* Maven 3.1.1+
* Git
* Android SDK
* AVD image running (API level 14) or higher.
* Android SDK with *ALL* packages installed
* You may use any IDE that supports Maven. Eclipse ADT will work fine after you complete prereq step.

## Usage as Android Library

1. Follow Prerequisites
2. Add reference to your project as Android library. Please check here: http://developer.android.com/tools/projects/projects-eclipse.html
3. Add project dependency for debugging in your project settings
4. Update your project's AndroidManifest.xml file to include:
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
      mContext = new AuthenticationContext(MainActivity.this, authority, true); // This will use SharedPreferences as            default cache
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
     mContext.acquireToken(MainActivity.this, resource, clientId, redirect, user_loginhint, PromptBehavior.Auto, "",
                    callback);
    ```
  * Resource is required, Clientid is required. You can setup redirectUri as your packagename and it is not required to be provided for acquireToken call. PromptBehavior helps to ask for credentials to skip cache and cookie. Callback ill be called after authorization code is exchanged for a token. It will have an object of AuthenticationResult, which has accesstoken, date expired, and idtoken info. 

11. **acquireTokenSilent**
You can call **acquireTokenSilent** to handle caching, and token refresh. It provides sync version as well.
 
    ```Java
     mContext.acquireTokenSilent(resource, clientid, userId, callback );
    ```

## Development
You can clone the repo and start contributing. if you want to setup maven enviroment please [check this](https://github.com/MSOpenTech/azure-activedirectory-library-for-android/wiki/Setting-up-maven-environment-for-Android)
       

## Customization
Library project resources can be overwritten by your app resources. This happens when app is building. It means that you can customize Authentication Activity layout the way you want. You need to make sure to keep id of controls that ADAL uses(Webview).

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

### PromptBehavior
ADAL provides option to specifiy prompt behavior. PromptBehavior.Auto will pop up UI if refresh token is invalid and user credentials are required. PromptBehavior.Always will skip the cache usage and always show UI.

### Silent token request from cache and refresh
This method does not use UI pop up and not require activity. It will return token from cache if available. If token is expired, it will try to refresh it. If refresh token is expired or failed, it will return AuthenticationException.
```Java
Future<AuthenticationResult> result = mContext.acquireTokenSilent(resource, clientid, userId, callback );
```
You can also make sync call with this method. You can set null to callback or use acquireTokenSilentSync.

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


### Encryption
ADAL encrypts the tokens and store in SharedPreferences by default. You can look at the StorageHelper class to see the details. Android introduced AndroidKeyStore for 4.3(API18) secure storage of private keys. ADAL uses that for API18 and above. If you want to use ADAL for lower SDK versions, you need to provide secret key at AuthenticationSettings.INSTANCE.setSecretKey

### Oauth2 Bearer challange
AuthenticationParameters class provides functionality to get the authorization_uri from Oauth2 bearer challange.

### Session cookies in Webview
Android webview does not clear session cookies after app is closed. You can handle this with sample code below:
```java
CookieSyncManager.createInstance(getApplicationContext());
CookieManager cookieManager = CookieManager.getInstance();
cookieManager.removeSessionCookie();
CookieSyncManager.getInstance().sync();
```
More about cookies: http://developer.android.com/reference/android/webkit/CookieSyncManager.html

=======

## License

Copyright (c) Microsoft Open Technologies, Inc.  All rights reserved. Licensed under the Apache License, Version 2.0 (the "License"); 
