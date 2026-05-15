plugins {
    id("com.android.application") version "8.11.1" apply false
    id("com.android.library") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
}

// Отключить сжатие PNG для всех модулей
subprojects {
    afterEvaluate {
        tasks.withType<com.android.build.gradle.tasks.MergeResources> {
            isCrunchPngs = false
        }
    }
}
