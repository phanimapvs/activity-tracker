# R8 / ProGuard rules for Daily Sadhana & Activity Tracker

# Keep Room Database & Entities
-keep class * extends androidx.room.RoomDatabase
-keep class com.activitytracker.app.data.local.entity.** { *; }
-dontwarn androidx.room.paging.**

# Keep Domain Models (for serialization & state flows)
-keep class com.activitytracker.app.domain.model.** { *; }
-keepclassmembers class com.activitytracker.app.domain.model.** { *; }

# Keep Hilt & Dagger
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class **.Dagger* { *; }
-keep class **.*_HiltModules* { *; }
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Keep KotlinX Serialization & Datetime
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.datetime.** { *; }

# Keep Compose
-keepclassmembers class androidx.compose.** { *; }
