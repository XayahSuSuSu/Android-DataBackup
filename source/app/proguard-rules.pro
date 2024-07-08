# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn com.xayah.**
-dontwarn android.**
-dontwarn com.android.**
-dontwarn dalvik.system.**
-dontwarn libcore.**

# smbj
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
-dontwarn org.slf4j.impl.**

# smbj-rpc
-dontwarn java.rmi.UnmarshalException

# awt
-dontwarn java.awt.**

# sftp
-dontwarn sun.security.x509.X509Key

-keep class com.xayah.** { *; }
-keep class android.** { *; }
-keep class com.android.** { *; }
-keep class dalvik.system.** { *; }
-keep class libcore.** { *; }

# smbj
-keep class javax.el.**
-keep class org.ietf.jgss.**
-keep class org.slf4j.impl.**

# SsaidUtil
-keep class org.xmlpull.**

# smbj-rpc
-keep class java.rmi.UnmarshalException

# awt
-keep class java.awt.**

# sftp
-keep class sun.security.x509.X509Key

# BC
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
