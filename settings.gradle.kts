pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        maven(url = "https://jitpack.io")
        mavenCentral()
    }
}

rootProject.name = "MTCQuiz"
include(":app")
include(":domain")
include(":mylibrary")
include(":data")
include(":presentation")
