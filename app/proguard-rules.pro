# ================================================================================
# ОСНОВНЫЕ ПРАВИЛА СОХРАНЕНИЯ (CORE)
# ================================================================================

# Сохранять атрибуты для отладки, работы библиотек и рефлексии
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# Сохранять перечисления и методы сериализации
-keepclassmembers enum * { *; }

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object readResolve();
    java.lang.Object writeReplace();
}

# ================================================================================
# ИСПРАВЛЕНИЯ ДЛЯ МОДУЛЕЙ TYR И DELTA (Критично для текущей ошибки сборки)
# ================================================================================

# Сохраняем всё из пакетов, на которые ругался R8
-keep class com.jbselfcompany.tyr.** { *; }
-keep interface com.jbselfcompany.tyr.** { *; }

-keep class org.thoughtcrime.securesms.** { *; }
-keep interface org.thoughtcrime.securesms.** { *; }

# Сохраняем Companion объекты, так как к ним обращаются через рефлексию/синглтоны
-keepclassmembers class **$Companion { *; }

# Специальное правило для внедрения зависимостей и репозиториев в Tyr
-keep class com.jbselfcompany.tyr.data.** { *; }
-keep class com.jbselfcompany.tyr.service.** { *; }
-keep class com.jbselfcompany.tyr.utils.AutoconfigServer { *; }
-keep class com.jbselfcompany.tyr.utils.LocaleHelper { *; }

# ================================================================================
# AI & JNI INTEGRATION (LlamaBridge)
# ================================================================================

# Запрещаем переименовывать модели данных
-keep class com.kakdela.p2p.model.** { *; }

# ЗАПРЕЩАЕМ обфускацию моста JNI.
-keep class com.kakdela.p2p.ai.LlamaBridge {
    native <methods>;
    <fields>;
    public *;
}

# Защищаем ViewModel и их конструкторы
-keep class com.kakdela.p2p.viewmodel.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# ================================================================================
# GUAVA (Исправляет Missing class com.google.common.io.MoreFiles)
# ================================================================================
-dontwarn com.google.common.io.**
-dontwarn com.google.common.collect.**
-dontwarn com.google.common.util.concurrent.**
-dontwarn com.google.common.cache.**
-keep class com.google.common.io.** { *; }
-keep class com.google.common.collect.** { *; }
-keep class com.google.common.base.** { *; }

# ================================================================================
# TERMUX / TERMINAL
# ================================================================================
-dontwarn com.termux.**
-keep class com.termux.** { *; }
-keep interface com.termux.** { *; }

# ================================================================================
# PDFBOX & APACHE POI (Работа с документами)
# ================================================================================
-dontwarn org.apache.pdfbox.**
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }

-dontwarn org.apache.poi.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.xmlbeans.**
-keep class org.apache.poi.** { *; }

# ================================================================================
# БИБЛИОТЕКИ БЕЗОПАСНОСТИ, КРИПТО И БД
# ================================================================================
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn net.sqlcipher.**
-keep class net.sqlcipher.** { *; }

# ================================================================================
# ГРАФИКА (libGDX)
# ================================================================================
-dontwarn com.badlogicgames.gdx.**
-keep class com.badlogicgames.gdx.** { *; }
-keep class com.badlogic.gdx.backends.android.** { *; }

# ================================================================================
# ANDROIDX, COMPOSE & KOTLIN
# ================================================================================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class com.google.android.material.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**

# ================================================================================
# ИСКЛЮЧЕНИЯ (DON'T WARN)
# ================================================================================
-dontwarn java.awt.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn org.osgi.framework.**
-dontwarn net.sf.saxon.**
