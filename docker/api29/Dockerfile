#
# THIS IS AN GENERATED-FILE.  DO NOT EDIT DIRECTLY
#
FROM gradle:6.7.1-jdk11 as BASE
USER root
ENV SDK_URL="https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip" \
    ANDROID_HOME="/usr/local/android-sdk" \
    CMAKE_BIN_URL="https://github.com/Kitware/CMake/releases/download/v3.18.5/cmake-3.18.5-Linux-x86_64.tar.gz" \
    CMAKE_TAR_FILE="cmake-3.18.5-Linux-x86_64.tar.gz" \
    CMAKE_HOME="/usr/local/cmake" \
    CMAKE_HOME_BIN="/usr/local/cmake/cmake-3.18.5-Linux-x86_64/bin" \
    NINJA_BIN_URL="https://github.com/ninja-build/ninja/releases/download/v1.10.2/ninja-linux.zip" \
    NINJA_ZIP_FILE="ninja-linux.zip"
RUN cd ~/.gradle \
    && touch gradle.properties \
    && printf "vstsUsername=VSTS\n" >> gradle.properties \
    && printf "vstsMavenAccessToken=mz3rbwtljuo7g5dt3l4nprqsbtnciuxezzslkuzv6267qka5zv4q\n" >> gradle.properties
RUN apt-get update \
    &&  apt-get -y install build-essential
# Download Android SDK and Fix SDKManager for JDK 11
RUN mkdir "$ANDROID_HOME" .android \
    && cd "$ANDROID_HOME" \
    && curl -o sdk.zip $SDK_URL \
    && unzip sdk.zip \
    && rm sdk.zip \
    && mkdir "$ANDROID_HOME/licenses" || true \
    && echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" > "$ANDROID_HOME/licenses/android-sdk-license"
RUN cd "$ANDROID_HOME"/tools \
    && mkdir jaxb_lib \ 
    && wget https://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar -O jaxb_lib/activation.jar \
    && wget https://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-impl/2.3.3/jaxb-impl-2.3.3.jar -O jaxb_lib/jaxb-impl.jar \
    && wget https://repo1.maven.org/maven2/com/sun/istack/istack-commons-runtime/3.0.11/istack-commons-runtime-3.0.11.jar -O jaxb_lib/istack-commons-runtime.jar \
    && wget https://repo1.maven.org/maven2/org/glassfish/jaxb/jaxb-xjc/2.3.3/jaxb-xjc-2.3.3.jar -O jaxb_lib/jaxb-xjc.jar \
    && wget https://repo1.maven.org/maven2/org/glassfish/jaxb/jaxb-core/2.3.0.1/jaxb-core-2.3.0.1.jar -O jaxb_lib/jaxb-core.jar \
    && wget https://repo1.maven.org/maven2/org/glassfish/jaxb/jaxb-jxc/2.3.3/jaxb-jxc-2.3.3.jar -O jaxb_lib/jaxb-jxc.jar \
    && wget https://repo1.maven.org/maven2/javax/xml/bind/jaxb-api/2.3.1/jaxb-api-2.3.1.jar -O jaxb_lib/jaxb-api.jar \
    && sed -ie 's%^CLASSPATH=.*%\0:$APP_HOME/jaxb_lib/*%' bin/sdkmanager bin/avdmanager
RUN apt install -yq --no-install-suggests --no-install-recommends qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils
# GET CMAKE and NINJA
RUN wget -nv $CMAKE_BIN_URL
RUN mkdir $CMAKE_HOME
RUN tar -zxf $CMAKE_TAR_FILE --directory $CMAKE_HOME
RUN wget $NINJA_BIN_URL
RUN unzip $NINJA_ZIP_FILE
RUN cp ninja $CMAKE_HOME_BIN/ninja

FROM BASE as DBI_UNIT
USER root
ENV PATH="/usr/local/cmake/cmake-3.18.5-Linux-x86_64/bin:/usr/local/android-sdk/emulator:/usr/local/android-sdk/tools:/usr/local/android-sdk/tools/bin:/usr/local/android-sdk/platform-tools:${PATH}" \
    ANDROID_HOME="/usr/local/android-sdk" \
    ANDROID_VERSION=29 \
    ANDROID_BUILD_TOOLS_VERSION=30.0.2 \
    ANDROID_NDK_VERSION=ndk;21.1.6352462
# Install Android Build Tool and Libraries
RUN $ANDROID_HOME/tools/bin/sdkmanager --update
RUN touch /root/.android/repositories.cfg
RUN $ANDROID_HOME/tools/bin/sdkmanager "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "platform-tools" \
    "${ANDROID_NDK_VERSION}" \
    "emulator" 

FROM DBI_UNIT as DBI_INSTRUMENTED
USER root
ENV ANDROID_HOME="/usr/local/android-sdk" \
    SYSTEM_IMAGE="system-images;android-29;google_apis;x86_64" \
    SYSTEM_IMAGE_TAG="google_apis"
RUN apt-get -y install qt5-default
RUN $ANDROID_HOME/tools/bin/sdkmanager "emulator"
RUN yes | $ANDROID_HOME/tools/bin/sdkmanager $SYSTEM_IMAGE
RUN echo no | $ANDROID_HOME/tools/bin/avdmanager create avd --force -g $SYSTEM_IMAGE_TAG -n test -k $SYSTEM_IMAGE