#! /bin/sh

echo "Downloading Junit report"
curl https://cloud.github.com/downloads/jsankey/android-junit-report/android-junit-report-1.5.8.jar -o ./libs/android-junit-report-1.5.8.jar

echo "Downloading mockito"
curl https://mockito.googlecode.com/files/mockito-all-1.9.5.jar -o ./libs/mockito-all-1.9.5.jar