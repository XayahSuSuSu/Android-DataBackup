import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.application.common)
    alias(libs.plugins.application.hilt)
    alias(libs.plugins.application.compose)
}

android {
    namespace = "com.xayah.databackup"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.xayah.databackup"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += listOf("abi", "feature")
    productFlavors {
        create("arm64-v8a") {
            dimension = "abi"
            versionCode = 4 + (android.defaultConfig.versionCode ?: 0)
        }
        create("armeabi-v7a") {
            dimension = "abi"
            versionCode = 3 + (android.defaultConfig.versionCode ?: 0)
        }
        create("x86_64") {
            dimension = "abi"
            versionCode = 2 + (android.defaultConfig.versionCode ?: 0)
        }
        create("x86") {
            dimension = "abi"
            versionCode = 1 + (android.defaultConfig.versionCode ?: 0)
        }
        create("foss") {
            dimension = "feature"
            applicationIdSuffix = ".foss"
        }
        create("premium") {
            dimension = "feature"
            applicationIdSuffix = ".premium"
        }
    }

    applicationVariants.all {
        outputs.forEach { output ->
            (output as BaseVariantOutputImpl).outputFileName =
                "DataBackup-${versionName}-${productFlavors[0].name}-${productFlavors[1].name}-${buildType.name}.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:util"))
    implementation(project(":core:hiddenapi"))
    implementation(project(":core:rootservice"))

    // Feature
    implementation(project(":feature:crash"))
    "fossImplementation"(project(":feature:guide:foss"))
    "fossImplementation"(project(":feature:flavor:foss"))
    "premiumImplementation"(project(":feature:guide:premium"))
    "premiumImplementation"(project(":feature:flavor:premium"))
    implementation(project(":feature:main:home"))
    implementation(project(":feature:main:packages"))
    implementation(project(":feature:main:directory"))
    implementation(project(":feature:main:log"))
    implementation(project(":feature:main:tree"))
    implementation(project(":feature:main:task"))

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // libsu
    implementation(libs.libsu.core)
}
