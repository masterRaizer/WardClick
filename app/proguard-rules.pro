# Сжатие кода R8 — нужно, чтобы APK весил меньше (без него ~120 МБ из-за библиотеки иконок).
# Имена классов не меняем (dontobfuscate), только вырезаем неиспользуемый код.
-dontobfuscate
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Модели данных, DTO, mock-сервер и сессия — их читает/пишет Gson через рефлексию
-keep class com.wardclick.app.data.** { *; }
-keep class com.wardclick.app.util.** { *; }
# Маршруты навигации (kotlinx.serialization)
-keep class com.wardclick.app.ui.navigation.** { *; }

# Yandex MapKit (JNI + рефлексия)
-keep class com.yandex.** { *; }
-dontwarn com.yandex.**

# Retrofit / OkHttp / Gson
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
