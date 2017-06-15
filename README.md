#Microsoft Azure Active Directory Authentication Library (ADAL) for Android
===========

The ADAL SDK for Android gives you the ability to add support for Work Accounts to your application with just a few lines of additional code. This SDK gives your application the full functionality of Microsoft Azure AD, including industry standard protocol support for OAuth2, Web API integration with user level consent, and two factor authentication support. Best of all, it’s FOSS (Free and Open Source Software) so that you can participate in the development process as we build these libraries. 

A Work Account is an identity you use to get work done no matter if at your business or on a college campus. Anywhere you need to get access to your work life you'll use a Work Account. The Work Account can be tied to an Active Directory server running in your datacenter or live completely in the cloud like when you use Office365. A Work Account will be how your users know that they are accessing their important documents and data backed my Microsoft security.

## ADAL for Android 1.12.0 Released!

## Build status
| Branch  | Status |
| ------------- | ------------- |
| dev (Travis)  | [![Build Status](https://travis-ci.org/AzureAD/azure-activedirectory-library-for-android.svg?branch=master)](https://travis-ci.org/AzureAD/azure-activedirectory-library-for-android) |
| dev (VSTS)  | [![Build status](https://identitydivision.visualstudio.com/_apis/public/build/definitions/a7934fdd-dcde-4492-a406-7fad6ac00e17/94/badge)](https://identitydivision.visualstudio.com/IDDP/_build/index?definitionId=94&_a=completed) |

Note: A corpnet account is required to view the VSTS build.

## Versions
Current version - 1.12.0
Minimum recommended version - 1.1.16  
You can find the changes for each version in the [change log](https://github.com/AzureAD/azure-activedirectory-library-for-android/blob/master/changelog.txt).

## Features
* Industry standard Oauth2 protocol support.
* IdToken exposure for full access to the token contents.
* Multi resource refresh token allows for apps registered together to access different APIs without prompting the user.
* Cache with Encryption for easily accessing existing tokens and session state with assurance it wasn't tampered with.
* Support for the Microsoft Azure AD Authenticator plug-in for Android, which will be released soon!
* Dialog and Fragment support

## Samples and Documentation

[We provide a full suite of sample applications and documentation on GitHub](https://github.com/AzureADSamples) to help you get started with learning the Azure Identity system. This includes tutorials for native clients such as Windows, Windows Phone, iOS, OSX, Android, and Linux. We also provide full walkthroughs for authentication flows such as OAuth2, OpenID Connect, Graph API, and other awesome features. 

Visit your Azure Identity samples for Android is here: [https://github.com/AzureADSamples/NativeClient-Android](https://github.com/AzureADSamples/NativeClient-Android)

Xamarin related info is here:
[https://github.com/AzureADSamples/NativeClient-Xamarin-Android](https://github.com/AzureADSamples/NativeClient-Xamarin-Android)

## Community Help and Support

We leverage [Stack Overflow](http://stackoverflow.com/) to work with the community on supporting Azure Active Directory and its SDKs, including this one! We highly recommend you ask your questions on Stack Overflow (we're all on there!) Also browser existing issues to see if someone has had your question before. 

We recommend you use the "adal" tag so we can see it! Here is the latest Q&A on Stack Overflow for ADAL: [http://stackoverflow.com/questions/tagged/adal](http://stackoverflow.com/questions/tagged/adal)

## SSO and Conditional Access Support

This library allows your application to support our [Enterprise Mobility Suite](https://www.microsoft.com/en-us/cloud-platform/enterprise-mobility-security), including [Conditional Access](https://www.microsoft.com/en-us/cloud-platform/conditional-access), so businesses can use your application in their secure environment. 

To configure your application to support these scenarios, please read this document: [How to enable cross-app SSO on Android using ADAL](https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-sso-android)

## Security Reporting

If you find a security issue with our libraries or services please report it to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as possible. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to GitHub Issues or any other public site. We will contact you shortly upon receiving the information. We encourage you to get notifications of when security incidents occur by visiting [this page](https://technet.microsoft.com/en-us/security/dd252948) and subscribing to Security Advisory Alerts.

## Contributing

All code is licensed under the Apache 2.0 license and we triage actively on GitHub. We enthusiastically welcome contributions and feedback. You can clone the repo and start contributing now. if you want to setup a maven enviroment please [check this](https://github.com/MSOpenTech/azure-activedirectory-library-for-android/wiki/Setting-up-maven-environment-for-Android)
More details [about contribution](https://github.com/AzureAD/azure-activedirectory-library-for-android/blob/master/contributing.md) 

## Versions
Please check the releases for updates.

## Quick Start
To build with Gradle,

  * Clone this repo in to a directory of your choice
  * Setup emulator with SDK 23
  * Go to the root folder where you cloned this repo
  * To run the sample app, connect the test device and run the command: ./gradlew :sample:installDebug
  * You should see app 'hello' installed in the test device
  * Enter test user credentials to try

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

## Download

We've made it easy for you to have multiple options to use this library in your Android project:

* You can use the source code to import this library into Android Studio and link to your application. 
* If using Android Studio, you can use *aar* package format and reference the binaries.

### Option 1: Source Zip

To download a copy of the source code, click "Download ZIP" on the right side of the page or click [here](https://github.com/AzureAD/azure-activedirectory-library-for-android/archive/v1.1.5.tar.gz).

### Option 2: Source via Git

To get the source code of the SDK via git just type:

    git clone git@github.com:AzureAD/azure-activedirectory-library-for-android.git
    cd ./azure-activedirectory-library-for-android/src

### Option 3: Binaries via Gradle

You can get the binaries from Maven central repo. AAR package can be included as follows in your project in AndroidStudio:

```gradle 
repositories {
    mavenCentral()
}
dependencies {
    // your dependencies here...
    compile('com.microsoft.aad:adal:1.12.+') {
        // if your app includes android support
        // libraries or Gson in its dependencies
        // exclude that groupId from ADAL's compile
        // task by un-commenting the appropriate
        // line below

        // exclude group: 'com.android.support'
        // exclude group: 'com.google.code.gson'
    }
}
```

### Option 4: aar via Maven

If you are using the m2e plugin in Eclipse, you can specify the dependency in your pom.xml file:

```xml
<dependency>
    <groupId>com.microsoft.aad</groupId>
    <artifactId>adal</artifactId>
    <version>1.12.0</version>
    <type>aar</type>
</dependency>
```

### Option 5: jar package inside libs folder
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

```xml
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

    ```java
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
    
    If you're implementing your authentication logic in a Fragment, you'll need to wrap it in a `IWindowComponent` before passing it as a parameter like this:

```java
mContext.acquireToken(wrapFragment(MainFragment.this), resource, clientId, redirect, user_loginhint, PromptBehavior.Auto, "",
                    callback);

private IWindowComponent wrapFragment(final Fragment fragment){
    return new IWindowComponent() {
        Fragment refFragment = fragment;
        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            refFragment.startActivityForResult(intent, requestCode);
        }
    };
}
```

   Explanation of the parameters(Example of those parameters could be found at [Android Native Client Sample](https://github.com/AzureADSamples/NativeClient-Android)):    * Resource is required and is the resource you are trying to access.
   * Clientid is required and comes from the AzureAD Portal.
   * You can setup redirectUri as your packagename. It is not required to be provided for the acquireToken call.
   * PromptBehavior helps to ask for credentials to skip cache and cookie. 
   * Callback will be called after authorization code is exchanged for a token. 

    The Callback will have an object of AuthenticationResult which has accesstoken, date expired, and idtoken info.

**acquireTokenSilentSync**

In order to get token back without prompt, you can call **acquireTokenSilentSync** which handles caching, and token refresh without UI prompt. It provides async version as well. **Note:** userId required in silent call is the one you get back from the interactive call) as parameter.

```java
mContext.acquireTokenSilentSync(String resource, String clientId, String userId);
```
or 
```java
mContext.acquireTokenSilent(String resource, String clientId, String userId, final AuthenticationCallback<AuthenticationResult> callback);
```
	
11. Broker:

	Microsoft Intune's Company portal App and Azure Authenticator App will provide the broker component. 
	In order to acquire token via broker, the following requirements have to meet(Please check samples\userappwithbroker for authentication via broker):
	* Starting version 1.1.14, developer has to explicitly specify set to use broker via:
		`AuthenticationSettings.INSTANCE.setUseBroker(true);`
	* Developer needs to register special redirectUri for broker usage. RedirectUri is in the format of msauth://packagename/Base64UrlencodedSignature. You can get your redirecturi for your app using the script `brokerRedirectPrint.ps1` on Windows or `brokerRedirectPrint.sh` on Linux or Mac. You can also use API call mContext.getBrokerRedirectUri. Signature is related to your signing certificates.
	* If target version is lower than 23, calling app has to have the following permissions declared in manifest(http://developer.android.com/reference/android/accounts/AccountManager.html):
		* GET_ACCOUNTS
		* USE_CREDENTIALS
		* MANAGE_ACOUNTS
	* If target version is 23, USE_CREDENTIALS and MANAGE_ACCOUNTS are already deprecated. But GET_ACCOUNTS is under protection level "dangerous", calling app is responsible for requesting the run-time permisson. You can      reference [Runtime permission request for API 23](http://developer.android.com/training/permissions/requesting.html).
	* There must be an account existed and registered via one of the two broker apps.
	
	AuthenticationContext provides API method to get the broker user. 
	
	`String brokerAccount =  mContext.getBrokerUser();`
	
	Broker user will be returned if account is valid. 

Using this walkthrough, you should have what you need to successfully integrate with Azure Active Directory. For more examples of this working, visit the AzureADSamples/ repository on GitHub.
       
## Important Information

### Customization

Library project resources can be overwritten by your application resources. This happens when your app is building. For this reason, you can customize Authentication Activity layout the way you want. You need to make sure to keep the id of the controls that ADAL uses(Webview).

### Broker

Broker component will be delivered with Intune's Company portal app. Account will be created in Account Manager. Account type is "com.microsoft.workaccount". It only allows single SSO account. It will create SSO cookie for this user after completing device challange for one of the apps. 

### Authority Url and ADFS

ADFS is not recognized as production STS, so you need to turn of instance discovery and pass false at AuthenticationContext constructor.

Authority url needs STS instance and tenant name: https://login.windows.net/yourtenant.onmicrosoft.com

### Federated sign-in failure if additional certificate downloads are required

Federated sign-in may fail when attempting to authenticate using the Azure Active Directory Authentication Library (ADAL) for Android. See [Using ADAL to authenticate from Android devices fails if additional certificate downloads are required](https://support.microsoft.com/en-us/help/3203929/using-adal-to-authenticate-from-android-devices-fails-if-additional-certificate-downloads-are-required) for more information. 

### Querying cache items

ADAL provides Default cache in SharedPrefrecens with some simple cache query fucntions. You can get the current cache from AuthenticationContext with:

```java
ITokenCacheStore cache = mContext.getCache();
```

You can also provide your cache implementation, if you want to customize it.

```java
mContext = new AuthenticationContext(MainActivity.this, authority, true, yourCache);
```

### PromptBehavior

ADAL provides option to specifiy prompt behavior. PromptBehavior.Auto will pop up UI if refresh token is invalid and user credentials are required. PromptBehavior.Always will skip the cache usage and always show UI.

### Silent token request from cache and refresh

This method does not use UI pop up and not require an activity. It will return token from cache if available. If token is expired, it will try to refresh it. If refresh token is expired or failed, it will return AuthenticationException.

```java
Future<AuthenticationResult> result = mContext.acquireTokenSilent(resource, clientid, userId, callback );
```
    
You can also make sync call with this method. You can set null to callback or use acquireTokenSilentSync.

### Diagnostics

The following are the primary sources of information for diagnosing issues:

+ Exceptions
+ Logs
+ Network traces

Also, note that correlation IDs are central to the diagnostics in the library. You can set your correlation IDs on a per request basis if you want to correlate an ADAL request with other operations in your code. If you don't set a correlations id then ADAL will generate a random one and all log messages and network calls will be stamped with the correlation id. The self generated id changes on each request.

#### Exceptions

This is obviously the first diagnostic. We try to provide helpful error messages. If you find one that is not helpful please file an issue and let us know. Please also provide device information such as model and SDK#.

#### Logs

You can configure the library to generate log messages that you can use to help diagnose issues. You configure logging by making the following call to configure a callback that ADAL will use to hand off each log message as it is generated.


 ```java
 Logger.getInstance().setExternalLogger(new ILogger() {
     @Override
     public void Log(String tag, String message, String additionalMessage, LogLevel level, ADALError errorCode) {
      ...
      // You can write this to logfile depending on level or errorcode.
      writeToLogFile(getApplicationContext(), tag +":" + message + "-" + additionalMessage);
     }
 }
 ```
Messages can be written to a custom log file as seen below. Unfortunately, there is no standard way of getting logs from a device. There are some services that can help you with this. You can also invent your own, such as sending the file to a server.

```java
private syncronized void writeToLogFile(Context ctx, String msg) {      
       File directory = ctx.getDir(ctx.getPackageName(), Context.MODE_PRIVATE);
       File logFile = new File(directory, "logfile");
       FileOutputStream outputStream = new FileOutputStream(logFile, true);
       OutputStreamWriter osw = new OutputStreamWriter(outputStream);
       osw.write(msg);
       osw.flush();
       osw.close(); 
}
```

##### Logging Levels

+ Error(Exceptions)
+ Warn(Warning)
+ Info(Information purposes)
+ Verbose(More details)

You set the log level like this:
`Logger.getInstance().setLogLevel(Logger.LogLevel.Verbose);`
 
 All log messages are sent to logcat in addition to any custom log callbacks.
 You can get log to a file form logcat as shown below:
 `adb logcat > "C:\logmsg\logfile.txt"`
 
 More examples about adb cmds: https://developer.android.com/tools/debugging/debugging-log.html#startingLogcat
 
#### Network Traces

You can use various tools to capture the HTTP traffic that ADAL generates.  This is most useful if you are familiar with the OAuth protocol or if you need to provide diagnostic information to Microsoft or other support channels.

Fiddler is the easiest HTTP tracing tool.  Use the following links to setup it up to correctly record ADAL network traffic.  In order to be useful it is necessary to configure fiddler, or any other tool such as Charles, to record unencrypted SSL traffic.  NOTE: Traces generated in this way may contain highly privileged information such as access tokens, usernames and passwords.  If you are using production accounts, do not share these traces with 3rd parties.  If you need to supply a trace to someone in order to get support, reproduce the issue with a temporary account with usernames and passwords that you don't mind sharing.

+ [Setting Up Fiddler For Android](http://docs.telerik.com/fiddler/configure-fiddler/tasks/ConfigureForAndroid)
+ [Configure Fiddler Rules For ADAL](https://github.com/AzureAD/azure-activedirectory-library-for-android/wiki/How-to-listen-to-httpUrlConnection-in-Android-app-from-Fiddler)


### Dialog mode
acquireToken method without activity supports dialog prompt.

### Encryption

ADAL encrypts the tokens and store in SharedPreferences by default. You can look at the StorageHelper class to see the details. ADAL uses AndroidKeyStore for 4.3(API18) and above for secure storage of private keys. If you want to use ADAL for lower SDK versions, you need to **provide secret key at AuthenticationSettings.INSTANCE.setSecretKey**

Following example is using the password based encryption key(which takes the specified password and salt). And then create the provider-independent secret key with the generated password based encryption key. ADAL requires the key to be 256 bits. You can use other key generation algorithm.

```java
    SecretKeyFactory keyFactory = SecretKeyFactory
		.getInstance("PBEWithSHA256And256BitAES-CBC-BC");
    SecretKey generatedSecretKey = keyFactory.generateSecret(new PBEKeySpec(your_password,
		byte-code-for-your-salt, 100, 256));
    SecretKey secretKey = new SecretKeySpec(generatedSecretKey.getEncoded(), "AES");
    AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
```

### Oauth2 Bearer challange

`AuthenticationParameters` class provides functionality to get the authorization_uri from Oauth2 bearer challange.

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

```xml
<string name="app_loading">Loading...</string>
<string name="broker_processing">Broker is processing</string>
<string name="http_auth_dialog_username">Username</string>
<string name="http_auth_dialog_password">Password</string>
<string name="http_auth_dialog_title">Sign In</string>
<string name="http_auth_dialog_login">Login</string>
<string name="http_auth_dialog_cancel">Cancel</string>
```

=======

### NTLM dialog
Adal version 1.1.0 supports NTLM dialog that is processed through onReceivedHttpAuthRequest event from WebViewClient. Dialog layout and strings can be customized.

## License

Copyright (c) Microsoft Open Technologies, Inc.  All rights reserved. Licensed under the Apache License, Version 2.0 (the "License"); 

## We Value and Adhere to the Microsoft Open Source Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
