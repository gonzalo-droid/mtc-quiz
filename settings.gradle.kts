pluginManagement {
    /**
     * Un included build es un proyecto Gradle separado dentro de tu proyecto principal,
     * cuya única función es contener lógica de construcción (build logic).
     * build-logic:convention -> libs de configuraciones de gradle para reutilizar en otros modulos
     */
    includeBuild("build-logic")
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
// Esto evita el uso de strings manuales como project(":auth:domain").
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":auth:data")
include(":auth:presentation")
include(":auth:domain")

include(":core:data")
include(":core:domain")
include(":core:database")
include(":core:presentation:designsystem")
include(":core:presentation:ui")

include(":evaluation:data")
include(":evaluation:presentation")
include(":evaluation:domain")

include(":pdf:presentation")
include(":pdf:data")
include(":pdf:domain")

include(":configuration:data")
include(":configuration:domain")
include(":configuration:presentation")

include(":home:data")
include(":home:presentation")
include(":home:domain")
include(":category:data")

include(":detail:data")
include(":detail:presentation")
include(":detail:domain")

include(":questionreview:data")
include(":questionreview:presentation")
include(":questionreview:domain")
