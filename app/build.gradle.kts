import java.util.Properties
import java.io.FileInputStream
import org.gradle.api.tasks.Copy

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
val composeBomVersion = "2024.11.00"
val composeCompilerVersion = "1.5.15"
val activityComposeVersion = "1.9.3"
val navigationComposeVersion = "2.8.5"
val lifecycleVersion = "2.8.7"

/* ------------------------- GDX Native Copy Task ------------------------- */
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
    compileSdk = 36 // Примечание: Убедитесь, что используете последние версии build-tools для SDK 36

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
            "\"${System.getenv("GEMINI_API_KEY") ?: localProperties.getProperty("GEMINI_API_KEY", "")}\""
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
            // Исправлено: STORE_PASSWORD вместо KEYSTORE_PASSWORD для соответствия GitHub Secrets
            val sPassword = System.getenv("STORE_PASSWORD") ?: localProperties.getProperty("RELEASE_STORE_PASSWORD")
            val kAlias = System.getenv("KEY_ALIAS") ?: localProperties.getProperty("RELEASE_KEY_ALIAS")
            val kPassword = System.getenv("KEY_PASSWORD") ?: localProperties.getProperty("RELEASE_KEY_PASSWORD")

            // Применяем подпись только если найдены пароли, иначе билд упадет с понятной ошибкой
            if (sPassword != null && kAlias != null) {
                storeFile = file("my-release-key.jks")
                storePassword = sPassword
                keyAlias = kAlias
                keyPassword = kPassword ?: sPassword // Если пароль ключа не задан, используем пароль стора
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isCrunchPngs = false
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
            // Исправлен путь к delta/libs для корректной работы в многомодульной структуре
            jniLibs.srcDirs(file("../delta/libs"), "src/main/jniLibs")
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
                "META-INF/library_release.kotlin_module",
                // Добавлены исключения для Apache POI
                "META-INF/services/javax.xml.stream.*"
            )
        }
    }
}

tasks.configureEach {
    if (name.contains("merge", ignoreCase = true) && name.contains("JniLibFolders", ignoreCase = true)) {
        dependsOn(copyAndroidNatives)
    }
}

dependencies {
    implementation(project(":deltachat"))
    implementation(project(":tyr"))

    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.guava:guava:$guavaVersion")

    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:$navigationComposeVersion")

    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.transition:transition:1.5.1")

    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

    implementation("com.google.crypto.tink:tink-android:$tinkVersion")

    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")

    implementation("org.json:json:20231013")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("org.apache.poi:poi-ooxml:$poiVersion")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
