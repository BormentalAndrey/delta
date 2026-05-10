pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Исправлено: убран www
    }
}

rootProject.name = "MultiAppLauncher"
include(":app")
include(":deltachat")
include(":tyr")

// Указываем пути к модулям в подпапках
project(":deltachat").projectDir = file("delta")
project(":tyr").projectDir = file("tyr/app")
