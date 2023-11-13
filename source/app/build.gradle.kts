import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt.android)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
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
        vectorDrawables {
            useSupportLibrary = true
        }
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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("Boolean", "ENABLE_VERBOSE", "false")
        }
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("Boolean", "ENABLE_VERBOSE", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        outputs.forEach { output ->
            (output as BaseVariantOutputImpl).outputFileName =
                "DataBackup-${versionName}-${productFlavors[0].name}-${productFlavors[1].name}-${buildType.name}.apk"
        }

        // Drop extension for foss build
        if (productFlavors[1].name == "foss") {
            mergeAssetsProvider.get().doLast {
                delete(fileTree(mapOf("dir" to mergeAssetsProvider.get().outputDir, "includes" to listOf("extension.zip"))))
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // RootService
    implementation(project(":librootservice"))

    // Hidden api
    implementation(project(":libhiddenapi"))

    // Core
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:util"))

    // Feature
    "fossImplementation"(project(":feature:home:foss"))
    "premiumImplementation"(project(":feature:home:premium"))
    implementation(project(":feature:directory"))
    implementation(project(":feature:task:packages:local"))
    implementation(project(":feature:task:medium:local"))

    // Accompanist
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.drawablepainter)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // OkHttp
    implementation(libs.okhttp)

    // Gson
    implementation(libs.gson)

    // libsu
    implementation(libs.libsu.core)

    // zip4j
    implementation(libs.zip4j)

    // Coil
    implementation(libs.coil.compose)

    // Palette
    implementation(libs.androidx.palette)

    // Firebase
    "premiumImplementation"(platform(libs.firebase.bom))
    "premiumImplementation"(libs.firebase.crashlytics)
    "premiumImplementation"(libs.firebase.analytics)

    // PickYou
    implementation(libs.pickyou)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)
}

kapt {
    correctErrorTypes = true
}
