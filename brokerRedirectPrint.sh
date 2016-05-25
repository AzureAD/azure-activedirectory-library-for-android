#!/bin/bash

# 
# Requirements
# Install and setup Java We assume $JAVA_HOME is set
# Install openssl either through your linux package manager or macOS package manager (we like brew)
# Update to keystore folders for debug and release

set -o errtrace
set -o pipefail

package_name="com.your.app"
java_bin=$JAVA_HOME/jre/bin
keytool=$java_bin/keytool

debug_key_store=$HOME/.android/debug.keystore
release_alias="androiddebugkey"
release_key_store=$HOME/.android/release.keystore
release_password="android"
android_key_store=

usage() {
    
    echo ""
    echo " This tool generates a ReplyURL you'll need to enter in to your Azure Portal"
    echo " to allow for your app to use the Microsoft Accounts broker app. This will"
    echo " allow your app to particiapte in SSO and do Certificate based authentication"
    echo " among other good things./n"
    echo ""
    echo "Usage:";
    echo " -d | --debug           genrates a replyURL using your debug keystore in Android Studio"
    echo " -r | --release         generates a replyURL using your production keystore in Android Studio"
    echo " -c | --package         the package name of your application"
    echo " -p | --password        your keystore password (default for debug keychain is android)"
    echo " -a | --alias           your keystore alias (default for debug keychain is androiddebugkey)"
    echo ""
}


if [ $# -gt 0 ]; then

while [ "$1" != "" ]; do
    case $1 in
        -d | --debug)           debug=1
                                ;;
            -r | --release )    release=1
                                ;;
            -c | --package )   shift
                                package_name=$1
                                ;;
            -p | --password )   shift
                                release_password=$1
                                ;;
            -a | --alias )      shift
                                release_alias=$1
                                ;;
        -h | --help )           usage
                                exit
                                ;;
        * )                     usage
                                exit 1
    esac
    shift
done
else
usage
fi

 



#---------------------------------
# Simple func() to encode a URL. Wrote it so we don't have an external dependency
#

rawurlencode() {
  local string="${1}"
  local strlen=${#string}
  local encoded=""
  local pos c o

  for (( pos=0 ; pos<strlen ; pos++ )); do
     c=${string:$pos:1}
     case "$c" in
        [-_.~a-zA-Z0-9] ) o="${c}" ;;
        * )               echo -v o '%%%02x' "'$c"
     esac
     encoded+="${o}"
  done
  echo "${encoded}"    # You can either set a return variable (FASTER) 
  REPLY="${encoded}"   #+or echo the result (EASIER)... or both... :p
}
#---------------------------------

#---------------------------------
# Simple Func() to generate the hash from the certificate provided by the android keystore.
makeTag() {
   tag=`$keytool -storepass $release_password -exportcert -alias $release_alias -keystore $android_key_store | openssl sha1 -binary |  openssl base64`
}
#---------------------------------

#---------------------------------
makeReplyURL() {
   printf "msauth://%s/%s\n" $package_name_encoded $tag
}
#---------------------------------

# Simple Func() to do release
makerelease() {
    
    if [ -f "$release_key_store" ]; then
    
        android_key_store=$release_key_store
     
        echo "We are using the following values"
        printf "Package Name: %s\n" $package_name
        printf "Keystore alias: %s\n" $release_alias
        printf "Keystore password: %s\n" $release_password
        printf "Keystore: %s\n" $android_key_store

        makeTag
        echo "Release Redirect URI is:"
        makeReplyURL


    else 
    printf "ERROR: The Release Android Key Store location %s was not found. Have you set up Android Studio for Google Play?\n" $android_key_store
  
    fi
}
#---------------------------------

# Simple Func() to do debug
makedebug() {
    
    if [ -f "$debug_key_store" ]; then
    
    android_key_store=$debug_key_store
    
    echo "We are using the following values"
    printf "Package Name: %s\n" $package_name
    printf "Keystore alias: %s\n" $release_alias
    printf "Keystore password: %s\n" $release_password
    printf "Keystore: %s\n" $android_key_store

    makeTag
    echo "Debug Redirect URI is:"
    makeReplyURL



    else 
    printf "ERROR: The Debug Android Key Store location %s was not found. Try deploying your app in debug mode first to generate keys.\n" $android_key_store
  
    fi
}
#---------------------------------
# main



# Encoding the package name
package_name_encoded=`rawurlencode "$package_name"`


if [ "$debug" = "1" ]; then

    makedebug
fi
if [ "$release" = "1" ]; then
    makerelease
fi




