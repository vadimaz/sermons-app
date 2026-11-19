pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "SermonIndex"
include(":app")

include(":core:model")
include(":core:common")
include(":core:navigation")
include(":core:domain")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:media")
include(":core:testing")
include(":core:ui")

include(":feature:home")
include(":feature:search")
include(":feature:bible")
include(":feature:scripture")
include(":feature:speakers")
include(":feature:topics")
include(":feature:sermon")
include(":feature:player")
include(":feature:library")
