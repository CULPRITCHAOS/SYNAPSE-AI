pluginManagement {
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SYNAPSE-AI"
include(":core:common")
include(":core:model-api")
include(":core:model-router")
include(":core:model-registry")
include(":core:apppack-api")
include(":core:tool-api")
include(":feature:orchestrator")
include(":core:model-gemma")
include(":app:mobile-host")
include(":core:tool-system")
