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
-keep,includedescriptorclasses class com.microsoft.identity.common.exception.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.broker.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.cache.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.logging.internal.dto.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.internal.logging.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.providers.** { *; }
-keep,includedescriptorclasses class com.microsort.identity.common.internal.telemtery.** { *; }
-keep,includedescriptorclasses class com.microsoft.identity.common.adal.internal.** { *;}
-keep,includedescriptorclasses class com.microsoft.identity.common.internal.ui.** { *; }
-dontwarn com.microsoft.identity.common.internal.ui.**
-keep,includedescriptorclasses class com.microsoft.device.display.** { *; }
-keep,includedescriptorclasses class org.apache.harmony.xnet.provider.jsse.NativeCrypto { *; }

##---------------Begin: proguard configuration for crypto classes  --------
-keep,includedescriptorclasses class com.google.crypto.tink.subtle.Ed25519Sign { *; }
-keep,includedescriptorclasses class com.google.crypto.tink.subtle.Ed25519Sign$KeyPair { *; }
-keep,includedescriptorclasses class com.google.crypto.tink.subtle.Ed25519Verify { *; }
-keep,includedescriptorclasses class com.google.crypto.tink.subtle.X25519 { *; }
-keep,includedescriptorclasses class org.bouncycastle.asn1.pkcs.PrivateKeyInfo { *; }
-keep,includedescriptorclasses class org.bouncycastle.asn1.x509.AlgorithmIdentifier { *; }
-keep,includedescriptorclasses class org.bouncycastle.asn1.x509.SubjectPublicKeyInfo { *; }
-keep,includedescriptorclasses class org.bouncycastle.cert.X509CertificateHolder { *; }
-keep,includedescriptorclasses class org.bouncycastle.cert.jcajce.JcaX509CertificateHolder { *; }
-keep,includedescriptorclasses class org.bouncycastle.crypto.BlockCipher { *; }
-keep,includedescriptorclasses class org.bouncycastle.crypto.CipherParameters { *; }
-keep,includedescriptorclasses class org.bouncycastle.crypto.InvalidCipherTextException { *; }
-keep,includedescriptorclasses class org.bouncycastle.crypto.engines.AESEngine { *; }
-keep,includedescriptorclasses class org.bouncycastle.crypto.modes.GCMBlockCipher { *; }
-keep,includedescriptorclasses class org.bouncycastle.crypto.params.AEADParameters { *; }
-keep,includedescriptorclasses class org.bouncycastle.crypto.params.KeyParameter { *; }
-keep,includedescriptorclasses class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keep,includedescriptorclasses class org.bouncycastle.openssl.PEMException { *; }
-keep,includedescriptorclasses class org.bouncycastle.openssl.PEMKeyPair { *; }
-keep,includedescriptorclasses class org.bouncycastle.openssl.PEMParser { *; }
-keep,includedescriptorclasses class org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter { *; }
-dontwarn com.google.crypto.tink.subtle.Ed25519Sign
-dontwarn com.google.crypto.tink.subtle.Ed25519Sign$KeyPair
-dontwarn com.google.crypto.tink.subtle.Ed25519Verify
-dontwarn com.google.crypto.tink.subtle.X25519
-dontwarn org.bouncycastle.asn1.pkcs.PrivateKeyInfo
-dontwarn org.bouncycastle.asn1.x509.AlgorithmIdentifier
-dontwarn org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
-dontwarn org.bouncycastle.cert.X509CertificateHolder
-dontwarn org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
-dontwarn org.bouncycastle.crypto.BlockCipher
-dontwarn org.bouncycastle.crypto.CipherParameters
-dontwarn org.bouncycastle.crypto.InvalidCipherTextException
-dontwarn org.bouncycastle.crypto.engines.AESEngine
-dontwarn org.bouncycastle.crypto.modes.GCMBlockCipher
-dontwarn org.bouncycastle.crypto.params.AEADParameters
-dontwarn org.bouncycastle.crypto.params.KeyParameter
-dontwarn org.bouncycastle.jce.provider.BouncyCastleProvider
-dontwarn org.bouncycastle.openssl.PEMException
-dontwarn org.bouncycastle.openssl.PEMKeyPair 
-dontwarn org.bouncycastle.openssl.PEMParser
-dontwarn org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter


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
