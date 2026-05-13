import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

/* ------------------------- Local properties ------------------------- */
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

/* ------------------------- Versions ------------------------- */
val roomVersion = "2.6.1"
val gdxVersion = "1.12.1"
val media3Version = "1.4.1"
val okhttpVersion = "4.12.0"
val tinkVersion = "1.15.0"
val coilVersion = "2.6.0"
val poiVersion = "5.2.5"
val guavaVersion = "33.2.1-android"
val composeBomVersion = "2024.12.00"
val composeCompilerVersion = "1.5.15"
val activityComposeVersion = "1.9.3"
val navigationComposeVersion = "2.8.5"
val lifecycleVersion = "2.8.7"

android {
    namespace = "com.launcher.multiapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.launcher.multiapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        missingDimensionStrategy("none", "foss")
        multiDexEnabled = true

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${System.getenv("GEMINI_API_KEY")
                ?: localProperties.getProperty("GEMINI_API_KEY", "")}\""
        )
    }

    flavorDimensions += "none"
    productFlavors {
        create("foss") {
            dimension = "none"
        }
        create("gplay") {
            dimension = "none"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = System.getenv("KEY_ALIAS")
                ?: localProperties.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("delta/libs")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += "**/*.so"
        }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST",
                "META-INF/NOTICE*",
                "META-INF/LICENSE*",
                "META-INF/kotlinx-coroutines-core.kotlin_module",
                "META-INF/tink/**",
                "META-INF/library_release.kotlin_module"
            )
        }
    }
}

/* ------------------------- Dependencies ------------------------- */
dependencies {
    // Основные модули
    implementation(project(":deltachat"))
    implementation(project(":tyr"))

    // Android Core
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.guava:guava:$guavaVersion")

    // Compose — обновлённый BOM и зависимости
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:$navigationComposeVersion")

    // Material
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.transition:transition:1.5.1")

    // Coil
    implementation("io.coil-kt:coil-compose:$coilVersion")

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

    // Security
    implementation("com.google.crypto.tink:tink-android:$tinkVersion")

    // Media
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    // WebRTC — используется из DeltaChat (Stream WebRTC удалён)

    // Graphics
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    // Utils
    implementation("org.json:json:20231013")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Docs
    implementation("org.apache.poi:poi-ooxml:$poiVersion")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Core desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Tests
    testImplementation("junit:junit:4.13.2")
}
