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
# ИСПРАВЛЕНИЯ ДЛЯ МОДУЛЕЙ MULTIAPP, TYR И SECURESMS (Финальное решение)
# ================================================================================

# Игнорируем отсутствующие аннотации сборщика (aQute/BND), на которые ругается R8
-dontwarn aQute.bnd.annotation.spi.**
-keep class aQute.bnd.annotation.spi.** { *; }

# КРИТИЧНО: Защищаем пакет лаунчера, откуда идут вызовы к Tyr и SecureSMS!
-keep class com.launcher.multiapp.** { *; }

# Сохраняем ВСЁ из проблемных пакетов, включая внутренние классы ($)
-keep class com.jbselfcompany.tyr.** { *; }
-keep interface com.jbselfcompany.tyr.** { *; }
-keep class org.thoughtcrime.securesms.** { *; }
-keep interface org.thoughtcrime.securesms.** { *; }

# Явное сохранение Companion-объектов (Kotlin) и их членов
-keepclassmembers class **$Companion { *; }
-keep class com.jbselfcompany.tyr.TyrApplication$Companion { *; }
-keep class com.jbselfcompany.tyr.service.YggmailService$Companion { *; }

# Сохраняем конкретные классы, которые R8 пометил как Missing
-keep class com.jbselfcompany.tyr.data.ConfigRepository { *; }
-keep class com.jbselfcompany.tyr.utils.AutoconfigServer { *; }
-keep class com.jbselfcompany.tyr.utils.LocaleHelper { *; }
-keep class org.thoughtcrime.securesms.BaseConversationListFragment$ConversationSelectedListener { *; }
-keep class org.thoughtcrime.securesms.ConversationListFragment { *; }

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
# БИБЛИОТЕКИ (Guava, Termux, Apache, Log4j)
# ================================================================================
-dontwarn com.google.common.**
-keep class com.google.common.** { *; }

-dontwarn com.termux.**
-keep class com.termux.** { *; }

-dontwarn org.apache.logging.log4j.**
-keep class org.apache.logging.log4j.** { *; }

-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }

-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }

# ================================================================================
# КРИПТОГРАФИЯ И БД
# ================================================================================
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }

-dontwarn net.sqlcipher.**
-keep class net.sqlcipher.** { *; }

# ================================================================================
# ГРАФИКА И ДВИЖКИ (libGDX)
# ================================================================================
-dontwarn com.badlogicgames.gdx.**
-keep class com.badlogicgames.gdx.** { *; }
-keep class com.badlogic.gdx.backends.android.** { *; }

# ================================================================================
# ANDROIDX & KOTLIN
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
# СУПРЕССИЯ ПРЕДУПРЕЖДЕНИЙ ДЛЯ НЕ-ANDROID КЛАССОВ
# ================================================================================
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn org.osgi.framework.**
-dontwarn net.sf.saxon.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn edu.umd.cs.findbugs.annotations.**
