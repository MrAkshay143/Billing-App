# ProGuard / R8 optimization and obfuscation keep rules for Dokandaar Billing App

# Preserve line numbers and source files for crash reporting
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# KEEP ALL APP CODE: Prevents stripping/obfuscating ViewModels, DAOs, Entities, Models, Navigation
-keep class com.billing.app.** { *; }
-keep interface com.billing.app.** { *; }
-keep enum com.billing.app.** { *; }

# Room database keep rules
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.Dao {
    <methods>;
}
-keep class * extends androidx.room.migration.Migration { *; }
-keep class *_Impl { *; }
-dontwarn androidx.room.**

# Keep ViewModels and ViewModelProvider Factory
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModelProvider$Factory { *; }
-dontwarn androidx.lifecycle.**

# Google ML Kit Barcode scanning & all internal components
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_barcode.**

-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_common.**

-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.android.gms.vision.**

-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.common.**

-keep class com.google.android.datatransport.** { *; }
-dontwarn com.google.android.datatransport.**

# ZXing QR Code generation
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
