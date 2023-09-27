import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
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

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += "room.schemaLocation" to "$projectDir/schemas"
            }
        }
    }

    // __(API)_(feature)_(abi)___(version)
    flavorDimensions += listOf("abi", "feature")
    productFlavors {
        create("arm64-v8a") {
            dimension = "abi"
            versionCode = 1000 + (android.defaultConfig.versionCode ?: 0)
        }
        create("armeabi-v7a") {
            dimension = "abi"
            versionCode = 2000 + (android.defaultConfig.versionCode ?: 0)
        }
        create("x86") {
            dimension = "abi"
            versionCode = 3000 + (android.defaultConfig.versionCode ?: 0)
        }
        create("x86_64") {
            dimension = "abi"
            versionCode = 4000 + (android.defaultConfig.versionCode ?: 0)
        }
        create("foss") {
            dimension = "feature"
            versionCode = 10000 + (android.defaultConfig.versionCode ?: 0)
            applicationIdSuffix = ".foss"
        }
        create("premium") {
            dimension = "feature"
            versionCode = 20000 + (android.defaultConfig.versionCode ?: 0)
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

    // RootService
    implementation(project(":librootservice"))

    // Hidden api
    implementation(project(":libhiddenapi"))

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

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coil
    implementation(libs.coil.compose)

    // Palette
    implementation(libs.androidx.palette)

    // Firebase
    "premiumImplementation"(platform(libs.firebase.bom))
    "premiumImplementation"(libs.firebase.crashlytics)
    "premiumImplementation"(libs.firebase.analytics)
}

kapt {
    correctErrorTypes = true
}
