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

-keep public class * extends androidx.fragment.app.Fragment { *; }
-dontwarn com.google.errorprone.annotations.Immutable

-keepattributes *Annotation*

# Retrofit
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }

-keep class  org.linphone.bcsws.UserConf { *; }
-keep class  org.linphone.bcsws.Member { *; }
-keep class  org.linphone.bcsws.Buddy { *; }
-keep class  org.linphone.bcsws.Misc { *; }
-keep class  org.linphone.bcsws.AuthResponse { *; }
-keep class  org.linphone.bcsws.DirectoryResponse { *; }
-keep class  org.linphone.bcsws.DirectoryItem { *; }
-keep class  org.linphone.bcsws.** { *; }
