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

# dms: only the app fragments and only their constructor: keeping every Fragment also kept unused Material classes
# (BottomSheetDialogFragment, MaterialDatePicker, EdgeToEdgeUtils) using the deprecated Window.setStatusBarColor /
# setNavigationBarColor (Play Console warning). With { *; } R8 also keeps the inherited Fragment methods
# (onStart, onSaveInstanceState...) and so every override of them, in any library Fragment.
-keep public class org.linphone.** extends androidx.fragment.app.Fragment { public <init>(); }
-dontwarn com.google.errorprone.annotations.Immutable

# dms: Firebase creates its registrars by name with the no-arg constructor (ComponentDiscovery), its own rule keeps
# only the class: with the R8 strict full mode keep rules (AGP 9) the constructor was removed and the app crashed at
# start ("FirebaseCrashlytics component is not present")
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }

-keepattributes *Annotation*

# Retrofit
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Exceptions, Signature, InnerClasses
-keep class okhttp3.** { *; }
-keep class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-keep class org.conscrypt.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.reflect.TypeToken

-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class  org.linphone.bcsws.UserConf { *; }
-keep class  org.linphone.bcsws.Member { *; }
-keep class  org.linphone.bcsws.Buddy { *; }
-keep class  org.linphone.bcsws.Misc { *; }
-keep class  org.linphone.bcsws.AuthResponse { *; }
-keep class  org.linphone.bcsws.DirectoryResponse { *; }
-keep class  org.linphone.bcsws.DirectoryItem { *; }
-keep class org.linphone.bcsws.CallReportItem { *; }
-keep class org.linphone.bcsws.CallReportResponse { *; }
-keep class org.linphone.bcsws.RemoteParty { *; }
-keep class  org.linphone.bcsws.** { *; }

# Retrofit suspend functions with R8 full mode (AGP 8+), BcsWs calls break without these
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# Gson generic types with R8 full mode
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
