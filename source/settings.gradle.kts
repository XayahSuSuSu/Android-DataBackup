pluginManagement {
    includeBuild("build-logic")
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
        maven("https://jitpack.io")
    }
}

rootProject.name = "DataBackup"
include(":app")
include(":librootservice")
include(":libhiddenapi")
include(":core:common")
include(":core:ui")
include(":core:model")
include(":core:database")
include(":core:data")
include(":core:datastore")
include(":core:util")
include(":feature:home:common")
include(":feature:home:foss")
include(":feature:home:premium")
include(":feature:directory")
