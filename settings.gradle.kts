pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("org\\.jetbrains.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
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
include(":domain")

// Core Modules
include(":core:database")
include(":core:data")
include(":core:work")
include(":core:permissions")

// Feature Modules
include(":feature:ocr")
include(":feature:home")
include(":feature:labeling")
include(":feature:media")
