pluginManagement {
    repositories {
        google()
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

rootProject.name = "WandasPhone"

// App module
include(":app")

// Core modules
include(":core:core-ui")
include(":core:core-tts")
include(":core:core-config")
include(":core:core-data")
include(":core:core-telecom")

// Feature modules
include(":feature:feature-home")
include(":feature:feature-phone")
include(":feature:feature-contacts")
include(":feature:feature-carer")
include(":feature:feature-kiosk")

