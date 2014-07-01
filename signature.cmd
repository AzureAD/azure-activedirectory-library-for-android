::	Broker RedirectUri
::
:: 1- Please copy your debug.keystore file into desktop from
:: 	your "C:\users\yourname\.android" folder
:: 2- Run this in git console or install openssl
:: 3- Java Jdk is needed for keytool.exe
REM Set your folders here
:: %1 JDK bin
:: %2 store pass
:: %3 alias
:: %4 keystore full path
:: %5 openssl path
%1 -storepass %2 -exportcert -alias %3 -keystore %4 | %5 sha1 -binary |  %5 base64 > tag.txt
