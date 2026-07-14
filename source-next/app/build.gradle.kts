plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.refine)
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

    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hidden api
    implementation(project(":hiddenapi"))

    // Native
    implementation(project(":native"))

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // The core module that provides APIs to a shell
    implementation(libs.libsu.core)

    // APIs for creating root services
    implementation(libs.libsu.service)

    // Compose navigation
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // JSON serialization
    implementation(libs.kotlinx.serialization.json)

    // Ktor
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Coil
    implementation(libs.coil.compose)

    // Animated vector
    implementation(libs.androidx.animation.graphics)

    // Work manager
    implementation(libs.androidx.work.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.adapters)
    ksp(libs.moshi.kotlin.codegen)

    // Gson
    implementation(libs.gson)

    // Koin
    implementation(libs.koin.androidx.compose)

    // Accompanist permissions
    implementation(libs.accompanist.permissions)

    // Arrow optics
    implementation(libs.arrow.optics)
    ksp(libs.arrow.optics.ksp.plugin)
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
            "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode",
            "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}
