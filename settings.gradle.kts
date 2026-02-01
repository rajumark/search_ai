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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Photo Search AI"
include(":app")

// Core modules
include(":core:common")
include(":core:database")
include(":core:datastore")
include(":core:ml")
include(":core:work")

// Feature modules
include(":feature:ocr")
include(":feature:face")
include(":feature:barcode")
include(":feature:image_label")
include(":feature:search")
include(":feature:permission")
include(":feature:battery")
