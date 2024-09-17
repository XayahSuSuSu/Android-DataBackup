plugins {
    alias(libs.plugins.library.common)
}

android {
    namespace = "com.xayah.core.hiddenapi"

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    annotationProcessor(libs.refine.annotation.processor)
    compileOnly(libs.refine.annotation)

    // Core
    implementation(project(":core:common"))
}