$client = new-object System.Net.WebClient 
$shell_app = new-object -com shell.application
$scriptpath = $MyInvocation.MyCommand.Path
$dir = Split-Path $scriptpath
$filename = "android-junit-report-1.5.8.jar"

Write-Host "Downloading Junit report"
$client.DownloadFile("https://github.com/downloads/jsankey/android-junit-report/android-junit-report-1.5.8.jar", "$dir\libs\$filename") 

