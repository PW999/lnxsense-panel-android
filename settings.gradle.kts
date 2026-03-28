// Standalone Gradle project — not listed in the parent Maven pom.xml.
// Android projects require Gradle (AGP does not support Maven), so this module
// lives alongside the Maven modules but is built independently with ./gradlew.
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "lnxsense-android"
include(":app")
