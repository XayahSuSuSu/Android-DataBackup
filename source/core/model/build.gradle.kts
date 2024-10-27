plugins {
    alias(libs.plugins.library.common)
    alias(libs.plugins.library.room)
    alias(libs.plugins.library.test)
    alias(libs.plugins.library.protobuf)
}

android {
    namespace = "com.xayah.core.model"

    buildFeatures {
        aidl = true
    }
}

dependencies {
    // Core
    implementation(project(":core:common"))
    compileOnly(project(":core:hiddenapi"))

    // Gson
    implementation(libs.gson)
}
