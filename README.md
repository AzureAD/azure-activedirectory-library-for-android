#ADAL Android SDK
===========

This is an Android library for Azure Active Directory(AAD). It provides simple functionality to get token, refresh token, and use cache. 

## Quick Start

* You need to add library as a reference to your project at Properties->Android->Library add.
* Add this activity to your AndroidManifest.xml
*   <activity
*            android:name="com.microsoft.adal.LoginActivity"
*            android:label="@string/title_login" >
*    </activity>
* Create AuthenticationContext at Main thread
*  
