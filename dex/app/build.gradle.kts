@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.refine)
}

android {
    namespace = "com.xayah.dex"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xayah.dex"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    testImplementation(libs.junit)
    implementation(libs.appcompat)
    implementation(libs.refine.runtime)
    implementation(libs.httpclient5.fluent)
    implementation(libs.slf4j.simple)
    implementation(libs.gson)

    compileOnly(project(":hiddenapi"))
}