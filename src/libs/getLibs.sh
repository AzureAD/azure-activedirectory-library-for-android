#! /bin/sh

echo "Downloading Google Gson 2.3.1"
curl -O http://google-gson.googlecode.com/files/google-gson-2.3.1-release.zip

echo "Decompressing..."
unzip google-gson-2.3.1-release.zip

echo "Remove zip"
rm google-gson-2.3.1-release.zip

echo "Move library"
mv ./google-gson-2.3.1/gson-2.3.1.jar ./

echo "Remove extra files"
rm -rf ./google-gson-2.3.1