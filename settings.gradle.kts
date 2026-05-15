pluginManagement {
    repositories {
        // Приоритет Google репозитория для плагинов Android
        google {
            content {
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("androidx")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS гарантирует, что все репозитории объявлены здесь, а не в build.gradle модулей
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // JitPack необходим для сторонних библиотек, загружаемых напрямую из GitHub
        maven { 
            url = uri("https://jitpack.io") 
            content {
                includeGroup("com.github.amulyakhare") // Для TextDrawable
                includeGroup("com.github.Baseflow")    // Для PhotoView
                includeGroup("com.github.bumptech.glide")
                includeGroup("com.github.davemorrissey")
            }
        }
    }
}

// Имя корневого проекта
rootProject.name = "MultiAppLauncher"

// Подключение модулей
include(":app")
include(":deltachat")
include(":tyr")

// Привязка имен модулей к физическим путям в файловой системе
// Модуль :deltachat находится в папке /delta в корне проекта
project(":deltachat").projectDir = file("delta")

// Модуль :tyr находится во вложенной папке /tyr/app
project(":tyr").projectDir = file("tyr/app")
