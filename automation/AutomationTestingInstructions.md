
# Automation Testing ADAL Android:

## Install pre-requisites:

### 1. Install node.js:
node.js is required to isntall Appium.  You can download the latest version of node.js here: [https://nodejs.org/en/](https://nodejs.org/en/)

### 2. Install Appium:
Appium can be installed using the instructions found here: [http://appium.io/docs/en/about-appium/getting-started/?lang=en](http://appium.io/docs/en/about-appium/getting-started/?lang=en)

### 3. Configure the Appium Android Driver (You may be fine already):
In order to test Android apps you'll need the appium Android Driver configured.  Instructions for configuring the android driver are found here: [http://appium.io/docs/en/drivers/android-uiautomator2/index.html](http://appium.io/docs/en/drivers/android-uiautomator2/index.html)

### 4. Verify your installation of Appium:
Appium provides a helpful tool for validating your configuration of appium.  You can install it using the following instructions:

```javascript
npm install -g appium-doctor
```

Run the following command to verify your setup:

```javascript
appium-doctor --android
```
appium-doctor will identity any issues, typically related to environment variables, for you to fix.

## Install the AutomationRunner Certificate
The Android automation accesses key vault in order to retrieve credentials for test accounts.  Access to key vault is dependent on installation of a certificate.  Use the following script to install that certificate.  Only authorized users will be able to install the certificate.  At the moment this means authorized Microsoft employees.  I haven't spent time at the moment on providing you with a foolproof script.  You may need to change the $pfxPath variable to point to the desired location on your workstation.

Other instructions:

1. Install Azure PowerShell if you don't already have installed: [https://docs.microsoft.com/en-us/powershell/azure/install-azurerm-ps?view=azurermps-5.7.0](https://docs.microsoft.com/en-us/powershell/azure/install-azurerm-ps?view=azurermps-5.7.0)
2. Update the pfxPath variable if you wish.
3. When prompted for credentails the username doesn't matter, just enter something, but the password does. Please use a strong password as this will be used to protect the private key when the certificate is written to disk.  (NOTE: The certificate will be deleted )

```powershell
#Attribution: http://www.sherweb.com/blog/powershell-ing-on-windows-server-how-to-import-certificates-using-powershell/
function Import-NewPfxCertificate {
param([String]$certPath,[String]$certRootStore = “CurrentUser”,[String]$certStore = “My”,$pfxPass = $null)
 $pfx = new-object System.Security.Cryptography.X509Certificates.X509Certificate2
 if ($pfxPass -eq $null) {$pfxPass = read-host “Enter the pfx password” -assecurestring}
 $pfx.import($certPath,$pfxPass,“Exportable,PersistKeySet”)
 $store = new-object System.Security.Cryptography.X509Certificates.X509Store($certStore,$certRootStore)
 $store.open(“MaxAllowed”)
 $store.add($pfx)
 $store.close()
}

Login-AzureRMAccount -TenantId microsoft.com

$pfxPath="C:\temp\test\automationrunner.pfx"
$cert = Get-AzureKeyVaultCertificate -VaultName "MSIDLABS" -Name "AutomationRunner"
$credential = Get-Credential

$certBytes = $cert.Certificate.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Pfx, $credential.Password)
[System.IO.File]::WriteAllBytes($pfxPath, $certBytes)

Import-NewPfxCertificate -certPath $pfxPath -pfxPass $credential.Password

Set-Location cert:\currentuser\My

(gci '33BC9D3DEB420C998D818EAFAA0BAE26296A9AA4').FriendlyName = "AutomationRunner"

Remove-Item $pfxPath

```

## Emulators and Phsyical Devices

Many (if not all of you) at the time of writing are using a version of Windows 10 for your development. Windows 10 introduced some new security features which take advantage of the Hyper-V.  In order to run emulators locally you'll need to disable those security features.  You can do so by:

1. Downloading the script found here: [https://www.microsoft.com/en-us/download/details.aspx?id=53337](https://www.microsoft.com/en-us/download/details.aspx?id=53337) 
2. Running the following command from an elevated command prompt:

```powershell
DG_Readiness_Tool_v3.4.ps1 -Disable
```

## Running Tests

The test are located in the automation project under the root project.  

### Before running the tests
Before running the tests you'll need to perform the following steps:

1. Update serentity.properties file to point to the apk that you would like to test.  Not everyone clones their project in the same directory and this affects the where the compiled APK is outputted.
2. Update serentity.properties device information to match an attached device or an available emulator.  The key property to update is appium.platformVersion.  Make sure the version in the serenity.properties file matches the available device/emulator.

### Running the tests
Test can be run within Android Studio.  Browse to the test class in question and run the test.  We'll be making some updates to further streamline this process.

