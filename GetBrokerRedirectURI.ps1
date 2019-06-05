<#
.SYNOPSIS
Obtain the RedirectURI from an APK.  Requires JDK\bin\keytool and AndroidSDK\build-tools\###\aapt to be on the path.

.EXAMPLE
GetBrokerRedirectURI MyApp.apk
#>
Param(
[Parameter(Mandatory=$true)] 
[string]$apkFile
)

<#
Extract all of the signature owner and SHA1 hex bytes from `keytool -printcert -jarfile`.
#>
function GetSHA1Hex {
    Param(
        [string]$apkFile
        )
    $output = keytool -printcert -jarfile $apkFile
    $ownerPrefix = "Owner:"
    $shaPrefix = "SHA1:"
    $signatures = @{}
    $owner = ""
    ForEach ($line in $output) {
        if ($line.contains($ownerPrefix)) {
            $owner = $line.replace($ownerPrefix, "").trim()
        }

        if ($line.contains($shaPrefix)) {
            $base64 = GetBase64FromSHA1Hex($line.replace($shaPrefix, "").trim())
            $signatures.add($owner, $base64)
        }
    }
    
    if ($signatures.Keys.count -eq 0) {
        write-error "No signatures found."
        exit
    }
    
    if ($signatures.Keys.count -gt 1) {
        write-warning "Multiple signatures detected, please select the correct one."
    }

    return $signatures
}

<#
Convert the string hex to bytes.
#>
function GetBase64FromSHA1Hex {
    Param(
        [string]$hexSig
        )

    $hex = $hexSig.split(":")
    $bytes = @()
    ForEach ($byte in $hex) {
        $bytes += [convert]::ToByte($byte, 16)
    }

    [System.Convert]::ToBase64String($bytes)
}

<#
Extract the APK package name from `aapt dump badging`.
#>
function GetPackageName {
    Param(
        [string]$apkFile
    )

    $output = aapt dump badging $apkFile
    $match = $output | select-string -Pattern "name='(.*?)'"

    if (-not $match) {
        write-error "Could not find the package name in aapt output."
        exit
    }

    return $match.Matches.Groups[1].Value
}


[Reflection.Assembly]::LoadWithPartialName("System.Web") | out-null

$signatures = GetSHA1Hex($apkFile)
$packageName = GetPackageName($apkFile)
if ($signatures.Keys.Count -gt 1) {
    $manySignatures = $True
}

write-host "Found package name: $packageName"
ForEach ($sig in $signatures.GetEnumerator()) {
    $encodedSignature = [System.Web.HttpUtility]::UrlEncode($sig.Value)

    write-host ""
    if ($manySignatures) {
        write-host "Found signature owner: $($sig.Name)"
    }
    write-host "Found signature fingerprint: $($sig.Value)"
    write-host -ForegroundColor Green "RedirectURI: msauth://$packageName/$encodedSignature"
}