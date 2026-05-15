import java.util.Properties
import java.io.FileInputStream
import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val roomVersion = "2.6.1"
val gdxVersion = "1.12.1"
val media3Version = "1.4.1"
val okhttpVersion = "4.12.0"
val tinkVersion = "1.15.0"
val coilVersion = "2.6.0"
val poiVersion = "5.2.5"
val guavaVersion = "33.2.1-android"
val composeBomVersion = "2024.11.00"
val composeCompilerVersion = "1.5.15"
val activityComposeVersion = "1.9.3"
val navigationComposeVersion = "2.8.5"
val lifecycleVersion = "2.8.7"

// Задача для копирования библиотек игры (libGDX)
val copyAndroidNatives = tasks.register<Copy>("copyAndroidNatives") {
    val platforms = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    into(layout.projectDirectory.dir("src/main/jniLibs"))
    platforms.forEach { platform ->
        val cfg = configurations.detachedConfiguration(
            dependencies.create("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-$platform")
        )
        from(cfg.map { zipTree(it) }) {
            include("**/*.so")
            into(platform)
        }
    }
}

android {
    namespace = "com.launcher.multiapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.launcher.multiapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64") }
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = composeCompilerVersion }

    sourceSets {
        getByName("main") { jniLibs.srcDirs("delta/libs", "src/main/jniLibs") }
    }

    packaging {
        jniLibs { useLegacyPackaging = true; pickFirsts += "**/*.so" }
    }
}

tasks.whenTaskAdded {
    if (name.contains("merge", true) && name.contains("JniLibFolders", true)) {
        dependsOn(copyAndroidNatives)
    }
}

dependencies {
    implementation(project(":deltachat"))
    implementation(project(":tyr"))

    // Android Core & Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:$navigationComposeVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // Media3 - ИСПРАВЛЕНО (добавлена сессия)
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version") 

    // Graphics
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    // Другие зависимости
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
}
