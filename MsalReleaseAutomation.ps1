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
