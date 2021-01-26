#  Copyright (c) Microsoft Corporation.
#  All rights reserved.
#
#  This code is licensed under the MIT License.
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files(the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions :
#
#  The above copyright notice and this permission notice shall be included in
#  all copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
#  THE SOFTWARE.

param ($labSecret, $authenticatorApkPath, $companyPortalApkPath)
$ErrorActionPreference = "Stop"

$path = pwd

Write-Output "Working directory = $path"

Write-Output "Test will be run on following device: "

adb devices

Write-Output "Pushing Authenticator APK to the device on the /data/local/tmp folder..."

adb push $authenticatorApkPath /data/local/tmp/Authenticator.apk

Write-Output "Pushing Company Portal APK to the device on the /data/local/tmp folder..."

adb push $companyPortalApkPath /data/local/tmp/CompanyPortal.apk

Write-Output "Building Azure Sample app..."

cd "$path\azuresample"
./gradlew app:assembleExternalRelease

Write-Output "Azure Sample build completed."

$azureSampleApkLocation = "$path\azuresample\app\build\outputs\apk\external\release\app-external-release.apk"

Write-Output "Azure Sample APK is located here: $azureSampleApkLocation"

Write-Output "Pushing Azure Sample APK to the device on the /data/local/tmp folder..."

adb push $azureSampleApkLocation /data/local/tmp/AzureSample.apk

Write-Output "Azure Sample APK has been pushed to the device!"

cd $path

Write-Output "Installing MSAL Automation app on the device..."

./gradlew msalautomationapp:installDistAutoBrokerDebug -PbrokerSource="LocalApk" -PlabSecret="$labSecret"

Write-Output "Installing MSAL Automation app connected tests on the device..."

./gradlew msalautomationapp:installDistAutoBrokerDebugAndroidTest

Write-Output "Running MSAL with Broker Test Plan..."

adb shell CLASSPATH=$(adb shell pm path androidx.test.services) app_process / androidx.test.services.shellexecutor.ShellMain am instrument -r -w -e targetInstrumentation com.msft.identity.client.sample.local.test/androidx.test.runner.AndroidJUnitRunner -e clearPackageData true   -e debug false -e package 'com.microsoft.identity.client.msal.automationapp.testpass.broker' androidx.test.orchestrator/androidx.test.orchestrator.AndroidTestOrchestrator

Write-Output "MSAL with broker test plan completed."

Write-Output "Installing Broker Automation app on the device..."

./gradlew brokerautomationapp:installDistAutoBrokerDebug -PbrokerSource="LocalApk" -PlabSecret="$labSecret"

Write-Output "Installing Broker Automation app connected tests on the device..."

./gradlew brokerautomationapp:installDistAutoBrokerDebugAndroidTest

Write-Output "Running ADAL with broker test plan..."

adb shell CLASSPATH=$(adb shell pm path androidx.test.services) app_process / androidx.test.services.shellexecutor.ShellMain am instrument -r -w -e targetInstrumentation com.msft.identity.client.sample.local.test/androidx.test.runner.AndroidJUnitRunner -e clearPackageData true   -e debug false -e package 'com.microsoft.identity.client.broker.automationapp.testpass.adal' androidx.test.orchestrator/androidx.test.orchestrator.AndroidTestOrchestrator

Write-Output "ADAL with broker test plan completed."

Write-Output "Running Broker basic validation test plan that uses first party apps..."

adb shell CLASSPATH=$(adb shell pm path androidx.test.services) app_process / androidx.test.services.shellexecutor.ShellMain am instrument -r -w -e targetInstrumentation com.msft.identity.client.sample.local.test/androidx.test.runner.AndroidJUnitRunner -e clearPackageData true   -e debug false -e package 'com.microsoft.identity.client.broker.automationapp.testpass.basic' androidx.test.orchestrator/androidx.test.orchestrator.AndroidTestOrchestrator

Write-Output "Broker basic validation test plan completed."

cd $path

Write-Output "The entire broker release automated test plan run is completed!"
