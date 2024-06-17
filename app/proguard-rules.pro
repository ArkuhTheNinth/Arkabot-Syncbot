# Preserve Application class and its methods
-keep class com.arkabot.syncbot.** { *; }

# Preserve library classes
-keep class com.dropbox.** { *; }
-keep class okhttp3.** { *; }
-keep class org.json.** { *; }
-keep class androidx.** { *; }
-keep class kotlinx.** { *; }

# Add any other rules to preserve methods/classes that should not be obfuscated
