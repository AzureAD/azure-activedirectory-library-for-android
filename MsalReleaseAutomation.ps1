param ($msalVersion, $labSecret)
$ErrorActionPreference = "Stop"

$path = pwd

Write-Output "Working directory = $path"

Write-Output "Test will be run on following device: "

adb devices

Write-Output "Building Azure Sample app with the supplied MSAL version..."

cd azuresample
./gradlew app:assembleExternalRelease -PdistMsalVersion="$msalVersion"

Write-Output "Azure Sample build completed."

$azureSampleApkLocation = "$path\azuresample\app\build\outputs\apk\external\release\app-external-release.apk"

Write-Output "Azure Sample APK is located here: $azureSampleApkLocation"

Write-Output "Pushing Azure Sample APK to the device on the /data/local/tmp folder..."

adb push $azureSampleApkLocation /data/local/tmp/AzureSample.apk

Write-Output "Azure Sample APK has been pushed to the device!"

cd $path

Write-Output "Starting to run MSAL Automation against the supplied MSAL version. The broker apps will be used from PlayStore."

./gradlew msalautomationapp:connectedDistAutoBrokerDebugAndroidTest -PbrokerSource="PlayStore" -PlabSecret="$labSecret" -PdistMsalVersion="$msalVersion"