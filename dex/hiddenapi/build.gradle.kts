plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.xayah.hiddenapi"
    compileSdk = 34

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    annotationProcessor(libs.refine.annotation.processor)
    compileOnly(libs.refine.annotation)
}