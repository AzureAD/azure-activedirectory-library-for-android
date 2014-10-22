#Microsoft Azure Active Directory Authentication Library (ADAL) for Android
===========

The ADAL SDK for Android gives you the ability to add support for Work Accounts to your application with just a few lines of additional code. This SDK gives your application the full functionality of Microsoft Azure AD, including industry standard protocol support for OAuth2, Web API integration with user level consent, and two factor authentication support. Best of all, it’s FOSS (Free and Open Source Software) so that you can participate in the development process as we build these libraries. 

**What is a Work Account?**

A Work Account is an identity you use to get work done no matter if at your business or on a college campus. Anywhere you need to get access to your work life you'll use a Work Account. The Work Account can be tied to an Active Directory server running in your datacenter or live completely in the cloud like when you use Office365. A Work Account will be how your users know that they are accessing their important documents and data backed my Microsoft security.

## ADAL for Android 1.0 Released!

Thanks to all your great feedback over the preview period, we have released 1.0 (GA) of the Microsoft Azure Active Directory Library for Android! 

## Features
* Industry standard Oauth2 protocol support.
* IdToken exposure for full access to the token contents.
* Multi resource refresh token allows for apps registered together to access different APIs without prompting the user.
* Cache with Encryption for easily accessing existing tokens and session state with assurance it wasn't tampered with.
* Support for the Microsoft Azure AD Authenticator plug-in for Android, which will be released soon!

## Samples and Documentation

[We provide a full suite of sample applications and documentation on GitHub](https://github.com/AzureADSamples) to help you get started with learning the Azure Identity system. This includes tutorials for native clients such as Windows, Windows Phone, iOS, OSX, Android, and Linux. We also provide full walkthroughs for authentication flows such as OAuth2, OpenID Connect, Graph API, and other awesome features. 

Visit your Azure Identity samples for Android is here: [https://github.com/AzureADSamples/NativeClient-Android](https://github.com/AzureADSamples/NativeClient-Android)

## Community Help and Support

We leverage [Stack Overflow](http://stackoverflow.com/) to work with the community on supporting Azure Active Directory and its SDKs, including this one! We highly recommend you ask your questions on Stack Overflow (we're all on there!) Also browser existing issues to see if someone has had your question before. 

We recommend you use the "adal" tag so we can see it! Here is the latest Q&A on Stack Overflow for ADAL: [http://stackoverflow.com/questions/tagged/adal](http://stackoverflow.com/questions/tagged/adal)

## Contributing

All code is licensed under the Apache 2.0 license and we triage actively on GitHub. We enthusiastically welcome contributions and feedback. You can clone the repo and start contributing now. if you want to setup a maven enviroment please [check this](https://github.com/MSOpenTech/azure-activedirectory-library-for-android/wiki/Setting-up-maven-environment-for-Android)

## Versions
Please check the releases for updates.

## Quick Start

To build with Maven, you can use the pom.xml at top level

  * Clone this repo in to a directory of your choice
  * Follow the steps at [Prerequests section to setup your maven for android](https://github.com/MSOpenTech/azure-activedirectory-library-for-android/wiki/Setting-up-maven-environment-for-Android)
  * Setup emulator with SDK 19
  * Go to the root folder where you cloned this repo
  * Run the command: mvn clean install
  * Change the directory to the Quick Start sample: cd samples\hello
  * Run the command: mvn android:deploy android:run
  * You should see app launching
  * Enter test user credentials to try!

Jar packages will be also submitted beside the aar package.

##Download

We've made it easy for you to have multiple options to use this library in your Android project:

* You can use the source code to import this library into Eclipse and link to your application. 
* If using Android Studio, you can use *aar* package format and reference the binaries.

###Option 1: Source Zip

To download a copy of the source code, click "Download ZIP" on the right side of the page or click [here](https://github.com/AzureAD/azure-activedirectory-library-for-android/archive/v1.0.0.tar.gz).

###Option 2: Source via Git

To get the source code of the SDK via git just type:

    git clone git@github.com:AzureAD/azure-activedirectory-library-for-android.git
    cd ./azure-activedirectory-library-for-android/src

###Option 3: Binaries via Gradle

You can get the binaries from Maven central repo. AAR package can be included as follows in your project in AndroidStudio:

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

If you are using the m2e plugin in Eclipse, you can specify the dependency in your pom.xml file:

```xml
<dependency>
    <groupId>com.microsoft.aad</groupId>
    <artifactId>adal</artifactId>
    <version>1.0.0</version>
    <type>aar</type>
</dependency>
```

###Option 5: jar package inside libs folder
You can get the jar file from maven the repo and drop into the *libs* folder in your project. You need to copy the required resources to your project as well since the jar packages don't include them.

## Prerequisites

* Maven 3.1.1+
* Git
* Android SDK
* AVD image running (API level 14) or higher.
* Android SDK with *ALL* packages installed
* You may use any IDE that supports Maven. Eclipse ADT will work fine after you complete prereq step.

## How To use this library

1. Follow the Prerequisites

2. Add a reference to your project and specify it as an Android library. If you are uncertain how to do this, click here for more information: http://developer.android.com/tools/projects/projects-eclipse.html

3. Add the project dependency for debugging in to your project settings

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
                android:name="com.microsoft.aad.adal.AuthenticationActivity"
                android:label="@string/title_login_hello_app" >
            </activity>
      ....
      <application/>
    ```

5. Register your WEBAPI service app in Azure Active Directory (AAD). If you're not sure what a tenant is or how you would get one, read [What is a Microsoft Azure AD tenant](http://technet.microsoft.com/library/jj573650.aspx)? or [Sign up for Microsoft Azure as an organization](http://www.windowsazure.com/en-us/manage/services/identity/organizational-account/). These docs should get you started on your way to using Windows Azure AD.
  * NOTE: You need to write down the APP ID URI for the next steps
6. Register your client native app at AAD. Select webapis in the list and give permission to previously registered WebAPI. If you need help with this step, see: [Register the REST API Service Windows Azure Active Directory](https://github.com/AzureADSamples/WebAPI-Nodejs/wiki/Setup-Windows-Azure-AD)
  * NOTE: You will need to write down the clientId and redirectUri parameters for the next steps.
7. Create an instance of AuthenticationContext at your main Activity. The details of this call are beyond the scope of this README, but you can get a good start by looking at the [Android Native Client Sample](https://github.com/AzureADSamples/NativeClient-Android). Below is an example:
 
    ```Java
    // Authority is in the form of https://login.windows.net/yourtenant.onmicrosoft.com
    mContext = new AuthenticationContext(MainActivity.this, authority, true); // This will use SharedPreferences as            default cache
    ```
  * NOTE: mContext is a field in your activity
 
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

9. To ask for a token, you define a callback

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
10. Finally, ask for a token using that callback:

    ```Java
     mContext.acquireToken(MainActivity.this, resource, clientId, redirect, user_loginhint, PromptBehavior.Auto, "",
                    callback);
    ```
    
Explanation of the parameters:
    
  * Resource is required and is the resource you are trying to access.
  * Clientid is required and comes from the AzureAD Portal.
  * You can setup redirectUri as your packagename. It is not required to be provided for the acquireToken call.
  * PromptBehavior helps to ask for credentials to skip cache and cookie. 
  * Callback will be called after authorization code is exchanged for a token. 
  
  The Callback will have an object of AuthenticationResult which has accesstoken, date expired, and idtoken info. 

Optional:  **acquireTokenSilent**

You can call **acquireTokenSilent** to handle caching, and token refresh. It provides sync version as well. It accepts userid as paremeter.
 
    ```java
     mContext.acquireTokenSilent(resource, clientid, userId, callback );
    ```
11. Broker:
  Microsoft Intune's Company portal app will provide the broker component. Adal will use the broker account, if there is one user account is created at this authenticator and Developer choose not to skip it. Developer can skip the broker user with:

    ```java
    AuthenticationSettings.Instance.setSkipBroker(true);
    ```
 Developer needs to register special redirectUri for broker usage. RedirectUri is in the format of msauth:<UrlEncoded packagename>%2C<Urlencoded signature>. You can get your redirecturi for your app using the script "brokerRedirectPrint.ps1" or use API call mContext.getBrokerRedirectUri. Signature is related to your signing certificates.
 
 
Using this walkthrough, you should have what you need to successfully integrate with Azure Active Directory. For more examples of this working, viist the AzureADSamples/ repository on GitHub.
       
## Important Information

### Customization

Library project resources can be overwritten by your application resources. This happens when your app is building. For this reason, you can customize Authentication Activity layout the way you want. You need to make sure to keep the id of the controls that ADAL uses(Webview).

### Broker
Broker component will be delivered with Intune's Company portal app. Account will be created in Account Manager. Account type is "com.microsoft.workaccount". It only allows single SSO account. It will create SSO cookie for this user after completing device challange for one of the apps. 

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

This method does not use UI pop up and not require an activity. It will return token from cache if available. If token is expired, it will try to refresh it. If refresh token is expired or failed, it will return AuthenticationException.

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

### Resource Overrides

The ADAL library includes English strings for the following two ProgressDialog messages.

Your application should overwrite them if localized strings are desired. 

```Java
<string name="app_loading">Loading...</string>
<string name="broker_processing">Broker is processing</string>
```

=======

## License

Copyright (c) Microsoft Open Technologies, Inc.  All rights reserved. Licensed under the Apache License, Version 2.0 (the "License"); 
