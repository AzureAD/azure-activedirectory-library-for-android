# How to run Automation during releases

## Introduction

The Android Client team maintains multiple libraries such as the MSAL library as well as the Broker library, and therefore maintains test plans that need to be executed as part of doing the release to ensure we don't regress our code. 

We've written automated tests that can be run to validate our code prior to doing these releases. This automation can be run at any time, however, this document primarily focuses on how to run it properly at the time of doing the release.

### MSAL Release

Run the [MSAL Release Automation Script](https://github.com/AzureAD/android-complete/blob/shahzaibj/msal-release-automation-script/MsalReleaseAutomation.ps1) located in Android-Complete project. 

The script can be run as follows:

`.\MsalReleaseAutomation.ps1 -msalVersion <msal-version> -labSecret <secret-value>`

The script takes two arguments as follows:

- **msalVersion** - this is msal version number (typically an RC build) that we are trying to test
- **labSecret** - the secret required to use LAB API. More details here: [LabSetup.md](https://github.com/AzureAD/android-complete/blob/shahzaibj/msal-release-automation-script/docs/Automation/labsetup.md)

### Broker Release

Run the [Broker Release Automation Script](https://github.com/AzureAD/android-complete/blob/shahzaibj/msal-release-automation-script/BrokerReleaseAutomation.ps1) located in Android-Complete project. 

The script can be run as follows:

`.\BrokerReleaseAutomation.ps1 -labSecret <secret-value> -authenticatorApkPath '<path-to-apk>' -companyPortalApkPath '<path-to-apk>'`

The script takes two arguments as follows:

- **labSecret** - the secret required to use LAB API. More details here: [LabSetup.md](https://github.com/AzureAD/android-complete/blob/shahzaibj/msal-release-automation-script/docs/Automation/labsetup.md)
- **authenticatorApkPath** - the path to Authenticator APK pointing to broker version (typically an RC build) that we are trying to test
- **companyPortalApkPath** - the path to Company Portal APK pointing to broker version (typically an RC build) that we are trying to test
