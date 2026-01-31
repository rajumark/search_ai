# ============================================================================
# PhotoSearchAI ProGuard/R8 Rules
# Full minification and obfuscation configuration for release builds
# ============================================================================

# ============================================================================
# GENERAL ANDROID RULES
# ============================================================================

# Keep line numbers for better crash reports (optional but recommended)
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name in stack traces
-renamesourcefileattribute SourceFile

# Keep generic type information for Kotlin
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin metadata
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# ============================================================================
# KOTLIN & COROUTINES
# ============================================================================

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ============================================================================
# HILT / DAGGER
# ============================================================================

# Keep Hilt-generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }

# Keep @Inject annotated constructors
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep @HiltViewModel classes
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Hilt entry points
-keep class * implements dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ============================================================================
# ROOM DATABASE
# ============================================================================

# Keep Room entities, DAOs, and Database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep Room-generated implementation classes
-keep class * implements androidx.room.RoomDatabase$Callback { *; }

# ============================================================================
# JETPACK COMPOSE
# ============================================================================

# Keep Compose runtime classes
-keep class androidx.compose.runtime.** { *; }

# Keep Composable functions metadata
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Compose UI classes
-keep class androidx.compose.ui.** { *; }

# ============================================================================
# ML KIT (Text Recognition)
# ============================================================================

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }

# Don't warn about ML Kit internal classes
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# ============================================================================
# COIL (Image Loading)
# ============================================================================

# Keep Coil classes
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================================
# DATASTORE
# ============================================================================

# Keep DataStore classes
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# ============================================================================
# WORKMANAGER
# ============================================================================

# Keep Worker classes
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Keep Hilt Worker injection
-keep class * extends androidx.hilt.work.HiltWorkerFactory { *; }

# ============================================================================
# PAGING
# ============================================================================

# Keep Paging classes
-keep class androidx.paging.** { *; }

# ============================================================================
# NAVIGATION
# ============================================================================

# Keep navigation arguments and destinations
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# ============================================================================
# APPLICATION SPECIFIC RULES
# ============================================================================

# Keep your data classes (entities, models)
-keep class com.photo.searchai.data.** { *; }
-keep class com.photo.searchai.model.** { *; }

# Keep repository implementations
-keep class com.photo.searchai.repository.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ============================================================================
# REMOVE LOGGING IN RELEASE
# ============================================================================

# Remove debug logging statements
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ============================================================================
# OPTIMIZATION FLAGS
# ============================================================================

# Enable aggressive optimizations
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Don't warn about missing classes that won't affect the app
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**