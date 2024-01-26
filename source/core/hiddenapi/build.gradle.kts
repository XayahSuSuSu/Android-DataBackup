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
}

dependencies {
    implementation(libs.hiddenapibypass)
    implementation(libs.androidx.core.ktx)

    // Core
    implementation(project(":core:common"))
}