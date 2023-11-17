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
include(":core:network")
include(":feature:guide:common")
include(":feature:guide:foss")
include(":feature:guide:premium")
include(":feature:main:home:common")
include(":feature:main:home:foss")
include(":feature:main:home:premium")
include(":feature:main:directory")
include(":feature:main:log")
include(":feature:main:task:packages:common")
include(":feature:main:task:packages:local")
include(":feature:main:task:packages:cloud")
include(":feature:main:task:medium:common")
include(":feature:main:task:medium:local")
include(":feature:main:task:medium:cloud")
