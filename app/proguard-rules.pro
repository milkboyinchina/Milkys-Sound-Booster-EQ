# ==============================================================================
# PROGUARD / R8 OPTIMIZATION & CODE SHRINKING CONFIGURATION
# Milkys Sound Booster & EQ
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. METADATA, STACK TRACES & ANNOTATIONS PRESERVATION
# ------------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable,Exceptions,InnerClasses,Signature,*Annotation*,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------------------
# 2. ANDROID CORE COMPONENTS & SERVICE TIMERS
# ------------------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.service.quicksettings.TileService

# ------------------------------------------------------------------------------
# 3. APPLICATION DOMAIN & STATE CLASSES
# ------------------------------------------------------------------------------
-keep class com.milkys.soundbooster.** { *; }
-keepclassmembers class com.milkys.soundbooster.** { *; }

# ------------------------------------------------------------------------------
# 4. JETPACK COMPOSE & MATERIAL 3
# ------------------------------------------------------------------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.ui.** { *; }

# ------------------------------------------------------------------------------
# 5. ROOM DATABASE & KSP GENERATED CODE
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class **_Impl { *; }
-keepclassmembers class * {
    @androidx.room.* *;
}

# ------------------------------------------------------------------------------
# 6. MOSHI & JSON SERIALIZATION
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn com.squareup.moshi.**

# ------------------------------------------------------------------------------
# 7. RETROFIT & OKHTTP NETWORKING
# ------------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# ------------------------------------------------------------------------------
# 8. GOOGLE PLAY SERVICES & ADMOB
# ------------------------------------------------------------------------------
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ------------------------------------------------------------------------------
# 9. KOTLIN COROUTINES & FLOW
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepclassmembers class * extends kotlinx.coroutines.internal.MainDispatcherFactory { *; }


