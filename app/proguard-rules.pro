# Keep app entry points
-keep class com.example.AiRelayApp { *; }
-keep class com.example.MainActivity { *; }

# Ktor / Netty
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.netty.** { *; }
-dontwarn io.netty.**
-dontwarn reactor.blockhound.**
-dontwarn sun.misc.**
-dontwarn java.lang.management.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# SLF4J
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp / Okio
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose / Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
