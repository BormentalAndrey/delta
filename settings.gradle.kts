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
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "MultiAppLauncher"
include(":app")
include(":deltachat")

// Указываем, что модуль deltachat находится в папке delta
project(":deltachat").projectDir = file("delta")
