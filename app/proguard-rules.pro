# Wall-E Desk Companion Proguard Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowoptimization class * {
    @kotlinx.serialization.Serializable class *;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
