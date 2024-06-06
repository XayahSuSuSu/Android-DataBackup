plugins {
    alias(libs.plugins.library.common)
}

android {
    namespace = "com.xayah.core.systemapi"

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)

    // Core
    implementation(project(":core:common"))
}