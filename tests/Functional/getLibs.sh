#! /bin/sh

echo "Downloading Junit report"
curl http://cloud.github.com/downloads/jsankey/android-junit-report/android-junit-report-1.5.8.jar -o android-junit-report-1.5.8.jar

echo "Move library to destination"
mv android-junit-report-1.5.8.jar ./libs

