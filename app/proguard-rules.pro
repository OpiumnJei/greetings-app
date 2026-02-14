# =====================================================================
# PROGUARD RULES - GREETINGS APP
# =====================================================================
# ProGuard/R8 es una herramienta que:
#   1. MINIFICA: Elimina código no utilizado (reduce tamaño del APK)
#   2. OFUSCA: Renombra clases/métodos a nombres cortos (a, b, c...)
#   3. OPTIMIZA: Mejora el rendimiento del bytecode
#
# PROBLEMA: Algunas librerías usan "reflexión" (acceden a clases por nombre)
# Si ofuscamos esos nombres, la app crashea en runtime.
# SOLUCIÓN: Usar reglas "-keep" para excluir clases de la ofuscación.
# =====================================================================

# Para más detalles:
#   http://developer.android.com/guide/developing/tools/proguard.html

# =====================================================================
# CONFIGURACIÓN DE DEBUG
# =====================================================================

# Preserva información de línea para stack traces legibles en crashs
-keepattributes SourceFile,LineNumberTable

# Oculta el nombre real del archivo fuente en los stack traces
-renamesourcefileattribute SourceFile

# =====================================================================
# MODELOS DE DATOS (Data Classes)
# =====================================================================

-keep class com.greetingsapp.mobile.data.model.** { *; }
-keepclassmembers class com.greetingsapp.mobile.data.model.** { *; }
-keep class com.greetingsapp.mobile.data.local.entities.** { *; }
-keepclassmembers class com.greetingsapp.mobile.data.local.entities.** { *; }

# =====================================================================
# RETROFIT
# =====================================================================

-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface com.greetingsapp.mobile.data.network.ApiService { *; }
-keep class com.greetingsapp.mobile.data.network.RetrofitClient { *; }

# =====================================================================
# GSON
# =====================================================================

-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepclassmembers class * {
    public <init>();
}
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# =====================================================================
# OKHTTP
# =====================================================================

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# =====================================================================
# COIL
# =====================================================================

-dontwarn coil.**
-keep class coil.** { *; }

# =====================================================================
# ROOM DATABASE
# =====================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# =====================================================================
# ADMOB
# =====================================================================

-keep class com.google.android.gms.ads.** { *; }

# =====================================================================
# KOTLIN COROUTINES
# =====================================================================

-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# =====================================================================
# KOTLIN (General)
# =====================================================================

-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**
-keepclassmembers class * {
    ** component1();
    ** component2();
    ** component3();
    ** component4();
    ** component5();
}

