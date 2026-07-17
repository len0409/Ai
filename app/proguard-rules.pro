# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Netty
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
