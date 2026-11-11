# kotlinx.serialization keeps generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class codes.loveless.archiveplayer.tv.**$$serializer { *; }
-keepclassmembers class codes.loveless.archiveplayer.tv.** { *** Companion; }
-keepclasseswithmembers class codes.loveless.archiveplayer.tv.** { kotlinx.serialization.KSerializer serializer(...); }
