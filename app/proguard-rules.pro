# AttendWise ProGuard Rules

# 1. General Optimization Rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# 2. Your Data Models (Keep for Firestore/Room reflection)
# If you use reflection or Firestore's automatic mapping, keep these.
-keep class com.ankit.attendwise.data.** { *; }
-keep class com.ankit.attendwise.models.** { *; }

# 3. Firebase / Google Services
# Most Firebase libraries include their own ProGuard rules.
# Only add these if you see specific warnings about missing Firebase classes.
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# 4. Room
# Room handles its own ProGuard rules via the compiler.
# Manual keep rules are usually unnecessary.
-dontwarn androidx.room.**

# 5. Compose
# Compose handles its own rules. Broadly keeping all of Compose is unnecessary.
-dontwarn androidx.compose.**

# 6. Kizitonwose Calendar
# This library might need specific keeps if it uses reflection for view binding or similar.
-keep class com.kizitonwose.calendar.** { *; }

# 7. Common R8/ProGuard Fixes
-ignorewarnings
-keep class androidx.lifecycle.DefaultLifecycleObserver
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepattributes *Annotation*,Signature,EnclosingMethod,InnerClasses
