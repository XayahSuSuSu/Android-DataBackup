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
include(":core:common")
include(":core:service")
include(":core:ui")
include(":core:model")
include(":core:database")
include(":core:data")
include(":core:datastore")
include(":core:util")
include(":core:hiddenapi")
include(":core:rootservice")
include(":feature:home:common")
include(":feature:home:foss")
include(":feature:home:premium")
include(":feature:directory")
include(":feature:task:packages:common")
include(":feature:task:packages:local")
include(":feature:task:packages:cloud")
include(":feature:task:medium:common")
include(":feature:task:medium:local")
include(":feature:task:medium:cloud")
