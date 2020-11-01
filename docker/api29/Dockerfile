FROM android-complete:0.1.0
USER root
ENV ANDROID_HOME="/usr/local/android-sdk" \
    ANDROID_VERSION=29 \
    ANDROID_BUILD_TOOLS_VERSION=29.0.3 \
    SYSTEM_IMAGE="system-images;android-24;default;armeabi-v7a"
# Install Android Build Tool and Libraries
RUN $ANDROID_HOME/tools/bin/sdkmanager --update
RUN $ANDROID_HOME/tools/bin/sdkmanager "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "platform-tools" \
    "emulator" 
RUN yes | $ANDROID_HOME/tools/bin/sdkmanager $SYSTEM_IMAGE
RUN echo no | $ANDROID_HOME/tools/bin/avdmanager create avd --force -n test -k $SYSTEM_IMAGE
RUN $ANDROID_HOME/platform-tools/adb start-server