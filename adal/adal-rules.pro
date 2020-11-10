# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Program Files\Android\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontskipnonpubliclibraryclasses

##---------------Begin: proguard configuration for Common  --------
-keep,includedescriptorclasses class com.microsoft.aad.adal.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.exception.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.broker.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.cache.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.dto.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.logging.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.providers.** { *; }
-keep,includedescriptorclasses class com.microsort.identity.common.internal.telemtery.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.adal.internal.** { *;}
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.ui.**
-dontwarn com.microsoft.identity.common.internal.ui.**
-keep,includedescriptorclasses class com.microsoft.device.display.** { *; }
-keep,includedescriptorclasses class com.microsoft.workaccount.** { *; }

##---------------Begin: proguard configuration for Nimbus  ----------
# Intentionally blank, left to consumers of ADAL to implement.

##---------------Begin: proguard configuration for Lombok  ----------
-dontwarn lombok.**

##---------------Begin: proguard configuration for Gson  --------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
